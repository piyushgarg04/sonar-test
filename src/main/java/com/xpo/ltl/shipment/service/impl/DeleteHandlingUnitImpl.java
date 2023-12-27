package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.ObjectUtils;

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
import com.xpo.ltl.api.shipment.transformer.v2.BillStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.BillStatusCd;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.java.util.cityoperations.ProNumber;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmLnhDimensionDelegate;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;

@RequestScoped
public class DeleteHandlingUnitImpl {

    private static final String HU_TYPE_CD_LOOSE = "LOOSE";

    private static final String HU_TYPE_CD_MOTOR = "MOTOR";

    private static final String ASTRAY_MOVEMENT_CD = "ASTRAY";

    private static final String DEL_HU_PGM_ID = "DELETHU";

    @Inject
    private ShmShipmentSubDAO shmShipmentSubDAO;

    @Inject
    private ShmLnhDimensionDelegate shmLnhDimensionDelegate;

    @Inject
    private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

    @PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
    private EntityManager db2EntityManager;

    public void deleteHandlingUnit(String childPro, TransactionContext txnContext, EntityManager entityManager) throws ServiceException {

        checkNotNull(childPro, "ChildPro is required.");
        checkNotNull(txnContext, "The TransactionContext is required.");
        checkNotNull(entityManager, "The EntityManager is required.");

        AuditInfo auditInfo = AuditInfoHelper.getAuditInfoWithPgmId(DEL_HU_PGM_ID, txnContext);
        ShmHandlingUnit shmHandlingUnitDB = validateAndGetShmHandlingUnit(childPro, txnContext, entityManager);
        ShmShipment shmShipment = validateAndGetShmShipment(shmHandlingUnitDB, txnContext);

        deleteShmHandlingUnit(shmHandlingUnitDB, entityManager, txnContext);

        // If PRO is NOT Astray
        if (isNotAstrayHUMovCd(shmHandlingUnitDB)) {
            List<ShmHandlingUnit> allShmHandlingUnitsDB = shmHandlingUnitSubDAO
                    .findByParentShipmentInstanceId(shmHandlingUnitDB.getId().getShpInstId(), entityManager);
            List<ShmHandlingUnit> allRemainingShmHandlingUnitsDB = CollectionUtils.emptyIfNull(allShmHandlingUnitsDB)
                .stream()
                .filter(hu -> hu.getId().getSeqNbr() != shmHandlingUnitDB.getId().getSeqNbr())
                .collect(Collectors.toList());

            if(CollectionUtils.isNotEmpty(allRemainingShmHandlingUnitsDB)) {
                recreateLnhDimension(allRemainingShmHandlingUnitsDB, shmShipment, auditInfo, entityManager, txnContext);

                //TODO no longer going to happen automatically.
//                updateHandlingUnitsWeight(shmHandlingUnitDB, shmShipment, allRemainingShmHandlingUnitsDB, auditInfo, txnContext);
            }
            
            updateShipment(shmShipment, shmHandlingUnitDB, auditInfo, entityManager, txnContext);
        }

    }

    /**
     * <p>
     * get shipment associated to HU.
     * </p>
     * <p>
     * If shipment billStat >= 3 (billed), only allow delete if Astray. If not Astray throw error cannot delete when part of
     * a billed shipment.
     * </p>
     */
    private ShmShipment validateAndGetShmShipment(ShmHandlingUnit shmHandlingUnitDB, TransactionContext txnContext) throws ValidationException {
        // Get related SHM_SHIPMENT (Lazy loading)
        ShmShipment shmShipment = shmHandlingUnitDB.getShmShipment();

        //Validation bypassed for now to allow deletion even on billed. TODO Add response to give certain warning. 
//        if (BasicTransformer.toLong(shmShipment.getBillStatCd()) >= BasicTransformer.toLong(BillStatusCdTransformer.toCode(BillStatusCd.BILLED))
//                && isNotAstrayHUMovCd(shmHandlingUnitDB)) {
//            throw ExceptionBuilder
//                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
//                .moreInfo("DeleteHandlingUnit", "Cannot delete when part of a billed shipment.")
//                .build();
//        }
        return shmShipment;
    }

    private ShmHandlingUnit validateAndGetShmHandlingUnit(String childPro, TransactionContext txnContext, EntityManager entityManager)
            throws ValidationException, NotFoundException {
        // Validate the childPro format
        ProNumber proNumber = ProNumber.from(childPro);
        if (!proNumber.isValid()) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.PRO_NUMBER_FORMAT, txnContext).build();
        }

        // Validate the childPro is existing, throw HU not found
        ShmHandlingUnit shmHandlingUnitDB = shmHandlingUnitSubDAO.findByTrackingProNumber(proNumber.getNormalized(), entityManager);
        if (shmHandlingUnitDB == null) {
            throw ExceptionBuilder.exception(NotFoundErrorMessage.SHM_HANDLING_UNIT_NOT_FOUND, txnContext).build();
        }
        return shmHandlingUnitDB;
    }

    private void deleteShmHandlingUnit(ShmHandlingUnit shmHandlingUnitDB, EntityManager entityManager, TransactionContext txnContext)
            throws ValidationException, NotFoundException {
        shmHandlingUnitSubDAO.remove(shmHandlingUnitDB, entityManager);
        shmHandlingUnitSubDAO.deleteDB2(shmHandlingUnitDB.getId(), shmHandlingUnitDB.getLstUpdtTmst(), db2EntityManager, txnContext);
    }

    /**
     * based on remaining child PROs, recreate Lnh Dimension based on remaining child PROs
     *
     */
    private void recreateLnhDimension(List<ShmHandlingUnit> allRemainingShmHandlingUnitsDB, ShmShipment shmShipment, AuditInfo auditInfo,
        EntityManager entityManager, TransactionContext txnContext) throws ServiceException {

        shmLnhDimensionDelegate.deleteDimensions(shmShipment, entityManager);
        entityManager.flush();
        db2EntityManager.flush();

        int nextDimSeqNbr = 1;
        for (ShmHandlingUnit shmHandlingUnit : allRemainingShmHandlingUnitsDB) {
            ShmLnhDimensionPK pk = new ShmLnhDimensionPK();
            pk.setShpInstId(shmHandlingUnit.getId().getShpInstId());
            pk.setDimSeqNbr(nextDimSeqNbr++);
            shmLnhDimensionDelegate
                .createShmLnhDimension(pk, shmHandlingUnit.getHeightNbr(), shmHandlingUnit.getWidthNbr(), shmHandlingUnit.getLengthNbr(),
                    auditInfo.getCreatedById(), shmHandlingUnit.getDimensionTypeCd(), auditInfo, entityManager, txnContext);
        }
    }

    /**
     * <p>
     * Depending on the type of HU being deleted (MOTOR or LOOSE).
     * substract 1 from totalMotorMoves and totalPiecesCnt of SHM_SHIPMENT
     * substract 1 from totalLoosePcs and totalPiecesCnt of SHM_SHIPMENT
     * </p>
     */
    private void updateShipment(ShmShipment shmShipment, ShmHandlingUnit shmHandlingUnitDB, AuditInfo auditInfo, EntityManager entityManager,
        TransactionContext txnContext) throws ValidationException, ServiceException {

        if (HU_TYPE_CD_MOTOR.equals(shmHandlingUnitDB.getTypeCd())
                && (NumberUtils.compare(BasicTransformer.toDouble(shmShipment.getMtrzdPcsCnt()), 1d) >= 0)) {
            shmShipment.setMtrzdPcsCnt(shmShipment.getMtrzdPcsCnt().subtract(BigDecimal.ONE));
        } else if (HU_TYPE_CD_LOOSE.equals(shmHandlingUnitDB.getTypeCd())
            && (NumberUtils.compare(BasicTransformer.toDouble(shmShipment.getLoosePcsCnt()), 1d) >= 0)) {
            shmShipment.setLoosePcsCnt(shmShipment.getLoosePcsCnt().subtract(BigDecimal.ONE));
        }
//TODO no longer doing this automatically. it has to be manually done as part of next iteration.
//        shmShipment.setTotPcsCnt(shmShipment.getTotPcsCnt().subtract(BigDecimal.ONE));
//        shmShipment.setTotVolCft(shmShipment.getTotVolCft().subtract(ObjectUtils.defaultIfNull(shmHandlingUnitDB.getVolCft(), BigDecimal.ZERO)));
//        shmShipment.setPupVolPct(shmShipment.getPupVolPct().subtract(ObjectUtils.defaultIfNull(shmHandlingUnitDB.getPupVolPct(), BigDecimal.ZERO)));
//        if (!BasicTransformer.toBoolean(shmHandlingUnitDB.getReweighInd())) {
//            shmShipment.setTotWgtLbs(shmShipment.getTotWgtLbs().subtract(ObjectUtils.defaultIfNull(shmHandlingUnitDB.getWgtLbs(), BigDecimal.ZERO)));
//        }
        DtoTransformer.setLstUpdateAuditInfo(shmShipment, auditInfo);

        shmShipmentSubDAO.save(shmShipment, entityManager);
        shmShipmentSubDAO.updateDB2ShmShipment(shmShipment, shmShipment.getLstUpdtTmst(), txnContext, db2EntityManager);
    }

    /**
     * Excluding remaining child PROs with reweigh ind of Y, we need to redistribute the remaining of SHM_SHIPMENT
     * totalWeightLbs after subtracting the excluded childPROs (optionally, you can just get the weight of the HU being
     * deleted and distribute that instead)
     *
     */
    private void updateHandlingUnitsWeight(ShmHandlingUnit shmHandlingUnitDB, ShmShipment shmShipment,
        List<ShmHandlingUnit> allRemainingShmHandlingUnitsDB, AuditInfo auditInfo, TransactionContext txnContext) throws ServiceException {

        if (!BasicTransformer.toBoolean(shmHandlingUnitDB.getReweighInd())) {
            List<ShmHandlingUnit> allRemaningShmHUsNonReweightedDB = allRemainingShmHandlingUnitsDB
                .stream()
                .filter(hu -> !BasicTransformer.toBoolean(hu.getReweighInd()))
                .collect(Collectors.toList());

            if (!allRemaningShmHUsNonReweightedDB.isEmpty()) {
                BigDecimal wgtToRedistribute = shmHandlingUnitDB.getWgtLbs();
                BigDecimal qtyHUToBeReweighted = new BigDecimal(allRemaningShmHUsNonReweightedDB.size());
                BigDecimal weightToAddForEachHU = wgtToRedistribute.divide(qtyHUToBeReweighted, 2, RoundingMode.HALF_DOWN);
                for (ShmHandlingUnit remaningHUNonRwghtd : allRemaningShmHUsNonReweightedDB) {
                    DtoTransformer.setAuditInfo(remaningHUNonRwghtd, auditInfo);
                    remaningHUNonRwghtd.setWgtLbs(remaningHUNonRwghtd.getWgtLbs().add(weightToAddForEachHU));
                    remaningHUNonRwghtd.setPupVolPct(ObjectUtils.defaultIfNull(remaningHUNonRwghtd.getPupVolPct(), new BigDecimal(0.1)));
                    shmHandlingUnitSubDAO
                        .updateDB2ShmHandlingUnit(remaningHUNonRwghtd, remaningHUNonRwghtd.getLstUpdtTmst(), txnContext, db2EntityManager);
                }
            }

        }
    }

    private boolean isNotAstrayHUMovCd(ShmHandlingUnit shmHU) {
        return !ASTRAY_MOVEMENT_CD.equals(shmHU.getHandlingMvmtCd());
    }

}
