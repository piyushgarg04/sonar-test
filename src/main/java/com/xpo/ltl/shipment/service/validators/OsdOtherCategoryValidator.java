package com.xpo.ltl.shipment.service.validators;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.javatuples.Quartet;
import org.javatuples.Quintet;
import org.javatuples.Triplet;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdHeader;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdImage;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.ConeColorCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OsdCategoryCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OsdStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OtherOsdReasonCdTransformer;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.CloseReasonCd;
import com.xpo.ltl.api.shipment.v2.ConeColorCd;
import com.xpo.ltl.api.shipment.v2.OsdCategoryCd;
import com.xpo.ltl.api.shipment.v2.OsdChildShipment;
import com.xpo.ltl.api.shipment.v2.OsdDocumentImage;
import com.xpo.ltl.api.shipment.v2.OsdParentShipment;
import com.xpo.ltl.api.shipment.v2.OsdStatusCd;
import com.xpo.ltl.api.shipment.v2.OtherOsdReasonCd;
import com.xpo.ltl.api.shipment.v2.UpsertOsdRqst;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;

public class OsdOtherCategoryValidator {

	private static final Map<OtherOsdReasonCd, List<CloseReasonCd>> validOtherReasonCdCloseReasonCdMap = new HashMap<OtherOsdReasonCd, List<CloseReasonCd>>() {
		{
			//CCS-9936: upsertOsd API Changes for "Other" OS&D Category Type
			put(OtherOsdReasonCd.FNL_IN_ERROR, Arrays.asList(CloseReasonCd.MADE_GCBZ_NEW_BILL, CloseReasonCd.DUP_ENTRY_CNCL_ENTRY));
			put(OtherOsdReasonCd.RCD_PROCESSED, Arrays.asList(CloseReasonCd.RECONSIGNMENT_PRCES, CloseReasonCd.DUP_ENTRY_CNCL_ENTRY));
			put(OtherOsdReasonCd.RTS_REQUESTED, Arrays.asList(CloseReasonCd.RETURNED_TO_SHIPPER, CloseReasonCd.DUP_ENTRY_CNCL_ENTRY));
			put(OtherOsdReasonCd.ONLY_CHILD_PRO,
					Arrays.asList(CloseReasonCd.TAGGED_TO_PARENT_PRO, CloseReasonCd.NO_PARENT_FOUND));
			put(OtherOsdReasonCd.CROSSED, Arrays.asList(CloseReasonCd.MADE_GCBZ, CloseReasonCd.MOVED_CURR_LOC_MOVR, CloseReasonCd.DUP_ENTRY_CNCL_ENTRY, CloseReasonCd.RECONCILED_CROSS_PRO));
			put(OtherOsdReasonCd.ENTRY_NOT_RQRD, Arrays.asList(CloseReasonCd.DUP_ENTRY_CNCL_ENTRY));
			put(OtherOsdReasonCd.UNBILLED, Arrays.asList(CloseReasonCd.SHPMNT_BLD_RDY_MOVE, CloseReasonCd.DUP_ENTRY_CNCL_ENTRY));
			put(OtherOsdReasonCd.UNDELIVERABLE, Arrays.asList(CloseReasonCd.DISP_FROM_SHIPPER, CloseReasonCd.DISP_TO_SEND_TO_CMK, CloseReasonCd.DUP_ENTRY_CNCL_ENTRY));
		}
	};
	

	private static final List<OtherOsdReasonCd> otherReasonCdsForNewParentProMandatoryValidation = Arrays.asList(
			OtherOsdReasonCd.FNL_IN_ERROR, OtherOsdReasonCd.RCD_PROCESSED, OtherOsdReasonCd.RTS_REQUESTED,
			OtherOsdReasonCd.CROSSED);
	
	@Inject
	private OsdCommonValidator osdCommonValidator;

	public void validate(UpsertOsdRqst upsertOsdRqst, final TransactionContext txnContext) throws ServiceException {

		osdCommonValidator.validate(upsertOsdRqst, txnContext);

		OsdParentShipment osdParentShipment = upsertOsdRqst.getOsdParentShipment();
		List<OsdDocumentImage> osdDocumentImages = upsertOsdRqst.getOsdDocumentImages();
		ActionCd actionCd = upsertOsdRqst.getActionCd();

		if (Objects.nonNull(osdParentShipment)) {
			switch (actionCd) {
			case ADD:
				validateCreateOtherCategoryPayload(osdParentShipment, osdDocumentImages, txnContext);
				validateOtherCategoryPayload(osdParentShipment, txnContext);
				break;
			case UPDATE:
				validateUpdateOtherCategoryPayload(osdParentShipment, txnContext);
				validateOtherCategoryPayload(osdParentShipment, txnContext);
				break;
			default:
				break;
			}
		}
	}

	private void validateCreateOtherCategoryPayload(OsdParentShipment osdParentShipment,
			List<OsdDocumentImage> osdDocumentImages, TransactionContext txnContext) throws ValidationException {
		if (Objects.isNull(osdParentShipment.getConeNbr())) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
					.moreInfo("coneNbr", "coneNbr is required.").log().build();
		}

		if (Objects.isNull(osdParentShipment.getConeColorCd())) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
					.moreInfo("coneColorCd", "coneColorCd is required.").log().build();
		} else if (osdParentShipment.getConeColorCd() != ConeColorCd.YELLOW) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo("coneColorCd", "Cone Color Cd should be Yellow.").log().build();
		}

		Long photoCount = CollectionUtils.emptyIfNull(osdDocumentImages).stream()
				.filter(docImage -> StringUtils.isNotEmpty(docImage.getDmsUrl())).distinct().count();
		if (Objects.isNull(photoCount) || photoCount < 1) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo("osdDocumentImages.dmsUrl", "At least 1 photo is required.").log().build();
		}

		if (StringUtils.isNotEmpty(osdParentShipment.getNewParentProNbr())) {
			if (Objects.isNull(osdParentShipment.getOtherOsdReasonCd())) {
				throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
						.moreInfo("otherOsdReasonCd", "otherOsdReasonCd is required.").log().build();
			}
		}

	}

	private void validateUpdateOtherCategoryPayload(OsdParentShipment osdParentShipment, TransactionContext txnContext)
			throws ValidationException {

		if (osdParentShipment.getConeColorCd() == ConeColorCd.RED) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo("coneColorCd", "Cone Color Cd cannot be Red.").log().build();
		}

	}

	private void validateOtherCategoryPayload(OsdParentShipment osdParentShipment, TransactionContext txnContext)
			throws ValidationException {

		if (StringUtils.isNotEmpty(osdParentShipment.getNewParentProNbr())) {
			if (!ProNumberHelper.isBluePro(osdParentShipment.getNewParentProNbr())) {
				throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
						.moreInfo("newParentProNbr", "The new PRO number entered does not have a valid format.").log()
						.build();
			}
		}

		if (Objects.nonNull(osdParentShipment.getStatusCd())) {
			String[] splittedValues = OsdStatusCdTransformer.toCode(osdParentShipment.getStatusCd()).split("_");
			String osdCategory = OsdCategoryCdTransformer.toCode(OsdCategoryCd.OTHER);
			if (!splittedValues[0].equalsIgnoreCase(osdCategory.substring(0, 2))) {
				throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
						.moreInfo("statusCd & osdCategoryCd", "OSD Status and OSD Category code are not matching.")
						.log().build();
			}
		}
	}

	public void validateOtherCategoryPayloadwithDB(UpsertOsdRqst upsertOsdRqst,
			Quintet<ShmShipment, ShmShipment, Boolean, Map<String, ShmOsdHeader>,ShmOsdHeader> shipmentAndOsdDetails,
			ShmOsdHeader shmosdHeader, TransactionContext txnContext, EntityManager entityManager)
			throws ServiceException {

		ShmShipment shmShipment = shipmentAndOsdDetails.getValue0();
		ShmShipment shmShipmentForNewParentProNbr = shipmentAndOsdDetails.getValue1();
		Boolean isLegacyPro = shipmentAndOsdDetails.getValue2();
		Map<String, ShmOsdHeader> shmOsdHeaderForChildPro = shipmentAndOsdDetails.getValue3();
		ShmOsdHeader shmOsdHeaderForConeColorValidation = shipmentAndOsdDetails.getValue4();

		osdCommonValidator.validateOsdPayloadWithDB(upsertOsdRqst, shmosdHeader, shmShipment, txnContext,
				entityManager);
		OsdParentShipment osdParentShipment = upsertOsdRqst.getOsdParentShipment();
		ActionCd actionCd = upsertOsdRqst.getActionCd();
		List<OsdChildShipment> osdChildShipments = upsertOsdRqst.getOsdChildShipments();

		if (Objects.nonNull(osdParentShipment)) {
			
			if (ActionCd.ADD == actionCd) {
				if (Objects.nonNull(shmOsdHeaderForConeColorValidation)) {
					throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
							.moreInfo("coneColorCd & coneNbr",
									String.format("Cone %s %s is already in use.",
											osdParentShipment.getConeColorCd(),
											osdParentShipment.getConeNbr()))
							.log().build();
				}
			}
			
			if (ActionCd.UPDATE == actionCd && Objects.nonNull(shmosdHeader)) {

				if (Objects.nonNull(osdParentShipment.getOsdCategoryCd())) {
					OsdCategoryCd existingCategory = OsdCategoryCdTransformer.toEnum(shmosdHeader.getOsdCategoryCd());
					OsdCategoryCd newCategory = osdParentShipment.getOsdCategoryCd();
					if (newCategory != existingCategory) {
						throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
								.moreInfo("osdCategoryCd", String.format(
										"Cannot update the category code for %s entries.", existingCategory.toString()))
								.log().build();
					}
				}

				if (Objects.nonNull(osdParentShipment.getNewParentProNbr())
						&& Objects.nonNull(shmosdHeader.getNewParentProNbrTxt())) {
					String exsitingNewParentProNbr = shmosdHeader.getNewParentProNbrTxt();
					String newParentProNbr = ProNumberHelper.toElevenDigitPro(osdParentShipment.getNewParentProNbr(),
							txnContext);
					if (!newParentProNbr.equals(exsitingNewParentProNbr)) {
						throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
								.moreInfo("newParentProNbr", "Cannot update the New Parent Pro Nbr.").log().build();
					}
				}

				if (Objects.nonNull(osdParentShipment.getParentProNbr())
						&& Objects.nonNull(shmosdHeader.getProNbrTxt())) {
					String exsitingParentProNbr = shmosdHeader.getProNbrTxt();
					String parentProNbr = ProNumberHelper.toElevenDigitPro(osdParentShipment.getParentProNbr(),
							txnContext);
					if (!parentProNbr.equals(exsitingParentProNbr)) {
						throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
								.moreInfo("parentProNbr", "Cannot update the Parent Pro Nbr.").log().build();
					}
				}

				if (Objects.nonNull(osdChildShipments)) {
					for (OsdChildShipment osdChildShipment : osdChildShipments) {
						String childProNbr = ProNumberHelper.toElevenDigitPro(osdChildShipment.getChildProNbr(),
								txnContext);
						ShmOsdImage shmOsdImageForChildPro = CollectionUtils.emptyIfNull(shmosdHeader.getShmOsdImages())
								.stream()
								.filter(docImage -> StringUtils.isNotEmpty(docImage.getProNbrTxt())
										&& childProNbr.equalsIgnoreCase(docImage.getProNbrTxt()))
								.findFirst().orElse(null);

						if (ActionCd.ADD == osdChildShipment.getActionCd()) {
							if (Objects.nonNull(shmOsdImageForChildPro)) {
								throw ExceptionBuilder
										.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
										.moreInfo("childProNbr",
												String.format(
														"%s - existing child PRO(s) cannot be added to the OS&D Entry.",
														osdChildShipment.getChildProNbr()))
										.log().build();
							}
						}
					}
				}
				
				if (Objects.nonNull(shmOsdHeaderForConeColorValidation) && 
						shmOsdHeaderForConeColorValidation.getOsdId() != shmosdHeader.getOsdId()) {
					throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
							.moreInfo("coneColorCd & coneNbr",
									String.format("Cone %s %s is already in use.",
											ConeColorCdTransformer.getLabel(
													ConeColorCdTransformer.toEnum(
													shmOsdHeaderForConeColorValidation.getConeColorCd())),
											shmOsdHeaderForConeColorValidation.getConeNbr()))
							.log().build();
				}

			}

			OtherOsdReasonCd otherOsdReasonCd = null;
			if (Objects.nonNull(osdParentShipment.getOtherOsdReasonCd())) {
				otherOsdReasonCd = osdParentShipment.getOtherOsdReasonCd();
			} else if (Objects.nonNull(shmosdHeader) && Objects.nonNull(shmosdHeader.getOtherReasonCd())) {
				otherOsdReasonCd = OtherOsdReasonCdTransformer.toEnum(shmosdHeader.getOtherReasonCd());
			}

			if (StringUtils.isNotEmpty(osdParentShipment.getNewParentProNbr()) && Objects.isNull(otherOsdReasonCd)) {
				throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
						.moreInfo("otherOsdReasonCd", "otherOsdReasonCd is required.").log().build();
			}

			if (Objects.nonNull(otherOsdReasonCd)) {
				if (!OtherOsdReasonCdTransformer.toCode(otherOsdReasonCd)
						.equalsIgnoreCase(OtherOsdReasonCdTransformer.toCode(OtherOsdReasonCd.ONLY_CHILD_PRO))) {

					String parentProNbr = null;
					if (Objects.nonNull(osdParentShipment.getParentProNbr())) {
						parentProNbr = osdParentShipment.getParentProNbr();
						if (Objects.isNull(shmShipment)) {
							throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
									.moreInfo("parentProNbr",
											String.format("Parent PRO# %s does not exist.", parentProNbr))
									.log().build();
						}
					} else if (Objects.nonNull(shmosdHeader) && Objects.nonNull(shmosdHeader.getProNbrTxt())) {
						parentProNbr = shmosdHeader.getProNbrTxt();
					}

					if (Objects.isNull(parentProNbr)) {
						throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
								.moreInfo("parentProNbr", "parentProNbr is required.").log().build();
					}
				} else if (StringUtils.isNotBlank(osdParentShipment.getParentProNbr())
						&& Objects.nonNull(shmShipment)) {
					throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
							.moreInfo("parentProNbr", "Please provide new Pro number.").log().build();
				}
			} else if (StringUtils.isNotBlank(osdParentShipment.getParentProNbr()) && Objects.isNull(shmShipment)) {
				throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
						.moreInfo("parentProNbr",
								String.format("Parent PRO# %s does not exist.", osdParentShipment.getParentProNbr()))
						.log().build();
			}

			if (Objects.nonNull(osdParentShipment.getStatusCd())) {
				if (OsdStatusCd.OT_CLOSED.equals(osdParentShipment.getStatusCd())) {

					if (Objects.isNull(otherOsdReasonCd)) {
						throw ExceptionBuilder.exception(ValidationErrorMessage.OSD_MANDATORY_FIELDS, txnContext)
								.moreInfo("otherOsdReasonCd", "otherOsdReasonCd is required.").log().build();
					}

					if (validOtherReasonCdCloseReasonCdMap.containsKey(otherOsdReasonCd)) {
						List<CloseReasonCd> closeReasonCds = validOtherReasonCdCloseReasonCdMap.get(otherOsdReasonCd);
						if (!closeReasonCds.contains(osdParentShipment.getCloseReasonCd())) {
							throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
									.moreInfo("closeReasonCd",
											String.format("For OtherOsdReasonCd %s, closeReason can be %s",
													otherOsdReasonCd,
													validOtherReasonCdCloseReasonCdMap.get(otherOsdReasonCd)))
									.log().build();
						}
					}
					// CCS-9937 : Make "New Parent PRO" mandatory at the time of Closing OS&D Entry for "Other" Category Type
					if (otherReasonCdsForNewParentProMandatoryValidation.contains(otherOsdReasonCd)) {
						if (StringUtils.isBlank(osdParentShipment.getNewParentProNbr())) {
							throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
									.moreInfo("newParentProNbr", "Please provide new Parent Pro number.").log().build();
						}
					}
				}
			}
			
			if (StringUtils.isNotEmpty(osdParentShipment.getNewParentProNbr())
					&& Objects.isNull(shmShipmentForNewParentProNbr)) {
				throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext).moreInfo(
						"newParentProNbr",
						String.format("New Parent PRO# %s does not exist.", osdParentShipment.getNewParentProNbr()))
						.log().build();
			}

			if (Objects.isNull(osdParentShipment.getOsdPiecesCount()) && isLegacyPro) {
				throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
						.moreInfo("osdPiecesCount", "OSD Pieces Count is required for Legacy Pro.").log().build();
			}

		}

		if (Objects.nonNull(shmOsdHeaderForChildPro)) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo("osdChildShipments.childProNbr", String.format(
							"An active OS&D entry already exists for child Pro %S.", shmOsdHeaderForChildPro.keySet()))
					.log().build();
		}

	}

	public void validateAuditInfo(AuditInfo auditInfo, final TransactionContext txnContext) throws ServiceException {
		osdCommonValidator.validateAuditInfo(auditInfo, txnContext);
	}

}
