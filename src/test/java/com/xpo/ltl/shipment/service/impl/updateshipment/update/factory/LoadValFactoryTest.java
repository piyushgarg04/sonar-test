package com.xpo.ltl.shipment.service.impl.updateshipment.update.factory;

import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.correction.CorrectionsLoadValuesFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate.ManRateLoadValuesFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class LoadValFactoryTest {

	@Mock
	private ManRateLoadValuesFactory mockManRateLoadValuesFactory;

	@Mock
	private CorrectionsLoadValuesFactory mockCorrectionsLoadValuesFactory;

	@InjectMocks
	private LoadValFactory loadValFactory;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testGetFactoryImplementation() {
		ShipmentUpdateActionCd manRateCommand = ShipmentUpdateActionCd.MANUAL_RATE;
		ShipmentUpdateActionCd correctionCommand = ShipmentUpdateActionCd.CORRECTION;

		LoadValuesFactory manRateFactory = loadValFactory.getFactoryImplementation(manRateCommand);
		LoadValuesFactory correctionFactory = loadValFactory.getFactoryImplementation(correctionCommand);

		assertNotNull(manRateFactory);
		assertNotNull(correctionFactory);

	}

}