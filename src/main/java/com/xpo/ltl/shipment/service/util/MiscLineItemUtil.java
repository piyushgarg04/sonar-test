package com.xpo.ltl.shipment.service.util;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.xpo.ltl.api.shipment.v2.MiscLineItemCd;
import com.xpo.ltl.api.shipment.v2.MiscLineItem;

public class MiscLineItemUtil {

    private MiscLineItemUtil() {}
    
    /**
     * Returns the COD (cash-on-delivery) amount expected; this is the amount from billing on the shipment, not the amount actually collected at time of delivery.
     */
    public static MiscLineItem getLineItemByType(MiscLineItemCd miscLineItemCd, List<MiscLineItem> miscLineItems) {
        
        if (CollectionUtils.isEmpty(miscLineItems)) {
            return null;
        }
        return miscLineItems.stream()
                .filter(lineItem -> null != lineItem.getLineTypeCd())
                .filter(lineItem -> lineItem.getLineTypeCd() == miscLineItemCd)
                .findFirst()
                .orElse(null);
    }
}
