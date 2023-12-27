package com.xpo.ltl.shipment.service.impl.updateshipment.update.autorate;

import com.xpo.ltl.api.shipment.v2.Commodity;
import com.xpo.ltl.api.shipment.v2.CommodityClassCd;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutoRateLoadCommodityValuesImplTest {

	@Test
	void loadtValues() {
		AutoRateLoadCommodityValuesImpl autoRateLoadCommodityValues = new AutoRateLoadCommodityValuesImpl();
		Commodity commodity = new Commodity();
		commodity.setAmount(10.5);
		commodity.setAsRatedClassCd(CommodityClassCd.CLSS_50);
		commodity.setMinimumChargeInd(true);
		commodity.setTariffsRate(20.3);

	}
}