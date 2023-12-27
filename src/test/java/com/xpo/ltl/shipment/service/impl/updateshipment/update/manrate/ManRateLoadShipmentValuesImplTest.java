package com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.impl.updateshipment.MockParent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ManRateLoadShipmentValuesImplTest extends MockParent {

	@Test
	public void testLoadShipmentValues() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		UpdateShipmentRqst updateShipmentRqst = (UpdateShipmentRqst) jsonStringToObject(UpdateShipmentRqst.class,
				this.getJsonFromProperty("06340060976.jsonUpdateShipmentRqst"));
		updateShipmentRqst = objectMapper.convertValue(updateShipmentRqst, new TypeReference<UpdateShipmentRqst>() {
		});

		ShmShipment mockShmShipment = mock(ShmShipment.class);

		// Create the class being tested
		ManRateLoadShipmentValuesImpl shipmentValuesImpl = new ManRateLoadShipmentValuesImpl();

		// Set up the behavior of the mock UpdateShipmentRqst object using when(...).thenReturn(...)

		// ... Set up other mock interactions ...

		// Call the method being tested
		shipmentValuesImpl.loadtValues(updateShipmentRqst, mockShmShipment);

		// Verify the expected interactions using verify(...)
		verify(mockShmShipment).setTotWgtLbs(BigDecimal.valueOf(updateShipmentRqst.getShipment().getTotalWeightLbs()));
		verify(mockShmShipment).setTotChrgAmt(BigDecimal.valueOf(updateShipmentRqst
				.getShipment()
				.getTotalChargeAmount()));

	}

}