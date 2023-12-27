package com.xpo.ltl.shipment.service.impl;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.GrayColor;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.xpo.ltl.api.invoice.v1.InvoiceDetailData;
import com.xpo.ltl.api.invoice.v1.InvoiceLine;
import com.xpo.ltl.api.invoice.v1.InvoiceShipmentParty;
import com.xpo.ltl.api.invoice.v1.ReferenceNumber;
import com.xpo.ltl.shipment.service.util.PdfFontEnum;

public class CreateCopyBillPdfImpl {
	
	/**
	 * Setup
	 */

	private static final Log log = LogFactory
			.getLog(CreateCopyBillPdfImpl.class);
	private static final String XPO_LOGISTICS_RESOURCE = "/xpo_logo_png.png";
	
	public static final int MARGIN_LEFT = 12;
	public static final int MARGIN_RIGHT = 12;
	public static final int MARGIN_TOP = 255;
	public static final int MARGIN_BOTTOM = 355;
	public static final int CANADA_MARGIN_TOP = 275;
	public static final int CANADA_MARGIN_BOTTOM = 370;

	private static final Rectangle PAGE_SIZE = PageSize.LETTER;

	/**
	 * Fonts in use thru out the PDF doc
	 */
	static Font ocrFont;
	static final String OCR_FONT = "/ocraext.ttf";

	/**
	 * Number Formats
	 */
	static final NumberFormat US_DOLLAR_FORMAT = NumberFormat
			.getCurrencyInstance(Locale.US);
	static final NumberFormat FLOAT_FORMAT = NumberFormat.getNumberInstance();
	static final NumberFormat INTEGER_FORMAT = NumberFormat.getNumberInstance();

	static final SimpleDateFormat dateFormatter = new SimpleDateFormat(
			"MM/dd/yyyy");

	static final Color LIGHT_LIGHT_GRAY = new GrayColor(.85f);

	static Map<String, Image> images;

	
	/**
	 * @Inject
	 	private AppContext appContext;
    */
	
	public byte[] createPdf(final InvoiceDetailData anInvoice) throws Exception {

		ByteArrayOutputStream outputStream = null;

		try {

			if (null == anInvoice) {
				throw new Exception(
						"PDF Create Exception - no Data ");
			}

			Document document = null;
			outputStream = new ByteArrayOutputStream();

			
				document = new Document(PAGE_SIZE, MARGIN_LEFT, MARGIN_RIGHT,
						MARGIN_TOP, MARGIN_BOTTOM);
		


			final PdfWriter writer = PdfWriter.getInstance(document,
					outputStream);


			final Rectangle page = document.getPageSize();
			final float width = page.getWidth() - document.leftMargin()
					- document.rightMargin();
			// add header and footer

			PdfPageEventHelper event = null;
			// event = new PdfInvoicePageEvent(anInvoice);
			

			writer.setPageEvent(event);

			document.open();

			final PdfPTable body = new PdfPTable(1);

			body.setTotalWidth(width);
			body.setLockedWidth(true);
			body.getDefaultCell().setBorder(Rectangle.NO_BORDER);

			body.setHeaderRows(2);
			body.setFooterRows(1);
			body.setSkipFirstHeader(true);
			body.setSkipLastFooter(true);

			body.addCell(PdfCopyBillUtils.getBlankLineWithHeight(1, 12f));

			body.addCell(PdfCopyBillUtils.getCell("( continued on next page )",
					Element.ALIGN_CENTER, PdfFontEnum.NORMAL.getFont()));

			body.addCell(addHeaderPartiesInfo(anInvoice));

			body.addCell(addBillDetail(anInvoice));

			body.addCell(PdfCopyBillUtils.getBlankLineWithHeight(1, 5f));

			

			if (StringUtils.isNotBlank(anInvoice.getShipperRemark())) {
				body.addCell(PdfCopyBillUtils.getCell(
						anInvoice.getShipperRemark(), Element.ALIGN_LEFT,
						PdfFontEnum.NORMAL.getFont()));
			}

			if (StringUtils.isNotBlank(anInvoice.getAuthRemark())) {
				body.addCell(PdfCopyBillUtils.getCell(anInvoice.getAuthRemark(),
						Element.ALIGN_LEFT, PdfFontEnum.NORMAL.getFont()));
			}

			final List<ReferenceNumber> otherSuppRefNbr = anInvoice.getOtherSuppRefNbr();
			if (CollectionUtils.isNotEmpty(otherSuppRefNbr))
			{
				final List<ReferenceNumber> refNumbers = addAdditionalReferenceNumbers(anInvoice);

				if ((refNumbers != null) && (refNumbers.size() > 0)) {

					body.addCell(
						PdfCopyBillUtils.getSpannedCell(
							"Additional Customer Reference Numbers:\",",
							Element.ALIGN_LEFT,
							PdfFontEnum.NORMAL.getFont(),
							2));

					for (final ReferenceNumber referenceNumber : refNumbers) {
						body.addCell(
							PdfCopyBillUtils.getCell(
								referenceNumber.getReferenceTypeCd() + "  " + referenceNumber.getReference(),
								Element.ALIGN_LEFT,
								PdfFontEnum.NORMAL.getFont()));
					}
				}
			}

			document.add(body);
			document.close();
            return outputStream.toByteArray();

		} catch (final Exception exe) {
			log.error("Create Exception : " + anInvoice.getProNbr(),
					exe);
			throw exe;
		} finally {
			IOUtils.closeQuietly(outputStream);
		}

	}

/**
 * Bill detail Commodity Lines and Totals with headings
 *
 * @param details
 * @return
 * @throws DocumentException
 */
private PdfPTable addBillDetail(final InvoiceDetailData invoice)
		throws DocumentException {

	final PdfPTable table = new PdfPTable(1);
	table.setWidthPercentage(100);
	table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

	// line items header
	final PdfPTable tableRows = new PdfPTable(6);
	tableRows.getDefaultCell().setBorder(Rectangle.NO_BORDER);
	tableRows.setWidthPercentage(100);
	tableRows.setWidths(new float[] { 10, 45, 10, 10, 15, 10 });
	tableRows.setHeaderRows(3);
	tableRows.setFooterRows(1);
	tableRows.setSkipLastFooter(true);

	if (PdfCopyBillUtils.isCanadianInvoice(invoice)) {

		tableRows.addCell(PdfCopyBillUtils.getTitleCell("PCS\nPIÉCES",
				Element.ALIGN_LEFT, 20f));
		tableRows
				.addCell(PdfCopyBillUtils
						.getTitleCell(
								"DESCRIPTION OF ARTICLES AND MARKS",
								Element.ALIGN_LEFT));
		tableRows.addCell(PdfCopyBillUtils.getTitleCell(
				"WEIGHT (lbs)", Element.ALIGN_RIGHT));
		tableRows.addCell(PdfCopyBillUtils.getTitleCell("RATE\nTARIF",
				Element.ALIGN_RIGHT));
		tableRows.addCell(PdfCopyBillUtils.getTitleCell("CHARGES",
				Element.ALIGN_RIGHT));

		tableRows.addCell(PdfCopyBillUtils.getTitleCell("",
				Element.ALIGN_RIGHT));

	} else {

		tableRows.addCell(PdfCopyBillUtils.getTitleCell("PCS"));
		tableRows.addCell(PdfCopyBillUtils
				.getTitleCell("DESCRIPTION OF ARTICLES AND MARKS"));
		tableRows.addCell(PdfCopyBillUtils.getTitleCell("WEIGHT (lbs)",
				Element.ALIGN_RIGHT));
		tableRows.addCell(PdfCopyBillUtils.getTitleCell("RATE",
				Element.ALIGN_RIGHT));
		tableRows.addCell(PdfCopyBillUtils.getTitleCell("CHARGES",
				Element.ALIGN_RIGHT));
		tableRows.addCell(PdfCopyBillUtils.getTitleCell(""));
	}

	tableRows.addCell(PdfCopyBillUtils.getBlankLineWithHeight(6, 5f));
	tableRows.addCell(PdfCopyBillUtils.getBlankLineWithHeight(6, 5f));

	int totalPieces = 0;

	final List<InvoiceLine> details = invoice.getInvoiceLines();
	boolean isAmountNegative = false;
	String lineDescription = "";

	for (final InvoiceLine item : details) {

		
//		lineDescription = PdfCopyBillUtils.buildDescriptionLine(item);

		if ((item.getPieces() != null)
				&& (item.getPieces().intValue() != 0)) {
			tableRows
					.addCell(PdfCopyBillUtils.getCell(
							INTEGER_FORMAT.format(item.getPieces()),
							Element.ALIGN_LEFT,
						PdfFontEnum.NORMAL.getFont()));
			totalPieces += item.getPieces().intValue();
		} else {
			tableRows.addCell(PdfCopyBillUtils.getBlankCell(1));
		}

		if (StringUtils.isNotBlank(lineDescription)) {
			tableRows
					.addCell(PdfCopyBillUtils.getCell(lineDescription,
							Element.ALIGN_LEFT,
							PdfFontEnum.NORMAL.getFont()));
		} else {
			tableRows.addCell(PdfCopyBillUtils.getBlankCell(1));
		}

		if ((item.getWeight() != null)
				&& (item.getWeight().intValue() != 0f)) {
			tableRows.addCell(PdfCopyBillUtils.getCell(
					INTEGER_FORMAT.format(item.getWeight()),
					Element.ALIGN_RIGHT,
					PdfFontEnum.NORMAL.getFont()));
		} else {
			tableRows.addCell(PdfCopyBillUtils.getBlankCell(1));
		}

	

		if ( (item.getAmount().floatValue() != 0f)) {
			tableRows.addCell(PdfCopyBillUtils.getCell(
					US_DOLLAR_FORMAT
							.format(isAmountNegative ? item.getAmount()
									.negate() : item.getAmount()),
					Element.ALIGN_RIGHT, PdfFontEnum.NORMAL
							.getFont()));
		} else {
			tableRows.addCell(PdfCopyBillUtils.getBlankCell(1));
		}

		tableRows.addCell(PdfCopyBillUtils.getBlankCell(1));
	}

	tableRows.addCell(PdfCopyBillUtils.getBlankLineWithHeight(6, 5f));

	// Add total Line
	tableRows.addCell(PdfCopyBillUtils.getCell(
			INTEGER_FORMAT.format(totalPieces), Element.ALIGN_LEFT,
			PdfFontEnum.NORMAL.getFont()));
	tableRows.addCell(PdfCopyBillUtils.getCell("TOTAL", Element.ALIGN_LEFT,
			PdfFontEnum.NORMAL.getFont()));
	tableRows.addCell(PdfCopyBillUtils.getCell(
			INTEGER_FORMAT.format(invoice.getTotalWeight()),
			Element.ALIGN_RIGHT, PdfFontEnum.NORMAL.getFont()));
	tableRows.addCell(PdfCopyBillUtils.getSpannedCell(
			PdfCopyBillUtils.formatCurrencyCode(invoice.getInvoiceAmount()
					.getCurrencyCd())
					+ " "
					+ US_DOLLAR_FORMAT.format(invoice
							.getInvoiceAmount().getAmt()),
			Element.ALIGN_RIGHT, PdfFontEnum.NORMAL.getFont(), 2));

	final PdfPCell debtorCell = PdfCopyBillUtils.getCell(
			PdfCopyBillUtils.formatDebtorCode(invoice.getDebtorType()),
			Element.ALIGN_LEFT, PdfFontEnum.NORMAL.getFont());
	debtorCell.setPaddingLeft(2f);

	tableRows.addCell(debtorCell);

	
	final PdfPCell cell = new PdfPCell();
	cell.setBorder(Rectangle.BOX);
	cell.setUseAscender(true);
	cell.setUseDescender(true);
	cell.addElement(tableRows);
	cell.setBorderColor(Color.LIGHT_GRAY);
	table.addCell(cell);

	return table;
}

private List<ReferenceNumber> addAdditionalReferenceNumbers(
		final InvoiceDetailData anInvoice) throws Exception
{
	// find unique reference numbers
	final List<ReferenceNumber> theNumbers = new ArrayList<ReferenceNumber>();
	final List<ReferenceNumber> otherSuppRefNbr = anInvoice.getOtherSuppRefNbr();
	if (CollectionUtils.isNotEmpty(otherSuppRefNbr))
	{
		for (final ReferenceNumber referenceNumber : otherSuppRefNbr)
		{
			final String refTypeCd = referenceNumber.getReferenceTypeCd();
			if (StringUtils.isNotBlank(refTypeCd))
			{
				final String refNbr = referenceNumber.getReference();
				if (StringUtils.equals(refTypeCd, "PO#") &&
					StringUtils.equals(refNbr, anInvoice.getRefNbrPO())) {
					continue;
				}

				if (StringUtils.equals(refTypeCd, "SN#") &&
					StringUtils.equals(refNbr, anInvoice.getRefNbrSN())) {
					continue;
				}
				
			}

			theNumbers.add(referenceNumber);
		}
	}

	return theNumbers;
}



private PdfPTable addHeaderPartiesInfo(final InvoiceDetailData anInvoice) throws Exception {

	final PdfPTable table1 = new PdfPTable(1);
	table1.setWidthPercentage(100);

	final PdfPTable table = new PdfPTable(4);
	table.setWidthPercentage(100);
	table.getDefaultCell().setBorder(Rectangle.NO_BORDER);
	table.setWidths(new float[] { 25, 25, 25, 25 });
	table.addCell(PdfCopyBillUtils.getTitleCell("SHIPPER"));
	table.addCell(PdfCopyBillUtils.getTitleCell("CONSIGNEE"));
	table.addCell(PdfCopyBillUtils.getTitleCell("REFERENCE NUMBERS"));
	table.addCell(PdfCopyBillUtils.getTitleCell("SHIPMENT DATE"));

	table.addCell(PdfCopyBillUtils.getBlankCell(4));

	table.addCell(getHeaderPartyTable(anInvoice.getShipper()));
	table.addCell(getHeaderPartyTable(anInvoice.getConsignee()));
	table.addCell(getHeaderReferenceNumbers(anInvoice));
	table.addCell(PdfCopyBillUtils.getCell(
		CreateCopyBillPdfImpl.dateFormatter.format(PdfCopyBillUtils.convertToDate(anInvoice.getPickupDate())),
		Element.ALIGN_LEFT, PdfFontEnum.NORMAL.getFont()));

	final PdfPCell cell = new PdfPCell();

	cell.setUseAscender(true);
	cell.setUseDescender(true);
	cell.setBorder(Rectangle.BOX);
	cell.setBorderColor(Color.LIGHT_GRAY);
	cell.addElement(table);
	table1.addCell(cell);

	return table1;

    }

    private PdfPTable getHeaderPartyTable(final InvoiceShipmentParty aParty) {

	final PdfPTable table = new PdfPTable(1);
	table.setWidthPercentage(100);
	table.getDefaultCell().setBorder(Rectangle.NO_BORDER);
	table.addCell(PdfCopyBillUtils.getCell(aParty.getName1(), Element.ALIGN_LEFT,
		PdfFontEnum.NORMAL.getFont()));
	table.addCell(PdfCopyBillUtils.getCell(aParty.getName2(), Element.ALIGN_LEFT,
		PdfFontEnum.NORMAL.getFont()));
	table.addCell(PdfCopyBillUtils.getCell(aParty.getAddressLine1(), Element.ALIGN_LEFT,
		PdfFontEnum.NORMAL.getFont()));
	table.addCell(PdfCopyBillUtils.getCell(
		aParty.getCity() + ", " + aParty.getStateCd() + ", "
			+ PdfCopyBillUtils.formatZip(aParty.getPostalCd(), aParty.getZip4RestUs()), Element.ALIGN_LEFT,
		PdfFontEnum.NORMAL.getFont()));
	table.addCell(PdfCopyBillUtils.getCell(aParty.getCountryCd(), Element.ALIGN_LEFT,
		PdfFontEnum.NORMAL.getFont()));

	return table;
    }
  
    private PdfPTable getHeaderReferenceNumbers(final InvoiceDetailData copyBill) {
    
   	final PdfPTable table = new PdfPTable(1);
    	table.setWidthPercentage(100);
    	table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

    	if (StringUtils.isNotBlank(copyBill.getRefNbrSN())) {
    	    table.addCell(PdfCopyBillUtils.getCell("SN# " + copyBill.getRefNbrSN(), Element.ALIGN_LEFT,
    		    PdfFontEnum.NORMAL.getFont()));
    	}

    	if (StringUtils.isNotBlank(copyBill.getRefNbrPO())) {
    	    table.addCell(PdfCopyBillUtils.getCell("PO# " + copyBill.getRefNbrPO(), Element.ALIGN_LEFT,
    		    PdfFontEnum.NORMAL.getFont()));
    	}

    	return table;
        }
}

