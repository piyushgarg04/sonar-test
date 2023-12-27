package com.xpo.ltl.shipment.service.impl.updateshipment.update.correction;

import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.LoadValues;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractShipment;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Objects;

public class CorrectionLoadShipmentValuesImpl extends AbstractShipment implements LoadValues<UpdateShipmentRqst, ShmShipment> {
	@Override
	public void loadtValues(UpdateShipmentRqst updateShipmentRqst, ShmShipment shmShipment) {

		if (Objects.nonNull(updateShipmentRqst.getShipment().getTotalWeightLbs())) {//TOT_WGT_LBS
			shmShipment.setTotWgtLbs(BigDecimal.valueOf(updateShipmentRqst.getShipment().getTotalWeightLbs()));
		}
		if (Objects.nonNull(updateShipmentRqst.getShipment().getTotalChargeAmount())) {//TOT_CHRG_AMT
			shmShipment.setTotChrgAmt(BigDecimal.valueOf(updateShipmentRqst.getShipment().getTotalChargeAmount()));
		}

		if (StringUtils.isNotEmpty(updateShipmentRqst.getShipment().getRatingTariffsId())) {//RTG_TRF_ID
			shmShipment.setRtgTrfId(updateShipmentRqst.getShipment().getRatingTariffsId());
		}

		if (Objects.nonNull(updateShipmentRqst.getShipment().getDiscountPercentage())) {//DISC_PCT

			shmShipment.setDiscPct(BigDecimal.valueOf(updateShipmentRqst.getShipment().getDiscountPercentage()));
			shmShipment.setDiscCd(getShipmentDiscCdFromDiscPct(shmShipment.getDiscPct()));

		}

		if (Objects.nonNull(updateShipmentRqst.getShipment().getGuaranteedInd())) {//GARNTD_IND
			shmShipment.setGarntdInd(getFlag(updateShipmentRqst.getShipment().getGuaranteedInd()));
		}

		if (Objects.nonNull(updateShipmentRqst.getShipment().getChargeToCd())) {//CHRG_TO_CD
			shmShipment.setChrgToCd(getChargeToCdAlt(updateShipmentRqst.getShipment().getChargeToCd().value()));
		}

		if (Objects.nonNull(updateShipmentRqst.getShipment().getOriginTerminalSicCd())) {//ORIG_TRMNL_SIC_CD
			shmShipment.setOrigTrmnlSicCd(updateShipmentRqst.getShipment().getOriginTerminalSicCd());
		}

		if (Objects.nonNull(updateShipmentRqst.getShipment().getDestinationTerminalSicCd())) {//DEST_TRMNL_SIC_CD
			shmShipment.setDestTrmnlSicCd(updateShipmentRqst.getShipment().getDestinationTerminalSicCd());
		}

		if (Objects.nonNull(updateShipmentRqst.getShipment().getTotalPalletsCount())) {//TOT_PLLT_CNT
			shmShipment.setTotPlltCnt(BigDecimal.valueOf(updateShipmentRqst
					.getShipment()
					.getTotalPalletsCount()
					.longValue()));
		}

		if (Objects.nonNull(updateShipmentRqst.getShipment().getLinealFootTotalNbr())) {//LINEAL_FOOT_TOTAL_NBR
			shmShipment.setLinealFootTotalNbr(BigDecimal.valueOf(updateShipmentRqst
					.getShipment()
					.getLinealFootTotalNbr()
					.longValue()));
		}

		if (Objects.nonNull(updateShipmentRqst.getShipment().getPickupDate())) {//PKUP_DT

			shmShipment.setPkupDt(BasicTransformer.toDate(updateShipmentRqst.getShipment().getPickupDate()));
		}
		if (Objects.nonNull(updateShipmentRqst.getShipment().getCashCollectInd())) {//CASH_COLL_IND

			shmShipment.setCashCollInd(getFlag(updateShipmentRqst.getShipment().getCashCollectInd()));
		}
		if (Objects.nonNull(updateShipmentRqst.getShipment().getCashInd())) {//CASH_IND

			shmShipment.setCashInd(getFlag(updateShipmentRqst.getShipment().getCashInd()));
		}

		if (Objects.nonNull(updateShipmentRqst.getShipment().getWarrantyStatusCd())) {//WARRANTY_STAT_CD
			shmShipment.setWarrantyStatCd(getWarrantyStatusCd(updateShipmentRqst
					.getShipment()
					.getWarrantyStatusCd()
					.name()));
		}

		if (Objects.nonNull(updateShipmentRqst.getShipment().getMotorizedPiecesCount())) {//MTRZD_PCS_CNT

			shmShipment.setMtrzdPcsCnt(BigDecimal.valueOf(updateShipmentRqst
					.getShipment()
					.getMotorizedPiecesCount()
					.longValue()));
		}

	}
}
