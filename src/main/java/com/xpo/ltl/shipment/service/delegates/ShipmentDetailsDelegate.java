package com.xpo.ltl.shipment.service.delegates;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityNotFoundException;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmt;
import com.xpo.ltl.api.shipment.service.entity.ShmMgmtRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.service.entity.ShmSrNbr;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.HandlingUnitMovementTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.HandlingUnit;
import com.xpo.ltl.api.shipment.v2.HandlingUnitMovementTypeCd;
import com.xpo.ltl.api.shipment.v2.ManagementRemark;
import com.xpo.ltl.api.shipment.v2.ShipmentDetailCd;
import com.xpo.ltl.api.shipment.v2.ShipmentDetails;
import com.xpo.ltl.api.shipment.v2.ShipmentId;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitEagerLoadPlan;
import com.xpo.ltl.shipment.service.dao.ShmShipmentEagerLoadPlan;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.ShipmentDetailCdUtil;

@ApplicationScoped
@LogExecutionTime
public class ShipmentDetailsDelegate {

    private static final Logger LOGGER = LogManager.getLogger(ShipmentDetailsDelegate.class);

    public ShipmentDetails buildDetails(ShmShipment shmShipment,
                                        ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan) {
        ShipmentDetails detail = new ShipmentDetails();

        detail.setShipment
            (EntityTransformer.toShipment
                 (Objects.requireNonNull(shmShipment)));

        buildParentShipmentId(shmShipment, shmShipmentEagerLoadPlan, detail);

        buildHazMat(shmShipment, shmShipmentEagerLoadPlan, detail);

        buildAsMatchedParty(shmShipment, shmShipmentEagerLoadPlan, detail);

        buildAccesorialService(shmShipment, shmShipmentEagerLoadPlan, detail);

        buildAdvanceBeyondCarrier(shmShipment, shmShipmentEagerLoadPlan, detail);

        buildCustomsBond(shmShipment, shmShipmentEagerLoadPlan, detail);

        buildCustomsControl(shmShipment, shmShipmentEagerLoadPlan, detail);

        buildHandlingUnit(shmShipment, shmShipmentEagerLoadPlan, detail);

        buildMiscLineItem(shmShipment, shmShipmentEagerLoadPlan, detail);

        buildRemark(shmShipment, shmShipmentEagerLoadPlan, detail);

        buildSuppRefNbr(shmShipment, shmShipmentEagerLoadPlan, detail);

        buildCommodity(shmShipment, shmShipmentEagerLoadPlan, detail);

        buildLinehaulDimension(shmShipment, shmShipmentEagerLoadPlan, detail);

        buildOperationsShipment(shmShipment, shmShipmentEagerLoadPlan, detail);

        buildManagementRemark(shmShipment, shmShipmentEagerLoadPlan, detail);

        buildMovement(shmShipment, shmShipmentEagerLoadPlan, detail);

        return detail;
    }

    private void buildParentShipmentId(ShmShipment shmShipment,
                                       ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
                                       ShipmentDetails detail) {
        if (!shmShipmentEagerLoadPlan.isShmShipment())
            return;

        try {
            ShmShipment parentShmShipment = shmShipment.getShmShipment();

            if (parentShmShipment == null
                || parentShmShipment.getShpInstId() == 0L)
                return;

            ShipmentId shipmentId = new ShipmentId();

            shipmentId.setShipmentInstId
                (String.valueOf(parentShmShipment.getShpInstId()));
            shipmentId.setProNumber
                (parentShmShipment.getProNbrTxt());
            shipmentId.setPickupDate
                (BasicTransformer.toXMLGregorianCalendar
                     (parentShmShipment.getPkupDt()));

            detail.setParentShipmentId(shipmentId);
        }
        catch (EntityNotFoundException e) {
            // This could happen when the requested PRO is MOVR and
            // parentShipment is NOT FOUND
            LOGGER.warn("buildParentShipmentId: Broken foreign key from ShmShipment with shpInstId={}, proNbrTxt={} to parent ShmShipment: {}",
                        shmShipment.getShpInstId(),
                        shmShipment.getProNbrTxt(),
                        ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private void buildHazMat(ShmShipment shmShipment,
                             ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
                             ShipmentDetails detail) {
        if (!shmShipmentEagerLoadPlan.isShmHazMats())
            return;

        detail.setHazMat
            (EntityTransformer.toHazMat(shmShipment.getShmHazMats()));
    }

    private void buildAsMatchedParty(ShmShipment shmShipment,
                                     ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
                                     ShipmentDetails details) {
        if (!shmShipmentEagerLoadPlan.isShmAsEntdCusts())
            return;

        details.setAsMatchedParty
            (EntityTransformer.toAsMatchedParty
                 (shmShipment.getShmAsEntdCusts()));
    }

    private void buildAccesorialService(ShmShipment shmShipment,
                                        ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
                                        ShipmentDetails detail) {
        if (!shmShipmentEagerLoadPlan.isShmAcSvcs())
            return;

        detail.setAccessorialService
            (EntityTransformer.toAccessorialService
                 (shmShipment.getShmAcSvcs()));
    }

    private void buildAdvanceBeyondCarrier(ShmShipment shmShipment,
                                           ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
                                           ShipmentDetails detail) {
        if (!shmShipmentEagerLoadPlan.isShmAdvBydCarrs())
            return;

        detail.setAdvanceBeyondCarrier
            (EntityTransformer.toAdvanceBeyondCarrier
                 (shmShipment.getShmAdvBydCarrs()));
    }

    private void buildCustomsBond(ShmShipment shmShipment,
                                  ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
                                  ShipmentDetails detail) {
        if (!shmShipmentEagerLoadPlan.isShmCustomsBonds())
            return;

        detail.setCustomsBond
            (EntityTransformer.toCustomsBond
                 (shmShipment.getShmCustomsBonds()));
    }

    private void buildCustomsControl(ShmShipment shmShipment,
                                     ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
                                     ShipmentDetails detail) {
        if (!shmShipmentEagerLoadPlan.isShmCustomsCntrls())
            return;

        detail.setCustomsControl
            (EntityTransformer.toCustomsControl
                 (shmShipment.getShmCustomsCntrls()));
    }

    private void buildHandlingUnit(ShmShipment shmShipment,
                                   ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
                                   ShipmentDetails details) {
        if (!shmShipmentEagerLoadPlan.isShmHandlingUnits())
            return;

        ShmHandlingUnitEagerLoadPlan shmHandlingUnitEagerLoadPlan =
            shmShipmentEagerLoadPlan.getShmHandlingUnitEagerLoadPlan();

        List<HandlingUnit> handlingUnits = new ArrayList<>();

        for (ShmHandlingUnit shmHandlingUnit : shmShipment.getShmHandlingUnits()) {
            HandlingUnit handlingUnit =
                EntityTransformer.toHandlingUnit(shmHandlingUnit);

            if (shmHandlingUnitEagerLoadPlan != null
                && shmHandlingUnitEagerLoadPlan.isShmHandlingUnitMvmts()) {
                List<ShmHandlingUnitMvmt> shmHandlingUnitMvmts = null;

                if (shmHandlingUnitEagerLoadPlan
                        .isShmHandlingUnitMvmtsForSplitOnly()) {
                    if (BooleanUtils.isTrue(handlingUnit.getSplitInd())) {
                        shmHandlingUnitMvmts = new ArrayList<>();

                        ShmHandlingUnitMvmt lastSplitShmHandlingUnitMvmt =
                            shmHandlingUnit.getShmHandlingUnitMvmts().stream()
                                .filter(shmHandlingUnitMvmt ->
                                            HandlingUnitMovementTypeCdTransformer.toEnum
                                                (shmHandlingUnitMvmt.getMvmtTypCd())
                                                == HandlingUnitMovementTypeCd.SPLIT)
                                .max(Comparator.comparing
                                         (shmHandlingUnitMvmt ->
                                              shmHandlingUnitMvmt
                                                  .getId()
                                                  .getMvmtSeqNbr()))
                                .orElse(null);

                        if (lastSplitShmHandlingUnitMvmt != null)
                            shmHandlingUnitMvmts.add
                                (lastSplitShmHandlingUnitMvmt);
                    }
                }
                else {
                    shmHandlingUnitMvmts =
                        shmHandlingUnit.getShmHandlingUnitMvmts();
                }

                handlingUnit.setHandlingUnitMovement
                    (EntityTransformer.toHandlingUnitMovement
                         (shmHandlingUnitMvmts));
            }

            handlingUnits.add(handlingUnit);
        }

        details.getShipment().setHandlingUnit(handlingUnits);
    }

    private void buildMiscLineItem(ShmShipment shmShipment,
                                   ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
                                   ShipmentDetails detail) {
        if (!shmShipmentEagerLoadPlan.isShmMiscLineItems())
            return;

        detail.setMiscLineItem
            (EntityTransformer.toMiscLineItem(shmShipment.getShmMiscLineItems()));
    }

    private void buildRemark(ShmShipment shmShipment,
                             ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
                             ShipmentDetails detail) {
        if (!shmShipmentEagerLoadPlan.isShmRemarks())
            return;

        detail.setRemark
            (EntityTransformer.toRemark(shmShipment.getShmRemarks()));
    }

    private void buildSuppRefNbr(ShmShipment shmShipment,
                                 ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
                                 ShipmentDetails detail) {
        if (!shmShipmentEagerLoadPlan.isShmSrNbrs())
            return;
        
        detail.setSuppRefNbr
            (EntityTransformer.toSuppRefNbr(shmShipment.getShmSrNbrs()));
    }

    private void buildCommodity(ShmShipment shmShipment,
                                ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
                                ShipmentDetails detail) {
        if (!shmShipmentEagerLoadPlan.isShmCommodities())
            return;

        detail.setCommodity
            (EntityTransformer.toCommodity(shmShipment.getShmCommodities()));
    }

    private void buildLinehaulDimension(ShmShipment shmShipment,
                                         ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
                                         ShipmentDetails detail) {
        if (!shmShipmentEagerLoadPlan.isShmLnhDimensions())
            return;

        detail.setLinehaulDimension
            (EntityTransformer.toLnhDimension(shmShipment.getShmLnhDimensions()));
    }

    private void buildOperationsShipment(ShmShipment shmShipment,
                                         ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
                                         ShipmentDetails detail) {
        if (!shmShipmentEagerLoadPlan.isShmOpsShipment())
            return;

        detail.setOperationsShipment
            (EntityTransformer.toOperationsShipment
                 (shmShipment.getShmOpsShipment()));
    }

	private void buildManagementRemark(ShmShipment shmShipment, ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
			ShipmentDetails detail) {
		if (!shmShipmentEagerLoadPlan.isShmMgmtRemarks())
			return;

		List<ShmMgmtRemark> mgmtRemarks = shmShipment.getShmMgmtRemarks().stream()
				.sorted(Comparator.comparing(mgmtRemark -> mgmtRemark.getRemarkId())).collect(Collectors.toList());

		detail.setManagementRemark(EntityTransformer.toManagementRemark(mgmtRemarks));

		if (null != detail.getManagementRemark()) {
			Integer sequenceNbr = 1;
			for (ManagementRemark managementRemark : detail.getManagementRemark()) {
				managementRemark.setSequenceNbr(EntityTransformer.toBigInteger(sequenceNbr++));
				managementRemark.setShipmentInstId(shmShipment.getShpInstId());
			}
		}

	}

    private void buildMovement(ShmShipment shmShipment,
                               ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
                               ShipmentDetails detail) {
        if (!shmShipmentEagerLoadPlan.isShmMovements())
            return;

        List<ShmMovement> shmMovements =
            shmShipment.getShmMovements().stream()
                .sorted(Comparator.comparing
                            (shmMovement -> shmMovement.getId().getSeqNbr()))
                .collect(Collectors.toList());

        detail.getShipment().setMovement
            (EntityTransformer.toMovement(shmMovements));
    }

}
