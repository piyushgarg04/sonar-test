package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.xpo.ltl.api.exception.ExceptionBuilder;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationErrorMessage;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.location.v2.GetRefSicAddressResp;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmt;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmtPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.HandlingUnitMovementTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ServiceTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.EventLogSubTypeCd;
import com.xpo.ltl.api.shipment.v2.EventLogTypeCd;
import com.xpo.ltl.api.shipment.v2.HandlingUnitMovementTypeCd;
import com.xpo.ltl.api.shipment.v2.MovementStatusCd;
import com.xpo.ltl.api.shipment.v2.NonLoadedHandlingUnit;
import com.xpo.ltl.api.shipment.v2.ServiceTypeCd;
import com.xpo.ltl.api.shipment.v2.SplitNonLoadedHandlingUnitsResp;
import com.xpo.ltl.api.shipment.v2.SplitNonLoadedHandlingUnitsRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitMvmtSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentEagerLoadPlan;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmEventDelegate;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.DB2DefaultValueUtil;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;

@ApplicationScoped
@LogExecutionTime
public class SplitNonLoadedHandlingUnitsImpl {

    private static final Logger LOGGER = LogManager.getLogger(SplitNonLoadedHandlingUnitsImpl.class);
    private static final String PGM_ID = "SPLITHU";
    private static final String MISSING_MOVEMENT_CD = "MISSING";
    private static final String TRAN_ID = "SPLT";

	@PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
	private EntityManager db2EntityManager;

	@Inject
	private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

	@Inject
    private ShmShipmentSubDAO shmShipmentSubDAO;

    @Inject
	private ShmHandlingUnitMvmtSubDAO shmHandlingUnitMvmtSubDAO;

    @Inject
    private ShmEventDelegate shmEventDelegate;

    @Inject
    private ExternalRestClient externalRestClient;

    public SplitNonLoadedHandlingUnitsResp splitNonLoadedHandlingUnits(
        SplitNonLoadedHandlingUnitsRqst splitNonLoadedHandlingUnitsRqst, TransactionContext txnContext,
        EntityManager entityManager) throws ServiceException {

        checkNotNull(splitNonLoadedHandlingUnitsRqst, "SplitNonLoadedHandlingUnits Request is required.");
        checkNotNull(txnContext, "The TransactionContext is required.");
        checkNotNull(entityManager, "The EntityManager is required.");

        AuditInfo auditInfo = AuditInfoHelper.getAuditInfoWithPgmId(PGM_ID, txnContext);
        LinkedHashMap<NonLoadedHandlingUnit, List<String>> errorMsgsMap = new LinkedHashMap<>();

        validateRequest(splitNonLoadedHandlingUnitsRqst, errorMsgsMap, txnContext);

        throwExceptionIfAnyValidationError(errorMsgsMap, txnContext);

        List<String> inputParentProList = splitNonLoadedHandlingUnitsRqst
            .getNonLoadedHandlingUnits()
            .stream()
            .map(hu -> hu.getParentProNbr())
            .filter(pro -> pro != null)
            .collect(Collectors.toList());

        String requestingSic = splitNonLoadedHandlingUnitsRqst
                .getNonLoadedHandlingUnits()
                .stream()
                .map(hu -> hu.getRequestingSicCd())
                .findFirst()
                .orElse(StringUtils.EMPTY);

        ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan =
            new ShmShipmentEagerLoadPlan();
        shmShipmentEagerLoadPlan.setShmAsEntdCusts(true);

        List<ShmShipment> shmShipmentList =
            shmShipmentSubDAO.listShipmentsByProNbrs
                (inputParentProList,
                 shmShipmentEagerLoadPlan,
                 entityManager);

        validateShipments(shmShipmentList, txnContext, entityManager);

        throwExceptionIfAnyValidationError(errorMsgsMap, txnContext);

        List<ShmHandlingUnit> allShmHandlingUnitsDB = shmHandlingUnitSubDAO
            .findByParentProNumberList(inputParentProList, entityManager);

        Map<String, List<ShmHandlingUnit>> parentProAndShmHUsMapFromDB = allShmHandlingUnitsDB
            .stream()
            .collect(Collectors.groupingBy(ShmHandlingUnit::getParentProNbrTxt, Collectors.toList()));

        validateChildProAgainstDB(splitNonLoadedHandlingUnitsRqst, parentProAndShmHUsMapFromDB, requestingSic, errorMsgsMap,
            txnContext);

        throwExceptionIfAnyValidationError(errorMsgsMap, txnContext);

        createShmHandlingUnitMvmtsAndUpdateShmHandlingUnit(allShmHandlingUnitsDB, parentProAndShmHUsMapFromDB,
            splitNonLoadedHandlingUnitsRqst, shmShipmentList, entityManager, auditInfo, txnContext);

        updateShmShipmentsAndCreateEvents(shmShipmentList, requestingSic, auditInfo, entityManager, txnContext);

        return new SplitNonLoadedHandlingUnitsResp();
	}


    private void updateShmShipmentsAndCreateEvents(List<ShmShipment> shmShipmentList, String requestingSic, AuditInfo auditInfo, EntityManager entityManager, TransactionContext txnContext) throws ServiceException {
        boolean mustCreateEvent = false;
        for (ShmShipment shmShipment : shmShipmentList) {
            mustCreateEvent = false;
            if (!BasicTransformer.toBoolean(shmShipment.getHandlingUnitSplitInd())) {
                mustCreateEvent = true;
            }
            DtoTransformer.setLstUpdateAuditInfo(shmShipment, auditInfo);
            shmShipment.setHandlingUnitSplitInd(BasicTransformer.toString(true));
            shmShipmentSubDAO.updateDB2ShmShipment(shmShipment, shmShipment.getLstUpdtTmst(), txnContext, db2EntityManager);
            if (mustCreateEvent) {
                shmEventDelegate
                    .createEvent(0L, EventLogTypeCd.SHIPMENT_UPDATE, EventLogSubTypeCd.SPLIT, shmShipment, null, requestingSic, Optional.empty(),
                        TRAN_ID, entityManager, auditInfo);
            }
        }
    }


    private void validateShipments(List<ShmShipment> shmShipmentList, TransactionContext txnContext,
        EntityManager entityManager) throws ServiceException {

        List<MoreInfo> moreInfoList = new ArrayList<MoreInfo>();

        for (ShmShipment shmShipment : shmShipmentList) {
            if (BasicTransformer.toBoolean(shmShipment.getHazmatInd())) {
                addMsgToMoreInfoList("Shipment Validation",
                        String
                            .format("Parent PRO %s is a HazMat shipment. Split is not allowed.",
                                formatProToNineDigitHyphen(shmShipment.getProNbrTxt(), txnContext)),
                    moreInfoList);
            }
            if (BasicTransformer.toBoolean(shmShipment.getGarntdInd())) {
                addMsgToMoreInfoList("Shipment Validation",
                        String
                            .format("Parent PRO %s is a Guaranteed shipment. Split is not allowed.",
                                formatProToNineDigitHyphen(shmShipment.getProNbrTxt(), txnContext)),
                    moreInfoList);
            }
            ServiceTypeCd svcTypCd = ServiceTypeCdTransformer.toEnum(shmShipment.getSvcTypCd());
            if (svcTypCd == ServiceTypeCd.RAPID_REMOTE_SERVICE) {
                addMsgToMoreInfoList("Shipment Validation",
                        String
                            .format("Parent PRO %s is a Rapid Remote Service shipment. Split is not allowed.",
                                formatProToNineDigitHyphen(shmShipment.getProNbrTxt(), txnContext)),
                    moreInfoList);
            }
            if (svcTypCd == ServiceTypeCd.GUARANTEED_BY_12_NOON) {
                addMsgToMoreInfoList("Shipment Validation",
                        String
                            .format("Parent PRO %s is a Guaranteed By Noon shipment. Split is not allowed.",
                                formatProToNineDigitHyphen(shmShipment.getProNbrTxt(), txnContext)),
                    moreInfoList);
            }

            // cross-border - get countryCode (LOC API) for the origin and destination SIC. If they are not the same, we
            // assume this is a cross-border shipment
            GetRefSicAddressResp originSicAddress = externalRestClient
                .getRefSicAddress(shmShipment.getOrigTrmnlSicCd(), txnContext);
            GetRefSicAddressResp destSicAddress = externalRestClient
                .getRefSicAddress(shmShipment.getDestTrmnlSicCd(), txnContext);

            if (originSicAddress != null && destSicAddress != null && originSicAddress.getLocAddress() != null
                    && destSicAddress.getLocAddress() != null && originSicAddress.getLocAddress().getCountryCd() != null
                    && destSicAddress.getLocAddress().getCountryCd() != null
                    && !originSicAddress
                        .getLocAddress()
                        .getCountryCd()
                        .equals(destSicAddress.getLocAddress().getCountryCd())) {
                addMsgToMoreInfoList("Shipment Validation",
                    String
                        .format("Parent PRO %s is a cross-border shipment. Split is not allowed.",
                            formatProToNineDigitHyphen(shmShipment.getProNbrTxt(), txnContext)),
                    moreInfoList);
            }

        }

        if (CollectionUtils.isNotEmpty(moreInfoList)) {
            throw ExceptionBuilder
            .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext).moreInfo(moreInfoList)
            .log()
            .build();
        }

    }


    private void addMsgToMoreInfoList(String location, String msg, List<MoreInfo> moreInfoList) {
        MoreInfo m1 = new MoreInfo();
        m1.setLocation(location);
        m1.setMessage(msg);
        moreInfoList.add(m1);
    }

    /**
     * if errorMsgsMap has at least one element, an exception will be throw. <br/>
     * The details of validation error will be sent in the MoreInfo part of a {@link ValidationException}.
     */
    private void throwExceptionIfAnyValidationError(LinkedHashMap<NonLoadedHandlingUnit, List<String>> errorMsgsMap,
        TransactionContext txnContext) throws ValidationException {
        if (!errorMsgsMap.isEmpty()) {
            List<MoreInfo> moreInfoList = new ArrayList<MoreInfo>();

            for (NonLoadedHandlingUnit nonLoadedHandlingUnit : errorMsgsMap.keySet()) {
                MoreInfo moreInfo = new MoreInfo();
                moreInfo
                    .setLocation(StringUtils.isBlank(nonLoadedHandlingUnit.getParentProNbr()) ? "N/A" :
                        formatProToNineDigitHyphen(nonLoadedHandlingUnit.getParentProNbr(), txnContext));
                moreInfo.setMessage(String.join(" ", errorMsgsMap.get(nonLoadedHandlingUnit)));
                moreInfoList.add(moreInfo);
            }

            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo(moreInfoList)
                .log()
                .build();
        }
    }


    /**
     * validate child pros and trailer nbr against data stored in DB.
     * <br/>
     * if any error is found a new message will be added to <code>errorMsgsMap</code> map.
     */
    private void validateChildProAgainstDB(SplitNonLoadedHandlingUnitsRqst splitNonLoadedHandlingUnitsRqst,
        Map<String, List<ShmHandlingUnit>> parentProAndShmHUsMapFromDB, String requestingSic,
        LinkedHashMap<NonLoadedHandlingUnit, List<String>> errorMsgsMap, TransactionContext txnContext)
            throws ValidationException {

        for (NonLoadedHandlingUnit nonLoadedHandlingUnit : splitNonLoadedHandlingUnitsRqst
            .getNonLoadedHandlingUnits()) {

            validateChildPro(nonLoadedHandlingUnit, parentProAndShmHUsMapFromDB, requestingSic,
                errorMsgsMap, txnContext);
        }
    }


    private void createShmHandlingUnitMvmtsAndUpdateShmHandlingUnit(List<ShmHandlingUnit> allShmHandlingUnitsDB,
        Map<String, List<ShmHandlingUnit>> parentProAndShmHUsMapFromDB,
        SplitNonLoadedHandlingUnitsRqst splitNonLoadedHandlingUnitsRqst, List<ShmShipment> shmShipmentList, EntityManager entityManager,
        AuditInfo auditInfo, TransactionContext txnContext) throws ServiceException {

        Map<ShmHandlingUnit, Long> nextMvmtSeqNbrMap = new HashedMap<>();

        for (NonLoadedHandlingUnit nonLoadedHandlingUnit : splitNonLoadedHandlingUnitsRqst
            .getNonLoadedHandlingUnits()) {

            List<ShmHandlingUnit> shmHandlingUnitListForParentPro = parentProAndShmHUsMapFromDB
                .get(nonLoadedHandlingUnit.getParentProNbr());
            Optional<ShmHandlingUnit> handlingUnitOpt = shmHandlingUnitListForParentPro
                .stream()
                .filter(hu -> hu.getChildProNbrTxt().equals(nonLoadedHandlingUnit.getChildProNbr()))
                .findFirst();

            if (!handlingUnitOpt.isPresent()) {
                continue;
            }

            ShmHandlingUnit handlingUnit = handlingUnitOpt.get();

            Optional<ShmShipment> shmShipment = shmShipmentList.stream().filter(shipment -> shipment.getProNbrTxt().equals(handlingUnit.getParentProNbrTxt())).findFirst();

            boolean parentOnTrailer = nonLoadedHandlingUnit.getTrailerNbr() != null ? NumberUtils.compare(nonLoadedHandlingUnit.getTrailerNbr(), 0L) > 0 : false;
            boolean parentOnDock = StringUtils.isNotBlank(nonLoadedHandlingUnit.getDockName());
            boolean mustSplitSiblings = (parentOnTrailer && handlingUnit.getCurrentTrlrInstId() != null 
                            && NumberUtils.compare(BasicTransformer.toLong(handlingUnit.getCurrentTrlrInstId()), nonLoadedHandlingUnit.getTrailerNbr()) == 0)
                    || (parentOnDock && handlingUnit.getCurrentDockLocTxt() != null 
                            && shmShipment.get().getCurrSicCd() != null && shmShipment.get().getCurrSicCd().equals(nonLoadedHandlingUnit.getRequestingSicCd())
                            && handlingUnit.getCurrentDockLocTxt().equals(nonLoadedHandlingUnit.getDockName()))
                    || (!parentOnDock && !parentOnTrailer && handlingUnit.getCurrentSicCd() != null 
                            && handlingUnit.getCurrentSicCd().equals(nonLoadedHandlingUnit.getRequestingSicCd()));

            // update current sic cd.
            updateShmHandlingUnit(
                handlingUnit,
                nonLoadedHandlingUnit.getTrailerNbr(),
                nonLoadedHandlingUnit.getRequestingSicCd(),
                mustSplitSiblings,
                shmHandlingUnitListForParentPro,
                nextMvmtSeqNbrMap,
                splitNonLoadedHandlingUnitsRqst.getSplitAuthorizedBy(),
                splitNonLoadedHandlingUnitsRqst.getSplitAuthorizedDateTime(),
                auditInfo,
                entityManager,
                txnContext);

       }
    }


    /**
     * updates current sic for given handling unit if the value has changed. <br/>
     * updates the split indicator to Y.
     *
     * @param long1
     * @param shmHandlingUnitListForParentPro
     * @param mustSplitSiblings
     */
    private void updateShmHandlingUnit(ShmHandlingUnit handlingUnit, Long trailerId, String requestingSicCd, boolean mustSplitSiblings, List<ShmHandlingUnit> shmHandlingUnitListForParentPro,
        Map<ShmHandlingUnit, Long> nextMvmtSeqNbrMap,
        String authorizedBy,
        XMLGregorianCalendar  authorizedDateTime,
        AuditInfo auditInfo,
        EntityManager entityManager,
        TransactionContext txnContext) throws ServiceException {

        if (!mustSplitSiblings) {
            DtoTransformer.setLstUpdateAuditInfo(handlingUnit, auditInfo);
            handlingUnit.setSplitInd(BasicTransformer.toString(true));

            shmHandlingUnitSubDAO
            .updateDB2ShmHandlingUnit(handlingUnit, handlingUnit.getLstUpdtTmst(), txnContext, db2EntityManager);

            long nextMvmtSeqNbr = getAndIncreaseMvmtSeqNbr(handlingUnit, nextMvmtSeqNbrMap);
            persistShipmentHandlingUnitMvmt(true, handlingUnit, nextMvmtSeqNbr, authorizedBy, authorizedDateTime,
                requestingSicCd, auditInfo, entityManager);

        } else {

            List<ShmHandlingUnit> siblingsList = shmHandlingUnitListForParentPro
                .stream()
                .filter(hu -> (!MISSING_MOVEMENT_CD.equals(hu.getHandlingMvmtCd()) 
                        || MovementStatusCd.FINAL_DLVD != MovementStatusCdTransformer.toEnum(hu.getMvmtStatCd()))
                    && !hu.getChildProNbrTxt().equals(handlingUnit.getChildProNbrTxt()))
                .collect(Collectors.toList());
            for (ShmHandlingUnit siblingShmHandlingUnit : siblingsList) {
                Boolean markAsSplit = false;

                if (BigDecimal.ZERO.compareTo(siblingShmHandlingUnit.getCurrentTrlrInstId()) != 0
                        && handlingUnit.getCurrentTrlrInstId().compareTo(siblingShmHandlingUnit.getCurrentTrlrInstId()) != 0) {
                    markAsSplit = true;
                }

                if (StringUtils.isNotBlank(siblingShmHandlingUnit.getCurrentDockLocTxt()) 
                        && !siblingShmHandlingUnit.getCurrentDockLocTxt().equals(handlingUnit.getCurrentDockLocTxt())) {
                    markAsSplit = true;
                }

                if (StringUtils.isNotBlank(siblingShmHandlingUnit.getCurrentSicCd()) 
                        && !requestingSicCd.equals(siblingShmHandlingUnit.getCurrentSicCd())) {
                    markAsSplit = true;
                }

                if (markAsSplit) {
                    if (!BasicTransformer.toBoolean(siblingShmHandlingUnit.getSplitInd())) {

                        DtoTransformer.setLstUpdateAuditInfo(siblingShmHandlingUnit, auditInfo);
                        siblingShmHandlingUnit.setSplitInd(BasicTransformer.toString(true));

                        shmHandlingUnitSubDAO.updateDB2ShmHandlingUnit(siblingShmHandlingUnit, siblingShmHandlingUnit.getLstUpdtTmst(), txnContext, db2EntityManager);

                        long nextMvmtSeqNbr = getAndIncreaseMvmtSeqNbr(siblingShmHandlingUnit, nextMvmtSeqNbrMap);
                        persistShipmentHandlingUnitMvmt(markAsSplit, siblingShmHandlingUnit, nextMvmtSeqNbr, authorizedBy, authorizedDateTime,
                            requestingSicCd, auditInfo, entityManager);
                    }
                } else {
                    if (BasicTransformer.toBoolean(siblingShmHandlingUnit.getSplitInd())) {

                        DtoTransformer.setLstUpdateAuditInfo(siblingShmHandlingUnit, auditInfo);
                        siblingShmHandlingUnit.setSplitInd(BasicTransformer.toString(false));

                        shmHandlingUnitSubDAO.updateDB2ShmHandlingUnit(siblingShmHandlingUnit, siblingShmHandlingUnit.getLstUpdtTmst(), txnContext, db2EntityManager);

                        long nextMvmtSeqNbr = getAndIncreaseMvmtSeqNbr(siblingShmHandlingUnit, nextMvmtSeqNbrMap);
                        persistShipmentHandlingUnitMvmt(markAsSplit, siblingShmHandlingUnit, nextMvmtSeqNbr, authorizedBy, authorizedDateTime,
                            requestingSicCd, auditInfo, entityManager);
                    }
                }
            }
        }
    }


    /**
     * if HU is already in the map, returns the value and increase the nbr in the map. <br/>
     * if HU is not in the map yet, finds the max, increases the nbr and store it in the map.
     *
     * @param handlingUnit
     * @param nextMvmtSeqNbrMap
     * @return
     */
    protected long getAndIncreaseMvmtSeqNbr(ShmHandlingUnit handlingUnit, Map<ShmHandlingUnit, Long> nextMvmtSeqNbrMap) {

        long nextMvmtSeqNbr;
        if (nextMvmtSeqNbrMap.containsKey(handlingUnit)) {
            nextMvmtSeqNbr = nextMvmtSeqNbrMap.get(handlingUnit) + 1;
            nextMvmtSeqNbrMap.put(handlingUnit, nextMvmtSeqNbr);
        } else {
            Optional<Long> mvmtSeqNbr = CollectionUtils
                .emptyIfNull(handlingUnit.getShmHandlingUnitMvmts())
                .stream()
                .map(mvmt -> (Long) mvmt.getId().getMvmtSeqNbr())
                .max(Long::compare);
            nextMvmtSeqNbr = mvmtSeqNbr.isPresent() ? mvmtSeqNbr.get() + 1L : 1L;
            nextMvmtSeqNbrMap.put(handlingUnit, nextMvmtSeqNbr);
        }

        return nextMvmtSeqNbr;
    }


    /**
     * if pro cannot be formatted, it returns the original value.
     */
    private String formatProToNineDigitHyphen(String pro, TransactionContext txnContext) {

        try {
            return ProNumberHelper.toNineDigitProHyphen(pro, txnContext);
        } catch (ValidationException e) {
            LOGGER.warn("Pro cannot be formatted: " + pro);
            return pro;
        }
    }


    protected void persistShipmentHandlingUnitMvmt(Boolean markAsSplit, ShmHandlingUnit shmHandlingUnit, long nextMvmSeqNbr,
        String authorizedBy, XMLGregorianCalendar authorizedDateTime, String requestingSicCd,
        AuditInfo auditInfo, EntityManager entityManager)
            throws ValidationException {

        ShmHandlingUnitMvmtPK id = new ShmHandlingUnitMvmtPK();
        id.setShpInstId(shmHandlingUnit.getId().getShpInstId());
        id.setSeqNbr(shmHandlingUnit.getId().getSeqNbr());
        id.setMvmtSeqNbr(nextMvmSeqNbr);

        ShmHandlingUnitMvmt shmHandlingUnitMvmt = new ShmHandlingUnitMvmt();
        DtoTransformer.setAuditInfo(shmHandlingUnitMvmt, auditInfo);
        shmHandlingUnitMvmt.setId(id);
        shmHandlingUnitMvmt.setShmHandlingUnit(shmHandlingUnit);
        shmHandlingUnitMvmt.setCrteUid(authorizedBy);
        shmHandlingUnitMvmt.setSplitAuthorizeBy(markAsSplit 
            ? authorizedBy
                : StringUtils.SPACE);
        Timestamp authorizedTimestamp = BasicTransformer
            .toTimestamp(authorizedDateTime);
        shmHandlingUnitMvmt.setSplitAuthorizeTmst(markAsSplit 
            ? authorizedTimestamp
                : DB2DefaultValueUtil.LOW_TMST);
        Timestamp currentTmst = new Timestamp(System.currentTimeMillis());
        shmHandlingUnitMvmt.setCrteTmst(currentTmst);
        shmHandlingUnitMvmt.setMvmtTmst(currentTmst);
        shmHandlingUnitMvmt
            .setMvmtTypCd(markAsSplit 
                ? HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.SPLIT)
                    : HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.UNSPLIT));
        shmHandlingUnitMvmt.setArchiveCntlCd(StringUtils.SPACE);
        shmHandlingUnitMvmt.setBypassScanInd(StringUtils.SPACE);
        shmHandlingUnitMvmt.setBypassScanReason(StringUtils.SPACE);
        shmHandlingUnitMvmt.setDmgdCatgCd(StringUtils.SPACE);
        shmHandlingUnitMvmt.setDockInstId(BigDecimal.ZERO);
        shmHandlingUnitMvmt.setExcpTypCd(StringUtils.SPACE);
        shmHandlingUnitMvmt.setMvmtRptgSicCd(StringUtils.isNotBlank(requestingSicCd) ? requestingSicCd : StringUtils.SPACE);
        shmHandlingUnitMvmt.setRfsdRsnCd(StringUtils.SPACE);
        shmHandlingUnitMvmt.setRmrkTxt(StringUtils.SPACE);
        shmHandlingUnitMvmt.setScanTmst(DB2DefaultValueUtil.LOW_TMST);
        shmHandlingUnitMvmt.setTrlrInstId(BigDecimal.ZERO);
        shmHandlingUnitMvmt.setUndlvdRsnCd(StringUtils.SPACE);

        shmHandlingUnitMvmtSubDAO.save(shmHandlingUnitMvmt, entityManager);
        shmHandlingUnitMvmtSubDAO.createDB2ShmHandlingUnitMvmt(shmHandlingUnitMvmt, db2EntityManager);
    }

    /**
     * <p>
     * finds the difference between input child pros contained in {@code NonLoadedHandlingUnit} and list of
     * {@code ShmHandlingUnit} passed by params.
     * </p>
     * <p>
     * if there is any difference, a new message will be added to the <code>mapLocationAndErrorMsgs</code> list passed
     * by
     * param.
     * </p>
     *
     * @param nonLoadedHandlingUnit
     * @param shmHandlingUnitsForAParentProDB
     * @param mapLocationAndErrorMsgs
     * @throws ValidationException
     */
    private void validateChildPro(NonLoadedHandlingUnit nonLoadedHandlingUnit,
        Map<String, List<ShmHandlingUnit>> parentProAndShmHUsMapFromDB, String requestingSic,
        LinkedHashMap<NonLoadedHandlingUnit, List<String>> errorMsgsMap,
        TransactionContext txnContext) throws ValidationException {

        List<ShmHandlingUnit> shmHandlingUnitsForAParentProDB = ListUtils
            .emptyIfNull(parentProAndShmHUsMapFromDB.get(nonLoadedHandlingUnit.getParentProNbr()));

        boolean childProExistsInDB = shmHandlingUnitsForAParentProDB
            .stream()
            .anyMatch(hu -> hu.getChildProNbrTxt().equals(nonLoadedHandlingUnit.getChildProNbr()));

        if (!childProExistsInDB) {
            String msg = String
                .format("Child PRO %s is not associated to Parent PRO %s.",
                    ProNumberHelper.toTenDigitPro(nonLoadedHandlingUnit.getChildProNbr()),
                    formatProToNineDigitHyphen(nonLoadedHandlingUnit.getParentProNbr(), txnContext));
            addErrorMessageToMap(nonLoadedHandlingUnit, msg, errorMsgsMap);
        }


        long countDifferentSicCd = shmHandlingUnitsForAParentProDB
            .stream()
            .map(hu -> StringUtils.trimToEmpty(hu.getCurrentSicCd()))
            .distinct()
            .count();

        boolean isAnyOnTrailer = shmHandlingUnitsForAParentProDB.stream().anyMatch(hu -> BigDecimal.ZERO.compareTo(hu.getCurrentTrlrInstId()) < 0);

        if (!isAnyOnTrailer && shmHandlingUnitsForAParentProDB.size() > 1 && countDifferentSicCd <= 1) {
            String msg = String
                .format("All Child PRO are at the same Sic or in dock location for Parent PRO %s.",
                    formatProToNineDigitHyphen(nonLoadedHandlingUnit.getParentProNbr(), txnContext));
            addErrorMessageToMap(nonLoadedHandlingUnit, msg, errorMsgsMap);
        }

    }

    /**
     * validates that the request has split auth by and date time.<br/>
     * validates that the request has at least 1 NonLoadedHandlingUnit.
     * validates the information for each NonLoadedHandlingUnit. if any validation error is found, that element is
     * removed from the request.
     * All validation errors are added to errorMsgMap.
     */
    protected void validateRequest(SplitNonLoadedHandlingUnitsRqst request,
        LinkedHashMap<NonLoadedHandlingUnit, List<String>> errorMsgsMap,
        TransactionContext txnContext)
            throws ValidationException {

        if (StringUtils.isBlank(request.getSplitAuthorizedBy())) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo("N/A", "Split authorized by param is required.")
                .log()
                .build();
        }

        if (request.getSplitAuthorizedDateTime() == null) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo("N/A", "Split authorized date time param is required.")
                .log()
                .build();
        }

        if (CollectionUtils.isEmpty(request.getNonLoadedHandlingUnits())
                || request.getNonLoadedHandlingUnits().stream().allMatch(hu -> hu.getParentProNbr() == null)) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo("N/A", "At least one NonLoadedHandlingUnit is required.")
                .log()
                .build();
        }

        Iterator<NonLoadedHandlingUnit> it = request.getNonLoadedHandlingUnits().iterator();
        while (it.hasNext()) {
            NonLoadedHandlingUnit nonLoadedHandlingUnit = it.next();

            if (StringUtils.isBlank(nonLoadedHandlingUnit.getParentProNbr())) {
                String msg = StringUtils.isBlank(nonLoadedHandlingUnit.getChildProNbr()) ? "Parent PRO is required." :
                    String
                        .format("Parent PRO is required for input child PRO %s.",
                            ProNumberHelper.toTenDigitPro(nonLoadedHandlingUnit.getChildProNbr()));

                addErrorMessageToMap(nonLoadedHandlingUnit, msg, errorMsgsMap);
                it.remove();
            } else {
                try {
                    nonLoadedHandlingUnit
                        .setParentProNbr(
                            ProNumberHelper.validateProNumber(nonLoadedHandlingUnit.getParentProNbr(), txnContext));

                    if (StringUtils.isBlank(nonLoadedHandlingUnit.getChildProNbr())) {
                        addErrorMessageToMap(nonLoadedHandlingUnit, "Child PRO is required.", errorMsgsMap);
                        it.remove();
                        continue;
                    } else {
                        try {
                            nonLoadedHandlingUnit
                                .setChildProNbr(ProNumberHelper
                                    .validateProNumber(nonLoadedHandlingUnit.getChildProNbr(), txnContext));
                        } catch (ServiceException se) {
                            addErrorMessageToMap(nonLoadedHandlingUnit,
                                String.format("Child PRO %s is invalid.", nonLoadedHandlingUnit.getChildProNbr()),
                                errorMsgsMap);
                            it.remove();
                            continue;
                        }
                    }
                } catch (ServiceException se) {
                    addErrorMessageToMap(nonLoadedHandlingUnit, "Parent PRO is invalid.", errorMsgsMap);
                    it.remove();
                    continue;
                }

            }

        }
    }

    /**
     * it finds the {@link NonLoadedHandlingUnit} key in the {@link LinkedHashMap locationAndErrorMsgsMap}.
     * If there is no key, a new one will be added and will be associated to <code>newMessage</code> as a value.<br/>
     * If there the key exists in the map, the <code>newMessage</code> will be appended to the current value.
     *
     * @param nonLoadedHandlingUnitKey
     * @param newMessage
     * @param errorMsgsMap
     */
    private void addErrorMessageToMap(NonLoadedHandlingUnit nonLoadedHandlingUnitKey, String newMessage,
        LinkedHashMap<NonLoadedHandlingUnit, List<String>> errorMsgsMap) {
        List<String> errors = errorMsgsMap.getOrDefault(nonLoadedHandlingUnitKey, new ArrayList<String>());
        errors.add(newMessage);
        errorMsgsMap.put(nonLoadedHandlingUnitKey, errors);
    }

}
