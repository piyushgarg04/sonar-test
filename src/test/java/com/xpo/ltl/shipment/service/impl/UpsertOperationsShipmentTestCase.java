package com.xpo.ltl.shipment.service.impl;

import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.service.entity.ShmOpsShipment;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.OperationsShipment;
import com.xpo.ltl.api.shipment.v2.RouteTypeCd;
import com.xpo.ltl.api.shipment.v2.UpsertOperationsShipmentResp;
import com.xpo.ltl.api.shipment.v2.UpsertOperationsShipmentRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShipmentOpsShmSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;

import junit.framework.TestCase;

public class UpsertOperationsShipmentTestCase extends TestCase {
	private static final String ROUTE_PREFIX = "TEST";
	private static final String ROUTE_SUFFIX = "TEST";
	private static final RouteTypeCd ROUTE_TYPE_CD = RouteTypeCd.ROUTED;
	private List<MoreInfo> moreInfos;

	@Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

	@Mock
	private EntityManager db2EntityManager;

	@Mock
	private AuditInfo auditInfo;


	private ShmShipmentSubDAO shipmentDAO;
	
	@Mock
	private ShipmentOpsShmSubDAO shipmentOpsShmSubDAO;

	@InjectMocks
	private OperationsShipmentImpl operationsShipmentImpl;

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
		moreInfos = Lists.newArrayList();
		

	}

	@Test
	public void testUpdate_RequestRequired() throws Exception {
		try {
			operationsShipmentImpl.upsertOperationShipment(null, txnContext, entityManager);
			
			fail("Expected an exception.");
		} catch (final Exception e) {
			assertEquals("The request is required.", e.getMessage());
		}
	}

	@Test
	public void testUpdate_TransactionContextRequired() throws Exception {
		try {
			final UpsertOperationsShipmentRqst request = new UpsertOperationsShipmentRqst();
			operationsShipmentImpl.upsertOperationShipment(request, null, entityManager);

			fail("Expected an exception");
		} catch (final Exception e) {
			assertEquals("The TransactionContext is required.", e.getMessage());
		}
	}

	@Test
	public void testUpdate_EntityManagerRequired() throws Exception {
		try {
			final UpsertOperationsShipmentRqst request = new UpsertOperationsShipmentRqst();
			operationsShipmentImpl.upsertOperationShipment(request, txnContext,null);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertEquals("The EntityManager is required.", e.getMessage());
		}
	}
	
	@Test
	public void testUpdate_ShipmentID_Required() throws Exception {
		try {
			final UpsertOperationsShipmentRqst request = new UpsertOperationsShipmentRqst();
	
			operationsShipmentImpl.upsertOperationShipment(request, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertEquals("Operations shipment is required.", e.getMessage());
		}
	}
	
	
	@Test
	public void testUpdate_upsertShipmentSuccess() throws Exception {
		try {
			final UpsertOperationsShipmentRqst request = new UpsertOperationsShipmentRqst();
			
			OperationsShipment os = new OperationsShipment();
			os.setShipmentInstId(5444611088940l);
			os.setPriorityNbr(BigInteger.valueOf(100));
			os.setHotShipmentInd(false);
			os.setCorrelationId("323636374748");
			
			request.setOperationsShipment(os);
			
			UpsertOperationsShipmentResp resp = operationsShipmentImpl.upsertOperationShipment(request, txnContext, entityManager);
			System.out.println(resp.toString());

		} catch (final Exception e) {
		}
	}

	
}
