package com.xpo.ltl.shipment.service.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Calendar;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmEventLog;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmt;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitPK;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.CreateHandlingUnitMovementRqst;
import com.xpo.ltl.api.shipment.v2.HandlingUnitMovement;
import com.xpo.ltl.api.shipment.v2.HandlingUnitMovementTypeCd;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShmEventLogSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitMvmtSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmMovementSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import com.xpo.ltl.shipment.service.util.TimestampUtil;

import junit.framework.TestCase;

public class CreateHandlingUnitMovementImplTestCase extends TestCase {

	private static final Long SHP_INST_ID = new Long(1234);
	private static final String INVALID_CHILD_PRO = "0083058910";
	private static final String INVALID_CHECK_DIGIT_PRO = "625534373";
	private static final String PRO_NUMBER = "02080966063";
	private static final String TRACKING_PRO_1 = "3541210282";
	private static final String TRACKING_PRO_2 = "6611396761";
	private static final String ZIP_CODE = "75374";
	private static final String TRAILER_TYPE_CD = "T";
	private static final String TRAILER_SUB_TYPE_CD = "TTC28";

	@Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

	@Mock
	private ShmShipmentSubDAO shipmentDAO;

	@Mock
	private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

	@Mock
	private ShmEventLogSubDAO shmEventLogSubDAO;

	@Mock
	private ShmHandlingUnitMvmtSubDAO shmHandlingUnitMvmtSubDAO;

	@Mock
	private ShmMovementSubDAO shmMovementSubDAO;

	@Mock
	private ExternalRestClient externalRestClient;

	@InjectMocks
	private CreateHandlingUnitMovementImpl createHandlingUnitMovementImpl;


	@Override
	@Before
	public void setUp()
	{
		MockitoAnnotations.initMocks(this);

		when(txnContext.getSrcApplicationId()).thenReturn("JUNIT");

		final User user = new User();
		user.setUserId("JUNIT");
		user.setEmployeeId("JUNIT");
		when(txnContext.getUser()).thenReturn(user);

		when(txnContext.getTransactionTimestamp()).thenReturn(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance()));

		when(txnContext.getCorrelationId()).thenReturn("0");
	}

	@Test
	public void testCreateHandlingUnitMovement_RequestRequired() throws Exception
	{
		try {
			createHandlingUnitMovementImpl.createHandlingUnitMovement(null, TRACKING_PRO_1, txnContext,
					entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains("The request is required."));
		}
	}

	@Test
	public void testCreateHandlingUnitMovement_TransactionContextRequired() throws Exception
	{
		try {
			CreateHandlingUnitMovementRqst request = new CreateHandlingUnitMovementRqst();
			createHandlingUnitMovementImpl.createHandlingUnitMovement(request, TRACKING_PRO_1, null,
					entityManager);

			fail("Expected an exception");
		} catch (final Exception e) {
			assertEquals("The TransactionContext is required.", e.getMessage());
		}
	}

	@Test
	public void testCreateHandlingUnitMovement_EntityManagerRequired() throws Exception
	{
		try {
			CreateHandlingUnitMovementRqst request = new CreateHandlingUnitMovementRqst();
			createHandlingUnitMovementImpl.createHandlingUnitMovement(request, TRACKING_PRO_1,
					txnContext, null);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertEquals("The EntityManager is required.", e.getMessage());
		}
	}

	@Test
	public void testCreateHandlingUnitMovement_HandlingMovementRequired() throws Exception
	{
		try {
			CreateHandlingUnitMovementRqst request = new CreateHandlingUnitMovementRqst();
			createHandlingUnitMovementImpl.createHandlingUnitMovement(request, TRACKING_PRO_1,
					txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains("The shipment handling unit movement is required."));
		}
	}

	@Test
	public void testCreateHandlingUnitMovement_ChildProNumberRequired() throws Exception
	{
		try {
			CreateHandlingUnitMovementRqst request = new CreateHandlingUnitMovementRqst();
			HandlingUnitMovement handlingUnitMovement = new HandlingUnitMovement();
			request.setHandlingUnitMovement(handlingUnitMovement);
			createHandlingUnitMovementImpl.createHandlingUnitMovement(request, null,
					txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains(ValidationErrorMessage.PRO_NBR_RQ.message()));
		}
	}

	@Test
	public void testCreateHandlingUnitMovement_InvalidChildProNumberFormat() throws Exception
	{
		try {
			CreateHandlingUnitMovementRqst request = new CreateHandlingUnitMovementRqst();
			HandlingUnitMovement handlingUnitMovement = new HandlingUnitMovement();
			request.setHandlingUnitMovement(handlingUnitMovement);
			createHandlingUnitMovementImpl.createHandlingUnitMovement(request, INVALID_CHILD_PRO,
					txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains(ValidationErrorMessage.PRO_NBR_CHK_DIGIT_ERROR.message()));
		}
	}

	@Test
	public void testCreateHandlingUnitMovement_InvalidCheckDigitPro() throws Exception
	{
		try {
			CreateHandlingUnitMovementRqst request = new CreateHandlingUnitMovementRqst();
			HandlingUnitMovement handlingUnitMovement = new HandlingUnitMovement();
			request.setHandlingUnitMovement(handlingUnitMovement);
			createHandlingUnitMovementImpl.createHandlingUnitMovement(request, INVALID_CHECK_DIGIT_PRO,
					txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains(ValidationErrorMessage.PRO_NBR_CHK_DIGIT_ERROR.message()));
		}
	}

	@Test
	public void testCreateHandlingUnitMovement_lastMovementDateTimeReq() throws Exception
	{
		try {
			CreateHandlingUnitMovementRqst request = new CreateHandlingUnitMovementRqst();
			HandlingUnitMovement handlingUnitMovement = new HandlingUnitMovement();
			request.setHandlingUnitMovement(handlingUnitMovement);
			createHandlingUnitMovementImpl.createHandlingUnitMovement(request, TRACKING_PRO_1,
					txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains(ValidationErrorMessage.MVMT_TMST_RQ.message()));
		}
	}

	@Test
	public void testCreateHandlingUnitMovement_movementReportingSicRequired() throws Exception
	{
		try {
			CreateHandlingUnitMovementRqst request = new CreateHandlingUnitMovementRqst();
			HandlingUnitMovement handlingUnitMovement = new HandlingUnitMovement();
			handlingUnitMovement.setMovementDateTime(TimestampUtil.getLowCalendar());
			request.setHandlingUnitMovement(handlingUnitMovement);
			createHandlingUnitMovementImpl.createHandlingUnitMovement(request, TRACKING_PRO_1,
					txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains(ValidationErrorMessage.LST_MV_RPT_SIC_RQ.message()));
		}
	}

	@Test
	public void testCreateHandlingUnitMovement_movementTypeCdRequired() throws Exception
	{
		try {
			CreateHandlingUnitMovementRqst request = new CreateHandlingUnitMovementRqst();
			HandlingUnitMovement handlingUnitMovement = new HandlingUnitMovement();
			handlingUnitMovement.setMovementDateTime(TimestampUtil.getLowCalendar());
			handlingUnitMovement.setMovementReportingSicCd("UPO");
			request.setHandlingUnitMovement(handlingUnitMovement);
			createHandlingUnitMovementImpl.createHandlingUnitMovement(request, TRACKING_PRO_1,
					txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains("Handling unit movement type code is required."));
		}
	}


	@Test
	public void testCreateHandlingUnitMovement_HappyPath() throws Exception
	{

		CreateHandlingUnitMovementRqst request = new CreateHandlingUnitMovementRqst();
		HandlingUnitMovement handlingUnitMovement = new HandlingUnitMovement();
		handlingUnitMovement.setMovementDateTime(TimestampUtil.getLowCalendar());
		handlingUnitMovement.setMovementReportingSicCd("UPO");
		handlingUnitMovement.setMovementTypeCd(HandlingUnitMovementTypeCd.DELIVER);
        handlingUnitMovement.setBypassScanInd(true);
        handlingUnitMovement.setScanDateTime(TimestampUtil.getLowCalendar());
		request.setHandlingUnitMovement(handlingUnitMovement);
		ShmHandlingUnit shmHandlingUnit = new ShmHandlingUnit();
		shmHandlingUnit.setChildProNbrTxt(TRACKING_PRO_1);
		ShmHandlingUnitPK handlingUnitPK = new ShmHandlingUnitPK();
		handlingUnitPK.setSeqNbr(1L);
		handlingUnitPK.setShpInstId(SHP_INST_ID);
		shmHandlingUnit.setId(handlingUnitPK);
		Long movementSeqNbr = 2L;


		when(shmHandlingUnitSubDAO.findByTrackingProNumber(
				ProNumberHelper.validateProNumber(TRACKING_PRO_1, txnContext), entityManager))
				.thenReturn(shmHandlingUnit);
		when(externalRestClient.isActiveSic(handlingUnitMovement.getMovementReportingSicCd(),
				txnContext)).thenReturn(true);
		when(shmHandlingUnitMvmtSubDAO.getNextMvmtBySeqNbrAndShpInstId(anyLong(), any(), any()))
				.thenReturn(movementSeqNbr);

		createHandlingUnitMovementImpl.createHandlingUnitMovement(request, TRACKING_PRO_1,
				txnContext, entityManager);

		verify(shmHandlingUnitMvmtSubDAO, times(1)).save(any(), any());
		verify(shmHandlingUnitMvmtSubDAO, times(1)).createDB2ShmHandlingUnitMvmt(any(
				ShmHandlingUnitMvmt.class), any());
	}

	@Test
	public void testCreateHandlingUnitMovement_MovementTypeCodeClose() throws Exception
	{
		CreateHandlingUnitMovementRqst request = new CreateHandlingUnitMovementRqst();
		HandlingUnitMovement handlingUnitMovement = new HandlingUnitMovement();
		handlingUnitMovement.setMovementDateTime(TimestampUtil.getLowCalendar());
		handlingUnitMovement.setMovementReportingSicCd("UPO");
		handlingUnitMovement.setMovementTypeCd(HandlingUnitMovementTypeCd.CLOSE);
        handlingUnitMovement.setBypassScanInd(true);
        handlingUnitMovement.setScanDateTime(TimestampUtil.getLowCalendar());
		request.setHandlingUnitMovement(handlingUnitMovement);
		ShmHandlingUnit shmHandlingUnit = new ShmHandlingUnit();
		shmHandlingUnit.setChildProNbrTxt(TRACKING_PRO_1);
		ShmHandlingUnitPK handlingUnitPK = new ShmHandlingUnitPK();
		handlingUnitPK.setSeqNbr(1L);
		handlingUnitPK.setShpInstId(SHP_INST_ID);
		shmHandlingUnit.setId(handlingUnitPK);
		Long movementSeqNbr = 2L;
		ShmShipment shmShipment = new ShmShipment();
		shmShipment.setTotPcsCnt(BigDecimal.TEN);
		shmShipment.setProNbrTxt(PRO_NUMBER);
		shmShipment.setShpInstId(SHP_INST_ID);


		when(shmHandlingUnitSubDAO.findByTrackingProNumber(ProNumberHelper.validateProNumber(TRACKING_PRO_1, txnContext),
				entityManager)).thenReturn(shmHandlingUnit);
		when(externalRestClient.isActiveSic(handlingUnitMovement.getMovementReportingSicCd(),
				txnContext)).thenReturn(true);
		when(shmHandlingUnitMvmtSubDAO.getNextMvmtBySeqNbrAndShpInstId(anyLong(), any(), any()))
				.thenReturn(movementSeqNbr);
		when(shipmentDAO.findByIdOrProNumber(null, SHP_INST_ID, entityManager))
				.thenReturn(shmShipment);
		when(shmMovementSubDAO.findMostRecentByShpInstId(SHP_INST_ID, entityManager))
				.thenReturn(null);

		createHandlingUnitMovementImpl.createHandlingUnitMovement(request, TRACKING_PRO_1,
				txnContext, entityManager);

		verify(shmHandlingUnitMvmtSubDAO, times(1)).save(any(), any());
		verify(shmHandlingUnitMvmtSubDAO, times(1)).createDB2ShmHandlingUnitMvmt(any(
				ShmHandlingUnitMvmt.class), any());

		verify(shipmentDAO, times(1)).save(any(), any());
		verify(shipmentDAO, times(1)).updateDB2ShmShipment(any(ShmShipment.class), any(),
				any(), any());

		verify(shmMovementSubDAO, times(1)).save(any(), any());
		verify(shmMovementSubDAO, times(1)).createDB2ShmMovement(any(ShmMovement.class), any());


		verify(shmEventLogSubDAO, times(1)).create(any(), any());
		verify(shmEventLogSubDAO, times(1)).createDB2ShmEventLog(any(ShmEventLog.class), any());


	}

}