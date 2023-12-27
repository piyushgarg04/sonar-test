package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Stopwatch;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.DmtTrueDebtorPredict;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.v2.GetPredictedTrueDebtorResp;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.DmtPredictedTrueDebtorDAO;


@RequestScoped
public class GetPredictedTrueDebtorImpl {

	private static final Log logger = LogFactory.getLog(GetPredictedTrueDebtorImpl.class);

	@Inject
	private DmtPredictedTrueDebtorDAO dmtPredictedTrueDebtorDAO;

	@Inject
	private  AppContext appContext;


	public GetPredictedTrueDebtorResp getPredictedTrueDebtor(String shipperCustomerId, String consigneeZipCd, final TransactionContext txnContext,
			final EntityManager entityManager) throws ServiceException {

		final Stopwatch sw = Stopwatch.createStarted();
		checkNotNull(txnContext, "TransactionContext is required");
		checkNotNull(entityManager, "EntityManager is required");
		checkNotNull(shipperCustomerId, "shipperCustomerId is required");
		
		String confLevelPct = appContext.getConfLevelPctPredTrueDebtor();
		int  totalShipmentCount = appContext.getTotalShipmentCountPredTrueDebtor();
		checkNotNull(confLevelPct, "confLevelPct is required");
		BigDecimal confLevelPctBd =new BigDecimal(confLevelPct);
		BigDecimal totalShipmentCountBd =new BigDecimal(totalShipmentCount);

		
		final Stopwatch sw2 = Stopwatch.createStarted();

		DmtTrueDebtorPredict dmtTrueDebtorPredict = dmtPredictedTrueDebtorDAO.getPredictedDebror(Long.parseLong(shipperCustomerId), consigneeZipCd,
				entityManager,confLevelPctBd, totalShipmentCountBd);

		logger.info(
				String.format("getPredictedTrueDebtor DAO call to get predictedTrueDebtor for shipperCustomerId %s in %s ms",
						shipperCustomerId, sw2.elapsed(TimeUnit.MILLISECONDS)));
		sw2.stop();

		final GetPredictedTrueDebtorResp resp = new GetPredictedTrueDebtorResp();

		if (dmtTrueDebtorPredict != null) {
			resp.setPredictedTrueDebtor(EntityTransformer.toPredictedTrueDebtor(dmtTrueDebtorPredict));
		}

		sw.stop();
		logger.info(
				String.format("GetPredictedTrueDebtorImpl.getPredictedTrueDebtor in %s ms", sw.elapsed(TimeUnit.MILLISECONDS)));
		return resp;
	}

}
