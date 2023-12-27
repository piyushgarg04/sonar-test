package com.xpo.ltl.shipment.service.impl.updateshipment.functional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.shipment.service.impl.updateshipment.MockParent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class LoadValuesToUpdateManRateAcSvcImplTest extends MockParent {

	@InjectMocks
	private LoadValuesToUpdateManRateAcSvcImpl loadValuesToUpdateManRateAcSvcImpl;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testLoad() {
		String pro = "06420172510";
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		List<ShmAcSvc> shmAcSvcs = new ArrayList<>();
		shmAcSvcs = (List<ShmAcSvc>) jsonStringToObject(shmAcSvcs.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmAcSvc"));
		shmAcSvcs = objectMapper.convertValue(shmAcSvcs, new TypeReference<List<ShmAcSvc>>() {
		});
		// Setup test data
		List<ShmAcSvc> shmAcSvcToInsert = new ArrayList<>();
		List<ShmAcSvc> shmAcSvcUpdated = new ArrayList<>();
		shmAcSvcToInsert.add(shmAcSvcs.get(0));
		shmAcSvcUpdated.add(shmAcSvcs.get(0));
		boolean fromDelete = false;
		String racfId = "testRacfId";

		List<ShmAcSvc> result = loadValuesToUpdateManRateAcSvcImpl.load(shmAcSvcToInsert,
				shmAcSvcUpdated,
				fromDelete,
				racfId,
				"2");
		assertNotNull(result);
	}
}