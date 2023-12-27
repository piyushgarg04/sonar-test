package com.xpo.ltl.shipment.service.impl.updateshipment.update.correction;

import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.v2.AccessorialService;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.LoadValues;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Objects;

public class CorrectionLoadAccessorialValuesImpl implements LoadValues<AccessorialService, ShmAcSvc> {
	private static final String YES_IND = "Y";
	private static final String NO_IND = "N";
	@LogExecutionTime
	@Override
	public void loadtValues(AccessorialService accessorialService, ShmAcSvc shmAcSvc) {
		if (Objects.nonNull(accessorialService.getAmount())) {
			shmAcSvc.setAmt(BigDecimal.valueOf(accessorialService.getAmount()));
		}

		if (Objects.nonNull(accessorialService.getTariffsRate())) {
			shmAcSvc.setTrfRt(BigDecimal.valueOf(accessorialService.getTariffsRate()));
		}

		if (StringUtils.isNotEmpty(accessorialService.getDescription())) {
			shmAcSvc.setDescTxt(accessorialService.getDescription());
		}
		if (Objects.nonNull(accessorialService.getMinimumChargeInd())) {
			boolean minimumChargeInd = accessorialService.getMinimumChargeInd();
			shmAcSvc.setMinChrgInd(minimumChargeInd ? YES_IND : NO_IND);
		}

	}
}
