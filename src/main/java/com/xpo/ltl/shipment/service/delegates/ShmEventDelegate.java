package com.xpo.ltl.shipment.service.delegates;

import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmEventLog;
import com.xpo.ltl.api.shipment.service.entity.ShmEventLogPK;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.BillClassCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.BillStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ChargeToCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.EventLogSubTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.EventLogTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MatchedPartyTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.DispatchEquipment;
import com.xpo.ltl.api.shipment.v2.EquipmentId;
import com.xpo.ltl.api.shipment.v2.EventLogSubTypeCd;
import com.xpo.ltl.api.shipment.v2.EventLogTypeCd;
import com.xpo.ltl.api.shipment.v2.MatchedPartyTypeCd;
import com.xpo.ltl.api.shipment.v2.MovementStatusCd;
import com.xpo.ltl.api.shipment.v2.Shipment;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmEventLogSubDAO;
import com.xpo.ltl.shipment.service.enums.MovementTypeEnum;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.DB2DefaultValueUtil;

public class ShmEventDelegate {

    private static final String UNLOAD = MovementTypeEnum.UNLOAD.getCode();
    private static final String STRIP_BY_TRAILER = "38";
    private static final String SPACE = " ";
    private static final String EVENT_TYPE_CD_CORRECTIONS = "C";
    private static final String EVENT_SUB_TYPE_CD_OBDV = "44";

    @Inject
    private ShmEventLogSubDAO shmEventLogSubDAO;

    @PersistenceContext(unitName="ltl-java-db2-shipment-jaxrs")
    private EntityManager db2EntityManager;

    public long createEventOnTripCompletion(DispatchEquipment dispatchEquipment,
                                            ShmShipment shmShipment,
                                            ShmMovement newMovement,
                                            AuditInfo auditInfo,
                                            EntityManager entityManager) {

        ShmEventLogPK pk = new ShmEventLogPK();
        pk.setShpInstId(shmShipment.getShpInstId());
        ShmEventLog eventLog =  new ShmEventLog();
        eventLog.setId(pk);

        eventLog.setTypCd(EventLogTypeCdTransformer.toCode(EventLogTypeCd.STRIP));
        eventLog.setSubTypCd(STRIP_BY_TRAILER);

        eventLog.setRptgSicCd(newMovement.getMvmtRptgSicCd());
        eventLog.setOccurredTmst(newMovement.getMvmtTmst());
        eventLog.setTrlrIdPfxTxt(dispatchEquipment.getEquipmentIdPrefix());
        eventLog.setTrlrIdSfxNbr(BasicTransformer.toBigDecimal(dispatchEquipment.getEquipmentIdSuffixNbr()));
        eventLog.setTotPcsCnt(shmShipment.getTotPcsCnt());
        eventLog.setTotWgtLbs(shmShipment.getTotWgtLbs());
        eventLog.setTotChrgAmt(shmShipment.getTotChrgAmt());
        eventLog.setProNbrTxt(shmShipment.getProNbrTxt());
        eventLog.setOrigTrmnlSicCd(shmShipment.getOrigTrmnlSicCd());
        eventLog.setDestTrmnlSicCd(shmShipment.getDestTrmnlSicCd());
        eventLog.setParentInstId(shmShipment.getParentInstId());
        eventLog.setMvmtSeqNbr(shmShipment.getCalcMvmtSeqNbr());
        DtoTransformer.setAuditInfo(eventLog, auditInfo);

        setDefaultValues(eventLog);
        return createEvent(eventLog, entityManager, true);
    }

    public long createEvent(long seqNbr, EventLogTypeCd eventLogTypeCd, EventLogSubTypeCd eventLogSubTypeCd, ShmShipment shmShipment,
        ShmMovement shmMovement, String occurredSicCd, Optional<EquipmentId> equipmentIdOpt, 
        String tranId, EntityManager entityManager, AuditInfo auditInfo) {

        ShmEventLogPK pk = new ShmEventLogPK();
        pk.setShpInstId(shmShipment.getShpInstId());
        pk.setSeqNbr(seqNbr);
        ShmEventLog eventLog = new ShmEventLog();

        DtoTransformer.setAuditInfo(eventLog, auditInfo);

        eventLog.setId(pk);
        eventLog.setTypCd(EventLogTypeCdTransformer.toCode(eventLogTypeCd));
        eventLog.setSubTypCd(EventLogSubTypeCdTransformer.toCode(eventLogSubTypeCd));
        //for C/70
        if (equipmentIdOpt.isPresent()
                && StringUtils.isNotBlank(equipmentIdOpt.get().getEquipmentPrefix()) 
                && equipmentIdOpt.get().getEquipmentIdSuffixNbr() != null) {
            EquipmentId trailer = equipmentIdOpt.get();
            String equipmentPrefix = trailer.getEquipmentPrefix();
            String equipmentSuffix = trailer.getEquipmentSuffix();
            eventLog.setTrlrIdSfxNbr(StringUtils.isNotBlank(equipmentSuffix) ? new BigDecimal(equipmentSuffix) : ZERO);
            eventLog.setTrlrIdPfxTxt(StringUtils.isNotBlank(equipmentPrefix) ? equipmentPrefix : StringUtils.SPACE);
        } else {
            eventLog.setTrlrIdPfxTxt(StringUtils.SPACE);
            eventLog.setTrlrIdSfxNbr(ZERO);
        }

        eventLog.setTrlrIdPfxTxt(SPACE);
        eventLog.setTrlrIdSfxNbr(ZERO);
        eventLog.setOccurredTmst(eventLog.getLstUpdtTmst());
        eventLog.setOccurredSicCd(occurredSicCd);
        eventLog.setRptgSicCd(occurredSicCd);
        eventLog.setTotPcsCnt(shmShipment.getTotPcsCnt());
        eventLog.setTotWgtLbs(shmShipment.getTotWgtLbs());
        eventLog.setTotChrgAmt(shmShipment.getTotChrgAmt());
        eventLog.setProNbrTxt(shmShipment.getProNbrTxt());
        eventLog.setOrigTrmnlSicCd(shmShipment.getOrigTrmnlSicCd());
        eventLog.setDestTrmnlSicCd(shmShipment.getDestTrmnlSicCd());
        eventLog.setParentInstId(shmShipment.getParentInstId());
        if (shmMovement != null && shmMovement.getId() != null && NumberUtils.compare(shmMovement.getId().getSeqNbr(), 0L) > 0) {
            eventLog.setMvmtSeqNbr(BasicTransformer.toBigDecimal(shmMovement.getId().getSeqNbr()));
        } else {
            eventLog.setMvmtSeqNbr(shmShipment.getCalcMvmtSeqNbr());
        }
        eventLog.setGarntdInd(shmShipment.getGarntdInd());
        eventLog.setHazmatInd(shmShipment.getHazmatInd());
        eventLog.setBillClassCd(shmShipment.getBillClassCd());
        eventLog.setMvmtStatCd(shmShipment.getMvmtStatCd());
        eventLog.setAdminStatCd(shmShipment.getBillStatCd());
        eventLog.setChrgToCd(shmShipment.getChrgToCd());
        eventLog.setPkupDt(shmShipment.getPkupDt());
        eventLog.setEstimatedDlvrDt(shmShipment.getEstimatedDlvrDt());
        eventLog.setTranId(tranId);
        eventLog.setParentInstId(null != shmShipment.getParentInstId()
                ? shmShipment.getParentInstId()
                : BigDecimal.ZERO);
        if (CollectionUtils.isNotEmpty(shmShipment.getShmAsEntdCusts())) {
            shmShipment.getShmAsEntdCusts().stream().forEach(asEntd -> {
                if (asEntd.getCisCustNbr() != null) {
                    if (MatchedPartyTypeCd.SHPR == MatchedPartyTypeCdTransformer.toEnum(asEntd.getTypCd())) {
                        eventLog.setShprCustNbr(asEntd.getCisCustNbr());
                        eventLog.setShprCntryCd(asEntd.getCntryCd());
                    } else if (MatchedPartyTypeCd.CONS == MatchedPartyTypeCdTransformer.toEnum(asEntd.getTypCd())) {
                        eventLog.setConsCustNbr(asEntd.getCisCustNbr());
                        eventLog.setConsCntryCd(asEntd.getCntryCd());
                    } else if (MatchedPartyTypeCd.BILL_TO_OTB == MatchedPartyTypeCdTransformer.toEnum(asEntd.getTypCd())) {
                        eventLog.setBil21stCustNbr(asEntd.getCisCustNbr());
                        eventLog.setBilto1stRelCd(asEntd.getBiltoRelCd());
                    } else if (MatchedPartyTypeCd.BILL_TO_INB == MatchedPartyTypeCdTransformer.toEnum(asEntd.getTypCd())) {
                        eventLog.setBil22ndCustNbr(asEntd.getCisCustNbr());
                        eventLog.setBilto1stRelCd(asEntd.getBiltoRelCd());
                    }
                }
            });
        }
        
        setDefaultValues(eventLog);
        return createEvent(eventLog, entityManager, true);

    }

    private void setDefaultValues(ShmEventLog eventLog) {
        eventLog.setAdminInstId(DB2DefaultValueUtil.getValueOr0(eventLog.getAdminInstId()));
        eventLog.setAdminStatCd(DB2DefaultValueUtil.getValueOrSpace(eventLog.getAdminStatCd()));
        eventLog.setArchiveCntlCd(DB2DefaultValueUtil.getValueOrSpace(eventLog.getArchiveCntlCd()));
        eventLog.setBil21stCustNbr(DB2DefaultValueUtil.getValueOr0(eventLog.getBil21stCustNbr()));
        eventLog.setBil22ndCustNbr(DB2DefaultValueUtil.getValueOr0(eventLog.getBil22ndCustNbr()));
        eventLog.setBillClassCd(DB2DefaultValueUtil.getValueOrSpace(eventLog.getBillClassCd()));
        eventLog.setBilto1stRelCd(DB2DefaultValueUtil.getValueOrSpace(eventLog.getBilto1stRelCd()));
        eventLog.setBilto2ndRelCd(DB2DefaultValueUtil.getValueOrSpace(eventLog.getBilto2ndRelCd()));
        eventLog.setBolInstId(DB2DefaultValueUtil.getValueOr0(eventLog.getBolInstId()));
        eventLog.setChrgToCd(DB2DefaultValueUtil.getValueOrSpace(eventLog.getChrgToCd()));
        eventLog.setConsCntryCd(DB2DefaultValueUtil.getValueOrSpace(eventLog.getConsCntryCd()));
        eventLog.setConsCustNbr(DB2DefaultValueUtil.getValueOr0(eventLog.getConsCustNbr()));
        eventLog.setCorrelationId(DB2DefaultValueUtil.getValueOrSpace(eventLog.getCorrelationId()));
        eventLog.setDebtorCd(DB2DefaultValueUtil.getValueOrSpace(eventLog.getDebtorCd()));
        eventLog.setDestTrmnlSicCd(DB2DefaultValueUtil.getValueOrSpace(eventLog.getDestTrmnlSicCd()));
        eventLog.setDmlTmst(DB2DefaultValueUtil.getValueOrLowTmst(eventLog.getDmlTmst()));
        eventLog.setDtlCapxtimestamp(DB2DefaultValueUtil.getValueOrLowTmst(eventLog.getDtlCapxtimestamp()));
        eventLog.setEdiSenderId(DB2DefaultValueUtil.getValueOrSpace(eventLog.getEdiSenderId()));
        eventLog.setEdiSenderShpId(DB2DefaultValueUtil.getValueOrSpace(eventLog.getEdiSenderShpId()));
        eventLog.setEnrouteInd(DB2DefaultValueUtil.getValueOrSpace(eventLog.getEnrouteInd()));
        eventLog.setEstimatedDlvrDt(DB2DefaultValueUtil.getValueOrLowTmst(eventLog.getEstimatedDlvrDt()));
        eventLog.setGarntdInd(DB2DefaultValueUtil.getValueOrSpace(eventLog.getGarntdInd()));
        eventLog.setHazmatInd(DB2DefaultValueUtil.getValueOrSpace(eventLog.getHazmatInd()));
        eventLog.setMvmtSeqNbr(DB2DefaultValueUtil.getValueOr0(eventLog.getMvmtSeqNbr()));
        eventLog.setMvmtStatCd(DB2DefaultValueUtil.getValueOrSpace(eventLog.getMvmtStatCd()));
        eventLog.setOccurredSicCd(DB2DefaultValueUtil.getValueOrSpace(eventLog.getOccurredSicCd()));
        eventLog.setOccurredTmst(DB2DefaultValueUtil.getValueOrLowTmst(eventLog.getOccurredTmst()));
        eventLog.setOrigTrmnlSicCd(DB2DefaultValueUtil.getValueOrSpace(eventLog.getOrigTrmnlSicCd()));
        eventLog.setParentInstId(DB2DefaultValueUtil.getValueOr0(eventLog.getParentInstId()));
        eventLog.setPgmId(DB2DefaultValueUtil.getValueOrSpace(eventLog.getPgmId()));
        eventLog.setPkupDt(DB2DefaultValueUtil.getValueOrLowTmst(eventLog.getPkupDt()));
        eventLog.setProNbrTxt(DB2DefaultValueUtil.getValueOrSpace(eventLog.getProNbrTxt()));
        eventLog.setPurInstId(DB2DefaultValueUtil.getValueOr0(eventLog.getPurInstId()));
        eventLog.setReplLstUpdtTmst(DB2DefaultValueUtil.getValueOrLowTmst(eventLog.getReplLstUpdtTmst()));
        eventLog.setRptgSicCd(DB2DefaultValueUtil.getValueOrSpace(eventLog.getRptgSicCd()));
        eventLog.setShprCntryCd(DB2DefaultValueUtil.getValueOrSpace(eventLog.getShprCntryCd()));
        eventLog.setShprCustNbr(DB2DefaultValueUtil.getValueOr0(eventLog.getShprCustNbr()));
        eventLog.setSubTypCd(DB2DefaultValueUtil.getValueOrSpace(eventLog.getSubTypCd()));
        eventLog.setThirdPartyInd(DB2DefaultValueUtil.getValueOrSpace(eventLog.getThirdPartyInd()));
        eventLog.setTotChrgAmt(DB2DefaultValueUtil.getValueOr0(eventLog.getTotChrgAmt()));
        eventLog.setTotPcsCnt(DB2DefaultValueUtil.getValueOr0(eventLog.getTotPcsCnt()));
        eventLog.setTotWgtLbs(DB2DefaultValueUtil.getValueOr0(eventLog.getTotWgtLbs()));
        eventLog.setTranId(DB2DefaultValueUtil.getValueOrSpace(eventLog.getTranId()));
        eventLog.setTrlrIdPfxTxt(DB2DefaultValueUtil.getValueOrSpace(eventLog.getTrlrIdPfxTxt()));
        eventLog.setTrlrIdSfxNbr(DB2DefaultValueUtil.getValueOr0(eventLog.getTrlrIdSfxNbr()));
        eventLog.setTypCd(DB2DefaultValueUtil.getValueOrSpace(eventLog.getTypCd()));
    }

    protected long createEvent(ShmEventLog eventLog, EntityManager entityManager, boolean toDb2) {
        long seqNbr = eventLog.getId().getSeqNbr();
        if (seqNbr == 0) {
            seqNbr = shmEventLogSubDAO.getLastUsedSeqNbr(eventLog.getId().getShpInstId(), entityManager, db2EntityManager) + 1;
        }
        eventLog.getId().setSeqNbr(seqNbr);

        if (toDb2) {
            shmEventLogSubDAO.createDB2ShmEventLog(eventLog, db2EntityManager);
        }

        entityManager.persist(eventLog);
        return seqNbr;
    }

    public long createUnassignStopShipmentEvent(
            Shipment shipment,
            MovementStatusCd movementStatusCd,
            DispatchEquipment dispatchEquipment,
            AuditInfo auditInfo,
            TransactionContext txnContext,
            EntityManager entityManager) {

        ShmEventLogPK pk = new ShmEventLogPK();
        pk.setShpInstId(shipment.getShipmentInstId());
        ShmEventLog eventLog =  new ShmEventLog();
        eventLog.setId(pk);

        eventLog.setTypCd(EVENT_TYPE_CD_CORRECTIONS);
        eventLog.setSubTypCd(EVENT_SUB_TYPE_CD_OBDV);

        // Last Movement details
        eventLog.setTrlrIdPfxTxt(dispatchEquipment.getEquipmentIdPrefix());
        eventLog.setTrlrIdSfxNbr(BasicTransformer.toBigDecimal(dispatchEquipment.getEquipmentIdSuffixNbr()));
        eventLog.setMvmtSeqNbr(BasicTransformer.toBigDecimal(dispatchEquipment.getEquipmentNbr()));

        // OpsEquipment details
        String currentSicCd = dispatchEquipment.getCurrentSic();
        eventLog.setOccurredSicCd(currentSicCd);
        eventLog.setRptgSicCd(currentSicCd);
        eventLog.setOccurredTmst(getEventCreateTimestamp(txnContext));

        // If shipment is not updated yet.
        eventLog.setMvmtStatCd(MovementStatusCdTransformer.toCode(movementStatusCd));

        // Shipment details
        eventLog.setTotPcsCnt(BasicTransformer.toBigDecimal(shipment.getTotalPiecesCount()));
        eventLog.setTotWgtLbs(BasicTransformer.toBigDecimal(shipment.getTotalWeightLbs()));
        eventLog.setTotChrgAmt(BasicTransformer.toBigDecimal(shipment.getTotalChargeAmount()));
        eventLog.setOrigTrmnlSicCd(shipment.getOriginTerminalSicCd());
        eventLog.setDestTrmnlSicCd(shipment.getDestinationTerminalSicCd());
        eventLog.setPkupDt(BasicTransformer.toDate(shipment.getPickupDate()));
        eventLog.setEstimatedDlvrDt(BasicTransformer.toDate(shipment.getEstimatedDeliveryDate()));
        eventLog.setChrgToCd(ChargeToCdTransformer.toCode(shipment.getChargeToCd()));
        eventLog.setAdminStatCd(BillStatusCdTransformer.toCode(shipment.getBillStatusCd()));
        eventLog.setBillClassCd(BillClassCdTransformer.toCode(shipment.getBillClassCd()));
        eventLog.setGarntdInd(BasicTransformer.toString(shipment.getGuaranteedInd()));
        eventLog.setHazmatInd(BasicTransformer.toString(shipment.getHazmatInd()));
        eventLog.setPgmId(SPACE);

        eventLog.setTranId(auditInfo.getUpdateByPgmId());

        DtoTransformer.setAuditInfo(eventLog, AuditInfoHelper.getAuditInfo(txnContext));

        setDefaultValues(eventLog);
        return createEvent(eventLog, entityManager, true);

    }

    private Timestamp getEventCreateTimestamp(TransactionContext txnContext) {
        return new Timestamp(AuditInfoHelper.getTransactionTimestamp(txnContext)
                .toGregorianCalendar()
                .toInstant()
                .toEpochMilli());
    }
}
