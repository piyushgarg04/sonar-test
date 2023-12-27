package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.location.v2.GetHostSicDetailsResp;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.util.DateTimeHelper;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmt;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmtPK;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitPK;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.service.entity.ShmXdockExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmXdockExcpPK;
import com.xpo.ltl.api.shipment.transformer.v2.DeliveryQualifierCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.HandlingUnitMovementTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementExceptionTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.DeliveryQualifierCd;
import com.xpo.ltl.api.shipment.v2.EquipmentId;
import com.xpo.ltl.api.shipment.v2.EventLogSubTypeCd;
import com.xpo.ltl.api.shipment.v2.EventLogTypeCd;
import com.xpo.ltl.api.shipment.v2.HandlingUnit;
import com.xpo.ltl.api.shipment.v2.HandlingUnitMovement;
import com.xpo.ltl.api.shipment.v2.HandlingUnitMovementTypeCd;
import com.xpo.ltl.api.shipment.v2.HandlingUnitsWithParentQualifier;
import com.xpo.ltl.api.shipment.v2.MovementExceptionTypeCd;
import com.xpo.ltl.api.shipment.v2.MovementStatusCd;
import com.xpo.ltl.api.shipment.v2.ShipmentMovementTypeCd;
import com.xpo.ltl.api.shipment.v2.UpdateHandlingUnitsResp;
import com.xpo.ltl.api.shipment.v2.UpdateHandlingUnitsRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitMvmtSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentEagerLoadPlan;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmXdockExcpSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmEventDelegate;
import com.xpo.ltl.shipment.service.delegates.ShmHandlingUnitDelegate;
import com.xpo.ltl.shipment.service.delegates.ShmMovementDelegate;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.DB2DefaultValueUtil;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;

@ApplicationScoped
@LogExecutionTime
public class UpdateHandlingUnitsImpl {

    private static final String DEFAULT_DOCK_TRAILER_PREFIX = "999";
    private static final long DEFAULT_DOCK_TRAILER_SUFFIX = 9999L;
    private static final String DEFAULT_DOCK_LD_RLSE_NBR = "99999999";
    private static final Logger LOGGER = LogManager.getLogger(UpdateHandlingUnitsImpl.class);
    private static final String HU_MVMT_CD_MISSING = "MISSING";
    private static final String HU_MVMT_CD_NORMAL = "NORMAL";
    private static final String PGM_ID = "UPDATEHU";
    private static final String TRAN_ID = "UPHU";
    private static final List<String> HU_MVMT_CDS = Arrays.asList(HU_MVMT_CD_NORMAL, HU_MVMT_CD_MISSING);
    private static final List<MovementExceptionTypeCd> HU_EXCP_TYP_CDS = Arrays
        .asList(MovementExceptionTypeCd.REFUSED, MovementExceptionTypeCd.DAMAGED, MovementExceptionTypeCd.UNDELIVERABLE,
            MovementExceptionTypeCd.EXCP_NOTED);
    private static final String FINAL_MOVEMENT_STATUS_CD = MovementStatusCdTransformer.toCode(MovementStatusCd.FINAL_DLVD);

	@PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
	private EntityManager db2EntityManager;

	@Inject
    private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

    @Inject
    private ShmHandlingUnitMvmtSubDAO shmHandlingUnitMvmtSubDAO;

    @Inject
    private ShmShipmentSubDAO shmShipmentSubDAO;

    @Inject
    private ShmXdockExcpSubDAO shmXdockExcpSubDAO;

    @Inject
    private ExternalRestClient externalRestClient;

    @Inject
    private ShmHandlingUnitDelegate shmHandlingUnitDelegate;

    @Inject
    private ShmEventDelegate shmEventDelegate;

    @Inject
    private ShmMovementDelegate shmMovementDelegate;

    /**
     * Allows to update a list of related handling units by parent PRO.
     * Updates such as:
     * Set a handling unit back to Normal when it is previously short
     * Set a handling unit to Missing when current location is unknown, also known as a short exception
     * Set a handling unit exception such as damage
     *
     * @param updateHandlingUnitRqst
     * @param entityManager
     * @param txnContext
     * @return
     * @throws ServiceException
     */
    public UpdateHandlingUnitsResp updateHandlingUnits(
        UpdateHandlingUnitsRqst updateHandlingUnitRqst, EntityManager entityManager,
        TransactionContext txnContext)
			throws ServiceException {

        checkNotNull(updateHandlingUnitRqst, "updateHandlingUnitRqst Request is required.");
        checkNotNull(txnContext, "The TransactionContext is required.");
        checkNotNull(entityManager, "The EntityManager is required.");

        UpdateHandlingUnitsResp resp = new UpdateHandlingUnitsResp();

        LinkedHashMap<String, List<String>> errorMsgsMap = new LinkedHashMap<>();

        validateRequest(updateHandlingUnitRqst, errorMsgsMap, txnContext);

        Map<String, HandlingUnit> reqHuMap = updateHandlingUnitRqst
            .getHandlingUnits()
            .stream()
            .filter(hu -> StringUtils.isNotBlank(hu.getChildProNbr()) && !errorMsgsMap.containsKey(hu.getChildProNbr()))
            .collect(Collectors.toMap(HandlingUnit::getChildProNbr, hu -> hu));

        //expecting to get a single parent pro as we validate this request
        List<String> parentPros = updateHandlingUnitRqst
                .getHandlingUnits()
                .stream()
                .filter(hu -> StringUtils.isNotBlank(hu.getParentProNbr()) && !errorMsgsMap.containsKey(hu.getParentProNbr()))
                .map(HandlingUnit::getParentProNbr)
                .distinct()
                .collect(Collectors.toList());

        List<ShmHandlingUnit> allShmHandlingUnitsDB = shmHandlingUnitSubDAO
                .findByParentProNumberList(parentPros, entityManager);
        int childProCount = allShmHandlingUnitsDB.size();

        List<String> childPros = Lists.newArrayList(reqHuMap.keySet());
        List<ShmHandlingUnit> shmHandlingUnitsDB = allShmHandlingUnitsDB
                .stream()
                .filter(hu -> childPros.contains(hu.getChildProNbrTxt()))
                .collect(Collectors.toList());

        validateChildProsAgaisntDB(reqHuMap, shmHandlingUnitsDB, updateHandlingUnitRqst.getCurrentTrailerInstanceId(), errorMsgsMap, txnContext);

        throwExceptionIfAnyValidationError(errorMsgsMap, txnContext);

        List<ShmHandlingUnit> allOtherSibling =  allShmHandlingUnitsDB
                .stream()
                .filter(hu -> !childPros.contains(hu.getChildProNbrTxt()))
                .collect(Collectors.toList());

        boolean anySplitSibling =  CollectionUtils.emptyIfNull(allOtherSibling)
                .stream()
            .anyMatch(hu -> !HU_MVMT_CD_MISSING.equalsIgnoreCase(hu.getHandlingMvmtCd()) && hu.getCurrentSicCd() != null && updateHandlingUnitRqst.getRequestingSicCd() != null
                    && !hu.getCurrentSicCd().equalsIgnoreCase(updateHandlingUnitRqst.getRequestingSicCd())
                    && BasicTransformer.toBoolean(hu.getSplitInd()));

//        boolean changeAllToNormal =
        updateHandlingUnitRqst
                .getHandlingUnits()
                .stream()
                .forEach(hu ->  {

                Optional<ShmHandlingUnit> huOnDB = allShmHandlingUnitsDB.stream().filter(huDB -> huDB.getChildProNbrTxt().equals(hu.getChildProNbr())).findAny();

                if (huOnDB.isPresent() && HU_MVMT_CD_MISSING.equalsIgnoreCase(huOnDB.get().getHandlingMvmtCd()) && HU_MVMT_CD_NORMAL.equalsIgnoreCase(hu.getHandlingMovementCd())) {
                    //this only happens on excp other than missing, do not attempt to split to force user to split it
                            hu.setSplitInd(false);
                    }
                });
//                .allMatch(hu -> hu.getHandlingMovementCd().equalsIgnoreCase(HU_MVMT_CD_NORMAL));

        persistAndUpdateEntities(shmHandlingUnitsDB, allShmHandlingUnitsDB, childProCount, reqHuMap,
            resp, updateHandlingUnitRqst.getRequestingSicCd(), updateHandlingUnitRqst.getUserId(),
            updateHandlingUnitRqst, allOtherSibling, txnContext,
            entityManager);

        return resp;
    }

    private void persistAndUpdateEntities(List<ShmHandlingUnit> shmHandlingUnitsDB, List<ShmHandlingUnit> allShmHandlingUnitsDB, int childProCount, Map<String, HandlingUnit> reqHuMap,
        UpdateHandlingUnitsResp resp, String requestingSicCd, String userId,
        UpdateHandlingUnitsRqst updateHandlingUnitsRqst, List<ShmHandlingUnit> allOtherSiblings, TransactionContext txnContext,
        EntityManager entityManager)
            throws ServiceException {

        AuditInfo auditInfo = AuditInfoHelper.getAuditInfoWithPgmAndUserId(PGM_ID, userId, txnContext);

        long shipmentInstId = allShmHandlingUnitsDB.get(0).getId().getShpInstId();

        ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan =
            new ShmShipmentEagerLoadPlan();
        shmShipmentEagerLoadPlan.setShmAsEntdCusts(true);

        ShmShipment shmShipment =
            shmShipmentSubDAO.findByProOrShipmentId
                (null,
                 null,
                 shipmentInstId,
                 false,
                 shmShipmentEagerLoadPlan,
                 entityManager);

        Pair<Boolean, ShmXdockExcp> resulteXdockException = createOrUpdateXdockException(shmShipment, childProCount, reqHuMap, requestingSicCd, userId,
            updateHandlingUnitsRqst, auditInfo, txnContext,
            entityManager);

        boolean anyFinal = CollectionUtils.emptyIfNull(allShmHandlingUnitsDB).stream().anyMatch(hu -> FINAL_MOVEMENT_STATUS_CD.equals(hu.getMvmtStatCd()));
        boolean allShort = resulteXdockException.getLeft() && !anyFinal;

        long lastEventSeqUsed = processWhenParentIsUnload(allShort, updateHandlingUnitsRqst, shmShipment, requestingSicCd, auditInfo, entityManager);

        lastEventSeqUsed = processWhenAllShort(allShort, updateHandlingUnitsRqst, shmShipment, requestingSicCd, shmHandlingUnitsDB, lastEventSeqUsed,
            auditInfo,
            entityManager,
            txnContext);

        String handlingUnitPartialInd = shmHandlingUnitDelegate
            .calculateHUPartialInd(Lists.newArrayList(reqHuMap.values()), shmHandlingUnitDelegate.buildChildProHUMap(allShmHandlingUnitsDB));

        boolean handlingUnitSplitInd = shmHandlingUnitDelegate
                .isShipmentSplit(Lists.newArrayList(reqHuMap.values()), allShmHandlingUnitsDB);

        List<ShmHandlingUnitPK> shmHandlingUnitPKList = ListUtils
                .emptyIfNull(shmHandlingUnitsDB)
                .stream()
                .map(hu -> hu.getId())
                .collect(Collectors.toList());
            List<ShmHandlingUnitMvmtPK> maxMvmtSeqNbrsList = shmHandlingUnitMvmtSubDAO
                .getMaxMvmtPKByShpInstIdAndSeqNbrPairs(shmHandlingUnitPKList, entityManager);

            updateOtherSiblingsFieldsWhenAllShort(allOtherSiblings, shmShipment, allShort, auditInfo, entityManager, txnContext);

        boolean anyPoorlyPackaged = false;
        boolean checkSplit = false;

        List<HandlingUnitsWithParentQualifier> huWithQualifierList = Lists.newArrayList();
        for (ShmHandlingUnit huDB : shmHandlingUnitsDB) {
            HandlingUnit hu = reqHuMap.get(huDB.getChildProNbrTxt());

            String remarks = null;
            MovementExceptionTypeCd mvmtExcpTypCd = null;
            if (CollectionUtils.isNotEmpty(hu.getHandlingUnitMovement())) {
                HandlingUnitMovement firstHUMvmt = hu.getHandlingUnitMovement().get(0);

                mvmtExcpTypCd = MovementExceptionTypeCdTransformer
                    .toEnum(firstHUMvmt.getExceptionTypeCd());

                if (StringUtils.isNotBlank(firstHUMvmt.getRemarks())) {
                    remarks = firstHUMvmt.getRemarks();
                }
            }

            Long nextMaxMvmtSeqNbr = maxMvmtSeqNbrsList
                    .stream()
                    .filter(huMvmtPK -> huMvmtPK.getShpInstId() == huDB.getId().getShpInstId()
                        && huMvmtPK.getSeqNbr() == huDB.getId().getSeqNbr())
                    .map(pk -> pk.getMvmtSeqNbr() + 1)
                    .findFirst()
                    .orElse(1L);

            if (HU_MVMT_CD_MISSING.equals(hu.getHandlingMovementCd())) {
                mvmtExcpTypCd = MovementExceptionTypeCd.SHORT;
            } else if ((mvmtExcpTypCd == null && StringUtils.isNotBlank(remarks)) ||
                    (mvmtExcpTypCd != null && !HU_EXCP_TYP_CDS.contains(mvmtExcpTypCd))) {
                mvmtExcpTypCd = MovementExceptionTypeCd.EXCP_NOTED;
            }

            ShmHandlingUnitMvmt shmHandlingUnitMvmt = null;
            if (mvmtExcpTypCd != null) {
                shmHandlingUnitMvmt = persistShmHandlingUnitMvmt(huDB, nextMaxMvmtSeqNbr, requestingSicCd, userId, remarks,
                    mvmtExcpTypCd, updateHandlingUnitsRqst.getDockInstanceId(),
                    updateHandlingUnitsRqst.getCurrentTrailerInstanceId(), auditInfo, entityManager);
            }

            // This cannot be switched back to false
            if (!BooleanUtils.toBoolean(huDB.getPoorlyPackagedInd()) && hu.getPoorlyPackagedInd() != null) {
                huDB.setPoorlyPackagedInd(BasicTransformer.toString(hu.getPoorlyPackagedInd()));
            }
            if (BooleanUtils.toBoolean(huDB.getPoorlyPackagedInd())) {
                anyPoorlyPackaged = true;
            }

            Boolean isSplitHUMvmt = HU_MVMT_CD_MISSING.equalsIgnoreCase(huDB.getHandlingMvmtCd()) && HU_MVMT_CD_NORMAL.equalsIgnoreCase(hu.getHandlingMovementCd()) ? hu.getSplitInd() : null;
            checkSplit = (HU_MVMT_CD_MISSING.equalsIgnoreCase(huDB.getHandlingMvmtCd()) && HU_MVMT_CD_NORMAL.equalsIgnoreCase(hu.getHandlingMovementCd())) || checkSplit;

            shmHandlingUnitDelegate
                .updateShmHandlingUnit(huDB, hu, updateHandlingUnitsRqst.getDockName(), updateHandlingUnitsRqst.getCurrentTrailerInstanceId(),
                    requestingSicCd, isSplitHUMvmt, shmShipment.getMvmtStatCd(), allShort, auditInfo,
                entityManager, txnContext);

            HandlingUnitsWithParentQualifier huWithQualifier = new HandlingUnitsWithParentQualifier();
            huWithQualifier.setChildPro(huDB.getChildProNbrTxt());
            huWithQualifier.setParentPro(huDB.getParentProNbrTxt());
            huWithQualifier.setDeliveryQualifierCd(allShort ? DeliveryQualifierCd.ALL_SHORT : null);
            if (resulteXdockException.getRight() != null) {
                huWithQualifier.setExceptionSequenceNbr(BasicTransformer.toBigInteger(resulteXdockException.getRight().getId().getSeqNbr()));
            } else {
                huWithQualifier.setExceptionSequenceNbr(BigInteger.ZERO);
            }
            if (shmHandlingUnitMvmt != null) {
                huWithQualifier.setMovementSequenceNbr(BasicTransformer.toBigInteger(shmHandlingUnitMvmt.getId().getMvmtSeqNbr()));
            }
            huWithQualifierList.add(huWithQualifier);
        }
        resp.setHandlingUnitsWithParentQualifier(huWithQualifierList);

        if (anyPoorlyPackaged && !BooleanUtils.toBoolean(shmShipment.getPoorlyPackagedInd())) {
            shmShipment.setPoorlyPackagedInd(BasicTransformer.toString(true));
        }
        DtoTransformer.setLstUpdateAuditInfo(shmShipment, auditInfo);
        boolean mustCreateSplitEvent = false;
        boolean mustCreateMissingEvent = false;
        if (!BasicTransformer.toBoolean(shmShipment.getHandlingUnitSplitInd()) && handlingUnitSplitInd) {
            mustCreateSplitEvent = true;
        }
        if (!BasicTransformer.toBoolean(shmShipment.getHandlingUnitPartialInd()) && BooleanUtils.isTrue(BasicTransformer.toBoolean(handlingUnitPartialInd))) {
            mustCreateMissingEvent = true;
        }
        if(!BasicTransformer.toString(handlingUnitSplitInd).equalsIgnoreCase(StringUtils.trimToEmpty(shmShipment.getHandlingUnitSplitInd()))) {
            shmShipment.setHandlingUnitSplitInd(BasicTransformer.toString(handlingUnitSplitInd));
        }
        if(!StringUtils.trimToEmpty(handlingUnitPartialInd).equalsIgnoreCase(StringUtils.trimToEmpty(shmShipment.getHandlingUnitPartialInd()))) {
            shmShipment.setHandlingUnitPartialInd(handlingUnitPartialInd);
        }
        
        shmShipmentSubDAO.save(shmShipment, entityManager);
        shmShipmentSubDAO.updateDb2ShmShipmentForUpdHU(shmShipment, db2EntityManager);

        if (mustCreateSplitEvent) {
            lastEventSeqUsed = lastEventSeqUsed == 0 ? 0 : ++lastEventSeqUsed;
            lastEventSeqUsed = shmEventDelegate
                .createEvent(lastEventSeqUsed, EventLogTypeCd.SHIPMENT_UPDATE, EventLogSubTypeCd.SPLIT, shmShipment, null, requestingSicCd,
                    Optional.empty(), TRAN_ID, entityManager, auditInfo);
        }
        if (mustCreateMissingEvent) {
            lastEventSeqUsed = lastEventSeqUsed == 0 ? 0 : ++lastEventSeqUsed;
            lastEventSeqUsed = shmEventDelegate
                .createEvent(lastEventSeqUsed, EventLogTypeCd.SHIPMENT_UPDATE, EventLogSubTypeCd.MISSING, shmShipment, null, requestingSicCd,
                    Optional.empty(), TRAN_ID, entityManager, auditInfo);
        }

    }

    /**
     * if the <code>parentUnloadInd</code> field in the request is TRUE:
     *
     * <ul>
     * <li>updates shipment mvmt status variable to ON_DOCK</li>
     * <li>creates a new UNLOAD shm mvmt.</li>
     * <li>creates a new 9/3A event.</li>
     * </ul>
     */
    private long processWhenParentIsUnload(boolean allShort, UpdateHandlingUnitsRqst updateHandlingUnitsRqst, ShmShipment shmShipment,
        String requestingSicCd,
        AuditInfo auditInfo, EntityManager entityManager) throws ServiceException {

        if (allShort || BooleanUtils.isNotTrue(updateHandlingUnitsRqst.getParentUnloadInd())) {
            return 0;
        }

        Optional<EquipmentId> equipmentIdOpt = buildEquipmentIdOpt(updateHandlingUnitsRqst);

        // update shipment mvmt status
        updateShmShimpentFields(
                shmShipment,
                requestingSicCd,
                MovementStatusCdTransformer.toEnum(shmShipment.getMvmtStatCd())
                        .equals(MovementStatusCd.FINAL_DLVD) ? null : MovementStatusCd.ON_DOCK,
                Optional.empty());

        ShmMovement shmMovement = shmMovementDelegate
            .createShmMvmt(equipmentIdOpt, requestingSicCd, false, ShipmentMovementTypeCd.UNLOAD, Optional.empty(), shmShipment.getShpInstId(),
                auditInfo, entityManager);

        long lastEventSeqUsed = shmEventDelegate
            .createEvent(0L, EventLogTypeCd.STRIP, EventLogSubTypeCd.UNLOAD_BY_PRO, shmShipment, shmMovement, requestingSicCd, equipmentIdOpt,
                TRAN_ID,
                entityManager, auditInfo);

        return lastEventSeqUsed;
    }

    private void updateOtherSiblingsFieldsWhenAllShort(List<ShmHandlingUnit> allOtherSiblings, ShmShipment shmShipment, boolean allShort, AuditInfo auditInfo,
        EntityManager entityManager, TransactionContext txnContext) throws ValidationException, ServiceException {

        if (!allShort) {
            return;
        }

        for (ShmHandlingUnit otherSibling : allOtherSiblings) {
            otherSibling.setMvmtStatCd(shmShipment.getMvmtStatCd());
            DtoTransformer.setLstUpdateAuditInfo(otherSibling, auditInfo);
            shmHandlingUnitSubDAO.save(otherSibling, entityManager);
            shmHandlingUnitSubDAO.updateDB2ShmHandlingUnit(otherSibling, otherSibling.getLstUpdtTmst(), txnContext, db2EntityManager);
        }
    }

    /**
     * <ul>
     * When dealing with PLT PROs, the following needs to happen when a dock exception is taken (short or damage on child
     * PROs):
     * <li>
     * When all short on final destination (including satellite) (and not on a delivery/planning route)
     * <ul>
     * <li>Shipment dlvyQalfr of J, currentSic update to requestingSic, mvmtStat of 4
     * </li>
     * <li>Create movement (type 7 - delivery) and movement exception on parent
     * </li>
     * <li>Create Event logs B/42 and B/FJ</li>
     * </li>
     * </ul>
     * <li>
     * When all short on final destination (including satellite) and was on a delivery route
     * <ul>
     * <li>Shipment currentSic update to requestingSic, mvmtStat of 1
     * </li>
     * <li>Create movement (type 4 - unload) and movement exception on parent
     * </li>
     * <li>Create Event logs 9/3A (if last known location is on a trailer and current mvmtStat 2))</li>
     * </ul>
     * </li>
     * <li>
     * When all short on final destination (including satellite) and was on a planning route
     * <ul>
     * <li>Shipment dlvyQalfr of J, currentSic update to requestingSic, mvmtStat of 4
     * </li>
     * <li>Create movement (type 7 - delivery) and movement exception on parent
     * </li>
     * <li>Create Event logs B/42 and B/FJ</li>
     * </ul>
     * </li>
     * <li>
     * When all short on a different location than dest SIC (including satellite)
     * <ul>
     * <li>Shipment currentSic update to requestingSic, mvmtStat of 1
     * </li>
     * <li>Create movement (type 4 - unload) and movement exception on parent
     * </li>
     * <li>Create Event logs 9/3A (if last known location is on a trailer and current mvmtStat 2)</li>
     * </ul>
     * </li>
     * </ul>
     */
    private long processWhenAllShort(boolean allShort, UpdateHandlingUnitsRqst updateHandlingUnitsRqst, ShmShipment shmShipment,
        String requestingSicCd, List<ShmHandlingUnit> shmHandlingUnitsDB, long lastEventSeqUsed, AuditInfo auditInfo, EntityManager entityManager,
        TransactionContext txnContext) throws ServiceException {

        if (!allShort) {
            return lastEventSeqUsed;
        }

        Boolean isOnDeliveryRoute = BooleanUtils.isTrue(updateHandlingUnitsRqst.getOnDeliveryRouteInd());
        Boolean isOnPlanningRoute = BooleanUtils.isTrue(updateHandlingUnitsRqst.getOnPlanningRouteInd());
        boolean isOnFinalDest = isReportingSicDestination(shmShipment.getDestTrmnlSicCd(), requestingSicCd, txnContext);
        boolean isOnFinalDestAndNotDlvryNorPlanning = isOnFinalDest && !isOnDeliveryRoute && !isOnPlanningRoute;
        boolean isOnFinalDestAndPlanning = isOnFinalDest && !isOnDeliveryRoute && isOnPlanningRoute;
        boolean isOnFinalDestAndDlvry = isOnFinalDest && isOnDeliveryRoute && !isOnPlanningRoute;
        MovementStatusCd currentMovementStatusCd = MovementStatusCdTransformer.toEnum(shmShipment.getMvmtStatCd());

        Optional<EquipmentId> equipmentIdOpt = buildEquipmentIdOpt(updateHandlingUnitsRqst);

        if ((isOnFinalDestAndNotDlvryNorPlanning || isOnFinalDestAndPlanning)) {
            lastEventSeqUsed = lastEventSeqUsed == 0 ? 0 : ++lastEventSeqUsed;
            updateShmShimpentFields(shmShipment, requestingSicCd, MovementStatusCd.INTERIM_DLVRY, Optional.of(DeliveryQualifierCd.ALL_SHORT));
            ShmMovement shmMovement = shmMovementDelegate
                .createShmMvmt(equipmentIdOpt, requestingSicCd, false, ShipmentMovementTypeCd.DELIVERY, Optional.of(DeliveryQualifierCd.ALL_SHORT),
                    shmShipment.getShpInstId(), auditInfo, entityManager);
            lastEventSeqUsed = shmEventDelegate
                .createEvent(lastEventSeqUsed, EventLogTypeCd.SHIPMENT_DLVY, EventLogSubTypeCd.UPDATE_DLVY, shmShipment, shmMovement,
                    requestingSicCd, Optional.empty(), TRAN_ID, entityManager, auditInfo);
            lastEventSeqUsed = shmEventDelegate
                .createEvent(++lastEventSeqUsed, EventLogTypeCd.SHIPMENT_DLVY, EventLogSubTypeCd.ALL_SHORT_DLVY, shmShipment, shmMovement,
                    requestingSicCd, Optional.empty(), TRAN_ID, entityManager, auditInfo);
        } else if (!isOnFinalDest || isOnFinalDestAndDlvry) {
            updateShmShimpentFields(shmShipment, requestingSicCd, MovementStatusCd.ON_DOCK, Optional.empty());
            if (MovementStatusCd.ON_TRAILER == currentMovementStatusCd) {
                lastEventSeqUsed = lastEventSeqUsed == 0 ? 0 : ++lastEventSeqUsed;
                ShmMovement shmMovement = shmMovementDelegate
                        .createShmMvmt(equipmentIdOpt, requestingSicCd, false, ShipmentMovementTypeCd.UNLOAD, Optional.empty(), shmShipment.getShpInstId(),
                            auditInfo, entityManager);
                lastEventSeqUsed = shmEventDelegate
                    .createEvent(lastEventSeqUsed, EventLogTypeCd.STRIP, EventLogSubTypeCd.UNLOAD_BY_PRO, shmShipment, shmMovement, requestingSicCd,
                        equipmentIdOpt, TRAN_ID, entityManager, auditInfo);
            }
        }

        return lastEventSeqUsed;
    }

    private Optional<EquipmentId> buildEquipmentIdOpt(UpdateHandlingUnitsRqst updateHandlingUnitsRqst) {
        Optional<EquipmentId> equipmentIdOpt = Optional.empty();
        EquipmentId equipmentId = new EquipmentId();
        boolean eqpAvailable = false;
        if (StringUtils.isBlank(updateHandlingUnitsRqst.getDockName())) {
            if (updateHandlingUnitsRqst.getCurrentTrailerInstanceId() != null) {
                equipmentId.setEquipmentInstId(BasicTransformer.toString(updateHandlingUnitsRqst.getCurrentTrailerInstanceId()));
                eqpAvailable = true;
            }
            if (updateHandlingUnitsRqst.getTrailerIdPrefix() != null && updateHandlingUnitsRqst.getTrailerIdSuffixNbr() != null) {
                equipmentId.setEquipmentPrefix(updateHandlingUnitsRqst.getTrailerIdPrefix());
                equipmentId.setEquipmentSuffix(BasicTransformer.toString(updateHandlingUnitsRqst.getTrailerIdSuffixNbr()));
                eqpAvailable = true;
            }
        }
        if (eqpAvailable) {
            equipmentIdOpt = Optional.of(equipmentId);
        }
        return equipmentIdOpt;
    }

    private void updateShmShimpentFields(ShmShipment shmShipment, String requestingSicCd, MovementStatusCd mvmtStatusCd,
            Optional<DeliveryQualifierCd> dlvryQfrCd) {

        shmShipment.setCurrSicCd(requestingSicCd);
        if (mvmtStatusCd != null) {
            shmShipment.setMvmtStatCd(MovementStatusCdTransformer.toCode(mvmtStatusCd));
        }
        if (dlvryQfrCd.isPresent()) {
            shmShipment.setDlvryQalfrCd(DeliveryQualifierCdTransformer.toCode(dlvryQfrCd.get()));
        }
    }

    /**
     * <ol>
     * <li>if there are no {@link MovementExceptionTypeCd#SHORT} or {@link MovementExceptionTypeCd#DAMAGED} exceptions
     * in the request, do nothing.</li>
     * <li>only for those childPors that have excp = {@link MovementExceptionTypeCd#SHORT} or
     * {@link MovementExceptionTypeCd#DAMAGED}.
     * </li>
     * <li>group allChildPros in the request by currentSic and filter by the requesingSic received in the req.
     * </li>
     * <li>find all {@link ShmXdockExcp} for that shipment in DB,
     * </li>
     * <li>if the latest {@link ShmXdockExcp} is for the same reportingSicCd that the one in the request and the user
     * that has
     * created the latest {@link ShmXdockExcp} is the same that the one in the request, and it hasn't been passed 2
     * hours from the
     * creation of the latest {@link ShmXdockExcp}, it will be updated. Otherwise, a new one is created.
     * </li>
     * </ol>
     */
    private Pair<Boolean, ShmXdockExcp> createOrUpdateXdockException(ShmShipment shmShipment,
        int childProCount, Map<String, HandlingUnit> reqHuMap,
        String requestingSicCd, String userId, UpdateHandlingUnitsRqst updateHandlingUnitsRqst, AuditInfo auditInfo,
        TransactionContext txnContext,
        EntityManager entityManager) throws ServiceException, ValidationException {

        Map<MovementExceptionTypeCd, Long> countMvmtExcpTypeCdsMap = reqHuMap
            .values()
            .stream()
            .filter(hu -> CollectionUtils.isNotEmpty(hu.getHandlingUnitMovement()))
            .map(hu -> MovementExceptionTypeCdTransformer
                .toEnum(hu.getHandlingUnitMovement().get(0).getExceptionTypeCd()))
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        BigDecimal dmgdCount = BigDecimal
            .valueOf(countMvmtExcpTypeCdsMap.getOrDefault(MovementExceptionTypeCd.DAMAGED, 0L));
        BigDecimal shortCount = BigDecimal
            .valueOf(countMvmtExcpTypeCdsMap.getOrDefault(MovementExceptionTypeCd.SHORT, 0L));

        if (BigDecimal.ZERO.equals(dmgdCount) && BigDecimal.ZERO.equals(shortCount) &&
                updateHandlingUnitsRqst.getHandlingUnits().stream().filter(Objects::nonNull).filter(hu -> hu.getPoorlyPackagedInd() != null).noneMatch(HandlingUnit::getPoorlyPackagedInd)) {
            return Pair.of(false, null);
        }

        List<ShmXdockExcp> shmXDockExcpList = shmXdockExcpSubDAO
            .findByShipmentInstId(shmShipment.getShpInstId(), entityManager);

        ShmXdockExcp latestShmXdockExcp = CollectionUtils
            .emptyIfNull(shmXDockExcpList)
            .stream()
            .max(Comparator.comparing((ShmXdockExcp xde) -> xde.getId().getSeqNbr()))
            .orElse(null);

        // same ShiftCd AND ( (same Pfx AND Sfx) OR same Dock)
        boolean samePfxSfxAndDock = latestShmXdockExcp != null && latestShmXdockExcp.getEvntShiftCd() != null
                && latestShmXdockExcp.getEvntShiftCd().equals(updateHandlingUnitsRqst.getEventShiftCd())
                && ((latestShmXdockExcp.getTrlrIdPfxTxt() != null && latestShmXdockExcp.getTrlrIdSfxNbr() != null
                && latestShmXdockExcp.getTrlrIdPfxTxt().equals(updateHandlingUnitsRqst.getTrailerIdPrefix())
                && latestShmXdockExcp
                            .getTrlrIdSfxNbr()
                            .equals(BasicTransformer.toBigDecimal(updateHandlingUnitsRqst.getTrailerIdSuffixNbr())))
                || (latestShmXdockExcp.getDockNmTxt() != null
                                && latestShmXdockExcp.getDockNmTxt().equals(updateHandlingUnitsRqst.getDockName())));

        boolean allShort = false;
        if (StringUtils.isNotBlank(updateHandlingUnitsRqst.getDockName())) {
            updateHandlingUnitsRqst.setTrailerIdPrefix(DEFAULT_DOCK_TRAILER_PREFIX);
            updateHandlingUnitsRqst.setTrailerIdSuffixNbr(DEFAULT_DOCK_TRAILER_SUFFIX);
            updateHandlingUnitsRqst.setLoadedReleaseNbr(DEFAULT_DOCK_LD_RLSE_NBR);
        }
        ShmXdockExcp xdockExcp = null;
        if (samePfxSfxAndDock && requestingSicCd.equals(latestShmXdockExcp.getRptgSicCd())
                && userId.equals(latestShmXdockExcp.getCrteUid())) {
            // update xdock.
            Triple<String,Long, Long> xdockTriple = buildCommentsTxt(latestShmXdockExcp.getCommentsTxt(), reqHuMap);
            BigDecimal existingShrtCnt = NumberUtils.compare(BasicTransformer.toInt(latestShmXdockExcp.getShortPcsCnt()), 0) < 1
                    ? latestShmXdockExcp.getShortPcsCnt() : latestShmXdockExcp.getShortPcsCnt().subtract(BasicTransformer.toBigDecimal(xdockTriple.getMiddle()));
            BigDecimal newShortCnt = shortCount.add(existingShrtCnt);
            BigDecimal existingDmgCnt = NumberUtils.compare(BasicTransformer.toInt(latestShmXdockExcp.getDmgdPcsCnt()), 0) < 1
                    ? latestShmXdockExcp.getDmgdPcsCnt() : latestShmXdockExcp.getDmgdPcsCnt().subtract(BasicTransformer.toBigDecimal(xdockTriple.getRight()));
            BigDecimal newDmgCnt = dmgdCount.add(existingDmgCnt);
            if (NumberUtils.compare(childProCount, BasicTransformer.toInt(newShortCnt)) == 0) {
                allShort = true;
            }
            xdockExcp = updateShmXdockExcp(latestShmXdockExcp, newDmgCnt, newShortCnt, allShort, xdockTriple.getLeft(),
                updateHandlingUnitsRqst, txnContext);
        } else {
            if (NumberUtils.compare(childProCount, BasicTransformer.toInt(shortCount)) == 0) {
                allShort = true;
            }
            Triple<String,Long, Long> xdockTriple = buildCommentsTxt(StringUtils.SPACE, reqHuMap);
            xdockExcp = createShmXdockExcp(xdockTriple.getLeft(), latestShmXdockExcp, requestingSicCd,
                dmgdCount, shortCount, allShort, shmShipment, auditInfo, userId, updateHandlingUnitsRqst,
                entityManager);
        }
        return Pair.of(allShort, xdockExcp);
    }

    private ShmXdockExcp updateShmXdockExcp(ShmXdockExcp latestShmXdockExcp, BigDecimal dmgdCount, BigDecimal shortCount,
        boolean allShort, String xdockComment, UpdateHandlingUnitsRqst updateHandlingUnitsRqst,
        TransactionContext txnContext) throws ServiceException {
        latestShmXdockExcp
            .setShortPcsCnt(shortCount);
        latestShmXdockExcp.setDmgdPcsCnt(dmgdCount);
        latestShmXdockExcp.setCrteTmst(BasicTransformer.toTimestamp(DateTimeHelper.getTransactionTimestamp()));
        latestShmXdockExcp.setAllShrtInd(BasicTransformer.toString(allShort));
        latestShmXdockExcp.setCommentsTxt(xdockComment);
        if (StringUtils.isNotBlank(updateHandlingUnitsRqst.getDockName())) {
            latestShmXdockExcp.setDockNmTxt(updateHandlingUnitsRqst.getDockName());
        }
        if (StringUtils.isNotBlank(updateHandlingUnitsRqst.getTrailerIdPrefix())) {
            latestShmXdockExcp.setTrlrIdPfxTxt(updateHandlingUnitsRqst.getTrailerIdPrefix());
        }
        if (updateHandlingUnitsRqst.getTrailerIdSuffixNbr() != null) {
            latestShmXdockExcp
                .setTrlrIdSfxNbr(BasicTransformer.toBigDecimal(updateHandlingUnitsRqst.getTrailerIdSuffixNbr()));
        }
        if (StringUtils.isNotBlank(updateHandlingUnitsRqst.getEventDoor())) {
            latestShmXdockExcp.setEvntDoorTxt(updateHandlingUnitsRqst.getEventDoor());
        }

        if (StringUtils.isNotBlank(updateHandlingUnitsRqst.getEventShiftCd())) {
            latestShmXdockExcp.setEvntShiftCd(updateHandlingUnitsRqst.getEventShiftCd());
        }

        if (StringUtils.isNotBlank(updateHandlingUnitsRqst.getLoadedReleaseNbr())) {
            latestShmXdockExcp.setLdRlseNbr(updateHandlingUnitsRqst.getLoadedReleaseNbr());
        }

        if(updateHandlingUnitsRqst.getDockInstanceId() != null) {
            latestShmXdockExcp.setOrigTrmnlSicCd(updateHandlingUnitsRqst.getRequestingSicCd());
            latestShmXdockExcp.setDestTrmnlSicCd(updateHandlingUnitsRqst.getRequestingSicCd());
        }else if (updateHandlingUnitsRqst.getCurrentTrailerInstanceId() != null) {
            latestShmXdockExcp.setOrigTrmnlSicCd(updateHandlingUnitsRqst.getCurrentTrailerOriginSicCd());
            latestShmXdockExcp
                .setDestTrmnlSicCd(
                    StringUtils.isBlank(updateHandlingUnitsRqst.getCurrentTrailerLoadDestinationCd()) ?
                        updateHandlingUnitsRqst.getRequestingSicCd() :
                        updateHandlingUnitsRqst.getCurrentTrailerLoadDestinationCd());
        }

        if (!BooleanUtils.toBoolean(latestShmXdockExcp.getPoorlyPackagedInd())) {
            latestShmXdockExcp.setPoorlyPackagedInd(BasicTransformer.toString(
                    updateHandlingUnitsRqst.getHandlingUnits().stream().filter(Objects::nonNull).filter(hu -> hu.getPoorlyPackagedInd() != null).anyMatch(HandlingUnit::getPoorlyPackagedInd)
            ));
        }

        shmXdockExcpSubDAO
            .updateDB2ShmXdockExcp(latestShmXdockExcp, latestShmXdockExcp.getCrteTmst(), txnContext,
                db2EntityManager);

        return latestShmXdockExcp;
    }

    private ShmXdockExcp createShmXdockExcp(String xdockComment,
        ShmXdockExcp latestShmXdockExcp, String requestingSicCd,
        BigDecimal dmgdCount, BigDecimal shortCount, boolean allShort, ShmShipment shmShipment,
        AuditInfo auditInfo, String userId, UpdateHandlingUnitsRqst updateHandlingUnitsRqst,
        EntityManager entityManager) throws ValidationException {

        ShmXdockExcp xdockExcp = new ShmXdockExcp();
        ShmXdockExcpPK id = new ShmXdockExcpPK();
        id.setSeqNbr(latestShmXdockExcp == null ? 1 : latestShmXdockExcp.getId().getSeqNbr() + 1);
        id.setShpInstId(shmShipment.getShpInstId());
        xdockExcp.setId(id);
        xdockExcp.setAllShrtInd(BasicTransformer.toString(allShort));
        xdockExcp.setCommentsTxt(xdockComment);
        xdockExcp.setCrteTmst(BasicTransformer.toTimestamp(DateTimeHelper.getTransactionTimestamp()));
        xdockExcp.setCrteTranCd(auditInfo.getCreateByPgmId());
        xdockExcp.setCrteUid(userId);
        xdockExcp.setDmgdPcsCnt(dmgdCount);
        if (updateHandlingUnitsRqst.getCurrentTrailerInstanceId() != null) {
            xdockExcp
                .setOrigTrmnlSicCd(updateHandlingUnitsRqst.getCurrentTrailerOriginSicCd());
            xdockExcp
                .setDestTrmnlSicCd(StringUtils.isBlank(updateHandlingUnitsRqst.getCurrentTrailerLoadDestinationCd()) ?
                    updateHandlingUnitsRqst.getRequestingSicCd() :
                    updateHandlingUnitsRqst.getCurrentTrailerLoadDestinationCd());
        }else {
            xdockExcp.setOrigTrmnlSicCd(updateHandlingUnitsRqst.getRequestingSicCd());
            xdockExcp.setDestTrmnlSicCd(updateHandlingUnitsRqst.getRequestingSicCd());
        }
        xdockExcp
            .setDockNmTxt(StringUtils.isBlank(updateHandlingUnitsRqst.getDockName()) ? StringUtils.SPACE :
                updateHandlingUnitsRqst.getDockName());
        xdockExcp
            .setEvntDoorTxt(StringUtils.isBlank(updateHandlingUnitsRqst.getEventDoor()) ? StringUtils.SPACE :
                updateHandlingUnitsRqst.getEventDoor());
        xdockExcp
            .setEvntShiftCd(StringUtils.isBlank(updateHandlingUnitsRqst.getEventShiftCd()) ? StringUtils.SPACE :
                updateHandlingUnitsRqst.getEventShiftCd());
        xdockExcp
            .setLdRlseNbr(StringUtils.isBlank(updateHandlingUnitsRqst.getLoadedReleaseNbr()) ? StringUtils.SPACE :
                updateHandlingUnitsRqst.getLoadedReleaseNbr());
        xdockExcp.setOverPcsCnt(BigDecimal.ZERO);
        xdockExcp.setProNbrTxt(shmShipment.getProNbrTxt());
        xdockExcp.setRptgSicCd(requestingSicCd);
        xdockExcp.setShortPcsCnt(shortCount);
        xdockExcp
            .setTrlrIdPfxTxt(StringUtils.isBlank(updateHandlingUnitsRqst.getTrailerIdPrefix()) ? StringUtils.SPACE :
                updateHandlingUnitsRqst.getTrailerIdPrefix());
        xdockExcp
            .setTrlrIdSfxNbr(updateHandlingUnitsRqst.getTrailerIdSuffixNbr() == null ? BigDecimal.ZERO :
                BasicTransformer.toBigDecimal(updateHandlingUnitsRqst.getTrailerIdSuffixNbr()));
        xdockExcp.setPoorlyPackagedInd(BasicTransformer.toString(
                updateHandlingUnitsRqst.getHandlingUnits() != null && updateHandlingUnitsRqst.getHandlingUnits().stream().anyMatch(x -> BooleanUtils.toBoolean(x.getPoorlyPackagedInd()))
                        ? Boolean.TRUE : Boolean.FALSE));



        shmXdockExcpSubDAO.save(xdockExcp, entityManager);
        shmXdockExcpSubDAO.createDB2ShmXdockExcp(xdockExcp, db2EntityManager);
        return xdockExcp;
    }

    /**
     * format like <childPro> - <excpTypeCd>: <remarks>; <childPro> - <excpTypeCd>: <remarks>.
     *
     * @return
     */
    private Triple<String,Long,Long> buildCommentsTxt(String existingComment, Map<String, HandlingUnit> reqHuMap) {
        Collection<HandlingUnit> hunits = reqHuMap.values();

        if (CollectionUtils.isEmpty(hunits)) {
            return Triple.of(existingComment, 0L, 0L);
        }

        Long shortCount = 0L;
        Long damageCount = 0L;
        StringBuffer strBuffer = new StringBuffer();
        strBuffer.append(existingComment);
        if (CollectionUtils.isNotEmpty(hunits)) {
            for (HandlingUnit hu : hunits) {
                if (existingComment.contains(hu.getChildProNbr())) {
                    int indexOfProOnComment = existingComment.indexOf(hu.getChildProNbr());
                    int indexOfExcpKeywordEnd = existingComment.indexOf(":", indexOfProOnComment);
                    if (indexOfExcpKeywordEnd < 0) {
                        indexOfExcpKeywordEnd = existingComment.indexOf(";", indexOfProOnComment);
                    }

                    if (indexOfExcpKeywordEnd > indexOfProOnComment) {
                        String commentEntryForPro = existingComment.substring(indexOfProOnComment, indexOfExcpKeywordEnd);
                        if (commentEntryForPro.contains(MovementExceptionTypeCd.SHORT.value())) {
                            shortCount++;
                        }
                        if (commentEntryForPro.contains(MovementExceptionTypeCd.DAMAGED.value())) {
                            damageCount++;
                        }
                    }
                    continue;
                }
                if (CollectionUtils.isNotEmpty(hu.getHandlingUnitMovement())) {
                    HandlingUnitMovement handlingUnitMovement = hu.getHandlingUnitMovement().get(0);
                    strBuffer.append(hu.getChildProNbr() + " - " + MovementExceptionTypeCdTransformer
                        .toEnum(handlingUnitMovement.getExceptionTypeCd())
                    .value());
                    if (StringUtils.isNotBlank(handlingUnitMovement.getRemarks())) {
                        strBuffer.append(": " + handlingUnitMovement.getRemarks());
                    }
                    strBuffer.append("; ");
                }
            }
        }
        return Triple.of(strBuffer.toString(), shortCount, damageCount);
    }

    /**
     * Method to persist a ShmHandlingUnitMvmt
     *
     * @param shmHandlingUnit
     * @param nextMvmSeqNbr
     * @param requestingSicCd
     * @param userId
     * @param remarks
     * @param mvmtExcpTypCd
     * @param dockInstId
     * @param trailerInstId
     * @param auditInfo
     * @param entityManager
     * @return ShmHandlingUnitMvmt
     * @throws ValidationException
     */
    protected ShmHandlingUnitMvmt persistShmHandlingUnitMvmt(ShmHandlingUnit shmHandlingUnit, long nextMvmSeqNbr,
        String requestingSicCd, String userId, String remarks,
        MovementExceptionTypeCd mvmtExcpTypCd, Long dockInstId, Long trailerInstId, AuditInfo auditInfo, EntityManager entityManager)
            throws ValidationException {

        ShmHandlingUnitMvmtPK id = new ShmHandlingUnitMvmtPK();
        id.setShpInstId(shmHandlingUnit.getId().getShpInstId());
        id.setSeqNbr(shmHandlingUnit.getId().getSeqNbr());
        id.setMvmtSeqNbr(nextMvmSeqNbr);

        ShmHandlingUnitMvmt shmHandlingUnitMvmt = new ShmHandlingUnitMvmt();
        DtoTransformer.setAuditInfo(shmHandlingUnitMvmt, auditInfo);
        shmHandlingUnitMvmt.setId(id);
        shmHandlingUnitMvmt.setShmHandlingUnit(shmHandlingUnit);
        Timestamp currentTmst = BasicTransformer.toTimestamp(DateTimeHelper.getTransactionTimestamp());
        shmHandlingUnitMvmt.setMvmtTmst(currentTmst);
        shmHandlingUnitMvmt
        .setMvmtTypCd(
            HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.DOCK_OPERATIONS_EXCEPTION));
        shmHandlingUnitMvmt.setExcpTypCd(mvmtExcpTypCd != null ?
            MovementExceptionTypeCdTransformer.toCode(mvmtExcpTypCd) : StringUtils.SPACE);
        shmHandlingUnitMvmt.setSplitAuthorizeBy(StringUtils.SPACE);
        shmHandlingUnitMvmt.setSplitAuthorizeTmst(DB2DefaultValueUtil.LOW_TMST);
        shmHandlingUnitMvmt.setRmrkTxt(remarks == null ? StringUtils.SPACE : remarks);
        shmHandlingUnitMvmt.setArchiveCntlCd(StringUtils.SPACE);
        shmHandlingUnitMvmt.setBypassScanInd(StringUtils.SPACE);
        shmHandlingUnitMvmt.setBypassScanReason(StringUtils.SPACE);
        shmHandlingUnitMvmt.setDmgdCatgCd(StringUtils.SPACE);
        shmHandlingUnitMvmt
            .setDockInstId(dockInstId == null ? BigDecimal.ZERO : BasicTransformer.toBigDecimal(dockInstId));
        shmHandlingUnitMvmt.setMvmtRptgSicCd(requestingSicCd);
        shmHandlingUnitMvmt.setRfsdRsnCd(StringUtils.SPACE);
        shmHandlingUnitMvmt.setScanTmst(DB2DefaultValueUtil.LOW_TMST);
        shmHandlingUnitMvmt
            .setTrlrInstId(
                trailerInstId == null ? BigDecimal.ZERO : BasicTransformer.toBigDecimal(trailerInstId));
        shmHandlingUnitMvmt.setUndlvdRsnCd(StringUtils.SPACE);

        shmHandlingUnitMvmtSubDAO.save(shmHandlingUnitMvmt, entityManager);
        shmHandlingUnitMvmtSubDAO.createDB2ShmHandlingUnitMvmt(shmHandlingUnitMvmt, db2EntityManager);

        return shmHandlingUnitMvmt;
    }

    /**
     * validate child pros nbr against data stored in DB.
     * <br/>
     * if any error is found a new msg will be added to <code>errorMsgMap</code> map.
     */
    private void validateChildProsAgaisntDB(Map<String, HandlingUnit> reqHuMap,
        List<ShmHandlingUnit> shmHandlingUnitsDB, Long reqCurrentTrailerInstId, LinkedHashMap<String, List<String>> errorMsgsMap,
        TransactionContext txnContext) {

        // validate child pros
        for (String childPro : reqHuMap.keySet()) {
            boolean existsInDB = shmHandlingUnitsDB
                .stream()
                .anyMatch(huDB -> huDB.getChildProNbrTxt().equals(childPro));
            if (!existsInDB) {
                addErrorMessageToMap(childPro, "Child PRO is not found.", errorMsgsMap);
            }
        }

        // validate parent pro nbrs
        List<String> parentProNbrList = shmHandlingUnitsDB
            .stream()
            .map(hu -> hu.getParentProNbrTxt())
            .distinct()
            .collect(Collectors.toList());

        if (parentProNbrList.size() > 1) {
            String msg = String.format("There are HUs of different Shipment. ParentPros: %s.", parentProNbrList);
            addErrorMessageToMap("N/A", msg, errorMsgsMap);
        }

    }

    private void validateRequest(UpdateHandlingUnitsRqst updateHandlingUnitRqst,
        LinkedHashMap<String, List<String>> errorMsgsMap, TransactionContext txnContext)
            throws ValidationException {

        if (StringUtils.isBlank(updateHandlingUnitRqst.getRequestingSicCd())) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo("N/A", "RequestingSicCd param is required.")
                .log()
                .build();
        }

        if (CollectionUtils.isEmpty(updateHandlingUnitRqst.getHandlingUnits())) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo("N/A", "At least one HandlingUnit is required.")
                .log()
                .build();
        }

        if (updateHandlingUnitRqst.getUserId() == null) {
            if (txnContext.getUser() != null && txnContext.getUser().getUserId() != null) {
                LOGGER.info("UserId has been updated with the one in the transaction context.");
                updateHandlingUnitRqst.setUserId(txnContext.getUser().getUserId());
            }else {
                addErrorMessageToMap("N/A", "userId is required.",
                    errorMsgsMap);
            }
        }

        Long trlrInst = updateHandlingUnitRqst.getCurrentTrailerInstanceId() != null ? updateHandlingUnitRqst.getCurrentTrailerInstanceId() : 0L;
        Long eqpSfx = updateHandlingUnitRqst.getTrailerIdSuffixNbr() != null ? updateHandlingUnitRqst.getTrailerIdSuffixNbr() : 0L;
        Long dockInst = updateHandlingUnitRqst.getDockInstanceId() != null ? updateHandlingUnitRqst.getDockInstanceId() : 0L;

        if (trlrInst.compareTo(0L) > 0
                || StringUtils.isNotBlank(updateHandlingUnitRqst.getTrailerIdPrefix())
                || eqpSfx.compareTo(0L) > 0) {
            if (trlrInst.compareTo(0L) <= 0
                    || StringUtils.isBlank(updateHandlingUnitRqst.getTrailerIdPrefix())
                    || eqpSfx.compareTo(0L) <= 0) {
                throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                    .moreInfo("N/A",
                        "CurrentTrailerInstId, TrailerIdPrefix and trailerIdSuffixNbr must all three be present when one is.")
                .log()
                .build();
            }
        }

        if ((dockInst.compareTo(0L) > 0 && StringUtils.isBlank(updateHandlingUnitRqst.getDockName()))
                || (dockInst.compareTo(0L) <= 0 && StringUtils.isNotBlank(updateHandlingUnitRqst.getDockName()))) {
            throw ExceptionBuilder
            .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
            .moreInfo("N/A", "DockInstId and dockName must both be present when one is.")
            .log()
            .build();
        }

        if (dockInst.compareTo(0L) > 0
                && (trlrInst.compareTo(0L) > 0
                        || StringUtils.isNotBlank(updateHandlingUnitRqst.getEventDoor()))) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo("N/A",
                    "It's not allowed receiving both DockInstId and CurrentTrailerInstId/EventDoor info too.")
                .log()
                .build();
        }

        long distinctParentProCount = updateHandlingUnitRqst
                .getHandlingUnits()
                .stream()
                .filter(hu -> StringUtils.isNotBlank(hu.getParentProNbr()) && !errorMsgsMap.containsKey(hu.getParentProNbr()))
                .map(HandlingUnit::getParentProNbr)
                .distinct()
                .count();

        //TODO enhance to allow handling of different parent pros in the future
        if (NumberUtils.compare(distinctParentProCount, 1L) != 0) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo("N/A", "Can process sibling PROs only.")
                .log()
                .build();
        }

        Iterator<HandlingUnit> it = updateHandlingUnitRqst.getHandlingUnits().iterator();
        while (it.hasNext()) {
            HandlingUnit hu = it.next();
            validateChildPro(hu, errorMsgsMap, txnContext);
            validateHuMvmtCd(hu, errorMsgsMap);
        }
    }

    private void validateChildPro(HandlingUnit hu, LinkedHashMap<String, List<String>> errorMsgsMap,
        TransactionContext txnContext) {
        if (StringUtils.isBlank(hu.getChildProNbr())) {
            addErrorMessageToMap("N/A", "Child PRO is required.", errorMsgsMap);
            return;
        }

        try {
            hu.setChildProNbr(ProNumberHelper.validateProNumber(hu.getChildProNbr(), txnContext));
        } catch (ServiceException se) {
            addErrorMessageToMap(hu.getChildProNbr(), String.format("Child PRO %s is invalid.", hu.getChildProNbr()),
                errorMsgsMap);
        }

    }

    private void validateHuMvmtCd(HandlingUnit hu, LinkedHashMap<String, List<String>> errorMsgsMap) {
        // validate handling mvmt cd is present.
        if (StringUtils.isBlank(hu.getHandlingMovementCd()) || !HU_MVMT_CDS.contains(hu.getHandlingMovementCd())) {
            addErrorMessageToMap(hu.getChildProNbr(), "Handling Unit Movement Code must be NORMAL or MISSING.",
                errorMsgsMap);
        }
        if (HU_MVMT_CD_MISSING.equals(hu.getHandlingMovementCd())) {
            if (CollectionUtils.isEmpty(hu.getHandlingUnitMovement())) {
                HandlingUnitMovement huMvmt = new HandlingUnitMovement();
                huMvmt.setExceptionTypeCd(MovementExceptionTypeCdTransformer.toCode((MovementExceptionTypeCd.SHORT)));
                hu.setHandlingUnitMovement(Lists.newArrayList(huMvmt));
            } else {
                hu.getHandlingUnitMovement().get(0).setExceptionTypeCd(MovementExceptionTypeCdTransformer.toCode((MovementExceptionTypeCd.SHORT)));
            }
        }
    }

    /**
     * if errorMsgsMap has at least one element, an exception will be throw. <br/>
     * The details of validation error will be sent in the MoreInfo part of a {@link ValidationException}.
     */
    private void throwExceptionIfAnyValidationError(LinkedHashMap<String, List<String>> errorMsgsMap,
        TransactionContext txnContext) throws ValidationException {
        if (!errorMsgsMap.isEmpty()) {
            List<MoreInfo> moreInfoList = new ArrayList<MoreInfo>();

            for (String childPro : errorMsgsMap.keySet()) {
                MoreInfo moreInfo = new MoreInfo();
                moreInfo.setLocation(childPro);
                moreInfo.setMessage(String.join(" ", errorMsgsMap.get(childPro)));
                moreInfoList.add(moreInfo);
            }

            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo(moreInfoList)
                .log()
                .build();
        }
    }

    protected boolean isReportingSicDestination(final String destSic, final String requestingSicCd,
        final TransactionContext txnContext) throws ServiceException {
        final String locSic = getHostSicCd(destSic, txnContext);

        return locSic.equalsIgnoreCase(requestingSicCd) || destSic.equalsIgnoreCase(requestingSicCd);
     }

    /**
     * calls an external rest client to get host sic details by sicCd.
     *
     * @param sicCd
     * @param txnContext
     * @return host sic code.
     * @throws ServiceException
     */
    private String getHostSicCd(final String sicCd, final TransactionContext txnContext) throws ServiceException {
        final GetHostSicDetailsResp response = externalRestClient.getHostSicDetails(sicCd, txnContext);
        if (response == null || response.getHostSicCd() == null) {
            throw ExceptionBuilder.exception(NotFoundErrorMessage.DEST_SIC_NF, txnContext).build();
        }
        return response.getHostSicCd();
    }


    /**
     * it finds the childPro key in the {@link LinkedHashMap errorMsgsMap}.
     * If there is no key, a new one will be added and will be associated to <code>newMessage</code> as a value.<br/>
     * If there the key exists in the map, the <code>newMessage</code> will be added to the list of values.
     *
     * @param childProKey
     * @param newMessage
     * @param errorMsgsMap
     */
    private void addErrorMessageToMap(String childProKey, String newMessage,
        LinkedHashMap<String, List<String>> errorMsgsMap) {
        List<String> errors = errorMsgsMap.getOrDefault(childProKey, new ArrayList<String>());
        errors.add(newMessage);
        errorMsgsMap.put(childProKey, errors);
    }

}
