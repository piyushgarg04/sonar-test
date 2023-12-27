package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpo.ltl.api.exception.ExceptionBuilder;
import com.xpo.ltl.api.exception.ServiceErrorMessage;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcpPK;
import com.xpo.ltl.api.shipment.service.entity.ShmMvmtExcpAction;
import com.xpo.ltl.api.shipment.service.entity.ShmMvmtExcpActionPK;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.MovementExceptionAction;
import com.xpo.ltl.api.shipment.v2.UpsertMovementExceptionActionResp;
import com.xpo.ltl.api.shipment.v2.UpsertMovementExceptionActionRqst;
import com.xpo.ltl.shipment.service.dao.ShipmentMovementExceptionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmMvmtExcpActionSubDAO;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;

@RequestScoped
public class MaintainShipmentMovementExceptionActionImpl {
	private static final Log log = LogFactory.getLog(MaintainShipmentMovementExceptionActionImpl.class);

	@Inject
	private ShmMvmtExcpActionSubDAO shmMvmtExcpActionDAO;

	@Inject
	private ShipmentMovementExceptionSubDAO shipmentMovementExceptionDAO;

	public UpsertMovementExceptionActionResp upsertMovementExceptionAction(
		final UpsertMovementExceptionActionRqst rqst,
		final TransactionContext txnContext,
		final EntityManager entityManager) throws ServiceException {

		checkNotNull(rqst, "The request is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
		checkNotNull(rqst.getMovementExceptionAction(), "The shipment movement exception action is required.");

		final MovementExceptionAction movementExceptionAction = rqst.getMovementExceptionAction();

		log.info(
			String.format(
				"Upserting shipment movement exception action for shipmentInstId %s",
				movementExceptionAction.getShipmentInstId()));

		if (movementExceptionAction.getListActionCd() != ActionCd.ADD
				&& movementExceptionAction.getListActionCd() != ActionCd.UPDATE)
			throw ExceptionBuilder
				.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
				.log()
				.moreInfo("upsertPreferenceGrouping", "ActionCd should be Add or Update.")
				.build();

		final AuditInfo auditInfo = new AuditInfo();

		ShmMvmtExcpAction actionEntity = null;

		if (movementExceptionAction.getListActionCd() == ActionCd.ADD) {
			AuditInfoHelper.setCreatedInfo(auditInfo, txnContext);
			AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);

			final ShmMovementExcpPK id = new ShmMovementExcpPK();
			id.setShpInstId(movementExceptionAction.getShipmentInstId());
			id.setSeqNbr(movementExceptionAction.getMovementExceptionSequenceNbr().longValue());
			id.setMvmtSeqNbr(movementExceptionAction.getMovementSequenceNbr().longValue());

			final ShmMovementExcp mvmtExcpEntity = shipmentMovementExceptionDAO.findById(id, entityManager);

			if (mvmtExcpEntity == null) {
				throw ExceptionBuilder
					.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
					.log()
					.moreInfo("upsertMovementExceptionAction", "mvmtExcpEntity not found.")
					.build();

			}

			actionEntity = new ShmMvmtExcpAction();
			final ShmMvmtExcpActionPK actionId = new ShmMvmtExcpActionPK();
			actionId.setShpInstId(movementExceptionAction.getShipmentInstId());
			actionId.setMvmtSeqNbr(movementExceptionAction.getMovementSequenceNbr().longValue());
			actionId.setMvmtExcpSeqNbr(movementExceptionAction.getMovementExceptionSequenceNbr().longValue());
			actionId.setSeqNbr(shmMvmtExcpActionDAO.findMaxSeqNbr(actionId, entityManager) + 1);
			actionEntity.setId(actionId);
			DtoTransformer.setAuditInfo(actionEntity, auditInfo);
			actionEntity.setShmMovementExcp(mvmtExcpEntity);
			actionEntity = DtoTransformer.toShmMvmtExcpAction(movementExceptionAction, actionEntity);
		} else {
			AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);

			final ShmMvmtExcpActionPK id = new ShmMvmtExcpActionPK();
			id.setShpInstId(movementExceptionAction.getShipmentInstId());
			id.setSeqNbr(movementExceptionAction.getSequenceNbr().longValue());
			id.setMvmtSeqNbr(movementExceptionAction.getMovementSequenceNbr().longValue());
			id.setMvmtExcpSeqNbr(movementExceptionAction.getMovementExceptionSequenceNbr().longValue());

			actionEntity = shmMvmtExcpActionDAO.findById(id, entityManager);

			if (actionEntity == null) {
				throw ExceptionBuilder
					.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
					.log()
					.moreInfo("upsertMovementExceptionAction", "actionEntity not found.")
					.build();

			}

			DtoTransformer.setLstUpdateAuditInfo(actionEntity, auditInfo);
			actionEntity = DtoTransformer.toShmMvmtExcpAction(movementExceptionAction, actionEntity);
		}

		actionEntity.setRecordVersionNbr(BigDecimal.ZERO);
		final ShmMvmtExcpAction respEntity = shmMvmtExcpActionDAO.save(actionEntity, entityManager);

		final UpsertMovementExceptionActionResp resp = new UpsertMovementExceptionActionResp();
		resp.setMovementExceptionAction(EntityTransformer.toMovementExceptionAction(respEntity));

		return resp;
	}

	public void deleteMovementExceptionAction(
		final Long shipmentInstId,
		final Integer sequenceNbr,
		final Integer movementSequenceNbr,
		final Integer movementExceptionSequenceNbr,
		final TransactionContext txnContext,
		final EntityManager entityManager) throws ServiceException {

		checkNotNull(shipmentInstId, "The shipment instance ID is required.");
		checkNotNull(movementSequenceNbr, "The movement sequence number is required.");
		checkNotNull(movementExceptionSequenceNbr, "The movement exception sequence number is required.");
		checkNotNull(sequenceNbr, "The sequence number is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");

		final ShmMvmtExcpActionPK id = new ShmMvmtExcpActionPK();
		id.setShpInstId(shipmentInstId);
		id.setSeqNbr(sequenceNbr);
		id.setMvmtSeqNbr(movementSequenceNbr);
		id.setMvmtExcpSeqNbr(movementExceptionSequenceNbr);

		final ShmMvmtExcpAction exceptionActionEntity = shmMvmtExcpActionDAO.findById(id, entityManager);

		if (exceptionActionEntity == null) {
			throw ExceptionBuilder
				.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
				.log()
				.moreInfo("deleteMovementExceptionAction", "ExceptionAction is not found")
				.build();
		}

		shmMvmtExcpActionDAO.remove(exceptionActionEntity, entityManager);
	}

}
