package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmOpsShipmentPilot;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.GetOperationsShipmentPilotResp;
import com.xpo.ltl.api.shipment.v2.OperationsShipmentPilot;
import com.xpo.ltl.api.shipment.v2.UpsertOperationsShipmentPilotResp;
import com.xpo.ltl.api.shipment.v2.UpsertOperationsShipmentPilotRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShipmentOpsShmPilotSubDAO;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;

@RequestScoped
public class OperationsShipmentImplPilot {
	
	private static final Log log = LogFactory.getLog(OperationsShipmentImplPilot.class);


	@Inject
	private ShipmentOpsShmPilotSubDAO shipmentOpsShmPilotSubDAO;
	
	public UpsertOperationsShipmentPilotResp upsertOperationShipmentPilot(
			UpsertOperationsShipmentPilotRqst rqst,
		TransactionContext txnContext,
		EntityManager entityManager) throws ServiceException {
		
		checkNotNull(rqst, "The request is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
		checkNotNull(rqst.getOperationsShipmentPilot(), "Operations shipment is required.");
		
		OperationsShipmentPilot operationsShipmentPilot = rqst.getOperationsShipmentPilot();
		ShmOpsShipmentPilot shmOpsShipmentPilotEntity = null;
		
		log.info(String.format("upsertOperationShipmentPilot in progress for shipmentId " + operationsShipmentPilot.getShipmentInstId()));		
		shmOpsShipmentPilotEntity = shipmentOpsShmPilotSubDAO .findById(operationsShipmentPilot.getShipmentInstId(), entityManager);	
		
		final AuditInfo auditInfo = new AuditInfo();
		if(shmOpsShipmentPilotEntity == null) {							
			AuditInfoHelper.setCreatedInfo(auditInfo, txnContext);
			AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);	
			shmOpsShipmentPilotEntity = new ShmOpsShipmentPilot();	
			DtoTransformer.setAuditInfo(shmOpsShipmentPilotEntity, auditInfo);
			if(txnContext.getCorrelationId() != null)
				shmOpsShipmentPilotEntity.setCorrelationId(txnContext.getCorrelationId());	
			shmOpsShipmentPilotEntity = DtoTransformer.toShmOpsShipmentPilot(operationsShipmentPilot, shmOpsShipmentPilotEntity);
		}		
		else {		
			
			if (txnContext.getTransactionTimestamp() != null) {// check for concurrency
			    if (shmOpsShipmentPilotEntity.getLstUpdtTmst().compareTo(BasicTransformer.toTimestamp(txnContext.getTransactionTimestamp())) >=0) {
			        throw ExceptionBuilder.exception(ServiceErrorMessage.UPDATE_VERSION_MISMATCH, txnContext)
			                .moreInfo("lstUpdtTmst", shmOpsShipmentPilotEntity.getLstUpdtTmst().toString())
			                .moreInfo("txnContext.transactionTimestamp", txnContext.getTransactionTimestamp().toString()).build();
			    }
			}			
			
			AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);	
			DtoTransformer.setLstUpdateAuditInfo(shmOpsShipmentPilotEntity, auditInfo);
			if(txnContext.getCorrelationId() != null)
				shmOpsShipmentPilotEntity.setCorrelationId(txnContext.getCorrelationId());	
			shmOpsShipmentPilotEntity =DtoTransformer.toShmOpsShipmentPilot(operationsShipmentPilot, shmOpsShipmentPilotEntity);	
		}
		
		final ShmOpsShipmentPilot respEntity = shipmentOpsShmPilotSubDAO.create(shmOpsShipmentPilotEntity, entityManager);
		final UpsertOperationsShipmentPilotResp resp = new UpsertOperationsShipmentPilotResp();
		resp.setOperationsShipmentPilot(EntityTransformer.toOperationsShipmentPilot(respEntity));

		log.info(String.format(" upsertOperationShipmentPilot completed for shipmentId " + operationsShipmentPilot.getShipmentInstId()));

		
		return resp;	
	}

	public GetOperationsShipmentPilotResp getOperationsShipmentPilot(Long shipmentInstId,TransactionContext txnContext,
			EntityManager entityManager) throws ServiceException{
		
		
		checkNotNull(shipmentInstId, "The shipmentInstId is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
			
		log.info(String.format("getOperationsShipmentPilot in progress for shipmentId " + shipmentInstId));
		
		ShmOpsShipmentPilot shmOpsShipmentPilotEntity = shipmentOpsShmPilotSubDAO .findById(shipmentInstId, entityManager);
		if (shmOpsShipmentPilotEntity == null) {
			throw ExceptionBuilder.exception(NotFoundErrorMessage.SHIPMENT_NOT_FOUND, txnContext).build();
		}
		GetOperationsShipmentPilotResp resp = new GetOperationsShipmentPilotResp();
		resp.setOperationsShipmentPilot(EntityTransformer.toOperationsShipmentPilot(shmOpsShipmentPilotEntity));
		
		log.info(String.format(" getOperationsShipmentPilot completed for shipmentId " + shipmentInstId));

		
		return resp;	
		
	}
	
	public void deleteOperationsShipmentPilot(Long shipmentInstId, TransactionContext txnContext,
			EntityManager entityManager) throws ServiceException {
		checkNotNull(shipmentInstId, "The shipmentInstId is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");

		log.info(String.format("deleteOperationsShipmentPilot in progress for shipmentInstId %d", shipmentInstId));

		ShmOpsShipmentPilot shmPilotToDelete = shipmentOpsShmPilotSubDAO.findById(shipmentInstId, entityManager);
		if (shmPilotToDelete == null) 
			throw ExceptionBuilder.exception(NotFoundErrorMessage.SHIPMENT_NOT_FOUND, txnContext).log().build();

		shipmentOpsShmPilotSubDAO.remove(shmPilotToDelete, entityManager);

		log.info(String.format("deleteOperationsShipmentPilot completed for shipmentInstId %d", shipmentInstId));
	}
}
