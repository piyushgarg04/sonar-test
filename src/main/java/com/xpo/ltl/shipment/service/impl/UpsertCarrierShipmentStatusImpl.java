package com.xpo.ltl.shipment.service.impl;

import java.math.BigDecimal;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmExternalStatus;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.UpsertCarrierShipmentStatusRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmExternalStatusSubDAO;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import com.xpo.ltl.shipment.service.validators.UpsertCarrierShipmentStatusValidator;


@ApplicationScoped
@LogExecutionTime
public class UpsertCarrierShipmentStatusImpl {
	
	@Inject
	private ShmExternalStatusSubDAO shmExternalStatusSubDAO;
	
	@Inject
	private UpsertCarrierShipmentStatusValidator upsertExternalStatusValidator;
	
	@PersistenceContext(unitName = "ltl-java-shipment-jaxrs")
	private EntityManager entityManager;	
	
	private static final String UNSUPPORTED = "UNSUPPORTED";
	private static final String EXTSHMUPDTE = "EXTSHMUPDTE";
	private static final String SHM_EXTERNAL_STATUS_SEQ = "SHM_EXTERNAL_STATUS_SEQ";
	
	public void upsertCarrierShipmentStatus(UpsertCarrierShipmentStatusRqst upsertCarrierShipmentStatusRqst,
			Long carrierId, TransactionContext txnContext)
			throws ServiceException, ValidationException {
		
		//validate input parameters
		upsertExternalStatusValidator.validateInputParameters(
				carrierId, upsertCarrierShipmentStatusRqst, txnContext, entityManager);
		
		String elevenDigitProNbr = null;
		if(StringUtils.isNotEmpty(upsertCarrierShipmentStatusRqst.getProNbr()))
			elevenDigitProNbr = ProNumberHelper.validateProNumber
				(upsertCarrierShipmentStatusRqst.getProNbr(), txnContext);

		// verify valid Carrier
		upsertExternalStatusValidator.validateCarrier(carrierId, txnContext);

		//verify valid shipmentInstId and proNbr
		ShmShipment shmShipmnt = upsertExternalStatusValidator.validateShipmentIdOrProNbr(elevenDigitProNbr, 
				upsertCarrierShipmentStatusRqst.getShipmentInstId(), txnContext, entityManager);
		
		//add data to SHM_EXTERNAL_STATUS
		ShmExternalStatus shmExternalStatus = new ShmExternalStatus();
		AuditInfo auditInfo = AuditInfoHelper.getAuditInfo(txnContext);
		
		shmExternalStatusSubDAO.setStatusId(shmExternalStatus, SHM_EXTERNAL_STATUS_SEQ, entityManager);
		shmExternalStatus.setCarrierId(new BigDecimal(carrierId));
		shmExternalStatus.setCarrierShipmentId(ObjectUtils.defaultIfNull(
				upsertCarrierShipmentStatusRqst.getCarrierShipmentId(), null));
		shmExternalStatus.setExceptionPiecesCnt(BasicTransformer.toBigDecimal(
				ObjectUtils.defaultIfNull(upsertCarrierShipmentStatusRqst.getExceptionPiecesCount(), null)));
		shmExternalStatus.setExceptionRemark(ObjectUtils.defaultIfNull(
				upsertCarrierShipmentStatusRqst.getExceptionRemark(), null));
		shmExternalStatus.setOccurredLatitudeNbr(BasicTransformer.toBigDecimal(
				ObjectUtils.defaultIfNull(upsertCarrierShipmentStatusRqst.getOccurredLatitudeNbr(), null)));
		shmExternalStatus.setOccurredLongitudeNbr(BasicTransformer.toBigDecimal(
				ObjectUtils.defaultIfNull(upsertCarrierShipmentStatusRqst.getOccurredLongitudeNbr(), null)));
		shmExternalStatus.setOccurredTmst(BasicTransformer.toTimestamp(
				ObjectUtils.defaultIfNull(upsertCarrierShipmentStatusRqst.getOccurredDateTime(), null)));
		shmExternalStatus.setProcessStatusCd(UNSUPPORTED);
		shmExternalStatus.setShpInstId(BasicTransformer.toBigDecimal(shmShipmnt.getShpInstId()));
		shmExternalStatus.setStatusCd(upsertCarrierShipmentStatusRqst.getStatusCd());
		
		shmExternalStatus.setCorrelationId(auditInfo.getCorrelationId());
		shmExternalStatus.setCrteBy(auditInfo.getCreatedById());
		shmExternalStatus.setCrtePgmId(EXTSHMUPDTE);
		shmExternalStatus.setCrteTmst(BasicTransformer.toTimestamp(auditInfo.getCreatedTimestamp()));
		shmExternalStatus.setLstUpdtBy(auditInfo.getUpdateById());
		shmExternalStatus.setLstUpdtPgmId(EXTSHMUPDTE);
		shmExternalStatus.setLstUpdtTmst(BasicTransformer.toTimestamp(auditInfo.getUpdatedTimestamp()));
		
		shmExternalStatusSubDAO.create(shmExternalStatus, entityManager);
		
	}
	
}
