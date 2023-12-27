package com.xpo.ltl.shipment.service.impl.updateshipment.update.factory;

import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.CustomsBondUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.correction.CorrectionCustomsBondUpdateImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class UpdateCustomsBondFactoryTest {

	@Mock
	private CorrectionCustomsBondUpdateImpl mockCorrectionCustomsBondUpdate;

	@InjectMocks
	private UpdateCustomsBondFactory customsBondFactory;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testGetUpdateImplementation() {
		ShipmentUpdateActionCd correctionCommand = ShipmentUpdateActionCd.CORRECTION;
		ShipmentUpdateActionCd autoRateCommand = ShipmentUpdateActionCd.AUTO_RATE;

		CustomsBondUpdate correctionUpdate = customsBondFactory.getUpdateImplementation(correctionCommand);
		CustomsBondUpdate autoRateUpdate = customsBondFactory.getUpdateImplementation(autoRateCommand);

		assertNotNull(correctionUpdate);
		assertNotNull(autoRateUpdate);

	}

}