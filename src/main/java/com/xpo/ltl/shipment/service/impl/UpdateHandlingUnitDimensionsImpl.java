package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmLnhDimension;
import com.xpo.ltl.api.shipment.service.entity.ShmLnhDimensionPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentVolumeMethodCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentVolumeTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.EventLogSubTypeCd;
import com.xpo.ltl.api.shipment.v2.EventLogTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentVolumeMethodCd;
import com.xpo.ltl.api.shipment.v2.ShipmentVolumeTypeCd;
import com.xpo.ltl.api.shipment.v2.UpdateHandlingUnitDimensionsResp;
import com.xpo.ltl.api.shipment.v2.UpdateHandlingUnitDimensionsRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmLnhDimensionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmEventDelegate;
import com.xpo.ltl.shipment.service.delegates.ShmHandlingUnitDelegate;
import com.xpo.ltl.shipment.service.delegates.ShmLnhDimensionDelegate;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import com.xpo.ltl.shipment.service.validators.UpdateHandlingUnitDimensionsValidator;

@RequestScoped
public class UpdateHandlingUnitDimensionsImpl {

	@PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
	private EntityManager db2EntityManager;

	@Inject
	private UpdateHandlingUnitDimensionsValidator validator;

	@Inject
	private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

	@Inject
	private ShmShipmentSubDAO shmShipmentSubDAO;

	@Inject
	private ShmLnhDimensionSubDAO shmLnhDimensionSubDAO;

    @Inject
    private ShmEventDelegate shmEventDelegate;

    @Inject
    private ShmLnhDimensionDelegate shmLnhDimensionDelegate;

    @Inject
    private ShmHandlingUnitDelegate shmHandlingUnitDelegate;

    private static final String HU_DIM_UPDT_PGM_ID = "HUDIMUPD";

	public UpdateHandlingUnitDimensionsResp updateHandlingUnitDimensions(
			UpdateHandlingUnitDimensionsRqst updateHandlingUnitDimensionsRqst,
        Double totalVolumeCubicFeet, Double pupVolumePercentage, ShipmentVolumeTypeCd cubicFeetProfileTypeCd, ShipmentVolumeMethodCd cubicFeetProfileMthdCd, String trackingProNbr,
        Optional<AuditInfo> auditInfoOpt,
        TransactionContext txnContext,
        EntityManager entityManager)
			throws ServiceException {

        checkNotNull(cubicFeetProfileMthdCd, "cubicFeetProfileMthdCd is required.");

        if (pupVolumePercentage == null || pupVolumePercentage == 0 || totalVolumeCubicFeet == null
                || totalVolumeCubicFeet == 0) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo("updateHandlingUnitDimensions", "TOT_VOL_CFT_NOT_CALCULATED or PUP_VOL_PCT_NOT_CALCULATED.")
                .build();
        }

		validator.validate(updateHandlingUnitDimensionsRqst, trackingProNbr,
				txnContext, entityManager);

        AuditInfo auditInfo = auditInfoOpt
            .orElse(AuditInfoHelper.getAuditInfoWithPgmId(HU_DIM_UPDT_PGM_ID, txnContext));

		double height = updateHandlingUnitDimensionsRqst.getHeightNbr();
		double width = updateHandlingUnitDimensionsRqst.getWidthNbr();
		double length = updateHandlingUnitDimensionsRqst.getLengthNbr();

		String capturedByUserId = updateHandlingUnitDimensionsRqst.getCapturedByUserId();
		String requestingSicCd = updateHandlingUnitDimensionsRqst.getRequestingSicCd();
		String dimensionTypeCd = updateHandlingUnitDimensionsRqst.getDimensionTypeCd();

		String formattedProNumber = ProNumberHelper.toElevenDigitPro(trackingProNbr, txnContext);

		UpdateHandlingUnitDimensionsResp resp = new UpdateHandlingUnitDimensionsResp();

		if (ProNumberHelper.isBluePro(trackingProNbr)) {
			// Update parent only
			ShmShipment shmShipment = shmShipmentSubDAO.findByIdOrProNumber(formattedProNumber,
					null, entityManager);

			if (Objects.nonNull(shmShipment)
					&& BasicTransformer.toBoolean(shmShipment.getHandlingUnitExemptionInd())) {

                // Removed by LPPLT-1738
                /*
                 * checkCurrentSicIsRequestingSicOrError(shmShipment.getCurrSicCd(), requestingSicCd,
                 * txnContext);
                 */

                updateShmShipmentVolume(shmShipment, totalVolumeCubicFeet, pupVolumePercentage,
                    cubicFeetProfileTypeCd, cubicFeetProfileMthdCd, auditInfo, txnContext, entityManager);

			} else {

				throw ExceptionBuilder.exception(NotFoundErrorMessage.SHIPMENT_NF, txnContext)
						.contextValues("Parent Pro Number: ", trackingProNbr)
						.moreInfo("UpdateHandlingUnitDimensionsImpl",
								"Parent Pro Number: " + trackingProNbr)
						.log().build();
			}
		} else {
			// Find parent and update all
			ShmHandlingUnit shmHandlingUnit = shmHandlingUnitSubDAO
					.findByTrackingProNumber(formattedProNumber, entityManager);

			if (Objects.isNull(shmHandlingUnit)) {
				throw ExceptionBuilder
						.exception(NotFoundErrorMessage.SHM_HANDLING_UNIT_NOT_FOUND, txnContext)
						.moreInfo("UpdateHandlingUnitDimensionsImpl",
								"Tracking Pro Number: " + trackingProNbr)
						.log().build();
			}

            // Removed by LPPLT-1738
			/* checkCurrentSicIsRequestingSicOrError(shmHandlingUnit.getCurrentSicCd(),
					requestingSicCd, txnContext);
			*/
			ShmShipment shmShipment = shmShipmentSubDAO
					.findByIdOrProNumber(shmHandlingUnit.getParentProNbrTxt(), null, entityManager);

			if (Objects.isNull(shmShipment)) {
				throw ExceptionBuilder.exception(NotFoundErrorMessage.SHIPMENT_NF, txnContext)
						.contextValues("Parent Pro Number: ", trackingProNbr)
						.moreInfo("UpdateHandlingUnitDimensionsImpl",
								"Parent Pro Number: " + trackingProNbr)
						.log().build();
			}

			// Update handling unit
            shmHandlingUnit = shmHandlingUnitDelegate.updateShmHandlingUnitDimensions(shmHandlingUnit, capturedByUserId,
                requestingSicCd, height, width, length, totalVolumeCubicFeet, pupVolumePercentage, dimensionTypeCd,
                auditInfo, txnContext, entityManager);

            entityManager.flush();
            db2EntityManager.flush();

			// Find all handling units
			List<ShmHandlingUnit> shmHandlingUnits = shmHandlingUnitSubDAO
					.findByParentProNumber(shmShipment.getProNbrTxt(), entityManager);

            double sumPupVolumePercentage = CollectionUtils
                .emptyIfNull(shmHandlingUnits)
                .stream()
                .filter(hu -> hu != null && hu.getPupVolPct() != null)
                .mapToDouble(hu -> BasicTransformer.toDouble(hu.getPupVolPct()))
                .sum();
            double sumTotalVolumeCubicFeet = CollectionUtils
                .emptyIfNull(shmHandlingUnits)
                .stream()
                .filter(hu -> hu != null && hu.getVolCft() != null)
                .mapToDouble(hu -> BasicTransformer.toDouble(hu.getVolCft()))
                .sum();

            boolean mustCreateCubeUpdate = NumberUtils.compare(BasicTransformer.toDouble(shmShipment.getPupVolPct()), sumPupVolumePercentage) != 0;
            // Update parent
            updateShmShipmentVolume(shmShipment, sumTotalVolumeCubicFeet, sumPupVolumePercentage,
                cubicFeetProfileTypeCd, cubicFeetProfileMthdCd, auditInfo, txnContext, entityManager);

			// Update lnh dimension
			ShmLnhDimensionPK pk = new ShmLnhDimensionPK();
			pk.setShpInstId(shmHandlingUnit.getId().getShpInstId());
			pk.setDimSeqNbr(shmHandlingUnit.getId().getSeqNbr());
			ShmLnhDimension shmLnhDimension = shmLnhDimensionSubDAO.findById(pk, entityManager);

			if (shmLnhDimension != null) {
                shmLnhDimensionDelegate.updateShmLnhDimension(shmLnhDimension, BasicTransformer.toBigDecimal(height), BasicTransformer.toBigDecimal(width), BasicTransformer.toBigDecimal(length),
                    capturedByUserId,
                    dimensionTypeCd, auditInfo, entityManager, txnContext);
			} else {
                shmLnhDimensionDelegate.createShmLnhDimension(pk, BasicTransformer.toBigDecimal(height), BasicTransformer.toBigDecimal(width), BasicTransformer.toBigDecimal(length), capturedByUserId,
                    dimensionTypeCd, auditInfo, entityManager, txnContext);
			}

			if (mustCreateCubeUpdate) {
                shmEventDelegate
                .createEvent(0L, EventLogTypeCd.CORRECTIONS, EventLogSubTypeCd.CUBE_UPDATE, shmShipment, null,
                    requestingSicCd, Optional.empty(), "UPDM", entityManager, auditInfo);
			}
			
			resp.setHandlingUnit(EntityTransformer.toHandlingUnit(shmHandlingUnit));
		}

		return resp;
	}

	private void checkCurrentSicIsRequestingSicOrError(String currSicCd, String requestingSicCd,
			TransactionContext txnContext) throws ValidationException {

		if (StringUtils.isNotBlank(currSicCd)
				&& !StringUtils.equals(currSicCd, requestingSicCd)) {

			throw ExceptionBuilder
					.exception(ValidationErrorMessage.RQST_SIC_NOT_MATCH_SHPMT_SIC, txnContext)
					.contextValues(requestingSicCd, currSicCd)
					.log().build();
		}
	}


    private void updateShmShipmentVolume(ShmShipment shmShipment, double volume, double pupVolPct,
        ShipmentVolumeTypeCd cubicFeetProfileTypeCd, ShipmentVolumeMethodCd cubicFeetProfileMthdCd, AuditInfo auditInfo,
			TransactionContext txnContext, EntityManager entityManager) throws ServiceException {

		shmShipment.setTotVolCft(BasicTransformer.toBigDecimal(volume));
        shmShipment.setPupVolPct(BasicTransformer.toBigDecimal(pupVolPct));
        shmShipment.setCftPrflMthdCd(ShipmentVolumeMethodCdTransformer.toCode(cubicFeetProfileMthdCd));
        shmShipment.setCftPrflTypeCd(
            ObjectUtils.defaultIfNull(ShipmentVolumeTypeCdTransformer.toCode(cubicFeetProfileTypeCd),
                StringUtils.SPACE));
		DtoTransformer.setLstUpdateAuditInfo(shmShipment, auditInfo);

		shmShipment = shmShipmentSubDAO.save(shmShipment, entityManager);
		shmShipmentSubDAO.updateDB2ShmShipment(shmShipment, shmShipment.getLstUpdtTmst(),
				txnContext, db2EntityManager);
	}

}
