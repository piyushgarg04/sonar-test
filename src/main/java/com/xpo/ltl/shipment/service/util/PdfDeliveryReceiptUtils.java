package com.xpo.ltl.shipment.service.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.text.WordUtils;

import com.google.common.collect.Lists;
import com.itextpdf.barcodes.Barcode39;
import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.canvas.PdfCanvasConstants;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.layout.property.VerticalAlignment;
import com.lowagie.text.pdf.BaseFont;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.v2.AccessorialService;
import com.xpo.ltl.api.shipment.v2.AdvanceBeyondCarrier;
import com.xpo.ltl.api.shipment.v2.AsMatchedParty;
import com.xpo.ltl.api.shipment.v2.BillClassCd;
import com.xpo.ltl.api.shipment.v2.CashOnDelivery;
import com.xpo.ltl.api.shipment.v2.Commodity;
import com.xpo.ltl.api.shipment.v2.DeliveryQualifierCd;
import com.xpo.ltl.api.shipment.v2.DocumentFormTypeCd;
import com.xpo.ltl.api.shipment.v2.FBDSDocument;
import com.xpo.ltl.api.shipment.v2.FbdsVersionCd;
import com.xpo.ltl.api.shipment.v2.MatchedPartyTypeCd;
import com.xpo.ltl.api.shipment.v2.RatedAsWeight;
import com.xpo.ltl.api.shipment.v2.ReducedCharge;
import com.xpo.ltl.api.shipment.v2.ShipmentCharge;
import com.xpo.ltl.api.shipment.v2.ShipmentNotification;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.pdf.FBDSDataRow;

public final class PdfDeliveryReceiptUtils {
    
    public static final int MARGIN_LEFT = 10;
    public static final int MARGIN_RIGHT = 10;
    public static final int MARGIN_TOP = 10;
    public static final int MARGIN_BOTTOM = 10;
    
    private static final String PART_PREPAID = "PartPrepaid";
	private static final String PART_COLLECT = "PartCollect";
	private static final String CASH_PREPAID = "CashPrepaid";
	private static final String FREIGHT_CHARGE = "FreightCharge";
	private static final String ERROR_RPT_TITLE = "DR Print Error Report";

    public static final float NORMAL_FONT_SIZE = 7.5f;
    public static final float NORMAL_FONT_SIZE_FRENCH = 8f;
    public static final float SMALL_FONT_SIZE_FRENCH = 7f;
    public static final float CHECKBOX_FONT_SIZE = 7.5f;
    public static final float LARGE_FONT_SIZE = 10;
    public static final float TITLE_FONT_SIZE = 10;
    
    public static final int DESCRIPTION_CHAR_LIMIT = 55;
    public static final float DESCRIPTION_WIDTH_LIMIT = 320f;
    public static final float DESCRIPTION_FONT_SIZE = 12;
    
    private int fbdsDataRowLimitSinglePage;
    private int fbdsDataRowLimitFirstPage;
    private int fbdsDataRowLimitMiddlePage;
    private int fbdsDataRowLimitLastPage;
    
    private PdfFont NORMAL_FONT;
    private PdfFont BOLD_FONT;
    private PdfFont CHECKBOX_FONT;
    private Image XPO_LOGO;
    private DocumentFormTypeCd reportType;

    
    
    private boolean inBetweenSingleAndFirstPageLimit;
    private FBDSEngToFrenchTranslatorUtil translatorUtil = new FBDSEngToFrenchTranslatorUtil();
    
    public PdfDeliveryReceiptUtils(DocumentFormTypeCd documentFormTypeCd,
    	int singlePageLimit, int firstPageLimit, int middlePageLimit, int lastPageLimit) {

    	reportType = documentFormTypeCd;
    	fbdsDataRowLimitSinglePage = singlePageLimit;
    	fbdsDataRowLimitFirstPage = firstPageLimit;
    	fbdsDataRowLimitMiddlePage = middlePageLimit;
    	fbdsDataRowLimitLastPage = lastPageLimit;
    	inBetweenSingleAndFirstPageLimit = false;
    }
    
    public boolean getInBetweenSingleAndFirstPageLimit() {
    	return inBetweenSingleAndFirstPageLimit;
    }


	public void initializeFonts(final byte[] fontContent, final byte[] imageContent,
		final TransactionContext txnContext, boolean initAdditionalContent) throws ServiceException {
		try {
			NORMAL_FONT = PdfFontFactory.createFont(StandardFonts.HELVETICA);
			BOLD_FONT = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
			if(initAdditionalContent) {
				if(fontContent != null) {
			    	FontProgram fontProgram = FontProgramFactory.createFont(fontContent);
			    	CHECKBOX_FONT = PdfFontFactory.createFont(fontProgram, BaseFont.IDENTITY_H, false);					
				}

		    	if(imageContent != null) {
		    		XPO_LOGO = new Image(ImageDataFactory.create(imageContent));
		    	}
			}
		} catch (IOException e) {
			throw com.xpo.ltl.api.exception.ExceptionBuilder
			.exception(com.xpo.ltl.api.exception.ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e)
			.moreInfo("Unable to initialize font/image: ", e.getMessage())
			.build();
		}
	}

    
	public Cell getTitleValueCell(
	final String title,
	final FbdsVersionCd version_DR,
    final String value,
    final TextAlignment titleAlignment,
    final TextAlignment valueAlignment,
    final int rowspan,
    final int colspan
	  ) {
	    final Cell cell = new Cell(rowspan, colspan);
	    if(title != null) {
	    	if(version_DR==FbdsVersionCd.BILINGUAL && !StringUtils.equals("PCS", title) && !StringUtils.equals("HM", title)) {
	    		final Paragraph titleParagraph = new Paragraph().add(translatorUtil.getFrenchTranslation(firstLetterCaps(title))).setFont(BOLD_FONT)
	    				.setFontSize(NORMAL_FONT_SIZE_FRENCH).setTextAlignment(TextAlignment.LEFT);
	    		final Paragraph titleParagraphFrench = new Paragraph().add(firstLetterCaps(title)).setFont(NORMAL_FONT)
	    				.setFontSize(SMALL_FONT_SIZE_FRENCH).setTextAlignment(TextAlignment.LEFT);
	    		cell.add(titleParagraph);
	    		cell.add(titleParagraphFrench);
	    	}
	    	else {	    		
	    		final Paragraph titleParagraph = new Paragraph().add(title).setFont(BOLD_FONT)
	    				.setFontSize(NORMAL_FONT_SIZE).setTextAlignment(titleAlignment);
	    		cell.add(titleParagraph);      
	    	}
	    }
	    
	    if(value != null) {
	    	final Paragraph valueParagraph = new Paragraph().add(value).setFont(NORMAL_FONT)
	    			.setFontSize(NORMAL_FONT_SIZE).setTextAlignment(valueAlignment).setVerticalAlignment(VerticalAlignment.BOTTOM);
	    	cell.add(valueParagraph);
	    }
	    return cell;
	  }
	
	public Cell getCheckboxValueCell(
		final String title,
	    final TextAlignment titleAlignment,
	    final FbdsVersionCd version_DR,
	    final int rowspan,
	    final int colspan
		  ) {
		    final Cell cell = new Cell(rowspan, colspan);
		    if(title != null) {
		    	String text = translatorUtil.getFrenchTranslation(firstLetterCaps(title));
		  	  final Paragraph titleParagraph = new Paragraph().add((version_DR==FbdsVersionCd.FRENCH)?text.toUpperCase():text).setFont(BOLD_FONT)
		      		  .setFontSize((version_DR==FbdsVersionCd.FRENCH)?CHECKBOX_FONT_SIZE:NORMAL_FONT_SIZE_FRENCH).setTextAlignment(titleAlignment);
		  	  titleParagraph.setMultipliedLeading(0);
		  	  titleParagraph.setFixedLeading(7);
		  	  cell.add(titleParagraph);
		  	  if(version_DR==FbdsVersionCd.BILINGUAL) {
		  		  final Paragraph titleParagraphFrench = new Paragraph().add(firstLetterCaps(title)).setFont(NORMAL_FONT)
		  				  .setFontSize((version_DR==FbdsVersionCd.FRENCH)?CHECKBOX_FONT_SIZE:SMALL_FONT_SIZE_FRENCH).setTextAlignment(titleAlignment);
		  		  titleParagraphFrench.setMultipliedLeading(0);
		  		  titleParagraphFrench.setFixedLeading(7);
		  		  cell.add(titleParagraphFrench);    
		  	  }
		    }
		    
		    cell.setPadding(-0.055f);
		    cell.setBorder(Border.NO_BORDER);
		    return cell;
	}
	
	public Cell getCheckboxValueCell(
			final String title,
		    final TextAlignment titleAlignment,
		    final int rowspan,
		    final int colspan
			  ) {
			    final Cell cell = new Cell(rowspan, colspan);
			    if(title != null) {
			  	  final Paragraph titleParagraph = new Paragraph().add(title).setFont(BOLD_FONT)
			      		  .setFontSize(CHECKBOX_FONT_SIZE).setTextAlignment(titleAlignment);
			  	  titleParagraph.setMultipliedLeading(0);
			  	  titleParagraph.setFixedLeading(7);
			  	  
			        cell.add(titleParagraph);      
			    }
			    
			    cell.setPadding(-0.055f);
			    cell.setBorder(Border.NO_BORDER);
			    return cell;
		}
	
	public Cell getInterfaceAccountCell(
		final FBDSDocument fbdsDocumentData,
		final FbdsVersionCd version_DR,
	    final MatchedPartyTypeCd partyTypeCd,
	    final TextAlignment titleAlignment,
	    final TextAlignment valueAlignment,
	    final int rowspan,
	    final int colspan
		  ) {
		    final Cell cell = new Cell(rowspan, colspan);
        	Table consigneeTableCell = new Table(2);
        	consigneeTableCell.useAllAvailableWidth();
        	Cell tableCell = new Cell();
        
        	
		    Text title = null;
		    String titleString = StringUtils.EMPTY;
		    AsMatchedParty asMatchedParty = null;
		    Table headerTable = new Table(2).useAllAvailableWidth();
		    if(partyTypeCd == MatchedPartyTypeCd.CONS) {
		    	titleString = "CONSIGNEE";
		    	title = new Text(titleString);
		    	asMatchedParty = fbdsDocumentData.getConsignee();
		    }
		    else if(partyTypeCd == MatchedPartyTypeCd.SHPR){
		    	titleString = "SHIPPER";
		    	title = new Text(titleString);
		    	asMatchedParty = fbdsDocumentData.getShipper();
		    }
		    else {
		    	titleString = "BILL TO";
		    	title = new Text(titleString);
		    	asMatchedParty = fbdsDocumentData.getBillTo();
		    }
		    
		    titleString = firstLetterCaps(titleString);
		    if(version_DR == FbdsVersionCd.BILINGUAL) {
		    	final Paragraph titleParagraph = new Paragraph().add(translatorUtil.getFrenchTranslation(titleString)).setFont(BOLD_FONT)
			      		  .setFontSize(NORMAL_FONT_SIZE_FRENCH).setTextAlignment(titleAlignment);
		    	headerTable.addCell(new Cell().setPadding(-0.2f).setBorder(Border.NO_BORDER).add(titleParagraph));
		    	if(partyTypeCd == MatchedPartyTypeCd.CONS && fbdsDocumentData.getWarrantyText() != null) {
		    		headerTable.addCell(new Cell().setPadding(-0.2f).setBorder(Border.NO_BORDER).add(new Paragraph().add(fbdsDocumentData.getWarrantyText()).setFont(NORMAL_FONT)
		    				.setFontSize(NORMAL_FONT_SIZE_FRENCH).setTextAlignment(TextAlignment.RIGHT)));
		    	}
		    	cell.add(headerTable);
		    	final Paragraph titleParagraphFrench = new Paragraph().add(titleString).setFont(NORMAL_FONT)
			      		  .setFontSize(SMALL_FONT_SIZE_FRENCH).setTextAlignment(titleAlignment);
			    cell.add(titleParagraphFrench);
		    }
		    else {	    	
		    	title = (version_DR==FbdsVersionCd.FRENCH)?new Text(translatorUtil.getFrenchTranslation(titleString).toUpperCase()):title;
		    	final Paragraph titleParagraph = new Paragraph().add(title).setFont(BOLD_FONT)
		    			.setFontSize(NORMAL_FONT_SIZE).setTextAlignment(titleAlignment);
		    	headerTable.addCell(new Cell().setPadding(-0.2f).setBorder(Border.NO_BORDER).add(titleParagraph));
		    	if(partyTypeCd == MatchedPartyTypeCd.CONS && fbdsDocumentData.getWarrantyText() != null) {
		    		headerTable.addCell(new Cell().setPadding(-0.2f).setBorder(Border.NO_BORDER).add(new Paragraph().add(fbdsDocumentData.getWarrantyText()).setFont(NORMAL_FONT)
		    				.setFontSize(NORMAL_FONT_SIZE).setTextAlignment(TextAlignment.RIGHT)));
		    	}
		    	cell.add(headerTable);
		    }
		    
        	if(asMatchedParty != null) {
        		if(partyTypeCd == MatchedPartyTypeCd.CONS) {
        			if(fbdsDocumentData.getConsigneeMessage() != null)
    	        		tableCell.add(createValueParagraph(valueAlignment, version_DR, fbdsDocumentData.getConsigneeMessage()));
    	        	if(fbdsDocumentData.getCollectorOfCustomMessage() != null)
    	        		tableCell.add(createValueParagraph(valueAlignment, version_DR, fbdsDocumentData.getCollectorOfCustomMessage()));
        		}
        		tableCell.add(createValueParagraph(valueAlignment, version_DR, asMatchedParty.getName1()));
        		tableCell.add(createValueParagraph(valueAlignment, version_DR, asMatchedParty.getName2()));
        		tableCell.add(createValueParagraph(valueAlignment, version_DR, asMatchedParty.getAddress()));
        		tableCell.add(createValueParagraph(valueAlignment, version_DR, asMatchedParty.getCity() + ", "
		           + asMatchedParty.getStateCd() + " " + asMatchedParty.getCountryCd() + " " + getZipCode(asMatchedParty)));
        		tableCell.setBorder(Border.NO_BORDER);
        		tableCell.setPadding(-0.4f);
	        	consigneeTableCell.addCell(tableCell);
	        }

        	cell.add(consigneeTableCell);
		    return cell;
	}
	
	private String getZipCode(AsMatchedParty asMatchedParty) {
		if(StringUtils.isNotBlank(asMatchedParty.getZip4RestUs())) {
			return asMatchedParty.getZip6() + "-" + asMatchedParty.getZip4RestUs();
		}
		return asMatchedParty.getZip6();
	}

	private Paragraph createValueParagraph(final TextAlignment valueAlignment, FbdsVersionCd version_DR, String value) {
		if(value != null) {		
			return new Paragraph().setFont(NORMAL_FONT).setFontSize((version_DR==FbdsVersionCd.BILINGUAL)?SMALL_FONT_SIZE_FRENCH:NORMAL_FONT_SIZE)
					.setTextAlignment(valueAlignment).setVerticalAlignment(VerticalAlignment.BOTTOM).add(value);			
		}
		else return null;
	}


	public Cell getDataRowCell(
		final String value,
		final FbdsVersionCd version_DR,
		final TextAlignment valueAlignment,
		final int rowspan,
	    final int colspan,
	    final boolean drawBottomLine,
	    final boolean isTotalItem) {
		final Cell cell = new Cell(rowspan, colspan);
	      
	      if(value != null) {
	    	  if(StringUtils.contains(value, ":COLL") || StringUtils.contains(value, ":PPD")
	    			  || StringUtils.contains(value, ":COL") || StringUtils.contains(value, ":BOTH")) {
	    		  String[] splitTotalValueParams = StringUtils.split(value.replace("null", " "), ":");
	    		  String totalValue = null;
	    		  if(!StringUtils.isBlank(splitTotalValueParams[0])) {
	    			  if(StringUtils.isAlpha(splitTotalValueParams[0])) {
	    				  totalValue = splitTotalValueParams[0];
	    			  }
	    			  else {
	    				  totalValue = retrieveDoubleString(NumberUtils.toDouble(splitTotalValueParams[0]), false);  
	    			  }
	    		  } else {
	    			  totalValue = StringUtils.EMPTY;
	    		  }
		  	      Table totalTableCell = new Table(2);
		  	      totalTableCell.useAllAvailableWidth();
		  	      totalTableCell.addCell(getTotalChargesCell(splitTotalValueParams[1], version_DR, TextAlignment.LEFT, isTotalItem));
		  	      totalTableCell.addCell(getTotalChargesCell(totalValue, version_DR, TextAlignment.RIGHT, isTotalItem));
		  	      	
		  	        cell.add(totalTableCell);
	    	  }
	    	  else if (StringUtils.contains(value, "OTHER REFERENCE NUMBERS: ")) {
	    		  
	    	  }
	    	  else {
	    		  final Paragraph valueParagraph = new Paragraph().add(value).setFont(NORMAL_FONT)
	  	      			.setFontSize((version_DR==FbdsVersionCd.BILINGUAL) ? SMALL_FONT_SIZE_FRENCH:NORMAL_FONT_SIZE).setTextAlignment(valueAlignment);
	  	      	  cell.add(valueParagraph);  
	    	  }
	      }
	      cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
	      cell.setPaddingBottom(-0.4f);
	      cell.setPaddingTop(-0.2f);
	      cell.setBorderTop(Border.NO_BORDER);
	      if(!drawBottomLine) {
	    	  cell.setBorderBottom(Border.NO_BORDER).setBorderTop(Border.NO_BORDER);  
	      }
	      return cell;
	}

	public Cell getDescriptionDataRowCell(
		final String value,
		final String additionalValue,
		final TextAlignment valueAlignment,
		final int rowspan,
	    final int colspan,
	    final boolean drawBottomLine,
	    final FbdsVersionCd version_DR) {
		final Cell cell = new Cell(rowspan, colspan);
		final Paragraph valueParagraph = new Paragraph();
			if(additionalValue != null) {
				valueParagraph.add(new Text(additionalValue).setFont(BOLD_FONT)
		  			.setFontSize((version_DR==FbdsVersionCd.BILINGUAL)?NORMAL_FONT_SIZE_FRENCH:NORMAL_FONT_SIZE).setTextAlignment(valueAlignment));
			}
			valueParagraph.add(new Text(value).setFont(NORMAL_FONT)
    		.setFontSize((version_DR==FbdsVersionCd.BILINGUAL)?NORMAL_FONT_SIZE:NORMAL_FONT_SIZE-0.1f).setTextAlignment(valueAlignment));
      	  cell.add(valueParagraph);
	      cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
	      cell.setPaddingBottom(-0.4f);
	      cell.setPaddingTop(-0.2f);
	      cell.setPaddingRight(-0.2f);
	      cell.setTextAlignment(valueAlignment);
	      cell.setBorderTop(Border.NO_BORDER);
	      if(!drawBottomLine) {
	    	  cell.setBorderBottom(Border.NO_BORDER).setBorderTop(Border.NO_BORDER);  
	      }
	      return cell;
	}
	
	private Cell getTotalChargesCell(String totalValue, FbdsVersionCd version_DR, TextAlignment textAlignment, boolean isTotalItem) {
		Cell tableCell = new Cell(1,1);
	    tableCell.add(new Paragraph().add(totalValue).setTextAlignment(textAlignment).setFontSize((version_DR==FbdsVersionCd.BILINGUAL) ? NORMAL_FONT_SIZE_FRENCH : NORMAL_FONT_SIZE)
	    	.setFont(isTotalItem ? BOLD_FONT : NORMAL_FONT));
	    tableCell.setPaddingBottom(-0.4f);
        tableCell.setPaddingTop(-0.2f);
        tableCell.setPaddingRight(-0.2f);
        tableCell.setBorder(Border.NO_BORDER);
		return tableCell;
	}

	public Cell getHeaderCell(
    final String title,
    final String value,
    final TextAlignment valueAlignment,
    final int rowspan,
    final int colspan,
    final float fontSize) {
    final Cell cell = new Cell(rowspan, colspan);
    
    if(title != null) {
    	final Paragraph valueParagraph = new Paragraph().add(title).setFont(BOLD_FONT)
    			.setFontSize(fontSize).setTextAlignment(valueAlignment);
    	cell.add(valueParagraph);
    }
    if(value != null) {
    	//CCS-2919: Adjusted pro number cell size
    	if(StringUtils.equals(title, "PRO NUMBER") || StringUtils.equals(title, "Pro Numéro")) {
        	final Paragraph valueParagraph = new Paragraph().add(value).setFont(BOLD_FONT)
        			.setFontSize(StringUtils.equals(title, "PRO NUMBER") ? 11 : fontSize).setTextAlignment(valueAlignment);
        	cell.add(valueParagraph);
        	cell.setPaddingRight(-1f);
    	}
    	else {
        	final Paragraph valueParagraph = new Paragraph().add(value).setFont(BOLD_FONT)
        			.setFontSize(fontSize).setTextAlignment(valueAlignment);
        	cell.add(valueParagraph);    		
    	}
    }
    cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
    return cell;
  }
	
	public String firstLetterCaps(final String str) {
		String res = StringUtils.EMPTY;
		String[] words = str.split(" ");
		for(int i = 0; i < words.length; i++) {
			String firstLetter = words[i].substring(0,1).toUpperCase();
		    String restLetters = words[i].substring(1).toLowerCase();
		    res += firstLetter+restLetters;
		    if(i != words.length-1) {
		    	res += " ";
		    }
		}
		return res;
		
	}

	public Cell getHeaderCellFrench(
		    final String title,
		    final String value,
		    final TextAlignment valueAlignment,
		    final int rowspan,
		    final int colspan,
		    final float fontSize) {
		    final Cell cell = new Cell(rowspan, colspan);
		    
		    if(title != null) {
		    	final Paragraph valueParagraph = new Paragraph().add(translatorUtil.getFrenchTranslation(firstLetterCaps(title))).setFont(BOLD_FONT)
		    			.setFontSize(fontSize).setTextAlignment(valueAlignment).setVerticalAlignment(VerticalAlignment.BOTTOM);
		    	final Paragraph valueParagraphFrench = new Paragraph().add(firstLetterCaps(title)).setFont(NORMAL_FONT)
		    			.setFontSize(fontSize-1).setTextAlignment(valueAlignment).setVerticalAlignment(VerticalAlignment.BOTTOM); 
		    	cell.add(valueParagraph);
		    	cell.add(valueParagraphFrench);
		    }
		    if(value != null) {
		    	//CCS-2919: Adjusted pro number cell size
		    	if(StringUtils.equals(title, "PRO NUMBER")) {
		        	final Paragraph valueParagraph = new Paragraph().add(value).setFont(BOLD_FONT)
		        			.setFontSize(StringUtils.equals(title, "PRO NUMBER") ? 11 : fontSize).setTextAlignment(valueAlignment);
		        	cell.add(valueParagraph);
		        	cell.setPaddingRight(-1f);
		    	}
		    	else {
		        	final Paragraph valueParagraph = new Paragraph().add(value).setFont(NORMAL_FONT)
		        			.setFontSize(fontSize).setTextAlignment(valueAlignment).setVerticalAlignment(VerticalAlignment.BOTTOM);
		        	final Paragraph valueParagraphXPO = new Paragraph().add("www.xpo.com").setFont(NORMAL_FONT)
		        			.setFontSize(fontSize).setTextAlignment(valueAlignment).setVerticalAlignment(VerticalAlignment.BOTTOM);
		        	cell.add(valueParagraph);    		
		        	cell.add(valueParagraphXPO);
		    	}

		    }
		    cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
		    return cell;
		  }
	
  public Cell createBarcode(
  	final String title,
  	final String code,
  	final String altText,
  	final int rowspan,
  	final int colspan,
  	final float height,
  	final float width,
  	final Float baselineShift,
  	final PdfDocument pdfDoc) {
  	Cell cell = new Cell(rowspan, colspan);
  	if(title != null) {
    	  final Paragraph titleParagraph = new Paragraph().add(title).setFont(BOLD_FONT)
        		  .setFontSize(NORMAL_FONT_SIZE).setTextAlignment(TextAlignment.LEFT);
          cell.add(titleParagraph);      
      }
  	
      Barcode39 barcode = new Barcode39(pdfDoc);
      barcode.setBarHeight(height);
      if(baselineShift != null) 
    	  barcode.setBaseline(baselineShift);
      barcode.setX(width);
      barcode.setCode(code);
      barcode.setAltText(altText);
      
      Image code39Image = new Image(barcode.createFormXObject(pdfDoc));
      code39Image.setHorizontalAlignment(HorizontalAlignment.LEFT);
      cell.add(code39Image);
      cell.setHorizontalAlignment(HorizontalAlignment.CENTER);
      cell.setPaddingLeft(-2f);
      cell.setPaddingRight(3f);
      return cell;
  }
  
	public Cell getXPOLogoPng(
		final int rowspan,
		final int colspan) throws MalformedURLException {
		Cell cell = new Cell(rowspan, colspan);
		
		if(XPO_LOGO != null) {
			XPO_LOGO.setWidth(UnitValue.createPercentValue(70f));
			XPO_LOGO.setHeight(UnitValue.createPercentValue(70f));
			XPO_LOGO.setHorizontalAlignment(HorizontalAlignment.CENTER);
			cell.add(XPO_LOGO);
			cell.setWidth(XPO_LOGO.getWidth().getValue());
			cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
			
			final Paragraph valueParagraph = new Paragraph().add("www.xpo.com").setFont(NORMAL_FONT)
	      			.setFontSize(NORMAL_FONT_SIZE).setTextAlignment(TextAlignment.CENTER);
	      	cell.add(valueParagraph);		
		}
		
		return cell;
	}
	
	public Cell getXPOLogoPngFrench(
			final int rowspan,
			final int colspan,
			final String title) throws MalformedURLException {
		
		Cell cell = new Cell(rowspan, colspan);
		
		if(XPO_LOGO != null) {
			XPO_LOGO.setWidth(UnitValue.createPercentValue(50f));
			XPO_LOGO.setHeight(UnitValue.createPercentValue(40f));
			XPO_LOGO.setHorizontalAlignment(HorizontalAlignment.LEFT);
			cell.add(XPO_LOGO);
			cell.setWidth(XPO_LOGO.getWidth().getValue());
			cell.setVerticalAlignment(VerticalAlignment.TOP);
			
			final Paragraph valueParagraph = new Paragraph().add(translatorUtil.getFrenchTranslation(firstLetterCaps(title))).setFont(BOLD_FONT)
					.setFontSize(NORMAL_FONT_SIZE_FRENCH).setTextAlignment(TextAlignment.LEFT).setPaddingTop(4f);
			final Paragraph valueParagraphFrench = new Paragraph().add(firstLetterCaps(title)).setFont(NORMAL_FONT)
					.setFontSize(SMALL_FONT_SIZE_FRENCH).setTextAlignment(TextAlignment.LEFT);
	      	cell.add(valueParagraph);		
	      	cell.add(valueParagraphFrench);
		}
		
		return cell;
	}

	public Cell getCheckboxCell(
		final boolean value,
		final int rowspan,
	    final int colspan,
	    final FbdsVersionCd version_DR) throws IOException {
		Cell cell = new Cell(rowspan, colspan);

	  	Paragraph paragraph = new Paragraph().setFontSize(NORMAL_FONT_SIZE).setFont(NORMAL_FONT)
	  			.setTextAlignment(TextAlignment.LEFT).setVerticalAlignment((version_DR==FbdsVersionCd.FRENCH)?VerticalAlignment.TOP:VerticalAlignment.MIDDLE)
	  			.add(getCheckBoxText(false))
	  			;
	  	
	  	cell.add(paragraph);
	  	cell.setBorder(Border.NO_BORDER);
	  	cell.setPaddingRight(-0.4f);
	  	cell.setPaddingTop(-0.6f);
	  	cell.setPaddingBottom(-0.6f);
//	  	cell.setPaddingLeft(-0.05f);
		return cell;
	}

	public Cell getShrinkWrapIntactCell(
		final int rowspan,
	    final int colspan,
	    final FbdsVersionCd version_DR) {
		Cell cell = new Cell(rowspan, colspan);

	  	Paragraph paragraph = new Paragraph().add((version_DR==FbdsVersionCd.FRENCH)?translatorUtil.getFrenchTranslation("Shrink Wrap Intact?").toUpperCase():"SHRINK WRAP INTACT?").setFontSize(CHECKBOX_FONT_SIZE).setFont(BOLD_FONT)
	  			.setTextAlignment(TextAlignment.LEFT);
	  	if(version_DR==FbdsVersionCd.BILINGUAL) {
	  		  paragraph = new Paragraph().add(translatorUtil.getFrenchTranslation("Shrink Wrap Intact?")).setFontSize(NORMAL_FONT_SIZE_FRENCH).setFont(BOLD_FONT)
	  	  			.setTextAlignment(TextAlignment.LEFT); 
	  		  cell.add(paragraph);
	    	  paragraph = new Paragraph().add("Shrink Wrap Intact?")
	    			  .setFont(NORMAL_FONT).setFontSize(SMALL_FONT_SIZE_FRENCH).setTextAlignment(TextAlignment.LEFT);
	    }
	  	cell.add(paragraph);
	  	
	  	
	  	paragraph = new Paragraph().setFontSize((version_DR==FbdsVersionCd.BILINGUAL)?SMALL_FONT_SIZE_FRENCH:NORMAL_FONT_SIZE).setFont(NORMAL_FONT)
	  			.setTextAlignment(TextAlignment.LEFT)
	  			.add(getCheckBoxText(false)).add(" Yes ")
	  			.add(getCheckBoxText(false)).add(" No ")
	  			.add(getCheckBoxText(false)).add(" N/A");
	  	cell.add(paragraph);
	  	
		return cell;
	}


	private Text getCheckBoxText(final boolean value) {
		return new Text(value ? String.valueOf('\u00FD') : String.valueOf('\u00A8'))
				  .setTextRenderingMode(PdfCanvasConstants.TextRenderingMode.FILL_STROKE)
				  .setStrokeWidth(0.5f)
				  .setFont(CHECKBOX_FONT)
		  		  .setFontSize(11);
	}

	public Cell getTableHeaderCell(
		final String title,
		final TextAlignment textAlignment,
		final int rowspan,
	    final int colspan) {
		final Cell cell = new Cell(rowspan, colspan);
	      if(title != null) {
	    	  final Paragraph titleParagraph = new Paragraph().add(title).setFont(BOLD_FONT)
	        		  .setFontSize(NORMAL_FONT_SIZE).setTextAlignment(textAlignment);
	          cell.add(titleParagraph);      
	      }

	      return cell;
	}
	
	public Cell getTitleValueCellwithEmptyParagraph(
			final String title,
			final String value,
			final FbdsVersionCd version_DR,
			final TextAlignment titleAlignment,
			final TextAlignment valueAlignment,
			final int rowspan,
			final int colspan,
			final boolean isDateTimeCell) {
		final Cell cell = new Cell(rowspan, colspan);
		if(title != null) {
			String text = translatorUtil.getFrenchTranslation(firstLetterCaps(title));
			final Paragraph titleParagraph = new Paragraph().add((version_DR!=FbdsVersionCd.DEFAULT)?(version_DR==FbdsVersionCd.FRENCH)?text.toUpperCase():text:title).setFont(BOLD_FONT)
					.setFontSize((version_DR==FbdsVersionCd.BILINGUAL)?NORMAL_FONT_SIZE_FRENCH:NORMAL_FONT_SIZE).setTextAlignment(titleAlignment);
			cell.add(titleParagraph);
			if(version_DR==FbdsVersionCd.BILINGUAL) {
				final Paragraph titleParagraphFrench = new Paragraph().add(firstLetterCaps(title))
						.setFont(NORMAL_FONT).setFontSize(SMALL_FONT_SIZE_FRENCH).setTextAlignment(titleAlignment);
				cell.add(titleParagraphFrench);
			}
		}
    
		if(value != null) {
			if(isDateTimeCell) {
				cell.add(new Paragraph(value).setFontSize(LARGE_FONT_SIZE)
						.setFont(BOLD_FONT).setTextAlignment(valueAlignment));
			}
			else {
				cell.add(new Paragraph(value).setFontSize((version_DR==FbdsVersionCd.BILINGUAL)?4.9f:6)
						.setFont(NORMAL_FONT).setTextAlignment((version_DR==FbdsVersionCd.BILINGUAL)?titleAlignment:valueAlignment));
			}
		}
 
		return cell;
	}

	public Cell getSignatureSectionCell(
	final String title,
    final String value,
    final FbdsVersionCd version_DR,
    final TextAlignment titleAlignment,
    final TextAlignment valueAlignment,
    final int rowspan,
    final int colspan,
    final boolean isDateTimeCell
  ) {
    final Cell cell = new Cell(rowspan, colspan);
    if(title != null) {
    String text = translatorUtil.getFrenchTranslation(firstLetterCaps(title));
  	  final Paragraph titleParagraph = new Paragraph().add((version_DR!=FbdsVersionCd.DEFAULT)?
  			  												(version_DR==FbdsVersionCd.FRENCH)?text.toUpperCase():text:title).setFont(BOLD_FONT)
      		  .setFontSize((version_DR==FbdsVersionCd.BILINGUAL)?NORMAL_FONT_SIZE_FRENCH:CHECKBOX_FONT_SIZE).setTextAlignment(titleAlignment);
        cell.add(titleParagraph);
        if(version_DR==FbdsVersionCd.BILINGUAL) {
      	  final Paragraph titleParagraphFrench = new Paragraph().add(firstLetterCaps(title))
      			  .setFont(NORMAL_FONT).setFontSize(SMALL_FONT_SIZE_FRENCH).setTextAlignment(titleAlignment);
      	  cell.add(titleParagraphFrench);
        }
    }
    
    if(value != null) {
    	if(isDateTimeCell) {
    		cell.add(new Paragraph(value).setFontSize(NORMAL_FONT_SIZE_FRENCH)
    			.setFont(BOLD_FONT).setTextAlignment(valueAlignment));
    	}
    	else {
    		cell.add(new Paragraph(value).setFontSize(6)
    			.setFont(NORMAL_FONT).setTextAlignment(valueAlignment));
    	}
    }
	
    cell.setPaddingTop(3);
    return cell;
  }

	
	public Cell getVerticalValueCell(
	    final String value,
	    final int rowspan,
	    final int colspan) {
		
		final Cell cell = new Cell(rowspan, colspan);
	    
	    if(value != null) {
	    	final Paragraph valueParagraph = new Paragraph().add(value).setFont(NORMAL_FONT)
	    			.setFontSize(NORMAL_FONT_SIZE).setTextAlignment(TextAlignment.CENTER)
	    			.setRotationAngle(Math.PI/2)
	    			.setVerticalAlignment(VerticalAlignment.BOTTOM)
	    			.setHorizontalAlignment(HorizontalAlignment.RIGHT)
	    			;
	    	cell.add(valueParagraph);
	    }
	    cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
	    cell.setHorizontalAlignment(HorizontalAlignment.RIGHT);
	    cell.setBorderBottom(Border.NO_BORDER);
	    return cell;
	}


	public Cell getNotesCell(
		final int rowspan,
	    final int colspan) {
		final Cell cell = new Cell(rowspan, colspan);
	    Paragraph paragraph = new Paragraph("SUBJECT TO THE TERMS AND CONDITIONS HEREIN,"
	    		+ " AND TARIFF CNWY-199 IN EFFECT ON DATE OF SHIPMENT.");
	    cell.add(paragraph);
	    cell.setFont(NORMAL_FONT).setFontSize(8);
	    cell.setPadding(-0.5f);
		return cell;
	}
	
	public String buildAppointmentCell(FBDSDocument fbdsDocumentData) {
		String result = null;
		if(fbdsDocumentData.getNotification() != null) {
			String notificationResult = null;
			ShipmentNotification notification = fbdsDocumentData.getNotification();

			notificationResult = Stream.of(notification.getMessage(), notification.getNotificationTime(),
				notification.getNotificationDate(), notification.getContactName())
			          .filter(s -> s != null && !s.isEmpty())
			          .collect(Collectors.joining(" "));
			if(StringUtils.isNotBlank(notification.getMessageObservation()) && !StringUtils.contains(notification.getMessageObservation(), "*SEE APPT")) {
				 notificationResult += '\n' + notification.getMessageObservation();
			}

			result = result != null ? result + notificationResult : notificationResult;
		}
		return result;
	}

	public List<FBDSDataRow> buildFbdsDataRows(FBDSDocument fbdsDocumentData, DocumentFormTypeCd reportType) {

		List<FBDSDataRow> fbdsDataRows = new ArrayList<FBDSDataRow>();
		
		if(reportType == DocumentFormTypeCd.COPY_BILL) {
			retrieveItemDescription("**** COPY BILL ****", fbdsDataRows);
		}
		
        //Driver Collect
        retrieveItemDescription(fbdsDocumentData.getDriverCollectDescription(), fbdsDataRows);
        
        //Movr Clearance Bill
        retrieveItemDescription(fbdsDocumentData.getMovrClearanceBillText(), fbdsDataRows);
        
        //Freezable
        retrieveItemDescription(fbdsDocumentData.getFreezableText(), fbdsDataRows);
        
        //Cash on Delivery (1)
        if(fbdsDocumentData.getRatesAndCharges() != null && fbdsDocumentData.getRatesAndCharges().getCashOnDelivery() != null &&
        		StringUtils.isNotBlank(fbdsDocumentData.getRatesAndCharges().getCashOnDelivery().getDescription1())) {
        	CashOnDelivery cashOnDelivery = fbdsDocumentData.getRatesAndCharges().getCashOnDelivery();
        	fbdsDataRows.addAll(createFbdsDataRowPartition(null, null,
        		cashOnDelivery.getDescription1(), null, null, null,
        		retrieveBigDecimalString(cashOnDelivery.getAmount(), false)));	
        }

        //Total (with CLearance Bill)
        if(StringUtils.isNotBlank(fbdsDocumentData.getMovrClearanceBillText())) {
        	retriveTotalItemRow(fbdsDocumentData, fbdsDataRows);
        }
        
        
        //Commodities
        retrieveCommodityRows(fbdsDocumentData, fbdsDataRows);
    
        //Rated as weight
        if(fbdsDocumentData.getRatesAndCharges() != null && fbdsDocumentData.getRatesAndCharges().getRatedAsWeight() != null) {
        	RatedAsWeight ratedAsWeight = fbdsDocumentData.getRatesAndCharges().getRatedAsWeight();
        	fbdsDataRows.addAll(createFbdsDataRowPartition(null, null, ratedAsWeight.getRatedAsWeightDescription(), null,
        		retrieveBigDecimalString(ratedAsWeight.getQuantity(), true), ratedAsWeight.getTariffRateTxt(),
        		retrieveBigDecimalString(ratedAsWeight.getAmount(), false)));
        }
        
        //Reduced Charge
        if(fbdsDocumentData.getRatesAndCharges() != null && fbdsDocumentData.getRatesAndCharges().getReducedCharge() != null) {
        	ReducedCharge reducedCharge = fbdsDocumentData.getRatesAndCharges().getReducedCharge();
        	fbdsDataRows.addAll(createFbdsDataRowPartition(null, null, reducedCharge.getReducedChargeText(), null, null, null,
        		retrieveBigDecimalString(reducedCharge.getAmount(), false)));
        }
        
        //Accessorials
        if(CollectionUtils.isNotEmpty(fbdsDocumentData.getAccessorials())) {
			for (AccessorialService accessorial : fbdsDocumentData.getAccessorials()) {
				if (StringUtils.isNotEmpty(accessorial.getAccessorialCd())) {
					fbdsDataRows.addAll(createFbdsDataRowPartition(null, null, accessorial.getDescription(), null, null,
							getAccesorialTariffRate(accessorial),
							retrieveDoubleString(accessorial.getAmount(), false)));
				}
			}
        }
        
        //Total (without Clearance Bill)
        if(StringUtils.isBlank(fbdsDocumentData.getMovrClearanceBillText())) {
        	retriveTotalItemRow(fbdsDocumentData, fbdsDataRows);
        }
        
        //Final item description 
        if(((isMovrPro(fbdsDocumentData)  && !isPartShort(fbdsDocumentData)) 
        		|| (isPseg(fbdsDocumentData) && isPartShort(fbdsDocumentData)))) {
        	retrieveItemDescription(fbdsDocumentData.getFinalItemsDescription(), fbdsDataRows);   	
        }
        
        //In Bond Description
        retrieveItemDescription(fbdsDocumentData.getInBondDescription(), fbdsDataRows);
        
        //Shipment Charges (Part Prepaid, Part Collect)
        retrieveShipmentChargesRow(fbdsDocumentData, fbdsDataRows, PART_PREPAID);
        retrieveShipmentChargesRow(fbdsDocumentData, fbdsDataRows, PART_COLLECT);
        
        //Cash on delivery (2)
        if(fbdsDocumentData.getRatesAndCharges() != null && fbdsDocumentData.getRatesAndCharges().getCashOnDelivery() != null &&
        		StringUtils.isNotBlank(fbdsDocumentData.getRatesAndCharges().getCashOnDelivery().getDescription2())) {
        	CashOnDelivery cashOnDelivery = fbdsDocumentData.getRatesAndCharges().getCashOnDelivery();
        	fbdsDataRows.addAll(createFbdsDataRowPartition(null, null,
        		cashOnDelivery.getDescription2(), null, null, null,
        		retrieveBigDecimalString(cashOnDelivery.getAmount(), false)));	
        }
        
        //Shipment Charges (Freight Charge)
        retrieveShipmentChargesRow(fbdsDocumentData, fbdsDataRows, FREIGHT_CHARGE);      
        
        //Final item description (GOBZ)
        if(!isMovrPro(fbdsDocumentData) && !isPartShort(fbdsDocumentData)) {
        	retrieveItemDescription(fbdsDocumentData.getFinalItemsDescription(), fbdsDataRows);
        }
        
        //Movr Clearance Pieces Outstanding
        retrieveListDescription(fbdsDocumentData.getChildProNbrs(), fbdsDocumentData.getMovrClearancePiecesOutstandingText(), fbdsDataRows);
        
        //Movr Clearance Pieces Bill HazMat
        retrieveItemDescription(fbdsDocumentData.getMovrClearanceBillHazMatText(), fbdsDataRows);
        
        //Customer Reference Numbers
        retrieveListDescription(fbdsDocumentData.getCustomerReferenceNbrs(), "CUSTOMER REFERENCE NUMBERS: ", fbdsDataRows);
        
        //Child pros
        retrieveListDescription(CollectionUtils.emptyIfNull(fbdsDocumentData.getHandlingUnits()).stream()
        	.map(handlingUnit -> ProNumberHelper.toTenDigitPro(handlingUnit.getChildProNbr()))
        	.collect(Collectors.toList()), "CHILD PRO #'s: ", fbdsDataRows);
        
        //PO Numbers
        retrieveListDescription(fbdsDocumentData.getPoNbrs(), "PO NUMBERS: ", fbdsDataRows);
        
        //Other Numbers
        retrieveListDescription(fbdsDocumentData.getOtherReferenceNbrs(), "OTHER REFERENCE NUMBERS: ", fbdsDataRows);
        
        //Authority Line Remark
        retrieveItemDescription(fbdsDocumentData.getAuthorityLineRemark(), fbdsDataRows);
        
        //Shipment Remark
        retrieveItemDescription(fbdsDocumentData.getShipmentRemark(), fbdsDataRows);
        
        //Hazmat Remark
        retrieveItemDescription(fbdsDocumentData.getHazmatRemark(), fbdsDataRows);
        
		//Shiplify Remark
		if(StringUtils.isNotBlank(fbdsDocumentData.getShiplifyRemark())) {
			retrieveListDescription(Arrays.asList("A LIFTGATE MIGHT BE REQUIRED"), "NO DOCK AND FORKLIFT ACCESS: ", fbdsDataRows);
		}

        //Delivery Attachment Remark
        retrieveItemDescription(fbdsDocumentData.getDeliveryAttachmentRemark(), fbdsDataRows);
       
        //Canadian Goods Service Remark
        retrieveItemDescription(fbdsDocumentData.getCanadianGoodsServicesRemark(), fbdsDataRows);
      
        //Quebec Gst Remark
        retrieveItemDescription(fbdsDocumentData.getQuebecGstRemark(), fbdsDataRows);
      
        //Shipment Charges (Cash)
        retrieveShipmentChargesRow(fbdsDocumentData, fbdsDataRows, CASH_PREPAID);

        //Notification data
        if(fbdsDocumentData.getNotification() != null) {
        	ShipmentNotification notification = fbdsDocumentData.getNotification();
        	retrieveItemDescription(notification.getFbdsMessage(), fbdsDataRows);
        }
        return fbdsDataRows;
	}

	private void retriveTotalItemRow(FBDSDocument fbdsDocumentData, List<FBDSDataRow> fbdsDataRows) {
		String totalCharges = retrieveCharges(fbdsDocumentData.getTotalAmount(), fbdsDocumentData.getTotalChargeAmountTextLine2());
		fbdsDataRows.addAll(createFbdsDataRowPartition(fbdsDocumentData.getTotalPieces() != null ? fbdsDocumentData.getTotalPieces().toString() : null,
        	null, fbdsDocumentData.getTotalChargeAmountTextLine1(), null, retrieveDoubleString(fbdsDocumentData.getTotalWeight(), true),
        		null, totalCharges));
	}

	private String retrieveCharges(String totalAmount, String chargeAmountText) {
		StringJoiner totalCharges = new StringJoiner(":");
		if(StringUtils.isNotBlank(totalAmount)) {
			totalCharges.add(totalAmount);
		}
		if(StringUtils.isNotBlank(chargeAmountText)) {
			if(StringUtils.isBlank(totalAmount)) {
				totalCharges.add(" ");
			}
			totalCharges.add(chargeAmountText);
		}
		
		return totalCharges.toString();
	}

	private void retrieveCommodityRows(FBDSDocument fbdsDocumentData, List<FBDSDataRow> fbdsDataRows) {
		if(CollectionUtils.isNotEmpty(fbdsDocumentData.getCommodities())) {
			for(Commodity cmdt: fbdsDocumentData.getCommodities()) {
				fbdsDataRows.addAll(createFbdsDataRowPartition(cmdt.getPiecesCount() != null ? cmdt.getPiecesCount().toString() : null, 
					cmdt.getHazardousMtInd(), cmdt.getDescription(), null,
					retrieveDoubleString(cmdt.getWeightLbs(), true),
					getCommodityTariffRate(cmdt),
					retrieveDoubleString(cmdt.getAmount(), false)));
			}
		}
	}
	
	private String getCommodityTariffRate(Commodity cmdt) {
		String resultString = null;
		if(BooleanUtils.isTrue(cmdt.getMinimumChargeInd() && cmdt.getTariffsRate() != null)) {
			resultString = "M";
		}
		else {
			if(cmdt.getTariffsRate() != null && cmdt.getTariffsRate().doubleValue() > 0.0) {
				resultString = retrieveDoubleString(cmdt.getTariffsRate(), false);
			}
		}
		return resultString;
	}
	
	private String getAccesorialTariffRate(AccessorialService accessorial) {
		String resultString = null;
		if(BooleanUtils.isTrue(accessorial.getMinimumChargeInd()  && accessorial.getTariffsRate() != null)) {
			resultString = "M";
		}
		else {
			if(accessorial.getTariffsRate() != null && accessorial.getTariffsRate().doubleValue() > 0.0) {
				resultString = retrieveDoubleString(accessorial.getTariffsRate(), false);
			}
		}
		return resultString;
	}

	public boolean isMovrPro(FBDSDocument fbdsDocumentData) {
		return fbdsDocumentData.getBillClassCd() == BillClassCd.ASTRAY_FRT_SEGMENT;
	}
	
	public boolean isClearance(FBDSDocument fbdsDocumentData) {
		return StringUtils.isNotBlank(fbdsDocumentData.getMovrClearanceBillText());
	}
	
	private boolean isPartShort(FBDSDocument fbdsDocumentData) {
		return fbdsDocumentData.getDeliveryQualifierCd() != null && 
				fbdsDocumentData.getDeliveryQualifierCd() == DeliveryQualifierCd.PARTIAL_SHORT;
	}
	public boolean isPseg(FBDSDocument fbdsDocumentData) {
		return fbdsDocumentData.getBillClassCd() != null &&
				fbdsDocumentData.getBillClassCd() == BillClassCd.PARTIAL_SEGMENT;
	}
	
	public String retrieveDoubleString(Double amount, boolean weightMode) {
		if(amount == null) return null;
		BigDecimal result = BasicTransformer.toBigDecimal(amount);
		return retrieveBigDecimalString(result, weightMode);
	}
	
	public String retrieveBigDecimalString(BigDecimal amount, boolean weightMode) {
		if(amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return null;
		BigDecimal result = amount.setScale(2, RoundingMode.CEILING);
		
		if(weightMode) {
			result = result.stripTrailingZeros();
			if(result.scale() >= 1) {
				result = result.setScale(2, RoundingMode.CEILING);
			}
		}
		
		return result.abs().toPlainString();
	}

	private void retrieveItemDescription(String item, List<FBDSDataRow> fbdsDataRows) {
		if(StringUtils.isNotBlank(item)) {
        	fbdsDataRows.addAll(createFbdsDataRowPartition(null, null, item, null, null, null, null));
        }
	}

	private void retrieveListDescription(List<String> list, String additionalDescription, List<FBDSDataRow> fbdsDataRows) {
		if(CollectionUtils.isNotEmpty(list)) {
        	fbdsDataRows.addAll(createFbdsDataRowPartition(null, null, String.join(", ", list), additionalDescription,
        		null, null, null));
        }
	}

	private void retrieveShipmentChargesRow(FBDSDocument fbdsDocumentData, List<FBDSDataRow> fbdsDataRows, String chargeType) {
		if(fbdsDocumentData.getRatesAndCharges() != null &&
        		CollectionUtils.isNotEmpty(fbdsDocumentData.getRatesAndCharges().getShipmentCharges())) {
			ShipmentCharge shipmentCharge = fbdsDocumentData.getRatesAndCharges().getShipmentCharges().stream().filter(shmCharge ->
			StringUtils.equals(shmCharge.getChargeType(), chargeType)).findFirst().orElse(null);
			if(shipmentCharge != null) {
				fbdsDataRows.addAll(createFbdsDataRowPartition(null, null, shipmentCharge.getDescription(), null, null, null,
						retrieveCharges(retrieveBigDecimalString(shipmentCharge.getAmount(), false), shipmentCharge.getPaymentType())));
			}
		}
	}

	private FBDSDataRow createFbdsDataRow(String pcs, Boolean hazardousInd,
		String description, String additionalDescription, String weight, String rate, String charges) {
		FBDSDataRow row = new FBDSDataRow();
		row.setPiecesNum(pcs);
		row.setHazardousInd(hazardousInd != null && hazardousInd.booleanValue() ? "X" : null);
		row.setDescription(description);
		row.setAdditionalDescription(additionalDescription);
		row.setWeight(weight);
		row.setRate(rate);
		row.setCharges(charges);
		return row;
	}
	
	private List<FBDSDataRow> createFbdsDataRowPartition(String pcs, Boolean hazardousInd,
		String description, String additionalDescription, String weight, String rate, String charges) {
		
		List<String> descriptionList = splitByCellLimit(description, additionalDescription);
		List<FBDSDataRow> dataRowPartition = new ArrayList<>();
		
		if(CollectionUtils.isNotEmpty(descriptionList)) {
			for (int i = 0; i < descriptionList.size(); ++i) {
				String descriptionPart = descriptionList.get(i);
				
				if(i == 0) {
					if(StringUtils.contains(descriptionPart, additionalDescription)) {
						descriptionPart = StringUtils.remove(descriptionPart, additionalDescription);
					}
					dataRowPartition.add(createFbdsDataRow(pcs, hazardousInd, descriptionPart, additionalDescription, weight, rate, charges));
				}
				else {
					dataRowPartition.add(createFbdsDataRow(null, null, descriptionPart, null, null, null, null));
				}
			}	
		}
		else {
			dataRowPartition.add(createFbdsDataRow(pcs, hazardousInd, description, additionalDescription, weight, rate, charges));
		}
		
		return dataRowPartition;
	}
	
	private List<String> splitByCellLimit(String description, String additionalDescription) {
		List<String> resultingList = new ArrayList<>();
		
		String content = StringUtils.isNoneBlank(additionalDescription) ? additionalDescription + description : description;
		
		if(content.length() > DESCRIPTION_CHAR_LIMIT) {
			while (content.length() > 1) {
				int contentLength = content.length();
				int leftChar = 0;
		        int lastChar = contentLength - 1;
				float availableWidth = 320f;
				PdfFont fontType = StringUtils.isNotBlank(additionalDescription) ? BOLD_FONT : NORMAL_FONT;
				if(StringUtils.isBlank(content)) {
		        	break;
		        }
	            if (availableWidth < fontType.getWidth(content, NORMAL_FONT_SIZE)) {
	            	while (leftChar < contentLength && lastChar != leftChar) {
	    	            availableWidth -= fontType.getWidth(content.charAt(leftChar), NORMAL_FONT_SIZE);
	    	            if (availableWidth > 0) {
	    	                leftChar++;
	    	            } else {
	    	                break;
	    	            }
	    	        } 
	            }
				String concatContent = leftChar > 0 ? content.substring(0, leftChar) : content;
				String wrap = WordUtils.wrap(content, concatContent.length());
				List<String> newContent = Arrays.asList(wrap.split(System.lineSeparator()));

				//Wrapping can fail in some instances when character is too long
				//and have no spaces at all. Consider whitespace, comma, colon and semicolon
				String newString = newContent.get(0);
				if(newString.length() > concatContent.length()) {
					newString = correctStringOverflow(newContent.get(0), concatContent.length()-8);
				}

		        resultingList.add(newString);
		        content = content.replace(newString, StringUtils.EMPTY).trim();
			}
		} else {
			resultingList.add(content);
		}
		
        return resultingList;
	}

	private String correctStringOverflow(String wrap, int lengthMax) {
		String result = StringUtils.EMPTY;
		List<String> matches = Arrays.asList(wrap.split("(?<=;)|(?<=:)|(?=,)|(?=\\s)"));
		if(CollectionUtils.isEmpty(matches) || matches.get(0).length() > lengthMax) {
			int endLimit = wrap.length() > lengthMax ? lengthMax- 1 : wrap.length() - 1;
			return wrap.substring(0, endLimit);
		}
		for (String match : matches) {
			if(result.length() + match.length() <= lengthMax) {
				result += match;
			}
			else {
				break;
			}
		}

		return result;
	}

	public Cell generateEmptyCell(int rowspan, int colspan, boolean drawBottomLine) {
		Cell cell = new Cell(rowspan,colspan);
		cell.add(new Paragraph("\n").setFontSize(NORMAL_FONT_SIZE));
		cell.setBorderTop(Border.NO_BORDER);
		if(!drawBottomLine) {
			cell.setBorderBottom(Border.NO_BORDER);
		}
		cell.setPaddingBottom(-0.4f);
	    cell.setPaddingTop(-0.2f);
		return cell;
	}
	
	public int countAdditionalLineOffset(FBDSDocument fbdsDocumentData) {
		int shipperConsigneeOverflow = 0;
		List<String> consigneeData = transformInterfaceAccountDataToList(fbdsDocumentData, fbdsDocumentData.getConsignee());
		List<String> shipperData = transformInterfaceAccountDataToList(fbdsDocumentData, fbdsDocumentData.getShipper());
		List<String> shipperNumbers = Arrays.asList(fbdsDocumentData.getShipperNbr().split("\n"));
		
		int shipperlines = Math.max(shipperData.size(), shipperNumbers.size());
		if(shipperlines + consigneeData.size() > 7) {
			shipperConsigneeOverflow = shipperlines + consigneeData.size() - 7;
		}
		
		return shipperConsigneeOverflow + 1;
	}

	private List<String> transformInterfaceAccountDataToList(
		FBDSDocument fbdsDocumentData,
		AsMatchedParty account) {
		List<String> accountData = new ArrayList<>();
		if (account != null) {
			if (account.getTypeCd() == MatchedPartyTypeCd.CONS) {
			if(StringUtils.isNotBlank(fbdsDocumentData.getConsigneeMessage())) accountData.add(fbdsDocumentData.getConsigneeMessage());
			if(StringUtils.isNotBlank(fbdsDocumentData.getCollectorOfCustomMessage())) accountData.add(fbdsDocumentData.getCollectorOfCustomMessage());	
		}
		if(StringUtils.isNotBlank(account.getName1())) accountData.add(account.getName1());
		if(StringUtils.isNotBlank(account.getName2())) accountData.add(account.getName2());
		if(StringUtils.isNotBlank(account.getAddress())) accountData.add(account.getAddress());
		if(StringUtils.isNotBlank(account.getCity())) accountData.add(account.getCity());
		}
		return accountData;
	}
	
	public boolean isFbds() {
		return reportType == DocumentFormTypeCd.FBDS;
	}

	public Cell getShipperNumbersCell(String value, TextAlignment textAlignment, FbdsVersionCd version_DR ) {
	    final Cell cell = new Cell(1, 28);
	    if(version_DR == FbdsVersionCd.BILINGUAL) {
	    	final Paragraph titleParagraph = new Paragraph().add(translatorUtil.getFrenchTranslation("Shipper Numbers")).setFont(BOLD_FONT)
					.setFontSize(NORMAL_FONT_SIZE_FRENCH).setTextAlignment(textAlignment);
	  	    final Paragraph titleParagraphFrench = new Paragraph().add("Shipper Numbers" +"\n").setFont(NORMAL_FONT)
					.setFontSize(SMALL_FONT_SIZE_FRENCH).setTextAlignment(textAlignment);
	  	    
	        cell.add(titleParagraph);
		    if(value != null) {
		    	Text text = new Text(value.replace('\n', '\t')).setFont(NORMAL_FONT)
		    			.setFontSize(SMALL_FONT_SIZE_FRENCH).setTextAlignment(textAlignment);
		    	titleParagraphFrench.add(text);
		    	
		    }
		    cell.add(titleParagraphFrench);
		    cell.setPaddingBottom(-1f).setPaddingTop(-1f);
	    }
	    else {	    	
	    	final Paragraph paragraph = new Paragraph();
	    	String titleString = "SHIPPER NUMBERS";
	    	String title = (version_DR==FbdsVersionCd.FRENCH)?translatorUtil.getFrenchTranslation(firstLetterCaps(titleString)).toUpperCase():titleString;
	    	title += "\t";
	    	Text text = new Text(title).setFont(BOLD_FONT)
	    			.setFontSize(NORMAL_FONT_SIZE).setTextAlignment(textAlignment);
	    	
	    	paragraph.add(text);
	    	
	    	if(value != null) {
	    		text = new Text(value.replace('\n', '\t')).setFont(NORMAL_FONT)
	    				.setFontSize(NORMAL_FONT_SIZE).setTextAlignment(textAlignment);
	    		paragraph.setTextAlignment(TextAlignment.JUSTIFIED);
	    		paragraph.add(text);
	    	}
	    	cell.add(paragraph);
	    }
	    
	    return cell;

	}
	
	public Cell addCheckBoxSectionCell(PdfDeliveryReceiptUtils pdfDeliveryReceiptUtils, FbdsVersionCd version_DR) throws IOException {
		Cell cell = new Cell(1,(version_DR==FbdsVersionCd.BILINGUAL)?11:9);
		
		Table table = new Table(UnitValue.createPercentArray(new float[] {1,4,1,5})).useAllAvailableWidth();
		if(version_DR!=FbdsVersionCd.DEFAULT)
		{
			if(version_DR!=FbdsVersionCd.FRENCH)
				table = new Table(UnitValue.createPercentArray(new float[] {0,8,0,8})).useAllAvailableWidth();
			else
				table = new Table(UnitValue.createPercentArray(new float[] {1,4,1,4})).useAllAvailableWidth();
			table.addCell(pdfDeliveryReceiptUtils.getCheckboxCell(false, 1, 1, version_DR));
			table.addCell(pdfDeliveryReceiptUtils.getCheckboxValueCell("Inside Delivery", TextAlignment.LEFT, version_DR, 1, 1));
			table.addCell(pdfDeliveryReceiptUtils.getCheckboxCell(false, 1, 1, version_DR));
			table.addCell(pdfDeliveryReceiptUtils.getCheckboxValueCell("Liftgate Service", TextAlignment.LEFT, version_DR, 1, 1));
			table.addCell(pdfDeliveryReceiptUtils.getCheckboxCell(false, 1, 1, version_DR));
			table.addCell(pdfDeliveryReceiptUtils.getCheckboxValueCell("Residential Delivery", TextAlignment.LEFT, version_DR, 1, 1));
			table.addCell(pdfDeliveryReceiptUtils.getCheckboxCell(false, 1, 1, version_DR));
			table.addCell(pdfDeliveryReceiptUtils.getCheckboxValueCell("Construction Util.", TextAlignment.LEFT, version_DR, 1, 1));
		}
		else{
			table.addCell(pdfDeliveryReceiptUtils.getCheckboxCell(false, 1, 1, version_DR));
			table.addCell(pdfDeliveryReceiptUtils.getCheckboxValueCell("INSIDE\nDELIVERY", TextAlignment.LEFT, 1, 1));
			table.addCell(pdfDeliveryReceiptUtils.getCheckboxCell(false, 1, 1, version_DR));
			table.addCell(pdfDeliveryReceiptUtils.getCheckboxValueCell("LIFTGATE\nSERVICE", TextAlignment.LEFT, 1, 1));
			table.addCell(pdfDeliveryReceiptUtils.getCheckboxCell(false, 1, 1, version_DR));
			table.addCell(pdfDeliveryReceiptUtils.getCheckboxValueCell("RESIDENTIAL\nDELIVERY", TextAlignment.LEFT, 1, 1));
			table.addCell(pdfDeliveryReceiptUtils.getCheckboxCell(false, 1, 1, version_DR));
			table.addCell(pdfDeliveryReceiptUtils.getCheckboxValueCell("CONSTRUCTION\n/UTIL", TextAlignment.LEFT, 1, 1));		
		}
		cell.setPaddingBottom(-0.3f);
        cell.add(table);
        
        
        return cell;
	}


	 // Generic function to construct a new LinkedList from ArrayList
	    public static <T> List<T> getInstance(List<T> arrayList)
	    {
	        List<T> linkedList = Lists.newLinkedList();
	        linkedList.addAll(arrayList);
	     
	        return linkedList;
	    }
	    
		public List<List<FBDSDataRow>> partitionFbdsDataRows(List<FBDSDataRow> fbdsDataRowsTotal) {
			
			List<List<FBDSDataRow>> partitions = new ArrayList<>();
			int firstPageLimit = fbdsDataRowLimitFirstPage;
			List<FBDSDataRow> firstPageList = new ArrayList<>();
			int middlePageLimit = fbdsDataRowLimitMiddlePage;
			List<List<FBDSDataRow>> middlePageList = getInstance(new ArrayList<>());
			int lastPageLimit = fbdsDataRowLimitLastPage;
			List<FBDSDataRow> lastPageList = new ArrayList<>();
			//Check single page limit
			if(fbdsDataRowsTotal.size() > fbdsDataRowLimitSinglePage) {
				for (int i = 0; i < fbdsDataRowsTotal.size(); ++i) {
					FBDSDataRow fbdsDataRow = fbdsDataRowsTotal.get(i);
					if(i < firstPageLimit) {
						if(i > fbdsDataRowLimitSinglePage && i == fbdsDataRowsTotal.size()-1) {
							inBetweenSingleAndFirstPageLimit = true;
						}
						firstPageList.add(fbdsDataRow);
					} else {
						middlePageList = getInstance(ListUtils.partition(fbdsDataRowsTotal.subList(i, fbdsDataRowsTotal.size()), middlePageLimit));
						break;
					}
				}
				if(inBetweenSingleAndFirstPageLimit) {
					//An edge case when we have more rows to fit into a single page, but they all fit to first page.
					//So in order to avoid last page being empty we take some rows from first page.
					lastPageList = firstPageList.subList(firstPageList.size()-5, firstPageList.size());
					firstPageList = firstPageList.subList(0, firstPageList.size()-5);
				}
				if(CollectionUtils.isNotEmpty(middlePageList)) {
					List<FBDSDataRow> lastMiddlePage = middlePageList.get(middlePageList.size() - 1);
					middlePageList.remove(middlePageList.size() - 1);
					
					if(lastMiddlePage.size() <= lastPageLimit) {
						lastPageList = lastMiddlePage;
					}
					else {
						List<List<FBDSDataRow>> remainingPageListPartition = getInstance(ListUtils.partition(lastMiddlePage, lastPageLimit));
						lastPageList = remainingPageListPartition.get(remainingPageListPartition.size() - 1);
						remainingPageListPartition.remove(remainingPageListPartition.size() - 1);

						middlePageList.addAll(ListUtils.partition(remainingPageListPartition.stream().flatMap(List::stream).collect(Collectors.toList()), middlePageLimit));
					}
				}
				
				partitions.add(firstPageList);
				if(CollectionUtils.isNotEmpty(middlePageList)) {
					partitions.addAll(middlePageList);
				}
				if(CollectionUtils.isNotEmpty(lastPageList)) {
					partitions.add(lastPageList);
				}
				
			}
			else {
				partitions.add(fbdsDataRowsTotal);		
			}
			
			return partitions;
		}

		public String buildAdvanceCarrierCell(FBDSDocument fbdsDocumentData) {
			String result = StringUtils.EMPTY;
			if(fbdsDocumentData.getAdvanceCarrier() != null) {
				AdvanceBeyondCarrier advanceCarrier = fbdsDocumentData.getAdvanceCarrier();
				if(fbdsDocumentData.getAdvancedRevenueAmount() != null)
					result = retrieveBigDecimalString(fbdsDocumentData.getAdvancedRevenueAmount(), false) + '\n';
				result += Stream.of(advanceCarrier.getCarrierScacCd(), advanceCarrier.getCarrierProNbr(),
					advanceCarrier.getCarrierPickupDate())
				          .filter(s -> s != null && !s.isEmpty())
				          .collect(Collectors.joining(","));
			}
			
			return result;
		}

		public Cell getFooter(FbdsVersionCd version_DR) {
		    final Cell cell = new Cell(1, 28);
	  	    final Paragraph paragraph = new Paragraph();
	    	Text text = new Text("SUBJECT TO TERMS AND CONDITIONS HEREIN, AND TARIFF CNWY-199 IN EFFECT ON DATE OF SHIPMENT.").setFont(NORMAL_FONT)
	    			.setFontSize(NORMAL_FONT_SIZE).setTextAlignment(TextAlignment.LEFT);
	    	if(version_DR == FbdsVersionCd.FRENCH) {
	    		text = new Text("SOUS RÉSERVE DES MODALITÉS ET CONDITIONS CI-DESSOUS ET DU TARIF CNWY-199 EN VIGUEUR À LA DATE D'EXPÉDITION.").setFont(NORMAL_FONT)
		    			.setFontSize(NORMAL_FONT_SIZE).setTextAlignment(TextAlignment.LEFT);
	    	}
	    	paragraph.add(text);
		    cell.add(paragraph);

		    return cell;
		}

		public Table buildErrorLogHeader(String routeName) {
		    
		    String reportTitle = ERROR_RPT_TITLE;
		    if (StringUtils.isNotBlank(routeName)) {
		        reportTitle = reportTitle + " - " + routeName;
		    }		    
			Table table = new Table(2);
	        table.useAllAvailableWidth();
	        table.addCell(new Cell(1, 1).add(new Paragraph().add(reportTitle).setFont(BOLD_FONT)
	  		  .setFontSize(22).setTextAlignment(TextAlignment.LEFT)).setBorder(Border.NO_BORDER));
	        table.addCell(new Cell(1, 1).add(new Paragraph().add(LocalDate.now()
	        	.format(DateTimeFormatter.ofPattern("dd/MM/YYYY")).toString()).setFont(NORMAL_FONT)
	  		  .setFontSize(14).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));
			return table;
		}

		public Paragraph buildErrorLogMessage(String message, boolean isBold) {
			Paragraph paragraph = new Paragraph().add(message).setFont(isBold ? NORMAL_FONT : BOLD_FONT)
	  		  .setFontSize(14).setTextAlignment(TextAlignment.LEFT);
			return paragraph;
		}
}
