//package com.xpo.ltl.shipment.service.impl;
//
//import static org.mockito.Mockito.when;
//
//import java.util.Calendar;
//
//import javax.persistence.EntityManager;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import com.xpo.ltl.api.rest.TransactionContext;
//import com.xpo.ltl.api.rest.User;
//import com.xpo.ltl.api.shipment.v2.OverageImageHeader;
//import com.xpo.ltl.api.transformer.BasicTransformer;
//import com.xpo.ltl.shipment.service.dao.ShmOvrgHdrSubDao;
//
//import junit.framework.TestCase;
//
//public class MaintainShmOvrgHdrImplTestCase extends TestCase {
//
//
//	@Mock
//	private TransactionContext txnContext;
//
//	@Mock
//	private EntityManager entityManager;
//
//	@InjectMocks
//	private MaintainShmOvrgHdrImpl maintainShmOvrgHdrImpl;
//
//	@Mock
//	private ShmOvrgHdrSubDao shmOvrgHdrDao;
//
//
//	@Override
//	@Before
//	public void setUp() {
//		MockitoAnnotations.initMocks(this);
//
//		when(txnContext.getSrcApplicationId()).thenReturn("JUNIT");
//
//		final User user = new User();
//		user.setUserId("JUNIT");
//		user.setEmployeeId("JUNIT");
//		when(txnContext.getUser()).thenReturn(user);
//
//		when(txnContext.getTransactionTimestamp())
//				.thenReturn(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance()));
//
//		when(txnContext.getCorrelationId()).thenReturn("0");
//	}
//
//	@Test
//	public void testUpsert_HeaderRequired() throws Exception {
//		try {
//			maintainShmOvrgHdrImpl.upsert(null, txnContext, entityManager);
//
//			fail("Expected an exception.");
//		} catch (final Exception e) {
//			assertEquals("The header is required.", e.getMessage());
//		}
//	}
//
//	@Test
//	public void testUpsert_TxnContextRequired() throws Exception {
//		try {
//			maintainShmOvrgHdrImpl.upsert(new OverageImageHeader(), null, entityManager);
//
//			fail("Expected an exception.");
//		} catch (final Exception e) {
//			assertEquals("The TransactionContext is required.", e.getMessage());
//		}
//	}
//
//	@Test
//	public void testUpsert_EntityManagerRequired() throws Exception {
//		try {
//			maintainShmOvrgHdrImpl.upsert(new OverageImageHeader(), txnContext, null);
//
//			fail("Expected an exception.");
//		} catch (final Exception e) {
//			assertEquals("The EntityManager is required.", e.getMessage());
//		}
//	}
//
//}