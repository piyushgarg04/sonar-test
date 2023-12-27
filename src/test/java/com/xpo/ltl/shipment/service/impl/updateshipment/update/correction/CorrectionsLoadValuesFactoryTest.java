package com.xpo.ltl.shipment.service.impl.updateshipment.update.correction;

import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.LoadValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CorrectionsLoadValuesFactoryTest {

	@InjectMocks
	private CorrectionsLoadValuesFactory correctionsLoadValuesFactory;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testGetFactoryImplementationForShmShipment() {
		LoadValues<ShmShipment, ShmShipment> result = correctionsLoadValuesFactory.getFactoryImplementation(ShmShipment.class);

		assertNotNull(result);

	}

	@Test
	public void testGetFactoryImplementationForShmAcSvc() {
		LoadValues<ShmAcSvc, ShmAcSvc> result = correctionsLoadValuesFactory.getFactoryImplementation(ShmAcSvc.class);

		assertNotNull(result);

	}

}