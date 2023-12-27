package com.xpo.ltl.shipment.service.impl;

import static org.mockito.Mockito.when;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmSvcOvrd;
import com.xpo.ltl.shipment.service.dao.ShmSvcOvrdSubDAO;

@RunWith(MockitoJUnitRunner.class)
public class GetServiceOverrideImplTest {

	private static final Long SHP_INST_ID = 1000000L;

	@Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

	@Mock
	private ShmSvcOvrdSubDAO shmSvcOvrdSubDAO;

	@Mock
	private GetServiceOverrideImpl getServiceOverrideImpl;

	@Test
	public void shouldReturnSvcOvrdData() throws ServiceException {
		ShmSvcOvrd shmSvcOvrd = buildSvcOvrdMockData();
		when(shmSvcOvrdSubDAO.findById(SHP_INST_ID, entityManager)).thenReturn(shmSvcOvrd);
	}

	private ShmSvcOvrd buildSvcOvrdMockData() {
		ShmSvcOvrd shmSvcOvrd = new ShmSvcOvrd();
		shmSvcOvrd.setShpInstId(SHP_INST_ID);
		shmSvcOvrd.setRsnCd("RSC");
		shmSvcOvrd.setRmrkTxt("Test");
		return shmSvcOvrd;
	}

}
