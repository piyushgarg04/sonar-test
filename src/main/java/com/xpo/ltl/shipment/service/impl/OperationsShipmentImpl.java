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
import com.xpo.ltl.api.shipment.service.entity.ShmOpsShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.OperationsShipment;
import com.xpo.ltl.api.shipment.v2.UpsertOperationsShipmentResp;
import com.xpo.ltl.api.shipment.v2.UpsertOperationsShipmentRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShipmentOpsShmSubDAO;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;

@RequestScoped
public class OperationsShipmentImpl {
	
	private static final Log log = LogFactory.getLog(OperationsShipmentImpl.class);


	@Inject
	private ShipmentOpsShmSubDAO shipmentOpsShmSubDAO;
	
	public UpsertOperationsShipmentResp upsertOperationShipment(
			UpsertOperationsShipmentRqst rqst,
		TransactionContext txnContext,
		EntityManager entityManager) throws ServiceException {
		
		checkNotNull(rqst, "The request is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
		checkNotNull(rqst.getOperationsShipment(), "Operations shipment is required.");
		
		OperationsShipment operationsShipment = rqst.getOperationsShipment();
		ShmOpsShipment shmOpsShipmentEntity = null;
		
		log.info(String.format("upsertOperationShipment in progress for shipmentId " + operationsShipment.getShipmentInstId()));

		
		shmOpsShipmentEntity = shipmentOpsShmSubDAO .findById(operationsShipment.getShipmentInstId(), entityManager);
		
		final AuditInfo auditInfo = new AuditInfo();
		if(shmOpsShipmentEntity == null) {							
			AuditInfoHelper.setCreatedInfo(auditInfo, txnContext);
			AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);	
			shmOpsShipmentEntity = new ShmOpsShipment();
			DtoTransformer.setAuditInfo(shmOpsShipmentEntity, auditInfo);
			if(txnContext.getCorrelationId() != null)
				shmOpsShipmentEntity.setCorrelationId(txnContext.getCorrelationId());	
			shmOpsShipmentEntity = DtoTransformer.toShmOpsShipment(operationsShipment, shmOpsShipmentEntity);		
		}		
		else {		
			
			if (txnContext.getTransactionTimestamp() != null) {// check for concurrency
			    if (shmOpsShipmentEntity.getLstUpdtTmst().compareTo(BasicTransformer.toTimestamp(txnContext.getTransactionTimestamp())) >=0) {
			        throw ExceptionBuilder.exception(ServiceErrorMessage.UPDATE_VERSION_MISMATCH, txnContext)
			                .moreInfo("lstUpdtTmst", shmOpsShipmentEntity.getLstUpdtTmst().toString())
			                .moreInfo("txnContext.transactionTimestamp", txnContext.getTransactionTimestamp().toString()).build();
			    }
			}			
			AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);	
			DtoTransformer.setLstUpdateAuditInfo(shmOpsShipmentEntity, auditInfo);
			if(txnContext.getCorrelationId() != null)
				shmOpsShipmentEntity.setCorrelationId(txnContext.getCorrelationId());
			shmOpsShipmentEntity =DtoTransformer.toShmOpsShipment(operationsShipment, shmOpsShipmentEntity);		
		}
		
		final ShmOpsShipment respEntity = shipmentOpsShmSubDAO.create(shmOpsShipmentEntity, entityManager);

		final UpsertOperationsShipmentResp resp = new UpsertOperationsShipmentResp();
		resp.setOperationsShipment(EntityTransformer.toOperationsShipment(respEntity));

		log.info(String.format(" upsertOperationShipment completed for shipmentId " + operationsShipment.getShipmentInstId()));

		
		return resp;	
	}

	public void deleteOperationsShipment(Long shipmentInstId, TransactionContext txnContext,
			EntityManager entityManager) throws ServiceException {
		checkNotNull(shipmentInstId, "The shipmentInstId is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");

		log.info(String.format("deleteOperationsShipment in progress for shipmentInstId %d", shipmentInstId));

		ShmOpsShipment shipmentToDelete = shipmentOpsShmSubDAO.findById(shipmentInstId, entityManager);
		if (shipmentToDelete == null) 
			throw ExceptionBuilder.exception(NotFoundErrorMessage.SHIPMENT_NOT_FOUND, txnContext).log().build();

		shipmentOpsShmSubDAO.remove(shipmentToDelete, entityManager);

		log.info(String.format("deleteOperationsShipment completed for shipmentInstId %d", shipmentInstId));
	}
}
