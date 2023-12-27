package com.xpo.ltl.shipment.service.validators;

import java.sql.Timestamp;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.xml.datatype.XMLGregorianCalendar;

import com.xpo.ltl.api.location.v2.GetLocationReferenceDetailsResp;
import com.xpo.ltl.api.location.v2.ListLocationFeaturesResp;
import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdHeader;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdImage;
import com.xpo.ltl.api.shipment.transformer.v2.ConeColorCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OsdCategoryCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OsdStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OverageApprovalStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.CloseReasonCd;
import com.xpo.ltl.api.shipment.v2.ConeColorCd;
import com.xpo.ltl.api.shipment.v2.OsdCategoryCd;
import com.xpo.ltl.api.shipment.v2.OsdImage;
import com.xpo.ltl.api.shipment.v2.OsdParentShipment;
import com.xpo.ltl.api.shipment.v2.OsdPayloadTypeCd;
import com.xpo.ltl.api.shipment.v2.OsdStatusCd;
import com.xpo.ltl.api.shipment.v2.OverageApprovalStatusCd;
import com.xpo.ltl.api.shipment.v2.UpsertOsdRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmOsdHdrSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmOsdImageSubDAO;
import com.xpo.ltl.shipment.service.impl.UpsertOsdImpl;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author skamble002
 *
 */
/**
 * @author skamble002
 *
 */
public class OsdHeaderValidator extends Validator {

	private static final Log LOGGER = LogFactory.getLog(OsdHeaderValidator.class);
    private static final String REQUEST_PRO_NBR_TXT = "request.shipmentIds.proNumber";
    private static final String CURRENT_LOCATION = UpsertOsdImpl.class.getCanonicalName();
    private static final int MAXLENGTH = 1500;
    private static final List<OsdCategoryCd> notOverageCategoryCds = Arrays.asList(OsdCategoryCd.DAMAGED, OsdCategoryCd.REFUSED, OsdCategoryCd.SHORT, OsdCategoryCd.OTHER);
	private static final List<OsdCategoryCd> refusedDamagedCds = Arrays.asList(OsdCategoryCd.REFUSED, OsdCategoryCd.DAMAGED);
	private static final List<OsdStatusCd> closedStatusCds = Arrays.asList(OsdStatusCd.D_CLOSED, OsdStatusCd.O_CLOSED, OsdStatusCd.R_CLOSED, OsdStatusCd.S_CLOSED, OsdStatusCd.OT_CLOSED);
	private static final List<OsdStatusCd> readyToShipStatusCds = Arrays.asList(OsdStatusCd.D_READY_TO_SHIP, OsdStatusCd.O_READY_TO_SHIP, OsdStatusCd.R_READY_TO_SHIP, OsdStatusCd.S_READY_TO_SHIP);
	private static final List<OverageApprovalStatusCd> hasApprovalStatusCds  = Arrays.asList(OverageApprovalStatusCd.APPROVED, OverageApprovalStatusCd.DISAPPROVED);

	@Inject
	private ShmOsdImageSubDAO shmOsdImageSubDAO;
	
	@Inject
	private ShmOsdHdrSubDAO shmOsdHdrSubDAO;
	
	@Inject
    private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;
	
    @Inject
	private ExternalRestClient externalRestClient;
    
    /**
     * Method to validate OSD Header request 
     * @param osdParentShipment
     * @param auditInfo
     * @param moreInfos
     * @param txnContext
     * @throws ServiceException 
     */
    public void validateOsdHeaderRqstForCreate(UpsertOsdRqst upsertOsdRqst, Boolean isLegacyPro, final AuditInfo auditInfo, final List<MoreInfo> moreInfos,
            final TransactionContext txnContext, final EntityManager entityManager) throws ServiceException {
    	
    	OsdParentShipment osdParentShipment = upsertOsdRqst.getOsdParentShipment();
    	
    	if(osdParentShipment !=null) {
    		// Validate Pro Number 
    		if(StringUtils.isNotBlank(osdParentShipment.getParentProNbr())){
    			validateProNumber(osdParentShipment, txnContext);
    		}
    		
			if(StringUtils.isNotBlank(osdParentShipment.getParentProNbr())) {
				validateParentProAlreadyExist(osdParentShipment, txnContext, entityManager );
			}
    		
    		// TODO : Parent or legacy PRO# should not be Final delivered. Validation Error: “Searched PRO# is final delivered and an OS&D entry cannot be created for it.”
    		
    /*    	SHP_INST_ID (If PRO Number is available it is mandatory) - fetch automatically
        	if (osdParentShipment.getParentProNbr() != null) {
        		if(osdParentShipment.getShipmentInstId() == null) {
        			addMoreInfo(moreInfos, "SHP_INST_ID", ValidationErrorMessage.SHIPMENT_INST_ID_RQ);
        		}
        	}*/
            
            //OSD_CATEGORY_CD - Mandatory  Enum values : Short, Refused, Overage, Damaged
            if (osdParentShipment.getOsdCategoryCd() == null) {
            	addMoreInfo(moreInfos, "osdCategoryCd", ValidationErrorMessage.OSD_MANDATORY_FIELDS);
            }
     
            if(osdParentShipment.getOsdCategoryCd() != null) {
            	  //When OSD_CATEGORY_CD = “Refused” or “Damaged” - Make sure Red Cone is selected initially.
            	if (osdParentShipment.getOsdCategoryCd().equals(OsdCategoryCd.REFUSED) 
                 		|| osdParentShipment.getOsdCategoryCd().equals(OsdCategoryCd.DAMAGED)) {
            		if(osdParentShipment.getConeColorCd() !=null) {
                        if (!osdParentShipment.getConeColorCd().equals(ConeColorCd.RED)) {
                         	addMoreInfo(moreInfos, "coneColorCd", ValidationErrorMessage.OSD_CONE_COLOR_INVALID);
                         }
            		}
                 }
            	 
                // OSD_CATEGORY_CD = “Overage” - Make sure Yellow Cone is selected initially
            	if (osdParentShipment.getOsdCategoryCd().equals(OsdCategoryCd.OVERAGE) 
            			&& osdParentShipment.getConeColorCd() != null) {
                        if (!osdParentShipment.getConeColorCd().equals(ConeColorCd.YELLOW)) {
                        	addMoreInfo(moreInfos, "coneColorCd", ValidationErrorMessage.OSD_CONE_COLOR_INVALID);
                        }
                }
            	
            	// CONE_NBR - Cone Number is required for Refused, Damaged and Overage Shipments 
            	if (osdParentShipment.getOsdCategoryCd().equals(OsdCategoryCd.REFUSED) 
                   		|| osdParentShipment.getOsdCategoryCd().equals(OsdCategoryCd.DAMAGED)
                   		|| osdParentShipment.getOsdCategoryCd().equals(OsdCategoryCd.OVERAGE)) {
                       if (osdParentShipment.getConeNbr() == null) {
                           addMoreInfo(moreInfos, "coneNbr", "Cone Number is required for Refused, Damaged and Overage Shipments");
                       } 
            	}
            	
                //CONE_NBR - Field is not required for Short Shipments.
            	 if (osdParentShipment.getOsdCategoryCd().equals(OsdCategoryCd.SHORT)) {
					if (osdParentShipment.getConeNbr() != null) {
						addMoreInfo(moreInfos, "coneNbr", "Cone Number is not required for Short");
					}
					if (osdParentShipment.getConeColorCd() != null) {
						addMoreInfo(moreInfos, "coneColorCd", "Cone Colour is not required for Short");
					}
                 }
            	 
            	// OSD_DESCRIPTION - Should not exceed MAX_LENGTH chars.
            	 if (osdParentShipment.getOsdCategoryCd().equals(OsdCategoryCd.OVERAGE)
            			 && StringUtils.isNotBlank(osdParentShipment.getOsdDescription())) {
            		 if(osdParentShipment.getOsdDescription() != null) {
                         if (osdParentShipment.getOsdDescription().length() > MAXLENGTH) {
                             addMoreInfo(moreInfos, "osdDescription", "Should not exceed " + MAXLENGTH + " Characters");
                         }  
            		 }
                 }
            	 
            	 if(osdParentShipment.getStatusCd() != null) {
                     String osdCategory = OsdCategoryCdTransformer.toString(osdParentShipment.getOsdCategoryCd()).toUpperCase();
                     String osdStatus = OsdStatusCdTransformer.toCode(osdParentShipment.getStatusCd()).toUpperCase();
                     if(!osdCategory.substring(0, 1).equals(osdStatus.substring(0, 1))) {
                     	addMoreInfo(moreInfos, "statusCd", "OSD Status and OSD Category code are not matching");
                     }
            	 }

				 if(osdParentShipment.getOverageProNbr() != null) {
					if(!ProNumberHelper.isYellowPro(osdParentShipment.getOverageProNbr())) {
						addMoreInfo(moreInfos, "overageProNbr", "Cannot accept a parent PRO format in overage pro field for OSD Parent shipment");
					}
				 }
				 
				 // CONE_COLOR_CD - Mandatory Field
				 if (osdParentShipment.getConeColorCd() == null 
						 && !osdParentShipment.getOsdCategoryCd().equals(OsdCategoryCd.SHORT)) {
					 addMoreInfo(moreInfos, "coneColorCd", ValidationErrorMessage.OSD_MANDATORY_FIELDS);
				 }
            }
            
            if(osdParentShipment.getConeColorCd() != null) {
            	//TODO: Orange cone color is for any OSD_CATEGORY_CD - defined as Work in Progress.	
            	if (osdParentShipment.getConeColorCd().equals(ConeColorCd.ORANGE)) {
               } 
         	   
         	   //TODO: Green cone color is for any OSD_CATEGORY_CD - defined as Ready to be shipped	
            	if (osdParentShipment.getConeColorCd().equals(ConeColorCd.GREEN)) {
              }

            	// CONE_COLOR_CD - Cone color cannot be green until the OS&D shipment is Ready to Ship
            	if (osdParentShipment.getConeColorCd().equals(ConeColorCd.GREEN) 
            			&& osdParentShipment.getStatusCd() != null) {
            		if (!readyToShipStatusCds.contains(osdParentShipment.getStatusCd())) {
                		 addMoreInfo(moreInfos, "coneColorCd", "Cone color cannot be green until the OS&D shipment is Ready to Ship");
            		}
            	}
            }
            
            // CONE_NBR - Provide numeric validations. 
           if (osdParentShipment.getConeNbr() != null) {
        	   if (!StringUtils.isNumeric(osdParentShipment.getConeNbr().toString())) {
                  	addMoreInfo(moreInfos, "coneNbr", "Cone Number is not numeric");
                  }
           }

			//REPORTING_SIC_CD - Mandatory - osdParentShipment.reportingSicCd
			if (StringUtils.isBlank(osdParentShipment.getReportingSicCd())) {
				addMoreInfo(moreInfos, "reportingSicCd", "Reporting SIC code information is Required");
			}
            
			//REPORTING_SIC_CD - Cannot be a M&T Sic
			else if(StringUtils.isNotBlank(osdParentShipment.getReportingSicCd())) {
				try {
					GetLocationReferenceDetailsResp resp = externalRestClient.getLocationReferenceDetails(osdParentShipment.getReportingSicCd(), txnContext);
					if(!(resp != null && resp.getCompanyOperations() != null 
						&& StringUtils.isNotBlank(resp.getCompanyOperations().getFreightOperationsType()) 
						&& !resp.getCompanyOperations().getFreightOperationsType().equals("T"))) {
						throw ExceptionBuilder
							.exception(ValidationErrorMessage.RPTG_SIC_INVALID, txnContext)
							.moreInfo(CURRENT_LOCATION, "The SIC code is not a valid value.")
							.log()
							.build(); 
						} 
					} catch (ServiceException e) {
						throw ExceptionBuilder
						   .exception(ValidationErrorMessage.RPTG_SIC_INVALID, txnContext)
						   .moreInfo(CURRENT_LOCATION, "The SIC code is not a valid value.")
						   .log()
						   .build();
					}
			}

            // STATUS_CD - Mandatory
            if (Objects.isNull(osdParentShipment.getStatusCd())) {
    			addMoreInfo(moreInfos, "statusCd", "OSD Status Code is required");
    			throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
    					.moreInfo(moreInfos).build();
            }
            
            //CCS-7890: Closed reason code validation 
            if(osdParentShipment.getStatusCd() != null) {
            	if(closedStatusCds.contains(osdParentShipment.getStatusCd())) {
                	if(osdParentShipment.getCloseReasonCd() == null) {
                		addMoreInfo(moreInfos, "closeReasonCd", "Closed Reason Code is required");
            			throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
            					.moreInfo(moreInfos).build();
                	}

                    checkClosingReasonByOSDCategory(
                            osdParentShipment.getOsdCategoryCd(),
                            osdParentShipment.getCloseReasonCd(), moreInfos);

                }
                if(!closedStatusCds.contains(osdParentShipment.getStatusCd())) {
                	if(osdParentShipment.getCloseReasonCd() != null) {
                		addMoreInfo(moreInfos, "closeReasonCd", "Closed Due To reason is required only for close OS&D entry");
            			throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
            					.moreInfo(moreInfos).build();
                	}
                }
            }
            
            // CCS-8043 : HU_CNT - Mandatory for legacy PRO - osdParentShipment.osdPiecesCount
            if (osdParentShipment.getOsdPiecesCount() == null
            		&& isLegacyPro
            		&& osdParentShipment.getOsdCategoryCd() != null
            		&& isNotOverage(osdParentShipment.getOsdCategoryCd())) {
    			addMoreInfo(moreInfos, "osdPiecesCount", "OSD Pieces Count is required. Is Legacy Pro. Parent Category is not Overage");
    			throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
    					.moreInfo(moreInfos).build();
            }
            
        	//	Reporting SIC Validation
    		if (StringUtils.isNotBlank(osdParentShipment.getReportingSicCd())) {

                validateReportingSicCd(osdParentShipment.getReportingSicCd(), txnContext);

    		}
    	}
        
        // CRTE_BY- Mandatory - 20 VARCHAR
        if (StringUtils.isBlank(auditInfo.getCreatedById())) {
        	addMoreInfo(moreInfos, "CRTE_BY", ValidationErrorMessage.OSD_MANDATORY_FIELDS);
        }
         
        // CRTE_TMST - Mandatory
        if (auditInfo.getCreatedTimestamp() == null ) {
        	addMoreInfo(moreInfos, "LST_UPDT_TMST", ValidationErrorMessage.OSD_MANDATORY_FIELDS);
        }
        
        // CRTE_PGM_ID - Mandatory - 50 VARCHAR
        if (StringUtils.isBlank(auditInfo.getCreateByPgmId())) {
        	addMoreInfo(moreInfos, "CRTE_PGM_ID", ValidationErrorMessage.OSD_MANDATORY_FIELDS);
        }
        
        // LST_UPDT_TMST - Mandatory - 
        if (auditInfo.getUpdatedTimestamp() == null ) {
        	addMoreInfo(moreInfos, "LST_UPDT_TMST", ValidationErrorMessage.OSD_MANDATORY_FIELDS);
        }
        
        // LST_UPDT_PGM_ID - Mandatory - Maximum 50 character
        if (StringUtils.isBlank(auditInfo.getUpdateByPgmId())) {
        	addMoreInfo(moreInfos, "LST_UPDT_PGM_ID", ValidationErrorMessage.OSD_MANDATORY_FIELDS);
        }
    }
    
    /**
     * Method to validate Pro Number
     * @param upsertOsdRqst
     * @param errorMsgsMap
     * @param txnContext
     */
    public void validateProNumber(OsdParentShipment osdParentShipment, TransactionContext txnContext) {
       
        List<MoreInfo> moreInfos = new ArrayList<>();
        
        String parentPro = osdParentShipment.getParentProNbr();
        
        if (StringUtils.isBlank(parentPro)) {
            addMoreInfo(moreInfos, REQUEST_PRO_NBR_TXT, "The PRO number can't blank");
            return;
        }

        try {
        	osdParentShipment.setParentProNbr(ProNumberHelper.validateProNumber(parentPro, txnContext));
        } catch (ServiceException se) {
            addMoreInfo(moreInfos, REQUEST_PRO_NBR_TXT, "The PRO number entered does not have a valid format");
        }

    }
    /**
     * Method to validate OSD Image Request
     * @param osdImage
     * @param auditInfo
     * @param moreInfos
     * @param actionCd
     * @param txnContext
     * @throws ServiceException 
     */
    public void validateOsdImageRqstForUpsert(OsdImage osdImage, OsdParentShipment osdParentShipment, final AuditInfo auditInfo, final List<MoreInfo> moreInfos,
            ActionCd actionCd, final TransactionContext txnContext, final EntityManager entityManager) throws ServiceException {
    	
    	if(osdParentShipment != null) {
            if(osdParentShipment.getOsdCategoryCd() != null) {
                Boolean notOverage = isNotOverage(osdParentShipment.getOsdCategoryCd());
        		if(notOverage){

        			if (StringUtils.isEmpty(osdParentShipment.getParentProNbr())
						&& actionCd == ActionCd.ADD) {
                    	addMoreInfo(moreInfos, "parentProNbr", ValidationErrorMessage.OSD_MANDATORY_FIELDS);
                    } 
        		}
            }
        	// Reporting SIC Validation
        	if (StringUtils.isNotBlank(osdParentShipment.getReportingSicCd())) {
    			boolean sicIsOperational = externalRestClient.isValidOperationalSic(osdParentShipment.getReportingSicCd(), txnContext);
    			if (!sicIsOperational) {
    				throw ExceptionBuilder.exception(ValidationErrorMessage.RPTG_SIC_INVALID, txnContext).log().build();
    			}
        	}
        	
    	}
        
        if(osdImage == null) {
			throw ExceptionBuilder
			.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
			.moreInfo(CURRENT_LOCATION, "Not a valid Overage Image Header request")
			.log()
			.build();
		}

		if(osdImage.getProNbr() != null) {
			if(!ProNumberHelper.isYellowPro(osdImage.getProNbr())){
				throw ExceptionBuilder
				.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
				.moreInfo(CURRENT_LOCATION, String.format("%s, Cannot accept parent PRO format for child pro numbers in OsdChildShipments.", osdImage.getProNbr()))
				.log()
				.build();
			}
		}
        
        // CRTE_BY- Mandatory - 20 VARCHAR
        if (StringUtils.isBlank(auditInfo.getCreatedById())) {
        	addMoreInfo(moreInfos, "CRTE_BY", ValidationErrorMessage.OSD_MANDATORY_FIELDS);
        }
        
        // CRTE_TMST - Mandatory
        if (auditInfo.getCreatedTimestamp() == null ) {
        	addMoreInfo(moreInfos, "LST_UPDT_TMST", ValidationErrorMessage.OSD_MANDATORY_FIELDS);
        }
        
        // CRTE_PGM_ID - Mandatory - 50 VARCHAR
        if (StringUtils.isBlank(auditInfo.getCreateByPgmId())) {
        	addMoreInfo(moreInfos, "CRTE_PGM_ID", ValidationErrorMessage.OSD_MANDATORY_FIELDS);
        }
        
        // LST_UPDT_TMST - Mandatory - 
        if (auditInfo.getUpdatedTimestamp() == null ) {
        	addMoreInfo(moreInfos, "LST_UPDT_TMST", ValidationErrorMessage.OSD_MANDATORY_FIELDS);
        }
        
        // LST_UPDT_PGM_ID - Mandatory - Maximum 50 character
        if (StringUtils.isBlank(auditInfo.getUpdateByPgmId())) {
        	addMoreInfo(moreInfos, "LST_UPDT_PGM_ID", ValidationErrorMessage.OSD_MANDATORY_FIELDS);
        }
    }

    public void validateGetOsdParameters(Long osdId, String proNbr, String reportingSicCd,
			OsdPayloadTypeCd osdPayloadTypeCd, TransactionContext txnContext)
			throws ServiceException {

		List<MoreInfo> moreInfos = new ArrayList<>();

		if (null == osdId && StringUtils.isBlank(proNbr)) {
			addMoreInfo(moreInfos, "osdId|proNbr",
					"Either (osdId and osdPayloadTypeCd) OR (proNbr and reportingSicCd and osdPayloadTypeCd) are required");
			throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
					.moreInfo(moreInfos).build();
		}

		if (null != osdId && null == osdPayloadTypeCd) {
			addMoreInfo(moreInfos, "osdPayloadTypeCd", "osdId and osdPayloadTypeCd are required");
			throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
					.moreInfo(moreInfos).build();
		}

		if (StringUtils.isNotBlank(proNbr) && (StringUtils.isBlank(reportingSicCd) || null == osdPayloadTypeCd)) {
			addMoreInfo(moreInfos, "reportingSicCd|osdPayloadTypeCd",
					"proNbr and reportingSicCd and osdPayloadTypeCd are required");
			throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
					.moreInfo(moreInfos).build();
		}

		if (StringUtils.isNotBlank(reportingSicCd)) {

            validateReportingSicCd(reportingSicCd, txnContext);

		}

	}
	
    public void validateOsdHeaderRqstForUpdate(UpsertOsdRqst upsertOsdRqst, ShmOsdHeader shmOsdHeaderEntity, final AuditInfo auditInfo, final List<MoreInfo> moreInfos,
            final TransactionContext txnContext, final EntityManager entityManager) throws ServiceException {
		
    	OsdParentShipment osdParentShipment = upsertOsdRqst.getOsdParentShipment();
    	String reportingSicCd = null;
		BigInteger coneNbr = null;
		String coneColorCd = null;
    	
    	if(osdParentShipment != null) {
    		// Validate Pro Number 
    		if(StringUtils.isNotBlank(osdParentShipment.getParentProNbr())){
    			validateProNumber(osdParentShipment, txnContext);
    		}

            // CONE_NBR - Provide numeric validations. 
    		if(osdParentShipment.getConeNbr() != null) {
    	        if (!StringUtils.isNumeric(osdParentShipment.getConeNbr().toString())) {
    	        	addMoreInfo(moreInfos, "coneNbr", "Cone Number is not numeric");
    	        }
    		}
            
            if(osdParentShipment.getOsdCategoryCd() != null
            		&& osdParentShipment.getStatusCd() != null) {
                String osdCategory = OsdCategoryCdTransformer.toString(osdParentShipment.getOsdCategoryCd()).toLowerCase();
                String osdStatus = OsdStatusCdTransformer.toCode(osdParentShipment.getStatusCd()).toLowerCase();
                if(!osdCategory.substring(0, 1).equals(osdStatus.substring(0, 1))) {
                	addMoreInfo(moreInfos, "statusCd|osdCategoryCd", "OSD Status and OSD Category code are not matching");
                }
            }
            
            if(shmOsdHeaderEntity.getOsdCategoryCd() != null
            		&& osdParentShipment.getStatusCd() != null
            		&& osdParentShipment.getOsdCategoryCd() == null) {	
            	String osdStatus = OsdStatusCdTransformer.toCode(osdParentShipment.getStatusCd()).toLowerCase();
                if(!shmOsdHeaderEntity.getOsdCategoryCd().toLowerCase().substring(0, 1).equals(osdStatus.substring(0, 1))) {
                	addMoreInfo(moreInfos, "statusCd", "OSD Status and OSD Category code are not matching when Parent Shipment Category Code is non existent");
                }
            }
            
            //CCS-8084: If the payload contains reporting SIC other than what exists in the tables, throw the error - “Reporting SIC cannot be updated.”
            if(StringUtils.isNotBlank(osdParentShipment.getReportingSicCd())
            		&& !shmOsdHeaderEntity.getReportingSicCd().equals(osdParentShipment.getReportingSicCd())) {
            	addMoreInfo(moreInfos, "reportingSicCd", "Reporting SIC cannot be updated.");
            }
            
          	//CCS-7890: Closed reason code validation 
            if(osdParentShipment.getStatusCd() != null) {
            	if(closedStatusCds.contains(osdParentShipment.getStatusCd())) {
                	if(osdParentShipment.getCloseReasonCd() == null) {
                		addMoreInfo(moreInfos, "closeReasonCd", "Closed Due To reason is required to close an OS&D entry");
            			throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
            					.moreInfo(moreInfos).build();
                	}

                    checkClosingReasonByOSDCategory(
                            OsdCategoryCdTransformer.toEnum(shmOsdHeaderEntity.getOsdCategoryCd()),
                            osdParentShipment.getCloseReasonCd(), moreInfos);

                }
                if(!closedStatusCds.contains(osdParentShipment.getStatusCd())) {
                	if(osdParentShipment.getCloseReasonCd() != null) {
                		addMoreInfo(moreInfos, "closeReasonCd", "Closed Due To reason is required only for close OS&D entry");
            			throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
            					.moreInfo(moreInfos).build();
                	}
                }
            }
            
            //CCS-8028: Should not be updated the arriveAtOsdDateTime for actionCd - Update
            if(shmOsdHeaderEntity.getArriveAtOsdTmst() != null
            		&& osdParentShipment.getArriveAtOsdDateTime() != null
            		&& !osdParentShipment.getArriveAtOsdDateTime().normalize().equals((BasicTransformer.toXMLGregorianCalendar(shmOsdHeaderEntity.getArriveAtOsdTmst()).normalize()))) {
            	throw ExceptionBuilder
				.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
				.moreInfo(CURRENT_LOCATION, "Cannot update arriveAtOsdDateTime after creation.")
				.log()
				.build();
            }

			//CCS-8035: Upsert OSD API should not allow any updates to OS&D Entry when statusCd=closed
			if(closedStatusCds.contains(OsdStatusCdTransformer.toEnum(shmOsdHeaderEntity.getStatusCd()))) {
				throw ExceptionBuilder
				.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
				.moreInfo(CURRENT_LOCATION, "Cannot update an OS&D once closed.")
				.log()
				.build();
			}

			//CCS-8083: coneColorCd update validations
			if(shmOsdHeaderEntity.getOsdCategoryCd() != null
				&& osdParentShipment.getConeColorCd() != null){
				OsdCategoryCd currentCategory = OsdCategoryCdTransformer.toEnum(shmOsdHeaderEntity.getOsdCategoryCd());
				if(currentCategory == OsdCategoryCd.OVERAGE){
					if(osdParentShipment.getConeColorCd() == ConeColorCd.RED) {
						addMoreInfo(moreInfos, "coneColorCd", "Cone colour cannot be updated to Red for overage entries.");
					}
				}
				else if(currentCategory == OsdCategoryCd.SHORT) {
					addMoreInfo(moreInfos, "coneColorCd", "Cone colour should be null for Shortages.");
				}
				else if(currentCategory == OsdCategoryCd.REFUSED 
					|| currentCategory == OsdCategoryCd.DAMAGED) {
					if(osdParentShipment.getConeColorCd() == ConeColorCd.YELLOW) {
						addMoreInfo(moreInfos, "coneColorCd", "Cone colour cannot be updated to Yellow for refused/damaged entries.");
					}
				}
			}

			//CCS-8087: Update CategoryCd in Upsert API Validations
			if(shmOsdHeaderEntity.getOsdCategoryCd() != null 
				&& osdParentShipment.getOsdCategoryCd() != null) {
					OsdCategoryCd existingCategory = OsdCategoryCdTransformer.toEnum(shmOsdHeaderEntity.getOsdCategoryCd());
					OsdCategoryCd newCategory = osdParentShipment.getOsdCategoryCd();
					if(!refusedDamagedCds.contains(existingCategory)
						&& newCategory != existingCategory){
							addMoreInfo(moreInfos, "osdCategoryCd", String.format("Cannot update the category code for %s entries.", existingCategory.toString(), newCategory.toString()));
					}
					else if(refusedDamagedCds.contains(existingCategory)
						&& !refusedDamagedCds.contains(newCategory)){
							addMoreInfo(moreInfos, "osdCategoryCd", String.format("Cannot update the category code from %s to %s for the OSD entry.", existingCategory.toString(), newCategory.toString()));
					}
			}
			
			if (Objects.nonNull(shmOsdHeaderEntity.getOsdCategoryCd())) {
				if (shmOsdHeaderEntity.getOsdCategoryCd().equals(OsdCategoryCdTransformer.toCode(OsdCategoryCd.REFUSED))
						|| shmOsdHeaderEntity.getOsdCategoryCd().equals(OsdCategoryCdTransformer.toCode(OsdCategoryCd.DAMAGED))
						|| shmOsdHeaderEntity.getOsdCategoryCd().equals(OsdCategoryCdTransformer.toCode(OsdCategoryCd.OVERAGE))) {

					if (Objects.nonNull(osdParentShipment.getReportingSicCd())) {
						reportingSicCd = osdParentShipment.getReportingSicCd();
					} else if (Objects.nonNull(shmOsdHeaderEntity.getReportingSicCd())) {
						reportingSicCd = shmOsdHeaderEntity.getReportingSicCd();
					}

					if (Objects.nonNull(osdParentShipment.getConeNbr())) {
						coneNbr = osdParentShipment.getConeNbr();
					} else if (Objects.nonNull(shmOsdHeaderEntity.getConeNbr())) {
						coneNbr = BasicTransformer.toBigInteger(shmOsdHeaderEntity.getConeNbr());
					} 
					//CCS-10459: Throw error message when core number is empty in request and DB
					else if (coneNbr == null) {
            			throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
						.moreInfo("coneNbr", String.format("Cone Number is required."))
						.log().build();
					}					

					if (Objects.nonNull(osdParentShipment.getConeColorCd())) {
						coneColorCd = ConeColorCdTransformer.toCode(osdParentShipment.getConeColorCd());
					} else if (Objects.nonNull(shmOsdHeaderEntity.getConeColorCd())) {
						coneColorCd = shmOsdHeaderEntity.getConeColorCd();
					}

					ShmOsdHeader shmOsdHeaderForConeColorValidation = shmOsdHdrSubDAO
							.getByConeAndSicDetails(coneColorCd, coneNbr, reportingSicCd, entityManager);

					if (Objects.nonNull(shmOsdHeaderForConeColorValidation)
							&& shmOsdHeaderForConeColorValidation.getOsdId() != shmOsdHeaderEntity.getOsdId()) {
						throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
								.moreInfo("coneColorCd & coneNbr",
										String.format("Cone %s %s is already in use.",
												ConeColorCdTransformer.getLabel(ConeColorCdTransformer
														.toEnum(shmOsdHeaderForConeColorValidation.getConeColorCd())),
												shmOsdHeaderForConeColorValidation.getConeNbr()))
								.log().build();
					}
				}
			}
			
        }
        // LST_UPDT_BY - Mandatory - 20 VARCHAR
        if (StringUtils.isBlank(auditInfo.getCreatedById())) {
        	addMoreInfo(moreInfos, "LST_UPDT_BY", ValidationErrorMessage.OSD_MANDATORY_FIELDS);
        }
        
        // LST_UPDT_TMST - Mandatory - 
        if (auditInfo.getUpdatedTimestamp() == null ) {
        	addMoreInfo(moreInfos, "LST_UPDT_TMST", ValidationErrorMessage.OSD_MANDATORY_FIELDS);
        }
        
        // LST_UPDT_PGM_ID - Mandatory - Maximum 50 character
        if (StringUtils.isBlank(auditInfo.getUpdateByPgmId())) {
        	addMoreInfo(moreInfos, "LST_UPDT_PGM_ID", ValidationErrorMessage.OSD_MANDATORY_FIELDS);
        }
    }
    
	private Boolean isNotOverage(OsdCategoryCd osdCategoryCd) {
		return (notOverageCategoryCds.contains(osdCategoryCd));
	}
	
	public void validateParentProAlreadyExist(OsdParentShipment osdParentShipment, TransactionContext txnContext,
			EntityManager entityManager) throws ServiceException, ValidationException {
		String elevenDigitProNum = ProNumberHelper.validateProNumber(osdParentShipment.getParentProNbr(), txnContext);
		if(StringUtils.isNotBlank(elevenDigitProNum)
				&& StringUtils.isNotBlank(osdParentShipment.getReportingSicCd())) {
			List<ShmOsdHeader> shmOsdHeaders = shmOsdHdrSubDAO.getByProNumberOrReportingSic(ProNumberHelper.validateProNumber(elevenDigitProNum,txnContext), osdParentShipment.getReportingSicCd(), entityManager);
			if (shmOsdHeaders != null) {
				for(ShmOsdHeader shmOsdHeader : shmOsdHeaders)	{
					if(StringUtils.isNotBlank(shmOsdHeader.getStatusCd())
							&& !closedStatusCds.contains(OsdStatusCdTransformer.toEnum(shmOsdHeader.getStatusCd()))
							&& osdParentShipment.getOsdCategoryCd() != null
							&& notOverageCategoryCds.contains(osdParentShipment.getOsdCategoryCd())) {
						throw ExceptionBuilder
						.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
						.moreInfo(CURRENT_LOCATION, "An active OS&D entry exists on this PRO. Cannot create a new OS&D entry.")
						.log()
						.build();
					}
				}
			}
		}        
	}

    public void validateReportingSicCd(String reportingSicCd, TransactionContext txnContext) throws ServiceException {

        if (!isValidReportingSicCode(reportingSicCd, txnContext)) {

            throw ExceptionBuilder.exception(ValidationErrorMessage.RPTG_SIC_INVALID, txnContext)
                    .moreInfo(CURRENT_LOCATION, String.format("This service is unavailable for SIC: %s", reportingSicCd))
                    .log()
                    .build();

        }

        boolean sicIsOperational = externalRestClient.isValidOperationalSic(reportingSicCd, txnContext);
        if (!sicIsOperational) {

            throw ExceptionBuilder.exception(ValidationErrorMessage.RPTG_SIC_INVALID, txnContext)
                    .log()
                    .build();

        }

    }

	private static final String OSD_VALIDATION_FEATURE_CODE = "OSDAPP";

    private boolean isValidReportingSicCode(String reportingSicCd, TransactionContext txnContext) {

        boolean isReportingSicCdValid = false;

        try {

            ListLocationFeaturesResp listLocationFeaturesResp = externalRestClient.getLocFeatureSetting(OSD_VALIDATION_FEATURE_CODE, txnContext);

            if (listLocationFeaturesResp.getLocationFeatures() != null) {

                isReportingSicCdValid = listLocationFeaturesResp.getLocationFeatures().stream()
                        .anyMatch(locFeatureSetting -> reportingSicCd.equalsIgnoreCase(locFeatureSetting.getSicCd()) && BasicTransformer.toBoolean(locFeatureSetting.getSettingValue()));


            }

        } catch (ServiceException e) {

            LOGGER.warn("Feature not available: " + OSD_VALIDATION_FEATURE_CODE);

        }

        return isReportingSicCdValid;

    }
    
    public void validateChildProAlreadyExist(OsdParentShipment osdParentShipment, ShmOsdImage shmOsdImageEntity, Long rqstOsdId, ActionCd actionCd, TransactionContext txnContext,
			EntityManager entityManager) throws ServiceException, ValidationException {
   
    	String elevenDigitProNum = StringUtils.EMPTY;
    	ShmOsdImage shmOsdImage = null;
    	List<ShmHandlingUnit> listHandlingUnit = new ArrayList<>();
    	
    	if(osdParentShipment != null
    			&& osdParentShipment.getOverageProNbr() != null) {
    			elevenDigitProNum = ProNumberHelper.validateProNumber(osdParentShipment.getOverageProNbr(), txnContext);
    	}
    	
    	if(osdParentShipment != null 
    			&& osdParentShipment.getOverageProNbr() == null
    			&& shmOsdImageEntity != null
    			&& shmOsdImageEntity.getProNbrTxt() != null) {
    		elevenDigitProNum = shmOsdImageEntity.getProNbrTxt();
    	}
    	
    	if(osdParentShipment != null 
    			&& osdParentShipment.getApprovalStatusCd() == null
    			&& shmOsdImageEntity != null
    			&& shmOsdImageEntity.getApprovalStatusCd() != null) {
    		osdParentShipment.setApprovalStatusCd(OverageApprovalStatusCdTransformer.toEnum(shmOsdImageEntity.getApprovalStatusCd()));
    	}
    	
    	shmOsdImage = shmOsdImageSubDAO.findByProNumber(elevenDigitProNum, entityManager);
    	Set<String> childProSet = new HashSet<>(Arrays.asList(elevenDigitProNum));
    	listHandlingUnit = shmHandlingUnitSubDAO.findByChildProNumberList(childProSet, entityManager);
    	
    	if(actionCd == ActionCd.ADD) {
  
    		childProNumberExistValidation(txnContext, shmOsdImage, listHandlingUnit, null);
    		
    		//CCS-8554: For Create - Parent PRO# for an overage OS&D entry should not be added to the record if the child PRO#/overage PRO# is not assigned to it first.
    		if(osdParentShipment != null
    				&& osdParentShipment.getParentProNbr() != null
        			&& (osdParentShipment.getOverageProNbr() == null
        			|| (shmOsdImage != null
        			&& shmOsdImage.getProNbrTxt() == null))) {
        		throw ExceptionBuilder
    			.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
    			.moreInfo(CURRENT_LOCATION, "Overage PRO# needs to be assigned before the OS&D entry can be matched.")
    			.log()
    			.build();
        	}
    	}
			
    	if(actionCd == ActionCd.UPDATE
    			&& osdParentShipment != null) {
        	// CCS-8011: overagePro cannot be repeated for update 
    		if(osdParentShipment.getApprovalStatusCd() == null
    				&& osdParentShipment.getOverageProNbr() != null) {
				String overageProNbr = ProNumberHelper.validateProNumber(osdParentShipment.getOverageProNbr(), txnContext);
				if( shmOsdImageEntity != null
    				&& shmOsdImageEntity.getProNbrTxt() != null) {
					if(!StringUtils.equals(overageProNbr ,shmOsdImageEntity.getProNbrTxt())) {
						throw ExceptionBuilder
						.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
						.moreInfo(CURRENT_LOCATION, "Cannot update/change overage PRO number for an OS&D entry.")
						.log()
						.build();
					}
				}
				else {
					childProNumberExistValidation(txnContext, shmOsdImage, listHandlingUnit, rqstOsdId);
				}
    		}
    		
    		//CCS-8554: For Update - Parent PRO# for an overage OS&D entry should not be added to the record if the child PRO#/overage PRO# is not assigned to it first.
    		if(osdParentShipment.getParentProNbr() != null
    				&& shmOsdImageEntity != null
    				&& shmOsdImageEntity.getProNbrTxt() == null
    				&& osdParentShipment.getOverageProNbr() == null) {
    			throw ExceptionBuilder
    			.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
    			.moreInfo(CURRENT_LOCATION, "Overage PRO# needs to be assigned before the OS&D entry can be matched.")
    			.log()
    			.build();
    		}
    		
    		//CCS-8394: Set Approval Status code for Overage
    		if(osdParentShipment.getApprovalStatusCd() != null
    				&& osdParentShipment.getOverageProNbr() != null) {
           		// For Y : Validation for Approved		
    			if(elevenDigitProNum != null
    					&& osdParentShipment.getApprovalStatusCd().equals(OverageApprovalStatusCd.APPROVED)){
    				childProNumberExistValidation(txnContext, shmOsdImage, listHandlingUnit, rqstOsdId);
            	}
        		
    			//CCS-9484: remove validation in upsertOSD API 
/*        		// For D : If the approval status is 'D', SHM_OSD_IMAGE.PRO_NBR_TXT should be a PRO# existing in SHM_HANDLING_UNIT.CHILD_PRO_NBR_TXT only.
        		if(osdParentShipment.getApprovalStatusCd().equals(OverageApprovalStatusCd.DISAPPROVED)) {
        			if(listHandlingUnit.isEmpty()) {
        				throw ExceptionBuilder
        				.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
        				.moreInfo(CURRENT_LOCATION, String.format("Child PRO %s does not exist", osdParentShipment.getOverageProNbr()))
        				.log()
        				.build();
        			}
        		}*/
    		}
    	}
			
    	//CCS-8394 For N : If, at the time of create or update, approval status is N, overage PRO# cannot be updated.
    	if(isOverageProAllowed(osdParentShipment, shmOsdImageEntity)){
    		throw ExceptionBuilder
    		.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
    		.moreInfo(CURRENT_LOCATION, String.format("Overage PRO %s cannot be updated without approving the Overage Entry.", osdParentShipment.getOverageProNbr()))
    		.log()
    		.build();
    	}
    }

	private boolean isOverageProAllowed(OsdParentShipment osdParentShipment, ShmOsdImage shmOsdImageEntity) {
		return osdParentShipment != null
    			&& osdParentShipment.getOverageProNbr() != null
    			&& ((osdParentShipment.getApprovalStatusCd() != null
    			&& osdParentShipment.getApprovalStatusCd().equals(OverageApprovalStatusCd.NEEDS_APPROVAL))
    					|| (shmOsdImageEntity != null
    					&& shmOsdImageEntity.getApprovalStatusCd() != null    				
    					&& shmOsdImageEntity.getApprovalStatusCd().equals(OverageApprovalStatusCdTransformer.toCode(OverageApprovalStatusCd.NEEDS_APPROVAL)))
    					&& (!hasApprovalStatusCds.contains(osdParentShipment.getApprovalStatusCd())
    							&& !hasApprovalStatusCds.contains(OverageApprovalStatusCdTransformer.toEnum(shmOsdImageEntity.getApprovalStatusCd()))));
	}

	private void childProNumberExistValidation(TransactionContext txnContext, ShmOsdImage shmOsdImage,
			List<ShmHandlingUnit> listHandlingUnit, Long rqstOsdId) throws ValidationException {
		if(shmOsdImage != null
				&& shmOsdImage.getShmOsdHeader() != null
				&& rqstOsdId != shmOsdImage.getShmOsdHeader().getOsdId()) {
			throw ExceptionBuilder
			.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
			.moreInfo(CURRENT_LOCATION, "Overage PRO is already in use.")
			.log()
			.build();
		}
			
		if(!listHandlingUnit.isEmpty()) {
			throw ExceptionBuilder
			.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
			.moreInfo(CURRENT_LOCATION, "This child PRO number cannot be used as it is already associated to a Parent PRO.")
			.log()
			.build();
		}
	}

    private static final List<CloseReasonCd> CLOSE_REASON_CODES_FOR_SHORT = Arrays.asList(
            CloseReasonCd.FINAL_DLVR_WITH_ASZZ,
            CloseReasonCd.FINAL_DLVR_WITH_PSZZ,
            CloseReasonCd.MATCH_FOUND,
            CloseReasonCd.FINAL_DLVR_IN_FULL,
            CloseReasonCd.DUP_ENTRY_CNCL_ENTRY);

    private static final List<CloseReasonCd> CLOSE_REASON_CODES_FOR_REFUSED_DAMAGED = Arrays.asList(
            CloseReasonCd.DISPOSED_OF_AT_SIC,
            CloseReasonCd.DISP_FROM_SHIPPER,
            CloseReasonCd.DUP_ENTRY_CNCL_ENTRY,
            CloseReasonCd.DISP_TO_SEND_TO_CMK,
            CloseReasonCd.LOA_TO_RTRN_TO_SHIPR,
            CloseReasonCd.SHPMNT_BLD_RDY_MOVE);

    private static final List<CloseReasonCd> CLOSE_REASON_CODES_FOR_OVERAGE = Arrays.asList(
            CloseReasonCd.MATCH_FOUND,
            CloseReasonCd.DISPOSED_OF_AT_SIC,
            CloseReasonCd.DUP_ENTRY_CNCL_ENTRY,
            CloseReasonCd.DISP_TO_SEND_TO_CMK,
            CloseReasonCd.TAGGED_TO_PARENT_PRO,
            CloseReasonCd.PRO_NBR_FOUND);

    private static String formatMessageToInvalidCloseReasonCodesMessage(String osdCategoryCode, List<CloseReasonCd> closeReasonCdList) {

        String closeReasonCdListString = closeReasonCdList.stream()
                .map(closeReasonCd -> closeReasonCd.toString())
                .collect(Collectors.joining(",", "{", "}"));

        return "For " + osdCategoryCode + " only Closed Reason Codes " + closeReasonCdListString + " are valid";

    }

    private void checkClosingReasonByOSDCategory(
            OsdCategoryCd osdCategoryCd,
            CloseReasonCd closeReasonCd,
            List<MoreInfo> moreInfos) {

        if (OsdCategoryCd.SHORT.equals(osdCategoryCd)) {

            if (!CLOSE_REASON_CODES_FOR_SHORT.contains(closeReasonCd)) {

                addMoreInfo(moreInfos, "closeReasonCd",
                        formatMessageToInvalidCloseReasonCodesMessage("SHORT", CLOSE_REASON_CODES_FOR_SHORT));

            }

        }

        if (refusedDamagedCds.contains(osdCategoryCd)) {

            if (!CLOSE_REASON_CODES_FOR_REFUSED_DAMAGED.contains(closeReasonCd)) {

                addMoreInfo(moreInfos, "closeReasonCd",
                        formatMessageToInvalidCloseReasonCodesMessage("REFUSED and DAMAGE", CLOSE_REASON_CODES_FOR_REFUSED_DAMAGED));

            }

        }

        if (OsdCategoryCd.OVERAGE.equals(osdCategoryCd)) {

            if (!CLOSE_REASON_CODES_FOR_OVERAGE.contains(closeReasonCd)) {

                addMoreInfo(moreInfos, "closeReasonCd",
                        formatMessageToInvalidCloseReasonCodesMessage("OVERAGE", CLOSE_REASON_CODES_FOR_OVERAGE));

            }

        }

    }

}