package com.xpo.ltl.shipment.service.enums;


public enum ShmNotificationCatgCdEnum {

    APPOINTMENT("A"),
    NOTIFICATION("N");
    
    private String code;
    
    ShmNotificationCatgCdEnum(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }

    public static ShmNotificationCatgCdEnum fromValue(final String v) {
        for (final ShmNotificationCatgCdEnum c : ShmNotificationCatgCdEnum.values()) {
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
