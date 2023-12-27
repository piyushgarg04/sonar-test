package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.FoodPoisonCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.FoodPoisonCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentFoodAndMotorMovesRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.FormatHelper;
import com.xpo.ltl.shipment.service.validators.Checkers;

@RequestScoped
public class UpdateShipmentFoodAndMotorMovesImpl {

	@PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
	private EntityManager db2EntityManager;

	@Inject
	private ShmShipmentSubDAO shipmentDAO;

	public void updateShipmentFoodAndMotorMoves(UpdateShipmentFoodAndMotorMovesRqst updateShipmentFoodAndMotorMovesRqst,
			TransactionContext txnContext, EntityManager entityManager) throws ServiceException {

        checkNotNull(txnContext, ValidationErrorMessage.TXN_CONTEXT_REQUIRED.message());
        checkNotNull(entityManager, ValidationErrorMessage.ENTITY_MANAGER_REQUIRED.message());
        checkNotNull(updateShipmentFoodAndMotorMovesRqst, ValidationErrorMessage.REQUEST_REQUIRED.message());

		List<MoreInfo> moreInfos =  Lists.newArrayList();
		validateShmFoodMotorMovesRqst(updateShipmentFoodAndMotorMovesRqst, moreInfos);

		String proNumber = updateShipmentFoodAndMotorMovesRqst.getProNbr();
		Long shipmentInstId = updateShipmentFoodAndMotorMovesRqst.getShipmentInstId();
		Boolean motorizedPiecesKnownInd;

		String formattedProNbr = null;
		if (StringUtils.isNotBlank(proNumber)){
			try {
				formattedProNbr = FormatHelper.formatProNbrNumber(proNumber, txnContext);
			} catch (ServiceException exception) {
				moreInfos.add(createMoreInfo("updateShipmentFoodAndMotorMoves",
						ValidationErrorMessage.PRO_NBR_FORMAT_INVALID.message(),
						ValidationErrorMessage.PRO_NBR_FORMAT_INVALID.errorCode()));
			}
		}

		if (CollectionUtils.isNotEmpty(moreInfos)) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo(moreInfos)
					.build();
		}

		ShmShipment shmShipment = shipmentDAO.findByIdOrProNumber(formattedProNbr, shipmentInstId,
				entityManager);

		if (Objects.isNull(shmShipment)){
			String moreInfoMessage = StringUtils.isNotBlank(proNumber) ?
					"PRO Number " + proNumber : "ShipmentInstId " + shipmentInstId;
			throw ExceptionBuilder.exception(NotFoundErrorMessage.SHIPMENT_NOT_FOUND, txnContext)
					.moreInfo("Shipment entities", moreInfoMessage)
					.build();
		}

		FoodPoisonCd foodPoisonCd = updateShipmentFoodAndMotorMovesRqst.getFoodPoisonCd();
		Boolean bulkLiquidInd = updateShipmentFoodAndMotorMovesRqst.getBulkLiquidInd();
		AuditInfo auditInfo = new AuditInfo();
		AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);
		String capturedByUserId = updateShipmentFoodAndMotorMovesRqst.getCapturedByUserId();
		if (StringUtils.isNotBlank(capturedByUserId)) {
			auditInfo.setUpdateById(capturedByUserId);
		}

		if (Objects.nonNull(foodPoisonCd)) {
			shmShipment.setFoodPoisonCd(FoodPoisonCdTransformer.toCode(foodPoisonCd));
		}
		if (Objects.nonNull(bulkLiquidInd)) {
			shmShipment.setBulkLqdInd(BasicTransformer.toString(bulkLiquidInd));
		}

		validateAndSetShipmentMotorizedPiecesCount(updateShipmentFoodAndMotorMovesRqst, shmShipment,
				txnContext);
		
		/* if updateMotorMovesInd is false then in current code we are setting MTRZ_PCS_KNWN_IND = N
		 So before setting it to N check MM count if it is greater than 0 set it to Y instead of N*/
		if (updateShipmentFoodAndMotorMovesRqst.getMotorizedPiecesCount().longValue() > 0) {
			motorizedPiecesKnownInd = true;
		} else {
			motorizedPiecesKnownInd = BooleanUtils.isTrue(
					updateShipmentFoodAndMotorMovesRqst.getUpdateMotorMovesInd());
		}
		
		shmShipment.setMtrzdPcsKnwnInd(BasicTransformer.toString(motorizedPiecesKnownInd));
		shmShipment.setLoosePcsCnt(
				new BigDecimal(updateShipmentFoodAndMotorMovesRqst.getLoosePiecesCount()));

		DtoTransformer.setAuditInfo(shmShipment, auditInfo);

		shipmentDAO.save(shmShipment, entityManager);
		shipmentDAO.updateDB2ShmShipment(shmShipment, shmShipment.getLstUpdtTmst(), txnContext,
				db2EntityManager);

		entityManager.flush();
		db2EntityManager.flush();

	}

	private void validateShmFoodMotorMovesRqst(
			UpdateShipmentFoodAndMotorMovesRqst updateShipmentFoodAndMotorMovesRqst,
			List<MoreInfo> moreInfoList) {

		if (Objects.isNull(moreInfoList)) {
			moreInfoList = Lists.newArrayList();
		}
		if (Objects.isNull(updateShipmentFoodAndMotorMovesRqst)) {
			moreInfoList.add(createMoreInfo("updateShipmentFoodAndMotorMovesRqst",
					ValidationErrorMessage.REQUEST_REQUIRED.message(),
					ValidationErrorMessage.REQUEST_REQUIRED.errorCode()));
		} else {
			String proNumber = updateShipmentFoodAndMotorMovesRqst.getProNbr();
			Long shipmentInstId = updateShipmentFoodAndMotorMovesRqst.getShipmentInstId();

			if (StringUtils.isBlank(proNumber) && Objects.isNull(shipmentInstId)) {
				moreInfoList.add(createMoreInfo("updateShipmentFoodAndMotorMovesRqst",
						ValidationErrorMessage.SHIP_ID_OR_PRO_IS_REQUIRED.message(),
						ValidationErrorMessage.SHIP_ID_OR_PRO_IS_REQUIRED.errorCode()));
			}

			if (BooleanUtils.isTrue(updateShipmentFoodAndMotorMovesRqst.getUpdateMotorMovesInd())) {
				BigInteger motorizedPiecesCount = updateShipmentFoodAndMotorMovesRqst
						.getMotorizedPiecesCount();
				if (Objects.isNull(motorizedPiecesCount)) {
					moreInfoList.add(createMoreInfo("motorizedPiecesCount",
							ValidationErrorMessage.MOTORIZED_PIECES_COUNT_REQ.message(),
							ValidationErrorMessage.MOTORIZED_PIECES_COUNT_REQ.errorCode()));
				} else if (motorizedPiecesCount.compareTo(BigInteger.ZERO) < 0) {
					moreInfoList.add(createMoreInfo("motorizedPiecesCount",
							ValidationErrorMessage.MPIECES_COUNT_GREATER_ZERO.message(),
							ValidationErrorMessage.MPIECES_COUNT_GREATER_ZERO.errorCode()));
				}
			}

			BigInteger loosePiecesCount = updateShipmentFoodAndMotorMovesRqst.getLoosePiecesCount();
			if (Objects.nonNull(loosePiecesCount)) {
				if (loosePiecesCount.compareTo(BigInteger.ZERO) < 0) {
					moreInfoList.add(createMoreInfo("loosePiecesCount",
							ValidationErrorMessage.LPIECES_COUNT_GREATER_ZERO.message(),
							ValidationErrorMessage.LPIECES_COUNT_GREATER_ZERO.errorCode()));
				}
			}
		}
	}

	private void validateAndSetShipmentMotorizedPiecesCount(
			UpdateShipmentFoodAndMotorMovesRqst updateShipmentFoodAndMotorMovesRqst,
			ShmShipment shmShipment, TransactionContext txnContext)
			throws com.xpo.ltl.api.exception.ValidationException {

		if (BooleanUtils.isTrue(updateShipmentFoodAndMotorMovesRqst.getUpdateMotorMovesInd())) {

			BigDecimal motorizedPiecesCount = new BigDecimal(
					updateShipmentFoodAndMotorMovesRqst.getMotorizedPiecesCount());

			shmShipment.setMtrzdPcsCnt(motorizedPiecesCount);
		}
	}

	private MoreInfo createMoreInfo(String location, String message, String errorCode) {
		MoreInfo moreInfo = new MoreInfo();
		moreInfo.setItemNbr(null);
		moreInfo.setErrorCode(errorCode);
		moreInfo.setMessage(message);
		moreInfo.setLocation(location);
		return moreInfo;
	}
}
