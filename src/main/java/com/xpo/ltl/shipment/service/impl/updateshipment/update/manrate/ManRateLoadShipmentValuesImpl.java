package com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate;

import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.LoadValues;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractShipment;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Objects;

public class ManRateLoadShipmentValuesImpl extends AbstractShipment implements LoadValues<UpdateShipmentRqst, ShmShipment> {
	@LogExecutionTime
	@Override
	public void loadtValues(UpdateShipmentRqst updateShipmentRqst, ShmShipment shmShipment) {
		if (Objects.nonNull(updateShipmentRqst.getShipment().getTotalWeightLbs())) {
			shmShipment.setTotWgtLbs(BigDecimal.valueOf(updateShipmentRqst.getShipment().getTotalWeightLbs()));
		}
		if (Objects.nonNull(updateShipmentRqst.getShipment().getTotalChargeAmount())) {
			shmShipment.setTotChrgAmt(BigDecimal.valueOf(updateShipmentRqst.getShipment().getTotalChargeAmount()));
		}
		if (Objects.nonNull(updateShipmentRqst.getShipment().getTotalUsdAmount())) {
			shmShipment.setTotUsdAmt(BigDecimal.valueOf(updateShipmentRqst.getShipment().getTotalUsdAmount()));
		}
		if (StringUtils.isNotEmpty(updateShipmentRqst.getShipment().getRatingTariffsId())) {
			shmShipment.setRtgTrfId(updateShipmentRqst.getShipment().getRatingTariffsId());
		}
		if (Objects.nonNull(updateShipmentRqst.getShipment().getSpotQuoteId())) {
			shmShipment.setSpotQuoteId(BigDecimal.valueOf(updateShipmentRqst.getShipment().getSpotQuoteId()));
		}
		if (Objects.nonNull(updateShipmentRqst.getShipment().getDiscountPercentage())) {

			shmShipment.setDiscPct(BigDecimal.valueOf(updateShipmentRqst.getShipment().getDiscountPercentage()));
			shmShipment.setDiscCd(getShipmentDiscCdFromDiscPct(shmShipment.getDiscPct()));

		}
		if (Objects.nonNull(updateShipmentRqst.getShipment().getBillStatusCd())) {
			shmShipment.setBillStatCd(getBillStatusCd(updateShipmentRqst.getShipment().getBillStatusCd().value()));
		}

		if (Objects.nonNull(updateShipmentRqst.getShipment().getRateOrRateAudtqNm())) {
			shmShipment.setRtOrRtAudtqNm(updateShipmentRqst.getShipment().getRateOrRateAudtqNm());
		} else {
			shmShipment.setRtOrRtAudtqNm(StringUtils.EMPTY);
		}
		if (Objects.nonNull(updateShipmentRqst.getShipment().getRateAuditorInitials())) {
			shmShipment.setRtAudtrInit(updateShipmentRqst.getShipment().getRateAuditorInitials());
		} else {
			shmShipment.setRtAudtrInit(StringUtils.EMPTY);
		}

		if (Objects.nonNull(updateShipmentRqst.getShipment().getGuaranteedInd())) {
			shmShipment.setGarntdInd(getFlag(updateShipmentRqst.getShipment().getGuaranteedInd()));
		}

		if (Objects.nonNull(updateShipmentRqst.getShipment().getInvoicingCurrencyCd())) {
			shmShipment.setInvcCrncd(getInvoiceCurrencyCd(updateShipmentRqst
					.getShipment()
					.getInvoicingCurrencyCd()
					.value()));
		}

		if (Objects.nonNull(updateShipmentRqst.getShipment().getRatingCurrencyCd())) {
			shmShipment.setRtgCrncd(getRatingCurrencyCd(updateShipmentRqst
					.getShipment()
					.getRatingCurrencyCd()
					.value()));
		}

	}
}
