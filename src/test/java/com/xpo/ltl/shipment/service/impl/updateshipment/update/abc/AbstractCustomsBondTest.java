package com.xpo.ltl.shipment.service.impl.updateshipment.update.abc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmCustomsBond;
import com.xpo.ltl.api.shipment.service.entity.ShmCustomsBond;
import com.xpo.ltl.api.shipment.service.entity.ShmCustomsBondPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.ShipmentCustomsSubDAO;
import com.xpo.ltl.shipment.service.impl.updateshipment.MockParent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class AbstractCustomsBondTest extends MockParent {

	@Mock
	private EntityManager entityManagerMock;
	@InjectMocks
	private AbstractCustomsBond abstractCustomsBond = Mockito.spy(new AbstractCustomsBond() {
	});
	@Mock
	private TransactionContext txnContext;

	@Mock
	private ShipmentCustomsSubDAO shipmentCustomsSubDAO;
	@Mock
	private AppContext appContext;
	AbstractCustomsBondTest() throws InstantiationException, IllegalAccessException {
	}

	@BeforeEach
	void setUp() {
		MockitoAnnotations.initMocks(this);
		when(appContext.getApplyDb2TwoPhaseCommit()).thenReturn(false);
	}

	@Test
	public void testResetSeqNumberCustomsBond() {
		String pro = "05260988803";
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		List<ShmCustomsBond> shmCustomsBonds = new ArrayList<>();
		shmCustomsBonds = (List<ShmCustomsBond>) jsonStringToObject(shmCustomsBonds.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmCustomsBond"));
		shmCustomsBonds = objectMapper.convertValue(shmCustomsBonds, new TypeReference<List<ShmCustomsBond>>() {
		});
		ShmShipment shmShipment = (ShmShipment) jsonStringToObject(ShmShipment.class,
				this.getJsonFromProperty(pro + ".jsonShmShipment"));
		shmShipment = objectMapper.convertValue(shmShipment, new TypeReference<ShmShipment>() {
		});

		List<ShmCustomsBond> result = AbstractCustomsBond.resetSeqNumberCustomsBond(shmShipment.getShpInstId(),
				shmCustomsBonds);

		assertFalse(result.isEmpty());
	}

	@Test
	public void testDeleteCustomsBond() throws ValidationException, NotFoundException {

		String pro = "05260988803";
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		List<ShmCustomsBond> customsBondList = new ArrayList<>();
		customsBondList = (List<ShmCustomsBond>) jsonStringToObject(customsBondList.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmCustomsBond"));
		customsBondList = objectMapper.convertValue(customsBondList, new TypeReference<List<ShmCustomsBond>>() {
		});

		doNothing().when(shipmentCustomsSubDAO).remove(customsBondList, entityManagerMock);
		doNothing().when(shipmentCustomsSubDAO).deleteDB2ShmCustomsBond(any(ShmCustomsBondPK.class),
				any(Function.class),
				any(EntityManager.class),
				any(TransactionContext.class));

		abstractCustomsBond.deleteCustomsBond(entityManagerMock, entityManagerMock, txnContext, customsBondList);

	}

	@Test
	public void testUpdateCustomsBond() throws ValidationException, NotFoundException {

		String pro = "05260988803";
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		List<ShmCustomsBond> customsBondList = new ArrayList<>();
		customsBondList = (List<ShmCustomsBond>) jsonStringToObject(customsBondList.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmCustomsBond"));
		customsBondList = objectMapper.convertValue(customsBondList, new TypeReference<List<ShmCustomsBond>>() {
		});

		doNothing().when(shipmentCustomsSubDAO).persist(customsBondList, entityManagerMock);
		doNothing().when(shipmentCustomsSubDAO).updateDB2ShmCustomsBond(any(ShmCustomsBond.class),
				any(Function.class),
				any(EntityManager.class),
				any(TransactionContext.class));

		abstractCustomsBond.updateCustomsBond(entityManagerMock, entityManagerMock, txnContext, customsBondList, "TRX");

	}

	@Test
	public void testInsertCustomsBond() throws ValidationException, NotFoundException {

		String pro = "05260988803";
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		List<ShmCustomsBond> customsBondList = new ArrayList<>();
		customsBondList = (List<ShmCustomsBond>) jsonStringToObject(customsBondList.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmCustomsBond"));
		customsBondList = objectMapper.convertValue(customsBondList, new TypeReference<List<ShmCustomsBond>>() {
		});

		doNothing().when(shipmentCustomsSubDAO).persist(customsBondList, entityManagerMock);
		when(shipmentCustomsSubDAO.createDB2ShmCustomsBond(any(ShmCustomsBond.class),
				any(EntityManager.class))).thenReturn(new DB2ShmCustomsBond());

		abstractCustomsBond.insertCustomsBond(entityManagerMock, entityManagerMock, txnContext, customsBondList, "TRX");

	}
}