package com.xpo.ltl.shipment.service.util;

import java.math.BigInteger;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.xpo.ltl.api.shipment.v2.AsMatchedParty;
import com.xpo.ltl.api.shipment.v2.MatchedPartyTypeCd;
import com.xpo.ltl.java.util.cityoperations.NumberUtil;

public class AsMatchedPartyUtil {

    private AsMatchedPartyUtil() {}
    
    public static BigInteger getValidCustomerInstId(AsMatchedParty asMatchedParty) {
        
        BigInteger custInstId = BigInteger.ZERO;
        
        if (NumberUtil.isNonZero(asMatchedParty.getCisCustNbr())) {
            custInstId = asMatchedParty.getCisCustNbr();
        } else if (NumberUtil.isNonZero(asMatchedParty.getAlternateCustNbr())) {
            custInstId = asMatchedParty.getAlternateCustNbr();
        }
        return custInstId;
    }
    
    public static AsMatchedParty getConsigneeFromList(List<AsMatchedParty> asMatchedParties) {
        
        if (CollectionUtils.isEmpty(asMatchedParties)) {
            return null;
        } 
        return asMatchedParties.stream()
                .filter(party -> party.getTypeCd() == MatchedPartyTypeCd.CONS)
                .findFirst()
                .orElse(null);
    }
}
