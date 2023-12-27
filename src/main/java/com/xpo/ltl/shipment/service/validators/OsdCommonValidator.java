package com.xpo.ltl.shipment.service.validators;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdHeader;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.OsdCategoryCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OsdStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.OsdChildShipment;
import com.xpo.ltl.api.shipment.v2.OsdManagementRemark;
import com.xpo.ltl.api.shipment.v2.OsdParentShipment;
import com.xpo.ltl.api.shipment.v2.OsdStatusCd;
import com.xpo.ltl.api.shipment.v2.UpsertOsdRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;

public class OsdCommonValidator {

	private static final List<OsdStatusCd> closedStatusCds = Arrays.asList(OsdStatusCd.D_CLOSED, OsdStatusCd.O_CLOSED,
			OsdStatusCd.R_CLOSED, OsdStatusCd.S_CLOSED, OsdStatusCd.OT_CLOSED);
	private static final List<OsdStatusCd> notStartedStatusCds = Arrays.asList(OsdStatusCd.D_NOT_STARTED,
			OsdStatusCd.O_NOT_STARTED, OsdStatusCd.R_NOT_STARTED, OsdStatusCd.S_NOT_STARTED,
			OsdStatusCd.OT_NOT_STARTED);

	@Inject
	private OsdHeaderValidator osdHeaderValidator;

	public void validate(UpsertOsdRqst upsertOsdRqst, final TransactionContext txnContext) throws ServiceException {

		OsdParentShipment osdParentShipment = upsertOsdRqst.getOsdParentShipment();
		ActionCd actionCd = upsertOsdRqst.getActionCd();

		if (Objects.isNull(actionCd)) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
					.moreInfo("actionCd", "actionCd is required").log().build();
		} else if (ActionCd.ADD != actionCd && ActionCd.UPDATE != actionCd) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext).moreInfo(
					"actionCd",
					"Please provide a valid actionCd (Add or Update) to perform a create/update operation for OS&D.")
					.log().build();
		}

		if (Objects.nonNull(osdParentShipment)) {
			switch (actionCd) {
			case ADD:
				validateCreateOsdPayload(osdParentShipment, txnContext);
			default:
				validateOsdPayload(upsertOsdRqst, txnContext);
			}
		}
	}

	private void validateCreateOsdPayload(OsdParentShipment osdParentShipment, final TransactionContext txnContext)
			throws ServiceException {
		if (Objects.isNull(osdParentShipment.getOsdCategoryCd())) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
					.moreInfo("osdCategoryCd", "osdCategoryCd is required.").log().build();
		}

		if (Objects.isNull(osdParentShipment.getReportingSicCd())) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
					.moreInfo("reportingSicCd", "reportingSicCd is required.").log().build();
		} else {
			osdHeaderValidator.validateReportingSicCd(osdParentShipment.getReportingSicCd(), txnContext);
		}

		if (Objects.nonNull(osdParentShipment.getStatusCd())) {
			if (!notStartedStatusCds.contains(osdParentShipment.getStatusCd())) {
				throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext).moreInfo(
						"statusCd",
						"Invalid OsdStatusCd provided for create OS&D operation.(Please remove this or provide a *not_started status.")
						.log().build();
			}

			String[] splittedValues = OsdStatusCdTransformer.toCode(osdParentShipment.getStatusCd()).split("_");
			String osdCategory = OsdCategoryCdTransformer.toCode(osdParentShipment.getOsdCategoryCd());
			if (!splittedValues[0].equalsIgnoreCase(osdCategory.substring(0, splittedValues[0].length()))) {
				throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
						.moreInfo("statusCd & osdCategoryCd", "OSD Status and OSD Category code are not matching.")
						.log().build();
			}
		}
	}

	private void validateOsdPayload(UpsertOsdRqst upsertOsdRqst, final TransactionContext txnContext)
			throws ValidationException {

		List<OsdChildShipment> osdChildShipments = upsertOsdRqst.getOsdChildShipments();
		List<OsdManagementRemark> osdManagementRemarks = upsertOsdRqst.getOsdManagementRemarks();
		OsdParentShipment osdParentShipment = upsertOsdRqst.getOsdParentShipment();

		validateOsdChildShipments(osdChildShipments, txnContext);
		validateOsdManagementRemarks(osdManagementRemarks, txnContext);

		if (Objects.nonNull(osdParentShipment.getStatusCd())) {
			if (closedStatusCds.contains(osdParentShipment.getStatusCd())
					&& (Objects.isNull(osdParentShipment.getCloseReasonCd()))) {
				throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
						.moreInfo("closeReasonCd", "Closed Reason Code is required.").log().build();
			}

			if (!closedStatusCds.contains(osdParentShipment.getStatusCd())
					&& Objects.nonNull(osdParentShipment.getCloseReasonCd())) {
				throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
						.moreInfo("closeReasonCd", "Closed Due To reason is required only for close OS&D entry.").log()
						.build();
			}
		} else if (Objects.nonNull(osdParentShipment.getCloseReasonCd())) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo("closeReasonCd", "Closed Due To reason is required only for close OS&D entry.").log()
					.build();
		}

		if (StringUtils.isNotEmpty(osdParentShipment.getParentProNbr())
				&& !ProNumberHelper.isBluePro(osdParentShipment.getParentProNbr())) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo("parentProNbr", "The PRO number entered does not have a valid format.").log().build();
		}

	}

	private void validateOsdManagementRemarks(List<OsdManagementRemark> osdManagementRemarks,
			final TransactionContext txnContext) throws ValidationException {
		if (Objects.nonNull(osdManagementRemarks)) {
			for (OsdManagementRemark osdManagementRemark : osdManagementRemarks) {
				if (StringUtils.isEmpty(osdManagementRemark.getRemark())) {
					throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
							.moreInfo("osdManagementRemarks.remark", "remark is mandatory.").log().build();
				}
			}
		}
	}

	private void validateOsdChildShipments(List<OsdChildShipment> osdChildShipments,
			final TransactionContext txnContext) throws ValidationException {
		if (Objects.nonNull(osdChildShipments)) {
			for (OsdChildShipment osdChildShipment : osdChildShipments) {
				if (Objects.isNull(osdChildShipment.getActionCd())) {
					throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
							.moreInfo("osdChildShipments.actionCd", "actionCd is required.").log().build();
				}

				if (StringUtils.isEmpty(osdChildShipment.getChildProNbr())) {
					throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
							.moreInfo("osdChildShipments.childProNbr", "Child Pro Number is required.").log().build();
				}

				if (!ProNumberHelper.isYellowPro(osdChildShipment.getChildProNbr())) {
					throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
							.moreInfo("osdChildShipments.childProNbr",
									"Cannot accept parent PRO format for child pro numbers in OsdChildShipments.")
							.log().build();
				}
			}
		}
	}

	public void validateOsdPayloadWithDB(UpsertOsdRqst upsertOsdRqst, ShmOsdHeader shmosdHeader,
			ShmShipment shmShipment, TransactionContext txnContext, EntityManager entityManager) throws ServiceException {
		OsdParentShipment osdParentShipment = upsertOsdRqst.getOsdParentShipment();

		ActionCd actionCd = upsertOsdRqst.getActionCd();

		if (Objects.nonNull(osdParentShipment)) {
			
			if (ActionCd.ADD == actionCd) {
				if (StringUtils.isNotEmpty(osdParentShipment.getParentProNbr())) {
					osdHeaderValidator.validateParentProAlreadyExist(osdParentShipment, txnContext, entityManager);
				}
			}
			
			if (ActionCd.UPDATE == actionCd) {
				if (StringUtils.isNotEmpty(osdParentShipment.getReportingSicCd())
						&& !osdParentShipment.getReportingSicCd().equals(shmosdHeader.getReportingSicCd())) {
					throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
							.moreInfo("reportingSicCd", "Reporting SIC cannot be updated.").log().build();
				}

				if (closedStatusCds.contains(OsdStatusCdTransformer.toEnum(shmosdHeader.getStatusCd()))) {
					throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
							.moreInfo("statusCd", "Cannot update an OS&D once closed.").log().build();
				}

				if (Objects.nonNull(osdParentShipment.getArriveAtOsdDateTime())
						&& !osdParentShipment.getArriveAtOsdDateTime().normalize().equals((BasicTransformer
								.toXMLGregorianCalendar(shmosdHeader.getArriveAtOsdTmst()).normalize()))) {
					throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
							.moreInfo("arriveAtOsdDateTime", "Cannot update arriveAtOsdDateTime after creation.").log()
							.build();
				}

				if (Objects.nonNull(osdParentShipment.getStatusCd())) {
					String osdCategory = shmosdHeader.getOsdCategoryCd().toLowerCase();
					String[] splittedValues = OsdStatusCdTransformer.toCode(osdParentShipment.getStatusCd()).split("_");
					if (!splittedValues[0].toLowerCase().equals(osdCategory.substring(0, splittedValues[0].length()))) {
						throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
								.moreInfo("statusCd & osdCategoryCd",
										"OSD Status and OSD Category code are not matching")
								.log().build();
					}
				}
			}

		}

	}

	public void validateAuditInfo(AuditInfo auditInfo, final TransactionContext txnContext) throws ServiceException {

		if (StringUtils.isBlank(auditInfo.getCreatedById())) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
					.moreInfo("auditInfo.createdById", "createdById is required.").log().build();
		}

		if (Objects.isNull(auditInfo.getCreatedTimestamp())) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
					.moreInfo("auditInfo.createdTimestamp", "createdTimestamp is required.").log().build();
		}

		if (StringUtils.isBlank(auditInfo.getCreateByPgmId())) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
					.moreInfo("auditInfo.createByPgmId", "createByPgmId is required.").log().build();
		}

		if (Objects.isNull(auditInfo.getUpdatedTimestamp())) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
					.moreInfo("auditInfo.updatedTimestamp", "updatedTimestamp is required.").log().build();
		}

		if (StringUtils.isBlank(auditInfo.getUpdateByPgmId())) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
					.moreInfo("auditInfo.updateByPgmId", "updateByPgmId is required.").log().build();
		}

	}

}