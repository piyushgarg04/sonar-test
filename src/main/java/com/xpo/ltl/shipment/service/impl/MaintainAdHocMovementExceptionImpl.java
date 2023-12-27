package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcpPK;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementPK;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementExceptionTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.MovementException;
import com.xpo.ltl.api.shipment.v2.MovementExceptionTypeCd;
import com.xpo.ltl.api.shipment.v2.UpsertAdHocMovementExceptionResp;
import com.xpo.ltl.api.shipment.v2.UpsertAdHocMovementExceptionRqst;
import com.xpo.ltl.shipment.service.dao.ShipmentMovementExceptionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmMovementSubDAO;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;

@RequestScoped
public class MaintainAdHocMovementExceptionImpl {

	@Inject
	private ShipmentMovementExceptionSubDAO shipmentMovementExceptionDAO;

    @Inject
    private ShmMovementSubDAO shipmentMovementDAO;

	public UpsertAdHocMovementExceptionResp upsertAdHocMovementException(
		UpsertAdHocMovementExceptionRqst rqst,
		TransactionContext txnContext,
		EntityManager entityManager) throws ServiceException {
		
		checkNotNull(rqst, "The request is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
		checkNotNull(rqst.getMovementException(), "The shipment movement exception is required.");

		MovementException adHocMovementExcp = rqst.getMovementException();

		if(adHocMovementExcp.getTypeCd() != MovementExceptionTypeCd.AD_HOC) {
			throw ExceptionBuilder
			.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
			.log()
			.moreInfo("upsertAdHocMovementException", "Movement Exception Type code should be Ad Hoc.")
			.build();
		}
		
		if(adHocMovementExcp.getListActionCd() != ActionCd.ADD && adHocMovementExcp.getListActionCd() != ActionCd.UPDATE) {
			throw ExceptionBuilder
			.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
			.log()
			.moreInfo("upsertAdHocMovementException", "ActionCd should be Add or Update.")
			.build();
		}
		
		ShmMovementPK id = new ShmMovementPK();
		id.setShpInstId(adHocMovementExcp.getShipmentInstId());
		id.setSeqNbr(adHocMovementExcp.getMovementSequenceNbr().longValue());
		ShmMovement movementEntity = shipmentMovementDAO.findById(id, entityManager);
		
		if(movementEntity == null) {
			throw ExceptionBuilder.exception(NotFoundErrorMessage.MOVEMENT_NF, txnContext)
			.moreInfo("upsertAdHocMovementException",
				String.format("shipmentInstId: %s", adHocMovementExcp.getShipmentInstId()))
			.build();
		}
		
		ShmMovementExcp movementExcpEntity = null;
		final AuditInfo auditInfo = new AuditInfo();
		
		AuditInfoHelper.setCreatedInfo(auditInfo, txnContext);
		AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);
		if(adHocMovementExcp.getListActionCd() == ActionCd.ADD) {
			movementExcpEntity = new ShmMovementExcp();
			final ShmMovementExcpPK excpId = new ShmMovementExcpPK();
			excpId.setShpInstId(adHocMovementExcp.getShipmentInstId());
			excpId.setMvmtSeqNbr(adHocMovementExcp.getMovementSequenceNbr().longValue());
			excpId.setSeqNbr(shipmentMovementExceptionDAO.findMaxSeqNbr(excpId, entityManager) + 1);
			DtoTransformer.setAuditInfo(movementExcpEntity, auditInfo);
			movementExcpEntity.setId(excpId);
			movementExcpEntity.setShmMovement(movementEntity);
			movementExcpEntity = DtoTransformer.toShmMovementExcp(adHocMovementExcp, movementExcpEntity);
		}
		else {
			
			final ShmMovementExcpPK excpId = new ShmMovementExcpPK();
			excpId.setShpInstId(adHocMovementExcp.getShipmentInstId());
			excpId.setMvmtSeqNbr(adHocMovementExcp.getMovementSequenceNbr().longValue());
			excpId.setSeqNbr(adHocMovementExcp.getSequenceNbr().longValue());
			
			movementExcpEntity = shipmentMovementExceptionDAO.findById(excpId, entityManager);

			if (movementExcpEntity == null) {
				throw ExceptionBuilder.exception(NotFoundErrorMessage.MOVEMENT_EXCP_NF, txnContext)
				.moreInfo("upsertAdHocMovementException",
					String.format("shipmentInstId: %s", adHocMovementExcp.getShipmentInstId()))
				.build();
			}

			movementExcpEntity = DtoTransformer.toShmMovementExcp(adHocMovementExcp, movementExcpEntity);
		}
		
		ShmMovementExcp result = shipmentMovementExceptionDAO.save(movementExcpEntity, entityManager);
		
		UpsertAdHocMovementExceptionResp resp = new UpsertAdHocMovementExceptionResp();
		resp.setMovementException(EntityTransformer.toMovementException(result));
		return resp;	
	}

	public void deleteAdHocMovementException(
		Long shipmentInstId,
		Integer movementSequenceNbr,
		Integer sequenceNbr,
		TransactionContext txnContext,
		EntityManager entityManager) throws ServiceException {
		checkNotNull(shipmentInstId, "The shipment instance ID is required.");
		checkNotNull(movementSequenceNbr, "The movement sequence number is required.");
		checkNotNull(sequenceNbr, "The sequence number is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");

		final ShmMovementExcpPK id = new ShmMovementExcpPK();
		id.setShpInstId(shipmentInstId);
		id.setSeqNbr(sequenceNbr);
		id.setMvmtSeqNbr(movementSequenceNbr);

		final ShmMovementExcp movementExcpEntity = shipmentMovementExceptionDAO.findById(id, entityManager);

		if (movementExcpEntity == null) {
			throw ExceptionBuilder.exception(NotFoundErrorMessage.MOVEMENT_EXCP_NF, txnContext)
			.moreInfo("deleteAdHocMovementException",
				String.format("shipmentInstId: %s", shipmentInstId))
			.build();
		} 
		
		if(MovementExceptionTypeCdTransformer.toEnum(
			movementExcpEntity.getTypCd()) != MovementExceptionTypeCd.AD_HOC) {
			throw ExceptionBuilder
			.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
			.log()
			.moreInfo("deleteAdHocMovementException", "Movement Exception Type code should be Ad Hoc")
			.build();
		}
		
		shipmentMovementExceptionDAO.remove(movementExcpEntity, entityManager);
	}

}
