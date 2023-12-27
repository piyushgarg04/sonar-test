package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationErrorMessageIF;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.humanresource.v1.Employee;
import com.xpo.ltl.api.location.v2.FeatureSetting;
import com.xpo.ltl.api.location.v2.ListLocationFeaturesResp;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCust;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.DataValidationError;
import com.xpo.ltl.api.shipment.v2.MoreInfo;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentsAsHandlingUnitExemptResp;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentsAsHandlingUnitExemptRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.ShmShipmentEagerLoadPlan;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.enums.UserRoleEnum;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;

@ApplicationScoped
@LogExecutionTime
public class UpdateShipmentAsHandlingUnitExemptImpl {

	private static final String FEATURE_SETTING_HU_EXEMPT_LANE = "HU_EXEMPT_LANE";
	
	private static final String FEATURE_SETTING_HU_EXEMPT_SHIPPER = "HU_EXEMPT_SHPR";

    private static final String SHIPPER_TYPE_CD = "1";
    
    private static final int MAX_SIZE_PRO_LIST_INPUT = 25;

    private static final String PGM_ID = "UPDSHEXM";

    @Inject
    private ShmShipmentSubDAO shmShipmentSubDAO;

    @Inject
    private ExternalRestClient externalRestClient;

    @PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
    private EntityManager db2EntityManager;

    @Inject
    private AppContext appContext;

    public UpdateShipmentsAsHandlingUnitExemptResp updateShipmentAsHandlingUnitExempt(
        UpdateShipmentsAsHandlingUnitExemptRqst exemptRqst,
			final TransactionContext txnContext, final EntityManager entityManager)
			throws ServiceException {

		checkNotNull(txnContext, "TransactionContext is required");
		checkNotNull(entityManager, "EntityManager is required");
        UpdateShipmentsAsHandlingUnitExemptResp response = new UpdateShipmentsAsHandlingUnitExemptResp();
        response.setWarnings(new ArrayList<DataValidationError>());

        List<String> formattedProNbrList = validatePROsForExemption(exemptRqst, response, txnContext,
            entityManager);
        if (formattedProNbrList.isEmpty() || CollectionUtils.isNotEmpty(response.getWarnings()))
            return response;

        AuditInfo auditInfo = AuditInfoHelper.getAuditInfoWithPgmId(PGM_ID, txnContext);
        shmShipmentSubDAO
            .bulkUpdateHandlingUnitExempByProNbrList(formattedProNbrList, BasicTransformer.toString(true),
                exemptRqst.getReason(), auditInfo, entityManager);
        shmShipmentSubDAO
            .db2BulkUpdateHandlingUnitExempByProNbrList(formattedProNbrList, BasicTransformer.toString(true),
                exemptRqst.getReason(), auditInfo,
                db2EntityManager);

        return response;

    }


    /**
     * Validate the data on the request. Pro Number is required
     */
    protected List<String> validatePROsForExemption(
        final UpdateShipmentsAsHandlingUnitExemptRqst exemptRqst,
        final UpdateShipmentsAsHandlingUnitExemptResp exemptResp,
        final TransactionContext txnContext, EntityManager entityManager) throws ValidationException, ServiceException {

        if (txnContext.getUser() == null || txnContext.getUser().getEmployeeId() == null) {
            MoreInfo moreInfo = new MoreInfo();
            moreInfo.setLocation("validatePROsForExempt");
            moreInfo.setMessage("EmployeeId not found in request.");
            addDataValidationErrorToResponse(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, moreInfo.getLocation(), moreInfo.getMessage(),
                exemptResp, Collections.singletonList(moreInfo));
            return Collections.emptyList();
        }

        UserRoleEnum pltAdminUserRole = appContext.isProd() ? UserRoleEnum.LTL_PLT_SYSTEM_ADMIN :
            UserRoleEnum.TST_LTL_PLT_SYSTEM_ADMIN;
        UserRoleEnum pltExempUserRole = appContext.isProd() ? UserRoleEnum.LTL_PLT_EXEMPTION : UserRoleEnum.TST_LTL_PLT_EXEMPTION;
        boolean isPltAdmin = ListUtils.emptyIfNull(txnContext.getUser().getRoles()).stream().anyMatch(r -> r.contains(pltAdminUserRole.toString()));
        boolean isPltExemp = ListUtils.emptyIfNull(txnContext.getUser().getRoles()).stream().anyMatch(r -> r.contains(pltExempUserRole.toString()) && !isPltAdmin);

        if (!isPltExemp && !isPltAdmin) {
            MoreInfo moreInfo = new MoreInfo();
            moreInfo.setMessage("User has no access to exempt");
            addDataValidationErrorToResponse(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, "employeeId", txnContext.getUser().getEmployeeId(), exemptResp,
                null    );
            return Collections.emptyList();
        } 
        
        if (exemptRqst == null || CollectionUtils.isEmpty(exemptRqst.getProNbrs())) {
            addDataValidationErrorToResponse(ValidationErrorMessage.REQUEST_REQUIRED, "ProNbr List size", "empty", exemptResp, null);
            return Collections.emptyList();
        }

        if (exemptRqst.getProNbrs().size() > MAX_SIZE_PRO_LIST_INPUT) {
            addDataValidationErrorToResponse(ValidationErrorMessage.INPUT_LIST_LENGTH_EXCEEDED, "ProNbr List size",
                String.valueOf(exemptRqst.getProNbrs().size()), exemptResp, null, String.valueOf(MAX_SIZE_PRO_LIST_INPUT));
            return Collections.emptyList();
        }

        List<String> validProNbrs = exemptRqst.getProNbrs().stream().map(pro -> {
            try {
                return ProNumberHelper.validateProNumber(pro, txnContext);
            } catch (ServiceException e) {
                addDataValidationErrorToResponse(ValidationErrorMessage.PRO_NUMBER_FORMAT, "ProNbr", pro, exemptResp,
                    null);
            }
            return StringUtils.EMPTY;
        }).filter(pro -> StringUtils.isNotBlank(pro)).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(validProNbrs)) {
            return validProNbrs;
        }

        ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan =
            new ShmShipmentEagerLoadPlan();
        shmShipmentEagerLoadPlan.setShmHandlingUnits(true);
        shmShipmentEagerLoadPlan.setShmAsEntdCusts(true);

        List<ShmShipment> shmShipmentsDB =
            shmShipmentSubDAO.listShipmentsByProNbrs
                (validProNbrs,
                 shmShipmentEagerLoadPlan,
                 entityManager);

        Employee employee = externalRestClient.getEmployeeDetailsByEmployeeId(txnContext.getUser().getEmployeeId(), txnContext);
        String employeeSic = employee.getBasicInfo().getDeptSic();
        ListLocationFeaturesResp locFeatureSettingLane = null;
        ListLocationFeaturesResp locFeatureSettingShipper = null;
        
        try {
            locFeatureSettingShipper = externalRestClient.getLocFeatureSetting(FEATURE_SETTING_HU_EXEMPT_SHIPPER, txnContext);
        } catch (Exception e) {
            //do nothing, assume all validation for shipper will fail below
            locFeatureSettingShipper = new ListLocationFeaturesResp();
        }

        try {
            locFeatureSettingLane = externalRestClient.getLocFeatureSetting(FEATURE_SETTING_HU_EXEMPT_LANE, txnContext);
        } catch (Exception e) {
            //do nothing, assume all validation for lane will fail below
            locFeatureSettingLane = new ListLocationFeaturesResp();
        }

        for (ShmShipment shmShipment : shmShipmentsDB) {
            String originSic = shmShipment.getOrigTrmnlSicCd();
            String destSic = shmShipment.getDestTrmnlSicCd();
            Optional<ShmAsEntdCust> shipper = CollectionUtils.emptyIfNull(shmShipment.getShmAsEntdCusts())
                    .stream()
                    .filter(shpr -> shpr.getTypCd().equalsIgnoreCase(SHIPPER_TYPE_CD))
                    .findAny();

            if (isPltExemp && !isPltAdmin) {

                boolean allowExemptByShipper = false;
                String shipperMadCd = null;
                if(shipper.isPresent() && shipper.get().getCisCustNbr() != null
                        && shipper.get().getCisCustNbr().compareTo(BigDecimal.ZERO) > 0) {
                    shipperMadCd = StringUtils.trimToEmpty(shipper.get().getAsMchMadCd());
                    allowExemptByShipper = locFeatureSettingShipper
                            .getLocationFeatures()
                            .stream()
                            .anyMatch(fSetting -> BasicTransformer.toBigDecimal(fSetting.getSettingValue()).compareTo(shipper.get().getCisCustNbr()) == 0);
                }
                if(!allowExemptByShipper) {
                    Optional<FeatureSetting> featureSettingForOrigAndDest = locFeatureSettingLane
                            .getLocationFeatures()
                            .stream()
                            .filter(fSetting -> fSetting.getSicCd().equals(originSic) && fSetting.getSettingValue().equals(destSic))
                            .findAny();

                    if (!featureSettingForOrigAndDest.isPresent()) {
                        MoreInfo moreInfo = new MoreInfo();
                        moreInfo.setMessage("Exemption not allowed for the shipper " + shipperMadCd + " nor the lane Orig: " + originSic + " - Dest: " + destSic);
                        addDataValidationErrorToResponse(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, "ProNbr", shmShipment.getProNbrTxt(), exemptResp,
                            Collections.singletonList(moreInfo));
                    }
                }
            }

            if (!isPltAdmin && !shmShipment.getCurrSicCd().equals(shmShipment.getOrigTrmnlSicCd())) {
                MoreInfo moreInfo = new MoreInfo();
                moreInfo.setMessage("Shipment Must be at Origin SIC");
                addDataValidationErrorToResponse(ValidationErrorMessage.PRO_NOT_AT_LOCATION, "ProNbr", shmShipment.getProNbrTxt(), exemptResp,
                    Collections.singletonList(moreInfo));
            }

            if (!isPltAdmin && !shmShipment.getCurrSicCd().equals(employeeSic)) {
                MoreInfo moreInfo = new MoreInfo();
                moreInfo.setMessage("Requestor must be at Current SIC");
                addDataValidationErrorToResponse(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, "ProNbr", shmShipment.getProNbrTxt(), exemptResp,
                    Collections.singletonList(moreInfo));
            }
            if (CollectionUtils.isNotEmpty(shmShipment.getShmHandlingUnits())) {
                addDataValidationErrorToResponse(ValidationErrorMessage.HANDLING_UNIT_EXIST, "ProNbr", shmShipment.getProNbrTxt(), exemptResp, null,
                    shmShipment.getProNbrTxt());
            }
        }

        if (StringUtils.isBlank(exemptRqst.getReason())) {
            addDataValidationErrorToResponse(ValidationErrorMessage.REQUIRED_INPUT_MISSING, "Reason", "", exemptResp, null,
                ": Exemption Reason");
        }

        // validate reason length
        if (StringUtils.isNotBlank(exemptRqst.getReason())
                && exemptRqst.getReason().length() > 50) {
            addDataValidationErrorToResponse(com.xpo.ltl.api.exception.ValidationErrorMessage.LENGTH_OF_VALUE_EXCEEDS_MAX, "Reason",
                exemptRqst.getReason(),
                exemptResp, null, "50", exemptRqst.getReason().length() + "");
        }

        return validProNbrs;
    }

    private void addDataValidationErrorToResponse(ValidationErrorMessageIF errorMsg, String fieldName, String fieldValue,
        UpdateShipmentsAsHandlingUnitExemptResp exemptResp, List<MoreInfo> moreInfo, String... contextValues) {
        DataValidationError dataValError = new DataValidationError();
        dataValError.setErrorCd(errorMsg.errorCode());
        dataValError.setMessage(errorMsg.message(contextValues));
        dataValError.setFieldName(fieldName);
        dataValError.setFieldValue(fieldValue);
        if (CollectionUtils.isNotEmpty(moreInfo)) {
            dataValError.setMoreInfo(moreInfo);
        }
        exemptResp.getWarnings().add(dataValError);
    }

}
