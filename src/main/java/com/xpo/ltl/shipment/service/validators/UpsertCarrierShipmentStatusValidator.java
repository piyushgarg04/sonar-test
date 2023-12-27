package com.xpo.ltl.shipment.service.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.carriermanagement.v1.GetCarrierDetailsResp;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.UpsertCarrierShipmentStatusRqst;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;

public class UpsertCarrierShipmentStatusValidator extends Validator {
	
	@Inject
	private ExternalRestClient externalClient;
	
	@Inject
	private ShmShipmentSubDAO shmShipmentSubDAO;
	
	public void validateInputParameters(Long carrierId, UpsertCarrierShipmentStatusRqst upsertCarrierShipmentStatusRqst,
			 TransactionContext txnContext, EntityManager entityManager) throws ValidationException {
		
		List<MoreInfo> moreInfo = new ArrayList<>();
		
		checkTransactionContext(txnContext);
		checkEntityManager(entityManager);
		
		if(carrierId == null || carrierId == 0L)
			moreInfo.add(createMoreInfo("carrierId", ValidationErrorMessage.VALIDATION_ERRORS_FOUND.message()));
		
		if(StringUtils.isEmpty(upsertCarrierShipmentStatusRqst.getStatusCd()))
			moreInfo.add(createMoreInfo("StatusCd", ValidationErrorMessage.VALIDATION_ERRORS_FOUND.message()));
		
		if(StringUtils.isEmpty(upsertCarrierShipmentStatusRqst.getProNbr()) && 
				(upsertCarrierShipmentStatusRqst.getShipmentInstId() == null || 
				upsertCarrierShipmentStatusRqst.getShipmentInstId() == 0L))
			moreInfo.add(createMoreInfo("proNbr, shipmentInstId", ValidationErrorMessage.VALIDATION_ERRORS_FOUND.message()));
		
		checkMoreInfo(txnContext, moreInfo);
	}
	
	public void validateCarrier(Long carrierId, TransactionContext txnContext) throws ServiceException {
		
		GetCarrierDetailsResp carrierDetailResp = externalClient.getCarrierDetails(carrierId, null, txnContext);
		if(Objects.isNull(carrierDetailResp) || Objects.isNull(carrierDetailResp.getCarrierMaster()))
			throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
			.moreInfo("UpsertExternalStatusImpl","No Carrier exists for given carrierId.")
			.build();
		
		
	}
	
	public ShmShipment validateShipmentIdOrProNbr(String proNbr, Long shipmentInstId, TransactionContext txnContext, EntityManager entityManager) throws ValidationException {
		
		ShmShipment shmShipment = shmShipmentSubDAO.findByIdOrProNumber(proNbr, shipmentInstId, entityManager);
		if(Objects.isNull(shmShipment))
			throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
			.moreInfo("UpsertExternalStatusValidator","No Shipment exists for given proNbr or ShipmentInstId.")
			.build();	
		return shmShipment;
	}

}
