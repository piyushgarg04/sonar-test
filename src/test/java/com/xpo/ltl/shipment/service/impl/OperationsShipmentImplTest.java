package com.xpo.ltl.shipment.service.impl;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.any;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmOpsShipment;
import com.xpo.ltl.shipment.service.dao.ShipmentOpsShmSubDAO;

public class OperationsShipmentImplTest {

	private static final List<ShmOpsShipment> SHIPMENT_LIST = new ArrayList<>();

	private static final long SHIPMENT_INST_ID = 100980990L;

	static {
		ShmOpsShipment shipmentMock = new ShmOpsShipment();
		shipmentMock.setShpInstId(SHIPMENT_INST_ID);
		SHIPMENT_LIST.add(shipmentMock);
	}

	@Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

	@Mock
	private ShipmentOpsShmSubDAO shipmentOpsShmSubDAO;

	@InjectMocks
	private OperationsShipmentImpl operationsShipmentImpl;

	@Before
	public void setUp() {

		MockitoAnnotations.initMocks(this);

		when(shipmentOpsShmSubDAO.findById(SHIPMENT_INST_ID, entityManager)).thenAnswer(new Answer<ShmOpsShipment>() {
			@Override
			public ShmOpsShipment answer(InvocationOnMock invocation) throws Throwable {
				return SHIPMENT_LIST.stream()
						.filter(shm -> Objects.equals(shm.getShpInstId(), invocation.getArguments()[0])).findFirst()
						.orElse(null);
			}
		});

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				SHIPMENT_LIST.remove(invocation.getArguments()[0]);
				return null;
			}
		}).when(shipmentOpsShmSubDAO).remove(any(ShmOpsShipment.class), any(EntityManager.class));
	}

	@Test
	public void deleteShipmentTest() throws ServiceException {
		operationsShipmentImpl.deleteOperationsShipment(SHIPMENT_INST_ID, txnContext, entityManager);
		assertNull(shipmentOpsShmSubDAO.findById(SHIPMENT_INST_ID, entityManager));
		assertEquals(0, SHIPMENT_LIST.size());
	}

	@Test
	public void shipmentInstIdRequiredTest() {
		NullPointerException nullPointerExceptionResult = assertThrows(NullPointerException.class,
				() -> operationsShipmentImpl.deleteOperationsShipment(null, txnContext, entityManager));
		assertEquals("The shipmentInstId is required.", nullPointerExceptionResult.getMessage());
	}

	@Test
	public void shipmentNotFoundedTest() {
		final long shipmentId = 234571L;
		NotFoundException notFoundExceptionResult = assertThrows(NotFoundException.class,
				() -> operationsShipmentImpl.deleteOperationsShipment(shipmentId, txnContext, entityManager));
		assertEquals("SHMN010-944E:Shipment not found.", notFoundExceptionResult.getMessage());
	}

}
