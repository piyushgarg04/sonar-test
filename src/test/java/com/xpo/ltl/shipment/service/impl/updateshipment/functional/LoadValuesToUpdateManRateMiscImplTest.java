package com.xpo.ltl.shipment.service.impl.updateshipment.functional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem;
import com.xpo.ltl.shipment.service.impl.updateshipment.MockParent;
import org.apache.commons.collections.ArrayStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class LoadValuesToUpdateManRateMiscImplTest extends MockParent {

	@InjectMocks
	private LoadValuesToUpdateManRateMiscImpl loadValuesToUpdateManRateMiscImpl;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testLoad() {
		String pro = "06420172510";
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		List<ShmMiscLineItem> shmMiscLineItems = new ArrayStack();
		shmMiscLineItems = (List<ShmMiscLineItem>) jsonStringToObject(shmMiscLineItems.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmMiscLineItem"));
		shmMiscLineItems = objectMapper.convertValue(shmMiscLineItems, new TypeReference<List<ShmMiscLineItem>>() {
		});

		List<ShmMiscLineItem> shmMiscLineItemsToInsert = new ArrayList<>();
		List<ShmMiscLineItem> shmMiscLineItemsUpdated = new ArrayList<>();
		shmMiscLineItemsToInsert.add(shmMiscLineItems.get(0));
		shmMiscLineItemsUpdated.add(shmMiscLineItems.get(0));
		boolean fromDelete = false;
		String racfId = "testRacfId";

		// Call the method under test
		List<ShmMiscLineItem> result = loadValuesToUpdateManRateMiscImpl.load(shmMiscLineItemsToInsert,
				shmMiscLineItemsUpdated,
				fromDelete,
				racfId,
				"2");

		// Perform assertions or verifications as needed
		assertNotNull(result);

	}

}