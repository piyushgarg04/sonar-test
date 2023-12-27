package com.xpo.ltl.shipment.service.impl.updateshipment.update.autorate;

import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.LoadValues;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractShipment;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Objects;

public class AutoRateLoadShipmentValuesImpl extends AbstractShipment implements LoadValues<UpdateShipmentRqst, ShmShipment> {
	@Override
	public void loadtValues(UpdateShipmentRqst updateShipmentRqst, ShmShipment shmShipment) {

//		SHM_SHIPMENT	disc_cd	Rate Response
		if (Objects.nonNull(updateShipmentRqst.getShipment().getDiscountCd()) ){
			shmShipment.setDiscCd(updateShipmentRqst.getShipment().getDiscountCd());
		}
//		SHM_SHIPMENT	warranty_ind	Rate Response
		if (Objects.nonNull(updateShipmentRqst.getShipment().getWarrantyInd()) ){
			shmShipment.setWarrantyInd(getFlag(updateShipmentRqst.getShipment().getWarrantyInd()));
		}
//		SHM_SHIPMENT	abs_min_chg_ind	Rate Response
		if (Objects.nonNull(updateShipmentRqst.getShipment().getAbsoluteMinimumChargeInd()) ){
			shmShipment.setAbsMinChgInd(getFlag(updateShipmentRqst.getShipment().getAbsoluteMinimumChargeInd()));
		}
//		SHM_SHIPMENT	prc_agrmt_id	Rate Response
		if (Objects.nonNull(updateShipmentRqst.getShipment().getPriceAgreementId()) ){
			shmShipment.setPrcAgrmtId(BigDecimal.valueOf(updateShipmentRqst.getShipment().getPriceAgreementId()) );
		}
//		SHM_SHIPMENT	prc_ruleset_nbr	Rate Response
		if (Objects.nonNull(updateShipmentRqst.getShipment().getPriceRulesetNbr()) ){
			shmShipment.setPrcRulesetNbr(BigDecimal.valueOf(updateShipmentRqst.getShipment().getPriceRulesetNbr()));
		}
//		SHM_SHIPMENT	rtg_crncd	Rate Response
		if (Objects.nonNull(updateShipmentRqst.getShipment().getRatingCurrencyCd()) ){
			shmShipment.setRtgCrncd(getRatingCurrencyCd(updateShipmentRqst.getShipment().getRatingCurrencyCd().value()) );
		}
//		SHM_SHIPMENT	crncy_conv_fctr   	Rate Response
		if (Objects.nonNull(updateShipmentRqst.getShipment().getCurrencyConversionFctr()) ){
			shmShipment.setCrncyConvFctr(BigDecimal.valueOf(updateShipmentRqst.getShipment().getCurrencyConversionFctr()));
		}
//		SHM_SHIPMENT	invc_crncd	Rate Response
		if (Objects.nonNull(updateShipmentRqst.getShipment().getInvoicingCurrencyCd()) ){
			shmShipment.setInvcCrncd(getInvoiceCurrencyCd(updateShipmentRqst.getShipment().getInvoicingCurrencyCd().value()) );
		}
//		SHM_SHIPMENT	shpr_to_cons_miles	Rate Response
		if (Objects.nonNull(updateShipmentRqst.getShipment().getShipperToConsigneeMiles()) ){
			shmShipment.setShprToConsMiles(BigDecimal.valueOf(updateShipmentRqst.getShipment().getShipperToConsigneeMiles().doubleValue()));
		}
		if (Objects.nonNull(updateShipmentRqst.getShipment().getTotalWeightLbs())) { //tot_wgt_lbs
			shmShipment.setTotWgtLbs(BigDecimal.valueOf(updateShipmentRqst.getShipment().getTotalWeightLbs()));
		}
		if (Objects.nonNull(updateShipmentRqst.getShipment().getTotalChargeAmount())) {//tot_chrg_amt
			shmShipment.setTotChrgAmt(BigDecimal.valueOf(updateShipmentRqst.getShipment().getTotalChargeAmount()));
		}
		if (Objects.nonNull(updateShipmentRqst.getShipment().getTotalUsdAmount())) { //tot_usd_amt
			shmShipment.setTotUsdAmt(BigDecimal.valueOf(updateShipmentRqst.getShipment().getTotalUsdAmount()));
		}
		if (StringUtils.isNotEmpty(updateShipmentRqst.getShipment().getRatingTariffsId())) {//rtg_trf_id
			shmShipment.setRtgTrfId(updateShipmentRqst.getShipment().getRatingTariffsId());
		}
		if (Objects.nonNull(updateShipmentRqst.getShipment().getDiscountPercentage())) {//disc_pct
			shmShipment.setDiscPct(BigDecimal.valueOf(updateShipmentRqst.getShipment().getDiscountPercentage()));
		}
	}
}
