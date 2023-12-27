package com.xpo.ltl.shipment.service.delegates;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmt;
import com.xpo.ltl.api.shipment.service.entity.ShmLnhDimensionPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.HandlingUnitTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.BulkUpsertHandlingUnitsRqst;
import com.xpo.ltl.api.shipment.v2.HandlingUnit;
import com.xpo.ltl.api.shipment.v2.HandlingUnitTypeCd;
import com.xpo.ltl.api.shipment.v2.MovementStatusCd;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitMvmtSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import com.xpo.ltl.shipment.service.validators.UpdateHandlingUnitDimensionsValidator;

@ApplicationScoped
public class ShmHandlingUnitDelegate {

    public static final String DEFAULT_PARENT_PRO_FOR_ASTRAY = "09990999991";
    private static final String DOCK = "DOCK";
    private static final Log LOGGER = LogFactory.getLog(ShmHandlingUnitDelegate.class);
    private static final String NORMAL_MOVEMENT_CD = "NORMAL";
    private static final String MISSING_MOVEMENT_CD = "MISSING";
    private static final String ASTRAY_MOVEMENT_CD = "ASTRAY";
    private static final List<String> HU_MVMT_CDS = Arrays
        .asList(NORMAL_MOVEMENT_CD, MISSING_MOVEMENT_CD, ASTRAY_MOVEMENT_CD);
    private static final List<String> DIMENSION_TYPE_CDS = Arrays.asList("ACCURACY", "DOCK", "PICKUP", "PICKUP_DIMENSIONER");
    private static final String HU_MVMT_CD_MISSING = "MISSING";

    @PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
    private EntityManager db2EntityManager;

    @Inject
    private UpdateHandlingUnitDimensionsValidator validator;

    @Inject
    private ShmShipmentSubDAO shipmentDAO;

    @Inject
    private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

    @Inject
    private ShmHandlingUnitMvmtSubDAO shmHandlingUnitMvmtSubDAO;

    @Inject
    private ShmLnhDimensionDelegate shmLnhDimensionDelegate;

    public boolean isNotAstrayHandlingUnitMovement(HandlingUnit reqHU) {
        return reqHU.getHandlingMovementCd() != null && !ASTRAY_MOVEMENT_CD.equals(reqHU.getHandlingMovementCd());
    }

    public boolean isNotAstrayShmHandlingUnitMovement(ShmHandlingUnit shmHU) {
        return shmHU.getHandlingMvmtCd() != null && !ASTRAY_MOVEMENT_CD.equals(shmHU.getHandlingMvmtCd());
    }

    /**
     * only for new HUs that their movCd is not ASTRAY.
     */
    public void updateWeightForNewHandlingUnits(List<HandlingUnit> handlingUnits, ShmShipment shmShipment, List<String> childProNumbersDB) {

        List<HandlingUnit> newHUs = handlingUnits
            .stream()
            .filter(
                reqHU -> !childProNumbersDB.contains(reqHU.getChildProNbr()) && isNotAstrayHandlingUnitMovement(reqHU))
            .collect(Collectors.toList());

        // if there are no new HU, we don't have to re weight anything.
        if (CollectionUtils.isEmpty(newHUs)) {
            LOGGER.info("No HandlingUnits to re weight.");
            return;
        }

        BigDecimal totWgtLbs = shmShipment.getTotWgtLbs();
        List<ShmHandlingUnit> dbShmHandlingUnitsForShipment = shmShipment.getShmHandlingUnits();

        List<ShmHandlingUnit> huListNonReweighed = dbShmHandlingUnitsForShipment
            .stream()
            .filter(hu -> !BasicTransformer.toBoolean(hu.getReweighInd())
                    && !ASTRAY_MOVEMENT_CD.equals(hu.getHandlingMvmtCd()))
            .collect(Collectors.toList());

        BigDecimal totalHUWeightReWeighted = dbShmHandlingUnitsForShipment
            .stream()
            .filter(hu -> BasicTransformer.toBoolean(hu.getReweighInd()))
            .map(hu -> hu.getWgtLbs())
            .reduce((x, y) -> x.add(y))
            .orElse(BigDecimal.ZERO);

        BigDecimal weightToReDistributeEvenly = totWgtLbs.subtract(totalHUWeightReWeighted);
        if (weightToReDistributeEvenly.signum() < 0) {
            weightToReDistributeEvenly = BigDecimal.ZERO;
        }

        BigDecimal qtyHUToBeReweighted = new BigDecimal(huListNonReweighed.size() + newHUs.size());

        BigDecimal weightForEachHU = weightToReDistributeEvenly.divide(qtyHUToBeReweighted, 2, RoundingMode.HALF_DOWN);

        if (CollectionUtils.isNotEmpty(huListNonReweighed)) {
            huListNonReweighed.stream().forEach(hu -> {
                hu.setWgtLbs(BigDecimal.ZERO.compareTo(weightForEachHU) == 0 ? BigDecimal.ONE : weightForEachHU);
            });
        }

        newHUs.stream().forEach(hu -> {
            hu.setWeightLbs(
                BasicTransformer.toDouble(
                    BigDecimal.ZERO.compareTo(weightForEachHU) == 0 ? BigDecimal.ONE : weightForEachHU));
        });

    }

    public void validateHandlingUnits(List<HandlingUnit> handlingUnitShipments, Map<String, String> childParentMap, TransactionContext txnContext, EntityManager entityManager)
            throws ValidationException, ServiceException, NotFoundException {

        if (CollectionUtils.isEmpty(handlingUnitShipments)) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.HU_OR_DIM_REQUIRED, txnContext).build();
        }

        List<String> childProNumbersDB = childParentMap != null ? Lists.newArrayList(childParentMap.keySet()) : null;

        for (HandlingUnit handlingUnit : handlingUnitShipments) {

            String childProNumber = ProNumberHelper.validateProNumber(handlingUnit.getChildProNbr(), txnContext);

            if (!ProNumberHelper.isYellowPro(childProNumber)) {
                throw ExceptionBuilder
                    .exception(ValidationErrorMessage.PRO_NBR_FORMAT_INVALID, txnContext)
                    .moreInfo("childProNbr", ValidationErrorMessage.YELLOW_PRO_FORMAT.message() + childProNumber)
                    .build();
            }
            handlingUnit.setChildProNbr(childProNumber);

            if (childParentMap != null) {
                String parentProDB = childParentMap.get(handlingUnit.getChildProNbr());
                String formattedParentPro = ProNumberHelper.toElevenDigitPro(handlingUnit.getParentProNbr(), txnContext);
                if (StringUtils.isNotBlank(parentProDB) && (!parentProDB.equals(formattedParentPro)
                        && !DEFAULT_PARENT_PRO_FOR_ASTRAY.equals(parentProDB))) {
                    throw ExceptionBuilder
                        .exception(ValidationErrorMessage.CHILD_PRO_ALREADY_USED_HH,
                        txnContext)
                        .moreInfo("BulkUpsertHandlingUnitsImpl", "handlingUnit.parentProNbr: " + handlingUnit.getParentProNbr())
                        .build();
                }
                handlingUnit.setParentProNbr(formattedParentPro);

                // TODO if update, should we enforce update of dimension
                if (isNotAstrayHandlingUnitMovement(handlingUnit) && !childProNumbersDB.contains(childProNumber)) {
                    if (ObjectUtils.anyNull(handlingUnit.getLengthNbr(), handlingUnit.getWidthNbr(), handlingUnit.getHeightNbr())) {
                        throw ExceptionBuilder.exception(ValidationErrorMessage.HU_DIMENSIONS_REQUIRED, txnContext).moreInfo("childProNbr", childProNumber).build();
                    }
                    validator.validateDimension(handlingUnit.getLengthNbr(), handlingUnit.getWidthNbr(), handlingUnit.getHeightNbr(), txnContext);
                }

            }

            // validate cube required only if we get any dimension.
            if (isNotAstrayHandlingUnitMovement(handlingUnit) && ObjectUtils.anyNotNull(handlingUnit.getLengthNbr(), handlingUnit.getWidthNbr(), handlingUnit.getHeightNbr())) {
                if (handlingUnit.getVolumeCubicFeet() == null || handlingUnit.getVolumeCubicFeet().compareTo(0D) < 0) {
                    throw ExceptionBuilder.exception(ValidationErrorMessage.TOT_VOL_CFT_IS_REQUIRED, txnContext).moreInfo("childProNbr", childProNumber).build();
                }

                if (handlingUnit.getPupVolumePercentage() == null || handlingUnit.getPupVolumePercentage().compareTo(0D) <= 0) {
                    throw ExceptionBuilder.exception(ValidationErrorMessage.PUP_VOL_PCT_NOT_CALCULATED, txnContext).moreInfo("childProNbr", childProNumber).build();
                }
            }

            if (handlingUnit.getTypeCd() == null) {
                throw ExceptionBuilder.exception(ValidationErrorMessage.CHILD_PRO_TYPE_CODE_IS_INVALID, txnContext).moreInfo("childProNbr", childProNumber).build();
            }

            if (handlingUnit.getHandlingMovementCd() != null && !HU_MVMT_CDS.contains(handlingUnit.getHandlingMovementCd())) {
                throw ExceptionBuilder
                    .exception(ValidationErrorMessage.CHILD_PRO_HM_INVALID, txnContext)
                    .moreInfo("childProNbr", childProNumber)
                    .moreInfo("handlingMovementCd must be valid when supplied", handlingUnit.getHandlingMovementCd())
                    .build();
            }

            if (handlingUnit.getDimensionTypeCd() != null && !DIMENSION_TYPE_CDS.contains(handlingUnit.getDimensionTypeCd())) {
                throw ExceptionBuilder.exception(ValidationErrorMessage.CHILD_PRO_DIMENSION_INVALID, txnContext).moreInfo("childProNbr must be valid when supplied", childProNumber).build();
            }
        }
    }

    public ShmHandlingUnit createShmHandlingUnitAndShmLnhDimension(HandlingUnit handlingUnit, Long shpInstId,
        Long shmSeqNbr, Date pkupDt, String requestingSicCd, XMLGregorianCalendar lastMvmtDateTime,
        AuditInfo auditInfo, EntityManager entityManager, TransactionContext txnContext, long nextDimensionSeqNbr) throws ServiceException {

        ShmHandlingUnit shmHandlingUnit = createShmHandlingUnit(handlingUnit, shpInstId, shmSeqNbr, pkupDt,
            requestingSicCd,
            lastMvmtDateTime, auditInfo, entityManager);

        ShmLnhDimensionPK pk = new ShmLnhDimensionPK();
        pk.setShpInstId(shmHandlingUnit.getId().getShpInstId());
        pk.setDimSeqNbr(nextDimensionSeqNbr);
        shmLnhDimensionDelegate.createShmLnhDimension(pk, shmHandlingUnit.getHeightNbr(), shmHandlingUnit.getWidthNbr(), shmHandlingUnit.getLengthNbr(), auditInfo.getCreatedById(),
            handlingUnit.getDimensionTypeCd(), auditInfo, entityManager, txnContext);

        return shmHandlingUnit;
    }

    public ShmHandlingUnit createShmHandlingUnit(HandlingUnit handlingUnit, Long shpInstId, Long shmSeqNbr, Date pkupDt,
        String requestingSicCd, XMLGregorianCalendar lastMvmtDateTime, AuditInfo auditInfo, EntityManager entityManager)
            throws ServiceException, ValidationException {

        handlingUnit.setShipmentInstanceId(shpInstId);
        handlingUnit.setSequenceNbr(BigInteger.valueOf(shmSeqNbr));

        setHandlingUnitDefaultValues(handlingUnit, shpInstId, pkupDt, requestingSicCd, lastMvmtDateTime, auditInfo,
            entityManager);

        ShmHandlingUnit shmHandlingUnit = DtoTransformer.toShmHandlingUnit(handlingUnit, null);
        DtoTransformer.setAuditInfo(shmHandlingUnit, auditInfo);
        shmHandlingUnitSubDAO.save(shmHandlingUnit, entityManager);
        shmHandlingUnitSubDAO.createDB2ShmHandlingUnit(shmHandlingUnit, db2EntityManager);

        return shmHandlingUnit;
    }

    public void updateShmHandlingUnit(ShmHandlingUnit huDB, HandlingUnit hu, String dockName, Long trailerInstId, String requestingSicCd,
        Boolean isSplitHUMvmt, String mvmtStatCd, boolean allShort, AuditInfo auditInfo,
        EntityManager entityManager,
        TransactionContext txnContext) throws ServiceException {
        huDB.setHandlingMvmtCd(hu.getHandlingMovementCd());
        huDB.setCurrentSicCd(hu.getHandlingMovementCd().equals(HU_MVMT_CD_MISSING) ? StringUtils.SPACE : requestingSicCd);
        BigDecimal trlrInst = (trailerInstId != null ? BasicTransformer.toBigDecimal(trailerInstId) : BigDecimal.ZERO);
        huDB.setCurrentTrlrInstId(hu.getHandlingMovementCd().equals(HU_MVMT_CD_MISSING) ? BigDecimal.ZERO : trlrInst);
        String dockLoc = (StringUtils.isNotBlank(dockName) ? dockName : StringUtils.SPACE);
        huDB.setCurrentDockLocTxt(hu.getHandlingMovementCd().equals(HU_MVMT_CD_MISSING) ? StringUtils.SPACE : dockLoc);
        if (hu.getHandlingMovementCd().equals(HU_MVMT_CD_MISSING)) {
            huDB.setSplitInd(BasicTransformer.toString(false));
        } else {
            huDB.setSplitInd(isSplitHUMvmt != null ? BasicTransformer.toString(isSplitHUMvmt) : huDB.getSplitInd());
        }
        huDB.setMvmtStatCd(mvmtStatCd);
        DtoTransformer.setLstUpdateAuditInfo(huDB, auditInfo);

        shmHandlingUnitSubDAO.save(huDB, entityManager);
        shmHandlingUnitSubDAO.updateDB2ShmHandlingUnit(huDB, huDB.getLstUpdtTmst(), txnContext, db2EntityManager);
    }

    public void updateShmHandlingUnit(ShmHandlingUnit shmHandlingUnit, HandlingUnit hu, String requestingSicCd, XMLGregorianCalendar lastMvmtDateTime, AuditInfo auditInfo,
        EntityManager entityManager,
        TransactionContext txnContext) throws ServiceException {

        shmHandlingUnit.setHandlingMvmtCd(Optional.ofNullable(hu.getHandlingMovementCd()).orElse(NORMAL_MOVEMENT_CD));
        shmHandlingUnit.setDimensionTypeCd(Optional.ofNullable(hu.getDimensionTypeCd()).orElse(DOCK));
        shmHandlingUnit.setTypeCd(Optional.ofNullable(HandlingUnitTypeCdTransformer.toCode(hu.getTypeCd())).orElse(HandlingUnitTypeCdTransformer.toCode(HandlingUnitTypeCd.UNKNOWN)));
        shmHandlingUnit.setMvmtStatCd(Optional.ofNullable(hu.getMovementStatusCd()).orElse(MovementStatusCdTransformer.toCode(MovementStatusCd.ON_DOCK)));
        shmHandlingUnit.setLstMvmtTmst(BasicTransformer.toTimestamp(lastMvmtDateTime));
        shmHandlingUnit.setCurrentSicCd(requestingSicCd);
        shmHandlingUnit.setLengthNbr(BasicTransformer.toBigDecimal(hu.getLengthNbr()));
        shmHandlingUnit.setWidthNbr(BasicTransformer.toBigDecimal(hu.getWidthNbr()));
        shmHandlingUnit.setHeightNbr(BasicTransformer.toBigDecimal(hu.getHeightNbr()));
        shmHandlingUnit.setVolCft(BasicTransformer.toBigDecimal(hu.getVolumeCubicFeet()));
        shmHandlingUnit.setPupVolPct(BasicTransformer.toBigDecimal(hu.getPupVolumePercentage()));
        DtoTransformer.setLstUpdateAuditInfo(shmHandlingUnit, auditInfo);

        shmHandlingUnitSubDAO.save(shmHandlingUnit, entityManager);
        shmHandlingUnitSubDAO.updateDB2ShmHandlingUnit(shmHandlingUnit, shmHandlingUnit.getLstUpdtTmst(), txnContext, db2EntityManager);

    }

    public ShmHandlingUnit updateShmHandlingUnitDimensions(ShmHandlingUnit shmHandlingUnit, String userId, String requestingSic, double height, double width, double length, double volumeCubFeet,
        double pupVolPct, String dimensionTypeCd, AuditInfo auditInfo, TransactionContext txnContext, EntityManager entityManager)
            throws ServiceException {

        if (StringUtils.isNotBlank(userId)) {
            auditInfo.setUpdateById(userId);
        }
        if (StringUtils.isNotBlank(requestingSic)) {
            shmHandlingUnit.setCurrentSicCd(requestingSic);
        }
        if (StringUtils.isNotBlank(dimensionTypeCd)) {
            shmHandlingUnit.setDimensionTypeCd(dimensionTypeCd);
        }

        shmHandlingUnit.setHeightNbr(BasicTransformer.toBigDecimal(height));
        shmHandlingUnit.setWidthNbr(BasicTransformer.toBigDecimal(width));
        shmHandlingUnit.setLengthNbr(BasicTransformer.toBigDecimal(length));
        shmHandlingUnit.setVolCft(BasicTransformer.toBigDecimal(volumeCubFeet));
        shmHandlingUnit.setPupVolPct(BasicTransformer.toBigDecimal(pupVolPct));

        DtoTransformer.setLstUpdateAuditInfo(shmHandlingUnit, auditInfo);

        shmHandlingUnit = shmHandlingUnitSubDAO.save(shmHandlingUnit, entityManager);
        shmHandlingUnitSubDAO.updateDB2ShmHandlingUnit(shmHandlingUnit, shmHandlingUnit.getLstUpdtTmst(), txnContext, db2EntityManager);

        return shmHandlingUnit;
    }

    /**
     * <p>
     * When a child PRO goes from mvmtCd of NORMAL to MISSING: SHM_SHIPMENT record for the parent PRO should have
     * HANDLING_UNIT_PARTIAL_IND set to Y
     * </p>
     * <p>
     * When a child PRO goes from mvmtCd of MISSING to NORMAL:
     * Check if all siblings have mvmtCd NOT EQUAL to MISSING, if yes, SHM_SHIPMENT record for the parent PRO should have
     * HANDLING_UNIT_PARTIAL_IND set to N
     * </p>
     * <p>
     * When a child PRO that is an ASTRAY is added to a shipment:
     * Check if the # of Astray (including siblings) is greater than or equal to the number of siblings that is MISSING, if
     * yes, SHM_SHIPMENT record for the parent PRO should have HANDLING_UNIT_PARTIAL_IND set to N (if not yet set to N)
     * </p>
     */
    /**
     * @param handlingUnitShipments
     *            from the request.
     * @param dbHuMap
     *            a map <ChildPro,ShmHandlingUnit> taken from the database.
     * @return 'Y' or 'N'.
     */
    public String calculateHUPartialInd(List<HandlingUnit> handlingUnitShipments, Map<String, ShmHandlingUnit> dbHuMap) {
        boolean result = false;
        for (HandlingUnit handlingUnit : handlingUnitShipments) {
            ShmHandlingUnit shmHandlingUnit = dbHuMap.get(handlingUnit.getChildProNbr());
            // update
            if (this.isNotAstrayHandlingUnitMovement(handlingUnit) && dbHuMap.keySet().contains(handlingUnit.getChildProNbr())) {
                if (shmHandlingUnit == null) { // it's new, so there is no current mvmt.
                    continue;
                }

                String currentMvmtCd = shmHandlingUnit.getHandlingMvmtCd();
                String newMvmtCd = handlingUnit.getHandlingMovementCd();

                if (NORMAL_MOVEMENT_CD.equals(currentMvmtCd) && MISSING_MOVEMENT_CD.equals(newMvmtCd)) {
                    result = true;
                    break;
                }
                if (MISSING_MOVEMENT_CD.equals(currentMvmtCd) && NORMAL_MOVEMENT_CD.equals(newMvmtCd)) {
                    boolean allSiblingsOtherThanMissingMvmtCd = org.apache.commons.collections4.CollectionUtils
                        .emptyIfNull(dbHuMap.values())
                        .stream()
                        .filter(shmHU -> shmHU.getChildProNbrTxt().equals(handlingUnit.getChildProNbr()))
                        .allMatch(hu -> !MISSING_MOVEMENT_CD.equals(hu.getHandlingMvmtCd()));
                    if (allSiblingsOtherThanMissingMvmtCd) {
                        result = false;
                    }
                }
            } else { // is astray.
                Collection<ShmHandlingUnit> dbHUs = org.apache.commons.collections4.CollectionUtils.emptyIfNull(dbHuMap.values());
                long astraysQty = dbHUs.stream().filter(shmHU -> !isNotAstrayShmHandlingUnitMovement(shmHU)).count();
                long missingQty = dbHUs.stream().filter(shmHU -> MISSING_MOVEMENT_CD.equals(shmHU.getHandlingMvmtCd())).count();

                if (astraysQty >= missingQty) {
                    result = false;
                }
            }
        }

        return BasicTransformer.toString(result);
    }

    /**
     * <p>
     * Check if any of the handling units from the request and from the database is split.
     * </p>
     *
     * @param handlingUnitShipments
     *            from the request.
     * @param allShmHandlingUnitsDB
     *            from the database.
     * @return boolean.
     */
    public boolean isShipmentSplit(List<HandlingUnit> handlingUnitShipments, List<ShmHandlingUnit> allShmHandlingUnitsDB) {

        List<String> childProsFromRqst = org.apache.commons.collections4.CollectionUtils
                .emptyIfNull(handlingUnitShipments)
                .stream()
                .map(hu -> hu.getChildProNbr())
                .collect(Collectors.toList());

        boolean anySplitSibling = org.apache.commons.collections4.CollectionUtils
                .emptyIfNull(allShmHandlingUnitsDB)
                .stream()
                .filter(hu -> !childProsFromRqst.contains(hu.getChildProNbrTxt()))
            .anyMatch(hu -> BasicTransformer.toBoolean(hu.getSplitInd()));

        boolean anySplitFromRqst = org.apache.commons.collections4.CollectionUtils
                .emptyIfNull(handlingUnitShipments)
                .stream()
                .anyMatch(hu -> BooleanUtils.isTrue(hu.getSplitInd()));

        return anySplitSibling || anySplitFromRqst;
    }

    public Map<String, ShmHandlingUnit> buildChildProHUMap(List<ShmHandlingUnit> shmHandlingUnitsDB) {
        if (CollectionUtils.isEmpty(shmHandlingUnitsDB)) {
            return new HashMap<String, ShmHandlingUnit>();
        }

        Map<String, ShmHandlingUnit> dbHuMap = shmHandlingUnitsDB.stream().collect(Collectors.toMap(ShmHandlingUnit::getChildProNbrTxt, hu -> hu));
        return dbHuMap;
    }

    /**
     * <ul>
     * <li>request: par1 chld1</li>
     * <li>After read ShmHandlingUnit of chld1</li>
     * <ol>
     * <li>scenario1: return a not found, create new record chld1 for par1</li>
     * <li>scenario2:
     * return a rec showing current parent pro is default pro (999-999991) <br/>
     * deleted that existing record<br/>
     * create new record chld1 for par1
     * </li>
     * <li>scenario3:
     * return a rec showing current parent pro is par1<br/>
     * update the existing chld1 record
     * </li>
     * <li>scenario4:
     * return a rec showing current parent pro is par2<br/>
     * return error that it's already associated to par2
     * </li>
     * </ol>
     * </ul>
     */
    public void createOrUpdateAstrayHandlingUnit(BulkUpsertHandlingUnitsRqst bulkUpsertHandlingUnitsRqst,
        HandlingUnit handlingUnit, ShmShipment shmShipmentForAstray, Long shmSeqNbr, XMLGregorianCalendar lastMvmtDateTime,
        Map<String, ShmHandlingUnit> dbHuMap,
        AuditInfo auditInfo, EntityManager entityManager, TransactionContext txnContext)
            throws ServiceException, ValidationException, NotFoundException {

        ShmHandlingUnit shmHandlingUnit = dbHuMap.get(handlingUnit.getChildProNbr());

        if (shmHandlingUnit == null) { // handling unit not found in DB
            // create a new HU.
            createShmHandlingUnit(handlingUnit, shmShipmentForAstray.getShpInstId(), shmSeqNbr,
                shmShipmentForAstray.getPkupDt(),
                bulkUpsertHandlingUnitsRqst.getRequestingSicCd(), lastMvmtDateTime, auditInfo, entityManager);
        } else if (ShmHandlingUnitDelegate.DEFAULT_PARENT_PRO_FOR_ASTRAY.equals(shmHandlingUnit.getParentProNbrTxt())) {

            if (CollectionUtils.isNotEmpty(shmHandlingUnit.getShmHandlingUnitMvmts())) {
                // all movements have to be removed.
                shmHandlingUnitMvmtSubDAO.deleteShmHandlingUnitMvmtByShmHUPK(shmHandlingUnit.getId(), entityManager);
                shmHandlingUnitMvmtSubDAO
                    .deleteDB2ShmHandlingUnitMvmtByShmHUPK(shmHandlingUnit.getId(), db2EntityManager);

                // remove the association btw hu and mvmts.
                Iterator<ShmHandlingUnitMvmt> iterator = shmHandlingUnit.getShmHandlingUnitMvmts().iterator();
                while (iterator.hasNext()) {
                    ShmHandlingUnitMvmt mvmt = iterator.next();
                    iterator.remove();
                    mvmt.setShmHandlingUnit(null);
                }
            }

            // delete existing record.
            shmHandlingUnitSubDAO.remove(shmHandlingUnit, entityManager);
            shmHandlingUnitSubDAO
                .deleteDB2(shmHandlingUnit.getId(), shmHandlingUnit.getLstUpdtTmst(), db2EntityManager, txnContext);
            // after delete in db2, a flush is needed because of an index issue.
            entityManager.flush();
            db2EntityManager.flush();

            createShmHandlingUnit(handlingUnit, shmShipmentForAstray.getShpInstId(), shmSeqNbr,
                shmShipmentForAstray.getPkupDt(),
                bulkUpsertHandlingUnitsRqst.getRequestingSicCd(), lastMvmtDateTime, auditInfo, entityManager);

        } else if (handlingUnit.getParentProNbr().equals(shmHandlingUnit.getParentProNbrTxt())) {
            // update existing chldpro.
            updateShmHandlingUnit(shmHandlingUnit, handlingUnit, bulkUpsertHandlingUnitsRqst.getRequestingSicCd(),
                lastMvmtDateTime, auditInfo, entityManager, txnContext);
        } else {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.PRO_NBR_CANT_BE_UPDATED, txnContext)
                .moreInfo("BulkUpsertHandlingUnitsImpl",
                    "Handling Unit: " + handlingUnit.getChildProNbr() + " is already associated to: "
                            + shmHandlingUnit.getParentProNbrTxt())
                .build();
        }
    }

    public void setHandlingUnitDefaultValues(HandlingUnit handlingUnit, long shpInstId, Date pkupDt, String requestingSicCd, XMLGregorianCalendar lastMvmtDateTime, AuditInfo auditInfo,
        EntityManager entityManager) {

        if (Objects.isNull(handlingUnit.getAuditInfo())) {
            handlingUnit.setAuditInfo(auditInfo);
        }
        if (Objects.isNull(handlingUnit.getCurrentSicCd())) {
            handlingUnit.setCurrentSicCd(requestingSicCd);
        }

        if (Objects.isNull(handlingUnit.getCurrentDockLocation())) {
            handlingUnit.setCurrentDockLocation(StringUtils.SPACE);
        }
        if (Objects.isNull(handlingUnit.getDimensionTypeCd())) {
            handlingUnit.setDimensionTypeCd(DOCK);
        }
        if (Objects.isNull(handlingUnit.getHandlingMovementCd())) {
            handlingUnit.setHandlingMovementCd(NORMAL_MOVEMENT_CD);
        }
        if (Objects.isNull(handlingUnit.getTypeCd())) {
            handlingUnit.setTypeCd(HandlingUnitTypeCd.UNKNOWN);
        }
        if (Objects.isNull(handlingUnit.getMovementStatusCd())) {
            handlingUnit.setMovementStatusCd(MovementStatusCdTransformer.toCode(MovementStatusCd.ON_DOCK));
        }
        if (Objects.isNull(handlingUnit.getLastMovementDateTime())) {
            handlingUnit.setLastMovementDateTime(lastMvmtDateTime);
        }
        if (Objects.isNull(handlingUnit.getArchiveInd())) {
            handlingUnit.setArchiveInd(false);
        }
        if (Objects.isNull(handlingUnit.getStackableInd())) {
            handlingUnit.setStackableInd(false);
        }
        if (handlingUnit.getReweighInd() == null) {
            handlingUnit.setReweighInd(false);
        }
        if (Objects.isNull(handlingUnit.getPickupDate())) {
            handlingUnit.setPickupDate(BasicTransformer.toDateString(pkupDt));
        }
        if (Objects.isNull(handlingUnit.getWeightLbs())) {
            handlingUnit.setWeightLbs(new Double(1));
        }
        if (Objects.isNull(handlingUnit.getCurrentTrailerInstanceId())) {
            handlingUnit.setCurrentTrailerInstanceId(0L);
        }
        if (Objects.isNull(handlingUnit.getVolumeCubicFeet())) {
            handlingUnit.setVolumeCubicFeet(0D);
        }
        if (Objects.isNull(handlingUnit.getLengthNbr())) {
            handlingUnit.setLengthNbr(new Double(0));
        }
        if (Objects.isNull(handlingUnit.getWidthNbr())) {
            handlingUnit.setWidthNbr(new Double(0));
        }
        if (Objects.isNull(handlingUnit.getHeightNbr())) {
            handlingUnit.setHeightNbr(new Double(0));
        }

        if (Objects.isNull(handlingUnit.getPoorlyPackagedInd())) {
            handlingUnit.setPoorlyPackagedInd(Boolean.FALSE);
        }

        if (Objects.isNull(handlingUnit.getSplitInd())) {
            handlingUnit.setSplitInd(false);
        }
    }

    public void updateHandlingUnitsWeight(BigDecimal totalWeightLbs, BigDecimal inputWeightLbs,
        List<ShmHandlingUnit> allShmHandlingUnitsDB, AuditInfo auditInfo, EntityManager entityManager, TransactionContext txnContext) throws ServiceException {

        List<ShmHandlingUnit> allRemaningShmHUsNonReweightedDB = allShmHandlingUnitsDB.stream().filter(hu -> !BasicTransformer.toBoolean(hu.getReweighInd())).collect(Collectors.toList());

        if (!allRemaningShmHUsNonReweightedDB.isEmpty()) {
            BigDecimal wgtToRedistribute = totalWeightLbs.subtract(inputWeightLbs);
            BigDecimal qtyHUToBeReweighted = new BigDecimal(allRemaningShmHUsNonReweightedDB.size());
            BigDecimal weightToAddForEachHU = wgtToRedistribute.divide(qtyHUToBeReweighted, 2, RoundingMode.HALF_DOWN);
            for (ShmHandlingUnit remaningHUNonRwghtd : allRemaningShmHUsNonReweightedDB) {
                DtoTransformer.setLstUpdateAuditInfo(remaningHUNonRwghtd, auditInfo);
                remaningHUNonRwghtd.setWgtLbs(BigDecimal.ZERO.compareTo(weightToAddForEachHU) == 0 ? BigDecimal.ONE : weightToAddForEachHU);
                remaningHUNonRwghtd.setPupVolPct(ObjectUtils.defaultIfNull(remaningHUNonRwghtd.getPupVolPct(), BigDecimal.ZERO));
                shmHandlingUnitSubDAO.save(remaningHUNonRwghtd, entityManager);
                shmHandlingUnitSubDAO.updateDB2ShmHandlingUnit(remaningHUNonRwghtd, remaningHUNonRwghtd.getLstUpdtTmst(), txnContext, db2EntityManager);
            }
        }


    }
}
