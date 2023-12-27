package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmLnhDimensionPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.BulkUpsertHandlingUnitsRqst;
import com.xpo.ltl.api.shipment.v2.HandlingUnit;
import com.xpo.ltl.api.shipment.v2.HandlingUnitTypeCd;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmHandlingUnitDelegate;
import com.xpo.ltl.shipment.service.delegates.ShmLnhDimensionDelegate;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import com.xpo.ltl.shipment.service.util.TimestampUtil;

@RequestScoped
public class BulkUpsertHandlingUnitsImpl {

    @Inject
    private ShmShipmentSubDAO shipmentDAO;

    @Inject
    private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

    @PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
    private EntityManager db2EntityManager;

    @Inject
    private ShmHandlingUnitDelegate shmHandlingUnitDelegate;

    @Inject
    private ShmLnhDimensionDelegate shmLnhDimensionDelegate;

    private static final String HU_PGM_ID = "UPSERTHU";

    public void bulkUpsertHandlingUnits(BulkUpsertHandlingUnitsRqst bulkUpsertHandlingUnitsRqst, TransactionContext txnContext, EntityManager entityManager) throws ServiceException {

        checkNotNull(txnContext, "The TransactionContext is required.");
        checkNotNull(entityManager, "The EntityManager is required.");
        checkNotNull(bulkUpsertHandlingUnitsRqst, "The request is required.");

        validateRequest(bulkUpsertHandlingUnitsRqst, txnContext, entityManager);

        List<String> childProNumbers = bulkUpsertHandlingUnitsRqst.getHandlingUnitShipments().stream().map(HandlingUnit::getChildProNbr).collect(Collectors.toList());
        List<ShmHandlingUnit> shmHandlingUnits = shmHandlingUnitSubDAO.listByChildProNumbers(childProNumbers, entityManager);

        Map<String, ShmHandlingUnit> dbHuMap = shmHandlingUnitDelegate.buildChildProHUMap(shmHandlingUnits);
        Map<String, String> childParentMap = shmHandlingUnits
                .stream()
                .collect(Collectors.toMap(ShmHandlingUnit::getChildProNbrTxt, ShmHandlingUnit::getParentProNbrTxt));
        List<String> childProNumbersDB = Lists.newArrayList(dbHuMap.keySet());

        List<String> distinctParentPros = bulkUpsertHandlingUnitsRqst
                .getHandlingUnitShipments()
                .stream()
                .filter(hu -> StringUtils.isNotBlank(hu.getParentProNbr()))
                .map(hu -> {
                    try {
                        return ProNumberHelper.validateProNumber(hu.getParentProNbr(), txnContext);
                    } catch (ServiceException e) {
                        return hu.getParentProNbr();
                    }
                })
                .distinct()
                .collect(Collectors.toList());

        if (NumberUtils.compare(distinctParentPros.size(), 1) != 0) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo("N/A", "Can process sibling PROs only.")
                .log()
                .build();
        }

        ShmShipment shmShipment = shipmentDAO
            .findByIdOrProNumber(distinctParentPros.get(0), null,
                entityManager);
        if (Objects.isNull(shmShipment)) {
            throw ExceptionBuilder
                .exception(NotFoundErrorMessage.SHIPMENT_NF, txnContext)
                .moreInfo("BulkUpsertHandlingUnitsImpl", "Parent Pro: " + distinctParentPros.get(0))
                .build();
        }

        shmHandlingUnitDelegate.validateHandlingUnits(bulkUpsertHandlingUnitsRqst.getHandlingUnitShipments(), childParentMap, txnContext, entityManager);

        AuditInfo auditInfo = AuditInfoHelper
            .getAuditInfoWithPgmAndUserId(HU_PGM_ID, bulkUpsertHandlingUnitsRqst.getUserId(), txnContext);

        Timestamp lastMvmtTimestamp = Timestamp.from(Instant.now());
        XMLGregorianCalendar lastMvmtDateTime = TimestampUtil.toXmlGregorianCalendar(lastMvmtTimestamp);


        shmHandlingUnitDelegate
            .updateWeightForNewHandlingUnits(bulkUpsertHandlingUnitsRqst.getHandlingUnitShipments(), shmShipment,
                childProNumbersDB);

        shmLnhDimensionDelegate.deleteDimensions(shmShipment, entityManager);

        BigDecimal loosePcsCnt = BigDecimal.ZERO;
        BigDecimal motorMovePcsCnt = BigDecimal.ZERO;
        BigDecimal totVolCft = BigDecimal.ZERO;
        BigDecimal pupVolPct = BigDecimal.ZERO;
        //TODO consider passing profile type and method
        String cftPrflMthdCd = StringUtils.SPACE;
        String cftPrflTypeCd = StringUtils.SPACE;

        long nextHUSeqNbr = shmHandlingUnitSubDAO.getNextSeqNbrByShpInstId(shmShipment.getShpInstId(), entityManager);
        entityManager.flush();
        db2EntityManager.flush();

        for (HandlingUnit handlingUnit : bulkUpsertHandlingUnitsRqst.getHandlingUnitShipments()) {
            if (shmHandlingUnitDelegate.isNotAstrayHandlingUnitMovement(handlingUnit)) {
                totVolCft = totVolCft.add(BasicTransformer.toBigDecimal(handlingUnit.getVolumeCubicFeet()));
                pupVolPct = pupVolPct.add(BasicTransformer.toBigDecimal(handlingUnit.getPupVolumePercentage()));
                if (handlingUnit.getTypeCd() == HandlingUnitTypeCd.LOOSE) {
                    loosePcsCnt = loosePcsCnt.add(BigDecimal.ONE);
                }
                if (handlingUnit.getTypeCd() == HandlingUnitTypeCd.MOTOR) {
                    motorMovePcsCnt = motorMovePcsCnt.add(BigDecimal.ONE);
                }
                if (childProNumbersDB.contains(handlingUnit.getChildProNbr())) {

                    shmHandlingUnitDelegate
                        .updateShmHandlingUnit(dbHuMap.get(handlingUnit.getChildProNbr()), handlingUnit,
                            bulkUpsertHandlingUnitsRqst.getRequestingSicCd(), lastMvmtDateTime, auditInfo,
                            entityManager, txnContext);
                } else {
                    shmHandlingUnitDelegate
                        .createShmHandlingUnit(handlingUnit, shmShipment.getShpInstId(),
                            nextHUSeqNbr++,
                            shmShipment.getPkupDt(), bulkUpsertHandlingUnitsRqst.getRequestingSicCd(), lastMvmtDateTime,
                            auditInfo, entityManager);
                }
            } else {
                shmHandlingUnitDelegate
                    .createOrUpdateAstrayHandlingUnit(bulkUpsertHandlingUnitsRqst, handlingUnit, shmShipment,
                        nextHUSeqNbr++, lastMvmtDateTime,
                        dbHuMap,
                    auditInfo, entityManager, txnContext);
            }
        }

        List<ShmHandlingUnit> huList = shmHandlingUnitSubDAO.findByParentShipmentInstanceId(shmShipment.getShpInstId(), entityManager);
        long nextDimensionSeqNbr = 1;

        for (ShmHandlingUnit shmHandlingUnit : huList) {

            ShmLnhDimensionPK pk = new ShmLnhDimensionPK();
            pk.setShpInstId(shmHandlingUnit.getId().getShpInstId());
            pk.setDimSeqNbr(nextDimensionSeqNbr++);
            shmLnhDimensionDelegate.createShmLnhDimension(pk, shmHandlingUnit.getHeightNbr(), shmHandlingUnit.getWidthNbr(), shmHandlingUnit.getLengthNbr(), auditInfo.getCreatedById(),
                shmHandlingUnit.getDimensionTypeCd(), auditInfo, entityManager, txnContext);
        }

        shmShipment
            .setHandlingUnitPartialInd(
                shmHandlingUnitDelegate
                    .calculateHUPartialInd(bulkUpsertHandlingUnitsRqst.getHandlingUnitShipments(),
                        shmHandlingUnitDelegate.buildChildProHUMap(shmShipment.getShmHandlingUnits())));
        
        shmShipment.setLoosePcsCnt(loosePcsCnt);
        shmShipment.setMtrzdPcsCnt(motorMovePcsCnt);
        shmShipment.setTotVolCft(totVolCft);
        shmShipment.setPupVolPct(pupVolPct);
//        shmShipment.setCftPrflMthdCd(cftPrflMthdCd);
//        shmShipment.setCftPrflTypeCd(cftPrflTypeCd);
        shmShipment.setHandlingUnitExemptionInd(BasicTransformer.toString(false));
        shmShipment.setHandlingUnitExemptionRsn(StringUtils.SPACE);
        DtoTransformer.setLstUpdateAuditInfo(shmShipment, auditInfo);
        shipmentDAO.save(shmShipment, entityManager);
        shipmentDAO.updateDb2ShmShipmentForBulkUpsertHUs(shmShipment, db2EntityManager);
    }

    private void validateRequest(BulkUpsertHandlingUnitsRqst bulkUpsertHandlingUnitsRqst,
        TransactionContext txnContext, EntityManager entityManager)
            throws ValidationException, NotFoundException, ServiceException {

        for (HandlingUnit handlingUnit : CollectionUtils.emptyIfNull(bulkUpsertHandlingUnitsRqst.getHandlingUnitShipments())) {
            if (handlingUnit != null) {
                if (StringUtils.isNotBlank(handlingUnit.getParentProNbr())) {
                    handlingUnit.setParentProNbr(ProNumberHelper.validateProNumber(handlingUnit.getParentProNbr(), txnContext));
                }
                handlingUnit.setChildProNbr(ProNumberHelper.validateProNumber(handlingUnit.getChildProNbr(), txnContext));
            }
        }

        if(StringUtils.isBlank(bulkUpsertHandlingUnitsRqst.getRequestingSicCd())) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.RPTG_SIC_RQ, txnContext).build();
        }

        if(StringUtils.isBlank(bulkUpsertHandlingUnitsRqst.getUserId())) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo("UserId", "User id is required.")
                .build();
        }

        if (CollectionUtils.isEmpty(bulkUpsertHandlingUnitsRqst.getHandlingUnitShipments())) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo("N/A", "At least one HandlingUnit is required.")
                .log()
                .build();
        }
    }

}
