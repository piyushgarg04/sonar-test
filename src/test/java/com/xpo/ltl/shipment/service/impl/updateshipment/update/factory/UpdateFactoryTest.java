package com.xpo.ltl.shipment.service.impl.updateshipment.update.factory;

import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.UpdateShipment;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.correction.UpdateCorrectionImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate.UpdateManRateImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class UpdateFactoryTest {

	@Mock
	private UpdateManRateImpl mockUpdateManRate;

	@Mock
	private UpdateCorrectionImpl mockUpdateCorrection;

	@InjectMocks
	private UpdateFactory updateFactory;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testGetUpdateImplementation() {

		UpdateShipment manRateUpdate = updateFactory.getUpdateImplementation(ShipmentUpdateActionCd.MANUAL_RATE);
		UpdateShipment correctionUpdate = updateFactory.getUpdateImplementation(ShipmentUpdateActionCd.MANUAL_RATE);

		assertNotNull(manRateUpdate);
		assertNotNull(correctionUpdate);

	}

}