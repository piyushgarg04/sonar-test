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
import com.xpo.ltl.api.shipment.service.entity.ShmOpsShipmentPilot;
import com.xpo.ltl.shipment.service.dao.ShipmentOpsShmPilotSubDAO;

public class OperationsShipmentImplPilotTest {

	private static final List<ShmOpsShipmentPilot> SHIPMENT_PILOT_LIST = new ArrayList<>();
	private static final long SHIPMENT_PILOT_ID = 8998717L;

	static {
		ShmOpsShipmentPilot shipmentPilotMock = new ShmOpsShipmentPilot();
		shipmentPilotMock.setShpInstId(SHIPMENT_PILOT_ID);
		SHIPMENT_PILOT_LIST.add(shipmentPilotMock);
	}

	@Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

	@Mock
	private ShipmentOpsShmPilotSubDAO shipmentOpsShmPilotSubDAO;

	@InjectMocks
	private OperationsShipmentImplPilot operationsShipmentImplPilot;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		when(shipmentOpsShmPilotSubDAO.findById(SHIPMENT_PILOT_ID, entityManager))
				.thenAnswer(new Answer<ShmOpsShipmentPilot>() {
					@Override
					public ShmOpsShipmentPilot answer(InvocationOnMock invocation) throws Throwable {
						return SHIPMENT_PILOT_LIST.stream()
								.filter(shm -> Objects.equals(shm.getShpInstId(), invocation.getArguments()[0]))
								.findFirst().orElse(null);
					}
				});

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				SHIPMENT_PILOT_LIST.remove(invocation.getArguments()[0]);
				return null;
			}
		}).when(shipmentOpsShmPilotSubDAO).remove(any(ShmOpsShipmentPilot.class), any(EntityManager.class));
	}

	@Test
	public void shipmentInstIdRequiredTest() {
		NullPointerException nullPointerExceptionResult = assertThrows(NullPointerException.class,
				() -> operationsShipmentImplPilot.deleteOperationsShipmentPilot(null, txnContext, entityManager));
		assertEquals("The shipmentInstId is required.", nullPointerExceptionResult.getMessage());
	}

	@Test
	public void shipmentPilotNotFoundedTest() {
		final long shipmentPilotId = 100001L;
		NotFoundException notFoundExceptionResult = assertThrows(NotFoundException.class,
				() -> operationsShipmentImplPilot.deleteOperationsShipmentPilot(shipmentPilotId, txnContext,
						entityManager));
		assertEquals("SHMN010-944E:Shipment not found.", notFoundExceptionResult.getMessage());
	}

	@Test
	public void deleteShipmentPilotTest() throws ServiceException {
		operationsShipmentImplPilot.deleteOperationsShipmentPilot(SHIPMENT_PILOT_ID, txnContext, entityManager);
		assertNull(shipmentOpsShmPilotSubDAO.findById(SHIPMENT_PILOT_ID, entityManager));
		assertEquals(0, SHIPMENT_PILOT_LIST.size());
	}

}
