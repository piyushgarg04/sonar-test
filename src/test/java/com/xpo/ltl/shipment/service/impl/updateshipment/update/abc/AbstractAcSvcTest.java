package com.xpo.ltl.shipment.service.impl.updateshipment.update.abc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.AccessorialService;
import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.ShipmentAcSvcSubDAO;
import com.xpo.ltl.shipment.service.impl.updateshipment.MockParent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractAcSvcTest extends MockParent {

	@Mock
	private EntityManager entityManager;

	@Mock
	private EntityManager db2EntityManager;

	@Mock
	private TransactionContext transactionContext;

	@Mock
	private ShipmentAcSvcSubDAO shipmentAcSvcSubDAO;
	@Mock
	private AppContext appContext;
	@InjectMocks
	private AbstractAcSvc abstractAcSvc = Mockito.spy(new AbstractAcSvc() {
	});

	AbstractAcSvcTest() throws InstantiationException, IllegalAccessException {
	}

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testGetAbcShmAcSvcListToAddWithNewCodes() throws ServiceException {
		String pro = "06420170900";
		List<ShmAcSvc> accessorialServiceList = new ArrayList<>();
		// Add ShmAcSvc instances to the list
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		UpdateShipmentRqst updateShipmentRqst = (UpdateShipmentRqst) jsonStringToObject(UpdateShipmentRqst.class,
				this.getJsonFromProperty(pro + ".jsonUpdateShipmentRqst"));
		updateShipmentRqst = objectMapper.convertValue(updateShipmentRqst, new TypeReference<UpdateShipmentRqst>() {
		});
		List<ShmAcSvc> shmAcSvcs = new ArrayList<>();
		List<AccessorialService> accessorialServices = updateShipmentRqst.getAccessorialServices();
		shmAcSvcs = (List<ShmAcSvc>) jsonStringToObject(accessorialServiceList.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmAcSvc"));
		shmAcSvcs = objectMapper.convertValue(accessorialServiceList, new TypeReference<List<ShmAcSvc>>() {
		});
		ShmShipment shmShipment = (ShmShipment) jsonStringToObject(ShmShipment.class,
				this.getJsonFromProperty(pro + ".jsonShmShipment"));
		shmShipment = objectMapper.convertValue(shmShipment, new TypeReference<ShmShipment>() {
		});

		long seqNumber = 1L;
		String userId = "testUser";
		Timestamp timestamp = new Timestamp(new Date().getTime());

		doNothing().when(shipmentAcSvcSubDAO).persist(anyList(), eq(entityManager));

		abstractAcSvc.getAbcShmAcSvcListToAdd(123L, accessorialServices, shmAcSvcs, shmShipment, seqNumber, userId);

	}

	@Test
	public void testGetAbcShmAcSvcListToAddWithoutNewCodes() throws ServiceException {

		// Add AccessorialService instances to the list
		String pro = "06420170900";
		List<ShmAcSvc> accessorialServiceList = new ArrayList<>();
		// Add ShmAcSvc instances to the list
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		UpdateShipmentRqst updateShipmentRqst = (UpdateShipmentRqst) jsonStringToObject(UpdateShipmentRqst.class,
				this.getJsonFromProperty(pro + ".jsonUpdateShipmentRqst"));
		updateShipmentRqst = objectMapper.convertValue(updateShipmentRqst, new TypeReference<UpdateShipmentRqst>() {
		});
		List<ShmAcSvc> shmAcSvcs = new ArrayList<>();
		List<AccessorialService> accessorialServices = updateShipmentRqst.getAccessorialServices();
		shmAcSvcs = (List<ShmAcSvc>) jsonStringToObject(accessorialServiceList.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmAcSvc"));
		shmAcSvcs = objectMapper.convertValue(accessorialServiceList, new TypeReference<List<ShmAcSvc>>() {
		});
		ShmShipment shmShipment = (ShmShipment) jsonStringToObject(ShmShipment.class,
				this.getJsonFromProperty(pro + ".jsonShmShipment"));
		shmShipment = objectMapper.convertValue(shmShipment, new TypeReference<ShmShipment>() {
		});

		// Set values for ShmShipment

		long seqNumber = 1L;
		String userId = "testUser";
		Timestamp timestamp = new Timestamp(new Date().getTime());

		doNothing().when(shipmentAcSvcSubDAO).persist(anyList(), eq(entityManager));
		abstractAcSvc.getAbcShmAcSvcListToAdd(123L, accessorialServices, shmAcSvcs, shmShipment, seqNumber, userId);

	}

	@Test
	void updateAccessorials() throws ValidationException, NotFoundException {
		String pro = "06420170900";
		List<ShmAcSvc> accessorialServiceList = new ArrayList<>();
		// Add ShmAcSvc instances to the list
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		accessorialServiceList = (List<ShmAcSvc>) jsonStringToObject(accessorialServiceList.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmAcSvc"));
		accessorialServiceList = objectMapper.convertValue(accessorialServiceList, new TypeReference<List<ShmAcSvc>>() {
		});

		doNothing()
				.when(shipmentAcSvcSubDAO)
				.updateDB2ShmAcSvc(any(), any(), eq(db2EntityManager), eq(transactionContext));
		when(appContext.getApplyDb2TwoPhaseCommit()).thenReturn(false);
		abstractAcSvc.updateAccessorials(entityManager,
				db2EntityManager,
				transactionContext,
				accessorialServiceList,
				"TRANSACTION_CD",
				"user");

	}

	@Test
	void setDefaultValuesAcSvc() {
		String userId = "user123";
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());

		ShmAcSvc shmAcSvc = mock(ShmAcSvc.class);

		AbstractAcSvc.setDefaultValuesAcSvc(userId, timestamp, shmAcSvc);

		verify(shmAcSvc).setDmlTmst(timestamp);
		verify(shmAcSvc).setDtlCapxtimestamp(timestamp);
		verify(shmAcSvc).setLstUpdtTmst(timestamp);
		verify(shmAcSvc).setReplLstUpdtTmst(timestamp);
		verify(shmAcSvc).setLstUpdtUid(userId);

		verify(shmAcSvc).setChrgToCd(Mockito.anyString());
		verify(shmAcSvc).setArchiveCntlCd(Mockito.anyString());
		verify(shmAcSvc).setMinChrgInd(Mockito.anyString());
		verify(shmAcSvc).setAcUom(Mockito.anyString());
		verify(shmAcSvc).setDescTxt(Mockito.anyString());

		verify(shmAcSvc).setTrfRt(BigDecimal.ZERO);
		verify(shmAcSvc).setAcQty(BigDecimal.ZERO);
		verify(shmAcSvc).setPpdPct(BigDecimal.ZERO);
	}

	@Test
	void resetSeqNumberAccessorialService() {

		String pro = "06420170900";
		List<ShmAcSvc> shmAcSvcs = new ArrayList();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		shmAcSvcs = (List<ShmAcSvc>) jsonStringToObject(shmAcSvcs.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmAcSvc"));
		shmAcSvcs = objectMapper.convertValue(shmAcSvcs, new TypeReference<List<ShmAcSvc>>() {
		});
		ShmShipment shmShipment = (ShmShipment) jsonStringToObject(ShmShipment.class,
				this.getJsonFromProperty(pro + ".jsonShmShipment"));
		shmShipment = objectMapper.convertValue(shmShipment, new TypeReference<ShmShipment>() {
		});
		List<ShmAcSvc> shmAcSvcsToDeleted = new ArrayList<>();
		List<ShmAcSvc> shmAcSvcsOriginal = new ArrayList<>();
		List<ShmAcSvc> shmAcSvcsToUpdate = new ArrayList<>();
		List<ShmAcSvc> shmAcSvcsToAdd = new ArrayList<>();
		shmAcSvcsToDeleted.add(shmAcSvcs.get(0));
		shmAcSvcsOriginal.addAll(shmAcSvcs);
		shmAcSvcsToUpdate.add(shmAcSvcs.get(1));
		shmAcSvcsToAdd.add(shmAcSvcs.get(2));
		List<ShmAcSvc> result = abstractAcSvc.resetAbcSeqNumberShmAcSvcList(shmShipment.getShpInstId(),
				shmAcSvcsToDeleted,
				shmAcSvcsOriginal,
				shmAcSvcsToUpdate,
				shmAcSvcsToAdd,
				false,
				"user",
				ShipmentUpdateActionCd.MANUAL_RATE);
		assertNotNull(result);
	}

	@Test
	void compareShmAcSvc() {
	}
}