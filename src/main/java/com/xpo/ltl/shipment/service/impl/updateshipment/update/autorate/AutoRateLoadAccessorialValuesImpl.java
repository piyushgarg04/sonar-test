package com.xpo.ltl.shipment.service.impl.updateshipment.update.autorate;

import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.v2.AccessorialService;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.LoadValues;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractAcSvc;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Objects;

public class AutoRateLoadAccessorialValuesImpl extends AbstractAcSvc implements LoadValues<AccessorialService, ShmAcSvc> {
	@LogExecutionTime
	@Override
	public void loadtValues(AccessorialService accessorialService, ShmAcSvc shmAcSvc) {

//		SHM_AC_SVC	seq_nbr
//		SHM_AC_SVC	ac_cd
//		SHM_AC_SVC	trf_rt
//		SHM_AC_SVC	amt
//		SHM_AC_SVC	min_chrg_ind
//		SHM_AC_SVC	desc_txt

		if (Objects.nonNull(accessorialService.getAmount())) {//AMT
			shmAcSvc.setAmt(BigDecimal.valueOf(accessorialService.getAmount()));
		}

		if (Objects.nonNull(accessorialService.getTariffsRate())) {//TRF_RT
			shmAcSvc.setTrfRt(BigDecimal.valueOf(accessorialService.getTariffsRate()));
		}

		if (StringUtils.isNotEmpty(accessorialService.getDescription())) {//DESC_TXT
			shmAcSvc.setDescTxt(accessorialService.getDescription());
		}
		if (Objects.nonNull(accessorialService.getMinimumChargeInd())) { //MIN_CHRG_IND
			shmAcSvc.setMinChrgInd(getFlag(accessorialService.getMinimumChargeInd()));
		}

	}
}
