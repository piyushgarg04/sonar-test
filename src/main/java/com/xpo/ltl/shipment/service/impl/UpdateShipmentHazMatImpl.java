package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmHazMat;
import com.xpo.ltl.api.shipment.service.entity.ShmHazMatPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.BillStatusCd;
import com.xpo.ltl.api.shipment.v2.HazMat;
import com.xpo.ltl.api.shipment.v2.Shipment;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentHazMatResp;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentHazMatRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmHazMatSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.FormatHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RequestScoped
public class UpdateShipmentHazMatImpl {

	private static final Set<String> VALID_BULK_QUANTITY_CODES = Sets.newHashSet("N", "B", "L");

	@Inject
	private ShmShipmentSubDAO shipmentDAO;

	@Inject
	private ShmHazMatSubDAO hazmatDAO;

	@PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
	private EntityManager db2EntityManager;

	public UpdateShipmentHazMatResp updateShipmentHazMat(
			UpdateShipmentHazMatRqst updateShipmentHazMatRqst, TransactionContext txnContext,
			EntityManager entityManager) throws ServiceException {
		checkNotNull(updateShipmentHazMatRqst, ValidationErrorMessage.REQUEST_REQUIRED.message());
		checkNotNull(txnContext, ValidationErrorMessage.TXN_CONTEXT_REQUIRED.message());
		checkNotNull(entityManager, ValidationErrorMessage.ENTITY_MANAGER_REQUIRED.message());

		List<MoreInfo> moreInfos = Lists.newArrayList();

		validateRequest(updateShipmentHazMatRqst, moreInfos);

		String formattedProNbr = null;
		if (Objects.nonNull(updateShipmentHazMatRqst.getProNbr())) {
			try {
				formattedProNbr = FormatHelper
						.formatProNbrNumber(updateShipmentHazMatRqst.getProNbr(), txnContext);
			} catch (ServiceException exception) {
				moreInfos.add(createMoreInfo("updateShipmentHazMat",
						ValidationErrorMessage.PRO_NBR_FORMAT_INVALID.message(),
						ValidationErrorMessage.PRO_NBR_FORMAT_INVALID.errorCode()));
			}
		}
		
		if (CollectionUtils.isNotEmpty(moreInfos)) {
			throw ExceptionBuilder
					.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo(moreInfos).build();
		}

		if (CollectionUtils.isNotEmpty(updateShipmentHazMatRqst.getHazMats())) {
			updateShipmentHazMatRqst.getHazMats()
					.forEach(hazMat -> validateHazMat(hazMat, moreInfos));
		}

		if (CollectionUtils.isNotEmpty(moreInfos)) {
			throw ExceptionBuilder
					.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo(moreInfos).build();
		}

		ShmShipment shmShipment = shipmentDAO.findByIdOrProNumber(formattedProNbr,
				updateShipmentHazMatRqst.getShipmentInstId(), entityManager);

		if (Objects.isNull(shmShipment)) {
			String moreInfoMessage = StringUtils.isNotBlank(formattedProNbr)
					? "PRO Number " + formattedProNbr
					: "ShpInstId " + updateShipmentHazMatRqst.getShipmentInstId();
			throw ExceptionBuilder.exception(NotFoundErrorMessage.SHIPMENT_NOT_FOUND, txnContext)
					.moreInfo("Shipment entities", moreInfoMessage).build();
		}

		final Shipment shipment = EntityTransformer.toShipment(shmShipment);

		if (!BillStatusCd.UNBILLED.equals(shipment.getBillStatusCd())) {
			if (CollectionUtils.isEmpty(updateShipmentHazMatRqst.getHazMats())) {
			
				moreInfos.add(createMoreInfo("updateShipmentHazMat",
						ValidationErrorMessage.HZM_DETAILS_REQUIRED_MARK_HZM.message(),
						ValidationErrorMessage.HZM_DETAILS_REQUIRED_MARK_HZM.errorCode()));

			} else if (!shipment.isHazmatInd()) {

				moreInfos.add(createMoreInfo("updateShipmentHazMat",
						ValidationErrorMessage.HZM_DETAILS_BILLED_SHM_NOALLOW.message(),
						ValidationErrorMessage.HZM_DETAILS_BILLED_SHM_NOALLOW.errorCode()));
			}
		}
		
		if (CollectionUtils.isNotEmpty(moreInfos)) {
			throw ExceptionBuilder
					.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo(moreInfos).build();
		}


		// Update parent shipment hazmat indicator
		boolean isRequestHazMat = CollectionUtils.isNotEmpty(updateShipmentHazMatRqst.getHazMats());
		if (shipment.isHazmatInd() != isRequestHazMat) {
			shmShipment.setHazmatInd(BasicTransformer.toString(isRequestHazMat));
			shmShipment = shipmentDAO.save(shmShipment, entityManager);
			shipmentDAO.updateDB2ShmShipment(shmShipment, shmShipment.getLstUpdtTmst(), txnContext,
					db2EntityManager);
		}

		// Delete existing hazmat records
		List<ShmHazMat> hazMats = hazmatDAO.findAllByShpInstId(shipment.getShipmentInstId(),
				entityManager);
		if (CollectionUtils.isNotEmpty(hazMats)) {
			hazmatDAO.remove(hazMats, entityManager);
			hazmatDAO.removeDB2ShmHazMats(hazMats, db2EntityManager, txnContext);
		}

		// Create new SHM_HAZ_MAT records for each group
		List<ShmHazMat> shmHazMatsCreated = Lists.newArrayList();
		if (CollectionUtils.isNotEmpty(updateShipmentHazMatRqst.getHazMats())) {
			for (HazMat hazMat : updateShipmentHazMatRqst.getHazMats()) {
				shmHazMatsCreated.add(createShmHazMat(hazMat, shipment.getShipmentInstId(),
						entityManager, txnContext));
			}
		}

		List<HazMat> respHazMats = EntityTransformer.toHazMat(shmHazMatsCreated);

		UpdateShipmentHazMatResp updateShipmentHazMatResp = new UpdateShipmentHazMatResp();
		updateShipmentHazMatResp.setHazMats(respHazMats);
		return updateShipmentHazMatResp;
	}

	private ShmHazMat createShmHazMat(final HazMat hazMat, final long shpInstId,
			EntityManager entityManager, TransactionContext txnContext) throws ServiceException {
		ShmHazMat shmHazMat = new ShmHazMat();
		shmHazMat.setId(new ShmHazMatPK());
		AuditInfo auditInfo = new AuditInfo();
		AuditInfoHelper.setCreatedInfo(auditInfo, txnContext);
		AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);
		DtoTransformer.setAuditInfo(shmHazMat, auditInfo);
		DtoTransformer.toShmHazMat(hazMat, shmHazMat);
		long hmSeqNbr = hazmatDAO.getNextSeqNbrByShpInstId(shpInstId, entityManager);
		shmHazMat.getId().setHmSeqNbr(hmSeqNbr);
		shmHazMat.getId().setShpInstId(shpInstId);

		if (StringUtils.isBlank(shmHazMat.getSourceCd())) {
			shmHazMat.setSourceCd(StringUtils.SPACE);
		}
		if (StringUtils.isBlank(shmHazMat.getZoneCd())) {
			shmHazMat.setZoneCd(StringUtils.SPACE);
		}
		if (StringUtils.isBlank(shmHazMat.getPackingGrpCd())) {
			shmHazMat.setPackingGrpCd(StringUtils.SPACE);
		}
		if (StringUtils.isBlank(shmHazMat.getClassLbl())) {
			shmHazMat.setClassLbl(StringUtils.SPACE);
		}
		if (StringUtils.isBlank(shmHazMat.getOvrdMethodNm())) {
			shmHazMat.setOvrdMethodNm(StringUtils.SPACE);
		}

		ShmHazMat savedShmHazMat = hazmatDAO.save(shmHazMat, entityManager);
		hazmatDAO.insertDB2ShmHazMat(shmHazMat, db2EntityManager);

		return savedShmHazMat;
	}

	private void validateRequest(UpdateShipmentHazMatRqst updateShipmentHazMatRqst,
			List<MoreInfo> moreInfos) {
		String proNumber = updateShipmentHazMatRqst.getProNbr();
		Long shipmentInstId = updateShipmentHazMatRqst.getShipmentInstId();

		if (StringUtils.isBlank(proNumber) && (shipmentInstId == null || shipmentInstId == 0)) {
			moreInfos.add(createMoreInfo("UpdateShipmentHazMatRqst",
					ValidationErrorMessage.SHIP_ID_OR_PRO_IS_REQUIRED.message(),
					ValidationErrorMessage.SHIP_ID_OR_PRO_IS_REQUIRED.errorCode()));
		}
	}

	private void validateHazMat(HazMat hazMat, List<MoreInfo> moreInfos) {
		if (Objects.isNull(hazMat.getHazmatWeightLbs()) || hazMat.getHazmatWeightLbs() <= 0) {
			moreInfos.add(createMoreInfo("hazmatWeightLbs",
					ValidationErrorMessage.HAZ_MAT_WEIGHT_REQD.message(),
					ValidationErrorMessage.HAZ_MAT_WEIGHT_REQD.errorCode()));
		}
		if (StringUtils.isBlank(hazMat.getHazmatUnna())) {
			moreInfos.add(createMoreInfo("hazmatUnna",
					ValidationErrorMessage.HAZ_MAT_UNNA_CODE_REQD.message(),
					ValidationErrorMessage.HAZ_MAT_UNNA_CODE_REQD.errorCode()));
		}
		if (StringUtils.isBlank(hazMat.getHazmatClassCd())) {
			moreInfos.add(createMoreInfo("hazmatClassCd",
					ValidationErrorMessage.HAZ_MAT_HAZARD_CLASS_CD_REQD.message(),
					ValidationErrorMessage.HAZ_MAT_HAZARD_CLASS_CD_REQD.errorCode()));
		}
		if (!VALID_BULK_QUANTITY_CODES.contains(hazMat.getHazmatBulkQuantityCd())) {
			moreInfos.add(createMoreInfo("hazmatBulkQuantityCd",
					ValidationErrorMessage.HAZ_MAT_BULK_QTY_CD_INV.message(),
					ValidationErrorMessage.HAZ_MAT_BULK_QTY_CD_INV.errorCode()));
		}
		if (Objects.isNull(hazMat.getHazmatResidueInd())) {
			moreInfos.add(createMoreInfo("hazmatResidueInd",
					ValidationErrorMessage.HAZ_MAT_RESIDUE_IND_INV.message(),
					ValidationErrorMessage.HAZ_MAT_RESIDUE_IND_INV.errorCode()));
		}
	}

	private MoreInfo createMoreInfo(String location, String message, String errorCode) {
		MoreInfo moreInfo = new MoreInfo();
		moreInfo.setItemNbr(null);
		moreInfo.setMessage(message);
		moreInfo.setLocation(location);
		moreInfo.setErrorCode(errorCode);
		return moreInfo;
	}
}
