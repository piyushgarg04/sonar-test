package com.xpo.ltl.shipment.service.impl.updateshipment.update.abc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmCommodity;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.ShmCommoditySubDAO;
import com.xpo.ltl.shipment.service.impl.updateshipment.MockParent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class AbstractCommodityTest extends MockParent {

	@Mock
	private EntityManager entityManager;

	@Mock
	private EntityManager db2EntityManager;

	@Mock
	private TransactionContext transactionContext;

	@Mock
	private ShmCommoditySubDAO shmCommoditySubDAO;
	@Mock
	private AppContext appContext;
	@InjectMocks
	private AbstractCommodity abstractCommodity = Mockito.spy(new AbstractCommodity() {
	});

	AbstractCommodityTest() throws InstantiationException, IllegalAccessException {
	}

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		when(appContext.getDb2CommitEnabledForUpdateShipment()).thenReturn(false);
	}

	@Test
	void updateCommodities() throws ValidationException, NotFoundException {
		String pro = "06340056883";
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		List<ShmCommodity> commodities = new ArrayList<>();
		// Add ShmCommodity instances to the list
		commodities = (List<ShmCommodity>) jsonStringToObject(commodities.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmCommodity"));
		commodities = objectMapper.convertValue(commodities, new TypeReference<List<ShmCommodity>>() {
		});
		String transactionCd = "testTransaction";

		doNothing()
				.when(shmCommoditySubDAO)
				.updateDB2ShmCommodity(any(), any(), eq(db2EntityManager), eq(transactionContext));
		abstractCommodity.updateCommodities(entityManager,
				db2EntityManager,
				transactionContext,
				commodities,
				transactionCd,
				"user");

	}

	@Test
	public void testSetCommodityDefaultValues() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		ShmCommodity shmCommodity = new ShmCommodity();

		abstractCommodity.setCommodityDefaultValues(shmCommodity);
		assertNotNull(shmCommodity.getAmt());

	}

	@Test
	public void testResetSeqNumberCommodities() {
		String pro = "06340056883";
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		ShmShipment shmShipment = (ShmShipment) jsonStringToObject(ShmShipment.class,
				this.getJsonFromProperty(pro + ".jsonShmShipment"));
		shmShipment = objectMapper.convertValue(shmShipment, new TypeReference<ShmShipment>() {
		});
		List<ShmCommodity> inputCommodities = new ArrayList<>();
		inputCommodities = (List<ShmCommodity>) jsonStringToObject(inputCommodities.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmCommodity"));
		inputCommodities = objectMapper.convertValue(inputCommodities, new TypeReference<List<ShmCommodity>>() {
		});
		List<ShmCommodity> result = AbstractCommodity.resetSeqNumberCommodities(shmShipment.getShpInstId(),
				inputCommodities, shmShipment);

		assertFalse(result.isEmpty()); // Ensure the result list has the expected size
	}

	@Test
	public void testGetAbcCommoditiesForInsert() throws ServiceException {
		String pro = "06340056883";
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		ShmShipment shmShipment = (ShmShipment) jsonStringToObject(ShmShipment.class,
				this.getJsonFromProperty(pro + ".jsonShmShipment"));
		shmShipment = objectMapper.convertValue(shmShipment, new TypeReference<ShmShipment>() {
		});
		UpdateShipmentRqst updateShipmentRqst = (UpdateShipmentRqst) jsonStringToObject(UpdateShipmentRqst.class,
				this.getJsonFromProperty(pro + ".jsonUpdateShipmentRqst"));
		updateShipmentRqst = objectMapper.convertValue(updateShipmentRqst, new TypeReference<UpdateShipmentRqst>() {
		});
		// Prepare mock behavior for ShmShipment

		updateShipmentRqst.getCommodities().forEach(commodity -> commodity.setSequenceNbr(null));
		// Execute the method
		List<ShmCommodity> result = abstractCommodity.getAbcCommoditiesForInsert(shmShipment,
				updateShipmentRqst.getCommodities());

		assertFalse(result.isEmpty()); // Ensure the result list has the expected size

	}

	@Test
	public void testDeleteCommodities() throws ValidationException, NotFoundException {
		MockitoAnnotations.initMocks(this);

		String pro = "06340056883";
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		List<ShmCommodity> inputCommodities = new ArrayList<>();
		inputCommodities = (List<ShmCommodity>) jsonStringToObject(inputCommodities.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmCommodity"));
		inputCommodities = objectMapper.convertValue(inputCommodities, new TypeReference<List<ShmCommodity>>() {
		});
		doNothing().when(shmCommoditySubDAO).remove(inputCommodities, entityManager);
		doNothing().when(shmCommoditySubDAO).deleteDB2ShmCommodity(any(), any(), any(), any());

		// Execute the method
		abstractCommodity.deleteCommodities(entityManager, db2EntityManager, transactionContext, inputCommodities);

	}
	@Test
	void updateCommoditiesException() throws ValidationException, NotFoundException {
		String pro = "06340056883";
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		List<ShmCommodity> commodities = new ArrayList<>();
		// Add ShmCommodity instances to the list
		commodities = (List<ShmCommodity>) jsonStringToObject(commodities.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmCommodity"));
		commodities = objectMapper.convertValue(commodities, new TypeReference<List<ShmCommodity>>() {
		});
		String transactionCd = "testTransaction";

		doNothing()
				.when(shmCommoditySubDAO)
				.updateDB2ShmCommodity(any(), any(), eq(db2EntityManager), eq(transactionContext));
		abstractCommodity.updateCommodities(entityManager,
				db2EntityManager,
				transactionContext,
				commodities,
				transactionCd,
				"user");
		// Mock the behavior of the DAO to throw the desired exception
		NotFoundException notFoundExceptionExceptionBuilder = ExceptionBuilder
				.exception(NotFoundErrorMessage.COMMODITY_NF, transactionContext)
				.moreInfo("Commodities", NotFoundErrorMessage.COMMODITY_NF)
				.build();
		doThrow(notFoundExceptionExceptionBuilder).when(shmCommoditySubDAO).updateDB2ShmCommodity(any(), any(), any(), any());
		when(appContext.getDb2CommitEnabledForUpdateShipment()).thenReturn(true);


		List<ShmCommodity> finalCommodities = commodities;
		final UnsupportedOperationException unsupportedOperationException = assertThrows(UnsupportedOperationException.class, () -> {
			abstractCommodity.updateCommodities(entityManager, entityManager, transactionContext,
					finalCommodities, "testTransactionCd",
					"user");

		});

		assertEquals(
				"com.xpo.ltl.api.exception.ServiceException: SHMN041-905E:Commodity update failed in {0}.  Contact Systems Support.(location:Commodities, message:SHMN010-905E:Commodity not found.(location:Commodities, message:Commodity not found., errorCode:SHMN010-905E))",
				unsupportedOperationException
						.getMessage());

	}
}