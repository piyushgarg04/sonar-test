package com.xpo.ltl.shipment.service.impl.updateshipment.update.factory;

import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.CommodityTransactions;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.CommodityUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.common.CommodityCommonImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate.ManRateCommodityUpdImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class UpdateCommodityFactoryTest {

	@Mock
	private ManRateCommodityUpdImpl mockManRateCommodityUpdImpl;

	@Mock
	private CommodityCommonImpl mockCorrectionCommodityImpl;

	@InjectMocks
	private UpdateCommodityFactory commodityFactory;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testGetUpdateImplementation() {
		ShipmentUpdateActionCd autoRateCommand = ShipmentUpdateActionCd.AUTO_RATE;
		ShipmentUpdateActionCd correctionCommand = ShipmentUpdateActionCd.CORRECTION;

		CommodityTransactions autoRateUpdate = commodityFactory.getUpdateImplementation(autoRateCommand);
		CommodityTransactions correctionUpdate = commodityFactory.getUpdateImplementation(correctionCommand);

		assertNotNull(autoRateUpdate);
		assertNotNull(correctionUpdate);

	}
}