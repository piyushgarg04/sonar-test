package com.xpo.ltl.shipment.service.util;

import static com.xpo.ltl.api.shipment.v2.ShipmentDetailCd.SHIPMENT_ONLY;

import java.util.Collection;

import org.apache.commons.collections4.CollectionUtils;

import com.xpo.ltl.api.shipment.v2.ShipmentDetailCd;

public class ShipmentDetailCdUtil {

    private ShipmentDetailCdUtil() {
    }

    public static boolean contains(Collection<ShipmentDetailCd> requestedDetailCds,
                                   boolean defaultWhenEmpty,
                                   ShipmentDetailCd... matchingDetailCds) {
        if (matchingDetailCds == null)
            return false;

        if (CollectionUtils.isEmpty(requestedDetailCds))
            return defaultWhenEmpty;

        for (ShipmentDetailCd matchingDetailCd : matchingDetailCds)
            if (requestedDetailCds.contains(matchingDetailCd))
                return true;

        return false;
    }

    public static boolean containsShipmentOnlyOrOthers(Collection<ShipmentDetailCd> shipmentDetailCds,
                                                       boolean defaultWhenEmpty) {
        if (contains(shipmentDetailCds, defaultWhenEmpty, SHIPMENT_ONLY))
            return true;

        if (CollectionUtils.isNotEmpty(shipmentDetailCds)
            && !shipmentDetailCds.contains(SHIPMENT_ONLY))
            return true;

        return false;
    }

}