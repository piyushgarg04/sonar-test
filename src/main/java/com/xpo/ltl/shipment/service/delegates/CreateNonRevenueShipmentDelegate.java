package com.xpo.ltl.shipment.service.delegates;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.freightflow.v2.PostalTransitTime;
import com.xpo.ltl.api.location.v2.GetRefSicAddressResp;
import com.xpo.ltl.api.pronumberreengineering.v1.ListProBolPrefixMasterResp;
import com.xpo.ltl.api.pronumberreengineering.v1.ProInUseCd;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ProFrtBillIndex;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCust;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCustPK;
import com.xpo.ltl.api.shipment.service.entity.ShmCmdyDimension;
import com.xpo.ltl.api.shipment.service.entity.ShmCommodity;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmt;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmtPK;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.BillClassCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.BillStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.DeliveryQualifierCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.HandlingUnitMovementTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MatchedPartySourceCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MatchedPartyStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MatchedPartyTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ServiceTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentMovementTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentSourceCdTransformer;
import com.xpo.ltl.api.shipment.v2.AsMatchedParty;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.BaseLog;
import com.xpo.ltl.api.shipment.v2.BillClassCd;
import com.xpo.ltl.api.shipment.v2.BillStatusCd;
import com.xpo.ltl.api.shipment.v2.Commodity;
import com.xpo.ltl.api.shipment.v2.CommodityDimension;
import com.xpo.ltl.api.shipment.v2.CreateNonRevenueShipmentRqst;
import com.xpo.ltl.api.shipment.v2.DataValidationError;
import com.xpo.ltl.api.shipment.v2.DeliveryQualifierCd;
import com.xpo.ltl.api.shipment.v2.EventLogSubTypeCd;
import com.xpo.ltl.api.shipment.v2.EventLogTypeCd;
import com.xpo.ltl.api.shipment.v2.GetProStatusResp;
import com.xpo.ltl.api.shipment.v2.HandlingUnit;
import com.xpo.ltl.api.shipment.v2.HandlingUnitMovementTypeCd;
import com.xpo.ltl.api.shipment.v2.HandlingUnitTypeCd;
import com.xpo.ltl.api.shipment.v2.MatchedPartySourceCd;
import com.xpo.ltl.api.shipment.v2.MatchedPartyTypeCd;
import com.xpo.ltl.api.shipment.v2.MoreInfo;
import com.xpo.ltl.api.shipment.v2.MovementStatusCd;
import com.xpo.ltl.api.shipment.v2.ProStatusCd;
import com.xpo.ltl.api.shipment.v2.ServiceTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentId;
import com.xpo.ltl.api.shipment.v2.ShipmentMovementTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentSkeleton;
import com.xpo.ltl.api.shipment.v2.ShipmentSourceCd;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.java.util.cityoperations.ProNumber;
import com.xpo.ltl.java.util.cityoperations.TimestampUtils;
import com.xpo.ltl.shipment.service.client.CalculateTransitTimeTask;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ProFrtBillIndexSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentAsEnteredCustomerDAO;
import com.xpo.ltl.shipment.service.dao.ShmCmdyDimensionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmCommoditySubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitMvmtSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmMovementSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.dto.InternalCreateNonRevenueShipmentResponseDTO;
import com.xpo.ltl.shipment.service.enums.BaseLogTypeEnum;
import com.xpo.ltl.shipment.service.helper.ShipmentSkeletonHelper;
import com.xpo.ltl.shipment.service.impl.GetProStatusCdImpl;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.TimestampUtil;

@ApplicationScoped
public class CreateNonRevenueShipmentDelegate {

    private static final int TRAILER_CAPACITY = 1540;
    private static final Logger LOGGER = LogManager.getLogger(CreateNonRevenueShipmentDelegate.class);
    private static final String DEFAULT_DATE_DB2 = "0001-01-01-00.00.00";
    private static final String TRANID = "CRNR";
    private static final String PGM_ID = "NONREVSH";
    private static final String DEFAULT_MOVR_SHPR_NAME2 = "XPO LOGISTICS FREIGHT, INC.";
    private static final List<String> PRO_FRT_IN_USE = Lists.newArrayList("3","4","7");
    private static final List<ProStatusCd> PRO_FRT_IN_USE_ENUM_LIST = Lists
        .newArrayList(ProStatusCd.RECEIVED_PURGED_MAYBE_ON_FBA, ProStatusCd.RECEIVED_ON_FILE_AND_BILLED, ProStatusCd.VOIDED);

    @PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
    private EntityManager db2EntityManager;

    @Inject
    private ProFrtBillIndexSubDAO proNbrFrtBillIndexDAO;

    @Inject
    ShmShipmentSubDAO shmShipmentSubDAO;

    @Inject
    ShmCmdyDimensionSubDAO shmCmdyDimensionSubDAO;

    @Inject
    ShmCommoditySubDAO shmCommoditySubDAO;

    @Inject
    ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

    @Inject
    ShmHandlingUnitMvmtSubDAO shmHandlingUnitMvmtSubDAO;

    @Inject
    ShipmentAsEnteredCustomerDAO shipmentAsEnteredCustomerDAO;

    @Inject
    ShmMovementSubDAO shmMovementSubDAO;

    @Inject
    GetProStatusCdImpl getProStatusCdImpl;

    @Inject
    private ShmEventDelegate shmEventDelegate;

    @Inject
    ExternalRestClient externalRestClient;

    public InternalCreateNonRevenueShipmentResponseDTO createNonRevenueShipment(CreateNonRevenueShipmentRqst request, TransactionContext txnContext, EntityManager entityManager)
            throws ServiceException {

        // pro number valid
        String proNbr = request.getShipmentSkeleton().getParentProNbr();
        ProNumber proNumber = ProNumber.from(proNbr);
        if (proNumber.isValid()) {
            proNbr = proNumber.getNormalized();
        } else {
            return buildOptDataValidationError(ValidationErrorMessage.PRO_NBR_FORMAT_INVALID, "Pro Number invalid.", proNbr, null);
        }

        ShmShipment shmShipmentFromDB = shmShipmentSubDAO.findByIdOrProNumber(proNbr, null, entityManager);
        if (shmShipmentFromDB != null) {
            String msg = String.format("PRO %s is already in use", proNbr);
            MoreInfo moreInfo1 = new MoreInfo();
            moreInfo1.setLocation("createNonRevenueShipment");
            moreInfo1.setMessage(msg);
            return buildOptDataValidationError(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, msg, proNbr, Arrays.asList(moreInfo1));
        }

        // pro number not used
        GetProStatusResp getProStatusResp = getProStatusCdImpl.getProStatus(proNbr, txnContext, entityManager);
        if (PRO_FRT_IN_USE_ENUM_LIST.contains(getProStatusResp.getProStatusCd())) {
            return buildOptDataValidationError(ValidationErrorMessage.PRO_NUMBER_ALREADY_BILLED, String.format("PRO %s is already in use", proNbr),
                proNbr, null);
        }
        
        GetProStatusResp getProStatusRespDB2 = getProStatusCdImpl.getProStatusDB2(proNbr, txnContext, db2EntityManager);
        if (PRO_FRT_IN_USE_ENUM_LIST.contains(getProStatusRespDB2.getProStatusCd())) {
            return buildOptDataValidationError(ValidationErrorMessage.PRO_NUMBER_ALREADY_BILLED, String.format("PRO %s is already in use", proNbr),
                proNbr, null);
        }

        String bolProPrefix = String.format("%03d", proNumber.getPrefix());
        ListProBolPrefixMasterResp legacyPrefix = externalRestClient.listProBolPrefixMaster(Lists.newArrayList(ProInUseCd.LEGACY), bolProPrefix, null, txnContext);

        Boolean isLegacy = (legacyPrefix != null && CollectionUtils.isNotEmpty(legacyPrefix.getBolPrefixMaster()));

        if (isLegacy) {

            if (CollectionUtils.isNotEmpty(request.getShipmentSkeleton().getHandlingUnits())) {
                throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext).moreInfo("createNonRevenueShipment", "Child Pro not allowed for legacy pro").build();
            }

        } else {
            if (CollectionUtils.isEmpty(request.getShipmentSkeleton().getHandlingUnits()) && !ObjectUtils.defaultIfNull(request.getShipmentSkeleton().getHandlingUnitExemptionInd(), false)) {
                throw ExceptionBuilder
                    .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                    .moreInfo("createNonRevenueShipment", "Child Pro required for ExemptionInd = false")
                    .build();
            }

            if (CollectionUtils.isNotEmpty(request.getShipmentSkeleton().getHandlingUnits()) && ObjectUtils.defaultIfNull(request.getShipmentSkeleton().getHandlingUnitExemptionInd(), false)) {
                throw ExceptionBuilder
                    .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                    .moreInfo("createNonRevenueShipment", "Child Pro not allowed for ExemptionInd = true")
                    .build();
            }

        }

        // orig and dest sic both must be valid
        String originSic = request.getShipmentSkeleton().getRequestingSicCd();
        GetRefSicAddressResp originRefSicAddressResp = externalRestClient
            .getRefSicAddress(originSic, txnContext);
        if (ObjectUtils
                .anyNull(originRefSicAddressResp, originRefSicAddressResp.getLocAddress())) {
            return buildOptDataValidationError(ValidationErrorMessage.ORIGIN_SIC_INVALID, "Origin Sic Invalid.", originSic, null);
        }

        String destSic = request.getShipmentSkeleton().getDestinationTerminalSicCd();
        GetRefSicAddressResp destRefSicAddressResp = externalRestClient
            .getRefSicAddress(destSic, txnContext);
        if (ObjectUtils
            .anyNull(destRefSicAddressResp, destRefSicAddressResp.getLocAddress())) {
            return buildOptDataValidationError(ValidationErrorMessage.DEST_SIC_INVALID, "Dest Sic Invalid.", destSic, null);
        }

        // must be a commodity supplied
        if(CollectionUtils.isEmpty(request.getCommodities())) {
            return buildOptDataValidationError(ValidationErrorMessage.ONE_COMMODITY_LINE_REQUIRED, "Commodity required.", null, null);
        }

        // validate billClassCd = GCBZ or COBZ. otherwise throw an exception.
        if (request.getBillClassCd() != BillClassCd.GENERAL_CLAIMS_BUS_SHPMT && request.getBillClassCd() != BillClassCd.CO_BUS_SHPMT) {
            return buildOptDataValidationError(ValidationErrorMessage.BILL_CLASS_INVALID, "Bill Class must be GCBZ or COBZ.", null, null);
        }

        AuditInfo auditInfo = AuditInfoHelper.getAuditInfoWithPgmId(PGM_ID, txnContext);

        if((request.getShipmentSkeleton().getTotalWeightLbs() == null || request.getShipmentSkeleton().getTotalWeightLbs().compareTo(0D) == 0
                || request.getShipmentSkeleton().getTotalPiecesCount() == null || request.getShipmentSkeleton().getTotalPiecesCount().compareTo(BigInteger.ZERO) == 0
                || request.getShipmentSkeleton().getMotorizedPiecesCount() == null || request.getShipmentSkeleton().getMotorizedPiecesCount().compareTo(BigInteger.ZERO) == 0
                || request.getShipmentSkeleton().getLoosePiecesCount() == null || request.getShipmentSkeleton().getLoosePiecesCount().compareTo(BigInteger.ZERO) == 0)
                && CollectionUtils.isNotEmpty(request.getShipmentSkeleton().getHandlingUnits())) {
           Double wgtLbs = request.getShipmentSkeleton().getHandlingUnits().stream()
                   .filter(hu -> hu.getWeightLbs() != null).map(hu -> hu.getWeightLbs()).collect(Collectors.summingDouble(Double::doubleValue));
           Long totPcs = request.getShipmentSkeleton().getHandlingUnits().stream().count();
           Long loosePcs = request.getShipmentSkeleton().getHandlingUnits().stream()
                   .filter(hu -> HandlingUnitTypeCd.LOOSE == hu.getTypeCd()).count();
           Long motorPcs = request.getShipmentSkeleton().getHandlingUnits().stream()
                   .filter(hu -> HandlingUnitTypeCd.MOTOR == hu.getTypeCd()).count();

           if(request.getShipmentSkeleton().getTotalWeightLbs() == null || request.getShipmentSkeleton().getTotalWeightLbs().compareTo(0D) == 0) {
               request.getShipmentSkeleton().setTotalWeightLbs(wgtLbs);
           }

           if(request.getShipmentSkeleton().getTotalPiecesCount() == null || request.getShipmentSkeleton().getTotalPiecesCount().compareTo(BigInteger.ZERO) == 0) {
               request.getShipmentSkeleton().setTotalPiecesCount(BasicTransformer.toBigInteger(totPcs));
           }

           if(request.getShipmentSkeleton().getLoosePiecesCount() == null || request.getShipmentSkeleton().getLoosePiecesCount().compareTo(BigInteger.ZERO) == 0) {
               request.getShipmentSkeleton().setLoosePiecesCount(BasicTransformer.toBigInteger(loosePcs));
           }

           if(request.getShipmentSkeleton().getMotorizedPiecesCount() == null || request.getShipmentSkeleton().getMotorizedPiecesCount().compareTo(BigInteger.ZERO) == 0) {
               request.getShipmentSkeleton().setMotorizedPiecesCount(BasicTransformer.toBigInteger(motorPcs));
               request.getShipmentSkeleton().setMotorizedPiecesKnownInd(true);
           }

        }

        assignCommoditySequenceNbrs(request);

        boolean isGCBZ = request.getBillClassCd() == BillClassCd.GENERAL_CLAIMS_BUS_SHPMT;

        ShmShipment shmShipment = createShipment(request, isGCBZ, auditInfo, txnContext, entityManager);
        createShipperAndConsigneeParties(request, shmShipment, originRefSicAddressResp, txnContext, auditInfo, entityManager);
        List<ShmHandlingUnit> shmHandlingUnitList = createHandlingUnits(request, shmShipment, txnContext, auditInfo, entityManager);
        createCommodities(request, shmShipment.getShpInstId(), auditInfo, entityManager);
        Optional<ShmMovement> shmMovementOpt = createMovement(isGCBZ, shmShipment, entityManager, auditInfo);
        createEvents(isGCBZ, shmShipment, shmMovementOpt, entityManager, auditInfo);
        createHandlingUnitMovements(isGCBZ, shmHandlingUnitList, shmShipment, entityManager, auditInfo);
        createBaseLog(isGCBZ, shmShipment, txnContext);


        upsertProFrtBillIndexByPro(shmShipment, auditInfo, entityManager, txnContext);

        InternalCreateNonRevenueShipmentResponseDTO response = new InternalCreateNonRevenueShipmentResponseDTO();
        response.setNonRevenueShipmentInstId(shmShipment.getShpInstId());
        return response;
    }

    private void createBaseLog(boolean isGCBZ, ShmShipment shmShipment, TransactionContext txnContext) {
        if (!isGCBZ) {
            return;
        }

        BaseLog baseLog = new BaseLog();
        ShipmentId shipmentId = new ShipmentId();
        shipmentId.setShipmentInstId(shmShipment.getShpInstId() + "");
        baseLog.setShipmentId(shipmentId);

        try {
            externalRestClient.startCreateBaseLogChEnsemble(BaseLogTypeEnum.BASE_LOG_42, Collections.singletonList(baseLog), txnContext);
        } catch (ServiceException e) {
            LOGGER.error("Error creating BaseLog 42 for shipment: " + shmShipment.getShpInstId(), e);
        }
    }

    private void createHandlingUnitMovements(boolean isGCBZ, List<ShmHandlingUnit> shmHandlingUnitList, ShmShipment shmShipment, EntityManager entityManager,
        AuditInfo auditInfo) throws ValidationException {

        if (!isGCBZ || CollectionUtils.isEmpty(shmHandlingUnitList)) {
            return;
        }

        List<ShmHandlingUnitMvmt> shmHandlingUnitMvmtList = shmHandlingUnitList.stream().map(hu -> {
            ShmHandlingUnitMvmt huMovement = new ShmHandlingUnitMvmt();
            ShmHandlingUnitMvmtPK id = new ShmHandlingUnitMvmtPK();
            id.setShpInstId(shmShipment.getShpInstId());
            id.setSeqNbr(hu.getId().getSeqNbr());
            id.setMvmtSeqNbr(1L);
            huMovement.setId(id);
            huMovement.setArchiveCntlCd(StringUtils.SPACE);
            huMovement.setBypassScanInd(BasicTransformer.toString(true));
            huMovement.setBypassScanReason(StringUtils.SPACE);
            huMovement.setDockInstId(BigDecimal.ZERO);
            huMovement.setDmgdCatgCd(StringUtils.SPACE);
            huMovement.setExcpTypCd(StringUtils.SPACE);
            huMovement.setMvmtTypCd(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.DELIVER));
            huMovement.setMvmtRptgSicCd(StringUtils.SPACE);
            huMovement.setMvmtTmst(hu.getLstMvmtTmst());
            huMovement.setRfsdRsnCd(StringUtils.SPACE);
            huMovement
                .setUndlvdRsnCd(StringUtils.SPACE);
            huMovement.setRmrkTxt(StringUtils.SPACE);
            huMovement
                .setScanTmst(DtoTransformer.toTimestamp(DEFAULT_DATE_DB2));
            huMovement.setSplitAuthorizeBy(StringUtils.SPACE);
            huMovement.setSplitAuthorizeTmst(DtoTransformer.toTimestamp(DEFAULT_DATE_DB2));
            huMovement.setTrlrInstId(BigDecimal.ZERO);

            DtoTransformer.setAuditInfo(huMovement, auditInfo);
            return huMovement;
        }).collect(Collectors.toList());

        shmHandlingUnitMvmtSubDAO.persist(shmHandlingUnitMvmtList, entityManager);
        shmHandlingUnitMvmtList.forEach(huMvmtDB -> shmHandlingUnitMvmtSubDAO.createDB2ShmHandlingUnitMvmt(huMvmtDB, db2EntityManager));
    }

    private void createEvents(boolean isGCBZ, ShmShipment shmShipment, Optional<ShmMovement> shmMovementOpt, EntityManager entityManager,
        AuditInfo auditInfo) {
        long createEventSeqNbr = shmEventDelegate
                .createEvent(0L, EventLogTypeCd.BILL_ENTRY, EventLogSubTypeCd.RATED_BILL, shmShipment, null, shmShipment.getOrigTrmnlSicCd(),
                    Optional.empty(), TRANID, entityManager, auditInfo);
        createEventSeqNbr = shmEventDelegate
                .createEvent(++createEventSeqNbr, EventLogTypeCd.CUSTOMER_MATCH, EventLogSubTypeCd.CUSTOMER_MATCH, shmShipment, null,
                    shmShipment.getOrigTrmnlSicCd(), Optional.empty(), TRANID, entityManager, auditInfo);
        if (isGCBZ) {
            shmEventDelegate
                .createEvent(++createEventSeqNbr, EventLogTypeCd.SHIPMENT_DLVY, EventLogSubTypeCd.UPDATE_DLVY, shmShipment,
                    shmMovementOpt.orElse(null),
                    shmShipment.getOrigTrmnlSicCd(), Optional.empty(), TRANID, entityManager, auditInfo);

            shmEventDelegate
                .createEvent(++createEventSeqNbr, EventLogTypeCd.SHIPMENT_DLVY, EventLogSubTypeCd.OVER_DLVY, shmShipment, shmMovementOpt.orElse(null),
                    shmShipment.getOrigTrmnlSicCd(), Optional.empty(), TRANID, entityManager, auditInfo);
        }
    }

    private Optional<ShmMovement> createMovement(boolean isGCBZ, ShmShipment shmShipment, EntityManager entityManager, AuditInfo auditInfo)
            throws ValidationException {

        if(!isGCBZ){
            return Optional.empty();
        }

        ShmMovement movement = new ShmMovement();
        ShmMovementPK id = new ShmMovementPK();
        id.setShpInstId(shmShipment.getShpInstId());
        id.setSeqNbr(1L);
        movement.setId(id);
        movement.setArchiveCntlCd(StringUtils.SPACE);
        movement.setCarrierId(BigDecimal.ZERO);
        movement.setCustLocArivTmst(TimestampUtils.getLowTimestamp());
        movement.setCurrSicCd(shmShipment.getCurrSicCd());
        movement.setDlvryQalfrCd(DeliveryQualifierCdTransformer.toCode(DeliveryQualifierCd.OVER_SHPMT));
        movement.setMvmtRptgSicCd(shmShipment.getLstMvRptgSicCd());
        movement.setMvmtTmst(shmShipment.getLstMvmtTmst());
        movement.setObcPkpDlvRteCd(StringUtils.SPACE);
        movement.setRshpCredInd(BasicTransformer.toString(false));
        movement.setRtePfxTxt(StringUtils.SPACE);
        movement.setRteSfxTxt(StringUtils.SPACE);
        movement.setTypCd(ShipmentMovementTypeCdTransformer.toCode(ShipmentMovementTypeCd.DELIVERY));
        movement.setTrlrInstId(BigDecimal.ZERO);
        movement.setTrlrIdPfxTxt(StringUtils.SPACE);
        movement.setTrlrIdSfxNbr(BigDecimal.ZERO);
        movement.setTrlrLdSeqNbr(BigDecimal.ZERO);
        movement.setQlfrRsnCd(StringUtils.EMPTY);
        movement.setScacCd(StringUtils.SPACE);
        DtoTransformer.setAuditInfo(movement, auditInfo);

        shmMovementSubDAO.save(movement, entityManager);
        shmMovementSubDAO.createDB2ShmMovement(movement, db2EntityManager);
        return Optional.of(movement);
    }

    private void assignCommoditySequenceNbrs(CreateNonRevenueShipmentRqst request) {

        short commSeqNbr = 0;
        for (Commodity commodity : CollectionUtils.emptyIfNull(request.getCommodities())) {
            commodity.setSequenceNbr(++commSeqNbr);
        }
    }

    private ShmShipment createShipment(CreateNonRevenueShipmentRqst request, boolean isGCBZ, AuditInfo auditInfo,
        TransactionContext txnContext,
        EntityManager entityManager) throws ServiceException {

        ShipmentSkeleton shipmentSkeleton = request.getShipmentSkeleton();

        ShmShipment shmShipment = new ShmShipment();

        XMLGregorianCalendar lastMvmtDateTime = shipmentSkeleton.getLastMovementDateTime();
        Timestamp lastMvmtTimestamp;
        if (Objects.isNull(lastMvmtDateTime) || TimestampUtil.isLowTimestamp(BasicTransformer.toTimestamp(lastMvmtDateTime))) {
            lastMvmtTimestamp = Timestamp.from(Instant.now());
            lastMvmtDateTime = TimestampUtil.toXmlGregorianCalendar(lastMvmtTimestamp);
        } else {
            lastMvmtTimestamp = BasicTransformer.toTimestamp(lastMvmtDateTime);
        }
        
        shmShipment.setBillClassCd(BillClassCdTransformer.toCode(request.getBillClassCd()));
        shmShipment.setBillStatCd(BillStatusCdTransformer.toCode(BillStatusCd.RATED));
        shmShipment.setProNbrTxt(shipmentSkeleton.getParentProNbr());
        shmShipment.setOrigTrmnlSicCd(shipmentSkeleton.getRequestingSicCd());
        shmShipment.setDestTrmnlSicCd(shipmentSkeleton.getDestinationTerminalSicCd());
        shmShipment.setCurrSicCd(shipmentSkeleton.getRequestingSicCd());

        shmShipment.setTotPcsCnt(BasicTransformer.toBigDecimal(ObjectUtils.defaultIfNull(shipmentSkeleton.getTotalPiecesCount(), BigInteger.ZERO)));
        shmShipment.setTotWgtLbs(BasicTransformer.toBigDecimal(ObjectUtils.defaultIfNull(shipmentSkeleton.getTotalWeightLbs(), 1D)));
        shmShipment.setMtrzdPcsCnt(BasicTransformer.toBigDecimal(ObjectUtils.defaultIfNull(shipmentSkeleton.getMotorizedPiecesCount(), BigInteger.ZERO)));
        shmShipment.setLoosePcsCnt(BasicTransformer.toBigDecimal(ObjectUtils.defaultIfNull(shipmentSkeleton.getLoosePiecesCount(), BigInteger.ZERO)));

        shmShipment.setTotVolCft(BasicTransformer.toBigDecimal(ObjectUtils.defaultIfNull(shipmentSkeleton.getTotalVolumeCubicFeet(), 0D)));
        shmShipment.setPupVolPct(calculatePupVolPct(shipmentSkeleton));

        shmShipment.setLstMvRptgSicCd(ObjectUtils.defaultIfNull(shipmentSkeleton.getLastMoveRptgSicCd(), StringUtils.SPACE));
        shmShipment.setHandlingUnitExemptionRsn(ObjectUtils.defaultIfNull(shipmentSkeleton.getHandlingUnitExemptionReason(), StringUtils.SPACE));
        shmShipment
            .setHandlingUnitExemptionInd(BasicTransformer.toString(ObjectUtils.defaultIfNull(shipmentSkeleton.getHandlingUnitExemptionInd(), false)));
        shmShipment
            .setMtrzdPcsKnwnInd(BasicTransformer
                .toString(shipmentSkeleton.getMotorizedPiecesCount() != null && shipmentSkeleton.getMotorizedPiecesCount().signum() == 1));

        shmShipment.setPkupDt(lastMvmtTimestamp);
        shmShipment.setSrceCd(ShipmentSourceCdTransformer.toCode(ShipmentSourceCd.PKUP_REQUEST));
        shmShipment.setSvcTypCd(ObjectUtils.defaultIfNull(ServiceTypeCdTransformer.toCode(shipmentSkeleton.getServiceTypeCd()), "1"));

        // default for a non-revenue
        shmShipment.setAbsMinChgInd(BasicTransformer.toString(false));
        shmShipment.setActlTrnstDays(BigDecimal.ZERO);
        shmShipment.setArchiveCntlCd(StringUtils.SPACE);
        shmShipment.setArchiveInd(BasicTransformer.toString(false));
        shmShipment.setApptRqrdInd(StringUtils.SPACE);
        shmShipment.setAudtInd(BasicTransformer.toString(false));
        shmShipment.setAreaInstId(BigDecimal.ZERO);
        shmShipment.setAutoRateableInd(BasicTransformer.toString(false));
        shmShipment.setBulkLqdInd(BasicTransformer.toString(false));
        shmShipment.setCalcMvmtSeqNbr(BigDecimal.ZERO);
        shmShipment.setCalcSvcDays(BigDecimal.ZERO);
        shmShipment.setCalcSvcTmst(TimestampUtil.getLowTimestamp());
        shmShipment.setCallForApptInd(BasicTransformer.toString(false));
        shmShipment.setCashCollInd(BasicTransformer.toString(false));
        shmShipment.setCashInd(BasicTransformer.toString(false));
        shmShipment.setCodInd(BasicTransformer.toString(false));
        shmShipment.setConsUnldTrlrCd(StringUtils.SPACE);
        shmShipment.setCrncyConvFctr(BigDecimal.ZERO);
        shmShipment.setCustProfInstId(BigDecimal.ZERO);
        shmShipment.setClsTm(BasicTransformer.toTime(ShipmentSkeletonHelper.DEFAULT_END_TIME));
        shmShipment.setCftPrflMthdCd(StringUtils.SPACE);
        shmShipment.setCftPrflTypeCd(StringUtils.SPACE);
        shmShipment.setChrgToCd(StringUtils.SPACE);
        shmShipment.setDiscPct(BigDecimal.ZERO);
        shmShipment.setDestNtfyInd(BasicTransformer.toString(false));
        shmShipment.setDlvryQalfrCd(isGCBZ ? DeliveryQualifierCdTransformer.toCode(DeliveryQualifierCd.OVER_SHPMT) : StringUtils.SPACE);
        shmShipment.setDlvrSigNmTxt(StringUtils.SPACE);
        shmShipment.setDlvrSigTmst(TimestampUtil.getLowTimestamp());
        shmShipment.setDiffCalcDays(BigDecimal.ZERO);
        shmShipment.setDiffTrnstDays(BigDecimal.ZERO);
        shmShipment.setDebtorTermFlipInd(StringUtils.SPACE);
        shmShipment.setDeclaredValueAmt(BigDecimal.ZERO);
        shmShipment.setDfltTrfId(StringUtils.SPACE);
        shmShipment.setDiscCd(StringUtils.SPACE);
        shmShipment.setDlvrInfoRqdCd(StringUtils.SPACE);
        shmShipment.setEndDlvrTm(BasicTransformer.toTime(ShipmentSkeletonHelper.DEFAULT_END_TIME));
        shmShipment.setEstTrnstDays(BigDecimal.ZERO);
        shmShipment.setExcessiveValueInd(BasicTransformer.toString(false));
        shmShipment.setExclusiveUseInd(BasicTransformer.toString(false));
        shmShipment.setExemptRsnCd(StringUtils.SPACE);
        shmShipment.setFbdsPrintCnt(BigDecimal.ZERO);
        shmShipment.setFoodPoisonCd(StringUtils.SPACE);
        shmShipment.setFromPortCd(StringUtils.SPACE);
        shmShipment.setFrzbleInd(BasicTransformer.toString(false));
        shmShipment.setGarntdCpnInd(BasicTransformer.toString(false));
        shmShipment.setGarntdInd(BasicTransformer.toString(false));
        shmShipment.setGblTrfcInd(BasicTransformer.toString(false));
        shmShipment.setHazmatInd(BasicTransformer.toString(false));
        shmShipment.setHandlingUnitPartialInd(BasicTransformer.toString(false));
        shmShipment.setHandlingUnitSplitInd(BasicTransformer.toString(false));
        shmShipment.setHviestCmdySeqNo(calculateHeaviestCmdySeqNo(request));
        shmShipment.setInvcCrncd(StringUtils.SPACE);
        shmShipment.setInspectedInd(BasicTransformer.toString(false));
        shmShipment.setLateTenderCd("N");
        shmShipment.setLinealFootTotalNbr(BigDecimal.ZERO);
        shmShipment.setLstMvmtTmst(TimestampUtil.getLowTimestamp());
        shmShipment.setLstMovrProTxt(StringUtils.SPACE);
        shmShipment.setManlRtgRqrdInd(BasicTransformer.toString(false));
        shmShipment.setMxDoorToDoorInd(BasicTransformer.toString(false));
        shmShipment.setMovrSuffix(StringUtils.SPACE);
        shmShipment.setMvmtStatCd(MovementStatusCdTransformer.toCode(isGCBZ ? MovementStatusCd.INTERIM_DLVRY : MovementStatusCd.ON_DOCK));
        shmShipment.setNtfictnCd(StringUtils.SPACE);
        shmShipment.setObcPkpDlvRteCd(StringUtils.SPACE);
        shmShipment.setPkupBackdateInd(BasicTransformer.toString(false));
        shmShipment.setPrgBlkInd(BasicTransformer.toString(false));
        shmShipment.setPurInstId(BigDecimal.ZERO);
        shmShipment.setPrcAgrmtId(BigDecimal.ZERO);
        shmShipment.setPrcRulesetNbr(BigDecimal.ZERO);
        shmShipment.setPkupTm(BasicTransformer.toTime(TimestampUtil.getLowTimestamp()));
        shmShipment.setPoorlyPackagedInd(BasicTransformer.toString(false));
        shmShipment.setReadyTm(BasicTransformer.toTime(ShipmentSkeletonHelper.DEFAULT_START_TIME));
        shmShipment.setReweighWgtLbs(BigDecimal.ZERO);
        shmShipment.setRtgOvrdCd(StringUtils.SPACE);
        shmShipment.setRevBillInd(BasicTransformer.toString(false));
        shmShipment.setRtgCrncd(StringUtils.SPACE);
        shmShipment.setReqrDlvrDt(BasicTransformer.toDate(ShipmentSkeletonHelper.DEFAULT_REQ_DELIVER_DATE));
        shmShipment.setRtgTrfId(StringUtils.SPACE);
        shmShipment.setRtAudtrInit(StringUtils.SPACE);
        shmShipment.setRtOrRtAudtqNm(StringUtils.SPACE);
        shmShipment.setRtePfxTxt(StringUtils.SPACE);
        shmShipment.setRteSfxTxt(StringUtils.SPACE);
        shmShipment.setRteTypCd(StringUtils.SPACE);
        shmShipment.setShpmtAcqrTypCd(StringUtils.SPACE);
        shmShipment.setShpSvcStatCd(StringUtils.SPACE);
        shmShipment.setSvcCalcStatCd(StringUtils.SPACE);
        shmShipment.setShprLdTrlrCd(StringUtils.SPACE);
        shmShipment.setShprToConsMiles(BigDecimal.ZERO);
        shmShipment.setSigSvcInd(BasicTransformer.toString(false));
        shmShipment.setSingleShpmtAcqrInd(BasicTransformer.toString(false));
        shmShipment.setSplitInd(BasicTransformer.toString(false));
        shmShipment.setSpotQuoteId(BigDecimal.ZERO);
        shmShipment.setSrceCd(StringUtils.SPACE);
        shmShipment.setStrtDlvrTm(BasicTransformer.toTime(ShipmentSkeletonHelper.DEFAULT_START_TIME));
        shmShipment.setTotChrgAmt(BigDecimal.ZERO);
        shmShipment.setTotUsdAmt(BigDecimal.ZERO);
        shmShipment.setToPortCd(StringUtils.SPACE);
        shmShipment.setTotPlltCnt(BigDecimal.ZERO);
        shmShipment.setTransactionId(StringUtils.SPACE);
        shmShipment.setTrnstMvmtSeqNbr(BigDecimal.ZERO);
        shmShipment.setWarrantyInd(BasicTransformer.toString(false));
        shmShipment.setWarrantyStatCd(StringUtils.SPACE);

        DtoTransformer.setAuditInfo(shmShipment, auditInfo);

        // sets StdTrnstDays/EstTrnstDays/EstimatedDlvrDt/SvcStrtDt
        ServiceTypeCd serviceTypeCd = ObjectUtils.defaultIfNull(shipmentSkeleton.getServiceTypeCd(), ServiceTypeCd.NORMAL);
        ShipmentSkeletonHelper
            .populateServiceStandardInfo(shmShipment, serviceTypeCd, true, calculateTransitTimeMap(shipmentSkeleton, txnContext, entityManager),
                txnContext, externalRestClient);

        shmShipmentSubDAO.create(shmShipment, entityManager);
        shmShipmentSubDAO.createDB2ShmShipment(shmShipment, null, db2EntityManager);
        entityManager.flush();
        db2EntityManager.flush();

        return shmShipment;
    }

    private BigDecimal calculatePupVolPct(ShipmentSkeleton shipmentSkeleton) {
        Double pupVolPct = 0D;
        if (shipmentSkeleton.getTotalVolumeCubicFeet() != null
                && (shipmentSkeleton.getPupVolumePercentage() == null || shipmentSkeleton.getPupVolumePercentage().equals(0D))) {
            pupVolPct = (shipmentSkeleton.getTotalVolumeCubicFeet() / TRAILER_CAPACITY) * 100;
            pupVolPct = (pupVolPct < .1D) ? .1D : pupVolPct;
        } else {
            pupVolPct = shipmentSkeleton.getPupVolumePercentage();
        }
        return BasicTransformer.toBigDecimal(pupVolPct);
    }

    private BigDecimal calculateHeaviestCmdySeqNo(CreateNonRevenueShipmentRqst request) {

        if (CollectionUtils.isEmpty(request.getCommodities())) {
            return BigDecimal.ZERO;
        }

        Optional<Commodity> heaviestComm = ListUtils
            .emptyIfNull(request.getCommodities())
            .stream()
            .max((a, b) -> ObjectUtils.defaultIfNull(a.getWeightLbs(), 0D).compareTo(ObjectUtils.defaultIfNull(b.getWeightLbs(), 0D)));

        return BigDecimal.valueOf(heaviestComm.get().getSequenceNbr());
    }


    private Map<String, PostalTransitTime> calculateTransitTimeMap(ShipmentSkeleton shipmentSkeleton, TransactionContext txnContext,
        EntityManager entityManager) throws ServiceException {
        Collection<CalculateTransitTimeTask> calculateTransitTimeTaskList = new ArrayList<>();
        ShipmentSkeletonHelper
            .populateShmShipmentListAndTransitTimeTaskList(Arrays.asList(shipmentSkeleton), null, new HashMap<>(),
                calculateTransitTimeTaskList, shmShipmentSubDAO, externalRestClient, txnContext, entityManager);
        Map<String, PostalTransitTime> calculateTransitTimeMap = ShipmentSkeletonHelper
            .getCalculateTransitTimeMap(calculateTransitTimeTaskList, externalRestClient, txnContext);
        return calculateTransitTimeMap;
    }

    private void createShipperAndConsigneeParties(CreateNonRevenueShipmentRqst request, ShmShipment shmShipment,
        GetRefSicAddressResp rqstRefSicAddressResp,
        TransactionContext txnContext, AuditInfo auditInfo, EntityManager entityManager) throws ServiceException {

        ShmAsEntdCust shipperParty = getShipperParty(request, shmShipment, rqstRefSicAddressResp, entityManager, auditInfo, txnContext);
        shipmentAsEnteredCustomerDAO.persist(shipperParty, entityManager);
        shipmentAsEnteredCustomerDAO.createDB2ShmAsEntdCust(shipperParty, db2EntityManager);

        ShmAsEntdCust consigneeParty = getConsigneeParty(request, shmShipment, entityManager, auditInfo);
        shipmentAsEnteredCustomerDAO.persist(consigneeParty, entityManager);
        shipmentAsEnteredCustomerDAO.createDB2ShmAsEntdCust(consigneeParty, db2EntityManager);
    }

    private ShmAsEntdCust getShipperParty(CreateNonRevenueShipmentRqst request, ShmShipment shmShipment, GetRefSicAddressResp originLocation,
        EntityManager entityManager,
        AuditInfo auditInfo, TransactionContext txnContext) throws ServiceException {

        AsMatchedParty asMatchedPartyShpr = request
            .getAsMatchedParties()
            .stream()
            .filter(e -> MatchedPartyTypeCd.SHPR == e.getTypeCd())
            .findFirst()
            .get();

        ShmAsEntdCust shipperParty = DtoTransformer.toShmAsEntdCust(asMatchedPartyShpr, new ShmAsEntdCust());

        shipperParty.setId(new ShmAsEntdCustPK());
        shipperParty.getId().setShpInstId(shmShipment.getShpInstId());
        shipperParty.getId().setSeqNbr(1L);

        shipperParty.setTypCd(MatchedPartyTypeCdTransformer.toCode(MatchedPartyTypeCd.SHPR));
        shipperParty.setAddrTxt(StringUtils.defaultIfBlank(shipperParty.getAddrTxt(),
            StringUtils.defaultIfBlank(originLocation.getLocAddress().getAddr1(), StringUtils.SPACE)));
        shipperParty
            .setCtyTxt(StringUtils.defaultIfBlank(shipperParty.getCtyTxt(),
                StringUtils.defaultIfBlank(originLocation.getLocAddress().getCityName(), StringUtils.SPACE)));
        shipperParty
            .setStCd(StringUtils.defaultIfBlank(shipperParty.getStCd(),
                StringUtils.defaultIfBlank(originLocation.getLocAddress().getCountrySubdivisionCd(), StringUtils.SPACE)));
        shipperParty
            .setZip4RestUsTxt(StringUtils.defaultIfBlank(shipperParty.getZip4RestUsTxt(),
                StringUtils.defaultIfBlank(originLocation.getLocAddress().getPostalExtCd(), StringUtils.SPACE)));
        shipperParty
            .setZip6Txt(StringUtils.defaultIfBlank(shipperParty.getZip6Txt(),
                StringUtils.defaultIfBlank(originLocation.getLocAddress().getPostalCd(), StringUtils.SPACE)));
        shipperParty
            .setCntryCd(StringUtils.defaultIfBlank(shipperParty.getCntryCd(),
                StringUtils.defaultIfBlank(originLocation.getLocAddress().getCountryCd(), StringUtils.SPACE)));

        shipperParty.setMchSourceCd(StringUtils.defaultIfBlank(shipperParty.getMchSourceCd(), MatchedPartySourceCdTransformer.toCode(MatchedPartySourceCd.NOT_MATCHED)));
        shipperParty.setName1Txt(asMatchedPartyShpr.getName1());
        shipperParty.setName2Txt(StringUtils.SPACE);
        shipperParty.setMchStatCd(MatchedPartyStatusCdTransformer.toCode(asMatchedPartyShpr.getMatchedStatusCd()));
        shipperParty.setDebtorInd(BasicTransformer.toString(false));
        shipperParty.setEMailId(ObjectUtils.defaultIfNull(shipperParty.getEMailId(), StringUtils.SPACE));
        shipperParty.setAllShpmtPpdInd(BasicTransformer.toString(false));
        shipperParty.setAlternateCustNbr(ObjectUtils.defaultIfNull(shipperParty.getAlternateCustNbr(), BigDecimal.ZERO));
        shipperParty.setArchiveCntlCd(StringUtils.SPACE);
        shipperParty.setAsMchMadCd(StringUtils.defaultIfBlank(shipperParty.getAsMchMadCd(), StringUtils.SPACE));
        shipperParty.setBiltoRelCd(StringUtils.SPACE);
        shipperParty.setBrkrCustKeyNbr(BigDecimal.ZERO);
        shipperParty.setCisCustNbr(ObjectUtils.defaultIfNull(shipperParty.getCisCustNbr(), BigDecimal.ZERO));
        shipperParty.setCredStatCd(StringUtils.defaultIfBlank(shipperParty.getCredStatCd(),"N"));
        shipperParty.setDirCd(StringUtils.SPACE);
        shipperParty.setLstMchTmst(TimestampUtil.getLowTimestamp());
        shipperParty.setMchInitTxt(StringUtils.SPACE);
        shipperParty.setMchStatCd(StringUtils.SPACE);
        shipperParty.setPacdNbr(StringUtils.SPACE);
        shipperParty.setPccdNbr(StringUtils.SPACE);
        shipperParty.setPextNbr(StringUtils.SPACE);
        shipperParty.setPhonNbr(StringUtils.SPACE);
        shipperParty.setPodImgInd(BasicTransformer.toString(false));
        shipperParty.setPodRqrdInd(BasicTransformer.toString(false));
        shipperParty.setPrefPmtCrncyCd(StringUtils.SPACE);
        shipperParty.setSelfInvcInd(BasicTransformer.toString(false));
        shipperParty.setUseAsEntrdInd(BasicTransformer.toString(false));

        DtoTransformer.setAuditInfo(shipperParty, auditInfo);

        return shipperParty;
    }

    private ShmAsEntdCust getConsigneeParty(CreateNonRevenueShipmentRqst request, ShmShipment shmShipment,
        EntityManager entityManager,
        AuditInfo auditInfo) throws ServiceException {

        // Get Consignee Party
        AsMatchedParty asMatchedPartyCons = request
            .getAsMatchedParties()
            .stream()
            .filter(e -> MatchedPartyTypeCd.CONS == e.getTypeCd())
            .findFirst()
            .get();


        ShmAsEntdCust consigneeParty = DtoTransformer.toShmAsEntdCust(asMatchedPartyCons, new ShmAsEntdCust());
        consigneeParty.setId(new ShmAsEntdCustPK());
        consigneeParty.getId().setShpInstId(shmShipment.getShpInstId());
        consigneeParty.getId().setSeqNbr(2L);

        consigneeParty.setAddrTxt(StringUtils.defaultIfBlank(consigneeParty.getAddrTxt(), StringUtils.SPACE));
        consigneeParty.setCtyTxt(StringUtils.defaultIfBlank(consigneeParty.getCtyTxt(), StringUtils.SPACE));
        consigneeParty.setStCd(StringUtils.defaultIfBlank(consigneeParty.getStCd(), StringUtils.SPACE));
        consigneeParty.setZip4RestUsTxt(StringUtils.defaultIfBlank(consigneeParty.getZip4RestUsTxt(), StringUtils.SPACE));
        consigneeParty.setZip6Txt(StringUtils.defaultIfBlank(consigneeParty.getZip6Txt(), StringUtils.SPACE));
        consigneeParty.setCntryCd(StringUtils.defaultIfBlank(consigneeParty.getCntryCd(), StringUtils.SPACE));

        consigneeParty.setTypCd(MatchedPartyTypeCdTransformer.toCode(MatchedPartyTypeCd.CONS));
        consigneeParty.setMchSourceCd(StringUtils.defaultIfBlank(consigneeParty.getMchSourceCd(), MatchedPartySourceCdTransformer.toCode(MatchedPartySourceCd.NOT_MATCHED)));
        consigneeParty.setEMailId(ObjectUtils.defaultIfNull(consigneeParty.getEMailId(), StringUtils.SPACE));
        consigneeParty.setAllShpmtPpdInd(BasicTransformer.toString(false));
        consigneeParty.setAlternateCustNbr(ObjectUtils.defaultIfNull(consigneeParty.getAlternateCustNbr(), BigDecimal.ZERO));
        consigneeParty.setArchiveCntlCd(StringUtils.SPACE);
        consigneeParty.setAsMchMadCd(StringUtils.defaultIfBlank(consigneeParty.getAsMchMadCd(), StringUtils.SPACE));
        consigneeParty.setBiltoRelCd(StringUtils.SPACE);
        consigneeParty.setBrkrCustKeyNbr(BigDecimal.ZERO);
        consigneeParty.setCisCustNbr(ObjectUtils.defaultIfNull(consigneeParty.getCisCustNbr(), BigDecimal.ZERO));
        consigneeParty.setCredStatCd(StringUtils.defaultIfBlank(consigneeParty.getCredStatCd(),"N"));
        consigneeParty.setDirCd(StringUtils.SPACE);
        consigneeParty.setLstMchTmst(TimestampUtil.getLowTimestamp());
        consigneeParty.setMchInitTxt(StringUtils.SPACE);
        consigneeParty.setMchStatCd(StringUtils.SPACE);
        consigneeParty.setPacdNbr(StringUtils.SPACE);
        consigneeParty.setPccdNbr(StringUtils.SPACE);
        consigneeParty.setPextNbr(StringUtils.SPACE);
        consigneeParty.setPhonNbr(StringUtils.SPACE);
        consigneeParty.setPodImgInd(BasicTransformer.toString(false));
        consigneeParty.setPodRqrdInd(BasicTransformer.toString(false));
        consigneeParty.setPrefPmtCrncyCd(StringUtils.SPACE);
        consigneeParty.setSelfInvcInd(BasicTransformer.toString(false));
        consigneeParty.setUseAsEntrdInd(BasicTransformer.toString(false));
        consigneeParty.setName2Txt(DEFAULT_MOVR_SHPR_NAME2);
        consigneeParty.setDebtorInd(BasicTransformer.toString(false));
        DtoTransformer.setAuditInfo(consigneeParty, auditInfo);

        return consigneeParty;
    }

    private void createCommodities(CreateNonRevenueShipmentRqst request, long shmInstId, AuditInfo auditInfo, EntityManager entityManager)
            throws ServiceException {

        if (CollectionUtils.isEmpty(request.getCommodities())) {
            return;
        }

        for (Commodity commodity : request.getCommodities()) {
            ShmCommodity shmCommodity = DtoTransformer.toShmCommodity(commodity, null);
            shmCommodity.getId().setShpInstId(shmInstId);
            shmCommodity.getId().setSeqNbr(commodity.getSequenceNbr()); // assigned at beginning of createNonRev.

            shmCommodity.setAmt(BigDecimal.ZERO);
            shmCommodity.setArchiveCntlCd(StringUtils.SPACE);
            shmCommodity.setAsRatedClassCd(StringUtils.defaultString(shmCommodity.getAsRatedClassCd(), StringUtils.EMPTY));
            shmCommodity.setChrgToCd(StringUtils.SPACE);
            shmCommodity.setClassTyp("55"); //default class
            shmCommodity.setDescTxt(ObjectUtils.defaultIfNull(commodity.getDescription(), StringUtils.SPACE));
            shmCommodity.setDfltClassSlctInd(BasicTransformer.toString(false));
            shmCommodity.setFrzbleInd(BasicTransformer.toString(false));
            shmCommodity.setHzMtInd(BasicTransformer.toString(false));
            shmCommodity.setMinChrgInd(BasicTransformer.toString(false));
            shmCommodity.setMixClssCmdyInd(BasicTransformer.toString(false));
            shmCommodity.setNmfcItmCd(StringUtils.SPACE);
            shmCommodity.setOriglDescTxt(StringUtils.SPACE);
            shmCommodity.setPkgCd(StringUtils.SPACE);
            shmCommodity.setPpdPct(BigDecimal.ZERO);
            shmCommodity.setRdcdWgt(BigDecimal.ZERO);
            shmCommodity.setRtgQty(BigDecimal.ZERO);
            shmCommodity.setRtgUom(StringUtils.SPACE);
            shmCommodity.setTrfRt(BigDecimal.ZERO);
            shmCommodity.setVolCft(BigDecimal.ZERO);
            shmCommodity.setWgtLbs((commodity.getWeightLbs() != null) ? BigDecimal.valueOf(commodity.getWeightLbs()) : BigDecimal.ZERO);
            shmCommodity.setShmCmdyDimensions(new ArrayList<>());
            DtoTransformer.setAuditInfo(shmCommodity, auditInfo);

            int dimSeqNbr = 0;
            for (CommodityDimension cmdyDimension : CollectionUtils.emptyIfNull(commodity.getCommodityDimension())) {
                ShmCmdyDimension shmCmdyDimension = DtoTransformer.toShmCmdyDimension(cmdyDimension, null);
                shmCmdyDimension.getId().setShpInstId(shmInstId);
                shmCmdyDimension.getId().setCmdySeqNbr(commodity.getSequenceNbr());
                shmCmdyDimension.getId().setDimSeqNbr(++dimSeqNbr);
                shmCmdyDimension.setPcsCnt(BigDecimal.ZERO);
                DtoTransformer.setAuditInfo(shmCmdyDimension, auditInfo);
                shmCmdyDimensionSubDAO.persist(shmCmdyDimension, entityManager);
                shmCmdyDimensionSubDAO.createDB2ShmCmdyDimension(shmCmdyDimension, db2EntityManager);
                shmCommodity.getShmCmdyDimensions().add(shmCmdyDimension);
            }

            shmCommoditySubDAO.persist(shmCommodity, entityManager);
            shmCommoditySubDAO.createDB2ShmCommodity(shmCommodity, db2EntityManager);
        }
    }

    // Update Handling Units for every childPro supplied
    private List<ShmHandlingUnit> createHandlingUnits(CreateNonRevenueShipmentRqst request, ShmShipment shmShipment, TransactionContext txnContext,
        AuditInfo auditInfo,
        EntityManager entityManager) throws ServiceException, ValidationException {

        List<HandlingUnit> handlingUnits = request.getShipmentSkeleton().getHandlingUnits();

        if (CollectionUtils.isEmpty(handlingUnits)) {
            return Collections.emptyList();
        }

        XMLGregorianCalendar xmlGregorianCalendar = BasicTransformer.toXMLGregorianCalendar(TimestampUtil.getLowTimestamp());
        handlingUnits.forEach(hu -> {
            hu.setReweighInd(ObjectUtils.defaultIfNull(hu.getReweighInd(), false));
            hu.setPoorlyPackagedInd(ObjectUtils.defaultIfNull(hu.getPoorlyPackagedInd(), false));
            hu.setSplitInd(ObjectUtils.defaultIfNull(hu.getSplitInd(), false));
            hu.setArchiveInd(ObjectUtils.defaultIfNull(hu.getArchiveInd(), false));
            hu
                .setLastMovementDateTime(ObjectUtils
                    .defaultIfNull(hu.getLastMovementDateTime(), xmlGregorianCalendar));
            hu.setCurrentDockLocation(ObjectUtils.defaultIfNull(hu.getCurrentDockLocation(), StringUtils.SPACE));
            hu.setCurrentLocation(ObjectUtils.defaultIfNull(hu.getCurrentLocation(), StringUtils.SPACE));
            hu.setCurrentSicCd(ObjectUtils.defaultIfNull(hu.getCurrentSicCd(), StringUtils.SPACE));
            hu.setCurrentTrailerInstanceId(ObjectUtils.defaultIfNull(hu.getCurrentTrailerInstanceId(), 0L));
            hu.setMoverProNbr(ObjectUtils.defaultIfNull(hu.getMoverProNbr(), StringUtils.SPACE));
            hu.setMoverSuffix(ObjectUtils.defaultIfNull(hu.getMoverSuffix(), StringUtils.SPACE));
            hu.setPupVolumePercentage(ObjectUtils.defaultIfNull(hu.getPupVolumePercentage(), 0D));
            hu.setPickupDate(ObjectUtils.defaultIfNull(hu.getPickupDate(), shmShipment.getPkupDt().toString()));
            hu.setHandlingMovementCd(ObjectUtils.defaultIfNull(hu.getHandlingMovementCd(), "NORMAL"));
            hu.setMovementStatusCd(ObjectUtils.defaultIfNull(shmShipment.getMvmtStatCd(), "1"));
            hu.setWeightLbs(ObjectUtils.defaultIfNull(hu.getWeightLbs(), 1D));
            hu.setDimensionTypeCd(ObjectUtils.defaultIfNull(hu.getDimensionTypeCd(), "DOCK"));
            hu.setSequenceNbr(BasicTransformer.toBigInteger(0));
        });

        List<ShmHandlingUnit> huDBs = DtoTransformer.toShmHandlingUnit(handlingUnits, null);

        AtomicInteger huSeqNbr = new AtomicInteger(0);
        huDBs.forEach(huDB -> {
            huDB.getId().setShpInstId(shmShipment.getShpInstId());
            huDB.getId().setSeqNbr(huSeqNbr.incrementAndGet());
            DtoTransformer.setAuditInfo(huDB, auditInfo);
        });

        shmHandlingUnitSubDAO.persist(huDBs, entityManager);
        huDBs.forEach(huDB -> shmHandlingUnitSubDAO.createDB2ShmHandlingUnit(huDB, db2EntityManager));
        return huDBs;
    }

    private ProFrtBillIndex upsertProFrtBillIndexByPro(ShmShipment shmShipment,
        AuditInfo auditInfo, EntityManager entityManager, TransactionContext txnContext) throws ServiceException {

        ProFrtBillIndex proFrtBillIndexEntity = proNbrFrtBillIndexDAO.findById(shmShipment.getProNbrTxt(), entityManager);

        boolean insertNew = proFrtBillIndexEntity == null;
        if(insertNew) {

            proFrtBillIndexEntity = new ProFrtBillIndex();
        }  else {
            if(PRO_FRT_IN_USE.contains(proFrtBillIndexEntity.getStatCd())) {
                throw ExceptionBuilder
                .exception(ValidationErrorMessage.PRO_NUMBER_ALREADY_BILLED, txnContext)
                .moreInfo("parentProNbr",
                        String.format("PRO %s is already in use",
                            shmShipment.getProNbrTxt()))
                    .build();
            }
        }

        proFrtBillIndexEntity.setProNbrTxt(shmShipment.getProNbrTxt());
        proFrtBillIndexEntity.setShpInstId(BasicTransformer.toBigDecimal(shmShipment.getShpInstId()));
        proFrtBillIndexEntity.setStatCd("3");
        proFrtBillIndexEntity.setMvmtUnitSeqNbr(new BigDecimal(1));
        proFrtBillIndexEntity.setProPfxOvrdInd("N");
        proFrtBillIndexEntity.setBillSicCd(shmShipment.getOrigTrmnlSicCd());

        DtoTransformer.setAuditInfo(proFrtBillIndexEntity, auditInfo);
        if(insertNew) {
            proNbrFrtBillIndexDAO.save(proFrtBillIndexEntity, entityManager);
            proNbrFrtBillIndexDAO.createDB2ProFrtBillIndex(proFrtBillIndexEntity, db2EntityManager);
        } else {
            proNbrFrtBillIndexDAO.save(proFrtBillIndexEntity, entityManager);
            proNbrFrtBillIndexDAO.updateDB2ProNbrFrtBillIndexForUpdSkeleton(proFrtBillIndexEntity, db2EntityManager);
        }

        return proFrtBillIndexEntity;


    }

    private InternalCreateNonRevenueShipmentResponseDTO buildOptDataValidationError(ValidationErrorMessage validationErrorMsg, String name, String value,
        List<MoreInfo> moreInfo) {
        InternalCreateNonRevenueShipmentResponseDTO response = new InternalCreateNonRevenueShipmentResponseDTO();

        DataValidationError d = new DataValidationError();
        d.setErrorCd(validationErrorMsg.name());
        d.setFieldName(name);
        d.setFieldValue(value);
        d.setMoreInfo(moreInfo);
        response.setDataValidationError(Optional.of(d));
        return response;
    }

}
