package com.xpo.ltl.shipment.service.impl.updateshipment.update.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.AccessorialService;
import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.impl.updateshipment.MockParent;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.LoadValues;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.correction.CorrectionLoadAccessorialValuesImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.correction.CorrectionsLoadValuesFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.LoadValFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AcSvcUpdateCommonImplTest extends MockParent {

	@InjectMocks
	private AcSvcUpdateCommonImpl acSvcUpdateCommon;

	@Mock
	private TransactionContext txnContext;
	@Mock
	private EntityManager entityManager;

	@InjectMocks
	private LoadValFactory loadValFactory = mock(LoadValFactory.class, Answers.CALLS_REAL_METHODS.get());

	@InjectMocks
	private CorrectionsLoadValuesFactory correctionsLoadValuesFactory = mock(CorrectionsLoadValuesFactory.class,
			Answers.CALLS_REAL_METHODS.get());

	@InjectMocks
	private CorrectionLoadAccessorialValuesImpl acSvcLoadValues;

	@Mock(answer = Answers.CALLS_REAL_METHODS)
	private LoadValues<AccessorialService, ShmAcSvc> mockLoadValues;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testGetItemsForTransactions() throws Exception {
		when(loadValFactory.getFactoryImplementation(ShipmentUpdateActionCd.CORRECTION)).thenReturn(
				correctionsLoadValuesFactory);

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

		Long shipmentInstId = 1L;
		List<AccessorialService> accessorialServiceList = new ArrayList<>();
		accessorialServiceList.add(updateShipmentRqst.getAccessorialServices().get(0));
		List<ShmAcSvc> shmAcSvcsOriginal = new ArrayList<>();
		shmAcSvcsOriginal = (List<ShmAcSvc>) jsonStringToObject(shmAcSvcsOriginal.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmAcSvc"));
		shmAcSvcsOriginal = objectMapper.convertValue(shmAcSvcsOriginal, new TypeReference<List<ShmAcSvc>>() {
		});

		String userId = "testUser";
		ShipmentUpdateActionCd shipmentUpdateActionCd = ShipmentUpdateActionCd.CORRECTION;
		String transactionCode = "testTransaction";
		TransactionContext transactionContext = new TransactionContext();

		// Implement your mock behavior and assertions here
		doReturn(acSvcLoadValues).when(correctionsLoadValuesFactory).getFactoryImplementation(ShmAcSvc.class);
		acSvcUpdateCommon.getItemsForTransactions(shipmentInstId,
				accessorialServiceList,
				shmAcSvcsOriginal,
				shmShipment,
				userId,
				shipmentUpdateActionCd,
				transactionCode,
				transactionContext);

		// Verify interactions and assertions
	}

	@Test
	public void testGetShmAcSvcListToUpdate() throws Exception {
		when(loadValFactory.getFactoryImplementation(ShipmentUpdateActionCd.CORRECTION)).thenReturn(
				correctionsLoadValuesFactory);
		doReturn(acSvcLoadValues).when(correctionsLoadValuesFactory).getFactoryImplementation(ShmAcSvc.class);
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

		Long shipmentInstId = 1L;
		List<AccessorialService> accessorialServiceList = new ArrayList<>();
		accessorialServiceList.add(updateShipmentRqst.getAccessorialServices().get(0));
		List<ShmAcSvc> shmAcSvcsOriginal = new ArrayList<>();
		shmAcSvcsOriginal = (List<ShmAcSvc>) jsonStringToObject(shmAcSvcsOriginal.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmAcSvc"));
		shmAcSvcsOriginal = objectMapper.convertValue(shmAcSvcsOriginal, new TypeReference<List<ShmAcSvc>>() {
		});
		updateShipmentRqst
				.getAccessorialServices()
				.forEach(accessorialService -> accessorialService.setDescription(
						accessorialService.getDescription() + "1"));

		// Get the private method using reflection
		Method method = AcSvcUpdateCommonImpl.class.getDeclaredMethod("getShmAcSvcListToUpdate",
				List.class,
				List.class,
				boolean.class,
				String.class,
				ShipmentUpdateActionCd.class,
				String.class,
				TransactionContext.class);
		method.setAccessible(true);  // Set the method to be accessible

		// Call the private method with mock data
		List<ShmAcSvc> result = (List<ShmAcSvc>) method.invoke(acSvcUpdateCommon,
				updateShipmentRqst.getAccessorialServices(),
				shmAcSvcsOriginal,
				false,
				"racfId",
				ShipmentUpdateActionCd.CORRECTION,
				"transactionCode",
				txnContext);
		List<ShmAcSvc> shmAcSvcsOriginal2 = new ArrayList<>();
		shmAcSvcsOriginal2 = (List<ShmAcSvc>) jsonStringToObject(shmAcSvcsOriginal2.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmAcSvc"));
		shmAcSvcsOriginal2 = objectMapper.convertValue(shmAcSvcsOriginal2, new TypeReference<List<ShmAcSvc>>() {
		});

		result = (List<ShmAcSvc>) method.invoke(acSvcUpdateCommon,
				updateShipmentRqst.getAccessorialServices(),
				shmAcSvcsOriginal2,
				true,
				"racfId",
				ShipmentUpdateActionCd.CORRECTION,
				"transactionCode",
				txnContext);
		// Add your assertions here
	}
}