package com.xpo.ltl.shipment.service.enums;

/**
 * Describes the SuppRef Number types.-
 *
 * @author skapcitzky
 *
 */
public enum SuppRefNbrTypeEnum {
	SN("SN", "SN#"),
	SN_NUMBER("SN#", "SN#"),
	GBL("GBL", "GBL"),
	PO("PO", "PO#"),
	PO_NUMBER("PO#", "PO#"),
	MH("MH", "J#"),
	CB("CB", "BOL"),
	K6("K6", "JN"),
	L1("L1", "ED"),
	QQ("QQ", "QTY"),
	RN("RN", "VER"),
	WF("WF", "APT#"),
	OTHER("OTH", "OTHER");

	private final String name;
	private final String formatValue;

	private SuppRefNbrTypeEnum(final String name, final String formatValue) {
		this.name = name;
		this.formatValue = formatValue;
	}

	public String getName() {
		return name;
	}

	public String getFormatValue() {
		return formatValue;
	}
}
