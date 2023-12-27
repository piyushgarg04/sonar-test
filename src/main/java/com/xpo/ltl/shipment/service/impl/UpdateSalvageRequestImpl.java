package com.xpo.ltl.shipment.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.humanresource.v1.Employee;
import com.xpo.ltl.api.infrastructure.client.v2.InfrastructureClient;
import com.xpo.ltl.api.location.v2.ListLocationFeaturesResp;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageQuantify;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageRequest;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageRequestNote;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageRqst;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DeliveryQualifierCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.SalvageRequestStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.DeliveryQualifierCd;
import com.xpo.ltl.api.shipment.v2.EventLogSubTypeCd;
import com.xpo.ltl.api.shipment.v2.EventLogTypeCd;
import com.xpo.ltl.api.shipment.v2.MovementStatusCd;
import com.xpo.ltl.api.shipment.v2.SalvageRequest;
import com.xpo.ltl.api.shipment.v2.SalvageRequestNote;
import com.xpo.ltl.api.shipment.v2.SalvageRequestStatusCd;
import com.xpo.ltl.api.shipment.v2.UpdateSalvageRequestResp;
import com.xpo.ltl.api.shipment.v2.UpdateSalvageRequestRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmSalvageQuantifySubDAO;
import com.xpo.ltl.shipment.service.dao.ShmSalvageRequestNoteSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmSalvageRequestSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmSalvageRqstSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmSalvageStatusSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmEventDelegate;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import com.xpo.ltl.shipment.service.util.ShmSalvageUtil;


@RequestScoped
public class UpdateSalvageRequestImpl {
	
	private static final String PGM_ID = "UPDTSLVG";
    private static final String TRANID = "USLV";
    private static final String CURRENT_LOCATION = UpdateSalvageRequestImpl.class.getCanonicalName();
    private static final Log logger = LogFactory.getLog(UpdateSalvageRequestImpl.class);

	@Inject
	private ShmSalvageRequestSubDAO shmSalvageRequestSubDAO;
	
	@Inject
	private ShmSalvageRqstSubDAO shmSalvageRqstSubDAO;
	
	@Inject
	private ShmSalvageStatusSubDAO shmSalvageStatusSubDAO;
	
	@Inject
	private ExternalRestClient externalRestClient;
	
	@Inject 
	private ShmSalvageRequestNoteSubDAO shmSalvageRequestNoteSubDAO;
	
	@Inject
	private ShmSalvageQuantifySubDAO shmSalvageQuantifySubDAO;


    @Inject
    private ShmShipmentSubDAO shmShipmentSubDAO;

    @Inject
    private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

    @Inject
    private ShmEventDelegate shmEventDelegate;

    @PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
    private EntityManager db2EntityManager;

	@PersistenceContext(unitName = "ltl-java-shipment-rpt-jaxrs")
	private EntityManager rptEntityManager;
	
	private static final Double DefaultValue = (Double) 0.0;
	private static final String EMAIL_FROM = "overage@xpo.com";
	private static final String EMAIL_FROM_NAME = "Overage";
	private static final String EMAIL_SUBJECT = "[OS&D] : Status update for Salvage ID:  ";
	
	public UpdateSalvageRequestResp updateSalvageRequest(
		final UpdateSalvageRequestRqst updateSalvageRequestRqst,
		final TransactionContext txnContext,
		final EntityManager entityManager) throws ServiceException {
		SalvageRequest salvageRequest = updateSalvageRequestRqst.getSalvageRequest();
		UpdateSalvageRequestResp updateSalvageRequestResp = new UpdateSalvageRequestResp();

		AuditInfo auditInfo = AuditInfoHelper.getAuditInfoWithPgmId(PGM_ID, txnContext);

		ShmSalvageRequest shmSalvageRequest = shmSalvageRequestSubDAO.findById(salvageRequest.getSalvageRequestId(), entityManager);
		ShmSalvageRqst shmSalvageRqst = null;
		if(shmSalvageRequest == null) {
			throw ExceptionBuilder
			.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
			.moreInfo(CURRENT_LOCATION, "Not a valid salvage request")
			.log()
			.build();
		}
		if(StringUtils.isNotEmpty(salvageRequest.getComment()) && salvageRequest.getComment().length() > 3999) {
			throw ExceptionBuilder
			.exception(ValidationErrorMessage.SHM_SALVAGE_VALIDATION_ERRORS, txnContext)
			.moreInfo(CURRENT_LOCATION, "Comment cannot be more than 4000 characters")
			.log()
			.build();
		}
		boolean isBackSync = false;
        try {
            ListLocationFeaturesResp locFeatureSetting = externalRestClient.getLocFeatureSetting("SLVG_BACKSYNC", txnContext);
            isBackSync = locFeatureSetting.getLocationFeatures() != null && BasicTransformer.toBoolean((locFeatureSetting.getLocationFeatures().get(0).getSettingValue()));
        } catch (ServiceException e) {
            logger.warn("Feature not available. No backsync to old salvage application will occur.");
        }
        if(isBackSync) {
			shmSalvageRqst = shmSalvageRqstSubDAO.findById(salvageRequest.getSalvageRequestId(), rptEntityManager);
        }
        
		if(StringUtils.isNotEmpty(updateSalvageRequestRqst.getFeedbackStatusRequest()) && updateSalvageRequestRqst.getFeedbackStatusRequest().equals("REQUESTOR_EDIT")) {
			shmSalvageRequest.setPackagingCd(salvageRequest.getPackagingCd());
			shmSalvageRequest.setWeightLbs(new BigDecimal(salvageRequest.getWeightLbs()));
			shmSalvageRequest.setFreightConditionCd(salvageRequest.getFreightConditionCd());
			shmSalvageRequest.setEstimatedValueCd(salvageRequest.getEstimatedValueCd());
			shmSalvageRequest.setSpecialHandlingInd(salvageRequest.getSpecialHandlingInd() != null && salvageRequest.getSpecialHandlingInd() ? "Y" : "N");
			getCubicFt(salvageRequest);
			shmSalvageRequest.setLengthNbr(new BigDecimal(salvageRequest.getLengthNbr()));
			shmSalvageRequest.setWidthNbr(new BigDecimal(salvageRequest.getWidthNbr()));
			shmSalvageRequest.setHeightNbr(new BigDecimal(salvageRequest.getHeightNbr()));
			shmSalvageRequest.setCubeNbr(new BigDecimal(salvageRequest.getCubeNbr()));
			shmSalvageRequest.setBrand(salvageRequest.getBrand());
			shmSalvageRequest.setCommentTxt(salvageRequest.getComment());
			DtoTransformer.setLstUpdateAuditInfo(shmSalvageRequest, auditInfo);
			shmSalvageRequestSubDAO.save(shmSalvageRequest, entityManager);
			if(shmSalvageRqst != null) {
				shmSalvageRqst.setPkgWeight(String.valueOf(salvageRequest.getWeightLbs()));
				shmSalvageRqst.setOverallCondition(salvageRequest.getFreightConditionCd());//fridge condi
				shmSalvageRqst.setEstimateValue(salvageRequest.getEstimatedValueCd());//estimate value
				shmSalvageRqst.setField7(salvageRequest.getSpecialHandlingInd() != null && salvageRequest.getSpecialHandlingInd()  == true ? "Y" : "N");
				shmSalvageRqst.setBrand(salvageRequest.getBrand());
				shmSalvageRqst.setPkgDimensionL(String.valueOf(salvageRequest.getLengthNbr()));
				shmSalvageRqst.setPkgDimensionW(String.valueOf(salvageRequest.getWidthNbr()));
				shmSalvageRqst.setPkgDimensionH(String.valueOf(salvageRequest.getHeightNbr()));
				shmSalvageRqst.setPkgDimensionC(String.valueOf(salvageRequest.getCubeNbr()));
				shmSalvageRqst.setSalvageQuantify(getShmSalvageQuantify(salvageRequest.getPackagingCd(), rptEntityManager));
				shmSalvageRqst.setWorkingBy(auditInfo.getUpdateById());
				Date updtDt = BasicTransformer.toDate(auditInfo.getUpdatedTimestamp());
				shmSalvageRqst.setUpdateDate(updtDt);
				shmSalvageRqstSubDAO.save(shmSalvageRqst, rptEntityManager);
			} else {
	            logger.warn("Salvage Request not found in salvage database.");
			}

			updateSalvageRequestResp.setSalvageRequest(salvageRequest);
			return updateSalvageRequestResp;
		}
		if(shmSalvageRqst != null) {
			String requestStatus = ShmSalvageUtil.getOldDBRequestStausValue(SalvageRequestStatusCdTransformer.toCode(salvageRequest.getStatusCd()));
			shmSalvageRqst.setSalvageStatus(shmSalvageStatusSubDAO.findById(requestStatus, rptEntityManager));
			shmSalvageRqst.setTracked(BasicTransformer.toString(BooleanUtils.isTrue(salvageRequest.getAbeyanceInd())));
			shmSalvageRqst.setComputerName("Unknown");
			shmSalvageRqstSubDAO.save(shmSalvageRqst, rptEntityManager);
		} else {
            logger.warn("Salvage Request not found in salvage database.");
		}

		shmSalvageRequest.setHoldForVendorInd(salvageRequest.getHoldForVendorInd() != null && salvageRequest.getHoldForVendorInd() ? "Y" : "N");
		shmSalvageRequest.setStatusCd(SalvageRequestStatusCdTransformer.toCode(salvageRequest.getStatusCd()));
		shmSalvageRequest.setAbeyanceInd(BasicTransformer.toString(BooleanUtils.isTrue(salvageRequest.getAbeyanceInd())));
		DtoTransformer.setLstUpdateAuditInfo(shmSalvageRequest, auditInfo);

		shmSalvageRequestSubDAO.save(shmSalvageRequest, entityManager);

        String original = ObjectUtils.defaultIfNull(updateSalvageRequestRqst.getFeedbackStatusRequest(), "");
		String request = "";

		int startIndex = original.indexOf("<a href");
		int stopIndex = original.indexOf("</a>");
		if(startIndex != -1 && stopIndex != -1) {
			request = original.substring(startIndex, stopIndex+4);
		}
		shmSalvageRequestNoteSubDAO.createShmSalvageRequestNote(getshmSalvageRequestNote(shmSalvageRequest,
				original.replace(request, ""), txnContext, entityManager), entityManager);
		sendStatusUpdateMail(txnContext, shmSalvageRequest, updateSalvageRequestRqst);

        resetShipmentDeliveryAndMovementStatus(salvageRequest, shmSalvageRequest, auditInfo, entityManager, txnContext);

        updateSalvageRequestResp.setSalvageRequest(salvageRequest);
        return updateSalvageRequestResp;
    }

    /**
     * <p>
     * if salvage is APPROVED_FOR_CMK and shipment dlvry qualifier code is OVER_SHPMT, it
     * </p>
     * <ul>
     * <li>resets shipment and HUs mvmtStatCd to ON_DOCK</li>
     * <li>moves shipment dlvry qualifier code to SPACE</li>
     * <li>sends an event B/42 that will update dlvry qualifier code of ScoDlvrShipment.</li>
     * </ul>
     */
    private void resetShipmentDeliveryAndMovementStatus(SalvageRequest salvageRequest, ShmSalvageRequest shmSalvageRequestDB, AuditInfo auditInfo,
        EntityManager entityManager, TransactionContext txnContext)
            throws ServiceException {

        if (SalvageRequestStatusCd.APPROVED_FOR_CMK == salvageRequest.getStatusCd()) {
            ShmShipment shmShipment = shmShipmentSubDAO.findByIdOrProNumber(
                ProNumberHelper.toElevenDigitPro(shmSalvageRequestDB.getProNbrTxt(), txnContext), null, entityManager);

            if (shmShipment != null && DeliveryQualifierCd.OVER_SHPMT == DeliveryQualifierCdTransformer.toEnum(shmShipment.getDlvryQalfrCd())) {
                // upd shm
                shmShipment.setDlvryQalfrCd(StringUtils.SPACE);
                String mvmtStatusCdOnDock = MovementStatusCdTransformer.toCode(MovementStatusCd.ON_DOCK);
                shmShipment.setMvmtStatCd(mvmtStatusCdOnDock);
                DtoTransformer.setLstUpdateAuditInfo(shmShipment, auditInfo);
                shmShipmentSubDAO.save(shmShipment, entityManager);
                shmShipmentSubDAO.updateDB2ShmShipment(shmShipment, shmShipment.getLstUpdtTmst(), txnContext, db2EntityManager);
                // upd HUs
                List<ShmHandlingUnit> shmHandlingUnits = shmShipment.getShmHandlingUnits();
                shmHandlingUnits.forEach(hu -> {
                    hu.setMvmtStatCd(mvmtStatusCdOnDock);
                    DtoTransformer.setLstUpdateAuditInfo(hu, auditInfo);
                });
                shmHandlingUnitSubDAO.persist(shmHandlingUnits, entityManager);
                for (ShmHandlingUnit hu : shmHandlingUnits) {
                    shmHandlingUnitSubDAO.updateDB2ShmHandlingUnit(hu, hu.getLstUpdtTmst(), txnContext, db2EntityManager);
                }

                // send event B/42
                shmEventDelegate
                    .createEvent(0L, EventLogTypeCd.SHIPMENT_DLVY, EventLogSubTypeCd.UPDATE_DLVY, shmShipment, null, shmShipment.getOrigTrmnlSicCd(),
                        Optional.empty(), TRANID, entityManager, auditInfo);
            }
        }
	}

	private ShmSalvageRequestNote getshmSalvageRequestNote(ShmSalvageRequest shmSalvageRequest, String respNote, TransactionContext txnContext, EntityManager entityManager) throws ServiceException {
		ShmSalvageRequestNote shmSalvageRequestNote = new ShmSalvageRequestNote();
		Long seqNbr = shmSalvageRequestNoteSubDAO.getNextSeqNbrForSalvageRequestNote(
				shmSalvageRequest.getSalvageRequestId(), entityManager);
		SalvageRequestNote salvageRequestNote = new SalvageRequestNote();
		salvageRequestNote.setSequenceNbr(BigInteger.valueOf(seqNbr));
		salvageRequestNote.setSalvageRequestId(shmSalvageRequest.getSalvageRequestId());
		DtoTransformer.setAuditInfo(shmSalvageRequestNote, getAuditInfo(txnContext));
		shmSalvageRequestNote = DtoTransformer.toShmSalvageRequestNote(
				salvageRequestNote, shmSalvageRequestNote);
		shmSalvageRequestNote.setShmSalvageRequest(shmSalvageRequest);
		shmSalvageRequestNote.setNote(respNote);
		shmSalvageRequestNote.setNoteTypeCd("APPROVER");
		return shmSalvageRequestNote;
	}
	
	private static AuditInfo getAuditInfo(final TransactionContext txnContext) {
		AuditInfo auditInfo = new AuditInfo();
		AuditInfoHelper.setCreatedInfo(auditInfo, txnContext);
		AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);
		return auditInfo;
	}
	
	private String getEmployeeEmail(Employee employee) {
		String employeeEmail = employee != null ? employee.getEmailAddress() : "overage@xpo.com";
		return employeeEmail;
	}
	
	private void sendStatusUpdateMail(TransactionContext txnContext,
			ShmSalvageRequest shmSalvageRequest, 
			UpdateSalvageRequestRqst updateSalvageRequestRqst) {
		
		try {
			Employee employee =  externalRestClient.getEmployeeDetailsByEmployeeId(shmSalvageRequest.getCrteBy(), txnContext);
			InfrastructureClient infrastructureClient = externalRestClient.getInfrastructureClient(txnContext);
			infrastructureClient.sendEmail(null, 
					updateSalvageRequestRqst.getCcEmailAddress(), 
					EMAIL_FROM, 
					EMAIL_FROM_NAME, 
					updateSalvageRequestRqst.getFeedbackStatusRequest(), 
					null, 
					EMAIL_SUBJECT + String.valueOf(shmSalvageRequest.getSalvageRequestId()) ,
					getEmployeeEmail(employee),
					null);
		} catch (com.xpo.ltl.api.client.exception.ServiceException | ServiceException e1) {
            logger.warn(e1.getMessage());		
           }
	}
	
	private static void getCubicFt(SalvageRequest salvageRequest) {
		if((isDoubleValueGreaterThanZero(salvageRequest.getLengthNbr()) && 
				isDoubleValueGreaterThanZero(salvageRequest.getWidthNbr()) &&
				isDoubleValueGreaterThanZero(salvageRequest.getHeightNbr()))) {
			if(isDoubleValueZero(salvageRequest.getLengthNbr())) {
				salvageRequest.setLengthNbr(DefaultValue);
			}
			if(isDoubleValueZero(salvageRequest.getHeightNbr())) {
				salvageRequest.setHeightNbr(DefaultValue);
			}
			if(isDoubleValueZero(salvageRequest.getWidthNbr())) {
				salvageRequest.setWidthNbr(DefaultValue);
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
}
