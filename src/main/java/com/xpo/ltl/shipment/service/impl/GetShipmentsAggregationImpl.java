package com.xpo.ltl.shipment.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.v2.GetShipmentsAggregationResp;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO.ShipmentsAggregation;

@RequestScoped
public class GetShipmentsAggregationImpl {

	@Inject
	private ShmShipmentSubDAO shipmentDAO;

	public GetShipmentsAggregationResp getShipmentsAggregation(
		final Date beginDate,
		final Date endDate,
		final List<BigDecimal> pricingAgreementIds,
		final TransactionContext txnContext,
		final EntityManager entityManager) {

		final ShipmentsAggregation shipmentAggregation = shipmentDAO
			.getShipmentAggregation(beginDate, endDate, pricingAgreementIds, txnContext, entityManager);

		final GetShipmentsAggregationResp response = new GetShipmentsAggregationResp();
		response.setCount(shipmentAggregation.getCount());
		response.setTotalRevenueAmount(shipmentAggregation.getTotalRevenueAmount());
		response.setTotalWeightLbs(shipmentAggregation.getTotalWeightLbs());

		return response;
	}

}
