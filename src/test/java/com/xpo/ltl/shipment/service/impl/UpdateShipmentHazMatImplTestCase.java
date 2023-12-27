package com.xpo.ltl.shipment.service.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.List;

import javax.persistence.EntityManager;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmHazMat;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.BillStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.BillStatusCd;
import com.xpo.ltl.api.shipment.v2.HazMat;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentHazMatResp;
import com.xpo.ltl.shipment.service.dao.ShmHazMatSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentHazMatRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;

import junit.framework.TestCase;

public class UpdateShipmentHazMatImplTestCase extends TestCase {
	private final static String PRO_NUMBER = "01230456789";

	@Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

	@Mock
	private ShmShipmentSubDAO shipmentDAO;

	@Mock
	private ShmHazMatSubDAO hazmatDAO;

	@InjectMocks
	private UpdateShipmentHazMatImpl updateShipmentHazMatImpl;

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
	public void test_RequestRequiredException() {
		try {
			updateShipmentHazMatImpl.updateShipmentHazMat(null, txnContext, entityManager);
			fail("Expected an exception.");
		} catch (final Exception e) {
			assertEquals(ValidationErrorMessage.REQUEST_REQUIRED.message(), e.getMessage());
		}
	}

	@Test
	public void test_TransactionContextRequiredException() {
		try {
			UpdateShipmentHazMatRqst request = new UpdateShipmentHazMatRqst();

			updateShipmentHazMatImpl.updateShipmentHazMat(request, null, entityManager);
			fail("Expected an exception.");
		} catch (final Exception e) {
			assertEquals(ValidationErrorMessage.TXN_CONTEXT_REQUIRED.message(), e.getMessage());
		}
	}

	@Test
	public void test_EntityManagerRequiredException() {
		try {
			UpdateShipmentHazMatRqst request = new UpdateShipmentHazMatRqst();

			updateShipmentHazMatImpl.updateShipmentHazMat(request, txnContext, null);
			fail("Expected an exception.");
		} catch (final Exception e) {
			assertEquals(ValidationErrorMessage.ENTITY_MANAGER_REQUIRED.message(), e.getMessage());
		}
	}

	@Test
	public void test_ShpInstIdOrProRequiredException() {
		try {
			UpdateShipmentHazMatRqst request = new UpdateShipmentHazMatRqst();

			updateShipmentHazMatImpl.updateShipmentHazMat(request, txnContext, entityManager);
			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage()
					.contains(ValidationErrorMessage.SHIP_ID_OR_PRO_IS_REQUIRED.message()));
		}
	}

	@Test
	public void test_HazmatWeightValidationException() {
		try {
			UpdateShipmentHazMatRqst request = new UpdateShipmentHazMatRqst();
			request.setProNbr(PRO_NUMBER);
			List<HazMat> hazMats = Lists.newArrayList(createRequestHazMat());
			hazMats.get(0).setHazmatWeightLbs(null);
			request.setHazMats(hazMats);

			updateShipmentHazMatImpl.updateShipmentHazMat(request, txnContext, entityManager);
			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(
					e.getMessage().contains(ValidationErrorMessage.HAZ_MAT_WEIGHT_REQD.message()));
		}
	}

	@Test
	public void test_HazmatUnnaValidationException() {
		try {
			UpdateShipmentHazMatRqst request = new UpdateShipmentHazMatRqst();
			request.setProNbr(PRO_NUMBER);
			List<HazMat> hazMats = Lists.newArrayList(createRequestHazMat());
			hazMats.get(0).setHazmatUnna(null);
			request.setHazMats(hazMats);

			updateShipmentHazMatImpl.updateShipmentHazMat(request, txnContext, entityManager);
			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage()
					.contains(ValidationErrorMessage.HAZ_MAT_UNNA_CODE_REQD.message()));
		}
	}

	@Test
	public void test_HazmatClassCdValidationException() {
		try {
			UpdateShipmentHazMatRqst request = new UpdateShipmentHazMatRqst();
			request.setProNbr(PRO_NUMBER);
			List<HazMat> hazMats = Lists.newArrayList(createRequestHazMat());
			hazMats.get(0).setHazmatClassCd(null);
			request.setHazMats(hazMats);

			updateShipmentHazMatImpl.updateShipmentHazMat(request, txnContext, entityManager);
			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage()
					.contains(ValidationErrorMessage.HAZ_MAT_HAZARD_CLASS_CD_REQD.message()));
		}
	}

	@Test
	public void test_HazmatBulkQuantityCdValidationException() {
		try {
			UpdateShipmentHazMatRqst request = new UpdateShipmentHazMatRqst();
			request.setProNbr(PRO_NUMBER);
			List<HazMat> hazMats = Lists.newArrayList(createRequestHazMat());
			hazMats.get(0).setHazmatBulkQuantityCd(null);
			request.setHazMats(hazMats);

			updateShipmentHazMatImpl.updateShipmentHazMat(request, txnContext, entityManager);
			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage()
					.contains(ValidationErrorMessage.HAZ_MAT_BULK_QTY_CD_INV.message()));
		}
	}

	@Test
	public void test_HazmatResidueIndValidationException() {
		try {
			UpdateShipmentHazMatRqst request = new UpdateShipmentHazMatRqst();
			request.setProNbr(PRO_NUMBER);
			List<HazMat> hazMats = Lists.newArrayList(createRequestHazMat());
			hazMats.get(0).setHazmatResidueInd(null);
			request.setHazMats(hazMats);

			updateShipmentHazMatImpl.updateShipmentHazMat(request, txnContext, entityManager);
			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage()
					.contains(ValidationErrorMessage.HAZ_MAT_RESIDUE_IND_INV.message()));
		}
	}

	@Test
	public void test_ShipmentNotFoundException() {
		try {
			UpdateShipmentHazMatRqst request = new UpdateShipmentHazMatRqst();
			request.setProNbr(PRO_NUMBER);
			request.setHazMats(Lists.newArrayList(createRequestHazMat()));

			when(shipmentDAO.findByIdOrProNumber(any(), any(), eq(entityManager))).thenReturn(null);

			updateShipmentHazMatImpl.updateShipmentHazMat(request, txnContext, entityManager);
			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains("Shipment not found"));
		}
	}

	@Test
	public void test_BilledShipment_HazmatDetailsRequiredException() {
		try {
			UpdateShipmentHazMatRqst request = new UpdateShipmentHazMatRqst();
			request.setProNbr(PRO_NUMBER);
			request.setHazMats(Lists.newArrayList());

			ShmShipment shmShipment = createShipment(BillStatusCd.BILLED, true);

			when(shipmentDAO.findByIdOrProNumber(any(), any(), eq(entityManager)))
					.thenReturn(shmShipment);

			updateShipmentHazMatImpl.updateShipmentHazMat(request, txnContext, entityManager);
			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage()
					.contains(ValidationErrorMessage.HZM_DETAILS_REQUIRED_MARK_HZM.message()));
		}
	}

	@Test
	public void test_BilledShipment_HazmatDetailsNotAllowedException() {
		try {
			UpdateShipmentHazMatRqst request = new UpdateShipmentHazMatRqst();
			request.setProNbr(PRO_NUMBER);
			request.setHazMats(Lists.newArrayList(createRequestHazMat()));

			ShmShipment shmShipment = createShipment(BillStatusCd.BILLED, false);

			when(shipmentDAO.findByIdOrProNumber(any(), any(), eq(entityManager)))
					.thenReturn(shmShipment);

			updateShipmentHazMatImpl.updateShipmentHazMat(request, txnContext, entityManager);
			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage()
					.contains(ValidationErrorMessage.HZM_DETAILS_BILLED_SHM_NOALLOW.message()));
		}
	}

	@Test
	public void test_BilledHazmatShipment_AddHazmats() throws ServiceException {
		long nextSeqNbr = 1L;
		UpdateShipmentHazMatRqst request = new UpdateShipmentHazMatRqst();
		request.setProNbr(PRO_NUMBER);
		request.setHazMats(Lists.newArrayList(createRequestHazMat()));

		ShmShipment shmShipment = createShipment(BillStatusCd.BILLED, true);
		ShmHazMat shmHazMat = createShmHazMat();
		List<ShmHazMat> shmHazMats = Lists.newArrayList(shmHazMat);

		when(shipmentDAO.findByIdOrProNumber(any(), any(), eq(entityManager)))
				.thenReturn(shmShipment);
		when(hazmatDAO.findAllByShpInstId(any(), eq(entityManager))).thenReturn(shmHazMats);
		when(hazmatDAO.getNextSeqNbrByShpInstId(any(), any())).thenReturn(nextSeqNbr);
		when(hazmatDAO.save(any(), any())).thenReturn(shmHazMat);

		UpdateShipmentHazMatResp response = updateShipmentHazMatImpl.updateShipmentHazMat(request,
				txnContext, entityManager);

		verify(hazmatDAO, times(1)).remove(anyList(), any());
		verify(hazmatDAO, times(1)).removeDB2ShmHazMats(any(), any(), any());
		verify(hazmatDAO, times(1)).save(any(), any());
		verify(hazmatDAO, times(1)).insertDB2ShmHazMat(any(), any());

		assertEquals("Response hazmat count", response.getHazMats().size(), shmHazMats.size());
	}

	@Test
	public void test_UnbilledNonHazmat_AddHazmats() throws ServiceException {
		long nextSeqNbr = 1L;
		UpdateShipmentHazMatRqst request = new UpdateShipmentHazMatRqst();
		request.setProNbr(PRO_NUMBER);
		request.setHazMats(Lists.newArrayList(createRequestHazMat()));

		ShmShipment shmShipment = createShipment(BillStatusCd.UNBILLED, false);
		ShmHazMat shmHazMat = createShmHazMat();
		List<ShmHazMat> shmHazMats = Lists.newArrayList(shmHazMat);

		when(shipmentDAO.save(any(), eq(entityManager))).thenReturn(shmShipment);
		when(shipmentDAO.findByIdOrProNumber(any(), any(), eq(entityManager)))
				.thenReturn(shmShipment);
		when(hazmatDAO.findAllByShpInstId(any(), eq(entityManager))).thenReturn(shmHazMats);
		when(hazmatDAO.getNextSeqNbrByShpInstId(any(), any())).thenReturn(nextSeqNbr);
		when(hazmatDAO.save(any(), any())).thenReturn(shmHazMat);

		UpdateShipmentHazMatResp response = updateShipmentHazMatImpl.updateShipmentHazMat(request,
				txnContext, entityManager);

		verify(shipmentDAO, times(1)).save(any(), any());
		verify(shipmentDAO, times(1)).updateDB2ShmShipment(any(ShmShipment.class), any(), any(),
				any());
		verify(shipmentDAO, times(1)).save(any(), any());
		verify(shipmentDAO, times(1)).updateDB2ShmShipment(any(ShmShipment.class), any(), any(),
				any());
		verify(hazmatDAO, times(1)).remove(anyList(), any());
		verify(hazmatDAO, times(1)).removeDB2ShmHazMats(any(), any(), any());
		verify(hazmatDAO, times(1)).save(any(), any());
		verify(hazmatDAO, times(1)).insertDB2ShmHazMat(any(), any());

		assertEquals("Response hazmat count", response.getHazMats().size(), shmHazMats.size());
	}

	@Test
	public void test_UnbilledHazmat_AddHazmats() throws ServiceException {
		long nextSeqNbr = 1L;
		UpdateShipmentHazMatRqst request = new UpdateShipmentHazMatRqst();
		request.setProNbr(PRO_NUMBER);
		request.setHazMats(Lists.newArrayList(createRequestHazMat()));

		ShmShipment shmShipment = createShipment(BillStatusCd.UNBILLED, true);
		ShmHazMat shmHazMat = createShmHazMat();
		List<ShmHazMat> shmHazMats = Lists.newArrayList(shmHazMat);

		when(shipmentDAO.save(any(), eq(entityManager))).thenReturn(shmShipment);
		when(shipmentDAO.findByIdOrProNumber(any(), any(), eq(entityManager)))
				.thenReturn(shmShipment);
		when(hazmatDAO.findAllByShpInstId(any(), eq(entityManager))).thenReturn(shmHazMats);
		when(hazmatDAO.getNextSeqNbrByShpInstId(any(), any())).thenReturn(nextSeqNbr);
		when(hazmatDAO.save(any(), any())).thenReturn(shmHazMat);

		UpdateShipmentHazMatResp response = updateShipmentHazMatImpl.updateShipmentHazMat(request,
				txnContext, entityManager);

		verify(shipmentDAO, times(0)).save(any(), any());
		verify(shipmentDAO, times(0)).updateDB2ShmShipment(any(ShmShipment.class), any(), any(),
				any());
		verify(hazmatDAO, times(1)).remove(anyList(), any());
		verify(hazmatDAO, times(1)).removeDB2ShmHazMats(any(), any(), any());
		verify(hazmatDAO, times(1)).save(any(), any());
		verify(hazmatDAO, times(1)).insertDB2ShmHazMat(any(), any());

		assertEquals("Response hazmat count", response.getHazMats().size(), shmHazMats.size());
	}

	@Test
	public void test_UnbilledHazmat_AddNoHazmats() throws ServiceException {
		long nextSeqNbr = 1L;
		UpdateShipmentHazMatRqst request = new UpdateShipmentHazMatRqst();
		request.setProNbr(PRO_NUMBER);
		request.setHazMats(Lists.newArrayList());

		ShmShipment shmShipment = createShipment(BillStatusCd.UNBILLED, true);
		ShmHazMat shmHazMat = createShmHazMat();
		List<ShmHazMat> shmHazMats = Lists.newArrayList(shmHazMat);

		when(shipmentDAO.save(any(), eq(entityManager))).thenReturn(shmShipment);
		when(shipmentDAO.findByIdOrProNumber(any(), any(), eq(entityManager)))
				.thenReturn(shmShipment);
		when(hazmatDAO.findAllByShpInstId(any(), eq(entityManager))).thenReturn(shmHazMats);
		when(hazmatDAO.getNextSeqNbrByShpInstId(any(), any())).thenReturn(nextSeqNbr);
		when(hazmatDAO.save(any(), any())).thenReturn(shmHazMat);

		UpdateShipmentHazMatResp response = updateShipmentHazMatImpl.updateShipmentHazMat(request,
				txnContext, entityManager);

		verify(shipmentDAO, times(1)).save(any(), any());
		verify(shipmentDAO, times(1)).updateDB2ShmShipment(any(ShmShipment.class), any(), any(),
				any());
		verify(hazmatDAO, times(1)).remove(anyList(), any());
		verify(hazmatDAO, times(1)).removeDB2ShmHazMats(any(), any(), any());
		verify(hazmatDAO, times(0)).save(any(), any());
		verify(hazmatDAO, times(0)).insertDB2ShmHazMat(any(), any());

		assertEquals("Response hazmat count", response.getHazMats().size(), 0);
	}

	private HazMat createRequestHazMat() {
		HazMat hazMat = new HazMat();
		hazMat.setHazmatUnna("UN1002");
		hazMat.setHazmatClassCd("2");
		hazMat.setHazmatWeightLbs(500L);
		hazMat.setHazmatBulkQuantityCd("L");
		hazMat.setHazmatResidueInd(true);
		return hazMat;
	}

	private ShmHazMat createShmHazMat() {
		ShmHazMat shmHazMat = new ShmHazMat();
		return shmHazMat;
	}

	private ShmShipment createShipment(BillStatusCd billStatusCd, boolean isHazMat) {
		ShmShipment shmShipment = new ShmShipment();
		shmShipment.setShpInstId(123456L);
		shmShipment.setBillStatCd(BillStatusCdTransformer.toCode(billStatusCd));
		shmShipment.setHazmatInd(BasicTransformer.toString(isHazMat));
		return shmShipment;
	}
}
