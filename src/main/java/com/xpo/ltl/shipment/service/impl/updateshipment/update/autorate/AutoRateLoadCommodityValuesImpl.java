package com.xpo.ltl.shipment.service.impl.updateshipment.update.autorate;

import com.xpo.ltl.api.shipment.service.entity.ShmCommodity;
import com.xpo.ltl.api.shipment.v2.Commodity;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.LoadValues;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractCommodity;

import java.math.BigDecimal;
import java.util.Objects;

public class AutoRateLoadCommodityValuesImpl extends AbstractCommodity implements LoadValues<Commodity, ShmCommodity> {
	@Override
	public void loadtValues(Commodity commodity, ShmCommodity shmCommodity) {

//		SHM_COMMODITY	as_rated_class_cd
//		SHM_COMMODITY	trf_rt
//		SHM_COMMODITY	amt
//		SHM_COMMODITY	min_chrg_ind

		if (Objects.nonNull(commodity.getAmount())) { //AMT
			shmCommodity.setAmt(BigDecimal.valueOf(commodity.getAmount()));
		}

		if (Objects.nonNull(commodity.getTariffsRate())) {//TRF_RT
			shmCommodity.setTrfRt(BigDecimal.valueOf(commodity.getTariffsRate()));
		}

		if (Objects.nonNull(commodity.getAsRatedClassCd())) {//AS_RATED_CLASS_CD
			shmCommodity.setAsRatedClassCd(getCommodityClassCd(commodity.getAsRatedClassCd().value()));
		}
		if (Objects.nonNull(commodity.getMinimumChargeInd())) { //MIN_CHRG_IND
			shmCommodity.setMinChrgInd(getFlag(commodity.getMinimumChargeInd()));
		}
	}
}
