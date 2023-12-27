package com.xpo.ltl.shipment.service.dao;

import static com.xpo.ltl.api.shipment.v2.ShipmentDetailCd.HANDLING_UNIT_MOVEMENT;

import java.util.Collection;

import com.xpo.ltl.api.shipment.v2.ShipmentDetailCd;
import com.xpo.ltl.shipment.service.util.ShipmentDetailCdUtil;

public class ShmHandlingUnitEagerLoadPlan {

    private boolean shmHandlingUnitMvmts;
    private boolean shmHandlingUnitMvmtsForSplitOnly;

    public ShmHandlingUnitEagerLoadPlan() {
    }

    public boolean isShmHandlingUnitMvmts() {
        return shmHandlingUnitMvmts;
    }

    public void setShmHandlingUnitMvmts(boolean shmHandlingUnitMvmts) {
        this.shmHandlingUnitMvmts = shmHandlingUnitMvmts;
    }

    public boolean isShmHandlingUnitMvmtsForSplitOnly() {
        return shmHandlingUnitMvmtsForSplitOnly;
    }

    public void setShmHandlingUnitMvmtsForSplitOnly(boolean shmHandlingUnitMvmtsForSplitOnly) {
        this.shmHandlingUnitMvmtsForSplitOnly = shmHandlingUnitMvmtsForSplitOnly;
    }

    public static ShmHandlingUnitEagerLoadPlan from(Collection<ShipmentDetailCd> shipmentDetailCds,
                                                    boolean shmHandlingUnitMvmtsForSplitOnly) {
        ShmHandlingUnitEagerLoadPlan plan = new ShmHandlingUnitEagerLoadPlan();

        plan.setShmHandlingUnitMvmts
            (ShipmentDetailCdUtil.contains
                 (shipmentDetailCds, true, HANDLING_UNIT_MOVEMENT));

        plan.setShmHandlingUnitMvmtsForSplitOnly
            (plan.isShmHandlingUnitMvmts()
             && shmHandlingUnitMvmtsForSplitOnly);

        return plan;
    }

    public static ShmHandlingUnitEagerLoadPlan from(Collection<ShipmentDetailCd> shipmentDetailCds) {
        return from(shipmentDetailCds, false);
    }

}
