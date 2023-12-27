package com.xpo.ltl.shipment.service.enums;


public enum NotificationEventCdEnum {

    RESCHEDULED("R", "Rescheduled"),
    SET("S", "Set"),
    CANCELLED("C", "Cancelled"),
    RECORDED("E", "Recorded"),
    UPDATED("U", "Updated"),
    CUSTOMER_CALLED("L", "Customer was called"),
    SYSTEM_CONVERSION("O", "System Conversion"),
    PENDING("P", "Pending");
    
    private String code;
    private String desc;
    
    NotificationEventCdEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static NotificationEventCdEnum fromValue(final String v) {
        for (final NotificationEventCdEnum c : NotificationEventCdEnum.values()) {
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
