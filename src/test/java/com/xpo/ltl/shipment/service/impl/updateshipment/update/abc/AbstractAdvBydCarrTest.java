package com.xpo.ltl.shipment.service.impl.updateshipment.update.abc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmAdvBydCarr;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.ShipmentAdvBydSubDAO;
import com.xpo.ltl.shipment.service.impl.updateshipment.MockParent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class AbstractAdvBydCarrTest extends MockParent {

	@Mock
	private EntityManager entityManager;

	@Mock
	private EntityManager db2EntityManager;

	@Mock
	private TransactionContext transactionContext;
	@Mock
	private ShipmentAdvBydSubDAO shipmentAdvBydSubDAO;

	@Mock
	private AppContext appContext;
	@InjectMocks
	private AbstractAdvBydCarr abstractAdvBydCarr = Mockito.spy(new AbstractAdvBydCarr() {
	});

	AbstractAdvBydCarrTest() throws InstantiationException, IllegalAccessException {
	}

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void updateShmAdvBydCarr() throws ValidationException, NotFoundException {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		String transactionCd = "testTransaction";
		String pro = "06420375031";

		List<ShmAdvBydCarr> shmAdvBydCarrs = new ArrayList<>();
		shmAdvBydCarrs = (List<ShmAdvBydCarr>) jsonStringToObject(shmAdvBydCarrs.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmAdvBydCarr"));
		shmAdvBydCarrs = objectMapper.convertValue(shmAdvBydCarrs, new TypeReference<List<ShmAdvBydCarr>>() {
		});
		when(appContext.getApplyDb2TwoPhaseCommit()).thenReturn(false);
		doNothing().when(shipmentAdvBydSubDAO).persist(anyList(), eq(entityManager));
		doNothing()
				.when(shipmentAdvBydSubDAO)
				.updateDB2ShmAdvBydCarr(any(), any(), eq(db2EntityManager), eq(transactionContext));

		abstractAdvBydCarr.updateShmAdvBydCarr(entityManager,
				db2EntityManager,
				transactionContext,
				shmAdvBydCarrs,
				transactionCd);

	}

	@Test
	public void testInsertAdvBydCarr() throws ValidationException {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		String transactionCd = "testTransaction";
		String pro = "06420375031";

		List<ShmAdvBydCarr> shmAdvBydCarrs = new ArrayList<>();
		shmAdvBydCarrs = (List<ShmAdvBydCarr>) jsonStringToObject(shmAdvBydCarrs.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmAdvBydCarr"));
		shmAdvBydCarrs = objectMapper.convertValue(shmAdvBydCarrs, new TypeReference<List<ShmAdvBydCarr>>() {
		});

		doNothing().when(shipmentAdvBydSubDAO).persist(anyList(), eq(entityManager));

		abstractAdvBydCarr.insertAdvBydCarr(entityManager,
				db2EntityManager,
				transactionContext,
				shmAdvBydCarrs,
				transactionCd);

	}

	@Test
	void resetSeqNumberAdvBydCarr() {
		Long shipmentInstId = 1L;
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		String transactionCd = "testTransaction";
		String pro = "06420375031";

		List<ShmAdvBydCarr> shmAdvBydCarrs = new ArrayList<>();
		shmAdvBydCarrs = (List<ShmAdvBydCarr>) jsonStringToObject(shmAdvBydCarrs.getClass(),
				this.getJsonFromProperty(pro + ".jsonShmAdvBydCarr"));
		shmAdvBydCarrs = objectMapper.convertValue(shmAdvBydCarrs, new TypeReference<List<ShmAdvBydCarr>>() {
		});

		List<ShmAdvBydCarr> result = AbstractAdvBydCarr.resetSeqNumberAdvBydCarr(shipmentInstId, shmAdvBydCarrs);

		assertNotNull(result);
	}

}