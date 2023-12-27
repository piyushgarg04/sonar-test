package com.xpo.ltl.shipment.service.impl;

import static org.mockito.Mockito.when;

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
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcpPK;
import com.xpo.ltl.api.shipment.service.entity.ShmMvmtExcpAction;
import com.xpo.ltl.api.shipment.service.entity.ShmMvmtExcpActionPK;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.MovementExceptionAction;
import com.xpo.ltl.api.shipment.v2.UpsertMovementExceptionActionResp;
import com.xpo.ltl.api.shipment.v2.UpsertMovementExceptionActionRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShipmentManagementRemarkSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentMovementExceptionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmMvmtExcpActionSubDAO;
import com.xpo.ltl.shipment.service.ejb.v1.ShipmentServiceBean;

import junit.framework.TestCase;

public class MaintainShipmentMovementExceptionActionImplTestCase extends TestCase {
	private static final Long SHP_INST_ID = new Long(1234);
	private static final Integer MVMT_SEQ_NBR = 1;
	private static final Integer SEQ_NBR = 2;
	private static final Integer MVMT_EXCP_SEQ_NBR = 3;

	@Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

	@Mock
	private ShipmentMovementExceptionSubDAO shipmentMovementExceptionDAO;

	@Mock
	private ShipmentManagementRemarkSubDAO shipmentManagementRemarkDAO;

	@Mock
	private ShmMvmtExcpActionSubDAO exceptionActionDAO;

	@Mock
	private AuditInfo auditInfo;

	@InjectMocks
	private MaintainShipmentMovementExceptionActionImpl maintainShipmentMovementExceptionActionImpl;

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
			maintainShipmentMovementExceptionActionImpl.upsertMovementExceptionAction(null, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertEquals("The request is required.", e.getMessage());
		}
	}

	@Test
	public void testUpsert_TransactionContextRequired() throws Exception
	{
		try {
			final UpsertMovementExceptionActionRqst request = new UpsertMovementExceptionActionRqst();
			maintainShipmentMovementExceptionActionImpl.upsertMovementExceptionAction(request, null, entityManager);

			fail("Expected an exception");
		} catch (final Exception e) {
			assertEquals("The TransactionContext is required.", e.getMessage());
		}
	}

	@Test
	public void testUpsert_EntityManagerRequired() throws Exception
	{
		try {
			final UpsertMovementExceptionActionRqst request = new UpsertMovementExceptionActionRqst();
			maintainShipmentMovementExceptionActionImpl.upsertMovementExceptionAction(request, txnContext, null);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertEquals("The EntityManager is required.", e.getMessage());
		}
	}

	@Test
	public void testUpsert_ShipmentMovementExceptionActionRequired() throws Exception
	{
		try {
			final UpsertMovementExceptionActionRqst request = new UpsertMovementExceptionActionRqst();
			maintainShipmentMovementExceptionActionImpl
				.upsertMovementExceptionAction(request, txnContext, entityManager);

			fail("Expected an exception.");
		} catch (final Exception e) {
			assertEquals("The shipment movement exception action is required.", e.getMessage());
		}
	}

	@Test
	public void testUpsert_ReturnsValidResponseWithShipmentManagementRemarkPK() throws Exception {
		final MovementExceptionAction action = new MovementExceptionAction();
		final UpsertMovementExceptionActionRqst request = new UpsertMovementExceptionActionRqst();

		action.setShipmentInstId(SHP_INST_ID);
		action.setSequenceNbr(BasicTransformer.toBigInteger(SEQ_NBR));
		action.setMovementSequenceNbr(BasicTransformer.toBigInteger(MVMT_SEQ_NBR));
		action.setMovementExceptionSequenceNbr(BasicTransformer.toBigInteger(MVMT_EXCP_SEQ_NBR));
		action.setReasonCd("Test");
		action.setListActionCd(ActionCd.ADD);
		request.setMovementExceptionAction(action);

		final ShmMvmtExcpActionPK actionId = new ShmMvmtExcpActionPK();
		actionId.setShpInstId(SHP_INST_ID);
		actionId.setSeqNbr(SEQ_NBR);
		actionId.setMvmtExcpSeqNbr(MVMT_EXCP_SEQ_NBR.longValue());
		actionId.setMvmtSeqNbr(MVMT_SEQ_NBR.longValue());

		final ShmMovementExcp shmMovementExcpEntity = new ShmMovementExcp();
		final ShmMovementExcpPK id = new ShmMovementExcpPK();
		id.setShpInstId(SHP_INST_ID);
		id.setSeqNbr(MVMT_EXCP_SEQ_NBR);
		id.setMvmtSeqNbr(MVMT_SEQ_NBR);
		shmMovementExcpEntity.setId(id);

		when(shipmentMovementExceptionDAO.findById(id, entityManager)).thenReturn(shmMovementExcpEntity);
		when(exceptionActionDAO.findMaxSeqNbr(actionId, entityManager))
			.thenReturn(SEQ_NBR.longValue());
		when(exceptionActionDAO.save(Matchers.any(ShmMvmtExcpAction.class), Matchers.eq(entityManager)))
			.thenReturn(generateShmMvmtExcpAction());

		final UpsertMovementExceptionActionResp resp = maintainShipmentMovementExceptionActionImpl
			.upsertMovementExceptionAction(request, txnContext, entityManager);

		final MovementExceptionAction actionResponce = resp.getMovementExceptionAction();

		assertEquals(SHP_INST_ID, actionResponce.getShipmentInstId());
		assertEquals(BasicTransformer.toBigInteger(MVMT_SEQ_NBR), actionResponce.getMovementSequenceNbr());
		assertEquals(
			BasicTransformer.toBigInteger(MVMT_EXCP_SEQ_NBR),
			actionResponce.getMovementExceptionSequenceNbr());
		assertEquals(
			BasicTransformer.toBigInteger(SEQ_NBR),
			actionResponce.getSequenceNbr());
	}

	private ShmMvmtExcpAction generateShmMvmtExcpAction() {
		final ShmMvmtExcpAction actionEntity = new ShmMvmtExcpAction();
		final ShmMvmtExcpActionPK id = new ShmMvmtExcpActionPK();
		id.setShpInstId(SHP_INST_ID);
		id.setSeqNbr(SEQ_NBR);
		id.setMvmtExcpSeqNbr(MVMT_EXCP_SEQ_NBR.longValue());
		id.setMvmtSeqNbr(MVMT_SEQ_NBR.longValue());

		actionEntity.setId(id);
		actionEntity.setReasonCd("Test");

		return actionEntity;
	}


}