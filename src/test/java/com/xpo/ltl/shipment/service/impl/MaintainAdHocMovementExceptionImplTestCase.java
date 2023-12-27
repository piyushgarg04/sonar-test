package com.xpo.ltl.shipment.service.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.Calendar;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcpPK;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementPK;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementExceptionTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.MovementException;
import com.xpo.ltl.api.shipment.v2.MovementExceptionTypeCd;
import com.xpo.ltl.api.shipment.v2.UpsertAdHocMovementExceptionResp;
import com.xpo.ltl.api.shipment.v2.UpsertAdHocMovementExceptionRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShipmentMovementExceptionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmMovementSubDAO;
import com.xpo.ltl.shipment.service.ejb.v1.ShipmentServiceBean;

import junit.framework.TestCase;

public class MaintainAdHocMovementExceptionImplTestCase extends TestCase {
	private static final Long SHP_INST_ID = new Long(1234);
	private static final Integer MVMT_SEQ_NBR = 1;
	private static final Integer SEQ_NBR = 2;

	@Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

	@Mock
	private ShipmentMovementExceptionSubDAO shipmentMovementExceptionDAO;

    @Mock
    private ShmMovementSubDAO shipmentMovementDAO;

	@Mock
	private AuditInfo auditInfo;

	@InjectMocks
	private MaintainAdHocMovementExceptionImpl maintainAdHocMovementExceptionImpl;

	@InjectMocks
	private ShipmentServiceBean shipmentServiceBean;

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
	public void testUpsert_RequestRequired() throws Exception
	{
		try {
			maintainAdHocMovementExceptionImpl.upsertAdHocMovementException(null, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertEquals("The request is required.", e.getMessage());
		}
	}

	@Test
	public void testUpsert_TransactionContextRequired() throws Exception
	{
		try {
			final UpsertAdHocMovementExceptionRqst request = new UpsertAdHocMovementExceptionRqst();
			maintainAdHocMovementExceptionImpl.upsertAdHocMovementException(request, null, entityManager);

			fail("Expected an exception");
		} catch (final Exception e) {
			assertEquals("The TransactionContext is required.", e.getMessage());
		}
	}

	@Test
	public void testUpsert_EntityManagerRequired() throws Exception
	{
		try {
			final UpsertAdHocMovementExceptionRqst request = new UpsertAdHocMovementExceptionRqst();
			maintainAdHocMovementExceptionImpl.upsertAdHocMovementException(request, txnContext, null);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertEquals("The EntityManager is required.", e.getMessage());
		}
	}

	@Test
	public void testUpsert_ShipmentMovementExceptionRequired() throws Exception
	{
		try {
			final UpsertAdHocMovementExceptionRqst request = new UpsertAdHocMovementExceptionRqst();
			maintainAdHocMovementExceptionImpl
				.upsertAdHocMovementException(request, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertEquals("The shipment movement exception is required.", e.getMessage());
		}
	}
	
	@Test
	public void testUpsert_ShipmentMovementNotFound() throws Exception
	{
		try {
			final UpsertAdHocMovementExceptionRqst request = new UpsertAdHocMovementExceptionRqst();
			
			request.setMovementException(generateMovementException());
			maintainAdHocMovementExceptionImpl
				.upsertAdHocMovementException(request, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains(NotFoundErrorMessage.MOVEMENT_NF.message()));
		}
	}
	
	@Test
	public void testUpsert_ShipmentMovementWrongListActionCd() throws Exception
	{
		try {
			final UpsertAdHocMovementExceptionRqst request = new UpsertAdHocMovementExceptionRqst();
			
			request.setMovementException(generateMovementException());
			request.getMovementException().setListActionCd(ActionCd.DELETE);
			maintainAdHocMovementExceptionImpl
				.upsertAdHocMovementException(request, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains("ActionCd should be Add or Update."));
		}
	}
	
	@Test
	public void testUpsert_ShipmentMovementAdHocRequired() throws Exception
	{
		try {
			final UpsertAdHocMovementExceptionRqst request = new UpsertAdHocMovementExceptionRqst();
			
			request.setMovementException(generateMovementException());
			request.getMovementException().setTypeCd(MovementExceptionTypeCd.DAMAGED);
			maintainAdHocMovementExceptionImpl
				.upsertAdHocMovementException(request, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertTrue(e.getMessage().contains("Movement Exception Type code should be Ad Hoc."));
		}
	}

	@Test
	public void testUpsert_ReturnsValidResponseWithShipmentManagementRemarkPK() throws Exception {
		
		final UpsertAdHocMovementExceptionRqst request = new UpsertAdHocMovementExceptionRqst();

		request.setMovementException(generateMovementException());

		final ShmMovementExcpPK actionId = new ShmMovementExcpPK();
		actionId.setShpInstId(SHP_INST_ID);
		actionId.setSeqNbr(SEQ_NBR);
		actionId.setMvmtSeqNbr(MVMT_SEQ_NBR.longValue());

		final ShmMovement shmMovementEntity = new ShmMovement();
		final ShmMovementPK id = new ShmMovementPK();
		id.setShpInstId(SHP_INST_ID);
		id.setSeqNbr(MVMT_SEQ_NBR);
		shmMovementEntity.setId(id);

		when(shipmentMovementDAO.findById(id, entityManager)).thenReturn(shmMovementEntity);
		when(shipmentMovementExceptionDAO.findMaxSeqNbr(actionId, entityManager))
			.thenReturn(SEQ_NBR.longValue());
		when(shipmentMovementExceptionDAO.save(Matchers.any(ShmMovementExcp.class), eq(entityManager)))
			.thenReturn(generateShmMvmtExcp());

		final UpsertAdHocMovementExceptionResp resp = maintainAdHocMovementExceptionImpl
			.upsertAdHocMovementException(request, txnContext, entityManager);

		final MovementException exceptionResponce = resp.getMovementException();

		assertEquals(SHP_INST_ID, exceptionResponce.getShipmentInstId());
		assertEquals(BasicTransformer.toBigInteger(MVMT_SEQ_NBR), exceptionResponce.getMovementSequenceNbr());
		assertEquals(
			BasicTransformer.toBigInteger(MVMT_SEQ_NBR),
			exceptionResponce.getMovementSequenceNbr());
		assertEquals(
			BasicTransformer.toBigInteger(SEQ_NBR),
			exceptionResponce.getSequenceNbr());
	}

	@Test
	public void testUpsert_addAdHocMovementException_expectAuditInfoToBePopulated() throws Exception {
		final UpsertAdHocMovementExceptionRqst rqst = new UpsertAdHocMovementExceptionRqst();

		final MovementException movementExceptionRqst = new MovementException();
		movementExceptionRqst.setShipmentInstId(5378835173941L);
		movementExceptionRqst.setMovementSequenceNbr(BigInteger.valueOf(2));
		movementExceptionRqst.setSequenceNbr(BigInteger.valueOf(2));
		movementExceptionRqst.setTypeCd(MovementExceptionTypeCd.AD_HOC);
		movementExceptionRqst.setRemarks("new test exception 5");
		movementExceptionRqst.setListActionCd(ActionCd.ADD);

		rqst.setMovementException(movementExceptionRqst);

		final ShmMovement shmMovementEntity = new ShmMovement();
		final ShmMovementPK id = new ShmMovementPK();
		id.setShpInstId(movementExceptionRqst.getShipmentInstId());
		id.setSeqNbr(movementExceptionRqst.getMovementSequenceNbr().longValue());
		shmMovementEntity.setId(id);

		when(shipmentMovementDAO.findById(id, entityManager)).thenReturn(shmMovementEntity);

		final ShmMovementExcpPK excpId = new ShmMovementExcpPK();
		excpId.setShpInstId(movementExceptionRqst.getShipmentInstId());
		excpId.setMvmtSeqNbr(movementExceptionRqst.getMovementSequenceNbr().longValue());
		when(shipmentMovementExceptionDAO.findMaxSeqNbr(excpId, entityManager)).thenReturn(SEQ_NBR.longValue());

		ShmMovementExcp shmEntity = DtoTransformer.toShmMovementExcp(movementExceptionRqst, new ShmMovementExcp());
		when(shipmentMovementExceptionDAO.save(any(ShmMovementExcp.class), eq(entityManager))).thenAnswer(i -> i.getArguments()[0]);

		final UpsertAdHocMovementExceptionResp resp = maintainAdHocMovementExceptionImpl
				.upsertAdHocMovementException(rqst, txnContext, entityManager);

		final MovementException movementExceptionResp = resp.getMovementException();

		assertNotNull(movementExceptionResp.getAuditInfo());
		assertNotNull(movementExceptionResp.getAuditInfo().getCreatedTimestamp());
		assertNotNull(movementExceptionResp.getAuditInfo().getCreateByPgmId());
	}

	private MovementException generateMovementException() {
		final MovementException exception = new MovementException();
		exception.setShipmentInstId(SHP_INST_ID);
		exception.setSequenceNbr(BasicTransformer.toBigInteger(SEQ_NBR));
		exception.setMovementSequenceNbr(BasicTransformer.toBigInteger(MVMT_SEQ_NBR));
		exception.setTypeCd(MovementExceptionTypeCd.AD_HOC);
		exception.setListActionCd(ActionCd.ADD);
		return exception;
	}

	private ShmMovementExcp generateShmMvmtExcp() {
		final ShmMovementExcp actionEntity = new ShmMovementExcp();
		final ShmMovementExcpPK id = new ShmMovementExcpPK();
		id.setShpInstId(SHP_INST_ID);
		id.setSeqNbr(SEQ_NBR);
		id.setMvmtSeqNbr(MVMT_SEQ_NBR.longValue());

		actionEntity.setId(id);
		actionEntity.setTypCd(MovementExceptionTypeCdTransformer.toCode(MovementExceptionTypeCd.AD_HOC));

		return actionEntity;
	}


}
