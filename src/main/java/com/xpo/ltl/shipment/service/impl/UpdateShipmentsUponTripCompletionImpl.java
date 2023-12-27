package com.xpo.ltl.shipment.service.impl;


import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.CollectionUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.DispatchEquipment;
import com.xpo.ltl.api.shipment.v2.MovementStatusCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentsUponTripCompletionRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmMovementSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmEventDelegate;
import com.xpo.ltl.shipment.service.enums.MovementTypeEnum;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.ShipmentUtil;
import com.xpo.ltl.shipment.service.util.TimestampUtil;
import com.xpo.ltl.shipment.service.validators.UpdateShipmentsUponTripCompletionValidator;

@RequestScoped
public class UpdateShipmentsUponTripCompletionImpl {

    private static final String N_IND = "N";
    private static final String TCON = MovementTypeEnum.TCON.getCode();
    private static final String UNLOAD = MovementTypeEnum.UNLOAD.getCode();

    @Inject
    private ShmShipmentSubDAO shmShipmentSubDAO;

    @Inject
    private ShmMovementSubDAO shmMovementSubDAO;

    @Inject
    private ShmEventDelegate shmEventDelegate;

    @Inject
    private UpdateShipmentsUponTripCompletionValidator validator;

    @PersistenceContext(unitName="ltl-java-db2-shipment-jaxrs")
    private EntityManager db2EntityManager;

    public void updateShipmentsUponTripCompletion(UpdateShipmentsUponTripCompletionRqst updateShipmentsUponTripCompletionRqst,
                                                  EntityManager entityManager, TransactionContext txnContext) throws ServiceException {

        validator.validate(updateShipmentsUponTripCompletionRqst, txnContext);

        List<ShmShipment> shmShipments = getShipmentsOnTrailer(updateShipmentsUponTripCompletionRqst, entityManager);
        if (CollectionUtils.isEmpty(shmShipments)) {
        	return;
        }

		AuditInfo auditInfo = Objects.nonNull(updateShipmentsUponTripCompletionRqst.getAuditInfo())
				? updateShipmentsUponTripCompletionRqst.getAuditInfo()
				: AuditInfoHelper.getAuditInfo(txnContext);
        
		List<Long> updatedShpInstIds = new ArrayList<>();
				
        for (ShmShipment shmShipment : shmShipments) {
            ShmMovement lastMovementOfShipment = ShipmentUtil.getLastSequenceId(shmShipment.getShmMovements(), shmShipment.getShpInstId());
            ShmMovement lastCreatedMovement = ShipmentUtil.getLastCreatedMovement(shmShipment.getShmMovements(), shmShipment.getShpInstId());

            boolean isCloseMvmt = Objects.nonNull(lastMovementOfShipment.getMvmtTmst()) && lastMovementOfShipment.getMvmtTmst().getTime() == updateShipmentsUponTripCompletionRqst.getCloseDateTime().toGregorianCalendar().getTimeInMillis();
            if (TCON.equalsIgnoreCase(lastMovementOfShipment.getTypCd()) && isCloseMvmt && lastCreatedMovement.getCrteTmst().compareTo(lastMovementOfShipment.getCrteTmst()) == 0) {
                Long nextSequenceId = lastMovementOfShipment.getId().getSeqNbr() + 1;

                ShmMovement newMovement = createNewMovement(shmShipment, nextSequenceId, updateShipmentsUponTripCompletionRqst.getTripCompletionEquipment(), auditInfo);
                shmMovementSubDAO.save(newMovement, entityManager);
                shmMovementSubDAO.createDB2ShmMovement(newMovement, db2EntityManager);
                
                updateShipment(shmShipment, lastMovementOfShipment, updateShipmentsUponTripCompletionRqst.getTripCompletionEquipment(), auditInfo);
                shmShipmentSubDAO.updateDB2ShmShipment(shmShipment,shmShipment.getLstUpdtTmst(), txnContext, db2EntityManager);

				shmEventDelegate.createEventOnTripCompletion(updateShipmentsUponTripCompletionRqst.getTripCompletionEquipment(), shmShipment, newMovement, auditInfo, entityManager);
				
				updatedShpInstIds.add(shmShipment.getShpInstId());
            }
        }

        entityManager.flush();
    }

	private List<ShmShipment> getShipmentsOnTrailer(
			UpdateShipmentsUponTripCompletionRqst updateShipmentsUponTripCompletionRqst, EntityManager entityManager) {
		
		BigDecimal trlrInstId = BasicTransformer.toBigDecimal(updateShipmentsUponTripCompletionRqst.getTripCompletionEquipment().getEquipmentId());
		Timestamp closeTimestamp = BasicTransformer.toTimestamp(updateShipmentsUponTripCompletionRqst.getCloseDateTime());
		
		List<ShmMovement> shmMovements = shmMovementSubDAO.findCloseMovementsByTrailer(trlrInstId, closeTimestamp, entityManager);
		
		/*
		 * The movement timestamp filter is done here, instead of in the DAO query because the DB movement timestamp has nanoseconds 
		 * while closeTimestamp from request only has millisecond precision. This is also why only the milliseconds are compared here.
		 */
		Set<Long> shipmentIdsWithCloseMovements = CollectionUtils.emptyIfNull(shmMovements).stream()
				.filter(shmMovement -> Objects.nonNull(shmMovement.getMvmtTmst()) && shmMovement.getMvmtTmst().getTime() == closeTimestamp.getTime())
				.map(shmMovement -> shmMovement.getId().getShpInstId())
				.collect(Collectors.toSet());
		
		if (CollectionUtils.isEmpty(shipmentIdsWithCloseMovements) && CollectionUtils.isNotEmpty(updateShipmentsUponTripCompletionRqst.getShipmentIds())) {
	        shipmentIdsWithCloseMovements = ShipmentUtil.getShipmentIds(updateShipmentsUponTripCompletionRqst.getShipmentIds());
		}
		
		List<ShmShipment> shmShipments = null;
		
        if (CollectionUtils.isNotEmpty(shipmentIdsWithCloseMovements)) {
            shmShipments = shmShipmentSubDAO.findShipmentOnTrailerWithMovementsByIds(shipmentIdsWithCloseMovements, entityManager);
        } else if (CollectionUtils.isNotEmpty(updateShipmentsUponTripCompletionRqst.getShipmentIds())) {
            List<String> proNbrs = ShipmentUtil.getProNumbers(updateShipmentsUponTripCompletionRqst.getShipmentIds());
            shmShipments = shmShipmentSubDAO.findShipmentOnTrailerWithMovementsByProNbrs(proNbrs, entityManager);
        }
        
        return shmShipments;
    }
    
    private ShmMovement createNewMovement(ShmShipment shmShipment,
                                          Long nextSequenceId,
                                          DispatchEquipment dispatchEquipment,
                                          AuditInfo auditInfo) {

        ShmMovementPK shmMovementPK = new ShmMovementPK();
        shmMovementPK.setShpInstId(shmShipment.getShpInstId());
        shmMovementPK.setSeqNbr(nextSequenceId);
        ShmMovement newMovement = new ShmMovement();
        newMovement.setMvmtTmst(BasicTransformer.toTimestamp(auditInfo.getCreatedTimestamp()));
        newMovement.setId(shmMovementPK);
        newMovement.setShmShipment(shmShipment);
        newMovement.setTypCd(UNLOAD);
        newMovement.setScacCd(SPACE);
        newMovement.setDlvryQalfrCd(SPACE);
        newMovement.setObcPkpDlvRteCd(SPACE);
        newMovement.setRshpCredInd(N_IND);
        newMovement.setMvmtRptgSicCd(dispatchEquipment.getCurrentSic());
        newMovement.setCurrSicCd(dispatchEquipment.getCurrentSic());
        newMovement.setTrlrInstId(BasicTransformer.toBigDecimal(dispatchEquipment.getEquipmentId()));
        newMovement.setTrlrIdPfxTxt(dispatchEquipment.getEquipmentIdPrefix());
        newMovement.setTrlrIdSfxNbr(BasicTransformer.toBigDecimal(dispatchEquipment.getEquipmentIdSuffixNbr()));
        newMovement.setTrlrLdSeqNbr(BigDecimal.ZERO);
        newMovement.setArchiveCntlCd(EMPTY);
        newMovement.setCustLocArivTmst(TimestampUtil.getLowTimestamp());
        newMovement.setQlfrRsnCd(EMPTY);
        newMovement.setRtePfxTxt(EMPTY);
        newMovement.setRteSfxTxt(EMPTY);
        newMovement.setCarrierId(BigDecimal.ZERO);

        DtoTransformer.setAuditInfo(newMovement, auditInfo);

        return newMovement;
    }

    private void updateShipment(ShmShipment shmShipment,
                                ShmMovement movement,
                                DispatchEquipment dispatchEquipment,
                                AuditInfo auditInfo) {
        shmShipment.setCurrSicCd(dispatchEquipment.getCurrentSic());
        shmShipment.setMvmtStatCd(MovementStatusCdTransformer.toCode(MovementStatusCd.ON_DOCK));
        shmShipment.setLstMvmtTmst(movement.getMvmtTmst());
        shmShipment.setLstMvRptgSicCd(dispatchEquipment.getCurrentSic());
        DtoTransformer.setLstUpdateAuditInfo(shmShipment, auditInfo);
    }
}
