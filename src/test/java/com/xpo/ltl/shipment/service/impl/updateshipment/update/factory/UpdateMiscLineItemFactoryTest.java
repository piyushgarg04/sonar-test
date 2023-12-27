package com.xpo.ltl.shipment.service.impl.updateshipment.update.factory;

import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.MiscLineItemsUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.common.MiscLineItemsUpdateCommonImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate.MiscLineItemsUpdateManRateImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class UpdateMiscLineItemFactoryTest {

	@Mock
	private MiscLineItemsUpdateManRateImpl mockMiscLineItemsUpdateManRate;

	@Mock
	private MiscLineItemsUpdateCommonImpl mockMiscLineItemsUpdateCorr;

	@InjectMocks
	private UpdateMiscLineItemFactory miscLineItemFactory;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testGetUpdateImplementation() {

		MiscLineItemsUpdate manRateUpdate = miscLineItemFactory.getUpdateImplementation(ShipmentUpdateActionCd.MANUAL_RATE);
		MiscLineItemsUpdate correctionUpdate = miscLineItemFactory.getUpdateImplementation(ShipmentUpdateActionCd.CORRECTION);

		assertNotNull(manRateUpdate);
		assertNotNull(correctionUpdate);

	}

}
