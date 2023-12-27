package com.xpo.ltl.shipment.service.enums;


public enum ShmNotificationStatusCdEnum {

    APPOINTMENT_SET("1"),
    PENDING("2"),
    CANCELLED("3"),
    DELIVERY_COMPLETED("4"),
    RESCHEDULE_REQUIRED("5"),
    APPOINTMENT_NOT_REQUIRED("6"),
    RESCHEDULE("7"),
    REMOVED_FROM_APPOINTMENT("8");
    
    private String code;
    
    ShmNotificationStatusCdEnum(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }

    public static ShmNotificationStatusCdEnum fromValue(final String v) {
        for (final ShmNotificationStatusCdEnum c : ShmNotificationStatusCdEnum.values()) {
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
