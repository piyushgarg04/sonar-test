package com.xpo.ltl.shipment.service.impl.updateshipment.update.correction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmCommodity;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.Commodity;
import com.xpo.ltl.api.shipment.v2.LineItemChargeToCd;
import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.impl.updateshipment.MockParent;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.common.CommodityCommonImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.LoadValFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CorrectionCommodityImplTest extends MockParent {

	@InjectMocks
	private CommodityCommonImpl correctionCommodity;
	@InjectMocks
	private CorrectionsLoadValuesFactory correctionsLoadValuesFactory = mock(CorrectionsLoadValuesFactory.class);
	@Mock
	private TransactionContext mockTransactionContext = mock(TransactionContext.class);
	@Mock
	private LoadValFactory loadValFactory = mock(LoadValFactory.class, Answers.CALLS_REAL_METHODS.get());
	@BeforeEach
	void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testCfGetCommoditiesToInsert() throws ServiceException {
		String pro = "06420172510";
		List<ShmCommodity> shmCommodity = new ArrayList();

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

		shmCommodity = (List<ShmCommodity>) jsonStringToObject(shmCommodity.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmCommodity"));
		shmCommodity = objectMapper.convertValue(shmCommodity, new TypeReference<List<ShmCommodity>>() {
		});

		// Mock the ServiceException
		ServiceException mockServiceException = mock(ServiceException.class);
		//		when(correctionCommodity.getCommoditiesToInsert(any(), any(), any())).thenThrow(mockServiceException);
		//
		// Act
		CompletableFuture<List<ShmCommodity>> cfResult = correctionCommodity.cfGetCommoditiesToInsert(updateShipmentRqst.getCommodities(),
				shmCommodity,
				shmShipment,
				mockTransactionContext);
		// Assert
		Assertions.assertNotNull(cfResult);
	}

	@Test
	public void testGetAllTx() throws ServiceException {

		//		List<String> pros = Arrays.asList(getJsonFromProperty("correctionsPros").split(","));
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		//		pros.forEach(pro -> {
		//			System.out.println(pro+": "+shmCommodity.size());
		//
		//		});
		// Arrange
		when(loadValFactory.getFactoryImplementation(ShipmentUpdateActionCd.CORRECTION)).thenReturn(
				correctionsLoadValuesFactory);
		String pro = "05260988803";
		List<ShmCommodity> shmCommodity = new ArrayList();
		UpdateShipmentRqst updateShipmentRqst = (UpdateShipmentRqst) jsonStringToObject(UpdateShipmentRqst.class,
				this.getJsonFromProperty(pro + ".jsonUpdateShipmentRqst"));
		updateShipmentRqst = objectMapper.convertValue(updateShipmentRqst, new TypeReference<UpdateShipmentRqst>() {
		});

		ShmShipment shmShipment = (ShmShipment) jsonStringToObject(ShmShipment.class,
				this.getJsonFromProperty(pro + ".jsonShmShipment"));
		shmShipment = objectMapper.convertValue(shmShipment, new TypeReference<ShmShipment>() {
		});
		shmCommodity = (List<ShmCommodity>) jsonStringToObject(shmCommodity.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmCommodity"));
		shmCommodity = objectMapper.convertValue(shmCommodity, new TypeReference<List<ShmCommodity>>() {
		});

		String mockUserId = "testUser";
		String mockTransactionCd = "testTransaction";

		// Mock methods used in the implementation
		List<ShmCommodity> mockCommoditiesToInsert = Arrays.asList(mock(ShmCommodity.class), mock(ShmCommodity.class));
		List<ShmCommodity> mockCommoditiesToUpdate = Arrays.asList(mock(ShmCommodity.class), mock(ShmCommodity.class));
		List<Commodity> commodities = new ArrayList<>();
		commodities.add(updateShipmentRqst.getCommodities().get(0));
		updateShipmentRqst.setCommodities(commodities);
		updateShipmentRqst
				.getCommodities()
				.forEach(commodity -> commodity.setDescription(commodity.getDescription() + " X"));
		Commodity commodity = new Commodity();
		commodity.setDescription("Desc");
		commodity.setAmount(0.0);
		commodity.setChrgToCd(LineItemChargeToCd.PPD);
		commodity.setShipmentInstId(shmShipment.getShpInstId());
		commodity.setTariffsRate(0.0);
		updateShipmentRqst.getCommodities().add(commodity);
		// Act Collections.singletonList()
//		Map<ActionCd, List<ShmCommodity>> result = correctionCommodity.getAllTx(shmCommodity,
//				shmShipment,
//				updateShipmentRqst,
//				mockUserId,
//				mockTransactionCd,
//				ShipmentUpdateActionCd.CORRECTION);
//
//		// Assert
//		Assertions.assertNotNull(result);
//
//		List<ShmCommodity> addResult = result.get(ActionCd.ADD);
//		List<ShmCommodity> updateResult = result.get(ActionCd.UPDATE);
//		List<ShmCommodity> deleteResult = result.get(ActionCd.DELETE);
//
//		// Verify expected values in the results
//		Assertions.assertEquals(2, addResult.size());
//		Assertions.assertNull(updateResult);
//		Assertions.assertEquals(2, deleteResult.size());
	}
}