package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.api.client.util.Lists;
import com.xpo.ltl.api.client.common.Attachment;
import com.xpo.ltl.api.client.common.JsonAttachment;
import com.xpo.ltl.api.documentmanagement.v1.ArchiveDocumentResp;
import com.xpo.ltl.api.documentmanagement.v1.DmsArchiveRequest;
import com.xpo.ltl.api.documentmanagement.v1.DmsIndex;
import com.xpo.ltl.api.documentmanagement.v1.ImageFile;
import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.humanresource.v1.Employee;
import com.xpo.ltl.api.infrastructure.client.v2.InfrastructureClient;
import com.xpo.ltl.api.location.v2.GetLocationReferenceDetailsResp;
import com.xpo.ltl.api.location.v2.ListLocationFeaturesResp;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdHeader;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdImage;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageRequest;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageRqst;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.ConeColorCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OsdCategoryCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OverageApprovalStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.SalvageRequestStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.BulkUpsertHandlingUnitsRqst;
import com.xpo.ltl.api.shipment.v2.ConeColorCd;
import com.xpo.ltl.api.shipment.v2.HandlingUnit;
import com.xpo.ltl.api.shipment.v2.HandlingUnitTypeCd;
import com.xpo.ltl.api.shipment.v2.OsdCategoryCd;
import com.xpo.ltl.api.shipment.v2.OsdImage;
import com.xpo.ltl.api.shipment.v2.OverageApprovalStatusCd;
import com.xpo.ltl.api.shipment.v2.SalvageRequestStatusCd;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmOsdHdrSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmOsdImageSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmSalvageRequestSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmSalvageRqstSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmSalvageStatusSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;


@RequestScoped
public class MaintainShmOvrgHdrImpl {
	private static final Log logger = LogFactory.getLog(MaintainShmOvrgHdrImpl.class);
	private static final String CURRENT_LOCATION = MaintainShmOvrgHdrImpl.class.getCanonicalName();
	private static final Double DefaultValue = 0.0;
	private static final String EMAIL_FROM = "overage@xpo.com";
	private static final String EMAIL_FROM_NAME = "Overage";

	private static final String SHM_OSD_HEADER_SEQ = "SHM_OSD_HEADER_SEQ";
	private static final String SHM_OVRG_IMG_HDR_SEQ = "SHM_OSD_IMAGE_SEQ";
	private static final String EMAIL_SUBJECT = "[MATCHED] : Activity under Shipment ";
	
	
    public final static String NEWLINE = "<br/>";
	
	@Inject
	private ShmOsdImageSubDAO shmOsdImageDAO;

	@Inject
	private ExternalRestClient externalRestClient;
	
	@Inject
	private ShmSalvageRqstSubDAO shmSalvageRqstSubDAO;
	
	@PersistenceContext(unitName = "ltl-java-shipment-rpt-jaxrs")
	private EntityManager rptEntityManager;
	
	@Inject
	private BulkUpsertHandlingUnitsImpl bulkUpsertHandlingUnitsImpl;
	
	@Inject
	DeleteHandlingUnitImpl deleteHandlingUnitImpl;
	
	@Inject
	private ShmShipmentSubDAO shmShipmentSubDAO;	
	
	@Inject
	private ShmSalvageRequestSubDAO shmSalvageRequestSubDAO;
	
	@Inject
    private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;
	
	@Inject
	private AppContext appContext;
	
	@Inject
	private ShmSalvageStatusSubDAO shmSalvageStatusSubDAO;
	
	@PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
	private EntityManager db2EntityManager;
	
	@Inject
	private ShmOsdHdrSubDAO shmOsdHdrSubDAO;


	public void upsert(OsdImage osdImage, TransactionContext txnContext,
			EntityManager entityManager) throws ServiceException {
		checkNotNull(osdImage, "The header is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
		AuditInfo auditInfo = new AuditInfo();
		AuditInfoHelper.setCreatedInfo(auditInfo, txnContext);
		AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);
		ShmOsdImage hdrEntity = null;
		ShmSalvageRequest relatedShmSalvage = null;
		osdImage.setAuditInfo(auditInfo);
		Long instId = osdImage.getInstId();
		
		boolean matchAction = false; //isMatchAction(overageImageHeader);
		boolean unmatchAction = false;
		if (instId != null) {
			hdrEntity = shmOsdImageDAO.findById(osdImage.getInstId(), entityManager);
			if(hdrEntity != null && StringUtils.isBlank(hdrEntity.getProNbrTxt())) {
				validateChildProAlreadyExist(osdImage, txnContext, entityManager);
			}
			osdImage.setApprovalStatusCd(OverageApprovalStatusCdTransformer.toEnum(hdrEntity.getApprovalStatusCd()));
			relatedShmSalvage = hdrEntity.getShmSalvageRequest();
			
			//consider a new origin pro as a match action if it's attached to a salvage (FE will only have Unmatch button if matched to a non-salvage PRO)
            matchAction = StringUtils.isNotBlank(osdImage.getOriginProNbr()) 
                    && ((!handleProNum(osdImage.getOriginProNbr(), txnContext).equals(StringUtils.trimToEmpty(hdrEntity.getOrigProNbrTxt())))
                            || BooleanUtils.isTrue(BasicTransformer.toBoolean(hdrEntity.getPartialPairedWithShortInd()))
                            || StringUtils.isBlank(hdrEntity.getOrigProNbrTxt()));
            
            unmatchAction = BooleanUtils.isNotTrue(osdImage.getOverPairedWithShortInd())
                    && BooleanUtils.isNotTrue(osdImage.getPartialPairedWithShortInd())
                    && (BooleanUtils.isTrue(BasicTransformer.toBoolean(hdrEntity.getPartialPairedWithShortInd()))
                            || BooleanUtils.isTrue(BasicTransformer.toBoolean(hdrEntity.getOverPairedWithShortInd())));
            
			if((hdrEntity != null && relatedShmSalvage != null)) {
				if(isApprovedSalvage(relatedShmSalvage) && matchAction) {
				    String cmkMsg = StringUtils.EMPTY;
				    if (SalvageRequestStatusCd.APPROVED_FOR_CMK == SalvageRequestStatusCdTransformer.toEnum(relatedShmSalvage.getStatusCd())) {
				        cmkMsg = " Please contact CMK.";
				    }
					throw ExceptionBuilder
					.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
					.moreInfo(CURRENT_LOCATION, "Cannot perform this action. Overage is on an Approved Salvage Request." + cmkMsg)
					.log()
					.build();
				}else if(!matchAction) {
				    throw ExceptionBuilder
				    .exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
				    .moreInfo(CURRENT_LOCATION, "Cannot Perform action. Overage is on a Salvage Request.")
				    .log()
				    .build();
				}
			}
			if(matchAction && relatedShmSalvage == null && StringUtils.isNotBlank(hdrEntity.getOrigProNbrTxt()) 
			        && BooleanUtils.isNotTrue(BasicTransformer.toBoolean(hdrEntity.getPartialPairedWithShortInd()))) {
				throw ExceptionBuilder
				.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
				.moreInfo(CURRENT_LOCATION, "Overage is already matched. Please unmatch from Original PRO.")
				.log()
				.build();
			}
			if(isPartialMatchAction(osdImage) && "Y".equals(hdrEntity.getPartialPairedWithShortInd())) {
				throw ExceptionBuilder
				.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
				.moreInfo(CURRENT_LOCATION, "This shipment is already partial matched, please Match it first.")
				.log()
				.build();
			}

			if(unmatchAction && 
					("N".equals(hdrEntity.getOverPairedWithShortInd()) &&
					"N".equals(hdrEntity.getPartialPairedWithShortInd()))) {
				throw ExceptionBuilder
				.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
				.moreInfo(CURRENT_LOCATION, "This shipment is already Unmatched, please Match it first.")
				.log()
				.build();
			}
			if(!matchAction && !unmatchAction && !isPartialMatchAction(osdImage) 
			        && (StringUtils.isNotBlank(hdrEntity.getOrigProNbrTxt()) || relatedShmSalvage != null)){
                throw ExceptionBuilder
                .exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
                .moreInfo(CURRENT_LOCATION, "This shipment is already Matched, edit is not allowed.")
                .log()
                .build();
			}
			
//			String elevenDigitProNumber = ProNumberHelper.toElevenDigitPro(overageImageHeader.getProNbr(), txnContext);
			if(matchAction || isPartialMatchAction(osdImage)) {
				ShmShipment shmShipment = shmShipmentSubDAO.findByIdOrProNumber(ProNumberHelper.toElevenDigitPro(osdImage.getOriginProNbr(), txnContext), null, entityManager);
				if(shmShipment!= null && (shmShipment.getMvmtStatCd() != null && shmShipment.getMvmtStatCd().equals("5"))) {
					throw ExceptionBuilder
					.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
					.moreInfo(CURRENT_LOCATION, "Cannot match to a Final delivered PRO.")
					.log()
					.build();
				}

                if (shmShipment != null && BasicTransformer.toBoolean(shmShipment.getHandlingUnitExemptionInd())) {

                    if (NumberUtils.compare(BasicTransformer.toInt(shmShipment.getMtrzdPcsCnt().add(shmShipment.getLoosePcsCnt())), 1) > 0) {
                        throw ExceptionBuilder
                            .exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
                            .moreInfo(CURRENT_LOCATION, "Cannot match to a handling unit exempt shipment with more than 1 piece.")
                            .log()
                            .build();
                    }
                }
			}
			
			if(unmatchAction
					&& !hdrEntity.getLstUpdtBy().equals(auditInfo.getUpdateById())
					&& hdrEntity.getOverPairedWithShortInd().equals(Boolean.TRUE)) {
				Employee employee = externalRestClient.getEmployeeDetailsByEmployeeId(hdrEntity.getLstUpdtBy(), txnContext);
				if(employee != null) {
					throw ExceptionBuilder
					.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
					.moreInfo(CURRENT_LOCATION, "Contact "+getEmployeeName(employee)+" ("+getEmployeeEmail(employee)+") to Unmatch this shipment ")
					.log()
					.build();
				} else {
					throw ExceptionBuilder
					.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
					.moreInfo(CURRENT_LOCATION, "Please contact SIC Admin")
					.log()
					.build();
				}
			}
		}

		if (hdrEntity == null || Long.valueOf(hdrEntity.getInstId()) == null || instId == null) {
			validateChildProAlreadyExist(osdImage, txnContext, entityManager);
			hdrEntity = new ShmOsdImage();
			hdrEntity.setCrteBy(auditInfo.getCreatedById());
			hdrEntity.setCrtePgmId(auditInfo.getCreateByPgmId());
			hdrEntity.setCrteTmst(
					new Timestamp(auditInfo.getCreatedTimestamp().toGregorianCalendar().getTimeInMillis()));
			ShmOsdHeader shmOsdHeader = createShmOsdHeaderEntry(osdImage, entityManager, auditInfo);
			hdrEntity.setShmOsdHeader(shmOsdHeader);
			osdImage.setApprovalStatusCd(OverageApprovalStatusCd.APPROVED);
			osdImage.setInstId(shmOsdImageDAO.getNextSequence(SHM_OVRG_IMG_HDR_SEQ, entityManager));
		}
		osdImage.setOsdCategoryCd(OsdCategoryCdTransformer.toCode(OsdCategoryCd.OVERAGE));
		hdrEntity.setLstUpdtBy(auditInfo.getUpdateById());
		hdrEntity.setLstUpdtPgmId(auditInfo.getUpdateByPgmId());
		hdrEntity.setLstUpdtTmst(new Timestamp(auditInfo.getUpdatedTimestamp().toGregorianCalendar().getTimeInMillis()));
		DtoTransformer.toShmOsdImage(osdImage, hdrEntity);
		//shmOsdImageDAO.setInstId(hdrEntity, SHM_OVRG_IMG_HDR_SEQ, entityManager);
		hdrEntity.setProNbrTxt(handleProNum(osdImage.getProNbr(), txnContext));
		
		if(instId != null && isNotEligibleForUnmatch(osdImage)) {
			hdrEntity.setOrigProNbrTxt(handleProNum(osdImage.getOriginProNbr(), txnContext));
			if(hdrEntity.getShmOsdHeader() != null) {
				ShmOsdHeader osdHeader = shmOsdHdrSubDAO.findById(hdrEntity.getShmOsdHeader().getOsdId(), entityManager);
				osdHeader.setProNbrTxt(handleProNum(osdImage.getOriginProNbr(), txnContext));
				osdHeader.setOsdDescription(hdrEntity.getDescTxt());
				shmOsdHdrSubDAO.save(osdHeader, entityManager);
			}
		} else {
			hdrEntity.setOrigProNbrTxt(null);
			if(hdrEntity.getShmOsdHeader() != null) {
				ShmOsdHeader osdHeader = shmOsdHdrSubDAO.findById(hdrEntity.getShmOsdHeader().getOsdId(), entityManager);
				osdHeader.setProNbrTxt(null);
				osdHeader.setOsdDescription(hdrEntity.getDescTxt());
				shmOsdHdrSubDAO.save(osdHeader, entityManager);
			}
		}
		hdrEntity = shmOsdImageDAO.save(hdrEntity, entityManager);
		
		logger.debug("Save Succesful for Shipment Image with PRO # " + hdrEntity.getProNbrTxt());
		entityManager.flush();
		if(matchAction) {
			String elevenDigitProNumber = ProNumberHelper.toElevenDigitPro(osdImage.getProNbr(), txnContext);
			
            boolean isBackSync = false;
            try {
                ListLocationFeaturesResp locFeatureSetting = externalRestClient.getLocFeatureSetting("SLVG_BACKSYNC", txnContext);
                isBackSync = locFeatureSetting.getLocationFeatures() != null && BasicTransformer.toBoolean((locFeatureSetting.getLocationFeatures().get(0).getSettingValue()));
            } catch (ServiceException e) {
                logger.warn("Feature not available. No backsync to old salvage application will occur.");
            }
            
			if(ProNumberHelper.isBluePro(elevenDigitProNumber) && hdrEntity != null && relatedShmSalvage != null) {
			    relatedShmSalvage.setStatusCd(SalvageRequestStatusCdTransformer.toCode(SalvageRequestStatusCd.CLOSED_RETURN_TO_CUSTOMER));
                relatedShmSalvage.setCommentTxt(relatedShmSalvage.getCommentTxt() + " ; The shipment " + getFormattedProNumber(relatedShmSalvage.getProNbrTxt(), txnContext)+ " under Salvage Request " + relatedShmSalvage.getSalvageRequestId() +" has been matched and the Salvage Request is closed.");
			    shmSalvageRequestSubDAO.save(relatedShmSalvage, entityManager);
			    if (isBackSync) {
			        ShmSalvageRqst shmSalvageRqst = shmSalvageRqstSubDAO.findById(relatedShmSalvage.getSalvageRequestId(), rptEntityManager);
			        shmSalvageRqst.setSalvageStatus(shmSalvageStatusSubDAO.findById("Closed: return-to-customer", rptEntityManager));
//			        shmSalvageRqst.setRequestCc(relatedShmSalvage.getCommentTxt());
			        shmSalvageRqstSubDAO.save(shmSalvageRqst, rptEntityManager);
			    }
                sendSalvageOverageMail(true, relatedShmSalvage, txnContext, hdrEntity);
			    hdrEntity.setShmSalvageRequest(null);
			    shmOsdImageDAO.save(hdrEntity, entityManager);
			} else if(ProNumberHelper.isYellowPro(elevenDigitProNumber) && hdrEntity != null && relatedShmSalvage != null) {					
			    boolean isClosed = false;
			    if(relatedShmSalvage.getPcsCnt().compareTo(new BigDecimal(1)) == 0) {
			        relatedShmSalvage.setPcsCnt(relatedShmSalvage.getPcsCnt().subtract(new BigDecimal(1)));
			        relatedShmSalvage.setStatusCd(SalvageRequestStatusCdTransformer.toCode(SalvageRequestStatusCd.CLOSED_RETURN_TO_CUSTOMER));
			        relatedShmSalvage.setCommentTxt(relatedShmSalvage.getCommentTxt() + " ; The shipment " + getFormattedProNumber(relatedShmSalvage.getProNbrTxt(), txnContext)+ " under Salvage Request " + relatedShmSalvage.getSalvageRequestId() +" has been matched and the Salvage Request is closed.");
			        isClosed = true;
			    } else {
			        relatedShmSalvage.setPcsCnt(relatedShmSalvage.getPcsCnt().subtract(new BigDecimal(1)));
			        relatedShmSalvage.setCommentTxt(relatedShmSalvage.getCommentTxt() + " ; The shipment "+getFormattedProNumber(relatedShmSalvage.getProNbrTxt(), txnContext)+" under Salvage Request "+ relatedShmSalvage.getSalvageRequestId()+" has been matched and removed. "
                    + "Kindly update the Salvage Request Parameters as per the recent changes before approving the request. Please find below details for matched shipment.");
			    }
                shmSalvageRequestSubDAO.save(relatedShmSalvage, entityManager);
			    if (isBackSync) {
			        ShmSalvageRqst shmSalvageRqst = shmSalvageRqstSubDAO.findById(relatedShmSalvage.getSalvageRequestId(), rptEntityManager);
			        int pieceCount = Integer.parseInt(shmSalvageRqst.getPkgPieces()) - 1;
			        if(Integer.parseInt(shmSalvageRqst.getPkgPieces()) == 1) {
			            shmSalvageRqst.setSalvageStatus(shmSalvageStatusSubDAO.findById("Closed: return-to-customer", rptEntityManager));
			            shmSalvageRqst.setPkgPieces(String.valueOf(pieceCount));
			        } else {
			            shmSalvageRqst.setPkgPieces(String.valueOf(pieceCount));
			        }
//			        shmSalvageRqst.setRequestCc(relatedShmSalvage.getCommentTxt());
			        shmSalvageRqstSubDAO.save(shmSalvageRqst, rptEntityManager);
			    }
                sendSalvageOverageMail(isClosed, relatedShmSalvage, txnContext, hdrEntity);
			    deleteHandlingUnitImpl.deleteHandlingUnit(elevenDigitProNumber, txnContext, entityManager);
			    hdrEntity.setShmSalvageRequest(null);
			    shmOsdImageDAO.save(hdrEntity, entityManager);
			}  else {
				sendOverageMail(txnContext, hdrEntity);
			}

			if(isEligibleToCreateAsTray(hdrEntity, instId)) {
	            createAsTray(hdrEntity, txnContext, entityManager);
	        }
		}
		
		if(unmatchAction && isEligibleToDeleteAsTray(hdrEntity, instId)) {
		    deleteHandlingUnitImpl.deleteHandlingUnit(ProNumberHelper.toElevenDigitPro(hdrEntity.getProNbrTxt(), txnContext), txnContext, entityManager);
		    //deleteAsTray(ProNumberHelper.toElevenDigitPro(hdrEntity.getProNbrTxt(), txnContext), entityManager, txnContext);
		}
	}
	
	private void sendOverageMail(TransactionContext txnContext,ShmOsdImage hdrEntity) {
		
		try {
			Employee employee =  externalRestClient.getEmployeeDetailsByEmployeeId(hdrEntity.getLstUpdtBy(), txnContext);
			String employeeName = getEmployeeName(employee);
			StringBuffer buffer = new StringBuffer().append("Please find below details for matched shipment.");
			buffer.append(NEWLINE+NEWLINE);
			buffer.append("Overage Pro number : " +getFormattedProNumber(hdrEntity.getProNbrTxt(), txnContext));
			buffer.append(NEWLINE);
			buffer.append("Original pro number : " + getFormattedProNumber(hdrEntity.getOrigProNbrTxt(), txnContext));
			buffer.append(NEWLINE);
			buffer.append("Uploaded SIC code : " + hdrEntity.getRptgSicCd());
			buffer.append(NEWLINE);
			buffer.append("Matched By : " + employeeName);
			buffer.append(NEWLINE+NEWLINE);
			buffer.append("If you have any query(s) please reach out to "+employeeName+" ("+getEmployeeEmail(employee)+")."); 
			InfrastructureClient infrastructureClient = externalRestClient.getInfrastructureClient(txnContext);
			infrastructureClient.sendEmail(null, 
					null, 
					EMAIL_FROM, 
					EMAIL_FROM_NAME, 
					buffer.toString(), 
					null, 
					EMAIL_SUBJECT + getFormattedProNumber(hdrEntity.getProNbrTxt(), txnContext) ,
					appContext.getSalvageToEmail().equals("dynamic") ? String.format("ltlcsr-%s@xpo.com", hdrEntity.getRptgSicCd().toLowerCase()) : appContext.getSalvageToEmail(),
					null);
		} catch (com.xpo.ltl.api.client.exception.ServiceException | ServiceException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	private void sendSalvageOverageMail(boolean isClosed, ShmSalvageRequest shmSalvageRequest, TransactionContext txnContext,ShmOsdImage hdrEntity) {
		
		try {
			Employee employee =  externalRestClient.getEmployeeDetailsByEmployeeId(hdrEntity.getLstUpdtBy(), txnContext);
			Employee requestor =  externalRestClient.getEmployeeDetailsByEmployeeId(shmSalvageRequest.getCrteBy(), txnContext);
			String employeeName = getEmployeeName(employee);
			String subject = "OS&D [MATCHED] : Activity under Shipment "+getFormattedProNumber(shmSalvageRequest.getProNbrTxt(), txnContext)+" and Salvage Request "+ shmSalvageRequest.getSalvageRequestId();
			StringBuffer buffer = new StringBuffer();
			if(!isClosed) {
				buffer.append("The shipment "+getFormattedProNumber(shmSalvageRequest.getProNbrTxt(), txnContext)+" under Salvage Request "+ shmSalvageRequest.getSalvageRequestId()+" has been matched and removed. "
					+ "Kindly update the Salvage Request Parameters as per the recent changes before approving the request. Please find below details for matched shipment.");
			} else {
				buffer.append("The shipment "+getFormattedProNumber(shmSalvageRequest.getProNbrTxt(), txnContext)+" under Salvage Request "+ shmSalvageRequest.getSalvageRequestId()+" has been matched and the Salvage Request is closed. Please find below details for matched shipment.");	
			}
			buffer.append(NEWLINE+NEWLINE);
			buffer.append("Overage Pro number : " +getFormattedProNumber(hdrEntity.getProNbrTxt(), txnContext));
			buffer.append(NEWLINE);
			buffer.append("Original pro number : " + getFormattedProNumber(hdrEntity.getOrigProNbrTxt(), txnContext));
			buffer.append(NEWLINE);
			buffer.append("Uploaded SIC code : " + hdrEntity.getRptgSicCd());
			buffer.append(NEWLINE);
			buffer.append("Matched By : " + employeeName);
			buffer.append(NEWLINE);
			buffer.append("TimeStamp : " + shmSalvageRequest.getLstUpdtTmst());
			buffer.append(NEWLINE+NEWLINE);
			buffer.append("If you have any query(s) please reach out to "+employeeName+" ("+getEmployeeEmail(employee)+")."); 
			InfrastructureClient infrastructureClient = externalRestClient.getInfrastructureClient(txnContext);
			String toEmail = appContext.getSalvageToEmail().equals("dynamic") ? 
					String.format("ltlcsr-%s@xpo.com", hdrEntity.getRptgSicCd().toLowerCase()) + ",Jessica.Davis@xpo.com,"+ getEmployeeEmail(requestor) : appContext.getSalvageToEmail();
			
			infrastructureClient.sendEmail(null, 
					null, 
					EMAIL_FROM, 
					EMAIL_FROM_NAME, 
					buffer.toString(), 
					null, 
					subject,
					toEmail,
					null);
		} catch (com.xpo.ltl.api.client.exception.ServiceException | ServiceException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private String getEmployeeEmail(Employee employee) {
		String employeeEmail = employee != null ? employee.getEmailAddress() : "SYSTEM";
		return employeeEmail;
	}

	private String getEmployeeName(Employee employee) {
		String employeeName =  employee != null ? employee.getBasicInfo().getFirstName() +" "+ employee.getBasicInfo().getLastName() : "SYSTEM";
		return employeeName;
	} 
	
	private String getFormattedProNumber(String proNumber, TransactionContext txnContext) throws ServiceException {
		final String elevenDigitProNum = ProNumberHelper.validateProNumber(proNumber, txnContext);
		if(ProNumberHelper.isYellowPro(elevenDigitProNum)) {
			return ProNumberHelper.toTenDigitPro(elevenDigitProNum);
		} else if(ProNumberHelper.isBluePro(elevenDigitProNum)) {
			return ProNumberHelper.toNineDigitProHyphen(elevenDigitProNum, txnContext);
		}
		return null;
	}

	private void validateChildProAlreadyExist(OsdImage osdImage, TransactionContext txnContext,
			EntityManager entityManager) throws ServiceException, ValidationException {
		String elevenDigitProNum = ProNumberHelper.validateProNumber(osdImage.getProNbr(), txnContext);
		if(StringUtils.isNotBlank(elevenDigitProNum) && ProNumberHelper.isYellowPro(elevenDigitProNum)) {
			ShmOsdImage shmOvrgImgHdr = getShmOSDImage(elevenDigitProNum, txnContext, entityManager);
			if(shmOvrgImgHdr != null) {
				throw ExceptionBuilder
				.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
				.moreInfo(CURRENT_LOCATION, "Cannot upload a new shipment with the existing PRO.")
				.log()
				.build();
			}
			Set<String> childProSet = new HashSet<>(Arrays.asList(elevenDigitProNum));
			List<ShmHandlingUnit> listHandlingUnit = shmHandlingUnitSubDAO.findByChildProNumberList(childProSet, entityManager);
			if(!listHandlingUnit.isEmpty()) {
			    throw ExceptionBuilder
			    .exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
			    .moreInfo(CURRENT_LOCATION, "This child PRO number cannot be used as it is already associated to a Parent PRO.")
			    .log()
			    .build();
			}
		} else if (StringUtils.isNotBlank(elevenDigitProNum) && ProNumberHelper.isBluePro(elevenDigitProNum)) {
			 throw ExceptionBuilder
			    .exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
			    .moreInfo(CURRENT_LOCATION, "Cannot upload a new shipment with the Parent PRO.")
			    .log()
			    .build();

		}
		if (StringUtils.isNotBlank(osdImage.getReportingSicCd())) {
			 try {
				 GetLocationReferenceDetailsResp resp = externalRestClient.getLocationReferenceDetails(osdImage.getReportingSicCd(), txnContext);
				 if(!(resp != null && resp.getCompanyOperations() != null && StringUtils.isNotBlank(resp.getCompanyOperations().getFreightOperationsType()) && !resp.getCompanyOperations().getFreightOperationsType().equals("T"))) {
					 throw ExceptionBuilder
					    .exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
					    .moreInfo(CURRENT_LOCATION, "The SIC code is not a valid value.")
					    .log()
					    .build(); 
				 }
	         } catch (ServiceException e) {
	        	 throw ExceptionBuilder
				    .exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
				    .moreInfo(CURRENT_LOCATION, "The SIC code is not a valid value.")
				    .log()
				    .build();
	         }
		}
	}

	private boolean isNotEligibleForUnmatch(OsdImage osdImage) {
		return osdImage.getPartialPairedWithShortInd() || osdImage.getOverPairedWithShortInd();
	}
	
	private boolean isMatchAction(OsdImage osdImage) {
		return osdImage.getOverPairedWithShortInd();
	}
	
	private boolean isPartialMatchAction(OsdImage osdImage) {
		return osdImage.getPartialPairedWithShortInd();
	}

	private boolean isEligibleToCreateAsTray(ShmOsdImage hdrEntity, Long instId) {
		return instId != null 
				&& !StringUtils.isBlank(hdrEntity.getProNbrTxt()) 
				&& !StringUtils.isBlank(hdrEntity.getOrigProNbrTxt()) 
				&& !StringUtils.isBlank(ProNumberHelper.isValidChildProNum(hdrEntity.getProNbrTxt()));
	}
	
	private boolean isEligibleToDeleteAsTray(ShmOsdImage hdrEntity, Long instId) {
		return instId != null 
				&& !StringUtils.isBlank(hdrEntity.getProNbrTxt()) 
				&& StringUtils.isBlank(hdrEntity.getOrigProNbrTxt()) 
				&& !StringUtils.isBlank(ProNumberHelper.isValidChildProNum(hdrEntity.getProNbrTxt()));
	}

	public String archiveDmsDocument(final String docClass, final String proNumber, final String content, int i,
			String token, TransactionContext txnContext, EntityManager entityManager) throws ServiceException {
		String elevenDigitProNum = ProNumberHelper.validateProNumber(proNumber, txnContext);
		if(StringUtils.isNotBlank(elevenDigitProNum) && ProNumberHelper.isBluePro(elevenDigitProNum)) {
		    //allow adding new image if this pro is already in overage or salvage. 
		    //overage will soon be all child pros
		    //salvage additional message uses parent pro for tag
		    ShmOsdImage existingOvrg = getShmOSDImage(elevenDigitProNum, txnContext, entityManager);
		    ShmSalvageRequest existingSlvg = shmSalvageRequestSubDAO.findBySalvageProNbr(ProNumberHelper.toNineDigitPro(proNumber, txnContext), entityManager);
		    if (existingOvrg == null && existingSlvg == null) {
		        throw ExceptionBuilder
		        .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
		        .moreInfo(CURRENT_LOCATION, "Cannot upload a new shipment with the Parent PRO.")
		        .log()
		        .build();
		    }
		}
		Attachment doc = Attachment.fromBase64String(handleProNum(proNumber, txnContext), "application/octet-stream",	content);
		ImageFile imageFile = new ImageFile();
		imageFile.setBase64Data(content);
        imageFile.setContentType("application/octet-stream");
        imageFile.setFileName(handleProNum(proNumber, txnContext));
        JsonAttachment<ImageFile> image = Attachment.fromObject("imageRequest", imageFile);	
		String dmsAuthToken = externalRestClient.retrieveDmsAuthToken(txnContext);
		DmsArchiveRequest dmsArchiveRqst = new DmsArchiveRequest();
		dmsArchiveRqst.setDocNumber(handleProNum(proNumber, txnContext));
		DmsIndex i1 = new DmsIndex();
		i1.setTag("PRO");
		i1.setValue(handleProNum(proNumber, txnContext));
		dmsArchiveRqst.setIndices(Arrays.asList(i1));
		JsonAttachment<DmsArchiveRequest> archiveRqst = Attachment.fromObject("archiveRequest", dmsArchiveRqst);
		ArchiveDocumentResp archiveDocument = externalRestClient.archiveOverageImageDocument(docClass, doc,archiveRqst,dmsAuthToken, txnContext);	
		return archiveDocument.getDocumentInfo().getDocArchiveTimestamp();
	}
	
	private String handleProNum(String proNumber, TransactionContext txnContext) throws ServiceException {
		try {
			return ProNumberHelper.validateProNumber(proNumber, txnContext);
		} catch (ServiceException ex) {
			throw ExceptionBuilder
			.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
			.moreInfo("proNumber", "Invalid PRO Number")
			.build();
		}
	}
	
	private ShmOsdImage getShmOSDImage(String elevenDigitProNum, final TransactionContext txnContext, 
			final EntityManager entityManager) throws ServiceException {
		if(StringUtils.isNotEmpty(elevenDigitProNum)) {
			ShmOsdImage shmOvrgImgHdr= shmOsdImageDAO.findByProNumber(elevenDigitProNum, entityManager);
			if(shmOvrgImgHdr == null) {
				if(ProNumberHelper.isYellowPro(elevenDigitProNum)) {
					shmOvrgImgHdr = shmOsdImageDAO.findByProNumber(ProNumberHelper.isValidChildProNum(elevenDigitProNum), entityManager);
				}
				if(ProNumberHelper.isBluePro(elevenDigitProNum)) {
					shmOvrgImgHdr = shmOsdImageDAO.findByProNumber(ProNumberHelper.formatProNineDigit(elevenDigitProNum), entityManager);
				}				
			}
			return shmOvrgImgHdr;
		} 
		return null;
	}
	
	private void createAsTray(final ShmOsdImage hdrEntity, final TransactionContext txnContext,
			final EntityManager entityManager) throws ServiceException {
		ShmShipment shmShipment = shmShipmentSubDAO.findByIdOrProNumber(ProNumberHelper.toElevenDigitPro(hdrEntity.getOrigProNbrTxt(), txnContext), null, entityManager);
		if(shmShipment == null) {
			throw ExceptionBuilder
			.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
			.moreInfo(CURRENT_LOCATION, "Cannot match a shipment with non-existing PRO")
			.log()
			.build();
		}
		List<String> nonLegacyProPrefixes = buildNonLegacyProPrefixUsage(txnContext);
		if(!nonLegacyProPrefixes.contains(ProNumberHelper.getProNumPrefix(hdrEntity.getOrigProNbrTxt()))) {
			throw ExceptionBuilder
			.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
			.moreInfo(CURRENT_LOCATION, "Please create a Mover PRO to move this shipment. Original PRO is Legacy PRO")
			.log()
			.build();
		}
		BulkUpsertHandlingUnitsRqst bulkUpsertHandlingUnitsRqst = new BulkUpsertHandlingUnitsRqst();
		bulkUpsertHandlingUnitsRqst.setRequestingSicCd(hdrEntity.getRptgSicCd());
		bulkUpsertHandlingUnitsRqst.setUserId(hdrEntity.getCrteBy());
		bulkUpsertHandlingUnitsRqst.setHandlingUnitShipments(getHandlingUnit(hdrEntity));
		bulkUpsertHandlingUnitsImpl.bulkUpsertHandlingUnits(bulkUpsertHandlingUnitsRqst, txnContext, entityManager);
	}
	
	private List<HandlingUnit> getHandlingUnit(final ShmOsdImage hdrEntity) {
		List<HandlingUnit> handlingUnitList = new ArrayList<HandlingUnit>();
		HandlingUnit handlingUnit = new HandlingUnit();
		handlingUnit.setChildProNbr(ProNumberHelper.isValidChildProNum(hdrEntity.getProNbrTxt()));
		handlingUnit.setParentProNbr(hdrEntity.getOrigProNbrTxt());
		handlingUnit.setHandlingMovementCd(hdrEntity.getReferenceNbrTxt());
		handlingUnit.setTypeCd(HandlingUnitTypeCd.UNKNOWN);
		handlingUnit.setWeightLbs(BasicTransformer.toDouble(hdrEntity.getWdthNbr()));
		handlingUnit.setHandlingMovementCd("ASTRAY");
		if((hdrEntity.getLenNbr() == null) && 
				hdrEntity.getHghtNbr() == null &&
				hdrEntity.getWdthNbr() == null) {
			handlingUnit.setVolumeCubicFeet(0.0);
			handlingUnit.setPupVolumePercentage(0.1);
			handlingUnit.setLengthNbr(DefaultValue);
			handlingUnit.setHeightNbr(DefaultValue);
			handlingUnit.setWidthNbr(DefaultValue);
		} else {
			if(hdrEntity.getLenNbr() == null) {
				hdrEntity.setLenNbr(new BigDecimal(DefaultValue));
			}
			if(hdrEntity.getHghtNbr() == null) {
				hdrEntity.setHghtNbr(new BigDecimal(DefaultValue));
			}
			if(hdrEntity.getWdthNbr() == null) {
				hdrEntity.setWdthNbr(new BigDecimal(DefaultValue));
			}
			BigDecimal volCft = (hdrEntity.getLenNbr().multiply(hdrEntity.getHghtNbr()).multiply( hdrEntity.getWdthNbr())).divide(new BigDecimal(1728), 2, RoundingMode.HALF_EVEN);
			BigDecimal pupVol = volCft.divide(new BigDecimal(1446), 2, RoundingMode.HALF_EVEN);
			BigDecimal pupVolValue = pupVol.signum() > 0.001 ? pupVol.multiply(new BigDecimal(100)) : new BigDecimal(0.1); 
			handlingUnit.setVolumeCubicFeet(volCft.doubleValue());
			handlingUnit.setPupVolumePercentage(pupVolValue.doubleValue());
			handlingUnit.setLengthNbr(hdrEntity.getLenNbr().doubleValue());
			handlingUnit.setHeightNbr(hdrEntity.getHghtNbr().doubleValue());
			handlingUnit.setWidthNbr(hdrEntity.getWdthNbr().doubleValue());
		}
		handlingUnitList.add(handlingUnit);
		return handlingUnitList;
	}
	
	private List<String> buildNonLegacyProPrefixUsage(TransactionContext txnContext) throws ServiceException {
		try {
        	return externalRestClient.listNonLegacyProBolPrefixMaster(txnContext)
    				.getBolPrefixMaster().stream()
    				.map(bolPrefixMaster -> bolPrefixMaster.getBolProPrefix())
    				.collect(Collectors.toList());
        } catch (final Exception e) {
            return Lists.newArrayList(); // if api call or building the list failed, assume blank list
        }
	}
	
	private void deleteAsTray(String childProNumber, EntityManager entityManager, TransactionContext txnContext) throws ValidationException, NotFoundException {
		ShmHandlingUnit shmHandlingUnit = shmHandlingUnitSubDAO.findByTrackingProNumber(childProNumber, entityManager);
		if(shmHandlingUnit != null) {
			shmHandlingUnitSubDAO.remove(shmHandlingUnit, entityManager);
			shmHandlingUnitSubDAO.deleteDB2(shmHandlingUnit.getId(), shmHandlingUnit.getLstUpdtTmst(), db2EntityManager, txnContext);
		}
	}
	
	private boolean isApprovedSalvage(ShmSalvageRequest salvageRequest) {
		return !(salvageRequest.getStatusCd().equals("RQST_PHOTO") ||
				salvageRequest.getStatusCd().equals("RQST_FEEDBACK") ||
				salvageRequest.getStatusCd().equals("SUBMITTED") ||
				salvageRequest.getStatusCd().equals("INSP_REPORT"));
	}
	
	private ShmOsdHeader createShmOsdHeaderEntry(OsdImage osdImage, EntityManager entityManager, AuditInfo auditInfo) throws ValidationException {
		ShmOsdHeader osdHeaderEntity = new ShmOsdHeader();
		shmOsdHdrSubDAO.setOsdId(osdHeaderEntity, SHM_OSD_HEADER_SEQ, entityManager);
		osdHeaderEntity.setReportingSicCd(osdImage.getReportingSicCd());
		osdHeaderEntity.setOsdNumberTxt(osdImage.getReportingSicCd()+"-"+osdHeaderEntity.getOsdId());
		osdHeaderEntity.setOsdCategoryCd("OVERAGE");
		osdHeaderEntity.setOsdDescription(osdImage.getDescription());
		osdHeaderEntity.setConeColorCd(ConeColorCdTransformer.toCode(ConeColorCd.YELLOW));
		osdHeaderEntity.setStatusCd("O_NOT_STARTED");
		osdHeaderEntity.setHuCnt(new BigDecimal(0));
		if(StringUtils.isNotEmpty(osdImage.getDmsUrl())) {
			String[] splitStrings = osdImage.getDmsUrl().split(",");
			List<String> stringList = Arrays.asList(splitStrings).stream().distinct().collect(Collectors.toList());
			osdHeaderEntity.setPhotoCnt(new BigDecimal(stringList.size()));
		}
		osdHeaderEntity.setArriveAtOsdTmst(new Timestamp(auditInfo.getCreatedTimestamp().toGregorianCalendar().getTimeInMillis()));
		osdHeaderEntity.setCrteBy(auditInfo.getCreatedById());
		osdHeaderEntity.setCrtePgmId(auditInfo.getCreateByPgmId());
		osdHeaderEntity.setCrteTmst(new Timestamp(auditInfo.getCreatedTimestamp().toGregorianCalendar().getTimeInMillis()));
		osdHeaderEntity.setLstUpdtBy(auditInfo.getUpdateById());
		osdHeaderEntity.setLstUpdtPgmId(auditInfo.getUpdateByPgmId());
		osdHeaderEntity.setLstUpdtTmst(new Timestamp(auditInfo.getUpdatedTimestamp().toGregorianCalendar().getTimeInMillis()));
		return shmOsdHdrSubDAO.save(osdHeaderEntity, entityManager);
	}

}