package com.xpo.ltl.shipment.service.impl;

import static org.mockito.Mockito.when;

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
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.RouteTypeCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentsWithRouteDtlRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;

import junit.framework.TestCase;

public class UpdateShipmentsWithRouteDtlImplTestCase extends TestCase {
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

	@Mock
	private ShmShipmentSubDAO shipmentDAO;

	@InjectMocks
	private UpdateShipmentsWithRouteDtlImpl updateShipmentsWithRouteDtlImpl;

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
			updateShipmentsWithRouteDtlImpl.updateShipmentsWithRouteDtl(null, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertEquals("The request is required.", e.getMessage());
		}
	}

	@Test
	public void testUpdate_TransactionContextRequired() throws Exception {
		try {
			final UpdateShipmentsWithRouteDtlRqst request = new UpdateShipmentsWithRouteDtlRqst();
			updateShipmentsWithRouteDtlImpl.updateShipmentsWithRouteDtl(request, null, entityManager);

			fail("Expected an exception");
		} catch (final Exception e) {
			assertEquals("The TransactionContext is required.", e.getMessage());
		}
	}

	@Test
	public void testUpdate_EntityManagerRequired() throws Exception {
		try {
			final UpdateShipmentsWithRouteDtlRqst request = new UpdateShipmentsWithRouteDtlRqst();
			updateShipmentsWithRouteDtlImpl.updateShipmentsWithRouteDtl(request, txnContext, null);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertEquals("The EntityManager is required.", e.getMessage());
		}
	}
	
	@Test
    public void testValidateRequest() {
		updateShipmentsWithRouteDtlImpl.validateRequest(null, moreInfos);
        assertTrue(moreInfoContainsString("request", moreInfos));
        moreInfos = Lists.newArrayList();

        UpdateShipmentsWithRouteDtlRqst request = new UpdateShipmentsWithRouteDtlRqst();
        updateShipmentsWithRouteDtlImpl.validateRequest(request, moreInfos);
        
        assertTrue(moreInfoContainsString("routePrefix", moreInfos));
        assertTrue(moreInfoContainsString("routeSuffix", moreInfos));
        assertTrue(moreInfoContainsString("routeTypeCd", moreInfos));
        assertTrue(moreInfoContainsString("At least one shipment", moreInfos));
        assertTrue(moreInfoContainsString("auditInfo", moreInfos));
    }
	
	@Test
    public void testValidateShipmentIds() {
        UpdateShipmentsWithRouteDtlRqst request = new UpdateShipmentsWithRouteDtlRqst();
        request.setShipmentInstIds(Lists.newArrayList(0l));
        updateShipmentsWithRouteDtlImpl.validateShipmentIds(moreInfos, request.getShipmentInstIds());
        assertTrue(moreInfoContainsString(ValidationErrorMessage.SHIPMENT_INST_ID_RQ.message(), moreInfos));
        
        moreInfos = Lists.newArrayList();
        request.setShipmentInstIds(Lists.newArrayList(1l,1l));
        updateShipmentsWithRouteDtlImpl.validateShipmentIds(moreInfos, request.getShipmentInstIds());
        assertTrue(moreInfoContainsString("duplicate shipmentInstId:1", moreInfos));
    }
	
	@Test
	public void testUpdate_ShipmentsWithRouteDtlThrowsNotFoundException() throws Exception {
		try {
			final UpdateShipmentsWithRouteDtlRqst request = new UpdateShipmentsWithRouteDtlRqst();
			final List<Long> shipmentInstIds = Lists.newArrayList(1l,2l);
			request.setShipmentInstIds(shipmentInstIds);
			request.setRoutePrefix(ROUTE_PREFIX);
			request.setRouteSuffix(ROUTE_SUFFIX);
			request.setRouteTypeCd(ROUTE_TYPE_CD);
			request.setAuditInfo(auditInfo);

			when(shipmentDAO.listShipmentsByShipmentIds(shipmentInstIds, entityManager)).thenReturn(null);

			updateShipmentsWithRouteDtlImpl.updateShipmentsWithRouteDtl(request, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			ServiceException se = (ServiceException) e;
			assertTrue(moreInfoContainsString("Shipments not found for the requested shipmentIds: [1, 2]", se.getFault().getMoreInfo()));
		}
	}

	@Test
	public void testUpdate_ShipmentsWithRouteDtlThrowsNotFoundExceptionWithMultiple() throws Exception {
		try {
			final UpdateShipmentsWithRouteDtlRqst request = new UpdateShipmentsWithRouteDtlRqst();
			final List<Long> shipmentInstIds = Lists.newArrayList(1l,2l);
			request.setShipmentInstIds(shipmentInstIds);
			request.setRoutePrefix(ROUTE_PREFIX);
			request.setRouteSuffix(ROUTE_SUFFIX);
			request.setRouteTypeCd(ROUTE_TYPE_CD);
			request.setAuditInfo(auditInfo);
	
			
			final ShmShipment shipment = new ShmShipment();
			shipment.setShpInstId(1);
			when(shipmentDAO.listShipmentsByShipmentIds(shipmentInstIds, entityManager)).thenReturn(Lists.newArrayList(shipment));
			
			updateShipmentsWithRouteDtlImpl.updateShipmentsWithRouteDtl(request, txnContext, entityManager);
			fail("Expected an exception.");
		} catch (final Exception e) {
			ServiceException se = (ServiceException) e;
			assertTrue(moreInfoContainsString("Shipments not found for the requested shipmentIds: [2]", se.getFault().getMoreInfo()));
		}
	}
	
	private boolean moreInfoContainsString(String req, List<MoreInfo> moreInfos) {
        return moreInfos.stream().anyMatch(moreInfo -> moreInfo.getMessage().contains(req));
    }
}
