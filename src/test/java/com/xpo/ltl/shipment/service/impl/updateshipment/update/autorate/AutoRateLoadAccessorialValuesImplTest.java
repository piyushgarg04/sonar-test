package com.xpo.ltl.shipment.service.impl.updateshipment.update.autorate;

import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.v1.Accessorial;
import com.xpo.ltl.api.shipment.v2.AccessorialService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutoRateLoadAccessorialValuesImplTest {

	@Test
	void loadtValues() {
		AutoRateLoadAccessorialValuesImpl autoRateLoadAccessorialValues = new AutoRateLoadAccessorialValuesImpl();
		AccessorialService accessorialService = new AccessorialService();
		accessorialService.setAmount(10.0);
		accessorialService.setMinimumChargeInd(true);
		accessorialService.setTariffsRate(10.2);
		accessorialService.setDescription("DESCRIPTION");
		autoRateLoadAccessorialValues.loadtValues(accessorialService, new ShmAcSvc());

	}
}