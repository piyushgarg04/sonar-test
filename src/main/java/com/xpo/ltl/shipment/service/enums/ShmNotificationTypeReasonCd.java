package com.xpo.ltl.shipment.service.enums;

/**
 * Models values for SHM_NOTIFICATION.TYP_CD which indicates the reason the notification was necessary.  This type code is actually areason code and is <U>not</U> the same as the 
 * SCO_NOTIFICATION.NTFICTN_TYP_CD.
 */
public enum ShmNotificationTypeReasonCd {

    DRIVER_COLLECT("1"),
    NOA_OR_DNC("2"),
    CONSTRUCTION_SITE("3"),
    RESIDENTIAL("4"),
    OTHER("5");
    
    private String code;
    
    ShmNotificationTypeReasonCd(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }

    public static ShmNotificationTypeReasonCd fromValue(final String v) {
        for (final ShmNotificationTypeReasonCd c : ShmNotificationTypeReasonCd.values()) {
            if (c.code.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

    @Override
    public String toString() {
        return code;
    }
}
