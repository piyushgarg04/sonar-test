package com.xpo.ltl.shipment.service.impl.updateshipment.update.abc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCust;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.ShipmentAsEnteredCustomerDAO;
import com.xpo.ltl.shipment.service.impl.updateshipment.MockParent;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class AbstractAsMatchedPartyTest extends MockParent {
	@Mock
	private EntityManager entityManager;

	@Mock
	private EntityManager db2EntityManager;

	@Mock
	private TransactionContext transactionContext;

	@Mock
	private ShipmentAsEnteredCustomerDAO shipmentAsEnteredCustomerDAO;

	private List<ShmAsEntdCust> shmAsEntdCusts = new ArrayList<>();

	@Mock
	private EntityTransaction entityTransaction;

	@Mock
	private AppContext appContext;
	@InjectMocks
	private AbstractAsMatchedParty abstractAsMatchedParty = Mockito.spy(new AbstractAsMatchedParty() {
	});

	AbstractAsMatchedPartyTest() throws InstantiationException, IllegalAccessException {
	}

	@BeforeEach
	void setUp() {
		MockitoAnnotations.initMocks(this);
		final User user = new User();
		user.setEmployeeId("testUser");

		transactionContext.setUser(user);
	}

	@Test
	public void testSetTxInfo() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchFieldException {
		// Arrange

		String mockTransactionCd = "testTransactionCd";

		List<ShmAsEntdCust> mockShmAsEntdCusts = new ArrayList<>();
		ShmAsEntdCust mockShmAsEntdCust1 = new ShmAsEntdCust();
		mockShmAsEntdCusts.add(mockShmAsEntdCust1);

		// Reflection: Get the private method "setTxInfo" from the AbstractAsMatchedParty class
		Method setTxInfoMethod = AbstractAsMatchedParty.class.getDeclaredMethod("setTxInfo",
				TransactionContext.class,
				List.class,
				String.class);
		setTxInfoMethod.setAccessible(true);
		setTxInfoMethod.invoke(abstractAsMatchedParty, transactionContext, mockShmAsEntdCusts, mockTransactionCd);

		// Assert

		assertEquals(mockTransactionCd, mockShmAsEntdCust1.getLstUpdtTranCd());
		assertEquals("LTLAPP_USER", mockShmAsEntdCust1.getLstUpdtUid());

	}

	@Test
	public void testSetShmAsEntdCustDefaultValues() throws Exception {

		ShmAsEntdCust mockShmAsEntdCust = new ShmAsEntdCust();
		Method setShmAsEntdCustDefaultValuesMethod = AbstractAsMatchedParty.class.getDeclaredMethod("setShmAsEntdCustDefaultValues",
				ShmAsEntdCust.class);
		setShmAsEntdCustDefaultValuesMethod.setAccessible(true);

		// Invoke the private method
		setShmAsEntdCustDefaultValuesMethod.invoke(abstractAsMatchedParty, mockShmAsEntdCust);

		assertEquals(BigDecimal.ZERO, mockShmAsEntdCust.getAlternateCustNbr());
		assertEquals(BigDecimal.ZERO, mockShmAsEntdCust.getBrkrCustKeyNbr());
		assertEquals(BigDecimal.ZERO, mockShmAsEntdCust.getCisCustNbr());
		assertNotNull(mockShmAsEntdCust.getLstMchTmst());
		assertNotNull(mockShmAsEntdCust.getLstUpdtTmst());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getAddrTxt());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getAllShpmtPpdInd());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getArchiveCntlCd());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getAsMchMadCd());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getBiltoRelCd());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getCntryCd());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getCredStatCd());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getCtyTxt());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getDebtorInd());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getDirCd());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getLstUpdtUid());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getMchInitTxt());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getMchSourceCd());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getMchStatCd());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getName1Txt());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getName2Txt());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getPacdNbr());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getPccdNbr());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getPextNbr());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getPhonNbr());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getPodImgInd());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getPodRqrdInd());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getPrefPmtCrncyCd());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getSelfInvcInd());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getStCd());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getTypCd());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getUseAsEntrdInd());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getZip4RestUsTxt());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getZip6Txt());
		assertEquals(StringUtils.SPACE, mockShmAsEntdCust.getEMailId());
	}

	@Test
	public void testGetDefaultTimestamp() throws Exception {

		Method getDefaultTimestampMethod = AbstractAsMatchedParty.class.getDeclaredMethod("getDefaultTimestamp");
		getDefaultTimestampMethod.setAccessible(true);

		// Invoke the private method
		Timestamp defaultTimestamp = (Timestamp) getDefaultTimestampMethod.invoke(abstractAsMatchedParty);

		// Assert the result
		assertNotNull(defaultTimestamp);

		// Parse the expected date string
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		Date expectedDate = sdf.parse("0001-12-30T04:00:00.000Z");

		// Convert the expected date to Timestamp
		Timestamp expectedTimestamp = new Timestamp(expectedDate.getTime());

		// Assert the timestamp value
		assertEquals(expectedTimestamp, defaultTimestamp);
	}

	@Test
	public void testUpdateAsMatchedParties() throws Exception {
		String pro = "06420172510";
		shmAsEntdCusts = new ArrayList<>();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		shmAsEntdCusts = (List<ShmAsEntdCust>) jsonStringToObject(shmAsEntdCusts.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmAsEntdCust"));
		shmAsEntdCusts = objectMapper.convertValue(shmAsEntdCusts, new TypeReference<List<ShmAsEntdCust>>() {
		});

		ShmAsEntdCust shmAsEntdCust = new ShmAsEntdCust();
		shmAsEntdCusts.add(shmAsEntdCust);

		// Mock EntityManager and transactions
		when(entityManager.getTransaction()).thenReturn(entityTransaction);
		when(entityTransaction.isActive()).thenReturn(true);
		doNothing().when(entityTransaction).begin();
		doNothing().when(entityTransaction).commit();
		doNothing().when(entityTransaction).rollback();

		// Mock DAO methods
		doNothing().when(shipmentAsEnteredCustomerDAO).persist(anyList(), any(EntityManager.class));
		doNothing().when(shipmentAsEnteredCustomerDAO).updateDB2ShmAsEntdCust(any(), any(), any(), any());

		// Call the method
		when(appContext.getApplyDb2TwoPhaseCommit()).thenReturn(true);
		assertDoesNotThrow(() -> {
			abstractAsMatchedParty.updateAsMatchedParties(entityManager,
					db2EntityManager,
					transactionContext,
					shmAsEntdCusts,
					"TEST");

		});

		assertEquals("TEST", shmAsEntdCust.getLstUpdtTranCd());
	}

	@Test
	public void testDeleteAsMatchedParties() throws Exception {
		String pro = "06420172510";
		shmAsEntdCusts = new ArrayList<>();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		shmAsEntdCusts = (List<ShmAsEntdCust>) jsonStringToObject(shmAsEntdCusts.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmAsEntdCust"));
		shmAsEntdCusts = objectMapper.convertValue(shmAsEntdCusts, new TypeReference<List<ShmAsEntdCust>>() {
		});

		// Mock EntityManager and transactions
		when(entityManager.getTransaction()).thenReturn(entityTransaction);
		when(entityTransaction.isActive()).thenReturn(true);
		doNothing().when(entityTransaction).begin();
		doNothing().when(entityTransaction).commit();
		doNothing().when(entityTransaction).rollback();

		// Mock DAO methods
		doNothing().when(shipmentAsEnteredCustomerDAO).persist(anyList(), any(EntityManager.class));
		doNothing().when(shipmentAsEnteredCustomerDAO).updateDB2ShmAsEntdCust(any(), any(), any(), any());

		// Call the method
		when(appContext.getApplyDb2TwoPhaseCommit()).thenReturn(true);
		assertDoesNotThrow(() -> {
			abstractAsMatchedParty.deleteAsMatchedParties(entityManager,
					db2EntityManager,
					transactionContext,
					shmAsEntdCusts);
		});

	}

	@Test
	public void testInsertAsMatchedParties() throws Exception {

		// Create test data
		shmAsEntdCusts = new ArrayList<>();
		ShmAsEntdCust shmAsEntdCust = new ShmAsEntdCust();
		shmAsEntdCusts.add(shmAsEntdCust);

		// Mock DAO methods
		doNothing().when(shipmentAsEnteredCustomerDAO).persist(anyList(), any(EntityManager.class));
		//		doNothing().when(shipmentAsEnteredCustomerDAO).createDB2ShmAsEntdCust(any(), any(EntityManager.class));
		when(appContext.getApplyDb2TwoPhaseCommit()).thenReturn(true);
		// Call the method
		assertDoesNotThrow(() -> {
			abstractAsMatchedParty.insertAsMatchedParties(entityManager,
					db2EntityManager,
					transactionContext,
					shmAsEntdCusts,
					"TEST");
		});

		// Assertions for properties after the method call
		assertNotNull(shmAsEntdCust.getLstUpdtTmst());
		assertNotNull(shmAsEntdCust.getLstUpdtTranCd());

	}
}