package com.xpo.ltl.shipment.service.transformers;

import java.util.ArrayList;
import java.util.List;

import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.service.entity.ShmShipmentHist;
import com.xpo.ltl.api.shipment.transformer.v2.BillClassCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.BillStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ChargeToCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ConsigneeUnloadedTrailerCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.DeliveryInfoRequiredCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.DeliveryQualifierCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ExemptReasonCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.FoodPoisonCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.InvoiceCurrencyCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.LateTenderCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.NotificationCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.RatingCurrencyCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.RouteTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ServiceCalculationStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ServiceStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentAcquiredTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentSourceCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentVolumeMethodCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentVolumeTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipperLoadedTrailerCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.WarrantyStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.Shipment;
import com.xpo.ltl.api.shipment.v2.Shipment_;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.api.transformer.EntityTransformActionListener;

public class ShipmentEntityTransformer extends BasicTransformer {

    public static Shipment toShipment(ShmShipmentHist entity) {
        return toShipment(entity, null);
    }

    public static Shipment toShipment
            (ShmShipmentHist entity,
             EntityTransformActionListener<Shipment, ShmShipment, Shipment_> listener) {
        if (entity == null)
            return null;

        Shipment dto = new Shipment();
        dto.setShipmentInstId(entity.getId().getShpInstId());
        dto.setBillClassCd(BillClassCdTransformer.toEnum(entity.getBillClassCd()));
        dto.setProNbr(toTrimmedString(entity.getProNbrTxt()));
        dto.setBillStatusCd(BillStatusCdTransformer.toEnum(entity.getBillStatCd()));
        dto.setChargeToCd(ChargeToCdTransformer.toEnum(entity.getChrgToCd()));
        dto.setOriginTerminalSicCd(toTrimmedString(entity.getOrigTrmnlSicCd()));
        dto.setDestinationTerminalSicCd(toTrimmedString(entity.getDestTrmnlSicCd()));
        dto.setCurrentSicCd(toTrimmedString(entity.getCurrSicCd()));
        dto.setSourceCd(ShipmentSourceCdTransformer.toEnum(entity.getSrceCd()));
        dto.setPickupDate(toDateString(entity.getPkupDt()));
        dto.setReadyTime(toTrimmedString(entity.getReadyTm()));
        dto.setCloseTime(toTrimmedString(entity.getClsTm()));
        dto.setTotalPiecesCount(toBigInteger(entity.getTotPcsCnt()));
        dto.setMotorizedPiecesCount(toBigInteger(entity.getMtrzdPcsCnt()));
        dto.setTotalWeightLbs(toDouble(entity.getTotWgtLbs()));
        dto.setDefaultTariffsId(toTrimmedString(entity.getDfltTrfId()));
        dto.setTotalChargeAmount(toDouble(entity.getTotChrgAmt()));
        dto.setTotalUsdAmount(toDouble(entity.getTotUsdAmt()));
        dto.setSplitInd(toBoolean(entity.getSplitInd()));
        dto.setHazmatInd(toBoolean(entity.getHazmatInd()));
        dto.setFreezableInd(toBoolean(entity.getFrzbleInd()));
        dto.setSignatureServiceInd(toBoolean(entity.getSigSvcInd()));
        dto.setRevenueBillInd(toBoolean(entity.getRevBillInd()));
        dto.setManualRatingRequiredInd(toBoolean(entity.getManlRtgRqrdInd()));
        dto.setAuditInd(toBoolean(entity.getAudtInd()));
        dto.setCashInd(toBoolean(entity.getCshInd()));
        dto.setCashCollectInd(toBoolean(entity.getCshCollInd()));
        dto.setGovtBolTrafficInd(toBoolean(entity.getGblTrfcInd()));
        dto.setGuaranteedInd(toBoolean(entity.getGarntdInd()));
        dto.setGuaranteedCouponInd(toBoolean(entity.getGarntdCpnInd()));
        dto.setPurgeBlockInd(toBoolean(entity.getPrgBlkInd()));
        dto.setLastMoverPro(toTrimmedString(entity.getLstMovrProTxt()));
        dto.setArchiveInd(toBoolean(entity.getArchiveInd()));
        dto.setCodInd(toBoolean(entity.getCodInd()));
        dto.setDiscountCd(toTrimmedString(entity.getDiscCd()));
        dto.setLastMoveRptgSicCd(toTrimmedString(entity.getLstMvRptgSicCd()));
        dto.setLastMovementDateTime(toXMLGregorianCalendar(entity.getLstMvmtTmst()));
        dto.setMovementStatusCd(MovementStatusCdTransformer.toEnum(entity.getMvmtStatCd()));
        dto.setDeliveryQualifierCd(DeliveryQualifierCdTransformer.toEnum(entity.getDlvryQalfrCd()));
        dto.setRatingCurrencyCd(RatingCurrencyCdTransformer.toEnum(entity.getRtgCrncd()));
        dto.setInvoicingCurrencyCd(InvoiceCurrencyCdTransformer.toEnum(entity.getInvcCrncd()));
        dto.setCurrencyConversionFctr(toDouble(entity.getCrncyConvFctr()));
        dto.setRequiredDeliveryDate(toDateString(entity.getReqrDlvrDt()));
        dto.setStartDeliveryTime(toTrimmedString(entity.getStrtDlvrTm()));
        dto.setEndDeliveryTime(toTrimmedString(entity.getEndDlvrTm()));
        dto.setFromPortCd(toTrimmedString(entity.getFromPortCd()));
        dto.setToPortCd(toTrimmedString(entity.getToPortCd()));
        dto.setObcPickupDlvRouteCd(toTrimmedString(entity.getObcPkpDlvRteCd()));
        dto.setCustProfitabilityInstId(toLong(entity.getCustProfInstId()));
        dto.setRatingTariffsId(toTrimmedString(entity.getRtgTrfId()));
        dto.setRateAuditorInitials(toTrimmedString(entity.getRtAudtrInit()));
        dto.setRateOrRateAudtqNm(toTrimmedString(entity.getRtOrRtAudtqNm()));
        dto.setHeaviestCommoditySequenceNo(toShort(entity.getHviestCmdySeqNo()));
        dto.setServiceTypeCd(toTrimmedString(entity.getSvcTypCd()));
        dto.setEstimatedDeliveryDate(toDateString(entity.getEstimatedDlvrDt()));
        dto.setFbdsPrintCount(toShort(entity.getFbdsPrintCnt()));
        dto.setArchiveControlCd(toTrimmedString(entity.getArchiveCntlCd()));
        dto.setDeliveryInformationRqdCd(DeliveryInfoRequiredCdTransformer.toEnum(entity.getDlvrInfoRqdCd()));
        dto.setDeliverySignatureNm(toTrimmedString(entity.getDlvrSigNmTxt()));
        dto.setDeliverySignatureDateTime(toXMLGregorianCalendar(entity.getDlvrSigTmst()));
        dto.setStandardTransitDays(toShort(entity.getStdTrnstDays()));
        dto.setActualTransitDays(toShort(entity.getActlTrnstDays()));
        dto.setTransitMovementSequenceNbr(toShort(entity.getTrnstMvmtSeqNbr()));
        dto.setWarrantyInd(toBoolean(entity.getWarrantyInd()));
        dto.setWarrantyStatusCd(WarrantyStatusCdTransformer.toEnum(entity.getWarrantyStatCd()));
        dto.setNotificationCd(NotificationCdTransformer.toEnum(entity.getNtfictnCd()));
        dto.setAbsoluteMinimumChargeInd(toBoolean(entity.getAbsMinChgInd()));
        dto.setDiscountPercentage(toDouble(entity.getDiscPct()));
        dto.setPriceAgreementId(toLong(entity.getPrcAgrmtId()));
        dto.setPriceRulesetNbr(toShort(entity.getPrcRulesetNbr()));
        dto.setRoutePrefix(toTrimmedString(entity.getRtePfxTxt()));
        dto.setRouteSuffix(toTrimmedString(entity.getRteSfxTxt()));
        dto.setRouteTypeCd(RouteTypeCdTransformer.toEnum(entity.getRteTypCd()));
        dto.setTotalVolumeCubicFeet(toDouble(entity.getTotVolCft()));
        dto.setTotalPalletsCount(toBigInteger(entity.getTotPlltCnt()));
        dto.setAreaInstId(toLong(entity.getAreaInstId()));
        dto.setAutoRateableInd(toBoolean(entity.getAutoRateableInd()));
        dto.setMotorizedPiecesKnownInd(toBoolean(entity.getMtrzdPcsKnwnInd()));
        dto.setEstimatedTransitDays(toShort(entity.getEstTrnstDays()));
        dto.setCalculatedServiceDays(toShort(entity.getCalcSvcDays()));
        dto.setCalculatedServiceDateTime(toXMLGregorianCalendar(entity.getCalcSvcTmst()));
        dto.setCalculatedMovementSequenceNbr(toShort(entity.getCalcMvmtSeqNbr()));
        dto.setDiffCalculatedDays(toShort(entity.getDiffCalcDays()));
        dto.setDiffTransitDays(toShort(entity.getDiffTrnstDays()));
        dto.setLateTenderCd(LateTenderCdTransformer.toEnum(entity.getLateTenderCd()));
        dto.setShipmentServiceStatusCd(ServiceStatusCdTransformer.toEnum(entity.getShpSvcStatCd()));
        dto.setServiceCalculatedStatusCd(ServiceCalculationStatusCdTransformer.toEnum(entity.getSvcCalcStatCd()));
        dto.setExemptReasonCd(ExemptReasonCdTransformer.toEnum(entity.getExemptRsnCd()));
        dto.setPickupBackdateInd(toBoolean(entity.getPkupBackdateInd()));
        dto.setFoodPoisonCd(FoodPoisonCdTransformer.toEnum(entity.getFoodPoisonCd()));
        dto.setRatingOverrideCd(toTrimmedString(entity.getRtgOvrdCd()));
        dto.setPickupTime(toTrimmedString(entity.getPkupTm()));
        dto.setServiceStartDate(toDateString(entity.getSvcStrtDt()));
        dto.setInspectedInd(toBoolean(entity.getInspectedInd()));
        dto.setSpotQuoteId(toLong(entity.getSpotQuoteId()));
        dto.setShipperToConsigneeMiles(toBigInteger(entity.getShprToConsMiles()));
        dto.setShipperLoadedTrlrCd(ShipperLoadedTrailerCdTransformer.toEnum(entity.getShprLdTrlrCd()));
        dto.setConsigneeUnloadTrlrCd(ConsigneeUnloadedTrailerCdTransformer.toEnum(entity.getConsUnldTrlrCd()));
        dto.setLinealFootTotalNbr(toShort(entity.getLinealFootTotalNbr()));
        dto.setSingleShipmentAcquiredInd(toBoolean(entity.getSingleShpmtAcqrInd()));
        dto.setShipmentAcquiredTypeCd(ShipmentAcquiredTypeCdTransformer.toEnum(entity.getShpmtAcqrTypCd()));
        dto.setLoosePiecesCount(toBigInteger(entity.getLoosePcsCnt()));
        dto.setPupVolumePercentage(toDouble(entity.getPupVolPct()));
        dto.setPurInstId(toLong(entity.getPurInstId()));
        dto.setBulkLiquidInd(toBoolean(entity.getBulkLqdInd()));
        dto.setCubicFeetProfileMthdCd(ShipmentVolumeMethodCdTransformer.toEnum(entity.getCftPrflMthdCd()));
        dto.setCubicFeetProfileTypeCd(ShipmentVolumeTypeCdTransformer.toEnum(entity.getCftPrflTypeCd()));
        dto.setExclusiveUseInd(toBoolean(entity.getExclusiveUseInd()));
        dto.setExcessiveValueInd(toBoolean(entity.getExcessiveValueInd()));
        dto.setDeclaredValueAmount(toDouble(entity.getDeclaredValueAmt()));
        dto.setMexicoDoorToDoorInd(toBoolean(entity.getMxDoorToDoorInd()));
        dto.setCallForAppointmentInd(toBoolean(entity.getCallForApptInd()));
        dto.setDebtorTermFlipInd(toBoolean(entity.getDebtorTermFlipInd()));
        dto.setTransactionId(toTrimmedString(entity.getTransactionId()));
        dto.setHandlingUnitExemptionInd(toBoolean(entity.getHandlingUnitExemptionInd()));
        dto.setHandlingUnitExemptionReason(toTrimmedString(entity.getHandlingUnitExemptionRsn()));
        dto.setAppointmentRequiredInd(toBoolean(entity.getApptRqrdInd()));
        dto.setDestinationNotifyInd(toBoolean(entity.getDestNtfyInd()));
        dto.setReweighWeightLbs(toDouble(entity.getReweighWgtLbs()));
        dto.setPoorlyPackagedInd(toBoolean(entity.getPoorlyPackagedInd()));
        dto.setHandlingUnitSplitInd(toBoolean(entity.getHandlingUnitSplitInd()));
        dto.setHandlingUnitPartialInd(toBoolean(entity.getHandlingUnitPartialInd()));

        if (listener != null) {
            dto.setPickupDate(listener.afterTransformDate(Shipment_.pickupDate, dto.getPickupDate()));
            dto.setReadyTime(listener.afterTransformTime(Shipment_.readyTime, dto.getReadyTime()));
            dto.setCloseTime(listener.afterTransformTime(Shipment_.closeTime, dto.getCloseTime()));
            dto.setLastMovementDateTime(listener.afterTransformDateTime(Shipment_.lastMovementDateTime, dto.getLastMovementDateTime()));
            dto.setRequiredDeliveryDate(listener.afterTransformDate(Shipment_.requiredDeliveryDate, dto.getRequiredDeliveryDate()));
            dto.setStartDeliveryTime(listener.afterTransformTime(Shipment_.startDeliveryTime, dto.getStartDeliveryTime()));
            dto.setEndDeliveryTime(listener.afterTransformTime(Shipment_.endDeliveryTime, dto.getEndDeliveryTime()));
            dto.setEstimatedDeliveryDate(listener.afterTransformDate(Shipment_.estimatedDeliveryDate, dto.getEstimatedDeliveryDate()));
            dto.setDeliverySignatureDateTime(listener.afterTransformDateTime(Shipment_.deliverySignatureDateTime, dto.getDeliverySignatureDateTime()));
            dto.setCalculatedServiceDateTime(listener.afterTransformDateTime(Shipment_.calculatedServiceDateTime, dto.getCalculatedServiceDateTime()));
            dto.setPickupTime(listener.afterTransformTime(Shipment_.pickupTime, dto.getPickupTime()));
            dto.setServiceStartDate(listener.afterTransformDate(Shipment_.serviceStartDate, dto.getServiceStartDate()));
        }

        AuditInfo auditInfo = new AuditInfo();
        dto.setAuditInfo(auditInfo);
        auditInfo.setUpdateByPgmId(toTrimmedString(entity.getSrcLstUpdtTranCd()));
        auditInfo.setUpdatedTimestamp(toXMLGregorianCalendar(entity.getSrcLstUpdtTmst()));
        auditInfo.setUpdateById(toTrimmedString(entity.getSrcLstUpdtBy()));

        return dto;
    }

    public static List<Shipment> toShipment(List<ShmShipmentHist> entities) {
        return toShipment(entities, null);
    }

    public static List<Shipment> toShipment
            (List<ShmShipmentHist> entities,
             EntityTransformActionListener<Shipment, ShmShipment, Shipment_> listener) {
        if (entities == null)
            return null;

        List<Shipment> out = new ArrayList<>();
        for (ShmShipmentHist entity : entities)
            out.add(toShipment(entity, listener));

        return out;
    }

}
