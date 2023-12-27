package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.lang.exception.ExceptionUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.v2.CreateGuaranteedFailureRerateResp;
import com.xpo.ltl.api.shipment.v2.CreateGuaranteedFailureRerateRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmGuaranteedFailRerateSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmGuaranteedFailRerate;
import com.xpo.ltl.api.shipment.service.entity.ShmGuaranteedFailReratePK;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;

public class CreateGuaranteedFailureRerateImpl {

	@Inject
	ShmGuaranteedFailRerateSubDAO shmGuaranteedFailRerateSubDAO;

	@Inject
	ShmShipmentSubDAO shmShipmentSubDAO;

	public CreateGuaranteedFailureRerateResp createGuaranteedFailureRerate(
			CreateGuaranteedFailureRerateRqst createGuaranteedFailureRerateRqst, TransactionContext txnContext,
			final EntityManager entityManager) throws ServiceException {
		checkNotNull(txnContext, "TransactionContext is required");
		checkNotNull(entityManager, "EntityManager is required");
		checkNotNull(createGuaranteedFailureRerateRqst, "The request is required.");
		long shpInstId = 0L;
		try {
			shpInstId = shmShipmentSubDAO.getIdByIdOrProNumber(null,
					createGuaranteedFailureRerateRqst.getShipmentInstId(), null, entityManager);
		} catch (Exception e) {
			throw ExceptionBuilder.exception(NotFoundErrorMessage.SHIPMENT_NOT_FOUND, txnContext)
					.moreInfo("ShipmentGuaranteedServiceBean.createGuaranteedFailureRerate",
							"An unexpected error occurred: " + ExceptionUtils.getRootCauseMessage(e))
					.build();
		}

		if (Objects.isNull(shpInstId)) {
			throw ExceptionBuilder.exception(NotFoundErrorMessage.SHIPMENT_NOT_FOUND, txnContext).build();
		}

		ShmGuaranteedFailRerate shmGuaranteedFailRerate = new ShmGuaranteedFailRerate();

		ShmGuaranteedFailReratePK shmGuaranteedFailReratePK = new ShmGuaranteedFailReratePK();
		shmGuaranteedFailReratePK.setShpInstId(createGuaranteedFailureRerateRqst.getShipmentInstId());
		if (createGuaranteedFailureRerateRqst.getGuaranteeFailureProcessedDate() != null) {
			shmGuaranteedFailReratePK.setGarntFailureProcessedDt(
					BasicTransformer.toDate(createGuaranteedFailureRerateRqst.getGuaranteeFailureProcessedDate()));
		}
		shmGuaranteedFailRerate.setId(shmGuaranteedFailReratePK);

		shmGuaranteedFailRerate.setRatingTypeCd(createGuaranteedFailureRerateRqst.getRatingTypeCd());
		shmGuaranteedFailRerate.setCorrelationId(AuditInfoHelper.getTransactionCorrelationId(txnContext));

		DtoTransformer.setAuditInfo(shmGuaranteedFailRerate, AuditInfoHelper.getAuditInfo(txnContext));
		shmGuaranteedFailRerateSubDAO.createShmGuaranteedFailRerate(shmGuaranteedFailRerate, entityManager);

		CreateGuaranteedFailureRerateResp createGuaranteedFailureRerateResp = new CreateGuaranteedFailureRerateResp();
		createGuaranteedFailureRerateResp
				.setGuaranteedFailureRerate(EntityTransformer.toGuaranteedFailureRerate(shmGuaranteedFailRerate));
		return createGuaranteedFailureRerateResp;
	}
}
