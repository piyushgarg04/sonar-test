package com.xpo.ltl.shipment.service.impl.updateshipment.update.factory;

import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.AdvBydCarrUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.correction.CorrectionAdvBydCarrUpdateImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate.ManRateAdvBydCarrUpdateImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class UpdateAdvBydCarrFactoryTest {

	@Mock
	private ManRateAdvBydCarrUpdateImpl mockManRateAdvBydCarrUpdateImpl;

	@Mock
	private CorrectionAdvBydCarrUpdateImpl mockCorrectionAdvBydCarrUpdateImpl;

	@InjectMocks
	private UpdateAdvBydCarrFactory advBydCarrFactory;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testGetUpdateImplementation() {
		ShipmentUpdateActionCd manRateCommand = ShipmentUpdateActionCd.MANUAL_RATE;
		ShipmentUpdateActionCd correctionCommand = ShipmentUpdateActionCd.CORRECTION;

		AdvBydCarrUpdate manRateUpdate = advBydCarrFactory.getUpdateImplementation(manRateCommand);
		AdvBydCarrUpdate correctionUpdate = advBydCarrFactory.getUpdateImplementation(correctionCommand);

		assertNotNull(manRateUpdate);
		assertNotNull(correctionUpdate);

		// Add more assertions or verifications as needed
		// ...
	}

	// Add more test methods to cover other scenarios
}