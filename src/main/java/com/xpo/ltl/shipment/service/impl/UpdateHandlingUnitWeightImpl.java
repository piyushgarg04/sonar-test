package com.xpo.ltl.shipment.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.UpdateHandlingUnitWeightRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.validators.UpdateHandlingUnitWeightValidator;

@RequestScoped
@LogExecutionTime
public class UpdateHandlingUnitWeightImpl {

    private static final Log LOGGER = LogFactory.getLog(UpdateHandlingUnitWeightImpl.class);

    @PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
    private EntityManager db2EntityManager;

    @Inject
    private UpdateHandlingUnitWeightValidator validator;

    @Inject
    private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

    @Inject
    private ShmShipmentSubDAO shmShipmentSubDAO;

    private static final String COMMON_PGM_ID = "HUREWGH";

    public void updateHandlingUnitWeight(UpdateHandlingUnitWeightRqst updateHandlingUnitWeightRqst,
        String childProNbr, TransactionContext txnContext, EntityManager entityManager) throws ServiceException {

        validator.validate(updateHandlingUnitWeightRqst, childProNbr, txnContext, entityManager);

        AuditInfo auditInfo = AuditInfoHelper.getAuditInfoWithPgmId(COMMON_PGM_ID, txnContext);

        BigDecimal newWeight = BasicTransformer.toBigDecimal(updateHandlingUnitWeightRqst.getWeightLbs());

        ShmHandlingUnit handlingUnit = shmHandlingUnitSubDAO
            .findByTrackingProNumber(childProNbr, entityManager);

        if (handlingUnit == null) {
            // throw handling unit not found?
            throw ExceptionBuilder
                .exception(NotFoundErrorMessage.SHM_HANDLING_UNIT_NOT_FOUND, txnContext)
                .moreInfo("UpdateHandlingUnitWeight", "ChildPro: " + childProNbr)
                .log()
                .build();
        }

        if (updateHandlingUnitWeightRqst.isReweighInd() == null || updateHandlingUnitWeightRqst.isReweighInd()) {

            // Update the Handling Unit weight. Set the REWEIGH_IND to Y for this input child PRO.
            updateWeightForActualHandlingUnit(handlingUnit, newWeight, Boolean.TRUE, txnContext, entityManager, auditInfo);

            String parentPro = handlingUnit.getParentProNbrTxt();

            // Get the SHM_SHIPMENT.TOT_WGT_LBS and subtract the input weight.
            ShmShipment shmShipment = shmShipmentSubDAO.findByIdOrProNumber(parentPro, null, entityManager);
            BigDecimal totWgtLbs = shmShipment.getTotWgtLbs();

            List<ShmHandlingUnit> handlingUnitListForParentPro = shmHandlingUnitSubDAO
                    .findByParentProNumber(parentPro, entityManager);

            updateWeightForHandlingUnits(handlingUnitListForParentPro, childProNbr, newWeight, totWgtLbs, txnContext,
                    entityManager, auditInfo);

            // -- Update SHM_SHIPMENT.REWEIGH_WGT_LBS with total weight of all SHM_HANDLING_UNIT
            updateTotalReWeightForShipment(shmShipment, handlingUnitListForParentPro, newWeight, childProNbr, txnContext,
                    entityManager, auditInfo);

        } else {

            updateWeightForActualHandlingUnit(handlingUnit, newWeight, Boolean.FALSE, txnContext, entityManager, auditInfo);

        }

    }

    private void updateWeightForActualHandlingUnit(
            ShmHandlingUnit handlingUnit,
            BigDecimal newWeight,
            Boolean reweighInd,
            TransactionContext txnContext,
            EntityManager entityManager,
            AuditInfo auditInfo)
                    throws ValidationException, ServiceException {

        handlingUnit.setReweighInd(BasicTransformer.toString(reweighInd));
        handlingUnit.setWgtLbs(newWeight);
        DtoTransformer.setLstUpdateAuditInfo(handlingUnit, auditInfo);
        shmHandlingUnitSubDAO.persist(handlingUnit, entityManager);
        shmHandlingUnitSubDAO
            .updateDB2ShmHandlingUnit(handlingUnit, handlingUnit.getLstUpdtTmst(), txnContext, db2EntityManager);
    }

    /**
     * Find all with REWEIGH_IND is N and divide the remainder weight evenly to these child PROs.
     */
    private void updateWeightForHandlingUnits(List<ShmHandlingUnit> handlingUnitListForParentPro, String childProNbr,
        BigDecimal newWeight, BigDecimal totWgtLbs, TransactionContext txnContext, EntityManager entityManager, AuditInfo auditInfo)
            throws ValidationException, ServiceException {

        List<ShmHandlingUnit> handlingUnitsListNonReWeighted = CollectionUtils
            .emptyIfNull(handlingUnitListForParentPro)
            .stream()
            .filter(
                hu -> !BasicTransformer.toBoolean(hu.getReweighInd()) && !hu.getChildProNbrTxt().equals(childProNbr))
            .collect(Collectors.toList());

        int handlingUnitQtyToReweight = handlingUnitsListNonReWeighted.size();

        if(handlingUnitQtyToReweight > 0) {
            BigDecimal totalHUWeightReWeighted = CollectionUtils
                .emptyIfNull(handlingUnitListForParentPro)
                .stream()
                .filter(
                    hu -> BasicTransformer.toBoolean(hu.getReweighInd()) && !hu.getChildProNbrTxt().equals(childProNbr))
                .map(hu -> hu.getWgtLbs())
                .reduce((x, y) -> x.add(y))
                .orElse(BigDecimal.ZERO);

            BigDecimal weightToReDistributeEvenly = totWgtLbs.subtract(totalHUWeightReWeighted).subtract(newWeight);
            if (weightToReDistributeEvenly.signum() < 0) {
                weightToReDistributeEvenly = BigDecimal.ZERO;
            }
            BigDecimal weightForEachHU = weightToReDistributeEvenly
                .divide(new BigDecimal(handlingUnitQtyToReweight), 2, RoundingMode.HALF_DOWN);
            handlingUnitsListNonReWeighted.stream().forEach(hu -> {
                hu.setWgtLbs(BigDecimal.ZERO.compareTo(weightForEachHU) == 0 ? BigDecimal.ONE : weightForEachHU);
                hu.setReweighInd(BasicTransformer.toString(Boolean.FALSE));
                DtoTransformer.setLstUpdateAuditInfo(hu, auditInfo);
            });

            shmHandlingUnitSubDAO.persist(handlingUnitsListNonReWeighted, entityManager);
            updateDB2ShmHandlingUnitList(handlingUnitsListNonReWeighted, txnContext);
        }
    }

    private void updateTotalReWeightForShipment(ShmShipment shmShipment,
        List<ShmHandlingUnit> handlingUnitListForParentPro, BigDecimal newWeight, String childProNbr,
        TransactionContext txnContext, EntityManager entityManager, AuditInfo auditInfo) 
                throws ValidationException, ServiceException {
        BigDecimal totReWeight = CollectionUtils
            .emptyIfNull(handlingUnitListForParentPro)
                .stream()
                .filter(hu -> !hu.getChildProNbrTxt().equals(childProNbr))
                .map(hu -> hu.getWgtLbs())
                .reduce((x, y) -> x.add(y))
            .orElse(BigDecimal.ZERO)
            .add(newWeight);
        shmShipment.setReweighWgtLbs(totReWeight);
        
        DtoTransformer.setLstUpdateAuditInfo(shmShipment, auditInfo);
        
        shmShipmentSubDAO.save(shmShipment, entityManager);
        shmShipmentSubDAO.updateDb2ShmShipmentForUpdHUWeight(shmShipment, db2EntityManager);

    }

    private void updateDB2ShmHandlingUnitList(List<ShmHandlingUnit> handlingUnitsListNonReWeighted,
        TransactionContext txnContext) throws ServiceException {
        for (ShmHandlingUnit hu : handlingUnitsListNonReWeighted) {
            shmHandlingUnitSubDAO.updateDB2ShmHandlingUnit(hu, hu.getLstUpdtTmst(), txnContext, db2EntityManager);
        }
    }

}
