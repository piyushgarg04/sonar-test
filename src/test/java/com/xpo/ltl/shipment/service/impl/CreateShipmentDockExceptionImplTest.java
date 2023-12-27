package com.xpo.ltl.shipment.service.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.service.entity.ShmXdockExcp;
import com.xpo.ltl.api.shipment.v2.CreateShipmentDockExceptionRqst;
import com.xpo.ltl.api.shipment.v2.XdockException;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmXdockExcpSubDAO;

import junit.framework.TestCase;

public class CreateShipmentDockExceptionImplTest extends TestCase {

	@Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

	@Mock
	private ExternalRestClient externalRestClient;

	@Mock
	private ShmShipmentSubDAO shmShipmentSubDAO;

	@Mock
	private ShmXdockExcpSubDAO shmXdockExcpSubDAO;

	@InjectMocks
	private CreateShipmentDockExceptionImpl createShipmentDockExceptionImpl;

	@Override
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testRequestIsRequired() {
		try {
			createShipmentDockExceptionImpl.createShipmentDockException(null, txnContext,
					entityManager);
			fail("Expected an exception");
		} catch (Exception e) {
			assertEquals("The request is required", e.getMessage());
		}
	}

	@Test
	public void testTxnContextIsRequired() {
		CreateShipmentDockExceptionRqst createShipmentDockExceptionRqst = new CreateShipmentDockExceptionRqst();
		try {
			createShipmentDockExceptionImpl.createShipmentDockException(
					createShipmentDockExceptionRqst, null,
							entityManager);
			fail("Expected an exception");
		} catch (Exception e) {
			assertEquals("The transanction context is required", e.getMessage());
		}
	}

	@Test
	public void testEntityManagerIsRequired() {
		CreateShipmentDockExceptionRqst createShipmentDockExceptionRqst = new CreateShipmentDockExceptionRqst();
		try {
			createShipmentDockExceptionImpl
					.createShipmentDockException(createShipmentDockExceptionRqst, txnContext, null);
			fail("Expected an exception");
		} catch (Exception e) {
			assertEquals("The entityManager is required", e.getMessage());
		}
	}

	@Test
	public void testDockExceptionIsRequired() {
		CreateShipmentDockExceptionRqst createShipmentDockExceptionRqst = new CreateShipmentDockExceptionRqst();
		createShipmentDockExceptionRqst.setDockException(null);

		try {
			createShipmentDockExceptionImpl.createShipmentDockException(
					createShipmentDockExceptionRqst, txnContext, entityManager);
			fail("Expected an exception");
		} catch (Exception e) {
			assertEquals("The DockException is required", e.getMessage());
		}
	}

	@Test
	public void testParentProNbrAndShipmentInstRequired() {
		String sicCode = "UPO";
		CreateShipmentDockExceptionRqst createShipmentDockExceptionRqst = new CreateShipmentDockExceptionRqst();
		XdockException dockException = new XdockException();
		dockException.setReportingSicCd(sicCode);
		createShipmentDockExceptionRqst.setDockException(dockException);
		createShipmentDockExceptionRqst.setParentProNbr(null);
		createShipmentDockExceptionRqst.setShipmentInstId(null);

		try {
			createShipmentDockExceptionImpl.createShipmentDockException(
					createShipmentDockExceptionRqst, txnContext, entityManager);

			fail("Expected an exception");
		} catch (Exception e) {
			assertEquals("SHMN020-048E:PRO Number or Shipment Instance ID is required.",
					e.getMessage());
		}
	}

	@Test
	public void testNotActiveSicAndLinehaulSic() {
		String sicCode = "UPO";
		CreateShipmentDockExceptionRqst createShipmentDockExceptionRqst = new CreateShipmentDockExceptionRqst();
		XdockException dockException = new XdockException();
		dockException.setReportingSicCd(sicCode);
		createShipmentDockExceptionRqst.setDockException(dockException);
		createShipmentDockExceptionRqst.setParentProNbr("02080966063");
		createShipmentDockExceptionRqst.setShipmentInstId(Long.valueOf(120));

		try {
			when(externalRestClient.isNotActiveSicAndLinehaul(Mockito.anyString(),
					Mockito.any(TransactionContext.class))).thenReturn(Boolean.TRUE);

			createShipmentDockExceptionImpl.createShipmentDockException(
					createShipmentDockExceptionRqst, txnContext, entityManager);
			fail("Expected an exception");

		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:createShipmentDockException, message:The reportingSicCd must be a valid and active Linehaul Sic)",
					e.getMessage());
		}
	}

	@Test
	public void testOverAndShortPiecesRequired() {
		String sicCode = "UPO";
		CreateShipmentDockExceptionRqst createShipmentDockExceptionRqst = new CreateShipmentDockExceptionRqst();
		XdockException dockException = new XdockException();
		dockException.setReportingSicCd(sicCode);
		dockException.setOverPiecesCount(Long.valueOf(12));
		dockException.setShortPiecesCount(Long.valueOf(254));
		createShipmentDockExceptionRqst.setDockException(dockException);
		createShipmentDockExceptionRqst.setParentProNbr("02080966063");
		createShipmentDockExceptionRqst.setShipmentInstId(Long.valueOf("120"));

		try {
			when(externalRestClient.isNotActiveSicAndLinehaul(Mockito.anyString(),
					Mockito.any(TransactionContext.class)))
							.thenReturn(Boolean.FALSE);

			createShipmentDockExceptionImpl.createShipmentDockException(
					createShipmentDockExceptionRqst, txnContext, entityManager);
			fail("Expected an exception");
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:createShipmentDockException, message:OverPicesCount and ShortPiecesCount cannot be both > 0)",
					e.getMessage());
		}
	}

	@Test
	public void testShmShipmentNotFound() {
		String sicCode = "UPO";
		CreateShipmentDockExceptionRqst createShipmentDockExceptionRqst = new CreateShipmentDockExceptionRqst();
		XdockException dockException = new XdockException();
		dockException.setReportingSicCd(sicCode);
		dockException.setOverPiecesCount(Long.valueOf(12));
		dockException.setShortPiecesCount(Long.valueOf(254));
		createShipmentDockExceptionRqst.setDockException(dockException);
		createShipmentDockExceptionRqst.setParentProNbr("02080966063");
		createShipmentDockExceptionRqst.setShipmentInstId(Long.valueOf("120"));

		try {
			when(externalRestClient.isNotActiveSicAndLinehaul(Mockito.anyString(),
					Mockito.any(TransactionContext.class)))
							.thenReturn(Boolean.FALSE);
			when(shmShipmentSubDAO.findByIdOrProNumber(Mockito.anyString(), Mockito.anyLong(),
					Mockito.any(EntityManager.class))).thenReturn(null);

			createShipmentDockExceptionImpl.createShipmentDockException(
					createShipmentDockExceptionRqst, txnContext, entityManager);
			fail("Expected an exception");
		} catch (Exception e) {
			assertEquals(
					"SHMN021-231E:Validation Errors found.(location:createShipmentDockException, message:OverPicesCount and ShortPiecesCount cannot be both > 0)",
					e.getMessage());
		}
	}

	@Test
	public void testShmXdocExcpCreated() throws ServiceException {
		String sicCode = "UPO";
		CreateShipmentDockExceptionRqst createShipmentDockExceptionRqst = new CreateShipmentDockExceptionRqst();
		XdockException dockException = new XdockException();
		Long sequenceNbr = Long.valueOf(457);
		Long shipmentInstId = Long.valueOf("120");
		dockException.setReportingSicCd(sicCode);
		dockException.setSequenceNbr(BigInteger.valueOf(sequenceNbr));
		dockException.setShipmentInstId(shipmentInstId);
		Long overPiecesCount = Long.valueOf(0);
		dockException.setOverPiecesCount(overPiecesCount);
		Long shortPiecesCount = Long.valueOf(0);
		dockException.setShortPiecesCount(shortPiecesCount);
		createShipmentDockExceptionRqst.setDockException(dockException);
		createShipmentDockExceptionRqst.setParentProNbr("02080966063");
		createShipmentDockExceptionRqst.setShipmentInstId(shipmentInstId);
		ShmShipment shmShipment = new ShmShipment();
		shmShipment.setShpInstId(shipmentInstId);
		shmShipment.setTotPcsCnt(BigDecimal.valueOf(255));

		when(externalRestClient.isNotActiveSicAndLinehaul(Mockito.anyString(),
				Mockito.any(TransactionContext.class))).thenReturn(Boolean.FALSE);
		when(shmShipmentSubDAO.findByIdOrProNumber(Mockito.anyString(), Mockito.anyLong(),
				Mockito.any(EntityManager.class))).thenReturn(shmShipment);
		when(shmXdockExcpSubDAO.getNextSeqNbrByShpInstId(Mockito.anyLong(),
				Mockito.any(EntityManager.class))).thenReturn(sequenceNbr);

		createShipmentDockExceptionImpl
				.createShipmentDockException(createShipmentDockExceptionRqst,
				txnContext, entityManager);

		verify(shmXdockExcpSubDAO, times(1)).save(any(), any());
		verify(shmXdockExcpSubDAO, times(1)).createDB2ShmXdockExcp(any(ShmXdockExcp.class), any());
	}

}
