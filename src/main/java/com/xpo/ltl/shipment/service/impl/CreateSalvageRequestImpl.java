package com.xpo.ltl.shipment.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.client.common.Response;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.humanresource.v1.Employee;
import com.xpo.ltl.api.location.v2.GetLocationReferenceDetailsResp;
import com.xpo.ltl.api.location.v2.GetRefSicAddressResp;
import com.xpo.ltl.api.location.v2.ListLocationFeaturesResp;
import com.xpo.ltl.api.pronumberreengineering.client.v1.ProNumberReengineeringClient;
import com.xpo.ltl.api.pronumberreengineering.v1.GetNextAvailableProNumberByTypeResp;
import com.xpo.ltl.api.pronumberreengineering.v1.ProNbrTypeCd;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdImage;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageQuantify;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageRequest;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageRqst;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageStatus;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.BillClassCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.SalvageRequestStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.SalvageRequestTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.AsMatchedParty;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.BillClassCd;
import com.xpo.ltl.api.shipment.v2.Commodity;
import com.xpo.ltl.api.shipment.v2.CreateNonRevenueShipmentRqst;
import com.xpo.ltl.api.shipment.v2.CreateSalvageRequestResp;
import com.xpo.ltl.api.shipment.v2.CreateSalvageRequestRqst;
import com.xpo.ltl.api.shipment.v2.HandlingUnit;
import com.xpo.ltl.api.shipment.v2.HandlingUnitTypeCd;
import com.xpo.ltl.api.shipment.v2.MatchedPartyTypeCd;
import com.xpo.ltl.api.shipment.v2.OsdImage;
import com.xpo.ltl.api.shipment.v2.SalvageRequest;
import com.xpo.ltl.api.shipment.v2.SalvageRequestStatusCd;
import com.xpo.ltl.api.shipment.v2.SalvageRequestTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentSkeleton;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmOsdImageSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmSalvageQualifySubDAO;
import com.xpo.ltl.shipment.service.dao.ShmSalvageQuantifySubDAO;
import com.xpo.ltl.shipment.service.dao.ShmSalvageRequestSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmSalvageRequestTypeSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmSalvageRqstSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmSalvageStatusSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import com.xpo.ltl.shipment.service.util.ShmSalvageUtil;


@RequestScoped
public class CreateSalvageRequestImpl {


	@Inject
	private ShmSalvageRequestSubDAO shmSalvageRequestSubDAO;
	
	@Inject
	private ShmSalvageRqstSubDAO shmSalvageRqstSubDAO;
	
	@Inject
	private ShmSalvageStatusSubDAO shmSalvageStatusSubDAO;
	
	@Inject
	private ShmSalvageQuantifySubDAO shmSalvageQuantifySubDAO;
	
	@Inject
	private ShmSalvageQualifySubDAO shmSalvageQualifySubDAO;
	
	@Inject
	private ShmSalvageRequestTypeSubDAO shmSalvageRequestTypeSubDAO;
	
	@Inject
	private ExternalRestClient externalRestClient;
	
	@Inject
	private ShmOsdImageSubDAO ShmOsdImageDao;
	
	@Inject
    private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;
	

	@Inject
	private ShmShipmentSubDAO shmShipmentSubDAO;

	
	@PersistenceContext(unitName = "ltl-java-shipment-rpt-jaxrs")
	private EntityManager rptEntityManager;

    private static final String CURRENT_LOCATION = CreateSalvageRequestImpl.class.getCanonicalName();
	private static final Double DEFAULT_VALUE = 0.0;
	private static final Logger LOGGER = LogManager.getLogger(CreateSalvageRequestImpl.class);
	
	
	public CreateSalvageRequestResp createSalvageRequest(
		final CreateSalvageRequestRqst createSalvageRequestRqst,
		final TransactionContext txnContext,
		final EntityManager entityManager) throws ServiceException {
		SalvageRequest salvageRequest = createSalvageRequestRqst.getSalvageRequest();
		CreateSalvageRequestResp createSalvageRequestResp = new CreateSalvageRequestResp();
		performValidation(createSalvageRequestRqst, txnContext, entityManager);
		List<String> proNumbers = createSalvageRequestRqst.getChildProNbrs();
		String newProNum = getNewProNumber(salvageRequest, txnContext, entityManager, proNumbers);

		salvageRequest.setProNbr(ProNumberHelper.toNineDigitPro(newProNum, txnContext));
		getCubicFt(salvageRequest);
		ShmSalvageRqst shmSalvageRqst = null;
		
		ShmSalvageRequest existingSalvage = shmSalvageRequestSubDAO.findBySalvageProNbr(salvageRequest.getProNbr(), entityManager);
		if (existingSalvage != null) {
		    throw ExceptionBuilder
		    .exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
		    .moreInfo(CURRENT_LOCATION, "The PRO is already in use")
		    .log()
		    .build();
		}
		if (StringUtils.isNotBlank(salvageRequest.getSicCd())) {
			 try {
				 GetLocationReferenceDetailsResp resp = externalRestClient.getLocationReferenceDetails(salvageRequest.getSicCd(), txnContext);
				 if(!(resp != null && resp.getCompanyOperations() != null && StringUtils.isNotBlank(resp.getCompanyOperations().getFreightOperationsType()) && !resp.getCompanyOperations().getFreightOperationsType().equals("T"))) {
					 throw ExceptionBuilder
					    .exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
					    .moreInfo(CURRENT_LOCATION, "The SIC code is not a valid value.")
					    .log()
					    .build(); 
				 }	         } catch (ServiceException e) {
	        	 throw ExceptionBuilder
				    .exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
				    .moreInfo(CURRENT_LOCATION, "The SIC code is not a valid value.")
				    .log()
				    .build();
	         }
		}
		
		salvageRequest.setFreightConditionCd(mapToCode(salvageRequest.getFreightConditionCd(), txnContext));
        
		boolean isBackSync = false;
		try {
		    ListLocationFeaturesResp locFeatureSetting = externalRestClient.getLocFeatureSetting("SLVG_BACKSYNC", txnContext);
		    isBackSync = locFeatureSetting.getLocationFeatures() != null && BasicTransformer.toBoolean((locFeatureSetting.getLocationFeatures().get(0).getSettingValue()));
		} catch (ServiceException e) {
            LOGGER.warn("Feature not available. No backsync to old salvage application will occur.");
		}
		
		if(isBackSync) {
			shmSalvageRqst = getShmSalvageRqst(salvageRequest, txnContext);
			String requestorUser = AuditInfoHelper.getTransactionUserId(txnContext);
			Employee employee = externalRestClient.getEmployeeDetailsByEmployeeId(requestorUser, txnContext);
			shmSalvageRqst.setRequestorEmail(getEmployeeEmail(employee));
			shmSalvageRqst.setRequestor(getEmployeeName(employee));
			shmSalvageRqst.setRequestorDisplayName(getEmployeeName(employee));
			shmSalvageRqst.setSalvageStatus(shmSalvageStatusSubDAO.findById("Open", rptEntityManager));
			shmSalvageRqst.setSalvageQualify(shmSalvageQualifySubDAO.findById("Overage", rptEntityManager));
			String value = ShmSalvageUtil.getOldDBRequestTypeValue(SalvageRequestTypeCdTransformer.toCode(salvageRequest.getSalvageRequestTypeCd()));
			shmSalvageRqst.setSalvageRequestType(shmSalvageRequestTypeSubDAO.findById(value, rptEntityManager));
			shmSalvageRqstSubDAO.setRequestId(shmSalvageRqst, "SHM_SALVAGE_REQ_SEQ", rptEntityManager);
			shmSalvageRqstSubDAO.save(shmSalvageRqst, rptEntityManager);
		}
		AuditInfo auditInfo = getAuditInfo(txnContext);
		salvageRequest.setSalvageRequestId(shmSalvageRqst != null ? shmSalvageRqst.getRequestId() : shmSalvageRqstSubDAO.getSalvageRequestId("SHM_SALVAGE_REQ_SEQ", rptEntityManager));
		salvageRequest.setAbeyanceInd(BooleanUtils.isTrue(salvageRequest.getAbeyanceInd()));
		ShmSalvageRequest shmSalvageRequest = new ShmSalvageRequest();
		DtoTransformer.setAuditInfo(shmSalvageRequest, auditInfo);
		DtoTransformer.toShmSalvageRequest(salvageRequest, shmSalvageRequest);
		List<OsdImage> osdImages = salvageRequest.getOsdImage();
		shmSalvageRequest.setStatusCd(SalvageRequestStatusCdTransformer.toCode(SalvageRequestStatusCd.OPEN));
		shmSalvageRequest.setSpecialHandlingInd(salvageRequest.getSpecialHandlingInd() != null && salvageRequest.getSpecialHandlingInd() ? "Y" : "N");
		shmSalvageRequest.setVisibleInd(salvageRequest.getVisibleInd() != null && salvageRequest.getVisibleInd() ? "Y" : "N");
		shmSalvageRequest.setSearchInd(salvageRequest.getSearchInd() != null && salvageRequest.getSearchInd() ? "Y" : "N");
		shmSalvageRequest.setDamageInfoInd(salvageRequest.getDamageInformationInd() != null && salvageRequest.getDamageInformationInd() ? "Y" : "N");
		shmSalvageRequest.setHoldForVendorInd(salvageRequest.getHoldForVendorInd() != null && salvageRequest.getHoldForVendorInd() ? "Y" : "N");
		shmSalvageRequest.setAbeyanceInd("N");
		shmSalvageRequest.setSalvageRequestId(salvageRequest.getSalvageRequestId());
		shmSalvageRequest.setQualifyTypeCd(salvageRequest.getQualifyTypeCd());
		String salvageDescription = CollectionUtils.isEmpty(osdImages) ? "" : osdImages.get(0).getDescription() ;
		shmSalvageRequest.setFreightDescription(salvageDescription);
		shmSalvageRequestSubDAO.save(shmSalvageRequest, entityManager);

		for(OsdImage osdImage : osdImages) {
			ShmOsdImage hdrEntity = ShmOsdImageDao.findById(osdImage.getInstId(), entityManager);
			hdrEntity.setOrigProNbrTxt(handleProNum(newProNum, txnContext));
			hdrEntity.setShmSalvageRequest(shmSalvageRequest);
			ShmOsdImageDao.save(hdrEntity, entityManager);
		}
		createSalvageRequestResp.setSalvageRequest(salvageRequest);
		return createSalvageRequestResp;
		
	}
	
	private String mapToCode(String freightConditionCd, TransactionContext txnContext) throws ValidationException{
        if(StringUtils.trimToEmpty(freightConditionCd).length() == 1) {
            //received a code
            if(!Lists.newArrayList("0","1","2","3","4","5").contains(freightConditionCd)) {
                throw ExceptionBuilder
                .exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
                .moreInfo(CURRENT_LOCATION, "Freight condition is invalid.")
                .log()
                .build();
            }
            return freightConditionCd;
        }
        Map<String, String> freightConditionMap = getFreightConditionMap();
        
        if (freightConditionMap.get(freightConditionCd) == null) {
            throw ExceptionBuilder
            .exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
            .moreInfo(CURRENT_LOCATION, "Freight condition is invalid.")
            .log()
            .build();
        }
        return freightConditionMap.get(freightConditionCd);
        
    }

    private String getNewProNumber(SalvageRequest salvageRequest, TransactionContext txnContext, EntityManager entityManager, List<String> proNumbers)throws ServiceException {
		List<OsdImage> osdImages = salvageRequest.getOsdImage();
		boolean processLegacyPro = false;
		if (osdImages!= null && ProNumberHelper.isYellowPro(osdImages.get(0).getProNbr())) {
			try {
				salvageRequest.setProNbr(getNewGCBZPro(txnContext));
			} catch (com.xpo.ltl.api.client.exception.ServiceException | ServiceException e) {
				throw ExceptionBuilder
				.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
				.moreInfo(CURRENT_LOCATION, e.getMessage())
				.log()
				.build();
			}
		} else if(StringUtils.isNotEmpty(salvageRequest.getProNbr())){
			salvageRequest.setProNbr(salvageRequest.getProNbr());
		} else {
			OsdImage OsdImage = salvageRequest.getOsdImage().get(0);
			salvageRequest.setProNbr(OsdImage.getProNbr());
			processLegacyPro = true;
		}
		
		ShmShipment shmShipment = shmShipmentSubDAO.findByIdOrProNumber(ProNumberHelper.toElevenDigitPro(salvageRequest.getProNbr(), txnContext),null,  entityManager);
		if(shmShipment != null) {
		    if (processLegacyPro) {
		        if (BillClassCd.GENERAL_CLAIMS_BUS_SHPMT == BillClassCdTransformer.toEnum(shmShipment.getBillClassCd())) {
		            if (StringUtils.isNotBlank(shmShipment.getMvmtStatCd()) && shmShipment.getMvmtStatCd().equals("5")) {
	                    throw ExceptionBuilder
	                    .exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
	                    .moreInfo(CURRENT_LOCATION, "The PRO is already in use")
	                    .log()
	                    .build();
		            }
		        } else {
	                throw ExceptionBuilder
	                .exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
	                .moreInfo(CURRENT_LOCATION, "The PRO is already in use")
	                .log()
	                .build();
		        }
		    } else {
		        throw ExceptionBuilder
		        .exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
		        .moreInfo(CURRENT_LOCATION, "The PRO is already in use")
		        .log()
		        .build();
		    }
		}
		if(!CollectionUtils.isEmpty(proNumbers)) {
			String elevenDigitProNumber = ProNumberHelper.toElevenDigitPro(salvageRequest.getProNbr(), txnContext);
			if(!ProNumberHelper.isBluePro(elevenDigitProNumber)) {
				throw ExceptionBuilder
				.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
				.moreInfo(CURRENT_LOCATION, "Not a valid parent pro number")
				.log()
				.build();
			}
			for(String proNumber : proNumbers) {
				if(!ProNumberHelper.isYellowPro(ProNumberHelper.toElevenDigitPro(proNumber, txnContext))) {
					throw ExceptionBuilder
					.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
					.moreInfo(CURRENT_LOCATION, "Not a valid child pro number")
					.log()
					.build();
				}	
			}
			List<ShmHandlingUnit> listHandlingUnits = shmHandlingUnitSubDAO.findByChildProNumberList(new HashSet<String>(proNumbers), entityManager);
			if(!CollectionUtils.isEmpty(listHandlingUnits)) {
				throw ExceptionBuilder
				.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
				.moreInfo(CURRENT_LOCATION, ProNumberHelper.toTenDigitPro(listHandlingUnits.get(0).getChildProNbrTxt())+" is already exist in handling unit")
				.log()
				.build();
			}
		}

		return salvageRequest.getProNbr(); 
	}

	private String getNewGCBZPro(TransactionContext txnContext)
			throws ServiceException, com.xpo.ltl.api.client.exception.ServiceException {
		Response<GetNextAvailableProNumberByTypeResp> proNumberResp;
		ProNumberReengineeringClient client = externalRestClient.getProNumberReengineeringClient(txnContext);
		proNumberResp = client.getNextAvailableProNumberByType(ProNbrTypeCd.GENERAL_CLAIMS_BUSINESS);
		return proNumberResp.getData().getProNbr();
	}
	
	private static AuditInfo getAuditInfo(final TransactionContext txnContext) {
		AuditInfo auditInfo = new AuditInfo();
		AuditInfoHelper.setCreatedInfo(auditInfo, txnContext);
		AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);
		return auditInfo;
	}
	
	protected ShmSalvageRqst getShmSalvageRqst(SalvageRequest salvageRequest, TransactionContext txnContext) throws ServiceException {
		ShmSalvageRqst shmSalvageRqst = new ShmSalvageRqst();
		shmSalvageRqst.setRequestor(String.valueOf(salvageRequest.getSalvageRequestId()));// user id 
		shmSalvageRqst.setRequestorDisplayName(salvageRequest.getSalvageRequestName()); //request name
		shmSalvageRqst.setRequestorSic(salvageRequest.getSicCd()); // user sic
		shmSalvageRqst.setRequestDate(new Date());// date
		shmSalvageRqst.setApplicationType("SALVAGE");
		shmSalvageRqst.setOriginalPro("NONE");// uploaded one
		shmSalvageRqst.setSalvagePro(salvageRequest.getProNbr());// new created 
		shmSalvageRqst.setPkgPieces(String.valueOf(salvageRequest.getPiecesCount()));// pieces
		shmSalvageRqst.setOverallCondition(salvageRequest.getFreightConditionCd());//fridge condi
		shmSalvageRqst.setEstimateValue(salvageRequest.getEstimatedValueCd());//estimate value
		shmSalvageRqst.setUpdateDate(new Date());// date
		shmSalvageRqst.setRequestCc("comment");//comment
		shmSalvageRqst.setIdentify3(getCommentPro(salvageRequest, txnContext));
		shmSalvageRqst.setPkgDimensionL(String.valueOf(salvageRequest.getLengthNbr()));
		shmSalvageRqst.setPkgDimensionW(String.valueOf(salvageRequest.getWidthNbr()));
		shmSalvageRqst.setPkgDimensionH(String.valueOf(salvageRequest.getHeightNbr()));
		shmSalvageRqst.setPkgDimensionC(String.valueOf(salvageRequest.getCubeNbr()));
		shmSalvageRqst.setPkgWeight(String.valueOf(salvageRequest.getWeightLbs()));
		shmSalvageRqst.setBrand(salvageRequest.getBrand());
		shmSalvageRqst.setOriginSic(salvageRequest.getSicCd());
		shmSalvageRqst.setSalvageQuantify(getShmSalvageQuantify(salvageRequest.getPackagingCd(), rptEntityManager));
		shmSalvageRqst.setField1(BasicTransformer.toString(BooleanUtils.isTrue(salvageRequest.getVisibleInd())));
		shmSalvageRqst.setField2(BasicTransformer.toString(BooleanUtils.isTrue(salvageRequest.getSearchInd())));
		shmSalvageRqst.setField3(BasicTransformer.toString(BooleanUtils.isTrue(salvageRequest.getDamageInformationInd())));
		shmSalvageRqst.setField7(BasicTransformer.toString(BooleanUtils.isTrue(salvageRequest.getSpecialHandlingInd())));
		shmSalvageRqst.setTracked("N");
		shmSalvageRqst.setComputerName("Unknown");
		setSalvageStatus(shmSalvageRqst);
		return shmSalvageRqst;
	}
	
	private ShipmentSkeleton getShipmentSkeleton(final TransactionContext txnContext, SalvageRequest salvageRequest,
			 CreateNonRevenueShipmentRqst request, boolean isChildPro) throws ValidationException {
		ShipmentSkeleton shipmentSkeleton = new ShipmentSkeleton();
		shipmentSkeleton.setParentProNbr(ProNumberHelper.toElevenDigitPro(salvageRequest.getProNbr(), txnContext));
		shipmentSkeleton.setRequestingSicCd(salvageRequest.getSicCd());
		shipmentSkeleton.setDestinationTerminalSicCd(salvageRequest.getSalvageRequestTypeCd() == SalvageRequestTypeCd.MOVE_TO_CSV ? "XTN" : "CMK");
        Long piecesCount = salvageRequest.getPiecesCount();
        shipmentSkeleton.setTotalPiecesCount(BigInteger.valueOf(piecesCount));
		shipmentSkeleton.setTotalWeightLbs(salvageRequest.getWeightLbs());
		shipmentSkeleton.setTotalVolumeCubicFeet(salvageRequest.getCubeNbr() != null ? salvageRequest.getCubeNbr().doubleValue() : 0);
		shipmentSkeleton.setLastMoveRptgSicCd(salvageRequest.getSicCd());
        shipmentSkeleton.setMotorizedPiecesKnownInd(true);
        boolean isPLTSKD = Arrays.asList("PLT", "SKD").contains(salvageRequest.getPackagingCd());
        if (isPLTSKD) {
            long motorMvsCount = piecesCount <= 2 ? piecesCount : 1;
            shipmentSkeleton.setMotorizedPiecesCount(BigInteger.valueOf(motorMvsCount));
        } else {
            shipmentSkeleton.setMotorizedPiecesCount(BigInteger.ZERO);
            shipmentSkeleton.setLoosePiecesCount(BigInteger.valueOf(piecesCount));
        }
		if(!isChildPro) {
			shipmentSkeleton.setHandlingUnitExemptionInd(true);
			shipmentSkeleton.setHandlingUnitExemptionReason("Exemption this for salvage");
		}	
		request.setBillClassCd(BillClassCd.GENERAL_CLAIMS_BUS_SHPMT);
		return shipmentSkeleton;
	}

	private void createCommodityAndAddToList(SalvageRequest salvageRequest, List<Commodity> commodityList) {
		Commodity comm = new Commodity();
		comm.setPiecesCount(BigInteger.valueOf(salvageRequest.getPiecesCount()));
		comm.setDescription("SALVAGE OVERAGE SHIPMENT");
		comm.setSourceCd("1");
        comm.setWeightLbs(salvageRequest.getWeightLbs());
        comm.setSequenceNbr((short)1);
		commodityList.add(comm);
	}

	private void getShipper(final TransactionContext txnContext, SalvageRequest salvageRequest,
			AsMatchedParty shipper) throws ServiceException {
		GetLocationReferenceDetailsResp resp = externalRestClient.getLocationReferenceDetails(salvageRequest.getSicCd(), txnContext);
		GetRefSicAddressResp originLocation = externalRestClient.getRefSicAddress(salvageRequest.getSicCd(), txnContext);

		if(null != originLocation 
		&& null != originLocation.getLocAddress()) {
			shipper.setName1(resp != null ? resp.getLocationReference().getSicName():" ");
			shipper.setAddress(originLocation.getLocAddress().getAddr1());
            shipper.setCity(originLocation.getLocAddress().getCityName());
			shipper.setCountryCd(originLocation.getLocAddress().getCountryCd());
			shipper.setStateCd(originLocation.getLocAddress().getCountrySubdivisionCd());
			shipper.setZip6(originLocation.getLocAddress().getPostalCd());
			shipper.setZip4RestUs(originLocation.getLocAddress().getPostalExtCd());
			shipper.setCountryCd(shipper.getCountryCd().equalsIgnoreCase("CA") ? "CN" : shipper.getCountryCd());
			shipper.setTypeCd(MatchedPartyTypeCd.SHPR);
		}
	}

	private void getConsignee(final TransactionContext txnContext, ShipmentSkeleton shipmentSkeleton,
			AsMatchedParty consignee) throws ServiceException {
		GetLocationReferenceDetailsResp consigneeLocationResp = externalRestClient.getLocationReferenceDetails(shipmentSkeleton.getDestinationTerminalSicCd(), txnContext);
		GetRefSicAddressResp consigneeLocation = externalRestClient.getRefSicAddress(shipmentSkeleton.getDestinationTerminalSicCd(), txnContext);
		if(null != consigneeLocation 
		        && null != consigneeLocation.getLocAddress()) {
				
			consignee.setName1(consigneeLocationResp != null ? consigneeLocationResp.getLocationReference().getSicName():" ");
			consignee.setAddress(consigneeLocation.getLocAddress().getAddr1());
			consignee.setCity(consigneeLocation.getLocAddress().getCityName());
			consignee.setCountryCd(consigneeLocation.getLocAddress().getCountryCd());
			consignee.setStateCd(consigneeLocation.getLocAddress().getCountrySubdivisionCd());
			consignee.setZip6(consigneeLocation.getLocAddress().getPostalCd());
			consignee.setZip4RestUs(consigneeLocation.getLocAddress().getPostalExtCd());
			consignee.setCountryCd(consignee.getCountryCd().equalsIgnoreCase("CA") ? "CN" : consignee.getCountryCd());
			consignee.setTypeCd(MatchedPartyTypeCd.CONS);
		}
	}
	
	private static void getCubicFt(SalvageRequest salvageRequest) {
		if((isDoubleValueGreaterThanZero(salvageRequest.getLengthNbr()) && 
				isDoubleValueGreaterThanZero(salvageRequest.getWidthNbr()) &&
				isDoubleValueGreaterThanZero(salvageRequest.getHeightNbr())) && isDoubleValueZero(salvageRequest.getCubeNbr())) {
			if(isDoubleValueZero(salvageRequest.getLengthNbr())) {
				salvageRequest.setLengthNbr(DEFAULT_VALUE);
			}
			if(isDoubleValueZero(salvageRequest.getHeightNbr())) {
				salvageRequest.setHeightNbr(DEFAULT_VALUE);
			}
			if(isDoubleValueZero(salvageRequest.getWidthNbr())) {
				salvageRequest.setWidthNbr(DEFAULT_VALUE);
			}
			BigDecimal volCft = new BigDecimal(salvageRequest.getLengthNbr()).
					multiply(new BigDecimal(salvageRequest.getHeightNbr())).
					multiply(new BigDecimal(salvageRequest.getWidthNbr())).divide(new BigDecimal(1728), 2, RoundingMode.HALF_EVEN);
			salvageRequest.setCubeNbr(volCft.doubleValue());
		}
	}
	
	private static boolean isDoubleValueZero(Double value) {
		if(value == null || (value != null && value == 0.0)) {
			return true;
		}
		return false;
	}
	
	private static boolean isDoubleValueGreaterThanZero(Double value) {
		if(value != null && value > 0.0) {
			return true;
		}
		return false;
	}
	
	private String getCommentPro(SalvageRequest salvageRequest, TransactionContext txnContext) throws ServiceException {
		String comment = " ";
		String elevenDigitProNumber;
		try {
			elevenDigitProNumber = ProNumberHelper.toElevenDigitPro(salvageRequest.getOsdImage().get(0).getProNbr(), txnContext);
			if(ProNumberHelper.isBluePro(elevenDigitProNumber)) {
				return comment;
			}
		} catch (ValidationException e) {
			throw ExceptionBuilder
			.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
			.moreInfo("proNumber", "Invalid PRO Number")
			.log()
			.build();
		}

		for(OsdImage osdImage : salvageRequest.getOsdImage()) {
			comment+=osdImage.getProNbr() +" ";	
		}
		return comment;
	}
	private void setSalvageStatus(ShmSalvageRqst shmSalvageRqst) {
		ShmSalvageStatus salvageStatus = new ShmSalvageStatus();
		salvageStatus.setStatus("Open");
		shmSalvageRqst.setSalvageStatus(salvageStatus);
	}
	
	private String getEmployeeEmail(Employee employee) {
		String employeeEmail = employee != null ? employee.getEmailAddress() : "overage@xpo.com";
		return employeeEmail;
	}
	
	private String handleProNum(String proNumber, TransactionContext txnContext) throws ServiceException {
		try {
			if(StringUtils.EMPTY.equals(proNumber)) {
				return StringUtils.EMPTY;
			}
			String elevenDigitProNum = ProNumberHelper.validateProNumber(proNumber, txnContext);
			if(StringUtils.isNotBlank(elevenDigitProNum)) {
				if(ProNumberHelper.isYellowPro(elevenDigitProNum)) {
					return ProNumberHelper.isValidChildProNum(elevenDigitProNum);
				}
				if(ProNumberHelper.isBluePro(elevenDigitProNum)) {
					return ProNumberHelper.formatProNineDigit(elevenDigitProNum);
				}
			}
		} catch (ServiceException ex) {
			throw ExceptionBuilder
			.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
			.moreInfo("proNumber", "Invalid PRO Number")
			.log()
			.build();
		}
		return null;
	}
	
	private void performValidation(CreateSalvageRequestRqst createSalvageRequestRqst, TransactionContext txnContext, EntityManager entityManager)  throws ServiceException{
		if(createSalvageRequestRqst.getSalvageRequest() == null) {
			throw ExceptionBuilder
			.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
			.moreInfo(CURRENT_LOCATION, "Not a valid salvage request")
			.log()
			.build();
		}
		if(CollectionUtils.isEmpty(createSalvageRequestRqst.getSalvageRequest().getOsdImage())) {
			throw ExceptionBuilder
			.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
			.moreInfo(CURRENT_LOCATION, "No shipment selected for salvage")
			.log()
			.build();
		}
		List<OsdImage> osdImages = createSalvageRequestRqst.getSalvageRequest().getOsdImage();
		for (OsdImage osdImage : osdImages) {		
			ShmOsdImage overageImageHeader = ShmOsdImageDao.findById(osdImage.getInstId(), entityManager);
			if(overageImageHeader!= null && overageImageHeader.getShmSalvageRequest() != null) {
				throw ExceptionBuilder
				.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
				.moreInfo(CURRENT_LOCATION, "The shipment has already salvaged")
				.log()
				.build();
			}
		}

	}
	
	private String getEmployeeName(Employee employee) {
		return  employee != null ? employee.getBasicInfo().getFirstName() +" "+ employee.getBasicInfo().getLastName() : "SYSTEM";
	}

    public void createBilledGCBZShipment(SalvageRequest salvageRequest, TransactionContext txnContext, EntityManager entityManager, List<String> proNumbers) 
            throws ServiceException {
        if(salvageRequest == null || StringUtils.isEmpty(salvageRequest.getProNbr())) {
            return;
        }
        ShmShipment shmShipment = shmShipmentSubDAO.findByIdOrProNumber(ProNumberHelper.toElevenDigitPro(salvageRequest.getProNbr(), txnContext),null,  entityManager);
        if (shmShipment != null) {
            return; //if existing, do not attempt to create again
        }

        List<OsdImage> osdImages = salvageRequest.getOsdImage();
        boolean isChildPro = false;
        //GENERAL_CLAIMS_BUS_SHPMT
        List<Commodity> commodityList = new ArrayList<Commodity>();
        if(!CollectionUtils.isEmpty(osdImages)) {
            String elevenDigitProNumber = ProNumberHelper.toElevenDigitPro(osdImages.get(0).getProNbr(), txnContext);
            if(StringUtils.isNotEmpty(elevenDigitProNumber) && ProNumberHelper.isYellowPro(elevenDigitProNumber) && CollectionUtils.isEmpty(proNumbers)) {
                proNumbers = osdImages.stream().map(OsdImage::getProNbr).collect(Collectors.toList());
            }
        }

        Double weightLbs = 0D;
        if(CollectionUtils.isEmpty(proNumbers)) {
            proNumbers = new ArrayList<>();
        } else {
            isChildPro = true;
            weightLbs = getWeightForEachPro(salvageRequest.getWeightLbs(), proNumbers.size());
        }
        List<HandlingUnit> listHandlingUnit = new ArrayList<HandlingUnit>();
        int seq = 1;
        for(String proNumber : proNumbers) {
            HandlingUnit handUnit = new HandlingUnit();
            handUnit.setCurrentSicCd(salvageRequest.getSicCd());
            handUnit.setChildProNbr(ProNumberHelper.toElevenDigitPro(proNumber, txnContext));
            handUnit.setParentProNbr(ProNumberHelper.toElevenDigitPro(salvageRequest.getProNbr(), txnContext));
            handUnit.setHandlingMovementCd("NORMAL");
            handUnit.setTypeCd(HandlingUnitTypeCd.MOTOR);
            handUnit.setDimensionTypeCd(" ");
            handUnit.setStackableInd(false);
            handUnit.setHeightNbr(new Double(0));
            handUnit.setWidthNbr(new Double(0));
            handUnit.setLengthNbr(new Double(0));
            handUnit.setVolumeCubicFeet(new Double(0));
            handUnit.setWeightLbs(weightLbs);
            handUnit.setListActionCd(ActionCd.ADD);
            handUnit.setSequenceNbr(BigInteger.valueOf(seq++));
            listHandlingUnit.add(handUnit);
        }
        CreateNonRevenueShipmentRqst request = new CreateNonRevenueShipmentRqst();
        ShipmentSkeleton shipmentSkeleton = getShipmentSkeleton(txnContext, salvageRequest, request, isChildPro);
        request.setShipmentSkeleton(shipmentSkeleton);
        request.getShipmentSkeleton().setHandlingUnits(listHandlingUnit);
        createCommodityAndAddToList(salvageRequest, commodityList);
        request.setCommodities(commodityList);
        List <AsMatchedParty> asMatchedPartyList = new ArrayList<>();
        AsMatchedParty shipper = new AsMatchedParty();
        AsMatchedParty consignee = new AsMatchedParty();

        getShipper(txnContext, salvageRequest, shipper);
        getConsignee(txnContext, shipmentSkeleton, consignee);
        asMatchedPartyList.add(shipper);
        asMatchedPartyList.add(consignee);

        request.setAsMatchedParties(asMatchedPartyList);    
        try {
            externalRestClient.createNonRevenueShipment(request, txnContext);
        } catch (ServiceException ex) {
            throw ExceptionBuilder
            .exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
            .moreInfo(CURRENT_LOCATION, "Error while billing pro number")
            .log()
            .build();               
        }
    }

    private Double getWeightForEachPro(Double weightLbs, int proNumberSize) {
        return proNumberSize == 0 ? 0D : (weightLbs / proNumberSize);
    }

    private ShmSalvageQuantify getShmSalvageQuantify(String packingCd, EntityManager entityManager) {
    	return shmSalvageQuantifySubDAO.findById(getOldDBPackagingValue(packingCd), entityManager);
    }
    
    public static String getOldDBPackagingValue(String code) {
        Map<String, String> packagingMap = getPackagingMap();
        if (packagingMap.get(code) == null)
            return "Other";
        return packagingMap.get(code);
    }
    
    private static Map<String, String> getPackagingMap(){
        Map<String, String> requestTypeMap = new HashMap<>();
        requestTypeMap.put("CTN", "Carton");
        requestTypeMap.put("CRT", "Crated");
        requestTypeMap.put("DRM", "Drum");
        requestTypeMap.put("LSE", "Loose");
        requestTypeMap.put("PLT", "Palletized");
        requestTypeMap.put("BDL", "Bundled");
        return requestTypeMap;
    }

    private static Map<String, String> getFreightConditionMap(){
        Map<String, String> requestTypeMap = new HashMap<>();
        requestTypeMap.put("Trash", "0");
        requestTypeMap.put("Bad", "1");
        requestTypeMap.put("Average", "2");
        requestTypeMap.put("Good", "3");
        requestTypeMap.put("Very Good", "4");
        requestTypeMap.put("Best", "5");
        return requestTypeMap;
    }
}
