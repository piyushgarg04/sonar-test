package com.xpo.ltl.shipment.service.dao;

import static com.xpo.ltl.api.shipment.v2.ShipmentDetailCd.ACCESSORIAL;
import static com.xpo.ltl.api.shipment.v2.ShipmentDetailCd.ADVANCE_BEYOND;
import static com.xpo.ltl.api.shipment.v2.ShipmentDetailCd.BILL_ENTRY_STATS;
import static com.xpo.ltl.api.shipment.v2.ShipmentDetailCd.COMMODITY;
import static com.xpo.ltl.api.shipment.v2.ShipmentDetailCd.CUSTOMS_BOND;
import static com.xpo.ltl.api.shipment.v2.ShipmentDetailCd.EVENT_LOG;
import static com.xpo.ltl.api.shipment.v2.ShipmentDetailCd.HANDLING_UNIT;
import static com.xpo.ltl.api.shipment.v2.ShipmentDetailCd.HANDLING_UNIT_MOVEMENT;
import static com.xpo.ltl.api.shipment.v2.ShipmentDetailCd.LINEHAUL_DIMENSIONS;
import static com.xpo.ltl.api.shipment.v2.ShipmentDetailCd.MANAGEMENT_REMARK;
import static com.xpo.ltl.api.shipment.v2.ShipmentDetailCd.MISC_LINE_ITEM;
import static com.xpo.ltl.api.shipment.v2.ShipmentDetailCd.MOVEMENT_EXCEPTION;
import static com.xpo.ltl.api.shipment.v2.ShipmentDetailCd.OPERATIONS_SHIPMENT;
import static com.xpo.ltl.api.shipment.v2.ShipmentDetailCd.REMARKS;
import static com.xpo.ltl.api.shipment.v2.ShipmentDetailCd.SHIPMENT_PARTIES;
import static com.xpo.ltl.api.shipment.v2.ShipmentDetailCd.SUPP_REF_NBR;

import java.util.Collection;

import com.xpo.ltl.api.shipment.v2.ShipmentDetailCd;
import com.xpo.ltl.shipment.service.util.ShipmentDetailCdUtil;

public class ShmShipmentEagerLoadPlan {

    private boolean shmAcSvcs;
    private boolean shmAdvBydCarrs;
    private boolean shmAsEntdCusts;
    private boolean shmCommodities;
    private boolean shmCustomsBonds;
    private boolean shmCustomsCntrls;
    private boolean shmEventLogs;
    private boolean shmHandlingUnits;
    private ShmHandlingUnitEagerLoadPlan shmHandlingUnitEagerLoadPlan;
    private boolean shmHazMats;
    private boolean shmLnhDimensions;
    private boolean shmMgmtRemarks;
    private boolean shmMiscLineItems;
    private boolean shmMovements;
    private boolean shmOpsShipment;
    private boolean shmRemarks;
    private boolean shmBillEntryStats;
    private boolean shmShipment;
    private boolean shmShipments;
    private boolean shmSrNbrs;

    public ShmShipmentEagerLoadPlan() {
        shmHandlingUnitEagerLoadPlan = new ShmHandlingUnitEagerLoadPlan();
    }

    public boolean isShmAcSvcs() {
        return shmAcSvcs;
    }

    public void setShmAcSvcs(boolean shmAcSvcs) {
        this.shmAcSvcs = shmAcSvcs;
    }

    public boolean isShmAdvBydCarrs() {
        return shmAdvBydCarrs;
    }

    public void setShmAdvBydCarrs(boolean shmAdvBydCarrs) {
        this.shmAdvBydCarrs = shmAdvBydCarrs;
    }

    public boolean isShmAsEntdCusts() {
        return shmAsEntdCusts;
    }

    public void setShmAsEntdCusts(boolean shmAsEntdCusts) {
        this.shmAsEntdCusts = shmAsEntdCusts;
    }

    public boolean isShmCommodities() {
        return shmCommodities;
    }

    public void setShmCommodities(boolean shmCommodities) {
        this.shmCommodities = shmCommodities;
    }

    public boolean isShmCustomsBonds() {
        return shmCustomsBonds;
    }

    public void setShmCustomsBonds(boolean shmCustomsBonds) {
        this.shmCustomsBonds = shmCustomsBonds;
    }

    public boolean isShmCustomsCntrls() {
        return shmCustomsCntrls;
    }

    public void setShmCustomsCntrls(boolean shmCustomsCntrls) {
        this.shmCustomsCntrls = shmCustomsCntrls;
    }

    public boolean isShmEventLogs() {
        return shmEventLogs;
    }

    public void setShmEventLogs(boolean shmEventLogs) {
        this.shmEventLogs = shmEventLogs;
    }

    public boolean isShmHandlingUnits() {
        return shmHandlingUnits;
    }

    public void setShmHandlingUnits(boolean shmHandlingUnits) {
        this.shmHandlingUnits = shmHandlingUnits;
    }

    public ShmHandlingUnitEagerLoadPlan getShmHandlingUnitEagerLoadPlan() {
        return shmHandlingUnitEagerLoadPlan;
    }

    public void setShmHandlingUnitEagerLoadPlan(ShmHandlingUnitEagerLoadPlan shmHandlingUnitEagerLoadPlan) {
        this.shmHandlingUnitEagerLoadPlan = shmHandlingUnitEagerLoadPlan;
    }

    public boolean isShmHazMats() {
        return shmHazMats;
    }

    public void setShmHazMats(boolean shmHazMats) {
        this.shmHazMats = shmHazMats;
    }

    public boolean isShmLnhDimensions() {
        return shmLnhDimensions;
    }

    public void setShmLnhDimensions(boolean shmLnhDimensions) {
        this.shmLnhDimensions = shmLnhDimensions;
    }

    public boolean isShmMgmtRemarks() {
        return shmMgmtRemarks;
    }

    public void setShmMgmtRemarks(boolean shmMgmtRemarks) {
        this.shmMgmtRemarks = shmMgmtRemarks;
    }

    public boolean isShmMiscLineItems() {
        return shmMiscLineItems;
    }

    public void setShmMiscLineItems(boolean shmMiscLineItems) {
        this.shmMiscLineItems = shmMiscLineItems;
    }

    public boolean isShmMovements() {
        return shmMovements;
    }

    public void setShmMovements(boolean shmMovements) {
        this.shmMovements = shmMovements;
    }

    public boolean isShmOpsShipment() {
        return shmOpsShipment;
    }

    public void setShmOpsShipment(boolean shmOpsShipment) {
        this.shmOpsShipment = shmOpsShipment;
    }

    public boolean isShmRemarks() {
        return shmRemarks;
    }

    public void setShmRemarks(boolean shmRemarks) {
        this.shmRemarks = shmRemarks;
    }

    public boolean isShmBillEntryStats() {
        return shmBillEntryStats;
    }

    public void setShmBillEntryStats(boolean shmBillEntryStats) {
        this.shmBillEntryStats = shmBillEntryStats;
    }

    public boolean isShmShipment() {
        return shmShipment;
    }

    public void setShmShipment(boolean shmShipment) {
        this.shmShipment = shmShipment;
    }

    public boolean isShmShipments() {
        return shmShipments;
    }

    public void setShmShipments(boolean shmShipments) {
        this.shmShipments = shmShipments;
    }

    public boolean isShmSrNbrs() {
        return shmSrNbrs;
    }

    public void setShmSrNbrs(boolean shmSrNbrs) {
        this.shmSrNbrs = shmSrNbrs;
    }

    public static ShmShipmentEagerLoadPlan from(Collection<ShipmentDetailCd> shipmentDetailCds,
                                                boolean shmHandlingUnitMvmtsForSplitOnly) {
        ShmShipmentEagerLoadPlan plan = new ShmShipmentEagerLoadPlan();

        plan.setShmAcSvcs
            (ShipmentDetailCdUtil.contains
                 (shipmentDetailCds, true, ACCESSORIAL));

        plan.setShmAdvBydCarrs
            (ShipmentDetailCdUtil.contains
                 (shipmentDetailCds, true, ADVANCE_BEYOND));

        plan.setShmAsEntdCusts
            (ShipmentDetailCdUtil.contains
                 (shipmentDetailCds, true, SHIPMENT_PARTIES));

        plan.setShmCommodities
            (ShipmentDetailCdUtil.contains
                 (shipmentDetailCds, true, COMMODITY));

        plan.setShmCustomsBonds
            (ShipmentDetailCdUtil.contains
                 (shipmentDetailCds, true, CUSTOMS_BOND));

        plan.setShmCustomsCntrls
            (ShipmentDetailCdUtil.contains
                 (shipmentDetailCds, true, CUSTOMS_BOND));

        plan.setShmEventLogs
            (ShipmentDetailCdUtil.contains
                 (shipmentDetailCds, false, EVENT_LOG));

        plan.setShmHandlingUnits
            (ShipmentDetailCdUtil.contains
                 (shipmentDetailCds,
                  true,
                  HANDLING_UNIT,
                  HANDLING_UNIT_MOVEMENT)
             || ShipmentDetailCdUtil.containsShipmentOnlyOrOthers
                    (shipmentDetailCds, true));

        plan.setShmHandlingUnitEagerLoadPlan
            (ShmHandlingUnitEagerLoadPlan.from
                 (shipmentDetailCds, shmHandlingUnitMvmtsForSplitOnly));

        plan.setShmHazMats
            (ShipmentDetailCdUtil.containsShipmentOnlyOrOthers
                 (shipmentDetailCds, true));

        plan.setShmLnhDimensions
            (ShipmentDetailCdUtil.contains
                 (shipmentDetailCds, true, LINEHAUL_DIMENSIONS));

        plan.setShmMgmtRemarks
            (ShipmentDetailCdUtil.contains
                 (shipmentDetailCds, true, MANAGEMENT_REMARK));

        plan.setShmMiscLineItems
            (ShipmentDetailCdUtil.contains
                 (shipmentDetailCds, true, MISC_LINE_ITEM));

        plan.setShmMovements
            (ShipmentDetailCdUtil.contains
                 (shipmentDetailCds, true, MOVEMENT_EXCEPTION));

        plan.setShmOpsShipment
            (ShipmentDetailCdUtil.contains
                 (shipmentDetailCds, true, OPERATIONS_SHIPMENT)
             || ShipmentDetailCdUtil.containsShipmentOnlyOrOthers
                    (shipmentDetailCds, true));

        plan.setShmRemarks
            (ShipmentDetailCdUtil.contains
                 (shipmentDetailCds, true, REMARKS));

        plan.setShmBillEntryStats
            (ShipmentDetailCdUtil.contains
                 (shipmentDetailCds, false, BILL_ENTRY_STATS));

        plan.setShmShipment
            (ShipmentDetailCdUtil.containsShipmentOnlyOrOthers
                 (shipmentDetailCds, true));

        plan.setShmSrNbrs
            (ShipmentDetailCdUtil.contains
                 (shipmentDetailCds, true, SUPP_REF_NBR));

        return plan;
    }

    public static ShmShipmentEagerLoadPlan from(Collection<ShipmentDetailCd> shipmentDetailCds) {
        return from(shipmentDetailCds, false);
    }

}
