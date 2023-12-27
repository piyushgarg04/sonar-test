package com.xpo.ltl.shipment.service.impl;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.DispatchEquipment;
import com.xpo.ltl.api.shipment.v2.Movement;
import com.xpo.ltl.api.shipment.v2.MovementStatusCd;
import com.xpo.ltl.api.shipment.v2.Shipment;
import com.xpo.ltl.api.shipment.v2.ShipmentId;
import com.xpo.ltl.api.shipment.v2.UpdateMovementForShipmentsRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmMovementSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmEventDelegate;
import com.xpo.ltl.shipment.service.enums.MovementTypeEnum;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.ShipmentUtil;
import com.xpo.ltl.shipment.service.validators.UpdateMovementForShipmentsValidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.collections4.CollectionUtils;

@ApplicationScoped
public class UpdateMovementForShipmentsImpl {

    @PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
    private EntityManager db2EntityManager;

    @Inject
    private UpdateMovementForShipmentsValidator updateMovementForShipmentsValidator;

    @Inject
    private ShmShipmentSubDAO shmShipmentSubDAO;

    @Inject
    private ShmMovementSubDAO shmMovementSubDAO;

    @Inject
    private ShmEventDelegate shmEventDelegate;

    public void updateMovementForShipments(
            UpdateMovementForShipmentsRqst request,
            TransactionContext txnContext,
            EntityManager entityManager) throws ServiceException {

        updateMovementForShipmentsValidator.validate(request, txnContext, entityManager);

        List<ShipmentId> shipmentIds = request.getShipmentIds();
        Set<Long> shpInsIds = ShipmentUtil.getShipmentIds(shipmentIds);

        MovementStatusCd movementStatusCd = request.getMovement().getMovementStatusCd();
        String currentSicCd = request.getMovement().getCurrentSicCd();

        AuditInfo auditInfo = getAuditInfo(request.getMovement(), txnContext);

        List<ShmShipment> shmShipments = shmShipmentSubDAO.listShipmentsByShipmentIds(shpInsIds, entityManager);
        validateMovementStatusCd(txnContext, shmShipments);

        // At this point all the shipment has SHM_SHIPMENT.mvmt_stat_cd = '3' (out for delivery)
        List<ShmMovement> shmMovements = shmMovementSubDAO.bulkReadBy(shpInsIds, MovementTypeEnum.OUT_FOR_DELIVERY, entityManager);
        if (CollectionUtils.size(shmMovements) < shipmentIds.size()) {
            throw ExceptionBuilder
                    .exception(NotFoundErrorMessage.MOVEMENT_NF, txnContext)
                    .build();
        }

        deleteMovementsInOutForDlvryStatus(entityManager, shpInsIds, shmMovements);
        bulkUpdateShipmentStatusAndSicCd(shpInsIds, movementStatusCd, currentSicCd, auditInfo, txnContext, entityManager);

        createEventLogShipments(
                EntityTransformer.toShipment(shmShipments),
                shpInsIds,
                movementStatusCd,
                currentSicCd,
                auditInfo,
                txnContext,
                entityManager);
    }

    private void deleteMovementsInOutForDlvryStatus(
            EntityManager entityManager, Collection<? extends Number> shipmentIds, List<ShmMovement> shmMovements) {
        shmMovementSubDAO.bulkDelete(shipmentIds, MovementTypeEnum.OUT_FOR_DELIVERY, entityManager);
        shmMovementSubDAO.bulkDeleteFromDB2(shipmentIds, MovementTypeEnum.OUT_FOR_DELIVERY, db2EntityManager);
        // Detach the objects from the session
        shmMovements.forEach(entityManager::detach);
    }

    private void validateMovementStatusCd(TransactionContext txnContext, List<ShmShipment> shmShipments)
            throws ValidationException {
        for (ShmShipment shmShipment : shmShipments) {
            if (MovementStatusCdTransformer.toEnum(shmShipment.getMvmtStatCd()) != MovementStatusCd.OUT_FOR_DLVRY) {
                throw ExceptionBuilder
                        .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                        .moreInfo("UpdateMovementForShipmentsImpl","Movement status invalid for movement")
                        .build();
            }
        }
    }

    private void createEventLogShipments(List<Shipment> shipments,
                                         Set<Long> shpInsIds,
                                         MovementStatusCd movementStatusCd,
                                         String currentSicCd,
                                         AuditInfo auditInfo,
                                         TransactionContext txnContext,
                                         EntityManager entityManager) throws NotFoundException, ValidationException {



        if (CollectionUtils.isNotEmpty(shipments)) {
            Map<Boolean, List<Shipment>> shipmentsByShipmentId = shipments
                    .stream()
                    .collect(Collectors.partitioningBy(shipment ->
                            shipment.getMovementStatusCd() == MovementStatusCd.OUT_FOR_DLVRY));

            Set<Long> nonOutOfDeliveryShipments = ShipmentUtil.convertToLongSetShipmentInstId(shipmentsByShipmentId.get(false));
            if (CollectionUtils.isNotEmpty(nonOutOfDeliveryShipments)) {
                buildExceptionMovementStatusInvalid(txnContext, nonOutOfDeliveryShipments);
            } else {
                Set<Long> outOfDeliveryShpInstIds = ShipmentUtil.convertToLongSetShipmentInstId(shipmentsByShipmentId.get(true));
                if (CollectionUtils.isNotEmpty(outOfDeliveryShpInstIds)) {
                    processOutOfDeliveryShipments(movementStatusCd, currentSicCd, auditInfo, txnContext, entityManager, shipments, outOfDeliveryShpInstIds);
                }
            }
        } else {
            buildExceptionMovementStatusInvalid(txnContext, shpInsIds);
        }
    }

    private void processOutOfDeliveryShipments(MovementStatusCd movementStatusCd,
                                               String currentSicCd,
                                               AuditInfo auditInfo,
                                               TransactionContext txnContext,
                                               EntityManager entityManager,
                                               List<Shipment> shipments,
                                               Set<Long> outOfDeliveryShpInstIds) throws NotFoundException {

        List<ShmMovement> latestMovements = shmMovementSubDAO.getLatestMovementsForShipmentIdSetByMovementType
                        (outOfDeliveryShpInstIds, MovementTypeEnum.OUT_FOR_DELIVERY, entityManager);

        Set<Long> nonOutFotDeliveryMovements = latestMovements
                .stream()
                .map(movement -> movement.getId().getShpInstId())
                .filter(shipmentInstId -> !outOfDeliveryShpInstIds.contains(shipmentInstId))
                .collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(nonOutFotDeliveryMovements)) {
            Map<Long, ShmMovement> latestMovementByShipmentId = CollectionUtils.emptyIfNull(latestMovements)
                    .stream()
                    .collect(Collectors.toMap(
                            shmMovement -> shmMovement.getId().getShpInstId(),
                            shmMovement -> shmMovement,
                            (k,v) -> v));

            for (Shipment shipment : shipments) {
                ShmMovement latestMovement = latestMovementByShipmentId.get(shipment.getShipmentInstId());
                if(latestMovement != null) {
                    DispatchEquipment dispatchEquipment = new DispatchEquipment();
                    dispatchEquipment.setCurrentSic(currentSicCd);
                    dispatchEquipment.setEquipmentNbr(BasicTransformer.toString(latestMovement.getId().getSeqNbr()));
                    dispatchEquipment.setEquipmentIdPrefix(latestMovement.getTrlrIdPfxTxt());
                    dispatchEquipment.setEquipmentIdSuffixNbr(BasicTransformer.toLong(latestMovement.getTrlrIdSfxNbr()));
                    shmEventDelegate.createUnassignStopShipmentEvent
                            (shipment, movementStatusCd, dispatchEquipment, auditInfo, txnContext, entityManager);
                }
            }
        } else {
            processShipmentsWithoutMovements(outOfDeliveryShpInstIds, txnContext);
        }
    }

    private void processShipmentsWithoutMovements(Set<Long> nonOutFotDeliveryMovements,
                                            TransactionContext txnContext) throws NotFoundException {
        List<MoreInfo> moreInfos =  Lists.newArrayList();
        nonOutFotDeliveryMovements.forEach(shipmentId ->
                moreInfos.add(
                        createMoreInfo(
                                "shipmentId",
                                "Movement not found for shipmentId: " + shipmentId)));
        throw ExceptionBuilder
                .exception(NotFoundErrorMessage.MOVEMENT_NF, txnContext)
                .moreInfo(moreInfos)
                .build();
    }

    private void buildExceptionMovementStatusInvalid(TransactionContext txnContext,
                                                     Set<Long> shipmentIds) throws ValidationException {
        List<MoreInfo> moreInfos =  Lists.newArrayList();
        shipmentIds.forEach(shipmentId ->
                moreInfos.add(
                        createMoreInfo(
                                "shipmentId",
                                "Movement status invalid for movement with shipmentId: " + shipmentId)));
        throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo(moreInfos)
                .build();
    }

    private MoreInfo createMoreInfo(String location, String message) {
        MoreInfo moreInfo = new MoreInfo();
        moreInfo.setItemNbr(null);
        moreInfo.setMessage(message);
        moreInfo.setLocation(location);
        return moreInfo;
    }

    private void bulkUpdateShipmentStatusAndSicCd(
            Set<Long> shpInsIds,
            MovementStatusCd movementStatusCd,
            String currentSicCd,
            AuditInfo auditInfo,
            TransactionContext txnContext,
            EntityManager entityManager) throws ServiceException {

        shmShipmentSubDAO.bulkUpdateShipmentStatusAndSicCd
                (shpInsIds, movementStatusCd, currentSicCd, auditInfo, txnContext, entityManager);
        shmShipmentSubDAO.bulkUpdateShipmentStatusAndSicCdFromDB2
                (shpInsIds, movementStatusCd, currentSicCd, auditInfo, txnContext, db2EntityManager);
    }

    private AuditInfo getAuditInfo(Movement movement, TransactionContext txnContext) {
        AuditInfo auditInfo = movement.getAuditInfo();
        if(auditInfo == null) {
            return AuditInfoHelper.getAuditInfo(txnContext);
        } else {
            return auditInfo;
        }
    }
}
