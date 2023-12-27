package com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate;

import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.AccessorialService;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.LoadValues;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class ManRateLoadValuesFactoryTest {

	private final LoadValues<AccessorialService, ShmAcSvc> acSvcLoadValues = mock(LoadValues.class);
	private final LoadValues<UpdateShipmentRqst, ShmShipment> shipmentLoadValues = mock(LoadValues.class);
	@InjectMocks
	private ManRateLoadValuesFactory factory = mock(ManRateLoadValuesFactory.class, Answers.CALLS_REAL_METHODS.get());

	@Test
	public void testGetFactoryImplementationForShmShipment() {

		LoadValues<ShmShipment, ?> result = factory.getFactoryImplementation(ShmShipment.class);

		assertNotNull(result);
	}

	@Test
	public void testGetFactoryImplementationForShmAcSvc() {
		Class<?> converterClass = ShmAcSvc.class;
		LoadValues<?, ShmAcSvc> result = factory.getFactoryImplementation(converterClass);

		assertTrue(result instanceof ManRateLoadAccessorialValuesImpl);
	}

	@Test
	public void testGetFactoryImplementationForUnknownClass() {
		Class<?> converterClass = SomeOtherClass.class;

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> factory.getFactoryImplementation(converterClass));

		assertEquals("Not implementation for " + converterClass.getName(), exception.getMessage());
	}

	@Test
	public void testCastToConverter() throws Exception {
		Method castToConverterMethod = ManRateLoadValuesFactory.class.getDeclaredMethod("castToConverter",
				Object.class);
		castToConverterMethod.setAccessible(true);

		LoadValues<?, ?> result = (LoadValues<?, ?>) castToConverterMethod.invoke(factory, shipmentLoadValues);
		assertEquals(shipmentLoadValues, result);
	}

	private static class SomeOtherClass {
	}

	// Add more test cases as needed
}
