package com.xpo.ltl.shipment.service.enums;

public enum MovementTypeEnum {

    UNLOAD("4"),
    OUT_FOR_DELIVERY("6"),
    TCON("3"),
    DELIVERY("7");

    private String code;

    MovementTypeEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

}
