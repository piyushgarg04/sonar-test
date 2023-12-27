package com.xpo.ltl.shipment.service.impl;

import java.util.Objects;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotNull;

import org.apache.logging.log4j.util.Strings;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmRtgInfoPass;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmRtgInfoPassPK;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.RatingInformationPass;
import com.xpo.ltl.api.shipment.v2.UpsertRatingInformationPassResp;
import com.xpo.ltl.api.shipment.v2.UpsertRatingInformationPassRqst;
import com.xpo.ltl.shipment.service.dao.ShmRtgInfoPassSubDAO;
import com.xpo.ltl.shipment.service.transformers.RatingInformationPassTransformer;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;

@RequestScoped
public class RatingInfoPassImpl {

	@PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
	private EntityManager db2EntityManager;

	@Inject
	private ShmRtgInfoPassSubDAO shmRtgInfoPassDAO;

	public UpsertRatingInformationPassResp upsertRatingInformationPass(
		@NotNull final UpsertRatingInformationPassRqst request,
		@NotNull final TransactionContext txnContext) throws ServiceException, ValidationException {
		Objects.requireNonNull(request, "Required parameter UpsertRatingInformationPassRqst request is null.");
		Objects.requireNonNull(txnContext, "Required parameter TransactionContext txnContext is null.");

		final RatingInformationPass pass = request.getRatingInformationPass();
		if (pass == null) {
			throw ExceptionBuilder
				.exception(ValidationErrorMessage.REQUEST_REQUIRED, txnContext)
				.moreInfo("location", "ratingInformationPass")
				.build();
		}

		if (Strings.isBlank(pass.getPassTypeCd()) || pass.getShipmentInstId() == null) {
			throw ExceptionBuilder
				.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
				.moreInfo("location", "passTypeCd, shipmentInstId")
				.moreInfo("error", "passTypeCd and shipmentInstId are required")
				.build();
		}

		final DB2ShmRtgInfoPassPK id = new DB2ShmRtgInfoPassPK();
		id.setPassTypCd(pass.getPassTypeCd());
		id.setShpInstId(pass.getShipmentInstId());
		DB2ShmRtgInfoPass entity = shmRtgInfoPassDAO.findById(id, db2EntityManager);

		final AuditInfo auditInfo = getAuditInfo(txnContext, pass);
		pass.setAuditInfo(auditInfo);

		if (entity == null) {
			entity = RatingInformationPassTransformer.toDb2Entity(pass);
			entity = shmRtgInfoPassDAO.createShmRtgInfoPass(entity, db2EntityManager);
		} else {
			entity = RatingInformationPassTransformer.updateDb2Entity(pass, entity);
		}

		final UpsertRatingInformationPassResp response = new UpsertRatingInformationPassResp();
		final RatingInformationPass dto = RatingInformationPassTransformer.toDto(entity);
		response.setRatingInformationPass(dto);
		return response;
	}

	private AuditInfo getAuditInfo(final TransactionContext txnContext, final RatingInformationPass pass) {
		final AuditInfo auditInfo;
		if (pass.getAuditInfo() != null) {
			auditInfo = AuditInfoHelper.getAuditInfoWithPgmId(pass.getAuditInfo().getCreateByPgmId(), txnContext);
		} else {
			auditInfo = AuditInfoHelper.getAuditInfo(txnContext);
		}
		return auditInfo;
	}
}
