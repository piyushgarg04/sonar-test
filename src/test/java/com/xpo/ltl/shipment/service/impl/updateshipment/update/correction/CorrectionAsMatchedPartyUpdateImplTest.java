package com.xpo.ltl.shipment.service.impl.updateshipment.update.correction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCust;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.AsMatchedParty;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.impl.updateshipment.MockParent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

class CorrectionAsMatchedPartyUpdateImplTest extends MockParent {

	@InjectMocks
	private CorrectionAsMatchedPartyUpdateImpl correctionAsMatchedPartyUpdate;

	@Mock
	private TransactionContext txnContext;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testGetAsMatchedPartiesForTransactions() throws Exception {
		String pro = "06420172510";
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		UpdateShipmentRqst updateShipmentRqst = (UpdateShipmentRqst) jsonStringToObject(UpdateShipmentRqst.class,
				this.getJsonFromProperty(pro + ".jsonUpdateShipmentRqst"));
		updateShipmentRqst = objectMapper.convertValue(updateShipmentRqst, new TypeReference<UpdateShipmentRqst>() {
		});

		ShmShipment shmShipment = (ShmShipment) jsonStringToObject(ShmShipment.class,
				this.getJsonFromProperty(pro + ".jsonShmShipment"));
		shmShipment = objectMapper.convertValue(shmShipment, new TypeReference<ShmShipment>() {
		});
		List<AsMatchedParty> asMatchedParties = new ArrayList<>();

		asMatchedParties.add(updateShipmentRqst.getAsMatchedParties().get(1));
		asMatchedParties.add(updateShipmentRqst.getAsMatchedParties().get(2));
		List<ShmAsEntdCust> shmAsEntdCustsOriginal = new ArrayList<>();
		shmAsEntdCustsOriginal = (List<ShmAsEntdCust>) jsonStringToObject(shmAsEntdCustsOriginal.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmAsEntdCust"));
		shmAsEntdCustsOriginal = objectMapper.convertValue(shmAsEntdCustsOriginal,
				new TypeReference<List<ShmAsEntdCust>>() {
				});

		String userId = "testUser";
		String transactionCd = "testTransaction";

		// Implement your mock behavior and assertions here

		correctionAsMatchedPartyUpdate.getAsMatchedPartiesForTransactions(asMatchedParties,
				shmAsEntdCustsOriginal,
				userId,
				shmShipment,
				transactionCd,
				txnContext);

		// Verify interactions and assertions
	}

}