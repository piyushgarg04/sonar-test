package com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.v2.AccessorialService;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.impl.updateshipment.MockParent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ManRateLoadAccessorialValuesImplTest extends MockParent {

	@Test
	void loadShipmentValues() {

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		UpdateShipmentRqst updateShipmentRqst = (UpdateShipmentRqst) jsonStringToObject(UpdateShipmentRqst.class,
				this.getJsonFromProperty("06340060976.jsonUpdateShipmentRqst"));
		updateShipmentRqst = objectMapper.convertValue(updateShipmentRqst, new TypeReference<UpdateShipmentRqst>() {
		});

		ShmAcSvc mockShmAcSvc = mock(ShmAcSvc.class);
		AccessorialService accessorialService = updateShipmentRqst.getAccessorialServices().get(0);
		// Create the class being tested
		ManRateLoadAccessorialValuesImpl accessorialValuesImpl = new ManRateLoadAccessorialValuesImpl();

		// Call the method being tested
		accessorialValuesImpl.loadtValues(accessorialService, mockShmAcSvc);

		// Verify the expected interactions using verify(...)
		verify(mockShmAcSvc).setAmt(BigDecimal.valueOf(accessorialService.getAmount()));
		verify(mockShmAcSvc).setTrfRt(BigDecimal.valueOf(accessorialService.getTariffsRate()));
	}
}