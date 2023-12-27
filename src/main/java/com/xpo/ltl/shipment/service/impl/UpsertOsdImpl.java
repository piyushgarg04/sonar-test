package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Quintet;

import com.google.api.client.util.Lists;
import com.google.gson.Gson;
import com.xpo.ltl.api.client.common.Attachment;
import com.xpo.ltl.api.client.common.JsonAttachment;
import com.xpo.ltl.api.documentmanagement.v1.ArchiveDocumentResp;
import com.xpo.ltl.api.documentmanagement.v1.DmsArchiveRequest;
import com.xpo.ltl.api.documentmanagement.v1.DmsIndex;
import com.xpo.ltl.api.documentmanagement.v1.ImageFile;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmMgmtRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdHeader;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdImage;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.ActionCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ConeColorCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OsdCategoryCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OsdStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OtherOsdReasonCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OverageApprovalStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.BulkUpsertHandlingUnitsRqst;
import com.xpo.ltl.api.shipment.v2.HandlingUnit;
import com.xpo.ltl.api.shipment.v2.HandlingUnitTypeCd;
import com.xpo.ltl.api.shipment.v2.IdentifiedLocationTypeCd;
import com.xpo.ltl.api.shipment.v2.ManagementRemark;
import com.xpo.ltl.api.shipment.v2.OsdCategoryCd;
import com.xpo.ltl.api.shipment.v2.OsdChildShipment;
import com.xpo.ltl.api.shipment.v2.OsdDocumentImage;
import com.xpo.ltl.api.shipment.v2.OsdHeader;
import com.xpo.ltl.api.shipment.v2.OsdImage;
import com.xpo.ltl.api.shipment.v2.OsdManagementRemark;
import com.xpo.ltl.api.shipment.v2.OsdParentShipment;
import com.xpo.ltl.api.shipment.v2.OsdStatusCd;
import com.xpo.ltl.api.shipment.v2.OtherOsdReasonCd;
import com.xpo.ltl.api.shipment.v2.OverageApprovalStatusCd;
import com.xpo.ltl.api.shipment.v2.RefusedReasonCd;
import com.xpo.ltl.api.shipment.v2.ShipmentManagementRemarkTypeCd;
import com.xpo.ltl.api.shipment.v2.UpsertOsdResp;
import com.xpo.ltl.api.shipment.v2.UpsertOsdRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShipmentManagementRemarkSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmOsdHdrSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmOsdImageSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.transformers.OsdEntityOtherCategoryTransformer;
import com.xpo.ltl.shipment.service.transformers.OsdEntityTransformer;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import com.xpo.ltl.shipment.service.validators.OsdHeaderValidator;
import com.xpo.ltl.shipment.service.validators.OsdOtherCategoryValidator;
import com.xpo.ltl.shipment.service.validators.Validator;


@RequestScoped
public class UpsertOsdImpl extends Validator {
	private static final String CURRENT_LOCATION = UpsertOsdImpl.class.getCanonicalName();
	private static final String SHM_OSD_HEADER_SEQ = "SHM_OSD_HEADER_SEQ";
	private static final String SHM_OVRG_IMG_HDR_SEQ = "SHM_OSD_IMAGE_SEQ";
	private static final String HYPEN = "-";
	private static final String CREATE_PGM_ID = "CRTEOSD";
	private static final String UPDATE_PGM_ID = "UPDTOSD";
	private static final String OVER_PAIRED_IND_DEFAULT = "N";
	private static final List<OsdCategoryCd> notOverageCategoryCds = Arrays.asList(OsdCategoryCd.DAMAGED, OsdCategoryCd.REFUSED, OsdCategoryCd.SHORT);
	private static final List<OsdStatusCd> closedStatusCds = Arrays.asList(OsdStatusCd.D_CLOSED, OsdStatusCd.O_CLOSED, OsdStatusCd.R_CLOSED, OsdStatusCd.S_CLOSED);
	private static final List<OsdStatusCd> notStartedStatusCds = Arrays.asList(OsdStatusCd.D_NOT_STARTED, OsdStatusCd.O_NOT_STARTED, OsdStatusCd.R_NOT_STARTED, OsdStatusCd.S_NOT_STARTED);
	private static final String CLOSED = "CLOSED";
	
	@Inject
	private ShmOsdHdrSubDAO shmOsdHdrSubDAO;
	
	@Inject
	private ShmShipmentSubDAO shipmentSubDAO;
	
	@Inject
	private ShmOsdImageSubDAO shmOsdImageSubDAO;
	
	@Inject
	private OsdEntityTransformer osdEntityTransformer;
	
	@Inject
	private ExternalRestClient externalRestClient;
	
	@PersistenceContext(unitName = "ltl-java-shipment-rpt-jaxrs")
	private EntityManager entityManager;
	
	@Inject
	private OsdHeaderValidator upsertOsdHeaderValidator;
	
	@Inject 
	private MaintainShipmentManagementRemarkImpl maintainShipmentManagementRemarkImpl;
	
	@Inject
	private OsdOtherCategoryValidator osdOtherCategoryValidator;
	
	@Inject
	private OsdEntityOtherCategoryTransformer osdEntityOtherCategoryTransformer;
	
	@Inject
	private ShipmentManagementRemarkSubDAO shipmentManagementRemarkDAO;
	
	@PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
	private EntityManager db2EntityManager;

	private static final Logger LOGGER = LogManager.getLogger(UpsertOsdImpl.class);

	/**
	 * Method to Upsert OSD Record
	 * @param upsertOsdHeaderRqst
	 * @param txnContext
	 * @param entityManager
	 * @return
	 * @throws ServiceException
	 * @throws DatatypeConfigurationException 
	 */
	public UpsertOsdResp upsertOsd(UpsertOsdRqst upsertOsdRqst, TransactionContext txnContext,
			EntityManager entityManager) throws ServiceException {
		checkNotNull(upsertOsdRqst, "The OSD header request is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
		
		UpsertOsdResp upsertOsdResp = new UpsertOsdResp();
		ShmOsdHeader osdHeaderEntity = new ShmOsdHeader();
		
		OsdParentShipment osdParentShipmentRqst = upsertOsdRqst.getOsdParentShipment();
		ShmOsdHeader shmOsdHeaderEntity = null;

		LOGGER.info(String.format("Request Payload for upsertOsd : %s ", new Gson().toJson(upsertOsdRqst)));

		if (upsertOsdRqst.getActionCd() == ActionCd.ADD) {
			if (Objects.nonNull(osdParentShipmentRqst) && Objects.nonNull(osdParentShipmentRqst.getOsdCategoryCd())
					&& OsdCategoryCd.OTHER == osdParentShipmentRqst.getOsdCategoryCd()) {
				return upsertOsdForOtherCategory(upsertOsdRqst, txnContext, shmOsdHeaderEntity, entityManager);
			}
		}

		if (upsertOsdRqst.getActionCd() == ActionCd.UPDATE) {
			if (upsertOsdRqst.getOsdId() == null) {
				throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
						.moreInfo(CURRENT_LOCATION, "OSD ID is required for Update").log().build();
			}

			shmOsdHeaderEntity = shmOsdHdrSubDAO.findById(upsertOsdRqst.getOsdId(), entityManager);

			if (shmOsdHeaderEntity == null) {
				throw ExceptionBuilder.exception(NotFoundErrorMessage.OSD_NOT_FOUND, txnContext)
						.moreInfo(CURRENT_LOCATION,
								String.format("OSD record not found, OsdId: %d", upsertOsdRqst.getOsdId()))
						.log().build();
			}

			if (OsdCategoryCdTransformer.toCode(OsdCategoryCd.OTHER).equalsIgnoreCase(shmOsdHeaderEntity.getOsdCategoryCd())) {
				return upsertOsdForOtherCategory(upsertOsdRqst, txnContext, shmOsdHeaderEntity, entityManager);
			}
		}
		
		List<OsdDocumentImage> osdDocumentImages = upsertOsdRqst.getOsdDocumentImages();
		if (Objects.isNull(osdDocumentImages)) {
			osdDocumentImages = new ArrayList<>();
		}
		List<MoreInfo> moreInfos = Lists.newArrayList();
		
    
		//CCS-8043: osdPiecesCount field validation
        Boolean isLegacyPro = Boolean.FALSE;
        String elevenDigitProNumber = StringUtils.EMPTY;
        if(osdParentShipmentRqst != null
        		&& osdParentShipmentRqst.getParentProNbr() != null) {
        	if(ProNumberHelper.isYellowPro(osdParentShipmentRqst.getParentProNbr())) {
				throw ExceptionBuilder
				.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
				.moreInfo(CURRENT_LOCATION, "Cannot accept a child PRO format in the parentPro field for OsdParentShipment")
				.log()
				.build();
			}
        	
	        try {
	        	osdParentShipmentRqst.setParentProNbr(ProNumberHelper.validateProNumber(osdParentShipmentRqst.getParentProNbr(), txnContext));
	        } catch (ServiceException se) {
	            addMoreInfo(moreInfos, "parentProNbr", "The PRO number entered does not have a valid format");
	        }
  
        	elevenDigitProNumber = osdParentShipmentRqst.getParentProNbr();
        	
        	List<ShmShipment> shmShipments = shipmentSubDAO
        			.findByProNumber(Collections.singletonList(elevenDigitProNumber), entityManager);
        	
        	/*CCS-8393: Check if the parentProNbr field in the request payload exists in SHM_SHIPMENT table. If yes, the create/update action should be allowed.
    		If no, the API should throw the error - “Parent PRO# does not exist.”*/
			if(CollectionUtils.isEmpty(shmShipments)){
				throw ExceptionBuilder
				.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
				.moreInfo(CURRENT_LOCATION, String.format("Parent PRO# %s does not exist.", elevenDigitProNumber))
				.log()
				.build();
			}
			
        	ShmShipment shmShipment = null;
		
        	if (CollectionUtils.isNotEmpty(shmShipments)
        			&& shmShipments.size() > 0) {
        		shmShipment = shmShipments.get(0);
        		if(Objects.isNull(osdParentShipmentRqst.getShipmentInstId())) {
        			osdParentShipmentRqst.setShipmentInstId(shmShipment.getShpInstId());
        		}
        		if (shmShipment.getShmHandlingUnits().size() == 0) {
        			isLegacyPro = Boolean.TRUE;
        		}
        	}
        }
			
		//ActionCode = ADD, Create OSD record
		if (upsertOsdRqst.getActionCd() == ActionCd.ADD) {
			createOsdRecord(upsertOsdRqst, isLegacyPro, txnContext, entityManager, upsertOsdResp, osdDocumentImages, 
					osdHeaderEntity, moreInfos);
		} 
		else if(upsertOsdRqst.getActionCd() == ActionCd.UPDATE) {
			updateOsdRecord(upsertOsdRqst, txnContext, entityManager, upsertOsdResp,
					osdDocumentImages, osdHeaderEntity, moreInfos);
		}
		else {
			throw ExceptionBuilder
					.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo(CURRENT_LOCATION, "Please provide a valid actionCd (ADD or UPDATE) to perform a create/update operation for OS&D.")
					.log()
					.build();
		}
		
		buildUpsertResponse(upsertOsdRqst, upsertOsdResp, osdHeaderEntity);
		
		LOGGER.info(String.format("Response Payload for upsertOsd : %s ", new Gson().toJson(upsertOsdResp)));
		
		return upsertOsdResp;
	}

	public void updateOsdRecord(UpsertOsdRqst upsertOsdRqst, TransactionContext txnContext,
			EntityManager entityManager, UpsertOsdResp upsertOsdResp, List<OsdDocumentImage> osdDocumentImages, 
			ShmOsdHeader osdHeaderEntity, List<MoreInfo> moreInfos)
			throws NotFoundException, ServiceException, ValidationException{
		
		AuditInfo auditInfo = AuditInfoHelper.getAuditInfoWithPgmId(UPDATE_PGM_ID, txnContext);
		
		OsdParentShipment osdParentShipmentRqst = upsertOsdRqst.getOsdParentShipment();
		
		if(upsertOsdRqst.getOsdId() == null) {
			throw ExceptionBuilder
			.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
			.moreInfo(CURRENT_LOCATION, "OSD ID is required for Update")
			.log()
			.build();
		}
		
		ShmOsdHeader shmOsdHeaderEntity = shmOsdHdrSubDAO.findById(upsertOsdRqst.getOsdId(), entityManager);
		
		if(shmOsdHeaderEntity == null){
			throw ExceptionBuilder
			.exception(NotFoundErrorMessage.OSD_NOT_FOUND, txnContext)
			.moreInfo(CURRENT_LOCATION, String.format("OSD record not found, OsdId: %d", upsertOsdRqst.getOsdId()))
			.log()
			.build();
		}
		
		upsertOsdHeaderValidator.validateOsdHeaderRqstForUpdate(upsertOsdRqst, shmOsdHeaderEntity, auditInfo, moreInfos, txnContext, entityManager);

		checkMoreInfo(txnContext, moreInfos);
		
		// For each new child PRO, a new record should be created same as mentioned before.
		// Update existing OSD Image record
		List<ShmOsdImage> shmOsdImageEntities = shmOsdImageSubDAO.findByOsdId(upsertOsdRqst.getOsdId(), entityManager);
		
		if(CollectionUtils.isEmpty(shmOsdImageEntities)){
			throw ExceptionBuilder
			.exception(NotFoundErrorMessage.OSD_NOT_FOUND, txnContext)
			.moreInfo(CURRENT_LOCATION, "OSD Image record not found")
			.log()
			.build();
		}
		
		List<OsdChildShipment> osdChildShipments = upsertOsdRqst.getOsdChildShipments();

		if (null != osdParentShipmentRqst) {
			osdChildShipments = updateOsdImagesIfDependentParentFieldsChanged(osdChildShipments, shmOsdImageEntities,
					osdParentShipmentRqst, shmOsdHeaderEntity);
		}

		OsdHeader updateOsdHeader = osdEntityTransformer.buildOsdHeaderRqst(osdParentShipmentRqst, txnContext);

		long documentImageCount = 0;
		
		String dmsUrl = StringUtils.EMPTY;
		if(CollectionUtils.isNotEmpty(osdDocumentImages)) {
			for (OsdDocumentImage osdDocumentImage : osdDocumentImages) {
				if(osdDocumentImage.getDmsUrl() != null) {
					dmsUrl += osdDocumentImage.getDmsUrl() + ",";
				}
				if(osdDocumentImage.getDocument() != null){
					//To retrieve the DMS auth Token and Archived Image in DMS
					archiveOsdImageToDms(osdDocumentImage, txnContext);
					dmsUrl += osdDocumentImage.getDmsUrl() + ",";
				}
			}
			if(!dmsUrl.isEmpty()) {
				dmsUrl = dmsUrl.substring(0, dmsUrl.length() -1);
			}
		}
		
		for(ShmOsdImage shmOsdImageEntity : CollectionUtils.emptyIfNull(shmOsdImageEntities)) {
			if(shmOsdImageEntity.getOsdCategoryCd() != null){
				if(isNotOverage(OsdCategoryCdTransformer.toEnum(shmOsdImageEntity.getOsdCategoryCd()))) {
					if(StringUtils.isBlank(shmOsdImageEntity.getProNbrTxt())) {
						String dbDmsUrl = shmOsdImageEntity.getDmsUrl();
						dmsUrl = setDmDmsUrl(dbDmsUrl, dmsUrl);
						if(StringUtils.isNotEmpty(dmsUrl)) {
							String[] splitStrings = dmsUrl.split(",");
							documentImageCount = splitStrings.length;
						}
					}
				}
				else {
					String dbDmsUrl = shmOsdImageEntity.getDmsUrl();
					dmsUrl = setDmDmsUrl(dbDmsUrl, dmsUrl);
					if(StringUtils.isNotEmpty(dmsUrl)) {
						String[] splitStrings = dmsUrl.split(",");
						documentImageCount = splitStrings.length;
					}
				} 
			}
		}
		
		setShmOsdHeaderEntityforUpdate(shmOsdHeaderEntity, updateOsdHeader, documentImageCount, txnContext);
		
		DtoTransformer.setLstUpdateAuditInfo(shmOsdHeaderEntity, auditInfo);
		
		shmOsdHdrSubDAO.save(shmOsdHeaderEntity, entityManager);
		LOGGER.info("Update Succesful for Shipment Osd Header with OSD ID : " + shmOsdHeaderEntity.getOsdId());
		entityManager.flush();

		updateOsdImages(shmOsdImageEntities, osdChildShipments, osdParentShipmentRqst, shmOsdHeaderEntity, 
				osdDocumentImages, upsertOsdRqst.getOsdId(), upsertOsdRqst.getActionCd(), txnContext, moreInfos, auditInfo, entityManager);
		
		// ALWAYS need to CREATE NEW SHM_MGMT_RMK : New note should be saved in SHM_MGMT_RMK table as mentioned before for any note sent in the API payload.
		List<OsdManagementRemark> osdManagementRemarks = upsertOsdRqst.getOsdManagementRemarks();
		if(CollectionUtils.isNotEmpty(osdManagementRemarks)) {
			createOsdMgmtRemarks(osdParentShipmentRqst, shmOsdHeaderEntity, osdManagementRemarks, txnContext, entityManager);
		}
	}

	private List<OsdChildShipment> updateOsdImagesIfDependentParentFieldsChanged(
			List<OsdChildShipment> osdChildShipments, List<ShmOsdImage> shmOsdImageEntities,
			OsdParentShipment osdParentShipmentRqst, ShmOsdHeader shmOsdHeaderEntity) {
		Set<String> rqstChildPros = new HashSet<String>();
		String[] updateFields = new String[3];
		Arrays.fill(updateFields, null);
		if(osdParentShipmentRqst.getReportingSicCd() != null
			|| osdParentShipmentRqst.getOsdCategoryCd() != null
			|| osdParentShipmentRqst.getStatusCd() != null) {
			if(osdParentShipmentRqst.getReportingSicCd() != shmOsdHeaderEntity.getReportingSicCd()){
				updateFields[0] = osdParentShipmentRqst.getReportingSicCd();
			}
			if(OsdCategoryCdTransformer.toCode(osdParentShipmentRqst.getOsdCategoryCd()) != shmOsdHeaderEntity.getOsdCategoryCd()){
				updateFields[1] = OsdCategoryCdTransformer.toCode(osdParentShipmentRqst.getOsdCategoryCd());
			}
			if(OsdStatusCdTransformer.toCode(osdParentShipmentRqst.getStatusCd()) != shmOsdHeaderEntity.getStatusCd()){
				updateFields[2] = OsdStatusCdTransformer.toCode(osdParentShipmentRqst.getStatusCd());
			}
		}
		if(updateFields[0] != null 
			|| updateFields[1] != null 
			|| updateFields[2] != null) {

			if(osdChildShipments == null){
				osdChildShipments = new ArrayList<OsdChildShipment>();
				for(ShmOsdImage img: CollectionUtils.emptyIfNull(shmOsdImageEntities)) {
					if(img.getProNbrTxt() != null) {
						OsdChildShipment osdChildShipment = new OsdChildShipment();
						osdChildShipment.setChildProNbr(img.getProNbrTxt());
						osdChildShipment.setActionCd(ActionCd.UPDATE);
						osdChildShipments.add(osdChildShipment);
					}
				}
			}
			else {
				for(OsdChildShipment child: CollectionUtils.emptyIfNull(osdChildShipments)) {
					rqstChildPros.add(child.getChildProNbr());
				}
				for(ShmOsdImage img: CollectionUtils.emptyIfNull(shmOsdImageEntities)) {
					if(!rqstChildPros.contains(img.getProNbrTxt())){
						if(img.getProNbrTxt() != null) {
							OsdChildShipment osdChildShipment = new OsdChildShipment();
							osdChildShipment.setChildProNbr(img.getProNbrTxt());
							osdChildShipment.setActionCd(ActionCd.UPDATE);
							osdChildShipments.add(osdChildShipment);
						}
					}
				}
			}
		}
		
	
		return osdChildShipments;
	}

	private void setShmOsdImageEntityforUpdate(ShmOsdImage shmOsdImageEntity, OsdImage updateOsdImage){
		
		if(updateOsdImage != null
				&& shmOsdImageEntity != null) {
			//CCS-8394: Set Approval Status code when Overage OS&D Entry is being created from HH
			shmOsdImageEntity = setApprovalStatusCd(shmOsdImageEntity, updateOsdImage);
			
			if(updateOsdImage.getBrand() != null
					&& shmOsdImageEntity.getBrand() != updateOsdImage.getBrand()){
				shmOsdImageEntity.setBrand(updateOsdImage.getBrand());
			}
			if(updateOsdImage.getComment() != null
					&& shmOsdImageEntity.getCommentTxt() != updateOsdImage.getComment()){
				shmOsdImageEntity.setCommentTxt(updateOsdImage.getComment());
			}
			if(updateOsdImage.getDescription() != null
					&& shmOsdImageEntity.getDescTxt() != updateOsdImage.getDescription()){
				shmOsdImageEntity.setDescTxt(updateOsdImage.getDescription());
			}
			if(updateOsdImage.getThumbnailImage() != null
					&& shmOsdImageEntity.getThumbnailImg() != updateOsdImage.getThumbnailImage()){
				shmOsdImageEntity.setThumbnailImg(updateOsdImage.getThumbnailImage());
			}
			if(updateOsdImage.getDmsUrl() != null
					&& shmOsdImageEntity.getDmsUrl() != updateOsdImage.getDmsUrl()){
				shmOsdImageEntity.setDmsUrl(updateOsdImage.getDmsUrl());
			}
			if(updateOsdImage.getHeightNbr() != null 
					&& shmOsdImageEntity.getHghtNbr() != (new BigDecimal(updateOsdImage.getHeightNbr()))){
				shmOsdImageEntity.setHghtNbr(new BigDecimal(updateOsdImage.getHeightNbr()));
			}
			if(updateOsdImage.getLengthNbr() != null 
					&& shmOsdImageEntity.getLenNbr() != (new BigDecimal(updateOsdImage.getLengthNbr()))){
				shmOsdImageEntity.setLenNbr(new BigDecimal(updateOsdImage.getLengthNbr()));
			}
			if(updateOsdImage.getOriginProNbr() != null
					&& shmOsdImageEntity.getOrigProNbrTxt() != updateOsdImage.getOriginProNbr()){
				shmOsdImageEntity.setOrigProNbrTxt(updateOsdImage.getOriginProNbr());
			}
			if(updateOsdImage.getOsdCategoryCd() != null
					&& shmOsdImageEntity.getOsdCategoryCd() != updateOsdImage.getOsdCategoryCd()){
				shmOsdImageEntity.setOsdCategoryCd(updateOsdImage.getOsdCategoryCd());
			}
			if(updateOsdImage.getPartialPairedWithShortInd() != null
					&& shmOsdImageEntity.getPartialPairedWithShortInd() != String.valueOf(updateOsdImage.getPartialPairedWithShortInd())){
				shmOsdImageEntity.setPartialPairedWithShortInd(String.valueOf(updateOsdImage.getPartialPairedWithShortInd()));
			}
			if(updateOsdImage.getPiecesCount() != null 
					&& shmOsdImageEntity.getPcsCnt() != (new BigDecimal(updateOsdImage.getPiecesCount()))){
				shmOsdImageEntity.setPcsCnt(new BigDecimal(updateOsdImage.getPiecesCount()));
			}
			if(updateOsdImage.getPackageCd() != null
					&& shmOsdImageEntity.getPkgCd() != updateOsdImage.getPackageCd()){
				shmOsdImageEntity.setPkgCd(updateOsdImage.getPackageCd());
			}
			if(updateOsdImage.getProNbr() != null
					&& shmOsdImageEntity.getProNbrTxt() != updateOsdImage.getProNbr()){
				shmOsdImageEntity.setProNbrTxt(updateOsdImage.getProNbr());
			}
			if(updateOsdImage.getReferenceNbr() != null
					&& shmOsdImageEntity.getReferenceNbrTxt() != updateOsdImage.getReferenceNbr()){
				shmOsdImageEntity.setReferenceNbrTxt(updateOsdImage.getReferenceNbr());
			}
			if(updateOsdImage.getSelectedTags() != null
					&& shmOsdImageEntity.getSelectedTags() != updateOsdImage.getSelectedTags()){
				shmOsdImageEntity.setSelectedTags(updateOsdImage.getSelectedTags());
			}
			if(updateOsdImage.getStatusCd() != null
					&& shmOsdImageEntity.getStatusCd() != updateOsdImage.getStatusCd()){
				shmOsdImageEntity.setStatusCd(updateOsdImage.getStatusCd());
			}
			if(updateOsdImage.getWidthNbr() != null 
					&& shmOsdImageEntity.getWdthNbr() != (new BigDecimal(updateOsdImage.getWidthNbr()))){
				shmOsdImageEntity.setWdthNbr(new BigDecimal(updateOsdImage.getWidthNbr()));
			}
			if(updateOsdImage.getWeightLbs() != null 
					&& shmOsdImageEntity.getWgtLbs() != (new BigDecimal(updateOsdImage.getWeightLbs()))){
				shmOsdImageEntity.setWgtLbs(new BigDecimal(updateOsdImage.getWeightLbs()));
			}
			
		}
	}

	private ShmOsdImage setApprovalStatusCd(ShmOsdImage shmOsdImageEntity, OsdImage updateOsdImage) {
		if((updateOsdImage.getApprovalStatusCd() != null
				&& !updateOsdImage.getApprovalStatusCd().equals(OverageApprovalStatusCd.NEEDS_APPROVAL))
				&& (shmOsdImageEntity.getApprovalStatusCd() != null
						&& shmOsdImageEntity.getApprovalStatusCd() != OverageApprovalStatusCdTransformer.toString(updateOsdImage.getApprovalStatusCd()))
				&& ((updateOsdImage.getOsdCategoryCd() != null
				&& updateOsdImage.getOsdCategoryCd().equalsIgnoreCase(OsdCategoryCdTransformer.toString(OsdCategoryCd.OVERAGE)))
				|| (shmOsdImageEntity.getOsdCategoryCd() != null    				
						&& shmOsdImageEntity.getOsdCategoryCd().equalsIgnoreCase(OsdCategoryCdTransformer.toString(OsdCategoryCd.OVERAGE))))){
			shmOsdImageEntity.setApprovalStatusCd(OverageApprovalStatusCdTransformer.toCode(updateOsdImage.getApprovalStatusCd()));
		}
		return shmOsdImageEntity;
	}

	public void updateOsdImages(List<ShmOsdImage> shmOsdImageEntities, List<OsdChildShipment> osdChildShipments,
			OsdParentShipment osdParentShipmentRqst, ShmOsdHeader shmOsdHeaderEntity, List<OsdDocumentImage> osdDocumentImages, long reqstOsdId,
			ActionCd actionCd, TransactionContext txnContext, List<MoreInfo> moreInfos, AuditInfo auditInfo, 
			EntityManager entityManager) throws ServiceException {
		
		String dmsUrl = StringUtils.EMPTY;
		String thumbnailImage = StringUtils.EMPTY;

		if(CollectionUtils.isNotEmpty(osdDocumentImages)) {
			for (OsdDocumentImage osdDocumentImage : osdDocumentImages) {
				if(osdDocumentImage.getDmsUrl() != null) {
					dmsUrl += osdDocumentImage.getDmsUrl() + ",";
				}
				if(osdDocumentImage.getThumbnailImage() != null ){
					thumbnailImage = osdDocumentImage.getThumbnailImage();
				}
				
				if(osdDocumentImage.getDocument() != null){
					//To retrieve the DMS auth Token and Archived Image in DMS
					archiveOsdImageToDms(osdDocumentImage, txnContext);
					dmsUrl += osdDocumentImage.getDmsUrl() + ",";
				}
			}
			if(!dmsUrl.isEmpty()) {
				dmsUrl = dmsUrl.substring(0, dmsUrl.length() -1);
			}
		}
		
		Set<String> dbChildProNumbers = new HashSet<String>();
		for(ShmOsdImage shmOsdImageEntity : CollectionUtils.emptyIfNull(shmOsdImageEntities)) {
			dbChildProNumbers.add(shmOsdImageEntity.getProNbrTxt());
			long headerOsdId = shmOsdImageEntity.getShmOsdHeader().getOsdId();
			if(shmOsdImageEntity.getOsdCategoryCd() != null){
				if(isNotOverage(OsdCategoryCdTransformer.toEnum(shmOsdImageEntity.getOsdCategoryCd()))) {
					if(StringUtils.isBlank(shmOsdImageEntity.getProNbrTxt())) {
						updateOsdImagesForParentPro(osdParentShipmentRqst, shmOsdImageEntity, dmsUrl, thumbnailImage, headerOsdId, txnContext, moreInfos, actionCd, auditInfo, entityManager);
					}
				}
				else {
					updateOsdImagesForOveragePros(shmOsdImageEntity, osdParentShipmentRqst, dmsUrl, thumbnailImage, reqstOsdId, actionCd, txnContext, moreInfos, auditInfo, entityManager);
				} 
			}
		}
		
		Set<String> duplicateChildShipments = new HashSet<>();
		
		if(CollectionUtils.isNotEmpty(osdChildShipments) 
			&& isNotOverage(OsdCategoryCdTransformer.toEnum(shmOsdHeaderEntity.getOsdCategoryCd()))) {
		    for(OsdChildShipment osdChildShipment : CollectionUtils.emptyIfNull(osdChildShipments)) {
		    	//CCS-9312: If we add same child pro twice in payload while creating/updating the OS&D entry then two entries are created for same child pro in OSD_IMAGE table.
		    	if (osdChildShipment != null
		    			&& duplicateChildShipments.add(osdChildShipment.getChildProNbr())) {
		    		if(osdChildShipment.getActionCd() == ActionCd.UPDATE) {
		    			boolean isChildValid = false;
		    			for(ShmOsdImage shmOsdImageEntity : CollectionUtils.emptyIfNull(shmOsdImageEntities)) {

		    				if(shmOsdImageEntity.getProNbrTxt() != null
		    						&& osdChildShipment.getChildProNbr() != null
		    						&& StringUtils.equals(shmOsdImageEntity.getProNbrTxt(),  ProNumberHelper.toElevenDigitPro(osdChildShipment.getChildProNbr(), txnContext))) {
		    					long headerOsdId = shmOsdImageEntity.getShmOsdHeader().getOsdId();
		    					isChildValid = true;
		    					updateOsdImagesForChildPros(shmOsdImageEntity, shmOsdHeaderEntity, osdChildShipment, osdParentShipmentRqst, dmsUrl, thumbnailImage, headerOsdId, txnContext, moreInfos, actionCd, auditInfo, entityManager);
		    				}
		    			}
		    			if(!isChildValid) {
		    				if(osdChildShipment.getChildProNbr() != null) {
		    					throw ExceptionBuilder
		    					.exception(NotFoundErrorMessage.PRO_NBR_NF, txnContext)
							.moreInfo(CURRENT_LOCATION, String.format("Child PRO %s does not exist for this OSD entry", osdChildShipment.getChildProNbr()))
							.log()
							.build(); 
						}
						else {
							throw ExceptionBuilder
							.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
							.moreInfo(CURRENT_LOCATION, "Please provide a childProNbr to update a child record.")
							.log()
							.build(); 
						}
		    			}
		    		}
		    		else if(osdChildShipment.getActionCd() == ActionCd.ADD
		    				&& osdChildShipment.getChildProNbr() !=null) {
		    			if(!dbChildProNumbers.contains(ProNumberHelper.toElevenDigitPro(osdChildShipment.getChildProNbr(),txnContext))) {
		    				createOsdImagesForChildPros(osdChildShipment, osdParentShipmentRqst, dmsUrl, thumbnailImage, shmOsdHeaderEntity, txnContext, moreInfos, actionCd, auditInfo, entityManager);
		    			}
		    			else {
		    				throw ExceptionBuilder
		    				.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
		    				.moreInfo(CURRENT_LOCATION, String.format("%s - existing child PRO(s) cannot be added to the OS&D Entry.", osdChildShipment.getChildProNbr()))
		    				.log()
		    				.build();
		    			}
		    		}
		    		else if(osdChildShipment.getActionCd() == ActionCd.DELETE
		    				&& osdChildShipment.getChildProNbr() != null) {
		    			if(dbChildProNumbers.contains(ProNumberHelper.toElevenDigitPro(osdChildShipment.getChildProNbr(),txnContext))) {
		    				if(notOverageCategoryCds.contains(OsdCategoryCdTransformer.toEnum(shmOsdHeaderEntity.getOsdCategoryCd()))) {
		    					deleteOsdImagesForChildPros(osdChildShipment, shmOsdImageEntities, entityManager, txnContext);
		    				}
		    				else{
		    					throw ExceptionBuilder
		    					.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
		    					.moreInfo(CURRENT_LOCATION, "Cannot delete child for overage records.")
		    					.log()
		    					.build();
		    				}
		    			}
		    			else {
		    				throw ExceptionBuilder
		    				.exception(NotFoundErrorMessage.PRO_NBR_NF, txnContext)
							.moreInfo(CURRENT_LOCATION, String.format("Child PRO %s does not exist on this OS&D entry", osdChildShipment.getChildProNbr()))
							.log()
							.build();
		    			}
		    		}
		    		else {
		    			throw ExceptionBuilder
		    			.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
		    			.moreInfo(CURRENT_LOCATION, "Please provide a valid actionCd to perform an operation on osdChildShipment(s).")
		    			.log()
		    			.build();
		    		}
		    	}
		    }
		}
	}
			
	private void deleteOsdImagesForChildPros(OsdChildShipment osdChildShipment, List<ShmOsdImage> shmOsdImageEntities, EntityManager entityManager,TransactionContext txnContext ) throws ValidationException {
		for(ShmOsdImage osdImage: CollectionUtils.emptyIfNull(shmOsdImageEntities)) {
			if(StringUtils.equals(osdImage.getProNbrTxt(), ProNumberHelper.toElevenDigitPro(osdChildShipment.getChildProNbr(),txnContext))){
				shmOsdImageSubDAO.remove(osdImage, entityManager);
				break;
			}
		}
		entityManager.flush();
	}

	private void buildUpsertResponse(UpsertOsdRqst upsertOsdRqst, UpsertOsdResp upsertOsdResp,
			ShmOsdHeader shmOsdHeaderEntity) {
		if(upsertOsdRqst.getActionCd().equals(ActionCd.ADD)) {
			upsertOsdResp.setOsdId(shmOsdHeaderEntity.getOsdId());
		}
		if(upsertOsdRqst.getActionCd().equals(ActionCd.UPDATE)) {
			upsertOsdResp.setOsdId(upsertOsdRqst.getOsdId());
		}
		if(shmOsdHeaderEntity.getProNbrTxt() !=null ) {
			upsertOsdResp.setProNbr(shmOsdHeaderEntity.getProNbrTxt());
		}
		upsertOsdResp.setActionCd(ActionCdTransformer.toCode(upsertOsdRqst.getActionCd()));
		if(shmOsdHeaderEntity.getReportingSicCd() != null) {
			upsertOsdResp.setReportingSicCd(shmOsdHeaderEntity.getReportingSicCd());
		}
		if(shmOsdHeaderEntity.getOsdCategoryCd() != null) {
			upsertOsdResp.setOsdCategoryCd(OsdCategoryCdTransformer.toEnum(shmOsdHeaderEntity.getOsdCategoryCd()));
		}
	}

	public void createOsdRecord(UpsertOsdRqst upsertOsdRqst, Boolean isLegacyPro, TransactionContext txnContext,
			EntityManager entityManager, UpsertOsdResp upsertOsdResp, List<OsdDocumentImage> osdDocumentImages, 
			ShmOsdHeader osdHeaderEntity, List<MoreInfo> moreInfos)
			throws ValidationException, ServiceException, NotFoundException {
		
		String elevenDigitProNumber = StringUtils.EMPTY;
		String dmsUrl = StringUtils.EMPTY;
		String thumbnailImage = StringUtils.EMPTY;
		
		OsdParentShipment osdParentShipmentRqst = upsertOsdRqst.getOsdParentShipment();
		
		AuditInfo auditInfo;
		if(upsertOsdRqst.getAuditInfo() != null){
			//pre-defined auditInfo is sent for Shortage workflow derived from SHM_MVMT_EXCEPTION
			auditInfo = upsertOsdRqst.getAuditInfo();
		}
		else{
			auditInfo = AuditInfoHelper.getAuditInfoWithPgmId(CREATE_PGM_ID, txnContext);
		}

		if(null != osdParentShipmentRqst.getStatusCd()
			&& !notStartedStatusCds.contains(osdParentShipmentRqst.getStatusCd())){
			throw ExceptionBuilder
				.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
				.moreInfo(CURRENT_LOCATION, "Invalid OsdStatusCd provided for create OS&D operation.(Please remove this or provide a *not_started status.")
				.log()
				.build();
		}
		else if(null == osdParentShipmentRqst.getStatusCd()){
			if(osdParentShipmentRqst.getOsdCategoryCd() == OsdCategoryCd.DAMAGED){
				osdParentShipmentRqst.setStatusCd(OsdStatusCd.D_NOT_STARTED);
			}
			else if(osdParentShipmentRqst.getOsdCategoryCd() == OsdCategoryCd.REFUSED){
				osdParentShipmentRqst.setStatusCd(OsdStatusCd.R_NOT_STARTED);
			}
			else if(osdParentShipmentRqst.getOsdCategoryCd() == OsdCategoryCd.OVERAGE){
				osdParentShipmentRqst.setStatusCd(OsdStatusCd.O_NOT_STARTED);
			}
			else if(osdParentShipmentRqst.getOsdCategoryCd() == OsdCategoryCd.SHORT){
				osdParentShipmentRqst.setStatusCd(OsdStatusCd.S_NOT_STARTED);
			}
		}
		
		upsertOsdHeaderValidator.validateOsdHeaderRqstForCreate(upsertOsdRqst, isLegacyPro, auditInfo, moreInfos, txnContext, entityManager);
		checkMoreInfo(txnContext, moreInfos);
		
		if(CollectionUtils.isNotEmpty(osdDocumentImages)) {
			for (OsdDocumentImage osdDocumentImage : osdDocumentImages) {
				if(osdDocumentImage.getDmsUrl() != null) {
					dmsUrl += osdDocumentImage.getDmsUrl() + ",";
				}
				if(osdDocumentImage.getThumbnailImage() != null){
					thumbnailImage = osdDocumentImage.getThumbnailImage();
				}
				
				if(osdDocumentImage.getDocument() != null){
					//To retrieve the DMS auth Token and Archived Image in DMS
					archiveOsdImageToDms(osdDocumentImage, txnContext);
					dmsUrl = osdDocumentImage.getDmsUrl();
				}
			}
			if(!dmsUrl.isEmpty()) {
				dmsUrl = dmsUrl.substring(0, dmsUrl.length() -1);
			}
		}

		if(osdParentShipmentRqst.getParentProNbr() != null) {
			elevenDigitProNumber = ProNumberHelper.toElevenDigitPro(osdParentShipmentRqst.getParentProNbr(), txnContext);
			osdParentShipmentRqst.setParentProNbr(elevenDigitProNumber);
		}

		// Build Osd Header Request 
		OsdHeader osdHeader = osdEntityTransformer.buildOsdHeaderRqst(osdParentShipmentRqst, txnContext);
		
		if (osdParentShipmentRqst.getOsdPiecesCount() != null) {
			osdHeader.setHandlingUnitCount(osdParentShipmentRqst.getOsdPiecesCount());
		}else if(!isLegacyPro) {
			osdHeader.setHandlingUnitCount(BigInteger.valueOf(1L));
		}

		/*ARRIVE_AT_OSD_TMST - Send as Timestamp*/
		//CCS-8061: Remove the validation for arriveAtOsdTmst field to be mandatory & default the value to current system timestamp 
		if (osdParentShipmentRqst.getArriveAtOsdDateTime() != null) {
			osdHeader.setArriveAtOsdDateTime(osdParentShipmentRqst.getArriveAtOsdDateTime());
		} else {
			osdHeader.setArriveAtOsdDateTime(auditInfo.getCreatedTimestamp());
		}

		if (osdParentShipmentRqst.getOsdCategoryCd().equals(OsdCategoryCd.OVERAGE)) {
			if(osdParentShipmentRqst.getIdentifiedLocationTypeCd().equals(IdentifiedLocationTypeCd.TRAILER)
				&& osdParentShipmentRqst.getIdentifiedLocationId() != null) {
					XMLGregorianCalendar dateTime = osdHeader.getArriveAtOsdDateTime();
					String[] trailerTokens = osdParentShipmentRqst.getIdentifiedLocationId().split("-");
					osdHeader.setLastSicCd(shmOsdHdrSubDAO.getLastCloseSic(trailerTokens , dateTime, entityManager));
				}
		}

		if(org.apache.commons.lang.StringUtils.isNotEmpty(dmsUrl)) {

			dmsUrl = cleanDuplicates(dmsUrl);

			String[] items = dmsUrl.split(",");
			int photoCnt = items.length;
			osdHeader.setPhotoCount((short) photoCnt);
		}else {
			osdHeader.setPhotoCount((short) 0);
		}
		
		if (osdParentShipmentRqst.getOsdCategoryCd() != null) {
			if (osdParentShipmentRqst.getOsdCategoryCd().equals(OsdCategoryCd.REFUSED)
					|| osdParentShipmentRqst.getOsdCategoryCd().equals(OsdCategoryCd.DAMAGED)
					|| osdParentShipmentRqst.getOsdCategoryCd().equals(OsdCategoryCd.OVERAGE)) {
				if (osdHeader.getPhotoCount() < 0) {
					addMoreInfo(moreInfos, "osdDocumentImages",
							"At least 1 photos is required for Refused, Damaged, Overage");
				}

				ShmOsdHeader shmOsdHeaderForConeColorValidation = shmOsdHdrSubDAO.getByConeAndSicDetails(
						ConeColorCdTransformer.toCode(osdParentShipmentRqst.getConeColorCd()),
						osdParentShipmentRqst.getConeNbr(), osdParentShipmentRqst.getReportingSicCd(), entityManager);

				if (Objects.nonNull(shmOsdHeaderForConeColorValidation)) {
					throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
							.moreInfo("coneColorCd & coneNbr",
									String.format("Cone %s %s is already in use.",
											osdParentShipmentRqst.getConeColorCd(), osdParentShipmentRqst.getConeNbr()))
							.log().build();
				}

			}
		}

		/*REFUSED_REASON_CD: VARCHAR 20 Enum Values
		 * Only Required for Refused Shipments*/
		if(osdParentShipmentRqst.getOsdCategoryCd() != null
				&& osdParentShipmentRqst.getRefusedReasonCd() != null) {
			if (osdParentShipmentRqst.getOsdCategoryCd().equals(OsdCategoryCd.REFUSED)) 
			{
				osdParentShipmentRqst.setRefusedReasonCd(osdParentShipmentRqst.getRefusedReasonCd());
			} else {
				osdParentShipmentRqst.setRefusedReasonCd(RefusedReasonCd.OTHER);
			}
		}
		
		DtoTransformer.setAuditInfo(osdHeaderEntity, auditInfo);
		
		DtoTransformer.toShmOsdHeader(osdHeader, osdHeaderEntity);
		
		if(elevenDigitProNumber != null) {
			osdHeaderEntity.setProNbrTxt(elevenDigitProNumber);
		}
         
		/*	OSD_ID - automatically created when a record is entered */
		shmOsdHdrSubDAO.setOsdId(osdHeaderEntity, SHM_OSD_HEADER_SEQ, entityManager);

		if(osdHeader.getHandlingUnitCount() != null) {
			osdHeaderEntity.setHuCnt(new BigDecimal(osdHeader.getHandlingUnitCount()));
		}

		/*OSD_NUMBER_TXT - REPORTING SIC_CD - OSD_ID*/
		osdHeaderEntity.setOsdNumberTxt(getOsdNumber(osdHeaderEntity.getOsdId(), osdParentShipmentRqst.getReportingSicCd()));
		
		// Save SHM OSD Header
		shmOsdHdrSubDAO.save(osdHeaderEntity, entityManager);
		LOGGER.info("Save Succesful for Shipment Osd Header with PRO # " + osdHeaderEntity.getProNbrTxt());
		entityManager.flush();
         
		/*Create OSD Image Records for Parent Pro and Child Pros*/		
		List<OsdChildShipment> osdChildShipments = upsertOsdRqst.getOsdChildShipments();
		
		createOsdImages(osdChildShipments, osdParentShipmentRqst, dmsUrl, thumbnailImage, osdHeaderEntity, null, upsertOsdRqst.getActionCd(),
				txnContext, moreInfos, auditInfo, entityManager);
		
		List<OsdManagementRemark> osdManagementRemarks = upsertOsdRqst.getOsdManagementRemarks();
		if(CollectionUtils.isNotEmpty(osdManagementRemarks)) {
			createOsdMgmtRemarks(osdParentShipmentRqst, osdHeaderEntity, osdManagementRemarks, txnContext, entityManager);
		}

		buildUpsertResponse(upsertOsdRqst, upsertOsdResp, osdHeaderEntity);
	}

	private String cleanDuplicates(String duplicatedStrings) {

		String[] splitStrings = duplicatedStrings.split(",");

		List<String> stringList = Arrays.asList(splitStrings).stream().distinct().collect(Collectors.toList());

		return String.join(",", stringList);

	}

	private Boolean isNotOverage(OsdCategoryCd osdCategoryCd) {
		return (notOverageCategoryCds.contains(osdCategoryCd));
	}
	
	public ManagementRemark buildMgmtRemarkRqst(OsdParentShipment osdParentShipmentRqst, 
			OsdManagementRemark osdManagementRemark,
			TransactionContext txnContext) {
		
		ManagementRemark mgmtRemarkRqst = new ManagementRemark(); 
		if(osdParentShipmentRqst.getOsdId() != null) {
			mgmtRemarkRqst.setParentOsdId(osdParentShipmentRqst.getOsdId());
		}
		mgmtRemarkRqst.setMovementExceptionSequenceNbr(BigInteger.ZERO);
		mgmtRemarkRqst.setMovementSequenceNbr(BigInteger.ZERO);
		if(osdManagementRemark.getRemark() != null) {
			mgmtRemarkRqst.setRemark(osdManagementRemark.getRemark());
		}
		mgmtRemarkRqst.setTypeCd(ShipmentManagementRemarkTypeCd.OSD_REMARKS);
				
		return mgmtRemarkRqst;
	}

	/**
	 * Method to upsert the Osd Image record for Parent and Child Pro
	 * @param osdChildShipments
	 * @param osdParentShipmentRqst
	 * @param dmsUrl
	 * @param txnContext
	 * @param moreInfos
	 * @param auditInfo
	 * @param entityManager
	 * @throws ServiceException
	 */
	public void createOsdImages(List<OsdChildShipment> osdChildShipments, OsdParentShipment osdParentShipmentRqst, String dmsUrl, String thumbnailImage,
			ShmOsdHeader osdHeaderEntity, Long rqstOsdId, ActionCd actionCd, TransactionContext txnContext, List<MoreInfo> moreInfos, AuditInfo auditInfo,
			EntityManager entityManager) throws ServiceException {
	
		Set<String> duplicateChildShipments = new HashSet<>();
		
		if(isNotOverage(osdParentShipmentRqst.getOsdCategoryCd())) {
			for(OsdChildShipment osdChildShipment : CollectionUtils.emptyIfNull(osdChildShipments)) {
				//CCS-9312: If we add same child pro twice in payload while creating/updating the OS&D entry then two entries are created for same child pro in OSD_IMAGE table.
				if (osdChildShipment != null
		    			&& duplicateChildShipments.add(osdChildShipment.getChildProNbr())) {
		    		createOsdImagesForChildPros(osdChildShipment, osdParentShipmentRqst, dmsUrl, thumbnailImage, osdHeaderEntity, txnContext, moreInfos, actionCd, auditInfo, entityManager);
		    	}
			}
		}

		if(osdParentShipmentRqst.getOsdCategoryCd() != null) {
			if(osdParentShipmentRqst.getOsdCategoryCd().equals(OsdCategoryCd.OVERAGE)){
				createOsdImagesForOveragePros(osdParentShipmentRqst, dmsUrl, thumbnailImage, osdHeaderEntity, null, actionCd, txnContext, moreInfos, auditInfo, entityManager);
			} 
		}

		if(osdParentShipmentRqst.getOsdCategoryCd() != null) {
			if(isNotOverage(osdParentShipmentRqst.getOsdCategoryCd())){
				createOsdImagesForParentPro(osdParentShipmentRqst, dmsUrl, thumbnailImage, osdHeaderEntity, txnContext, moreInfos, actionCd, auditInfo, entityManager);
			}
		}
	}

	/**
	 * Method to get OSD Number txt
	 * @param osdId
	 * @param reportingSicCd
	 * @return
	 */
	private String getOsdNumber(Long osdId, String reportingSicCd) {
		String osdNumberTxt = null;
		
		if(reportingSicCd != null && osdId != null) {
			osdNumberTxt = reportingSicCd.concat(HYPEN).concat(osdId.toString());
		}
		return osdNumberTxt;
	}

	/**
	 * Method to Retrieve DMS token and Archive the Osd Image to DMS
	 * @param osdDocumentImage
	 * @param osdImageList
	 * @param txnContext
	 * @throws ServiceException
	 */
	public void archiveOsdImageToDms(OsdDocumentImage osdDocumentImage, TransactionContext txnContext) throws ServiceException {
		try {
			List<String> docArchiveTimestamp = new ArrayList<String>();
					String dmsAuthToken = externalRestClient.retrieveDmsAuthToken(txnContext);
					String encodedString = osdDocumentImage.getDocument();
					docArchiveTimestamp.add(archiveDmsDocument("OSDF",
							osdDocumentImage.getIndices().get(0).getValue(),
							encodedString, 0,dmsAuthToken,
							txnContext));
				String dmsUrl = String.join(",", docArchiveTimestamp);
				osdDocumentImage.setDmsUrl(dmsUrl);
		} catch (final ServiceException e) {
			LOGGER.error("Service Exception during Image Archive : " + ExceptionUtils.getFullStackTrace(e));
			throw e;
		} catch (final RuntimeException e) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
			.contextValues(String.format("Run time  Exception during Image Archive :")).log(e).build();
		}		
	}

	//Archive Image document to DMS 
	/**
	 * Method to archieve OSD Image to DMS
	 * @param docClass
	 * @param proNumber
	 * @param content
	 * @param i
	 * @param token
	 * @param txnContext
	 * @return
	 * @throws ServiceException
	 */
	public String archiveDmsDocument(final String docClass, final String proNumber, final String content, int i,
			String token, TransactionContext txnContext) throws ServiceException {
		Attachment doc = Attachment.fromBase64String(handleProNum(proNumber, txnContext), "application/octet-stream",   content);
		ImageFile imageFile = new ImageFile();
		imageFile.setBase64Data(content);
	    imageFile.setContentType("application/octet-stream");
	    imageFile.setFileName(handleProNum(proNumber, txnContext));
	    JsonAttachment<ImageFile> image = Attachment.fromObject("imageRequest", imageFile); 
	    DmsArchiveRequest dmsArchiveRqst = new DmsArchiveRequest();
	    dmsArchiveRqst.setDocNumber(handleProNum(proNumber, txnContext));
	    DmsIndex i1 = new DmsIndex();
	    i1.setTag("PRO");
	    i1.setValue(handleProNum(proNumber, txnContext));
	    dmsArchiveRqst.setIndices(Arrays.asList(i1));
	    JsonAttachment<DmsArchiveRequest> archiveRqst = Attachment.fromObject("archiveRequest", dmsArchiveRqst);
	    ArchiveDocumentResp archiveDocument = externalRestClient.archiveOverageImageDocument(docClass, doc,archiveRqst,token, txnContext);  
	    return archiveDocument.getDocumentInfo().getDocArchiveTimestamp();
	}

	//SET OSD Header details
	/**
	 * Method to create the Osd Images for Child Pros
	 * @param osdChildShipments
	 * @param osdParentShipment
	 * @param dmsUrl
	 * @param txnContext
	 * @param moreInfos
	 * @param auditInfo
	 * @param entityManager
	 * @throws ServiceException
	 */
	public void createOsdImagesForChildPros(OsdChildShipment osdChildShipment, OsdParentShipment osdParentShipment, String dmsUrl,
			String thumbnailImage, ShmOsdHeader osdHeaderEntity, TransactionContext txnContext, List<MoreInfo> moreInfos, ActionCd actionCd,
			AuditInfo auditInfo, EntityManager entityManager) throws ServiceException {
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");

		if(osdChildShipment.getActionCd() == ActionCd.ADD) {
			//Create osd Image Request for Child Pros
			OsdImage osdImageRqst = osdEntityTransformer.buildImageRqstForChildPros(osdChildShipment, osdHeaderEntity, osdParentShipment, auditInfo, txnContext);
			
			//upsertOsdHeaderValidator.validateChildProAlreadyExist(osdChildShipment.getChildProNbr(), txnContext, entityManager);
			// Perform Validation for Osd Image Request for Upsert
			upsertOsdHeaderValidator.validateOsdImageRqstForUpsert(osdImageRqst, osdParentShipment, auditInfo, moreInfos, actionCd, txnContext, entityManager);
			checkMoreInfo(txnContext, moreInfos);
			
			// Save Osd Image record for Child Pro
			saveOsdImage(osdImageRqst, osdHeaderEntity, entityManager, auditInfo);
		}
		else {
			throw ExceptionBuilder
			.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
			.moreInfo(CURRENT_LOCATION, "Please provide a valid actionCd to perform an operation on osdChildShipment(s), ADD while creating a new OS&D entry.")
			.log()
			.build();
		}
	}

	public void createOsdImagesForOveragePros(OsdParentShipment osdParentShipment, String dmsUrl, String thumbnailImage,
			ShmOsdHeader shmOsdHeaderEntity, Long rqstOsdId, ActionCd actionCd, TransactionContext txnContext, List<MoreInfo> moreInfos,
			AuditInfo auditInfo, EntityManager entityManager) throws ServiceException {
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
		
		//Create osd Image Request for Overage Pros
		OsdImage osdImageRqst = osdEntityTransformer.buildImageRqstForOveragePros(osdParentShipment, null, dmsUrl, thumbnailImage, actionCd, auditInfo, txnContext);
	
		// Perform Validation for Osd Image Request for Upsert
		if(osdParentShipment != null) {
			upsertOsdHeaderValidator.validateChildProAlreadyExist(osdParentShipment, null, null, actionCd, txnContext, entityManager);	
		}
		upsertOsdHeaderValidator.validateOsdImageRqstForUpsert(osdImageRqst, osdParentShipment, auditInfo, moreInfos, actionCd, txnContext, entityManager);
		checkMoreInfo(txnContext, moreInfos);
			
		// Save Osd Image record for Overage Pro
		saveOsdImage(osdImageRqst, shmOsdHeaderEntity, entityManager, auditInfo);
	}

	public void updateOsdImagesForOveragePros(ShmOsdImage shmOsdImageEntity, OsdParentShipment osdParentShipment, String dmsUrl, 
			String thumbnailImage, long rqstOsdId, ActionCd actionCd, TransactionContext txnContext, List<MoreInfo> moreInfos,
			AuditInfo auditInfo, EntityManager entityManager) throws ServiceException {
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
		
		String dbDmsUrl = shmOsdImageEntity.getDmsUrl();
		dmsUrl = setDmDmsUrl(dbDmsUrl, dmsUrl);
		
		//Create osd Image Request for Overage Pros
		OsdImage osdImageRqst = osdEntityTransformer.buildImageRqstForOveragePros(osdParentShipment, shmOsdImageEntity, dmsUrl, thumbnailImage, actionCd, auditInfo, txnContext);
	
		// Perform Validation for Osd Image Request for Upsert
		if(osdParentShipment != null
				&& !closedStatusCds.contains(osdParentShipment.getStatusCd())) {
			upsertOsdHeaderValidator.validateChildProAlreadyExist(osdParentShipment, shmOsdImageEntity, rqstOsdId, actionCd, txnContext, entityManager);	
		}
		upsertOsdHeaderValidator.validateOsdImageRqstForUpsert(osdImageRqst, osdParentShipment, auditInfo, moreInfos, actionCd, txnContext, entityManager);
		checkMoreInfo(txnContext, moreInfos);
			
		// Update  Osd Image record for Overage Pro
		updateOsdImage(shmOsdImageEntity, osdImageRqst, entityManager, auditInfo);
	}
	
	public void saveOsdImage(OsdImage osdImageRqst, ShmOsdHeader osdHeaderEntity, EntityManager entityManager, AuditInfo auditInfo) throws ServiceException {
	
		ShmOsdImage osdImageEntity = new ShmOsdImage();
		
		DtoTransformer.setAuditInfo(osdImageEntity, auditInfo);
		osdImageRqst.setOverPairedWithShortInd(false);
		DtoTransformer.toShmOsdImage(osdImageRqst, osdImageEntity);
		shmOsdImageSubDAO.setInstId(osdImageEntity, SHM_OVRG_IMG_HDR_SEQ, entityManager);
		osdImageEntity.setOverPairedWithShortInd(OVER_PAIRED_IND_DEFAULT);
		osdImageEntity.setCrteBy(auditInfo.getCreatedById());
		osdImageEntity.setCrtePgmId(auditInfo.getCreateByPgmId());
		osdImageEntity.setCrteTmst(new Timestamp(auditInfo.getCreatedTimestamp().toGregorianCalendar().getTimeInMillis()));
		osdImageEntity.setLstUpdtBy(auditInfo.getUpdateById());
		osdImageEntity.setLstUpdtPgmId(auditInfo.getUpdateByPgmId());
		osdImageEntity.setLstUpdtTmst(new Timestamp(auditInfo.getUpdatedTimestamp().toGregorianCalendar().getTimeInMillis()));
		osdImageEntity.setPcsCnt(osdHeaderEntity.getHuCnt());
		osdImageEntity.setShmOsdHeader(osdHeaderEntity);
		
		//SAVE - OSD Image details to SHM_OSD_IMAGE Table
		osdImageEntity = shmOsdImageSubDAO.save(osdImageEntity, entityManager);
		LOGGER.info("Save Succesful OSD Image Record with PRO # " + osdImageEntity.getProNbrTxt());
		entityManager.flush();
	}
	
	public void updateOsdImage(ShmOsdImage osdImageEntity, OsdImage osdImageRqst, EntityManager entityManager, AuditInfo auditInfo) throws ServiceException {
		
		DtoTransformer.setLstUpdateAuditInfo(osdImageEntity, auditInfo);
		setShmOsdImageEntityforUpdate(osdImageEntity, osdImageRqst);
				
		//UPDATE - OSD Image details to SHM_OSD_IMAGE Table
		shmOsdImageSubDAO.save(osdImageEntity, entityManager);
		LOGGER.info("Update Succesful OSD Image Record with PRO # " + osdImageEntity.getProNbrTxt());
		entityManager.flush();
	}
	
	public void createOsdImagesForParentPro(OsdParentShipment osdParentShipment, String dmsUrl, String thumbnailImage, 
			ShmOsdHeader shmOsdHeaderEntity,TransactionContext txnContext, List<MoreInfo> moreInfos, ActionCd actionCd,
			AuditInfo auditInfo, EntityManager entityManager) throws ServiceException {
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
		
		//Create osd Image Request for Parent Pros
		OsdImage osdImageRqst = osdEntityTransformer.buildImageRqstForParentPros(osdParentShipment, dmsUrl, thumbnailImage, auditInfo, txnContext);
		
		// Validation for Osd Image Request for Upsert
		upsertOsdHeaderValidator.validateOsdImageRqstForUpsert(osdImageRqst, osdParentShipment, auditInfo, moreInfos, actionCd, txnContext, entityManager);
			
		// Save Osd Image record for Child Pro
		saveOsdImage(osdImageRqst, shmOsdHeaderEntity, entityManager, auditInfo);
	}
	
	public void updateOsdImagesForParentPro(OsdParentShipment osdParentShipment, ShmOsdImage shmOsdImage, String dmsUrl, 
			String thumbnailImage, Long headerOsdId,TransactionContext txnContext, List<MoreInfo> moreInfos, ActionCd actionCd,
			AuditInfo auditInfo, EntityManager entityManager) throws ServiceException {
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
		
		String dbDmsUrl = shmOsdImage.getDmsUrl();
		dmsUrl = setDmDmsUrl(dbDmsUrl, dmsUrl);
				
		//Create osd Image Request for Parent Pros
		OsdImage osdImageRqst = osdEntityTransformer.buildImageRqstForParentPros(osdParentShipment, dmsUrl, thumbnailImage, auditInfo, txnContext);
		
		// Validation for Osd Image Request for Upsert
		upsertOsdHeaderValidator.validateOsdImageRqstForUpsert(osdImageRqst, osdParentShipment, auditInfo, moreInfos, actionCd, txnContext, entityManager);
			
		// Update Osd Image record for Child Pro
		updateOsdImage(shmOsdImage, osdImageRqst, entityManager, auditInfo);
	}
	
	private String setDmDmsUrl(String dbDmsUrl, String dmsUrl) {
		if(dbDmsUrl != null
				&& dmsUrl != null) {
			dmsUrl = dbDmsUrl.concat(",").concat(dmsUrl);
		}
		return cleanDuplicates(dmsUrl);
	}

	public void updateOsdImagesForChildPros(ShmOsdImage shmOsdImageEntity, ShmOsdHeader osdHeaderEntity, OsdChildShipment osdChildShipment, OsdParentShipment osdParentShipment,
			String dmsUrl, String thumbnailImage, Long headerOsdId, TransactionContext txnContext, List<MoreInfo> moreInfos, ActionCd actionCd,
			AuditInfo auditInfo, EntityManager entityManager) throws ServiceException {
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
		
			//Create osd Image Request for Child Pros
			OsdImage osdImageRqst = osdEntityTransformer.buildImageRqstForChildPros(osdChildShipment, osdHeaderEntity, osdParentShipment, auditInfo, txnContext);
			
			// Perform Validation for Osd Image Request for Upsert
			upsertOsdHeaderValidator.validateOsdImageRqstForUpsert(osdImageRqst, osdParentShipment, auditInfo, moreInfos, actionCd, txnContext, entityManager);
			checkMoreInfo(txnContext, moreInfos);
			
			// Update Osd Image record for Child Pro
			updateOsdImage(shmOsdImageEntity, osdImageRqst, entityManager, auditInfo);
	}

	private String handleProNum(String proNumber, TransactionContext txnContext) throws ServiceException {
		try {
			String elevenDigitProNum = ProNumberHelper.validateProNumber(proNumber, txnContext);
			if(elevenDigitProNum != null) {
				if(ProNumberHelper.isYellowPro(elevenDigitProNum)) {
					return ProNumberHelper.isValidChildProNum(elevenDigitProNum);
				}
				if(ProNumberHelper.isBluePro(elevenDigitProNum)) {
					return ProNumberHelper.formatProNineDigit(elevenDigitProNum);
				}
			}
		} catch (ServiceException ex) {
			throw ExceptionBuilder
			.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
			.moreInfo("proNumber", "Invalid PRO Number")
			.build();
		}
		return null;
	}
	
	private void setShmOsdHeaderEntityforUpdate(ShmOsdHeader shmOsdHeaderEntity, OsdHeader updateOsdHeader, long documentImageCount,TransactionContext txnContext) throws ServiceException {
		if(updateOsdHeader != null) {
			if(updateOsdHeader.getArriveAtOsdDateTime() != null 
					&& shmOsdHeaderEntity.getArriveAtOsdTmst() != 
					(new Timestamp(updateOsdHeader.getArriveAtOsdDateTime().toGregorianCalendar().getTimeInMillis()))) {
				shmOsdHeaderEntity.setArriveAtOsdTmst(new Timestamp(updateOsdHeader.getArriveAtOsdDateTime().toGregorianCalendar().getTimeInMillis()));
			}
			if(updateOsdHeader.getAssignedUser() != null 
					&& shmOsdHeaderEntity.getAssignedUser() != updateOsdHeader.getAssignedUser()) {
				shmOsdHeaderEntity.setAssignedUser(updateOsdHeader.getAssignedUser());
			}
			if(updateOsdHeader.getCloseReasonCd() != null 
					&& shmOsdHeaderEntity.getCloseReasonCd() != updateOsdHeader.getCloseReasonCd()) {
				shmOsdHeaderEntity.setCloseReasonCd(updateOsdHeader.getCloseReasonCd());
			}
			if(updateOsdHeader.getConeColorCd() != null  
					&& shmOsdHeaderEntity.getConeColorCd() != updateOsdHeader.getConeColorCd()) {
				shmOsdHeaderEntity.setConeColorCd(updateOsdHeader.getConeColorCd());
			}
			if(updateOsdHeader.getConeNbr() != null
					&& shmOsdHeaderEntity.getConeNbr() != (new BigDecimal(updateOsdHeader.getConeNbr()))) {
				shmOsdHeaderEntity.setConeNbr(new BigDecimal(updateOsdHeader.getConeNbr()));
			}
			if(updateOsdHeader.getCorrelationId() != null 
					&& shmOsdHeaderEntity.getCorrelationId() != updateOsdHeader.getCorrelationId()) {
				shmOsdHeaderEntity.setCorrelationId(updateOsdHeader.getCorrelationId());
			}
			if(updateOsdHeader.getDockWorkerUserId() != null 
					&& shmOsdHeaderEntity.getDockWorkerUserid() != updateOsdHeader.getDockWorkerUserId()) {
				shmOsdHeaderEntity.setDockWorkerUserid(updateOsdHeader.getDockWorkerUserId());
			}
			if(updateOsdHeader.getHandlingUnitCount() != null 
					&& shmOsdHeaderEntity.getHuCnt() != (new BigDecimal (updateOsdHeader.getHandlingUnitCount()))) {
				shmOsdHeaderEntity.setHuCnt(new BigDecimal(updateOsdHeader.getHandlingUnitCount()));
			}
			if(updateOsdHeader.getIdentifiedLocationId() != null 
					&& shmOsdHeaderEntity.getIdentifiedLocId() != updateOsdHeader.getIdentifiedLocationId()) {
				shmOsdHeaderEntity.setIdentifiedLocId(updateOsdHeader.getIdentifiedLocationId());
			}
			if(updateOsdHeader.getIdentifiedLocationTypeCd() != null 
					&& shmOsdHeaderEntity.getIdentifiedLocTypeCd() != updateOsdHeader.getIdentifiedLocationTypeCd()) {
				shmOsdHeaderEntity.setIdentifiedLocTypeCd(updateOsdHeader.getIdentifiedLocationTypeCd());
			}
			
			//CCS-8067: When Updating OSDCategoryType from Damaged to Refused, pass the ReasonCd value as blank into the SHM_OSD_HDR table
			if(shmOsdHeaderEntity.getRefusedReasonCd() != null
				&& OsdCategoryCdTransformer.toEnum(shmOsdHeaderEntity.getOsdCategoryCd()) == OsdCategoryCd.REFUSED
				&& OsdCategoryCdTransformer.toEnum(updateOsdHeader.getOsdCategoryCd()) == OsdCategoryCd.DAMAGED){
				shmOsdHeaderEntity.setRefusedReasonCd(null);
			}
			else if(updateOsdHeader.getRefusedReasonCd() != null
					&& shmOsdHeaderEntity.getRefusedReasonCd() != updateOsdHeader.getRefusedReasonCd()) {
				shmOsdHeaderEntity.setRefusedReasonCd(updateOsdHeader.getRefusedReasonCd());
			}

			if(updateOsdHeader.getOsdCategoryCd() != null
					&& shmOsdHeaderEntity.getOsdCategoryCd() != updateOsdHeader.getOsdCategoryCd()) {
				shmOsdHeaderEntity.setOsdCategoryCd(updateOsdHeader.getOsdCategoryCd());
			}
			if(updateOsdHeader.getOsdDayCount() != null
					&& shmOsdHeaderEntity.getOsdDayCnt() != (new BigDecimal(updateOsdHeader.getOsdDayCount()))) {
				shmOsdHeaderEntity.setOsdDayCnt(new BigDecimal(updateOsdHeader.getOsdDayCount()));
			}
			if(updateOsdHeader.getOsdDescription() != null
					&& shmOsdHeaderEntity.getOsdDescription() != updateOsdHeader.getOsdDescription()) {
				shmOsdHeaderEntity.setOsdDescription(updateOsdHeader.getOsdDescription());
			}
			if(documentImageCount > 0) {
				shmOsdHeaderEntity.setPhotoCnt(BasicTransformer.toBigDecimal(documentImageCount));
			}
			if(updateOsdHeader.getStatusCd() != null
					&& shmOsdHeaderEntity.getStatusCd() != updateOsdHeader.getStatusCd()) {
				shmOsdHeaderEntity.setStatusCd(updateOsdHeader.getStatusCd());
			}
			if(updateOsdHeader.getOsdCloseDateTime() != null){
				shmOsdHeaderEntity.setOsdCloseTmst(BasicTransformer.toTimestamp(updateOsdHeader.getOsdCloseDateTime()));
			}		
			if(updateOsdHeader.getProNbr() != null
					&& OsdCategoryCdTransformer.toEnum(shmOsdHeaderEntity.getOsdCategoryCd()) == OsdCategoryCd.OVERAGE){
				shmOsdHeaderEntity.setProNbrTxt(ProNumberHelper.toElevenDigitPro(updateOsdHeader.getProNbr(), txnContext));
			}
		}
	}
	
	public void createOsdMgmtRemarks(OsdParentShipment osdParentShipmentRqst, ShmOsdHeader osdHeaderEntity,
			List<OsdManagementRemark> osdManagementRemarks, TransactionContext txnContext, EntityManager entityManager)
			throws NotFoundException, ServiceException, ValidationException {
		for(OsdManagementRemark osdManagementRemark: CollectionUtils.emptyIfNull(osdManagementRemarks)){
					
			ManagementRemark managementRemark = buildMgmtRemarkRqst(osdParentShipmentRqst, osdManagementRemark, txnContext);
			managementRemark.setParentOsdId(osdHeaderEntity.getOsdId());
			if(osdParentShipmentRqst.getShipmentInstId() != null) {
				managementRemark.setParentShipmentInstId(osdParentShipmentRqst.getShipmentInstId());
			}
			else if(osdHeaderEntity.getShpInstId() != null) {
				managementRemark.setParentShipmentInstId(BasicTransformer.toLong(osdHeaderEntity.getShpInstId()));
			}

			maintainShipmentManagementRemarkImpl.createMgmtRemark(managementRemark, null, null, txnContext, entityManager);  
		}
	}
	
	public UpsertOsdResp upsertOsdForOtherCategory(UpsertOsdRqst upsertOsdRqst, TransactionContext txnContext, ShmOsdHeader shmOsdHeader,
			EntityManager entityManager) throws ServiceException {
		
		UpsertOsdResp upsertOsdResp = new UpsertOsdResp();
		List<ShmOsdImage> childProTobeDeleted = new ArrayList<>();
		osdOtherCategoryValidator.validate(upsertOsdRqst, txnContext);
		
		AuditInfo auditInfo = getAuditInfo(upsertOsdRqst, txnContext);
		osdOtherCategoryValidator.validateAuditInfo(auditInfo, txnContext);

		Quintet<ShmShipment, ShmShipment, Boolean, Map<String, ShmOsdHeader>, ShmOsdHeader> shipmentAndOsdDetails = getShipmentAndOsdDetails(upsertOsdRqst, shmOsdHeader, txnContext, entityManager);

		osdOtherCategoryValidator.validateOtherCategoryPayloadwithDB(upsertOsdRqst, shipmentAndOsdDetails, shmOsdHeader,
				txnContext, entityManager);
		
		ShmOsdHeader osdHeaderEntity = osdEntityOtherCategoryTransformer.buildShmOsdHeader(upsertOsdRqst,
				shipmentAndOsdDetails, shmOsdHeader, auditInfo, childProTobeDeleted, txnContext, entityManager);
		
		shmOsdHdrSubDAO.save(osdHeaderEntity, entityManager);		
		if (Objects.nonNull(osdHeaderEntity.getShmOsdImages())
				&& CollectionUtils.isNotEmpty(osdHeaderEntity.getShmOsdImages())) {
			for (ShmOsdImage shmOsdImage : osdHeaderEntity.getShmOsdImages()) {
				if (childProTobeDeleted.contains(shmOsdImage)) {
					shmOsdImageSubDAO.remove(shmOsdImage, entityManager);
				} else {
					shmOsdImageSubDAO.save(shmOsdImage, entityManager);
				}
			}
		}
		if(Objects.nonNull(osdHeaderEntity.getShmMgmtRemarks()) && CollectionUtils.isNotEmpty(osdHeaderEntity.getShmMgmtRemarks())) {
			for (ShmMgmtRemark shmMgmtRemark : osdHeaderEntity.getShmMgmtRemarks()) {
				shipmentManagementRemarkDAO.save(shmMgmtRemark, entityManager);
			}
		}
		entityManager.flush();
		
		LOGGER.info("Save Succesful for Shipment Osd Header with PRO # " + osdHeaderEntity.getProNbrTxt());
		
		buildUpsertResponse(upsertOsdRqst, upsertOsdResp, osdHeaderEntity);
		
		LOGGER.info(String.format("Response Payload for upsertOsd : %s ", new Gson().toJson(upsertOsdResp)));
		
		return upsertOsdResp;

	}

	private AuditInfo getAuditInfo(UpsertOsdRqst upsertOsdRqst, TransactionContext txnContext) {
		ActionCd actionCd = upsertOsdRqst.getActionCd();
		AuditInfo auditInfo = null;
		if (upsertOsdRqst.getAuditInfo() != null) {
			auditInfo = upsertOsdRqst.getAuditInfo();
		} else if (ActionCd.ADD == actionCd) {
			auditInfo = AuditInfoHelper.getAuditInfoWithPgmId(CREATE_PGM_ID, txnContext);
		} else if (ActionCd.UPDATE == actionCd) {
			auditInfo = AuditInfoHelper.getAuditInfoWithPgmId(UPDATE_PGM_ID, txnContext);
		}
		return auditInfo;
	}
	
	private Quintet<ShmShipment, ShmShipment, Boolean, Map<String, ShmOsdHeader>,ShmOsdHeader> getShipmentAndOsdDetails(
			UpsertOsdRqst upsertOsdRqst, ShmOsdHeader shmOsdHeader, TransactionContext txnContext,
			EntityManager entityManager) throws ValidationException {

		ShmShipment shmShipment = null;
		ShmShipment shmShipmentForNewParentProNbr = null;
		ShmOsdHeader shmOsdHeaderForConeColorValidation = null;
		Map<String, ShmOsdHeader> shmOsdHeaderForChildProMap = null;
		OsdParentShipment osdParentShipment = upsertOsdRqst.getOsdParentShipment();
		List<OsdChildShipment> osdChildShipments = upsertOsdRqst.getOsdChildShipments();
		OtherOsdReasonCd otherOsdReasonCd = null;
		Boolean isLegacyPro = Boolean.FALSE;
		Boolean checkDuplicatesForChildPro = Boolean.FALSE;
		String reportingSicCd = null;
		String parentProNbr = null;
		BigInteger coneNbr = null;
		String coneColorCd = null;
		

		if (Objects.nonNull(osdParentShipment)) {
			if (Objects.nonNull(osdParentShipment.getParentProNbr())) {
				List<ShmShipment> shmShipments = shipmentSubDAO.findByProNumber(
						Collections.singletonList(
								ProNumberHelper.toElevenDigitPro(osdParentShipment.getParentProNbr(), txnContext)),
						entityManager);
				if (Objects.nonNull(shmShipments) && !CollectionUtils.isEmpty(shmShipments)) {
					shmShipment = shmShipments.get(0);
					if (shmShipment.getShmHandlingUnits().size() == 0) {
						isLegacyPro = Boolean.TRUE;
					}
				}
			}

			if (Objects.nonNull(osdParentShipment.getNewParentProNbr())) {
				List<ShmShipment> shmShipments = shipmentSubDAO.findByProNumber(
						Collections.singletonList(
								ProNumberHelper.toElevenDigitPro(osdParentShipment.getNewParentProNbr(), txnContext)),
						entityManager);
				if (Objects.nonNull(shmShipments) && !CollectionUtils.isEmpty(shmShipments)) {
					shmShipmentForNewParentProNbr = shmShipments.get(0);
				}
			}

			if (Objects.nonNull(osdParentShipment.getOtherOsdReasonCd())) {
				otherOsdReasonCd = osdParentShipment.getOtherOsdReasonCd();
			} 
			
			
			if(Objects.nonNull(osdParentShipment.getParentProNbr())) {
				parentProNbr = osdParentShipment.getParentProNbr();
			}else if (Objects.nonNull(shmOsdHeader) && Objects.nonNull(shmOsdHeader.getProNbrTxt())) {
				parentProNbr = shmOsdHeader.getProNbrTxt();
			}
			
			if(Objects.isNull(parentProNbr)) {
				checkDuplicatesForChildPro = Boolean.TRUE;
			}
			
			if (Objects.nonNull(osdParentShipment.getReportingSicCd())) {
				reportingSicCd = osdParentShipment.getReportingSicCd();
			} else if (Objects.nonNull(shmOsdHeader) && Objects.nonNull(shmOsdHeader.getReportingSicCd())) {
				reportingSicCd = shmOsdHeader.getReportingSicCd();
			}

			if (Objects.nonNull(osdParentShipment.getConeNbr())) {
				coneNbr = osdParentShipment.getConeNbr();
			} else if (Objects.nonNull(shmOsdHeader) && Objects.nonNull(shmOsdHeader.getConeNbr())) {
				coneNbr = BasicTransformer.toBigInteger(shmOsdHeader.getConeNbr());
			}

			if (Objects.nonNull(osdParentShipment.getConeColorCd())) {
				coneColorCd = ConeColorCdTransformer.toCode(osdParentShipment.getConeColorCd());
			} else if (Objects.nonNull(shmOsdHeader) && Objects.nonNull(shmOsdHeader.getConeColorCd())) {
				coneColorCd = shmOsdHeader.getConeColorCd();
			}

			shmOsdHeaderForConeColorValidation = shmOsdHdrSubDAO.getByConeAndSicDetails(coneColorCd, coneNbr,
					reportingSicCd, entityManager);

		}

		if (Objects.isNull(otherOsdReasonCd) && Objects.nonNull(shmOsdHeader) && Objects.nonNull(shmOsdHeader.getOtherReasonCd())) {
			otherOsdReasonCd = OtherOsdReasonCdTransformer.toEnum(shmOsdHeader.getOtherReasonCd());
		}
		
		if ((Objects.nonNull(otherOsdReasonCd) && OtherOsdReasonCdTransformer.toCode(otherOsdReasonCd)
				.equalsIgnoreCase(OtherOsdReasonCdTransformer.toCode(OtherOsdReasonCd.ONLY_CHILD_PRO)))
				|| (checkDuplicatesForChildPro)) {
			if (Objects.nonNull(osdChildShipments)) {
				for (OsdChildShipment osdChildShipment : osdChildShipments) {
					if (Objects.nonNull(osdChildShipment.getActionCd())
							&& ActionCd.ADD.equals(osdChildShipment.getActionCd())) {
						String elevenDigitProNumber = ProNumberHelper
								.toElevenDigitPro(osdChildShipment.getChildProNbr(), txnContext);
						ShmOsdHeader shmOsdHeaderForChildPro = shmOsdHdrSubDAO.getByOsdIdOrProNumber(null,
								elevenDigitProNumber, reportingSicCd, null, null, Boolean.TRUE, entityManager);
						if (Objects.nonNull(shmOsdHeaderForChildPro)
								&& !shmOsdHeaderForChildPro.getStatusCd().contains(CLOSED)) {
							shmOsdHeaderForChildProMap = new HashMap<>();
							shmOsdHeaderForChildProMap.put(osdChildShipment.getChildProNbr(), shmOsdHeaderForChildPro);
							break;
						}
					}
				}

			}
		}

		return new Quintet<>(shmShipment, shmShipmentForNewParentProNbr, isLegacyPro, shmOsdHeaderForChildProMap,shmOsdHeaderForConeColorValidation);

	}
} 