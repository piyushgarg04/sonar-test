package com.xpo.ltl.shipment.service.impl;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmRtgInfo;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.v2.GetRatingInformationResp;
import com.xpo.ltl.api.shipment.v2.RatingInformation;
import com.xpo.ltl.shipment.service.dao.ShmRtgInfoSubDAO;

@RequestScoped
public class GetRatingInformationImpl {

	@Inject
	private ShmRtgInfoSubDAO shmRtgInfoDAO;

	public GetRatingInformationResp getRatingInformation(
		@NotNull final Long shipmentInstId,
		@NotNull final TransactionContext txnContext,
		@NotNull final EntityManager entityManager) throws ServiceException {

		final ShmRtgInfo entity = shmRtgInfoDAO.findById(shipmentInstId, entityManager);
		if (entity == null) {
			throw ExceptionBuilder.exception(NotFoundErrorMessage.RTG_INFO_NOT_FOUND, txnContext).build();
		}
		final RatingInformation dto = EntityTransformer.toRatingInformation(entity);

		final GetRatingInformationResp resp = new GetRatingInformationResp();
		resp.setRatingInformation(dto);
		return resp;
	}
}
