package com.xpo.ltl.shipment.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Calendar;

import javax.persistence.EntityManager;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.service.entity.ShmRtgInfo;
import com.xpo.ltl.api.shipment.v2.RatingInformation;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmRtgInfoSubDAO;

public class GetRatingInformationImplTest {

	@InjectMocks
	private GetRatingInformationImpl bean;

	@Mock
	private ShmRtgInfoSubDAO ratingInfoDao;

	@Mock
	private EntityManager em;

	private TransactionContext txnContext;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);

		txnContext = new TransactionContext();
		txnContext.setTransactionTimestamp(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance()));
		final User user = new User();
		user.setUserId("user");
		txnContext.setUser(user);
	}

	@Test(expected = NotFoundException.class)
	public void getRatingInformationNotFound() throws Exception {
		bean.getRatingInformation(50L, txnContext, em).getRatingInformation();
	}

	@Test
	public void getRatingInformationFound() throws Exception {
		final long id = 50L;
		final ShmRtgInfo entity = new ShmRtgInfo();
		entity.setShpInstId(id);

		Mockito.when(ratingInfoDao.findById(id, em)).thenReturn(entity);

		final RatingInformation resp = bean.getRatingInformation(id, txnContext, em).getRatingInformation();

		assertThat(resp.getShipmentInstId(), Matchers.equalTo(id));
	}

}
