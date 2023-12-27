package com.xpo.ltl.shipment.service.impl.updateshipment.update.correction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.impl.updateshipment.MockParent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CorrectionLoadShipmentValuesImplTest extends MockParent {

	@Mock
	private UpdateShipmentRqst mockUpdateShipmentRqst = mock(UpdateShipmentRqst.class,
			Answers.CALLS_REAL_METHODS.get());

	@Mock
	private ShmShipment mockShmShipment = mock(ShmShipment.class, Answers.CALLS_REAL_METHODS.get());

	@InjectMocks
	private CorrectionLoadShipmentValuesImpl correctionLoadShipmentValuesImpl;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testLoadShipmentValues() {
		// Mock behavior and interactions
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		UpdateShipmentRqst updateShipmentRqst = (UpdateShipmentRqst) jsonStringToObject(UpdateShipmentRqst.class,
				this.getJsonFromProperty("06420172510.jsonUpdateShipmentRqst"));
		updateShipmentRqst = objectMapper.convertValue(updateShipmentRqst, new TypeReference<UpdateShipmentRqst>() {
		});

		correctionLoadShipmentValuesImpl.loadtValues(updateShipmentRqst, mockShmShipment);

		// Verify the expected interactions
		verify(mockShmShipment).setTotWgtLbs(BigDecimal.valueOf(updateShipmentRqst.getShipment().getTotalWeightLbs()));
		verify(mockShmShipment).setTotChrgAmt(BigDecimal.valueOf(updateShipmentRqst
				.getShipment()
				.getTotalChargeAmount()));
		verify(mockShmShipment).setRtgTrfId(updateShipmentRqst.getShipment().getRatingTariffsId());
		verify(mockShmShipment).setDiscPct(BigDecimal.valueOf(updateShipmentRqst
				.getShipment()
				.getDiscountPercentage()));

		verify(mockShmShipment).setChrgToCd("P");
		verify(mockShmShipment).setOrigTrmnlSicCd(updateShipmentRqst.getShipment().getOriginTerminalSicCd());
		verify(mockShmShipment).setDestTrmnlSicCd(updateShipmentRqst.getShipment().getDestinationTerminalSicCd());
		verify(mockShmShipment).setTotPlltCnt(BigDecimal.valueOf(updateShipmentRqst
				.getShipment()
				.getTotalPalletsCount()
				.longValue()));
		verify(mockShmShipment).setLinealFootTotalNbr(BigDecimal.valueOf(updateShipmentRqst
				.getShipment()
				.getLinealFootTotalNbr()));

	}

}