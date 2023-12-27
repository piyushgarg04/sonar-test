package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmLnhDimension;
import com.xpo.ltl.api.shipment.service.entity.ShmLnhDimensionPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentVolumeMethodCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentVolumeTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.EventLogSubTypeCd;
import com.xpo.ltl.api.shipment.v2.EventLogTypeCd;
import com.xpo.ltl.api.shipment.v2.LnhDimension;
import com.xpo.ltl.api.shipment.v2.ShipmentVolumeMethodCd;
import com.xpo.ltl.api.shipment.v2.ShipmentVolumeTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentWithDimension;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentLinehaulDimensionsRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmLnhDimensionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentEagerLoadPlan;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmEventDelegate;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.TimestampUtil;

@ApplicationScoped
@LogExecutionTime
public class UpdateShipmentLinehaulDimensionsImpl {

    private static final String UPDATE_LNH_DIMENSION_PGM_ID = "LHDIMUPD";

    @Inject
    private ShmShipmentSubDAO shmShipmentSubDAO;

    @Inject
    private ShmEventDelegate shmEventDelegate;

    @Inject
    private ShmLnhDimensionSubDAO shmLnhDimensionSubDAO;

    @PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
    private EntityManager db2EntityManager;

    public void updateShipmentLinehaulDimensions(
        UpdateShipmentLinehaulDimensionsRqst updateShipmentLinehaulDimensionsRqst, Double pupVolumePercentage,
        Double totalVolumeCubicFeet, ShipmentVolumeMethodCd cubicFeetProfileMthdCd,
        ShipmentVolumeTypeCd cubicFeetProfileTypeCd, boolean mustCreateCubeUpdt, EntityManager entityManager,
        TransactionContext txnContext) throws ServiceException {

        AuditInfo auditInfo = null;

        if (StringUtils.isNotBlank(updateShipmentLinehaulDimensionsRqst.getCapturedByUserId())) {
            auditInfo = AuditInfoHelper.getAuditInfoWithPgmAndUserId(UPDATE_LNH_DIMENSION_PGM_ID,
                updateShipmentLinehaulDimensionsRqst.getCapturedByUserId(), txnContext);
        } else {
            auditInfo = AuditInfoHelper.getAuditInfoWithPgmId(UPDATE_LNH_DIMENSION_PGM_ID, txnContext);
        }

        checkNotNull(cubicFeetProfileMthdCd, "cubicFeetProfileMthdCd is required.");

        if (Objects.isNull(totalVolumeCubicFeet) || Objects.isNull(pupVolumePercentage)) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.TOT_VOL_CFT_IS_REQUIRED, txnContext)
                .moreInfo("updateShipmentLinehaulDimensions",
                    "totalVolumeCubicFeet and pupVolumePercentage are required.")
                .build();

        } else if (pupVolumePercentage <= 0) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.PUP_VOL_PCT_NOT_CALCULATED, txnContext)
                .moreInfo("updateShipmentLinehaulDimensions", "pupVolumePercentage must be positive.")
                .build();
        }

        if(CollectionUtils.isEmpty(updateShipmentLinehaulDimensionsRqst.getShipmentWithDimensions())){
            throw ExceptionBuilder
                    .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                    .moreInfo("updateShipmentLinehaulDimensions", "ShipmentWithDimensions cannot be empty or null.")
                    .build();
        }

        for(ShipmentWithDimension shipmentWithDimension: updateShipmentLinehaulDimensionsRqst.getShipmentWithDimensions()) {

            if (CollectionUtils.isEmpty(shipmentWithDimension.getLinehaulDimensions())) {
                throw ExceptionBuilder
                        .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                        .moreInfo("updateShipmentLinehaulDimensions", "linehaulDimensions cannot be empty or null.")
                        .build();
            }

            Long shipmentInstId = shipmentWithDimension.getLinehaulDimensions().get(0).getShipmentInstId();

            validateDimensionData(shipmentInstId, shipmentWithDimension.getLinehaulDimensions(), txnContext);

            ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan =
                new ShmShipmentEagerLoadPlan();
            shmShipmentEagerLoadPlan.setShmAsEntdCusts(true);

            ShmShipment shmShipment =
                shmShipmentSubDAO.findByProOrShipmentId
                    (null,
                     null,
                     shipmentInstId,
                     false,
                     shmShipmentEagerLoadPlan,
                     entityManager);

            if (shmShipment == null) {
                throw ExceptionBuilder
                        .exception(NotFoundErrorMessage.SHIPMENT_NOT_FOUND, txnContext)
                        .moreInfo("shipmentInstId", BasicTransformer.toString(shipmentInstId))
                        .build();
            }

            shmLnhDimensionSubDAO.bulkDeleteByShipmentInstId(shipmentInstId, entityManager);
            shmLnhDimensionSubDAO.bulkDeleteByShipmentInstIdFromDB2(shipmentInstId, db2EntityManager);

            entityManager.flush();
            db2EntityManager.flush();

            int dimSeqNbr = 0;
            for (LnhDimension lnhDimension : shipmentWithDimension.getLinehaulDimensions()) {

                dimSeqNbr++;
                ShmLnhDimension shmLnhDimension = new ShmLnhDimension();
                ShmLnhDimensionPK id = new ShmLnhDimensionPK();
                id.setShpInstId(shipmentInstId);
                id.setDimSeqNbr(dimSeqNbr);
                shmLnhDimension.setId(id);
                DtoTransformer.setAuditInfo(shmLnhDimension, auditInfo);
                if (auditInfo != null) {
                    shmLnhDimension.setCapturedByCd(
                        lnhDimension.getCapturedByCd() != null ? lnhDimension.getCapturedByCd() : StringUtils.SPACE);
                    shmLnhDimension.setCapturedByTmst(DtoTransformer.toTimestamp(auditInfo.getUpdatedTimestamp()));
                    shmLnhDimension.setCapturedByUid(auditInfo.getUpdateById());
                }
                shmLnhDimension.setDmlTmst(TimestampUtil.getLowTimestamp());
                shmLnhDimension.setShmShipment(shmShipment);

                shmLnhDimension.setPcsCnt(BasicTransformer.toBigDecimal(lnhDimension.getPiecesCount()));
                shmLnhDimension
                    .setLenNbr(Optional
                        .ofNullable(BasicTransformer.toBigDecimal(lnhDimension.getLengthNbr()))
                        .orElse(BigDecimal.ZERO));
                shmLnhDimension
                    .setWdthNbr(Optional
                        .ofNullable(BasicTransformer.toBigDecimal(lnhDimension.getWidthNbr()))
                        .orElse(BigDecimal.ZERO));
                shmLnhDimension
                    .setHghtNbr(Optional
                        .ofNullable(BasicTransformer.toBigDecimal(lnhDimension.getHeightNbr()))
                        .orElse(BigDecimal.ZERO));
                shmLnhDimension.setStackableInd(BasicTransformer
                    .toString(lnhDimension.getStackableInd() != null ? lnhDimension.getStackableInd() : true));

                shmLnhDimensionSubDAO.persist(shmLnhDimension, entityManager);
                shmLnhDimensionSubDAO.createDB2ShmLnhDimension(shmLnhDimension, db2EntityManager);

            }

            final BigDecimal totVol = BasicTransformer.toBigDecimal(totalVolumeCubicFeet);
            final BigDecimal pupVolPct = BasicTransformer.toBigDecimal(pupVolumePercentage);

            if (!totVol.equals(shmShipment.getTotVolCft()) || !pupVolPct.equals(shmShipment.getPupVolPct())) {

                shmShipment.setPupVolPct(pupVolPct);
                shmShipment.setTotVolCft(totVol);
                shmShipment.setCftPrflMthdCd(ShipmentVolumeMethodCdTransformer.toCode(cubicFeetProfileMthdCd));
                shmShipment
                    .setCftPrflTypeCd(ObjectUtils
                        .defaultIfNull(ShipmentVolumeTypeCdTransformer.toCode(cubicFeetProfileTypeCd),
                            StringUtils.SPACE));

                // fix for some oracle db data with null (not allowed in db2)
                if (shmShipment.getHandlingUnitExemptionRsn() == null) {
                    shmShipment.setHandlingUnitExemptionRsn(StringUtils.SPACE);
                }

                DtoTransformer.setLstUpdateAuditInfo(shmShipment, auditInfo);

                shmShipmentSubDAO.persist(shmShipment, entityManager);
                shmShipmentSubDAO
                    .updateDB2ShmShipmentDimensionCaptureInfo(shmShipment.getShpInstId(),
                        Optional.of(shmShipment.getPupVolPct()), Optional.of(shmShipment.getTotVolCft()), Optional.of(shmShipment.getCftPrflMthdCd()),
                        Optional.of(shmShipment.getCftPrflTypeCd()),
                        Optional.of(shmShipment.getHandlingUnitExemptionRsn()), shmShipment.getLstUpdtTranCd(),
                        shmShipment.getLstUpdtTmst(), shmShipment.getLstUpdtUid(), db2EntityManager);

                shmEventDelegate
                    .createEvent(0L, EventLogTypeCd.CORRECTIONS, EventLogSubTypeCd.CUBE_UPDATE, shmShipment, null,
                    updateShipmentLinehaulDimensionsRqst.getRequestingSicCd(), Optional.empty(), "UPDM", entityManager, auditInfo);

            } else if (mustCreateCubeUpdt) {
                shmEventDelegate
                .createEvent(0L, EventLogTypeCd.CORRECTIONS, EventLogSubTypeCd.CUBE_UPDATE, shmShipment, null,
                updateShipmentLinehaulDimensionsRqst.getRequestingSicCd(), Optional.empty(), "UPDM", entityManager, auditInfo);

            }
        }

    }

    private void validateDimensionData(Long shipmentInstanceId, List<LnhDimension> lnhDimensions, TransactionContext txnContext) throws ValidationException {

        for (LnhDimension lnhDimension : CollectionUtils.emptyIfNull(lnhDimensions)) {

            if (lnhDimension.getShipmentInstId() == null
                    || !lnhDimension.getShipmentInstId().equals(shipmentInstanceId)) {
                throw ExceptionBuilder
                .exception(ValidationErrorMessage.SHIPMENT_INST_ID_RQ, txnContext)
                    .moreInfo("shipmentInstId",
                        lnhDimension.getShipmentInstId() != null ?
                            BasicTransformer.toString(lnhDimension.getShipmentInstId()) :
                            null)
                .build();
            }

            if (lnhDimension.getPiecesCount() == null || lnhDimension.getPiecesCount() < 1L) {
                throw ExceptionBuilder
                .exception(ValidationErrorMessage.PCS_CNT_MUST_BE_ENTD, txnContext)
                .moreInfo("shipmentInstId", BasicTransformer.toString(lnhDimension.getShipmentInstId()))
                .build();
            }

            if (lnhDimension.getLengthNbr() != null
                    && (lnhDimension.getLengthNbr() < 0 || lnhDimension.getLengthNbr() > 636)) {
                throw ExceptionBuilder
                .exception(ValidationErrorMessage.LENGTH_GREATER_THAN_636, txnContext)
                .moreInfo("shipmentInstId", BasicTransformer.toString(lnhDimension.getShipmentInstId()))
                .build();
            }
            if (lnhDimension.getWidthNbr() != null
                    && (lnhDimension.getWidthNbr() < 0 || lnhDimension.getWidthNbr() > 103)) {
                throw ExceptionBuilder
                .exception(ValidationErrorMessage.WIDTH_HEIGHT_GREATER_THAN_103, txnContext)
                .moreInfo("shipmentInstId", BasicTransformer.toString(lnhDimension.getShipmentInstId()))
                .build();
            }
            if (lnhDimension.getHeightNbr() != null
                    && (lnhDimension.getHeightNbr() < 0 || lnhDimension.getHeightNbr() > 103)) {
                throw ExceptionBuilder
                .exception(ValidationErrorMessage.WIDTH_HEIGHT_GREATER_THAN_103, txnContext)
                .moreInfo("shipmentInstId", BasicTransformer.toString(lnhDimension.getShipmentInstId()))
                .build();
            }
        }

    }

}
