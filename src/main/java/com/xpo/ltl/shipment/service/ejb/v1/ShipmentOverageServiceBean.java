package com.xpo.ltl.shipment.service.ejb.v1;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.ListMetadata;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.v2.ShipmentOverageServiceIF;
import com.xpo.ltl.api.shipment.v2.ArchiveShipmentImagesResp;
import com.xpo.ltl.api.shipment.v2.ArchiveShipmentImagesRqst;
import com.xpo.ltl.api.shipment.v2.CreateSalvageRequestNoteResp;
import com.xpo.ltl.api.shipment.v2.CreateSalvageRequestNoteRqst;
import com.xpo.ltl.api.shipment.v2.CreateSalvageRequestResp;
import com.xpo.ltl.api.shipment.v2.CreateSalvageRequestRqst;
import com.xpo.ltl.api.shipment.v2.ListBrandsForSalvageResp;
import com.xpo.ltl.api.shipment.v2.ListSalvageRequestNotesResp;
import com.xpo.ltl.api.shipment.v2.ListSalvageRequestsResp;
import com.xpo.ltl.api.shipment.v2.ListSalvageRequestsRqst;
import com.xpo.ltl.api.shipment.v2.OsdImage;
import com.xpo.ltl.api.shipment.v2.SalvageRequestStatusCd;
import com.xpo.ltl.api.shipment.v2.UpdateSalvageRequestResp;
import com.xpo.ltl.api.shipment.v2.UpdateSalvageRequestRqst;
import com.xpo.ltl.api.shipment.v2.UpsertShipmentOverageImageRqst;
import com.xpo.ltl.shipment.service.impl.CreateSalvageRequestImpl;
import com.xpo.ltl.shipment.service.impl.CreateSalvageRequestNoteImpl;
import com.xpo.ltl.shipment.service.impl.ListSalvageBrandImpl;
import com.xpo.ltl.shipment.service.impl.ListSalvageRequestNoteImpl;
import com.xpo.ltl.shipment.service.impl.ListSalvageRequestsImpl;
import com.xpo.ltl.shipment.service.impl.MaintainShmOvrgHdrImpl;
import com.xpo.ltl.shipment.service.impl.UpdateSalvageRequestImpl;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;


@Stateless
@Local(ShipmentOverageServiceIF.class)
@TransactionManagement(TransactionManagementType.CONTAINER)
@LogExecutionTime
public class ShipmentOverageServiceBean implements ShipmentOverageServiceIF {
	private static final Log logger = LogFactory.getLog(ShipmentOverageServiceBean.class);

	@PersistenceContext(unitName = "ltl-java-shipment-jaxrs" )
	EntityManager entityManager;

	@Resource
	EJBContext ejbContext;

    @Inject
    private InternalServiceBean internalServiceBean;

	@Inject
	private MaintainShmOvrgHdrImpl maintainShmOvrgHdrImpl;
	
	@Inject
	private ListSalvageBrandImpl listSalvageBrandImpl;
	
	@Inject
	private ListSalvageRequestsImpl listSalvageRequestsImpl; 
	
	@Inject
	private CreateSalvageRequestImpl createSalvageRequestImpl;
	
	@Inject
	private UpdateSalvageRequestImpl updateSalvageRequestImpl;
	
	@Inject
	private CreateSalvageRequestNoteImpl createSalvageRequestNoteImpl;
	
	@Inject
	private ListSalvageRequestNoteImpl listSalvageRequestNoteImpl;
	
	private static final String NOT_IMPLEMENTED_MESSAGE = "This Service is not yet implemented";

	@Override
	public ArchiveShipmentImagesResp archiveShipmentImages(ArchiveShipmentImagesRqst archiveShipmentImagesRqst,
			String dmsAuth, TransactionContext txnContext) throws ServiceException, ValidationException {
		try {
			ArchiveShipmentImagesResp archiveShipmentImageResp = new ArchiveShipmentImagesResp();
			List<String> docArchiveTimestamp = new ArrayList<String>();
			for (int i = 0; i < archiveShipmentImagesRqst.getDocuments().size(); i++) {
				String encodedString = archiveShipmentImagesRqst.getDocuments().get(i);
				docArchiveTimestamp.add(maintainShmOvrgHdrImpl.archiveDmsDocument("OSDF",
						archiveShipmentImagesRqst.getIndices().get(0).getValue(),
						encodedString, i, archiveShipmentImagesRqst.getDmsAuth(),
						txnContext, entityManager));
			}
			archiveShipmentImageResp.setDocArchiveTimestamps(docArchiveTimestamp);
			return archiveShipmentImageResp;

		} catch (final ServiceException e) {
            ejbContext.setRollbackOnly();
			logger.error("Service Exception during Image Archive : " + ExceptionUtils.getFullStackTrace(e));
            throw e;
		} catch (final RuntimeException e) {
            ejbContext.setRollbackOnly();
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
					.contextValues(String.format("Run time  Exception during Image Archive :")).log(e).build();
		}
	}

	@Override
	public void upsertShipmentOverageImage(UpsertShipmentOverageImageRqst upsertShipmentOverageImageRqst,
			TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
		try {
			if (upsertShipmentOverageImageRqst == null
					|| upsertShipmentOverageImageRqst.getOverageImageHeader() == null) {
				throw ExceptionBuilder.exception(ValidationErrorMessage.REQUEST_REQUIRED, txnContext).build();
			} else {
				OsdImage osdImage = upsertShipmentOverageImageRqst.getOverageImageHeader();
				maintainShmOvrgHdrImpl.upsert(osdImage, txnContext, entityManager);
			}
		} catch (final ServiceException e) {
			ejbContext.setRollbackOnly();
			logger.error("Service Exception during Upsert : " + ExceptionUtils.getFullStackTrace(e));
			throw e;
		} catch (final RuntimeException e) {
			ejbContext.setRollbackOnly();
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
					.contextValues(String.format("Run time  Exception during Upsert :")).log(e).build();
		}
	}

	@Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
	public CreateSalvageRequestResp createSalvageRequest(CreateSalvageRequestRqst createSalvageRequestRqst,
			TransactionContext txnContext) throws ServiceException, ValidationException {
	    CreateSalvageRequestResp createSalvageRequestResp = null;
		try {
			if (createSalvageRequestRqst == null) {
				throw ExceptionBuilder.exception(ValidationErrorMessage.REQUEST_REQUIRED, txnContext).build();
			} else {
			    createSalvageRequestResp = internalServiceBean.createSalvageRequest(createSalvageRequestRqst, txnContext, entityManager);
			    try {
			        internalServiceBean.createBilledGCBZShipment(createSalvageRequestResp.getSalvageRequest(), createSalvageRequestRqst.getChildProNbrs(), txnContext, entityManager);
			    } catch (Exception e) {
                    logger.warn("Service Exception on GCBZ shipment creation call : " + ExceptionUtils.getFullStackTrace(e));
                    //TODO make the api return validationError so we can show better error
			    }
			    return createSalvageRequestResp;
			}
		        
		} catch (final ServiceException e) {
			logger.error("Service Exception during Insert : " + ExceptionUtils.getFullStackTrace(e));
			throw e;
		} catch (final RuntimeException e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
					.contextValues(String.format("Run time  Exception during Upsert :")).log(e).build();
		}
	}

	@Override
	public ListBrandsForSalvageResp listBrandsForSalvage(ListMetadata listMetadata, TransactionContext txnContext)
			throws ServiceException, ValidationException, NotFoundException {
		return listSalvageBrandImpl.listSalvageBrand(listMetadata, txnContext, entityManager);
	}

	@Override
	public ListSalvageRequestsResp listSalvageRequests(ListSalvageRequestsRqst listSalvageRequestsRqst,
			TransactionContext txnContext) throws ServiceException, ValidationException {
		return listSalvageRequestsImpl.listSalvageRequests(listSalvageRequestsRqst, txnContext, entityManager);
	}

	@Override
	public CreateSalvageRequestNoteResp createSalvageRequestNote(
			CreateSalvageRequestNoteRqst createSalvageRequestNoteRqst, TransactionContext txnContext)
			throws ServiceException, ValidationException {
		try {
			if (createSalvageRequestNoteRqst == null) {
				throw ExceptionBuilder.exception(ValidationErrorMessage.REQUEST_REQUIRED, txnContext).build();
			} else {
				return createSalvageRequestNoteImpl.createSalvageRequestNote(createSalvageRequestNoteRqst, txnContext, entityManager);
			}
		} catch (final ServiceException e) {
			ejbContext.setRollbackOnly();
			logger.error("Service Exception during Insert : " + ExceptionUtils.getFullStackTrace(e));
			throw e;
		} catch (final RuntimeException e) {
			ejbContext.setRollbackOnly();
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
					.contextValues(String.format("Run time  Exception during Upsert :")).log(e).build();
		}
	}

	@Override
	public ListSalvageRequestNotesResp listSalvageRequestNotes(Long salvageRequestId, TransactionContext txnContext)
			throws ServiceException, ValidationException, NotFoundException {
		return listSalvageRequestNoteImpl.listSalvageRequestNotes(salvageRequestId, txnContext, entityManager);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public UpdateSalvageRequestResp updateSalvageRequest(UpdateSalvageRequestRqst updateSalvageRequestRqst,
			TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
		try {
		    UpdateSalvageRequestResp resp = null;
			if (updateSalvageRequestRqst == null) {
				throw ExceptionBuilder.exception(ValidationErrorMessage.REQUEST_REQUIRED, txnContext).build();
			} else {
			    resp = internalServiceBean.updateSalvageRequest(updateSalvageRequestRqst, txnContext, entityManager);
			    
			    if (resp.getSalvageRequest() != null 
			            && resp.getSalvageRequest().getSalvageRequestId() != null
			            && SalvageRequestStatusCd .APPROVED_FOR_CMK == resp.getSalvageRequest().getStatusCd()) {
			        
			        try {
			            internalServiceBean.processApprovedForCMK(resp.getSalvageRequest().getSalvageRequestId(), txnContext);
			        } catch (final ServiceException e) {
			            logger.warn("Service Exception on approved salvages channel API call : " + ExceptionUtils.getFullStackTrace(e));
			        }
			    }
			}
			return resp;
		} catch (final ServiceException e) {
			logger.error("Service Exception during Update : " + ExceptionUtils.getFullStackTrace(e));
			throw e;
		} catch (final RuntimeException e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
					.contextValues(String.format("Run time  Exception during Upsert :")).log(e).build();
		}
	}

}