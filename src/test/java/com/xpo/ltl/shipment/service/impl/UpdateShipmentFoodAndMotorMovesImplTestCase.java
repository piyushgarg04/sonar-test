package com.xpo.ltl.shipment.service.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.FoodPoisonCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentFoodAndMotorMovesRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;

public class UpdateShipmentFoodAndMotorMovesImplTestCase {

	private static final Long SHP_INST_ID = new Long(1234);
	private static final String PRO_NUMBER = "02080966063";

	@Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

	@Mock
	private EntityManager db2EntityManager;

	@Mock
	private ShmShipmentSubDAO shipmentDAO;

	@InjectMocks
	private UpdateShipmentFoodAndMotorMovesImpl updateShipmentFoodAndMotorMovesImpl;

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
	public void testUpdateShipmentFoodAndMotorMoves_RequestRequired() {

		Throwable exception = Assertions
				.assertThrows(Throwable.class, () ->
						updateShipmentFoodAndMotorMovesImpl
								.updateShipmentFoodAndMotorMoves(null, txnContext,
										entityManager));

		Assertions.assertTrue(exception.getMessage()
				.contains(ValidationErrorMessage.REQUEST_REQUIRED.message()));
	}

	@Test
	public void testUpdateShipmentFoodAndMotorMoves_TransactionContextRequired() {

		UpdateShipmentFoodAndMotorMovesRqst request = new UpdateShipmentFoodAndMotorMovesRqst();

		Throwable exception = Assertions
				.assertThrows(Throwable.class, () ->
						updateShipmentFoodAndMotorMovesImpl
								.updateShipmentFoodAndMotorMoves(request, null, entityManager));

		Assertions.assertTrue(exception.getMessage()
				.contains(ValidationErrorMessage.TXN_CONTEXT_REQUIRED.message()));
	}

	@Test
	public void testUpdateShipmentFoodAndMotorMoves_EntityManagerRequired() {

		UpdateShipmentFoodAndMotorMovesRqst request = new UpdateShipmentFoodAndMotorMovesRqst();

		Throwable exception = Assertions
				.assertThrows(Throwable.class, () ->
						updateShipmentFoodAndMotorMovesImpl
								.updateShipmentFoodAndMotorMoves(request, txnContext, null));

		Assertions.assertTrue(exception.getMessage()
				.contains(ValidationErrorMessage.ENTITY_MANAGER_REQUIRED.message()));
	}

	@Test
	public void testUpdateShipmentFoodAndMotorMoves_ShipmentInstanceIdOrProRequired() {

		UpdateShipmentFoodAndMotorMovesRqst request = new UpdateShipmentFoodAndMotorMovesRqst();
		request.setProNbr(null);
		request.setShipmentInstId(null);

		ServiceException exception = Assertions
				.assertThrows(ServiceException.class, () ->
						updateShipmentFoodAndMotorMovesImpl
								.updateShipmentFoodAndMotorMoves(request, txnContext,
										entityManager));

		Assertions.assertTrue(exception.getMessage()
				.contains(ValidationErrorMessage.SHIP_ID_OR_PRO_IS_REQUIRED.message()));
	}

	@Test
	public void testUpdateShipmentFoodAndMotorMoves_MotorizedPiecesCountRequired() {

		UpdateShipmentFoodAndMotorMovesRqst request = new UpdateShipmentFoodAndMotorMovesRqst();
		request.setProNbr(null);
		request.setShipmentInstId(SHP_INST_ID);
		request.setUpdateMotorMovesInd(Boolean.TRUE);

		ServiceException exception = Assertions
				.assertThrows(ServiceException.class, () ->
						updateShipmentFoodAndMotorMovesImpl
								.updateShipmentFoodAndMotorMoves(request, txnContext,
										entityManager));

		Assertions.assertTrue(exception.getMessage()
				.contains(ValidationErrorMessage.MOTORIZED_PIECES_COUNT_REQ.message()));
	}

	@Test
	public void testUpdateShipmentFoodAndMotorMoves_MotorizedPiecesCountVInvalidValue() {

		UpdateShipmentFoodAndMotorMovesRqst request = new UpdateShipmentFoodAndMotorMovesRqst();
		request.setProNbr(null);
		request.setShipmentInstId(SHP_INST_ID);
		request.setUpdateMotorMovesInd(Boolean.TRUE);
		request.setMotorizedPiecesCount(new BigInteger("-1"));

		ServiceException exception = Assertions
				.assertThrows(ServiceException.class, () ->
						updateShipmentFoodAndMotorMovesImpl
								.updateShipmentFoodAndMotorMoves(request, txnContext,
										entityManager));

		Assertions.assertTrue(exception.getMessage()
				.contains(ValidationErrorMessage.MPIECES_COUNT_GREATER_ZERO.message()));
	}

	@Test
	public void testUpdateShipmentFoodAndMotorMoves_LoosePiecesCount() {

		UpdateShipmentFoodAndMotorMovesRqst request = new UpdateShipmentFoodAndMotorMovesRqst();
		request.setProNbr(null);
		request.setShipmentInstId(SHP_INST_ID);
		request.setLoosePiecesCount(new BigInteger("-1"));

		ServiceException exception = Assertions
				.assertThrows(ServiceException.class, () ->
						updateShipmentFoodAndMotorMovesImpl
								.updateShipmentFoodAndMotorMoves(request, txnContext,
										entityManager));

		Assertions.assertTrue(exception.getMessage()
				.contains(ValidationErrorMessage.LPIECES_COUNT_GREATER_ZERO.message()));
	}

	@Test
	public void testUpdateShipmentFoodAndMotorMoves_ShipmentNotFoundException() {

		UpdateShipmentFoodAndMotorMovesRqst request = new UpdateShipmentFoodAndMotorMovesRqst();
		request.setProNbr(null);
		request.setShipmentInstId(SHP_INST_ID);
		request.setFoodPoisonCd(FoodPoisonCd.FOOD);

		when(shipmentDAO.findByIdOrProNumber(null, SHP_INST_ID, entityManager))
				.thenReturn(null);

		ServiceException exception = Assertions
				.assertThrows(ServiceException.class, () ->
						updateShipmentFoodAndMotorMovesImpl
								.updateShipmentFoodAndMotorMoves(request, txnContext,
										entityManager));

		Assertions.assertTrue(exception.getMessage()
				.contains(NotFoundErrorMessage.SHIPMENT_NOT_FOUND.message()));
	}

	@Test
	public void testUpdateShipmentFoodAndMotorMoves_ReturnsValidResponse() throws Exception {
		UpdateShipmentFoodAndMotorMovesRqst request = new UpdateShipmentFoodAndMotorMovesRqst();
		request.setProNbr(PRO_NUMBER);
		request.setFoodPoisonCd(FoodPoisonCd.FOOD);
		request.setUpdateMotorMovesInd(Boolean.TRUE);
		request.setLoosePiecesCount(BigInteger.TEN);
		request.setMotorizedPiecesCount(BigInteger.TEN);
		ShmShipment shmShipment = new ShmShipment();
		shmShipment.setTotPcsCnt(BigDecimal.TEN);
		shmShipment.setProNbrTxt(PRO_NUMBER);

		when(shipmentDAO.findByIdOrProNumber(PRO_NUMBER, null, entityManager)).thenReturn(shmShipment);

		updateShipmentFoodAndMotorMovesImpl.updateShipmentFoodAndMotorMoves(request, txnContext, entityManager);

		verify(shipmentDAO, times(1)).save(any(), any());

		verify(shipmentDAO, times(1)).updateDB2ShmShipment(any(ShmShipment.class), any(),
				eq(txnContext), eq(db2EntityManager));

	}

}