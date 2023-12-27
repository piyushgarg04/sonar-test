package com.xpo.ltl.shipment.service.enums;

public enum BaseLogTypeEnum {

	BASE_LOG_41("41"), 
	BASE_LOG_42("42"), 
	BASE_LOG_44("44"), 
	BASE_LOG_30("30"),
    BASE_LOG_3A("3A"),
    BASE_LOG_38("38"),
    BASE_LOG_70("70");

	private String code;

	BaseLogTypeEnum(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public static BaseLogTypeEnum fromValue(final String v) {
		for (final BaseLogTypeEnum c : BaseLogTypeEnum.values()) {
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
