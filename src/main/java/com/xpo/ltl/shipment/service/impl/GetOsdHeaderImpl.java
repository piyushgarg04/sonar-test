package com.xpo.ltl.shipment.service.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Quartet;

import com.google.gson.Gson;
import com.xpo.ltl.api.exception.NotFoundErrorMessageIF;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.humanresource.v2.InterfaceEmployee;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdHeader;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.GetOsdResp;
import com.xpo.ltl.api.shipment.v2.OsdCategoryCd;
import com.xpo.ltl.api.shipment.v2.OsdChildShipment;
import com.xpo.ltl.api.shipment.v2.OsdManagementRemark;
import com.xpo.ltl.api.shipment.v2.OsdParentShipment;
import com.xpo.ltl.api.shipment.v2.OsdPayloadTypeCd;
import com.xpo.ltl.api.shipment.v2.OsdStatusCd;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShipmentMovementExceptionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmOsdHdrSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.transformers.OsdEntityTransformer;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import com.xpo.ltl.shipment.service.validators.OsdHeaderValidator;

@ApplicationScoped
@LogExecutionTime
public class GetOsdHeaderImpl {
	
	public static final String CURRENT_LOCATION = GetOsdHeaderImpl.class.getCanonicalName();
	@Inject
	private OsdHeaderValidator osdHeaderValidator;

	@Inject
	private ShmOsdHdrSubDAO shmOsdHdrSubDAO;

	@Inject
	private ExternalRestClient externalRestClient;

	@Inject
	private ShmShipmentSubDAO shipmentSubDAO;

	@Inject
	private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

	@Inject
	private ShipmentMovementExceptionSubDAO shipmentMovementExceptionSubDAO;
	
	private static final Logger LOGGER = LogManager.getLogger(GetOsdHeaderImpl.class);

	public GetOsdResp getOsd(Long osdId, String proNbr, OsdCategoryCd osdCategoryCd, String reportingSicCd,
			OsdStatusCd osdStatusCd, OsdPayloadTypeCd osdPayloadTypeCd,
			final TransactionContext txnContext, final EntityManager entityManager) throws ServiceException {
		
		LOGGER.info(String.format(
				"Request Payload for getOsd : osdId %s , proNbr %s , osdCategoryCd %s , reportingSicCd %s , osdStatusCd %s , osdPayloadTypeCd %s",
				osdId, proNbr, osdCategoryCd, reportingSicCd, osdStatusCd, osdPayloadTypeCd));
		
		Quartet<ShmOsdHeader, ShmShipment, ShmMovementExcp, Boolean> shmOsdDetails = null;
		osdHeaderValidator.validateGetOsdParameters(osdId, proNbr, reportingSicCd, osdPayloadTypeCd,
				txnContext);
		// Case 1 : When osdId is not null
		if (Objects.nonNull(osdId)) {
			shmOsdDetails = buildShmOsdDetailsFromOsdId(osdId, osdCategoryCd, reportingSicCd, osdStatusCd, txnContext,
					entityManager);
		}

		// Case 2 : When proNbr is not null
		if (StringUtils.isNotBlank(proNbr)) {
			shmOsdDetails = buildShmOsdDetailsFromProNbr(proNbr, osdCategoryCd, reportingSicCd, osdStatusCd, txnContext,
					entityManager);
		}

		ShmShipment newParentShmShipment = null;
		// case 3 : when newParentProNbr is not blank
		if (shmOsdDetails.getValue0() != null && StringUtils.isNotBlank(shmOsdDetails.getValue0().getNewParentProNbrTxt())) {
			List<ShmShipment> shmShipments = shipmentSubDAO.findByProNumber(Collections.singletonList(shmOsdDetails.getValue0().getNewParentProNbrTxt()),
			entityManager);
			if(shmShipments.size() > 0) {
				newParentShmShipment = shmShipments.get(0);
			}
		}

		GetOsdResp getOsdResp = buildResponse(osdPayloadTypeCd, shmOsdDetails, newParentShmShipment, proNbr, txnContext);
		LOGGER.info(String.format("Response Payload for getOsd : %s ", new Gson().toJson(getOsdResp)));
		return getOsdResp;
	}

	private Quartet<ShmOsdHeader, ShmShipment, ShmMovementExcp, Boolean> buildShmOsdDetailsFromOsdId(Long osdId,
			OsdCategoryCd osdCategoryCd, String reportingSicCd, OsdStatusCd osdStatusCd,
			final TransactionContext txnContext, final EntityManager entityManager) throws ServiceException {
		ShmOsdHeader shmOsdHeader = null;
		ShmShipment shmShipment = null;
		ShmMovementExcp shmMovementExcp = null;
		Boolean isLegacyPro = Boolean.FALSE;
		shmOsdHeader = shmOsdHdrSubDAO.getByOsdIdOrProNumber(osdId, null, reportingSicCd, osdStatusCd, osdCategoryCd,
				Boolean.FALSE, entityManager);

		// Record Exist in OSD Tables
		if (Objects.nonNull(shmOsdHeader)) {
			// Case 1.a : osdCategoryCd is not Overage (Refused/Damaged/Short)
			// Case 1.b : osdCategoryCd is Overage and ProNbrTxt has values.
			if (Objects.nonNull(shmOsdHeader.getOsdCategoryCd())
					&& (!shmOsdHeader.getOsdCategoryCd().equals(OsdCategoryCd.OVERAGE.toString()))
					|| (shmOsdHeader.getOsdCategoryCd().equals(OsdCategoryCd.OVERAGE.toString())
							&& StringUtils.isNotBlank(shmOsdHeader.getProNbrTxt()))) {
				String elevenDigitProNumber = shmOsdHeader.getProNbrTxt();
				List<ShmShipment> shmShipments = shipmentSubDAO
						.findByProNumber(Collections.singletonList(elevenDigitProNumber), entityManager);

				if (shmShipments.size() > 0) {
					shmShipment = shmShipments.get(0);
					if (shmShipment.getShmHandlingUnits().size() == 0) {
						isLegacyPro = Boolean.TRUE;
						List<ShmMovementExcp> shmMovementExcps = shipmentMovementExceptionSubDAO
								.findByShpInstIds(Collections.singletonList(shmShipment.getShpInstId()), entityManager);
						shmMovementExcp = CollectionUtils.emptyIfNull(shmMovementExcps).stream()
								.sorted(Comparator.comparing(ShmMovementExcp::getCrteTmst).reversed()).findFirst()
								.orElse(null);

					}
				}
			}
		} else {
			// No Record Found Error based on osdId
			throw ExceptionBuilder.exception(NotFoundErrorMessage.OSD_NOT_FOUND, txnContext).log().build();
		}
		Quartet<ShmOsdHeader, ShmShipment, ShmMovementExcp, Boolean> shmOsdDetails = new Quartet<ShmOsdHeader, ShmShipment, ShmMovementExcp, Boolean>(
				shmOsdHeader, shmShipment, shmMovementExcp, isLegacyPro);
		return shmOsdDetails;
	}

	private Quartet<ShmOsdHeader, ShmShipment, ShmMovementExcp, Boolean> buildShmOsdDetailsFromProNbr(String proNbr,
			OsdCategoryCd osdCategoryCd, String reportingSicCd, OsdStatusCd osdStatusCd,
			final TransactionContext txnContext, final EntityManager entityManager) throws ServiceException {
		Quartet<ShmOsdHeader, ShmShipment, ShmMovementExcp, Boolean> shmOsdDetails = null;
		// Get Eleven Digit Pro Number
		String elevenDigitProNumber = ProNumberHelper.validateProNumber(proNbr, txnContext);
		// proNbr could be parent pro / child pro
		Boolean childProInd = ProNumberHelper.isYellowPro(elevenDigitProNumber);
		// Case 2.1 : ProNbr is parent Pro
		if (!childProInd) {
			shmOsdDetails = buildShmOsdDetailsFromParentProNbr(osdCategoryCd, reportingSicCd, osdStatusCd,
					elevenDigitProNumber, txnContext, entityManager);
		}
		// Case 2.1 : ProNbr is child Pro
		else {
			shmOsdDetails = buildShmOsdDetailsFromChildProNbr(osdCategoryCd, reportingSicCd, osdStatusCd,
					elevenDigitProNumber, txnContext, entityManager);
		}
		return shmOsdDetails;

	}

	private Quartet<ShmOsdHeader, ShmShipment, ShmMovementExcp, Boolean> buildShmOsdDetailsFromChildProNbr(
			OsdCategoryCd osdCategoryCd, String reportingSicCd, OsdStatusCd osdStatusCd, String elevenDigitProNumber,
			final TransactionContext txnContext, final EntityManager entityManager) throws ServiceException {
		ShmOsdHeader shmOsdHeader = null;
		ShmShipment shmShipment = null;
		ShmMovementExcp shmMovementExcp = null;
		Boolean isLegacyPro = Boolean.FALSE;
		// Fetch parent pro from child pro
		List<ShmHandlingUnit> shmhandlingUnits = shmHandlingUnitSubDAO
				.findByChildProNumberList(Collections.singleton(elevenDigitProNumber), entityManager);
		if (Objects.nonNull(shmhandlingUnits) && shmhandlingUnits.size() > 0) {
			elevenDigitProNumber = shmhandlingUnits.get(0).getParentProNbrTxt();
			shmOsdHeader = shmOsdHdrSubDAO.getByOsdIdOrProNumber(null, elevenDigitProNumber, reportingSicCd,
					osdStatusCd, osdCategoryCd, Boolean.FALSE, entityManager);
			List<ShmShipment> shmShipments = shipmentSubDAO
					.findByProNumber(Collections.singletonList(elevenDigitProNumber), entityManager);
			if (shmShipments.size() > 0) {
				shmShipment = shmShipments.get(0);
			}
		}

		// No parent pro available
		else {
			shmOsdHeader = shmOsdHdrSubDAO.getByOsdIdOrProNumber(null, elevenDigitProNumber, reportingSicCd,
					osdStatusCd, osdCategoryCd, Boolean.TRUE, entityManager);
			// For Overage Match Scenario, Entry will not be available in SHM_HANDLING_UNIT
			// Table from child pro, but must be available from parent pro.
			if (Objects.nonNull(shmOsdHeader)) {
				if (StringUtils.isNotBlank(shmOsdHeader.getProNbrTxt())) {
					List<ShmShipment> shmShipments = shipmentSubDAO
							.findByProNumber(Collections.singletonList(shmOsdHeader.getProNbrTxt()), entityManager);

					if (shmShipments.size() > 0) {
						isLegacyPro = Boolean.TRUE;
						shmShipment = shmShipments.get(0);
						List<ShmMovementExcp> shmMovementExcps = shipmentMovementExceptionSubDAO
								.findByShpInstIds(Collections.singletonList(shmShipment.getShpInstId()), entityManager);
						shmMovementExcp = CollectionUtils.emptyIfNull(shmMovementExcps).stream()
								.sorted(Comparator.comparing(ShmMovementExcp::getCrteTmst).reversed()).findFirst()
								.orElse(null);
					} else {
						// Pro# doesn't exist
						throw ExceptionBuilder.exception(NotFoundErrorMessage.PRO_NBR_NF, txnContext).log().build();
					}
				}
			} else {
				shmOsdHeader = shmOsdHdrSubDAO.getByOsdIdOrProNumber(null, elevenDigitProNumber, null, osdStatusCd,
						osdCategoryCd, Boolean.TRUE, entityManager);
				if (Objects.nonNull(shmOsdHeader)) {
					// Child Pro# exist but on different sic
					throw ExceptionBuilder.exception(NotFoundErrorMessage.PRO_NBR_NF, txnContext).log().build();
				} else {
					// Child Pro# doesn't exist
					throw ExceptionBuilder.exception(NotFoundErrorMessage.CHILD_PRO_NBR_NF, txnContext).log().build();
				}
			}
		}
		Quartet<ShmOsdHeader, ShmShipment, ShmMovementExcp, Boolean> shmOsdDetails = new Quartet<ShmOsdHeader, ShmShipment, ShmMovementExcp, Boolean>(
				shmOsdHeader, shmShipment, shmMovementExcp, isLegacyPro);
		return shmOsdDetails;
	}

	private Quartet<ShmOsdHeader, ShmShipment, ShmMovementExcp, Boolean> buildShmOsdDetailsFromParentProNbr(
			OsdCategoryCd osdCategoryCd, String reportingSicCd, OsdStatusCd osdStatusCd, String elevenDigitProNumber,
			final TransactionContext txnContext, final EntityManager entityManager) throws ServiceException {
		ShmOsdHeader shmOsdHeader = null;
		ShmShipment shmShipment = null;
		ShmMovementExcp shmMovementExcp = null;
		Boolean isLegacyPro = Boolean.FALSE;
		// Fetch OSD Information
		shmOsdHeader = shmOsdHdrSubDAO.getByOsdIdOrProNumber(null, elevenDigitProNumber, reportingSicCd, osdStatusCd,
				osdCategoryCd, Boolean.FALSE, entityManager);
		// Fetch Shipment Informations
		List<ShmShipment> shmShipments = shipmentSubDAO.findByProNumber(Collections.singletonList(elevenDigitProNumber),
				entityManager);

		if (shmShipments.size() == 0) {
			// Pro# doesn't exist
			throw ExceptionBuilder.exception(NotFoundErrorMessage.PRO_NBR_NF, txnContext).log().build();
		}
		shmShipment = shmShipments.get(0);
		if (shmShipment.getShmHandlingUnits().size() == 0) {
			isLegacyPro = Boolean.TRUE;
			List<ShmMovementExcp> shmMovementExcps = shipmentMovementExceptionSubDAO
					.findByShpInstIds(Collections.singletonList(shmShipment.getShpInstId()), entityManager);
			shmMovementExcp = CollectionUtils.emptyIfNull(shmMovementExcps).stream()
					.sorted(Comparator.comparing(ShmMovementExcp::getCrteTmst).reversed()).findFirst().orElse(null);

		}
		Quartet<ShmOsdHeader, ShmShipment, ShmMovementExcp, Boolean> shmOsdDetails = new Quartet<ShmOsdHeader, ShmShipment, ShmMovementExcp, Boolean>(
				shmOsdHeader, shmShipment, shmMovementExcp, isLegacyPro);
		return shmOsdDetails;
	}

	private GetOsdResp buildResponse(OsdPayloadTypeCd osdPayloadTypeCd,
			Quartet<ShmOsdHeader, ShmShipment, ShmMovementExcp, Boolean> shmOsdDetails,
			ShmShipment newParentShmShipment, String proNbr,
			final TransactionContext txnContext) throws ValidationException {
		GetOsdResp getOsdResp = new GetOsdResp();
		ShmOsdHeader shmOsdHeader = shmOsdDetails.getValue0();
		ShmShipment shmShipment = shmOsdDetails.getValue1();
		ShmMovementExcp shmMovementExcp = shmOsdDetails.getValue2();
		Boolean isLegacyPro = shmOsdDetails.getValue3();

		Map<String, InterfaceEmployee> employeeDetailsMap = getEmployeeDetailsMap(txnContext, shmOsdHeader);

		OsdParentShipment osdParentShipment = OsdEntityTransformer.buildOsdParentShipment(shmOsdHeader,
				shmShipment, shmMovementExcp, isLegacyPro, osdPayloadTypeCd, employeeDetailsMap, newParentShmShipment, proNbr, txnContext);

		List<OsdChildShipment> osdChildShipments = OsdEntityTransformer.buildOsdChildShipments(shmShipment,
				shmOsdHeader, osdPayloadTypeCd, proNbr);

		List<OsdManagementRemark> osdManagementRemarks = OsdEntityTransformer
				.buildOsdManagementRemarks(employeeDetailsMap, shmOsdHeader, osdPayloadTypeCd, proNbr);

		getOsdResp.setOsdParentShipment(osdParentShipment);
		getOsdResp.setOsdChildShipments(osdChildShipments);
		getOsdResp.setOsdManagementRemarks(osdManagementRemarks);
		return getOsdResp;
	}

	private Map<String, InterfaceEmployee> getEmployeeDetailsMap(final TransactionContext txnContext,
			ShmOsdHeader shmOsdHeader) {
		Map<String, InterfaceEmployee> employeeDetailsMap = null;
		if (Objects.nonNull(shmOsdHeader)) {
			List<String> userIds = CollectionUtils.emptyIfNull(shmOsdHeader.getShmMgmtRemarks()).stream()
					.filter(osdRemark -> StringUtils.isNotEmpty(osdRemark.getCrteBy()))
					.map(osdRemark -> osdRemark.getCrteBy()).distinct().collect(Collectors.toList());

			if (StringUtils.isNotBlank(shmOsdHeader.getDockWorkerUserid())) {
				userIds.add(shmOsdHeader.getDockWorkerUserid());
			}
			if (StringUtils.isNotBlank(shmOsdHeader.getAssignedUser())) {
				userIds.add(shmOsdHeader.getAssignedUser());
			}
			if (StringUtils.isNotBlank(shmOsdHeader.getCrteBy())) {
				userIds.add(shmOsdHeader.getCrteBy());
			}

			if (CollectionUtils.isNotEmpty(userIds)) {
				// Employee Details Map based on usedIds
				employeeDetailsMap = externalRestClient.getEmployeeDetailsMap(userIds, txnContext);
			}
		}
		return employeeDetailsMap;
	}

}
