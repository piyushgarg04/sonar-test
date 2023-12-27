package com.xpo.ltl.shipment.service.impl;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

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
import com.lowagie.text.pdf.PdfWriter;
import com.xpo.ltl.api.invoice.v1.InvoiceShipmentParty;
import com.xpo.ltl.api.invoice.v1.ReferenceNumber;
import com.xpo.ltl.api.shipment.v2.Commodity;
import com.xpo.ltl.api.shipment.v2.Remark;
import com.xpo.ltl.api.shipment.v2.SuppRefNbr;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.util.CopyBillPdfHelper;
import com.xpo.ltl.shipment.service.util.PdfEnum;
import com.xpo.ltl.shipment.service.util.PdfFontEnum;


public class CreateCopyBillPdf{

	/**
	 * Setup
	 */

	private static final Log log = LogFactory
			.getLog(CreateCopyBillPdf.class);
			
	private static final String XPO_LOGISTICS_RESOURCE = "/xpo_logo_png.png";

	public static final int MARGIN_LEFT = 12;
	public static final int MARGIN_RIGHT = 12;
	public static final int MARGIN_TOP = 25;
	public static final int MARGIN_BOTTOM = 35;


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


	 @Inject
	 	private AppContext appContext;

	 @PostConstruct
		public void loadImagesAndFonts() {

			FLOAT_FORMAT.setMinimumFractionDigits(2);
			FLOAT_FORMAT.setMaximumFractionDigits(3);
			INTEGER_FORMAT.setParseIntegerOnly(true);

		images = new HashMap<String, Image>();
		try {
				images.put(
						PdfEnum.XPO_LOGISTICS_IMAGE.getName(),
						Image.getInstance(this.getClass().getResource("/images" + CreateCopyBillPdf.XPO_LOGISTICS_RESOURCE)));
			} catch (final Exception exe) {
				log.fatal("Problems loading images", exe);
			}
		}

	 private static PdfPTable addImage() throws Exception {

			final PdfPCell text = new PdfPCell();
			text.setBorder(Rectangle.NO_BORDER);

			final float[] widths = { 20, 80 };
			final PdfPTable table = new PdfPTable(2);
			table.setWidths(widths);
			table.setWidthPercentage(100);
			final Image anImage = CreateCopyBillPdf.images
					.get(PdfEnum.XPO_LOGISTICS_IMAGE.getName());
			table.getDefaultCell().setBorder(Rectangle.NO_BORDER);
			table.addCell(anImage);
			table.addCell(PdfCopyBillUtils.getBlankCell(1));
			return table;
		}

		public byte[] createPdf(final CopyBillPdfHelper copyBillPdf) throws Exception {
			ByteArrayOutputStream outputStream = null;

			try {

				if (null == copyBillPdf) {
					throw new Exception(
							"PDF Create Exception - no Data ");
				}

				Document document = null;
				outputStream = new ByteArrayOutputStream();
				document = new Document(PAGE_SIZE, MARGIN_LEFT, MARGIN_RIGHT,
							MARGIN_TOP, MARGIN_BOTTOM);

				log.info("creating pdf-2");

				final PdfWriter writer = PdfWriter.getInstance(document, outputStream);


				final Rectangle page = document.getPageSize();
				final float width = page.getWidth() - document.leftMargin()
						- document.rightMargin();

				/* add header and footer
				*/

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
			    body.addCell(addImage());

				body.addCell(addHeaderTitle(copyBillPdf));
				body.addCell(PdfCopyBillUtils.getBlankLineWithHeight(1, 12f));
				body.addCell(PdfCopyBillUtils.getBlankLineWithHeight(1, 12f));
				
				body.addCell(addHeaderLine2(copyBillPdf));
				body.addCell(PdfCopyBillUtils.getBlankLineWithHeight(1, 12f));
				
				body.addCell(addHeaderPartiesInfo(copyBillPdf));

				body.addCell(addBillDetail(copyBillPdf));
				body.addCell(PdfCopyBillUtils.getBlankLineWithHeight(1, 5f));

				if (StringUtils.isNotBlank(copyBillPdf.getShipperRemark())) {
					body.addCell(PdfCopyBillUtils.getCell(
							(String) copyBillPdf.getShipperRemark(), Element.ALIGN_LEFT,
							PdfFontEnum.NORMAL.getFont()));
				}


				final List<SuppRefNbr> refNumbers = copyBillPdf.getSuppRefNbrs();

				if (CollectionUtils.isNotEmpty(refNumbers))

				{
					//final List<ReferenceNumber> refNumbers = addAdditionalReferenceNumbers(copyBillPdf);

					if ((refNumbers != null) && (refNumbers.size() > 0)) {

						body.addCell(
							PdfCopyBillUtils.getSpannedCell(
								"Reference Numbers: ",
								Element.ALIGN_LEFT,
								PdfFontEnum.HEADING_BOLD.getFont(),
								2));

						for (final SuppRefNbr referenceNumber : refNumbers) {
							body.addCell(
								PdfCopyBillUtils.getCell(
									referenceNumber.getTypeCd() + "  " + referenceNumber.getRefNbr(),
									Element.ALIGN_LEFT,
									PdfFontEnum.NORMAL.getFont()));
						}
					}

					body.addCell(PdfCopyBillUtils.getBlankLineWithHeight(1, 5f));
				}

				final List<Remark> remarks = copyBillPdf.getRemarks();

				if (CollectionUtils.isNotEmpty(remarks))

				{
					if ((remarks != null) && remarks.size() > 0) {

						body.addCell(
							PdfCopyBillUtils.getSpannedCell(
								"Remarks: ",
								Element.ALIGN_LEFT,
								PdfFontEnum.HEADING_BOLD.getFont(),
								2));

						for (final Remark shpremarks : remarks) {
							body.addCell(
								PdfCopyBillUtils.getCell(
									shpremarks.getRemark(),
									Element.ALIGN_LEFT,
									PdfFontEnum.NORMAL.getFont()));
						}
					}
				}

				document.add(body);
				document.close();
	            return outputStream.toByteArray();

			} catch (final Exception exe) {
				log.error("Create Exception : " + copyBillPdf.getProNbr(),
						exe);
				throw exe;
			} finally {
				IOUtils.closeQuietly(outputStream);
			}

		}

/**
 * Bill detail Commodity Lines and Totals with headings
 *
 * @param copyBillPdf
 * @return
 * @throws DocumentException
 */
private PdfPTable addBillDetail(final CopyBillPdfHelper copyBillPdf)
		throws DocumentException {

	final PdfPTable table = new PdfPTable(1);
	table.setWidthPercentage(100);
	table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

	// line items header
	final PdfPTable tableRows = new PdfPTable(7);
	tableRows.getDefaultCell().setBorder(Rectangle.NO_BORDER);
	tableRows.setWidthPercentage(100);
	tableRows.setWidths(new float[] { 8, 5, 10, 45, 10, 10, 12 });

	tableRows.addCell(PdfCopyBillUtils.getTitleCell("PCS"));
	tableRows.addCell(PdfCopyBillUtils.getTitleCell("HZ"));
	tableRows.addCell(PdfCopyBillUtils.getTitleCell("PKG"));
	tableRows.addCell(PdfCopyBillUtils
				.getTitleCell("DESCRIPTION OF ARTICLES AND MARKS"));
	tableRows.addCell(PdfCopyBillUtils.getTitleCell("WEIGHT (lbs)",
				Element.ALIGN_RIGHT));
	tableRows.addCell(PdfCopyBillUtils.getTitleCell("RATE",
				Element.ALIGN_RIGHT));
	tableRows.addCell(PdfCopyBillUtils.getTitleCell("CHARGES",
				Element.ALIGN_RIGHT));

	tableRows.addCell(PdfCopyBillUtils.getBlankLineWithHeight(6, 5f));
	tableRows.addCell(PdfCopyBillUtils.getBlankLineWithHeight(6, 5f));

	int totalPieces = 0;

	final List<Commodity> details = copyBillPdf.getCommodities();
	boolean isAmountNegative = false;
	String lineDescription = "";

	for (final Commodity item : details) {


		lineDescription = PdfCopyBillUtils.buildDescriptionLine(item);

		if ((item.getPiecesCount() != null)
				&& (item.getPiecesCount().intValue() != 0)) {
			tableRows
					.addCell(PdfCopyBillUtils.getCell(
							INTEGER_FORMAT.format(item.getPiecesCount()),
							Element.ALIGN_LEFT,
						PdfFontEnum.NORMAL.getFont()));
			totalPieces += item.getPiecesCount().intValue();
		} else {
			tableRows.addCell(PdfCopyBillUtils.getBlankCell(1));
		}
		
		    tableRows.addCell(PdfCopyBillUtils.getBlankCell(1));
		 {
			tableRows
                    .addCell(PdfCopyBillUtils.getCell(item.getPackageCd() != null ? item.getPackageCd().toString() : "",
							Element.ALIGN_LEFT,
							PdfFontEnum.NORMAL.getFont()));
		}
		if (StringUtils.isNotBlank(lineDescription)) {
			tableRows
					.addCell(PdfCopyBillUtils.getCell(lineDescription,
							Element.ALIGN_LEFT,
							PdfFontEnum.NORMAL.getFont()));
		} else {
			tableRows.addCell(PdfCopyBillUtils.getBlankCell(1));
		}

		if ((item.getWeightLbs() != null)
				&& (item.getWeightLbs().intValue() != 0f)) {
			tableRows.addCell(PdfCopyBillUtils.getCell(
					INTEGER_FORMAT.format(item.getWeightLbs()),
					Element.ALIGN_RIGHT,
					PdfFontEnum.NORMAL.getFont()));
		} else {
			tableRows.addCell(PdfCopyBillUtils.getBlankCell(1));
		}

		if ((item.getTariffsRate() != null)
				&& (item.getTariffsRate().floatValue() != 0f)) {
			tableRows.addCell(PdfCopyBillUtils.getCell(
					FLOAT_FORMAT.format(item.getTariffsRate()),
					Element.ALIGN_RIGHT,
					PdfFontEnum.NORMAL.getFont()));
		} else {
			tableRows.addCell(PdfCopyBillUtils.getBlankCell(1));
		}

		if ((item.getAmount() != null)
				&& (item.getAmount().floatValue() != 0f)) {
			tableRows.addCell(PdfCopyBillUtils.getCell(
					US_DOLLAR_FORMAT
							.format(item.getAmount()),
					Element.ALIGN_RIGHT, PdfFontEnum.NORMAL
							.getFont()));
		} else {
			tableRows.addCell(PdfCopyBillUtils.getBlankCell(1));
		}
	}

	tableRows.addCell(PdfCopyBillUtils.getBlankCell(3));
	if (StringUtils.isNotBlank(copyBillPdf.getParentProNbr())) {
		tableRows.addCell(PdfCopyBillUtils.getCell("**TO MOVE FRT SHORT ON THIS MOVEMENT PRO " + PdfCopyBillUtils.formatPro(copyBillPdf.getProNbr()), Element.ALIGN_LEFT,
				PdfFontEnum.NORMAL.getFont()));
		tableRows.addCell(PdfCopyBillUtils.getBlankCell(3));
          }
		tableRows.addCell(PdfCopyBillUtils.getBlankLineWithHeight(7, 5f));
	
	// Add total Line
	tableRows.addCell(PdfCopyBillUtils.getCell(
			INTEGER_FORMAT.format(totalPieces), Element.ALIGN_LEFT,
			PdfFontEnum.NORMAL.getFont()));
 	tableRows.addCell(PdfCopyBillUtils.getBlankCell(2));
	tableRows.addCell(PdfCopyBillUtils.getCell("TOTAL", Element.ALIGN_LEFT,
			PdfFontEnum.NORMAL.getFont()));

	tableRows.addCell(PdfCopyBillUtils.getCell(
			INTEGER_FORMAT.format(copyBillPdf.getTotalWeightLbs()),
			Element.ALIGN_RIGHT, PdfFontEnum.NORMAL.getFont()));

	tableRows.addCell(PdfCopyBillUtils.getSpannedCell(
  	 US_DOLLAR_FORMAT.format(copyBillPdf.getTotalChargeAmount()),
  			Element.ALIGN_RIGHT, PdfFontEnum.NORMAL.getFont(), 2));
   	tableRows.addCell(PdfCopyBillUtils.getBlankCell(1));
   	
	// final PdfPCell debtorCell = PdfCopyBillUtils.getCell(
	//		PdfCopyBillUtils.formatDebtorCode(copyBillPdf.getDebtorType()),
	//		Element.ALIGN_LEFT, PdfFontEnum.NORMAL.getFont());
	// debtorCell.setPaddingLeft(2f);
	// tableRows.addCell(debtorCell);


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
		final CopyBillPdfHelper copyBillPdf) throws Exception
{
	// Print Additional Reference numbers
	final List<ReferenceNumber> theNumbers = new ArrayList<ReferenceNumber>();
	final List<SuppRefNbr> otherSuppRefNbr = copyBillPdf.getSuppRefNbrs();
	if (CollectionUtils.isNotEmpty(otherSuppRefNbr))
	{
		for (final SuppRefNbr referenceNumber : otherSuppRefNbr)
		{
			final String refTypeCd = referenceNumber.getTypeCd();
			if (StringUtils.isNotBlank(refTypeCd))
			{
				final String refNbr = referenceNumber.getRefNbr();
				if (StringUtils.equals(refTypeCd, "PO#") &&
					StringUtils.equals(refNbr, copyBillPdf.getRefNbrPO())) {
					continue;
				}

				if (StringUtils.equals(refTypeCd, "SN#") &&
					StringUtils.equals(refNbr, copyBillPdf.getRefNbrSN())) {
					continue;
				}
			}

			theNumbers.addAll((Collection<? extends ReferenceNumber>) referenceNumber);
		}
	}

	return theNumbers;
}


private PdfPTable addHeaderPartiesInfo(final CopyBillPdfHelper copyBillPdf) throws Exception {

	final PdfPTable table1 = new PdfPTable(1);
	table1.setWidthPercentage(100);

	final PdfPTable table = new PdfPTable(4);
	table.setWidthPercentage(100);
	table.getDefaultCell().setBorder(Rectangle.NO_BORDER);
	table.setWidths(new float[] { 30, 30, 25, 15 });
	table.addCell(PdfCopyBillUtils.getHeaderCell("SHIPPER", 0, 0));
	table.addCell(PdfCopyBillUtils.getHeaderCell("CONSIGNEE", 0, 0));
	table.addCell(PdfCopyBillUtils.getHeaderCell("SHIPPER'S NUMBER", 0, 0));
	table.addCell(PdfCopyBillUtils.getHeaderCell("BILL TO", 0, 0));

	table.addCell(PdfCopyBillUtils.getBlankCell(4));
	table.addCell(getHeaderPartyTable(copyBillPdf.getShipper()));
	table.addCell(getHeaderPartyTable(copyBillPdf.getConsignee()));
	table.addCell(getHeaderReferenceNumbers(copyBillPdf));
	//table.addCell(getHeaderPartyTable(copyBillPdf.getBillTo()));
	table.addCell(PdfCopyBillUtils.getBlankCell(1));

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

    private PdfPTable getHeaderReferenceNumbers(final CopyBillPdfHelper copyBillPdf) {

   	final PdfPTable table = new PdfPTable(1);
    	table.setWidthPercentage(100);
    	table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

    	if (StringUtils.isNotBlank(copyBillPdf.getRefNbrSN())) {
    	    table.addCell(PdfCopyBillUtils.getCell("SN# " + copyBillPdf.getRefNbrSN(), Element.ALIGN_LEFT,
    		    PdfFontEnum.NORMAL.getFont()));
    	}

    	if (StringUtils.isNotBlank(copyBillPdf.getRefNbrPO())) {
    	    table.addCell(PdfCopyBillUtils.getCell("PO# " + copyBillPdf.getRefNbrPO(), Element.ALIGN_LEFT,
    		    PdfFontEnum.NORMAL.getFont()));
    	}

    	if (StringUtils.isNotBlank(copyBillPdf.getParentProNbr())) {
    	    table.addCell(PdfCopyBillUtils.getCell("Original Pro#  " + PdfCopyBillUtils.formatPro(copyBillPdf.getParentProNbr()), Element.ALIGN_LEFT,
    		    PdfFontEnum.NORMAL.getFont()));

    	}

    	return table;
        }

    private PdfPTable addHeaderTitle(final CopyBillPdfHelper copyBillPdf) throws Exception
      {
    	final PdfPTable table = new PdfPTable(4);
    	table.setWidthPercentage(100);
    	final float[] widths = { 30, 30, 30, 10 };
    	table.setWidths(widths);
    	table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

    	table.addCell(PdfCopyBillUtils.getBlankCell(1));
    	table.addCell(PdfCopyBillUtils.getCell("LTL COPY BILL ", Element.ALIGN_LEFT,
    		PdfFontEnum.TITLE_BOLD.getFont()));
    	table.addCell(PdfCopyBillUtils.getCell("PRO NUMBER : " + PdfCopyBillUtils.formatPro(copyBillPdf.getProNbr()), Element.ALIGN_LEFT,
    		PdfFontEnum.TITLE_BOLD.getFont()));
    	table.addCell(PdfCopyBillUtils.getBlankCell(1));
        	
    	return table;
        }
    
    private PdfPTable addHeaderLine2(final CopyBillPdfHelper copyBillPdf) throws Exception
    {

    final PdfPTable table1 = new PdfPTable(1);
    table1.setWidthPercentage(100);
	
  	final PdfPTable table = new PdfPTable(5);
  	table.setWidthPercentage(100);
	table.getDefaultCell().setBorder(Rectangle.NO_BORDER);
	table.setWidths(new float[] { 20, 20, 20, 20, 20 });
	
	table.addCell(PdfCopyBillUtils.getHeaderCell("DATE", 0, 0));
	table.addCell(PdfCopyBillUtils.getHeaderCell("ORIGIN", 0, 0));
	table.addCell(PdfCopyBillUtils.getHeaderCell("DEST", 0, 0));
	table.addCell(PdfCopyBillUtils.getHeaderCell("OUR REV", 0, 0));
	table.addCell(PdfCopyBillUtils.getHeaderCell("PRO NUMBER", 0, 0));
	
	table.addCell(PdfCopyBillUtils.getBlankCell(5));
    table.addCell(PdfCopyBillUtils.getCell(
    		CreateCopyBillPdf.dateFormatter.format(PdfCopyBillUtils.convertToDate(copyBillPdf.getPickupDate())),
    		Element.ALIGN_LEFT, PdfFontEnum.NORMAL.getFont()));
    table.addCell(PdfCopyBillUtils.getCell(copyBillPdf.getOriginTerminalSicCd() ,Element.ALIGN_LEFT,
	  		PdfFontEnum.NORMAL.getFont()));
    table.addCell(PdfCopyBillUtils.getCell(copyBillPdf.getDestinationTerminalSicCd(),Element.ALIGN_LEFT,
  		PdfFontEnum.NORMAL.getFont()));
    table.addCell(PdfCopyBillUtils.getBlankCell(1));
	if (StringUtils.isNotBlank(copyBillPdf.getParentProNbr())) {
        table.addCell(PdfCopyBillUtils.getCell(PdfCopyBillUtils.formatPro(copyBillPdf.getParentProNbr()) + "*", Element.ALIGN_LEFT,
  		PdfFontEnum.NORMAL.getFont()));
        }
        else  table.addCell(PdfCopyBillUtils.getCell(PdfCopyBillUtils.formatPro(copyBillPdf.getProNbr()), Element.ALIGN_LEFT,
          		PdfFontEnum.NORMAL.getFont()));
  	
  	final PdfPCell cell = new PdfPCell();

	cell.setUseAscender(true);
	cell.setUseDescender(true);
	cell.setBorder(Rectangle.BOX);
	cell.setBorderColor(Color.LIGHT_GRAY);
	cell.addElement(table);
	table1.addCell(cell); 
	
  	return table1;
     }
    
}

