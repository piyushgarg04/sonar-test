package com.xpo.ltl.shipment.service.ejb.v1;

import java.util.List;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.lang.exception.ExceptionUtils;

import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.v2.BulkCreateShipmentSkeletonRqst;
import com.xpo.ltl.api.shipment.v2.CreateAndArchiveCopyBillDocumentRqst;
import com.xpo.ltl.api.shipment.v2.CreateMoverShipmentResp;
import com.xpo.ltl.api.shipment.v2.CreateMoverShipmentRqst;
import com.xpo.ltl.api.shipment.v2.CreateNonRevenueShipmentRqst;
import com.xpo.ltl.api.shipment.v2.CreateSalvageRequestResp;
import com.xpo.ltl.api.shipment.v2.CreateSalvageRequestRqst;
import com.xpo.ltl.api.shipment.v2.CreateShipmentSkeletonRqst;
import com.xpo.ltl.api.shipment.v2.SalvageRequest;
import com.xpo.ltl.api.shipment.v2.SalvageRequestStatusCd;
import com.xpo.ltl.api.shipment.v2.ShipmentSkeletonResponse;
import com.xpo.ltl.api.shipment.v2.UpdateSalvageRequestResp;
import com.xpo.ltl.api.shipment.v2.UpdateSalvageRequestRqst;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dto.InternalCreateNonRevenueShipmentResponseDTO;
import com.xpo.ltl.shipment.service.impl.CreateAndArchiveCopyBillDocumentImpl;
import com.xpo.ltl.shipment.service.impl.CreateMoverShipmentImpl;
import com.xpo.ltl.shipment.service.impl.CreateNonRevenueShipmentImpl;
import com.xpo.ltl.shipment.service.impl.CreateSalvageRequestImpl;
//import com.xpo.ltl.shipment.service.impl.CreateSalvageRequestImpl;
import com.xpo.ltl.shipment.service.impl.CreateShipmentSkeletonImpl;
import com.xpo.ltl.shipment.service.impl.UpdateSalvageRequestImpl;

@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class InternalServiceBean {

    @Resource
    private EJBContext ejbContext;

    @Inject
    private CreateShipmentSkeletonImpl createShipmentSkeletonImpl;

    @Inject
    private CreateAndArchiveCopyBillDocumentImpl createAndArchiveCopyBillDocumentImpl;

    @Inject
    private CreateMoverShipmentImpl createMoverShipmentImpl;

    @Inject
    private CreateNonRevenueShipmentImpl createNonRevenueShipmentImpl;

    @Inject
    private CreateSalvageRequestImpl createSalvageRequestImpl;
    
    @Inject
    private UpdateSalvageRequestImpl updateSalvageRequestImpl;
    
    @Inject
    private ExternalRestClient externalRestClient;
    
   @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ShipmentSkeletonResponse createShipmentSkeleton(CreateShipmentSkeletonRqst createShipmentSkeletonRqst,
        TransactionContext txnContext, EntityManager entityManager) throws ServiceException {
        try {
            return createShipmentSkeletonImpl.createShipmentSkeleton(
                createShipmentSkeletonRqst, txnContext,
                entityManager);

        } catch (ServiceException e) {
            ejbContext.setRollbackOnly();
            throw e;
        } catch (RuntimeException e) {
            ejbContext.setRollbackOnly();
            throw ExceptionBuilder
                .exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
                .moreInfo("InternalServiceBean.createShipmentSkeleton",
                    "An unexpected error occurred: " + ExceptionUtils.getRootCauseMessage(e))
                .log(e)
                .build();
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void performLegacyUpdates(ShipmentSkeletonResponse shipmentSkeletonResponse, TransactionContext txnContext) throws ServiceException {
        try {
            createShipmentSkeletonImpl.performLegacyUpdates(shipmentSkeletonResponse, txnContext);
        } catch (RuntimeException e) {
            throw ExceptionBuilder
                .exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
                .moreInfo("InternalServiceBean.performLegacyUpdates",
                    "An unexpected error occurred: " + ExceptionUtils.getRootCauseMessage(e))
                .log(e)
                .build();
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<ShipmentSkeletonResponse> bulkCreateShipmentSkeleton(
        BulkCreateShipmentSkeletonRqst bulkCreateShipmentSkeletonRqst, TransactionContext txnContext,
        EntityManager entityManager) throws ServiceException {
        try {
            return createShipmentSkeletonImpl
                .bulkCreateShipmentSkeleton(bulkCreateShipmentSkeletonRqst, txnContext,
                entityManager);
        } catch (ServiceException e) {
            ejbContext.setRollbackOnly();
            throw e;
        } catch (RuntimeException e) {
            ejbContext.setRollbackOnly();
            throw ExceptionBuilder
                .exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
                .moreInfo("InternalServiceBean.bulkCreateShipmentSkeleton",
                    "An unexpected error occurred: " + ExceptionUtils.getRootCauseMessage(e))
                .log(e)
                .build();
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void bulkPerformLegacyUpdates(List<ShipmentSkeletonResponse> shipmentSkeletonResponseList,
        TransactionContext txnContext) throws ServiceException {

        try {
            createShipmentSkeletonImpl.bulkPerformLegacyUpdates(shipmentSkeletonResponseList, txnContext);
        } catch (RuntimeException e) {
            throw ExceptionBuilder
                    .exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
                    .moreInfo("InternalServiceBean.bulkPerformLegacyUpdates",
                            "An unexpected error occurred: " + ExceptionUtils.getRootCauseMessage(e))
                    .log(e)
                    .build();
        }

    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void createAndArchiveCopyBillDocument(CreateAndArchiveCopyBillDocumentRqst createAndArchiveCopyBillDocumentRqst,
                                                                                 TransactionContext txnContext, EntityManager entityManager) throws ServiceException {

        try {
           createAndArchiveCopyBillDocumentImpl.createAndArchiveCopyBillDocument(createAndArchiveCopyBillDocumentRqst, txnContext, entityManager);
        } catch (Exception e) {
            throw ExceptionBuilder
                    .exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
                    .moreInfo("InternalServiceBean.createAndArchiveCopyBillDocument",
                            "An unexpected error occurred: " + ExceptionUtils.getRootCauseMessage(e))
                    .log(e)
                    .build();
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public CreateMoverShipmentResp createMoverShipment(CreateMoverShipmentRqst request,
                                                       TransactionContext txnContext, EntityManager entityManager) throws ServiceException {

        try {
            return createMoverShipmentImpl.createMoverShipment(request, txnContext, entityManager);
        } catch (Exception e) {
            ejbContext.setRollbackOnly();
            throw ExceptionBuilder
                    .exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
                    .moreInfo("InternalServiceBean.createMoverShipment",
                            "An unexpected error occurred: " + ExceptionUtils.getRootCauseMessage(e))
                    .log(e)
                    .build();
        }
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public InternalCreateNonRevenueShipmentResponseDTO createNonRevenueShipmentImpl(CreateNonRevenueShipmentRqst createNonRevenueShipmentRqst, TransactionContext txnContext,
        EntityManager entityManager)
            throws ServiceException {

        try {
            return createNonRevenueShipmentImpl.createNonRevenueShipmentImpl(createNonRevenueShipmentRqst, txnContext, entityManager);
        } catch (Exception e) {
            ejbContext.setRollbackOnly();
            throw ExceptionBuilder
                .exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
                .moreInfo("InternalServiceBean.createNonRevenueShipmentImpl", "An unexpected error occurred: " + ExceptionUtils.getRootCauseMessage(e))
                .log(e)
                .build();
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public UpdateSalvageRequestResp updateSalvageRequest(UpdateSalvageRequestRqst updateSalvageRequestRqst,
        TransactionContext txnContext, EntityManager entityManager) throws ServiceException, ValidationException, NotFoundException {
        try {
            if (updateSalvageRequestRqst == null) {
                throw ExceptionBuilder.exception(ValidationErrorMessage.REQUEST_REQUIRED, txnContext).build();
            } else {
                return updateSalvageRequestImpl.updateSalvageRequest(updateSalvageRequestRqst, txnContext, entityManager);
            }
        } catch (final ServiceException e) {
            ejbContext.setRollbackOnly();
            throw e;
        } catch (final RuntimeException e) {
            ejbContext.setRollbackOnly();
            throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
            .contextValues(String.format("Run time  Exception during Upsert :")).log(e).build();
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void processApprovedForCMK(Long salvageRequestId, TransactionContext txnContext) throws ServiceException {
        try {
            externalRestClient.startApprovedSalvagesChEnsemble(salvageRequestId, txnContext);
        } catch (ServiceException e) {
            throw e;
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public CreateSalvageRequestResp createSalvageRequest(CreateSalvageRequestRqst createSalvageRequestRqst,
        TransactionContext txnContext, EntityManager entityManager) throws ServiceException, ValidationException, NotFoundException {
        try {
            if (createSalvageRequestRqst == null) {
                throw ExceptionBuilder.exception(ValidationErrorMessage.REQUEST_REQUIRED, txnContext).build();
            } else {
                return createSalvageRequestImpl.createSalvageRequest(createSalvageRequestRqst, txnContext, entityManager);
            }
        } catch (final ServiceException e) {
            ejbContext.setRollbackOnly();
            throw e;
        } catch (final RuntimeException e) {
            ejbContext.setRollbackOnly();
            throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
            .contextValues(String.format("Run time  Exception during Upsert :")).log(e).build();
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void createBilledGCBZShipment(SalvageRequest salvageRequest, List<String> childPros, TransactionContext txnContext, EntityManager entityManager) throws ServiceException {
        try {
            createSalvageRequestImpl.createBilledGCBZShipment(salvageRequest, txnContext, entityManager, childPros);
        } catch (ServiceException e) {
            throw e;
        }
    }

}
