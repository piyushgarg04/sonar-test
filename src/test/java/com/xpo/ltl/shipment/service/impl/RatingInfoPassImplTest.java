package com.xpo.ltl.shipment.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.util.Calendar;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmRtgInfoPass;
import com.xpo.ltl.api.shipment.v2.RatingInformationPass;
import com.xpo.ltl.api.shipment.v2.UpsertRatingInformationPassRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmRtgInfoPassSubDAO;

public class RatingInfoPassImplTest {

	@InjectMocks
	private RatingInfoPassImpl bean;

	@Mock
	private ShmRtgInfoPassSubDAO ratingInfoPassDao;

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

	@Test(expected = ValidationException.class)
	public void UpsertRatingInformationPassEmptyRequest() throws Exception {
		final UpsertRatingInformationPassRqst request = new UpsertRatingInformationPassRqst();

		bean.upsertRatingInformationPass(request, txnContext).getRatingInformationPass();
	}

	@Test(expected = ValidationException.class)
	public void UpsertRatingInformationPassNoId() throws Exception {
		final UpsertRatingInformationPassRqst request = new UpsertRatingInformationPassRqst();
		request.setRatingInformationPass(new RatingInformationPass());

		bean.upsertRatingInformationPass(request, txnContext).getRatingInformationPass();
	}

	@Test
	public void UpsertRatingInformationPassCreate() throws Exception {
		final UpsertRatingInformationPassRqst request = new UpsertRatingInformationPassRqst();
		final RatingInformationPass pass = getRatingInformationPass(request);

		final DB2ShmRtgInfoPass res = new DB2ShmRtgInfoPass();
		res.setPrcAgrmtId(BigDecimal.valueOf(pass.getPriceAgreementId()));
		Mockito.when(ratingInfoPassDao.createShmRtgInfoPass(Mockito.any(), Mockito.any())).thenReturn(res);

		final RatingInformationPass resp = bean
			.upsertRatingInformationPass(request, txnContext)
			.getRatingInformationPass();

		assertThat(resp.getPriceAgreementId(), CoreMatchers.equalTo(pass.getPriceAgreementId()));
	}

	@Test
	public void UpsertRatingInformationPassUpdate() throws Exception {
		final UpsertRatingInformationPassRqst request = new UpsertRatingInformationPassRqst();
		final RatingInformationPass pass = getRatingInformationPass(request);
		pass.setRulesTariffName(null);

		final DB2ShmRtgInfoPass existing = new DB2ShmRtgInfoPass();
		existing.setPrcAgrmtId(BigDecimal.valueOf(pass.getPriceAgreementId()));
		existing.setFscTrfNmTxt("txt");
		existing.setRulesTrfNmTxt("rules");
		Mockito.when(ratingInfoPassDao.findById(Mockito.any(), Mockito.any())).thenReturn(existing);

		final RatingInformationPass resp = bean
			.upsertRatingInformationPass(request, txnContext)
			.getRatingInformationPass();

		assertThat(resp.getPriceAgreementId(), CoreMatchers.equalTo(pass.getPriceAgreementId()));
		assertThat(resp.getFscTariffName(), CoreMatchers.equalTo(pass.getFscTariffName()));
		assertThat(resp.getRulesTariffName(), CoreMatchers.equalTo(existing.getRulesTrfNmTxt()));
	}

	private RatingInformationPass getRatingInformationPass(final UpsertRatingInformationPassRqst request) {
		final RatingInformationPass pass = new RatingInformationPass();
        pass.setAbsoluteMinChargeInd(true);
		pass.setPriceAgreementId(1L);
		pass.setShipmentInstId(12L);
		pass.setPassTypeCd("T");
		pass.setFscTariffName("TRF");
		pass.setOffshoreTotalAccessorialAmount(5.0);
		pass.setOffshoreTotalLinehaulChargeAmount(1.0);
		pass.setOffshoreTotalFscAmount(1.0);
        pass.setPriceRulesetNbr(BasicTransformer.toBigInteger(5));
		pass.setRatingTariffName("RATTRF");
        pass.setRatingTariffVersion(1L);
		pass.setRulesTariffName("RLTRF");
        pass.setRulesTariffVersion(1L);
		pass.setTotalAccessorialAmount(1.0);
		pass.setTotalChargeAmount(1.5);
		pass.setTotalDiscountAmount(5.0);
		pass.setTotalFscAmount(1.0);
		pass.setTotalLinehaulChargeAmount(1.0);
		pass.setTotalTaxAmount(2.0);
		request.setRatingInformationPass(pass);
		return pass;
	}
}
