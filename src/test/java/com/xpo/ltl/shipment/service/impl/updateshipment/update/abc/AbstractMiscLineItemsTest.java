package com.xpo.ltl.shipment.service.impl.updateshipment.update.abc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItemPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.ShmMiscLineItemSubDAO;
import com.xpo.ltl.shipment.service.impl.updateshipment.MockParent;
import com.xpo.ltl.shipment.service.impl.updateshipment.functional.LoadValuesToUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.functional.LoadValuesToUpdateManRateMiscImpl;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractMiscLineItemsTest extends MockParent {
	@InjectMocks
	private AbstractMiscLineItems abstractMiscLineItems = Mockito.spy(new AbstractMiscLineItems() {
	});

	@Mock
	private EntityManager entityManager;

	@Mock
	private EntityManager db2EntityManager;

	@Mock
	private TransactionContext transactionContext = mock(TransactionContext.class);

	@Mock
	private ShmMiscLineItemSubDAO shmMiscLineItemSubDAO;
	@Mock
	private AppContext appContext;
	AbstractMiscLineItemsTest() throws InstantiationException, IllegalAccessException {
	}

	@BeforeEach
	void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testSetDefaultValuesMiscLineItem() {

		// Prepare input values
		String userId = "testUser";
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		ShmMiscLineItem shmMiscLineItem = new ShmMiscLineItem();

		// Execute the method
		AbstractMiscLineItems.setDefaultValuesMiscLineItem(userId, timestamp, shmMiscLineItem);

		// Verify the default values are set correctly
		assertEquals(timestamp, shmMiscLineItem.getDmlTmst());
		assertEquals(timestamp, shmMiscLineItem.getDtlCapxtimestamp());
		assertEquals(timestamp, shmMiscLineItem.getLstUpdtTmst());
		assertEquals(timestamp, shmMiscLineItem.getReplLstUpdtTmst());
		assertEquals(userId, shmMiscLineItem.getLstUpdtUid());
		assertEquals(StringUtils.SPACE, shmMiscLineItem.getPmtMethCd());
		assertEquals(StringUtils.SPACE, shmMiscLineItem.getChrgToCd());
		assertEquals(StringUtils.SPACE, shmMiscLineItem.getArchiveCntlCd());
		assertEquals(StringUtils.SPACE, shmMiscLineItem.getCheckNbrTxt());
		assertEquals("N", shmMiscLineItem.getMinChrgInd());
		assertEquals(StringUtils.SPACE, shmMiscLineItem.getUom());
		assertEquals(BigDecimal.ZERO, shmMiscLineItem.getTrfRt());
		assertEquals(BigDecimal.ZERO, shmMiscLineItem.getQty());
		assertEquals(BigDecimal.ZERO, shmMiscLineItem.getPpdPct());

	}

	@Test
	public void testResetSeqNumberMiscLineItem() {
		String pro = "06420172510";
		List<ShmMiscLineItem> shmMiscLineItems = new ArrayList();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		shmMiscLineItems = (List<ShmMiscLineItem>) jsonStringToObject(shmMiscLineItems.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmMiscLineItem"));
		shmMiscLineItems = objectMapper.convertValue(shmMiscLineItems, new TypeReference<List<ShmMiscLineItem>>() {
		});
		ShmShipment shmShipment = (ShmShipment) jsonStringToObject(ShmShipment.class,
				this.getJsonFromProperty(pro + ".jsonShmShipment"));
		shmShipment = objectMapper.convertValue(shmShipment, new TypeReference<ShmShipment>() {
		});

		// Execute the method
		AbstractMiscLineItems.resetSeqNumberMiscLineItem(shmShipment.getShpInstId(), shmMiscLineItems);

		// Verify the sequence numbers and shipment IDs are set correctly
		AtomicReference<Long> seq = new AtomicReference<>(1L);
		ShmShipment finalShmShipment = shmShipment;
		shmMiscLineItems.forEach(shmMiscLineItem -> {
			if (Objects.nonNull(shmMiscLineItem.getId())) {
				assertEquals(seq.getAndSet(seq.get() + 1), shmMiscLineItem.getId().getSeqNbr());
				assertEquals(finalShmShipment.getShpInstId(), shmMiscLineItem.getId().getShpInstId());
			} else {
				ShmMiscLineItemPK expectedId = new ShmMiscLineItemPK();
				expectedId.setShpInstId(finalShmShipment.getShpInstId());
				expectedId.setSeqNbr(seq.getAndSet(seq.get() + 1));
				assertEquals(expectedId, shmMiscLineItem.getId());
			}
		});

		// ... Add more verification based on your logic ...
	}

	@Test
	public void testUpdateMiscLineItem() throws ValidationException, NotFoundException {
		String pro = "06420172510";
		List<ShmMiscLineItem> shmMiscLineItems = new ArrayList();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		shmMiscLineItems = (List<ShmMiscLineItem>) jsonStringToObject(shmMiscLineItems.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmMiscLineItem"));
		shmMiscLineItems = objectMapper.convertValue(shmMiscLineItems, new TypeReference<List<ShmMiscLineItem>>() {
		});
		String transactionCd = "TestTransaction";

		// Mock behavior for DAO methods
		doNothing().when(shmMiscLineItemSubDAO).persist(anyList(), eq(entityManager));
		doNothing()
				.when(shmMiscLineItemSubDAO)
				.updateDB2ShmMiscLineItem(any(ShmMiscLineItem.class),
						any(Function.class),
						eq(db2EntityManager),
						eq(transactionContext));

		// Execute the method
		when(appContext.getApplyDb2TwoPhaseCommit()).thenReturn(false);
		List<ShmMiscLineItem> finalShmMiscLineItems = shmMiscLineItems;
		assertDoesNotThrow(() -> {
			abstractMiscLineItems.updateMiscLineItem(entityManager,
					db2EntityManager,
					transactionContext,
					finalShmMiscLineItems,
					transactionCd,
					"user");
		});
	}

	@Test
	public void testDeleteMiscLineItems() {

		List<ShmMiscLineItem> shmMiscLineItemListToDelete = new ArrayList<>();
		ShmMiscLineItem shmMiscLineItem = new ShmMiscLineItem();
		shmMiscLineItemListToDelete.add(shmMiscLineItem);

		// Mock DAO methods
		doNothing().when(shmMiscLineItemSubDAO).remove(anyList(), any(EntityManager.class));
		when(appContext.getApplyDb2TwoPhaseCommit()).thenReturn(false);
		// Call the method
		assertDoesNotThrow(() -> {
			abstractMiscLineItems.deleteMiscLineItems(entityManager,
					db2EntityManager,
					null,
					shmMiscLineItemListToDelete);
		});

	}

	@Test
	public void testAddMiscLineItem() throws ValidationException {

		List<ShmMiscLineItem> shmMiscLineItemListToAdd = new ArrayList<>();
		ShmMiscLineItem shmMiscLineItem = new ShmMiscLineItem();
		shmMiscLineItemListToAdd.add(shmMiscLineItem);

		// Mock DAO methods
		doNothing().when(shmMiscLineItemSubDAO).persist(anyList(), any(EntityManager.class));
		when(appContext.getApplyDb2TwoPhaseCommit()).thenReturn(false);
		// Call the method
		assertDoesNotThrow(() -> {
			abstractMiscLineItems.addMiscLineItem(entityManager,
					db2EntityManager,
					null,
					shmMiscLineItemListToAdd,
					"TXN_CD",
					"user");
		});

	}

	@Test
	public void testResetSeqNumberShmMiscLineItemList() {
		String pro = "06420172510";
		List<ShmMiscLineItem> shmMiscLineItemsOriginal = new ArrayList();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		shmMiscLineItemsOriginal = (List<ShmMiscLineItem>) jsonStringToObject(shmMiscLineItemsOriginal.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmMiscLineItem"));
		shmMiscLineItemsOriginal = objectMapper.convertValue(shmMiscLineItemsOriginal,
				new TypeReference<List<ShmMiscLineItem>>() {
				});
		ShmShipment shmShipment = (ShmShipment) jsonStringToObject(ShmShipment.class,
				this.getJsonFromProperty(pro + ".jsonShmShipment"));
		shmShipment = objectMapper.convertValue(shmShipment, new TypeReference<ShmShipment>() {
		});

		List<ShmMiscLineItem> shmMiscLineItemsToDeleted = new ArrayList<>();
		shmMiscLineItemsToDeleted.add(shmMiscLineItemsOriginal.get(0));
		List<ShmMiscLineItem> shmMiscLineItemsToUpdate = new ArrayList<>();
		shmMiscLineItemsToUpdate.add(shmMiscLineItemsOriginal.get(0));
		shmMiscLineItemsToUpdate.forEach(shmMiscLineItem -> shmMiscLineItem.setDescTxt(
				shmMiscLineItem.getDescTxt() + " XX"));
		List<ShmMiscLineItem> shmMiscLineItemsToAdd = new ArrayList<>();
		boolean fromDelete = false;
		String userId = "user123";

		// Mock LoadValuesToUpdate implementation
		LoadValuesToUpdate<ShmMiscLineItem, ShmMiscLineItem> loadValuesToUpdate = mock(LoadValuesToUpdateManRateMiscImpl.class);
		when(loadValuesToUpdate.load(any(), any(), anyBoolean(), anyString(), any())).thenReturn(shmMiscLineItemsToUpdate);

		// Call the method
		ShmShipment finalShmShipment = shmShipment;
		List<ShmMiscLineItem> finalShmMiscLineItemsOriginal = shmMiscLineItemsOriginal;
		List<ShmMiscLineItem> result = assertDoesNotThrow(() -> {
			return abstractMiscLineItems.resetSeqNumberShmMiscLineItemList(finalShmShipment.getShpInstId(),
					shmMiscLineItemsToDeleted,
					finalShmMiscLineItemsOriginal,
					shmMiscLineItemsToUpdate,
					shmMiscLineItemsToAdd,
					fromDelete,
					userId,
					ShipmentUpdateActionCd.MANUAL_RATE);
		});

		// Assertions for the result
		assertNotNull(result);
		// ... add assertions for the result ...

	}

}