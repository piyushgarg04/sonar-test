package com.xpo.ltl.shipment.service.enums;


public enum ShipmentAppointmentStatusEnum {

    APPOINTMENT_SET("1"),
    PENDING("2"),
    CANCELLED("3"),
    DELIVERY_COMPLETED("4"),
    RESCHEDULE_REQD("5"),
    APPOINTMENT_NOT_REQD("6"),
    RESCHEDULED("7"),
    REMOVED_FROM_APPOINTMENT("8");
    
    private String code;
    
    ShipmentAppointmentStatusEnum(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }

    public static ShipmentAppointmentStatusEnum fromValue(final String v) {
        for (final ShipmentAppointmentStatusEnum c : ShipmentAppointmentStatusEnum.values()) {
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
