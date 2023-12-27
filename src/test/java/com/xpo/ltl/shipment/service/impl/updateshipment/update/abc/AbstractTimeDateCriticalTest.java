package com.xpo.ltl.shipment.service.impl.updateshipment.update.abc;

import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmTmDtCritical;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.ShipmentTdcSubDAO;
import org.hibernate.HibernateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractTimeDateCriticalTest {

	@Mock
	private EntityManager entityManager;

	@Mock
	private ShipmentTdcSubDAO shipmentTdcSubDAO;

	@Mock
	private TransactionContext txnContext;
	@Mock
	private AppContext appContext;
	@InjectMocks
	private AbstractTimeDateCritical abstractTimeDateCritical = mock(AbstractTimeDateCritical.class,
			Answers.CALLS_REAL_METHODS.get());
	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		final User user = new User();
		user.setEmployeeId("EmployeeId");

		txnContext.setUser(user);
		when(appContext.getDb2CommitEnabledForUpdateShipment()).thenReturn(true);
	}

	@Test
	public void testUpdateTimeDateCriticalsThrowsValidationException() throws Exception {
		// Create test data
		List<ShmTmDtCritical> testList = new ArrayList<>();
		testList.add(new ShmTmDtCritical());

		EntityManager mockEntityManager = mock(EntityManager.class);
		EntityManager mockDb2EntityManager = mock(EntityManager.class);
		TransactionContext mockTransactionContext = mock(TransactionContext.class);

		// Mock the behavior of the DAO to throw the desired exception
		NotFoundException notFoundExceptionExceptionBuilder = ExceptionBuilder
				.exception(NotFoundErrorMessage.SHM_TM_DT_CRITICAL_NF, mockTransactionContext)
				.moreInfo("TmDtCritical", NotFoundErrorMessage.SHM_TM_DT_CRITICAL_NF)
				.build();
		doThrow(notFoundExceptionExceptionBuilder).when(shipmentTdcSubDAO).updateDB2ShmTmDtCritical(any(), any(), any(), any());


		final UnsupportedOperationException unsupportedOperationException = assertThrows(UnsupportedOperationException.class, () -> {
			abstractTimeDateCritical.updateTimeDateCriticals(mockEntityManager, mockDb2EntityManager, mockTransactionContext, testList, "testTransactionCd");

		});

		assertEquals(
				"com.xpo.ltl.api.exception.ServiceException: SHMN041-933E:TDC Update Failed(location:TmDtCritical, message:SHMN010-941E:Shipment Time Date Critical information not found(location:TmDtCritical, message:Shipment Time Date Critical information not found, errorCode:SHMN010-941E))",
				unsupportedOperationException
						.getMessage());



	}


	@Test
	void addTimeDateCriticals() throws ValidationException, NotFoundException {
		List<ShmTmDtCritical> testList = new ArrayList<>();
		testList.add(new ShmTmDtCritical());

		EntityManager mockEntityManager = mock(EntityManager.class);
		EntityManager mockDb2EntityManager = mock(EntityManager.class);
		TransactionContext mockTransactionContext = mock(TransactionContext.class);

		// Mock the behavior of the DAO to throw the desired exception
		HibernateException hibernateException = new HibernateException("Error");
		doThrow(hibernateException).when(shipmentTdcSubDAO).createDB2ShmTmDtCritical(any(), any());


		final UnsupportedOperationException unsupportedOperationException = assertThrows(UnsupportedOperationException.class, () -> {
			abstractTimeDateCritical.addTimeDateCriticals(mockEntityManager, mockDb2EntityManager, mockTransactionContext, testList, "testTransactionCd");

		});

		assertEquals(
				"com.xpo.ltl.api.exception.ServiceException: SHMN040-941E:TDC Create Failed(location:TmDtCritical, message:Error)",
				unsupportedOperationException
						.getMessage());
	}
}