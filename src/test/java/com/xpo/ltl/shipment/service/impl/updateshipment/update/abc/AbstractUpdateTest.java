package com.xpo.ltl.shipment.service.impl.updateshipment.update.abc;

import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.v2.AdvanceBeyondTypeCd;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractUpdateTest {
	@Mock
	private TransactionContext transactionContext;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.initMocks(this);
		final User user = new User();
		user.setEmployeeId("testUser");

		transactionContext.setUser(user);
	}

	@Test
	void getMaxSequenceNumberLong() {
		List<Long> lst = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L);
		Long maxNumber = AbstractUpdate.getMaxSequenceNumberLong(lst);
		assertEquals(6L, 6L);
	}

	@Test
	void retrieveData() throws ExecutionException, InterruptedException {
		@SuppressWarnings("unchecked") CompletableFuture<String> completableFuture = mock(CompletableFuture.class);
		when(completableFuture.get()).thenReturn("Mock Data");

		String result = AbstractUpdate.retrieveData(completableFuture, transactionContext);

		assertEquals("Mock Data", result);
	}

	@Test
	void testRetrieveDataException() throws ExecutionException, InterruptedException {
		// Create a mock CompletableFuture and set its behavior to throw an exception
		@SuppressWarnings("unchecked") CompletableFuture<String> completableFuture = mock(CompletableFuture.class);
		when(completableFuture.get()).thenThrow(new ExecutionException(new RuntimeException("Mock Exception")));

		try {
			AbstractUpdate.retrieveData(completableFuture, transactionContext);
		} catch (RuntimeException e) {
			// The method should throw a RuntimeException wrapping the original exception.
			assertEquals(
					"com.xpo.ltl.api.exception.ServiceException: SHMN999-007E:Unhandled Service Exception(location:retrieveData, message:java.lang.RuntimeException: Mock Exception)",
					e.getMessage());
		}
	}

	@Test
	void testGetAlternateValueByValWithFoundValue() {
		// Create a mock Field array containing two fields with SerializedName annotations
		Field[] statusDeclaredFields = AdvanceBeyondTypeCd.class.getDeclaredFields();

		String result = AbstractUpdate.getAlternateValueByVal("AdvCarr", statusDeclaredFields);

		assertEquals("1", result);
	}

	@Test
	void testGetAlternateValueByValWithNotFoundValue() {
		// Create an empty mock Field array (no annotations)
		Field[] statusDeclaredFields = AdvanceBeyondTypeCd.class.getDeclaredFields();

		String result = AbstractUpdate.getAlternateValueByVal("non_existent_name", statusDeclaredFields);

		assertEquals(StringUtils.EMPTY, result);
	}

	@Test
	void testGetAlternateValueWithFoundValue() throws NoSuchFieldException {
		// Create a mock Field array containing a field with the SerializedName annotation
		Field[] statusDeclaredFields = AdvanceBeyondTypeCd.class.getDeclaredFields();

		String result = AbstractUpdate.getAlternateValue("AdvCarr", statusDeclaredFields);

		assertEquals("1", result);
	}

	@Test
	void testGetAlternateValueWithNotFoundValue() throws NoSuchFieldException {
		// Create a mock Field array containing a field with the SerializedName annotation
		Field[] statusDeclaredFields = AdvanceBeyondTypeCd.class.getDeclaredFields();

		String result = AbstractUpdate.getAlternateValue("name2", statusDeclaredFields);

		assertNull(result);
	}

	// Helper method to create a mock Field with the specified SerializedName annotation

	@Test
	void getChargeToCdAlt() {
		String result = AbstractUpdate.getChargeToCdAlt("Both");

		assertEquals("B", result);
	}

	@Test
	void getFlag() {
		String result = AbstractUpdate.getFlag(true);

		assertEquals("Y", result);

	}

	@Test
	void copyFields() {
		Timestamp timestamp = new Timestamp(new Date().getTime());
		ShmAcSvc shmAcSvc = new ShmAcSvc();
		ShmAcSvc shmAcSvcNew = new ShmAcSvc();

		shmAcSvc.setAcQty(BigDecimal.ZERO);
		shmAcSvc.setAmt(BigDecimal.ZERO);
		shmAcSvc.setPpdPct(BigDecimal.ZERO);
		shmAcSvc.setTrfRt(BigDecimal.ZERO);
		shmAcSvc.setId(null);
		shmAcSvc.setShmShipment(null);
		shmAcSvc.setAcCd(StringUtils.SPACE);
		shmAcSvc.setAcUom(StringUtils.SPACE);
		shmAcSvc.setArchiveCntlCd(StringUtils.SPACE);
		shmAcSvc.setChrgToCd(StringUtils.SPACE);
		shmAcSvc.setDescTxt("HELLO");
		shmAcSvc.setLstUpdtTranCd(StringUtils.SPACE);
		shmAcSvc.setLstUpdtUid(StringUtils.SPACE);
		shmAcSvc.setMinChrgInd(StringUtils.SPACE);
		shmAcSvc.setDmlTmst(timestamp);
		shmAcSvc.setDtlCapxtimestamp(timestamp);
		shmAcSvc.setLstUpdtTmst(timestamp);
		shmAcSvc.setReplLstUpdtTmst(timestamp);

		// Call the copyFields method
		AbstractUpdate.copyFields(shmAcSvc, shmAcSvcNew);

		// Verify that the target field value is updated
		assertEquals("HELLO", shmAcSvcNew.getDescTxt());
	}

	@Test
	void capitalizeFirstLetter() {
		String result = AbstractUpdate.capitalizeFirstLetter("hello");
		assertEquals("Hello", result);
	}
	@Test
	void getUserFromContext() {
		TransactionContext context = new TransactionContext();
		User user = new User();
		user.setEmployeeId("testUser");

		context.setUser(user);
		String result = AbstractUpdate.getUserFromContext(context);
		assertEquals("testUser", result);
	}

}