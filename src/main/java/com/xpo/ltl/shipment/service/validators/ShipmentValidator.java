package com.xpo.ltl.shipment.service.validators;

import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.shipment.v2.ShipmentId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public abstract class ShipmentValidator extends Validator
{
    private static final String REQUEST_SHIPMENT_INST_ID_LIST = "request.shipmentIds";
    private static final String REQUEST_SHIPMENT_INST_ID = "request.shipmentIds.shipmentInstId";
    private static final String REQUEST_PRO_NBR_TXT = "request.shipmentIds.proNumber";

    protected void checkShipmentIds(List<ShipmentId> shipmentIds, List<MoreInfo> moreInfos) {
        if (shipmentIds == null) {
            addMoreInfo(moreInfos, REQUEST_SHIPMENT_INST_ID_LIST, "ShipmentIds is null");
        }
        else {
            if (shipmentIds.isEmpty()) {
                addMoreInfo(moreInfos, REQUEST_SHIPMENT_INST_ID_LIST, "ShipmentIds is empty");
            }
            Set<String> proNbrs = new HashSet<>();
            Set<String> shpInstIds = new HashSet<>();
            for (ShipmentId shipmentId : shipmentIds) {
                String proNbr = shipmentId.getProNumber();
                if (proNbr != null) {
                    if (StringUtils.isBlank(proNbr))
                        addMoreInfo(moreInfos, REQUEST_PRO_NBR_TXT, "ProNumber is blank");

                    if (!proNbrs.add(proNbr))
                        addMoreInfo(moreInfos, REQUEST_PRO_NBR_TXT, "Duplicate ProNumber " + proNbr);
                }
                String shpInstId = shipmentId.getShipmentInstId();
                if (shpInstId != null) {
                    if (StringUtils.isBlank(shpInstId))
                        addMoreInfo(moreInfos, REQUEST_SHIPMENT_INST_ID, "ShipmentInstId is blank");

                    if (!NumberUtils.isParsable(shpInstId))
                        addMoreInfo(moreInfos, REQUEST_SHIPMENT_INST_ID, "Invalid ShipmentInstId " + shpInstId);

                    if (!shpInstIds.add(shpInstId))
                        addMoreInfo(moreInfos, REQUEST_SHIPMENT_INST_ID, "Duplicate ShipmentInstId " + shpInstId);
                }
            }
            if (CollectionUtils.isNotEmpty(proNbrs) && CollectionUtils.isNotEmpty(shpInstIds)) {
                addMoreInfo(moreInfos, REQUEST_SHIPMENT_INST_ID_LIST, "ProNumbers and ShipmentInstIds are mutually exclusive");
            }
        }
    }
}
