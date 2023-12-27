package com.xpo.ltl.shipment.service.impl.updateshipment.update.correction;

import com.xpo.ltl.api.shipment.service.entity.ShmCommodity;
import com.xpo.ltl.api.shipment.v2.Commodity;
import com.xpo.ltl.api.shipment.v2.CommodityClassCd;
import com.xpo.ltl.api.shipment.v2.CommodityPackageCd;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class CorrectionLoadCommodityValuesImplTest {

	@Test
	void loadtValues() {
		CorrectionLoadCommodityValuesImpl correctionLoadCommodityValues = new CorrectionLoadCommodityValuesImpl();
		Commodity commodity	 = new Commodity();
		commodity.setAmount(1D);
		commodity.setClassType(CommodityClassCd.CLSS_50);
		commodity.setHazardousMtInd(true);
		commodity.setPackageCd(CommodityPackageCd.BAG);
		commodity.setPiecesCount(BigInteger.ZERO);
		commodity.setTariffsRate(2.0);
		commodity.setWeightLbs(3.0);
		correctionLoadCommodityValues.loadtValues(commodity, new ShmCommodity());
	}
}