package com.xpo.ltl.shipment.service.util;

import java.awt.Color;

import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;

public enum PdfFontEnum
{
    MAX( FontFactory.getFont("Roboto", 14, Font.NORMAL, Color.BLACK)),
    MAX_BOLD( FontFactory.getFont("Roboto", 14, Font.BOLD, Color.BLACK)),
    HEADER( FontFactory.getFont("Roboto", 12, Font.NORMAL, Color.BLACK)),
    HEADER_BOLD( FontFactory.getFont("Roboto", 12, Font.BOLD, Color.BLACK)),
    TITLE( FontFactory.getFont("Roboto", 11, Font.NORMAL, Color.BLACK)),
    TITLE_WHITE( FontFactory.getFont("Roboto", 11, Font.NORMAL, Color.WHITE)),
    TITLE_BOLD( FontFactory.getFont("Roboto", 11, Font.BOLD, Color.BLACK)),
    HEADING_BOLD( FontFactory.getFont("Roboto", 9, Font.BOLD, Color.BLACK)),
    HEADING( FontFactory.getFont("Roboto", 9, Font.NORMAL, Color.BLACK)),
    NORMAL( FontFactory.getFont("Roboto", 8, Font.NORMAL, Color.BLACK)),
    NORMAL_BOLD( FontFactory.getFont("Roboto", 8, Font.BOLD, Color.BLACK)),
    SMALL_GRAY( FontFactory.getFont("Roboto", 6, Font.NORMAL, Color.GRAY)),
    SMALL( FontFactory.getFont("Roboto", 6, Font.NORMAL, Color.BLACK)),
    SMALL_BOLD( FontFactory.getFont("Roboto", 6, Font.BOLD, Color.BLACK)),
    EDIT_LINE( FontFactory.getFont("Roboto", 10, Font.BOLD, Color.LIGHT_GRAY));

    private final Font theFont;

    private PdfFontEnum(final Font font) {
	this.theFont = font;
    }

    public Font getFont() {
	return theFont;
    }

}