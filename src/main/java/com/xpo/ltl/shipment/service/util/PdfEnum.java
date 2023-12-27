package com.xpo.ltl.shipment.service.util;

import java.awt.Color;

import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;

public enum PdfEnum
{
	ADDITIONAL_REFERENCE_NUMBERS("Additional Customer Reference Numbers:"),
	CNWY_IMAGE("CNWY"),
	XPO_LOGISTICS_IMAGE("XPO_LOGISTICS"),

	OCR_FONT("OCR");

	private final String name;

	private PdfEnum(final String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

}