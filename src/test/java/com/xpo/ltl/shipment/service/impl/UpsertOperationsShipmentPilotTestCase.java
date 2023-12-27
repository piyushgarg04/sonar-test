package com.xpo.ltl.shipment.service.impl;

import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.Calendar;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.OperationsShipmentPilot;
import com.xpo.ltl.api.shipment.v2.UpsertOperationsShipmentPilotResp;
import com.xpo.ltl.api.shipment.v2.UpsertOperationsShipmentPilotRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShipmentOpsShmPilotSubDAO;

import junit.framework.TestCase;

public class UpsertOperationsShipmentPilotTestCase extends TestCase {
	
	@Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

	@Mock
	private AuditInfo auditInfo;

	
	@Mock
	private ShipmentOpsShmPilotSubDAO shipmentOpsShmPilotSubDAO;

	@InjectMocks
	private OperationsShipmentImplPilot operationsShipmentImplPilot;

	@Override
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		when(txnContext.getSrcApplicationId()).thenReturn("JUNIT");

		final User user = new User();
		user.setUserId("JUNIT");
		user.setEmployeeId("JUNIT");
		when(txnContext.getUser()).thenReturn(user);

		when(txnContext.getTransactionTimestamp())
				.thenReturn(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance()));

		when(txnContext.getCorrelationId()).thenReturn("0");


	}

	@Test
	public void testUpdate_RequestRequired() throws Exception {
		try {
			operationsShipmentImplPilot.upsertOperationShipmentPilot(null, txnContext, entityManager);
			
			fail("Expected an exception.");
		} catch (final Exception e) {
			assertEquals("The request is required.", e.getMessage());
		}
	}

	@Test
	public void testUpdate_TransactionContextRequired() throws Exception {
		try {
			final UpsertOperationsShipmentPilotRqst request = new UpsertOperationsShipmentPilotRqst();
			operationsShipmentImplPilot.upsertOperationShipmentPilot(request, null, entityManager);

			fail("Expected an exception");
		} catch (final Exception e) {
			assertEquals("The TransactionContext is required.", e.getMessage());
		}
	}

	@Test
	public void testUpdate_EntityManagerRequired() throws Exception {
		try {
			final UpsertOperationsShipmentPilotRqst request = new UpsertOperationsShipmentPilotRqst();
			operationsShipmentImplPilot.upsertOperationShipmentPilot(request, txnContext,null);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertEquals("The EntityManager is required.", e.getMessage());
		}
	}
	
	@Test
	public void testUpdate_ShipmentID_Required() throws Exception {
		try {
			final UpsertOperationsShipmentPilotRqst request = new UpsertOperationsShipmentPilotRqst();
			operationsShipmentImplPilot.upsertOperationShipmentPilot(request, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertEquals("Operations shipment is required.", e.getMessage());
		}
	}
	
	
	@Test
	public void testUpdate_upsertShipmentSuccess() throws Exception {
		try {
			final UpsertOperationsShipmentPilotRqst request = new UpsertOperationsShipmentPilotRqst();
			
			OperationsShipmentPilot os = new OperationsShipmentPilot();
			os.setShipmentInstId(5444611088940l);
			os.setPriorityNbr(BigInteger.valueOf(100));
			os.setHotShipmentInd(false);
			os.setCorrelationId("323636374748");
			
			request.setOperationsShipmentPilot(os);
			
			UpsertOperationsShipmentPilotResp resp = operationsShipmentImplPilot.upsertOperationShipmentPilot(request, txnContext, entityManager);
			System.out.println(resp.toString());

		} catch (final Exception e) {
		}
	}

	
}
