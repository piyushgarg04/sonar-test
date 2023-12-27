package com.xpo.ltl.shipment.service.impl;

import java.awt.Color;
import java.math.BigDecimal;
import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.StringUtils;

import com.lowagie.text.Anchor;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfCell;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.draw.DottedLineSeparator;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.xpo.ltl.api.invoice.v1.InvoiceDetailData;
import com.xpo.ltl.api.invoice.v1.InvoiceShipmentParty;
import com.xpo.ltl.api.shipment.v2.Commodity;
import com.xpo.ltl.shipment.service.util.PdfFontEnum;

	public abstract class PdfCopyBillUtils {

		static float TITLE_CELL_HEIGHT = 20f;
		static float BLANK_LINE_HEIGHT = 10f;
		
		static PdfPCell getCell(final String value, final int alignment,
				final Font font) {
			final PdfPCell cell = new PdfPCell();
			cell.setUseAscender(true);
			cell.setUseDescender(true);
			cell.setBorder(Rectangle.NO_BORDER);
			final Paragraph p = new Paragraph(value, font);
			p.setAlignment(alignment);
			cell.addElement(p);
			return cell;
		}

		static PdfPCell getCellWithPadding(final String value, final Font font,
				final float padding) {
			final PdfPCell cell = new PdfPCell();
			cell.setUseAscender(true);
			cell.setUseDescender(true);
			cell.setBorder(Rectangle.NO_BORDER);
			final Paragraph p = new Paragraph(value, font);
			p.setIndentationRight(padding);
			cell.addElement(p);
			return cell;
		}

		static PdfPCell getSpannedCell(final String value, final int alignment,
				final Font font, final int colspan) {
			final PdfPCell cell = new PdfPCell();
			cell.setUseAscender(true);
			cell.setUseDescender(true);
			cell.setBorder(Rectangle.NO_BORDER);
			final Paragraph p = new Paragraph(value, font);
			p.setAlignment(alignment);
			cell.addElement(p);
			cell.setColspan(colspan);
			return cell;
		}

		static PdfPCell getDashedLineCell() {
			final PdfPCell cell = new PdfPCell();
			cell.setUseAscender(true);
			cell.setUseDescender(true);
			cell.setBorder(Rectangle.NO_BORDER);
			final DottedLineSeparator dashedLine = new DottedLineSeparator();
		//	dashedLine.setDash(10);
			dashedLine.setGap(7);
			dashedLine.setLineWidth(1);
			final Paragraph p = new Paragraph();
			p.add(dashedLine);
			cell.addElement(p);
			return cell;
		}

		static PdfPCell getSolidLineCell(final int alignment) {
			final PdfPCell cell = new PdfPCell();
			cell.setUseAscender(true);
			cell.setUseDescender(true);
			cell.setBorder(Rectangle.NO_BORDER);
			final LineSeparator line = new LineSeparator();
			line.setLineWidth(1);
			line.setLineColor(Color.LIGHT_GRAY);
			line.setAlignment(alignment);
			final Paragraph p = new Paragraph();
			p.add(line);
			cell.addElement(p);
			return cell;
		}

		static PdfPCell getGrayUnderlineCell(final String value) {

			final PdfPCell cell = new PdfPCell();
			cell.setUseAscender(true);
			cell.setUseDescender(true);
			cell.setBorder(Rectangle.NO_BORDER);
			final Paragraph p = new Paragraph(value,PdfFontEnum.NORMAL.getFont());
			p.setAlignment(Element.ALIGN_LEFT);
			cell.addElement(p);
			return cell;
		}

		static PdfPCell getTitleCell(final String value) {
			return PdfCopyBillUtils.getTitleCell(value, Element.ALIGN_LEFT,
					PdfCopyBillUtils.TITLE_CELL_HEIGHT);
		}

		static PdfPCell getTitleCell(final String value, final int alignment) {
			return PdfCopyBillUtils.getTitleCell(value, alignment,
					PdfCopyBillUtils.TITLE_CELL_HEIGHT);
		}

		static PdfPCell getTitleCell(final String value, final int alignment,
				final float cellHeight) {

			final PdfPCell cell = new PdfPCell();
			cell.setUseAscender(true);
			cell.setUseDescender(true);
			cell.setBorder(Rectangle.NO_BORDER);
			final Paragraph p = new Paragraph(value,PdfFontEnum.NORMAL.getFont());
			p.setAlignment(alignment);
			p.setLeading(0, 1.2f);
			cell.setMinimumHeight(cellHeight);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			cell.setBackgroundColor(CreateCopyBillPdf.LIGHT_LIGHT_GRAY);
			cell.addElement(p);
			return cell;
		}

		static PdfPCell getHeaderCell(final String value, final int alignment,
				final float cellHeight) {

			final PdfPCell cell = new PdfPCell();
			cell.setUseAscender(true);
			cell.setUseDescender(true);
			cell.setBorder(Rectangle.NO_BORDER);
			final Paragraph p = new Paragraph(value,PdfFontEnum.HEADING_BOLD.getFont());
			p.setAlignment(alignment);
			p.setLeading(0, 1.0f);
			cell.setMinimumHeight(cellHeight);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			cell.addElement(p);
			return cell;
		}

		static PdfPCell getBlankCell(final int colspan) {
			final PdfPCell cell = new PdfPCell(Phrase.getInstance(""));
			cell.setBorder(Rectangle.NO_BORDER);
			cell.setColspan(colspan);
			return cell;
		}

		static PdfPCell getBlankLine(final int colspan) {
			final PdfPCell cell = new PdfPCell(new Phrase("  "));
			cell.setBorder(Rectangle.NO_BORDER);
			cell.setColspan(colspan);
			return cell;
		}
		
		static PdfPCell getBlankLineWithHeight(final int colspan, final float cellHeight) {
			final PdfPCell cell = new PdfPCell(new Phrase("  "));
			cell.setMinimumHeight(cellHeight);
			cell.setFixedHeight(cellHeight);
			cell.setBorder(Rectangle.NO_BORDER);
			cell.setColspan(colspan);
			return cell;
		}

		static Anchor getAnchor(final String text, final String url, final Font font) {
			final Anchor anchor = new Anchor(text,font);
			anchor.setName(text);
			anchor.setReference(url);
			return anchor;
		}

		static String formatPro(final String proNumber) {

			if (StringUtils.isEmpty(proNumber) || (proNumber.trim().length() < 9)) {
				return proNumber;
			}

			if (proNumber.trim().length() == 9) {
				return proNumber.trim().substring(0, 3) + '-'
						+ proNumber.trim().substring(3);
			}

			if (proNumber.trim().length() == 11 ||
			        proNumber.trim().length() == 12) {
				return proNumber.trim().substring(1, 4) + '-'
						+ proNumber.trim().substring(5);
			}
			
			return proNumber;

		}

		static String formatProNineDigit(final String proNumber) {

			if (StringUtils.isEmpty(proNumber) || (proNumber.trim().length() < 9)) {
				return proNumber;
			}

			if (proNumber.trim().length() == 9) {
				return proNumber.trim();
			}

			if (proNumber.trim().length() == 11) {
				return proNumber.trim().substring(1, 4)
						+ proNumber.trim().substring(5);
			}

			return proNumber.trim();

		}

		// Using the 16 digits (9 PRO + 7 Amount Due), do the following
		//
		// 1. Multiply Odd digits by 2 = Sum the digits of the product = Sum all
		// product digits
		// For example: First digit = 6 = 6 x 2 = 12 = 1 + 2 = 3 - use 3 to sum the
		// digits
		// 2. Even digits = Sum all Even digits
		// 3. Total of the #1 and #2.
		// 4. Add 10 to the Amount in #3.
		// 5. Get Remainder of Amount in #4 divided by 10.
		// 6. Subtract Remainder in #5 from Amount in #4 to get the Difference
		// 7. Check Digit is the difference between Amount #3 and result of #6.
		//
		static String formatCheckDigit(final String checkNumber) {

			int compareOne = 0;

			for (int i = 0; i < checkNumber.length(); i++) {
				final int number = Integer.parseInt(String.valueOf(checkNumber
						.charAt(i)));
				if (i % 2 == 1) {
					compareOne += number;
				} else {
					final int oddProduct = number * 2;
					compareOne += (oddProduct % 10) + oddProduct / 10;
				}
			}

			if ((compareOne % 10) == 0) {
				return "0";
			}

			final int outNumber = compareOne + 10;
			final int compareTwo = outNumber - (outNumber % 10);

			return String.valueOf(Math.abs(compareOne - compareTwo));
		}

		static String formatZip(final String postalCode, final String zip4) {

			if (StringUtils.isBlank(postalCode)) {
				return StringUtils.EMPTY;
			}

			if (StringUtils.isBlank(zip4)) {
				return postalCode.trim();
			}

			return postalCode.trim() + '-' + zip4.trim();
		}

		static String formatCityStateZip(final String city, final String state,
				final String postalCode) {

			if (StringUtils.isBlank(city) && StringUtils.isBlank(state)
					&& StringUtils.isBlank(postalCode)) {
				return StringUtils.EMPTY;
			}

			return StringUtils.trimToEmpty(city) + ", "
					+ StringUtils.trimToEmpty(state) + " "
					+ StringUtils.trimToEmpty(postalCode);
		}

		static boolean isRateM(final Double double1,
				final Boolean isMinimumCharge) {

			return (((isMinimumCharge != null) && (isMinimumCharge.booleanValue())) || ((double1 != null) && (double1
					.doubleValue() < 0)));
		}

		static String buildDescriptionLine(final Commodity item) {

			if (item == null) {
				return "";
			}

			final StringBuilder builder = new StringBuilder();

						
			if (StringUtils.isNotBlank(item.getDescription())) {
				builder.append(item.getDescription().trim());
				builder.append(" ");
			}

				if (StringUtils.isNotBlank(item.getNmfcItemCd())) {
				builder.append(" NMFC: " + item.getNmfcItemCd()) ;
		    }
			if (StringUtils.isNotBlank(item.getClassType().toString())) {
				builder.append(" CLASS: " + item.getClassType()) ;
		    }
			
			return builder.toString();
		}

		static Date convertToDate(final XMLGregorianCalendar aCalendar) {

			if (aCalendar == null) {
				return null;
			}

			return aCalendar.toGregorianCalendar().getTime();

		}

		static String formatInvoiceNumber(final String proNbr,
				BigDecimal invoiceAmount) {

			if (StringUtils.isEmpty(proNbr)) {
				return null;
			}

			if (invoiceAmount.signum() == -1){
				invoiceAmount = invoiceAmount.negate();
			}
			
			final String formattedAmount = java.lang.String.format("%07d",
					(int) (invoiceAmount.floatValue() * 100));

			final String checkDigit = formatCheckDigit(PdfCopyBillUtils.formatPro(
					proNbr).replace("-", "")
					+ formattedAmount);

			return PdfCopyBillUtils.formatPro(proNbr).replace("-", "") + ' '
					+ formattedAmount + ' ' + checkDigit;
		}

		static String formatDebtorCode(final String debtorCode) {

			if (debtorCode == null) {
				return "unkown";
			}

			if (debtorCode.trim().toUpperCase().startsWith("P")) {
				return "PREPAID";
			}

			if (debtorCode.trim().toUpperCase().startsWith("C")) {
				return "COLLECT";
			}

			if (debtorCode.trim().toUpperCase().startsWith("B")) {
				return "BOTH";
			}

			return debtorCode.trim();

		}

		static String formatCurrencyCode(final String currencyCode) {

			if (currencyCode == null) {
				return " ";
			}

			if (currencyCode.trim().toUpperCase().startsWith("U")) {
				return "US";
			}

			if (currencyCode.trim().toUpperCase().startsWith("C")) {
				return "CAD";
			}

			if (currencyCode.trim().toUpperCase().startsWith("M")) {
				return "MX";
			}

			return " ";

		}

		static InvoiceShipmentParty getRemitToPartyUSA() {

			final InvoiceShipmentParty aParty = new InvoiceShipmentParty();
			aParty.setAddressLine1("29559 Network Place");
			aParty.setName1("XPO Logistics Freight, Inc.");
			aParty.setCountryCd("US");
			aParty.setPostalCd("60673");
			aParty.setStateCd("IL");
			aParty.setZip4RestUs("1559");
			aParty.setCity("Chicago");
			return aParty;
		}

		// If Canada add Canada to last address line
		// ex: TORONTO, ON CANADA M5W OC9
		static InvoiceShipmentParty getRemitToPartyCAN() {

			final InvoiceShipmentParty aParty = new InvoiceShipmentParty();
			aParty.setName1("XPO Logistics Freight Canada, Inc.");
			aParty.setName2("c/o T10289C");
			aParty.setAddressLine1("PO Box 4918 Station A");
			aParty.setCountryCd("CA");
			aParty.setPostalCd("M5W 0C9");
			aParty.setStateCd("ON");
			aParty.setCity("Toronto");
			return aParty;
		}

		static InvoiceShipmentParty getHeaderSendTo(
				final InvoiceDetailData anInvoice) {

			if ((anInvoice.getBillTo() == null)
					|| (StringUtils.isBlank(anInvoice.getBillTo().getName1())
							&& StringUtils
									.isBlank(anInvoice.getBillTo().getName2())
							&& StringUtils.isBlank(anInvoice.getBillTo().getCity())
							&& StringUtils.isBlank(anInvoice.getBillTo()
									.getCountryCd()) && StringUtils
								.isBlank(anInvoice.getBillTo().getAddressLine1()))) {
				return anInvoice.getSendTo();
			}

			return anInvoice.getBillTo();
		}

		// Is a Canadian Invoice
		public static boolean isCanadianInvoice(final InvoiceDetailData anInvoice) {

			final InvoiceShipmentParty sendToParty = PdfCopyBillUtils
					.getHeaderSendTo(anInvoice);

			if (sendToParty == null
					|| (StringUtils.isBlank(sendToParty.getCountryCd()))) {
				return false;
			}

			return sendToParty.getCountryCd().startsWith("C");
		}

		public static String formatFileName(final String proNumber,
				final int counter) {
			return "/CopyBill-" + proNumber + '_' + System.currentTimeMillis()
					+ ".pdf";
		}

		public static String formatArchiveFileName(final String proNumber) {
			return "/CopyBill-" + proNumber + ".pdf";
		}

	}


