package com.xpo.ltl.shipment.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.io.Resources;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.AreaBreakType;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.BillClassCd;
import com.xpo.ltl.api.shipment.v2.DataValidationError;
import com.xpo.ltl.api.shipment.v2.DeliveryQualifierCd;
import com.xpo.ltl.api.shipment.v2.DocumentFormTypeCd;
import com.xpo.ltl.api.shipment.v2.FBDSDocument;
import com.xpo.ltl.api.shipment.v2.FbdsVersionCd;
import com.xpo.ltl.api.shipment.v2.MatchedPartyTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentDetailCd;
import com.xpo.ltl.shipment.pdf.FBDSDataRow;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.ShmShipmentEagerLoadPlan;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.enums.PdfPageTypeCd;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.FBDSCopyBillUtil;
import com.xpo.ltl.shipment.service.util.FBDSEngToFrenchTranslatorUtil;
import com.xpo.ltl.shipment.service.util.PdfDeliveryReceiptUtils;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;

@RequestScoped
@LogExecutionTime
public class PdfDeliveryReceiptImpl {
	
	private static final Log LOGGER = LogFactory.getLog(CreateFbdsDocumentsImpl.class);
    // public static final String IMG_DEST = "/xpo_logo_png.png";
	public static final String IMG_DEST = "/XPO_logo_new.png";
	public static final String CB_FONT_DEST = "/wingding_0.ttf";
    public static final String lineSeparator = " _______________ ";
	
    public static final int FBDS_DATA_ROW_LIMIT_SINGLE_PAGE = 10;
    public static final int FBDS_DATA_ROW_LIMIT_SINGLE_PAGE_FR = 5;
    public static final int FBDS_DATA_ROW_LIMIT_FIRST_PAGE = 16;
    public static final int FBDS_DATA_ROW_LIMIT_FIRST_PAGE_FR =  5;
    public static final int FBDS_DATA_ROW_LIMIT_MIDDLE_PAGE = 25;
    public static final int FBDS_DATA_ROW_LIMIT_LAST_PAGE = 18;
    public static final int COPY_BILL_DATA_ROW_LIMIT = 42;
    public static final int MARGIN_LEFT = 15;
    public static final int MARGIN_RIGHT = 15;
    public static final int MARGIN_TOP = 15;
    public static final int MARGIN_TOP_FRENCH = 4;
    public static final int MARGIN_BOTTOM_FRENCH = 3;
    public static final int MARGIN_MID_FRENCH = 33;
    public static final int MARGIN_BOTTOM = 5;
        
    public static final float NORMAL_FONT_SIZE = 7.5f;
    public static final float LARGE_FONT_SIZE = 10;
    public static final float TITLE_FONT_SIZE = 10;
    
    private int articlesAndRemarksGridLimit;
    
    @Inject
    private AppContext appContext;
    
    @Inject
    private ShmShipmentSubDAO shmShipmentSubDAO;
    
	private static final FBDSEngToFrenchTranslatorUtil translatorUtil = new FBDSEngToFrenchTranslatorUtil();


    public byte[] generateFBDSDocumentPdf(
    	final FBDSDocument fbdsDocumentData,
    	final FbdsVersionCd version_DR,
    	final DocumentFormTypeCd documentFormTypeCd,
    	final TransactionContext txnContext,
    	final EntityManager entityManager) throws ServiceException {

    	PdfDocument pdfDoc = null;
    	Document document = null;
    	ByteArrayOutputStream out = null;
    	articlesAndRemarksGridLimit = (version_DR!=FbdsVersionCd.BILINGUAL) ? FBDS_DATA_ROW_LIMIT_SINGLE_PAGE : FBDS_DATA_ROW_LIMIT_SINGLE_PAGE_FR;
        try {
        	out = new ByteArrayOutputStream();
	        pdfDoc = new PdfDocument(new PdfWriter(out));
	        pdfDoc.setDefaultPageSize(PageSize.LETTER);
	      //Get font and image resources
	        byte[] fontContent = IOUtils.toByteArray(Resources.getResource(appContext.getFontsResourcePath() + CB_FONT_DEST));
			byte[] imageContent = IOUtils.toByteArray(Resources.getResource(appContext.getImagesResourcePath() + IMG_DEST));
			PdfDeliveryReceiptUtils pdfDeliveryReceiptUtils = new PdfDeliveryReceiptUtils(documentFormTypeCd,
					(version_DR==FbdsVersionCd.BILINGUAL)? FBDS_DATA_ROW_LIMIT_SINGLE_PAGE_FR : FBDS_DATA_ROW_LIMIT_SINGLE_PAGE,
					(version_DR==FbdsVersionCd.BILINGUAL)? FBDS_DATA_ROW_LIMIT_FIRST_PAGE_FR : FBDS_DATA_ROW_LIMIT_FIRST_PAGE,
							FBDS_DATA_ROW_LIMIT_MIDDLE_PAGE,FBDS_DATA_ROW_LIMIT_LAST_PAGE);
			articlesAndRemarksGridLimit = articlesAndRemarksGridLimit - Math.abs(pdfDeliveryReceiptUtils.countAdditionalLineOffset(fbdsDocumentData));
			pdfDeliveryReceiptUtils.initializeFonts(fontContent, imageContent, txnContext, true);
			List<FBDSDataRow> fbdsDataRowsTotal = pdfDeliveryReceiptUtils.buildFbdsDataRows(fbdsDocumentData, documentFormTypeCd);
			document = new Document(pdfDoc);
			float heightDefault = PageSize.LETTER.getHeight();
	        if(version_DR==FbdsVersionCd.BILINGUAL) {
	        	heightDefault -=  (MARGIN_TOP_FRENCH + MARGIN_BOTTOM_FRENCH);
		        document.setMargins(MARGIN_TOP_FRENCH, MARGIN_RIGHT, MARGIN_BOTTOM_FRENCH, MARGIN_LEFT);
	    	}
	        
	        else {
	        	heightDefault -=  (MARGIN_TOP + MARGIN_BOTTOM);
	        	document.setMargins(MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM, MARGIN_LEFT);
	 
	        }
	        final float height = heightDefault;
	        final float width = PageSize.LETTER.getWidth()- MARGIN_LEFT - MARGIN_RIGHT;
	        
	        if(documentFormTypeCd == DocumentFormTypeCd.FBDS) {
	        	List<List<FBDSDataRow>> fbdsDataRowsPartitions = pdfDeliveryReceiptUtils
	        			.partitionFbdsDataRows(fbdsDataRowsTotal);
	        	int pageTotal = fbdsDataRowsPartitions.size();
	        	for (int i = 0; i < fbdsDataRowsPartitions.size(); ++i) {
	        		PdfPageTypeCd pageTypeCd = PdfPageTypeCd.SINGLE;
	        		if(i == 0 && fbdsDataRowsPartitions.size() > 1) {
	        			pageTypeCd = PdfPageTypeCd.FIRST;
	        		}
	        		else if(i != 0 && i < fbdsDataRowsPartitions.size() - 1) {
	        			pageTypeCd = PdfPageTypeCd.MIDDLE;
	        		}
	        		else if(i != 0 && i == fbdsDataRowsPartitions.size() - 1) {
	        			pageTypeCd = PdfPageTypeCd.LAST;
	        		}
	        		List<FBDSDataRow> fbdsDataRows = fbdsDataRowsPartitions.get(i);
	        		document.add(buildDeliveryReceipt(fbdsDocumentData, version_DR, (version_DR!=FbdsVersionCd.DEFAULT)?"CONTRACTUAL COPY":"XPO CONTRACTUAL COPY", txnContext, pdfDoc,
	        				height, width, pdfDeliveryReceiptUtils, pageTotal, i, fbdsDataRows, pageTypeCd, false));
//	        			document.add(new LineSeparator(new CustomDashedLineSeparator()));
	        		document.add(buildDeliveryReceipt(fbdsDocumentData, version_DR, "CUSTOMER COPY", txnContext, pdfDoc,
	        				height, width, pdfDeliveryReceiptUtils, pageTotal, i, fbdsDataRows, pageTypeCd, true));
	        	}
	        }
	        else {
	        	List<List<FBDSDataRow>> fbdsDataRowsPartitions = ListUtils.partition(fbdsDataRowsTotal, COPY_BILL_DATA_ROW_LIMIT);
	        	int pageTotal = fbdsDataRowsPartitions.size();
	        	for (int i = 0; i < fbdsDataRowsPartitions.size(); ++i) {
	        		List<FBDSDataRow> fbdsDataRows = fbdsDataRowsPartitions.get(i);
	        		document.add(buildDeliveryReceipt(fbdsDocumentData, FbdsVersionCd.DEFAULT, null, txnContext, pdfDoc,
	        				height, width, pdfDeliveryReceiptUtils, pageTotal, i, fbdsDataRows, PdfPageTypeCd.SINGLE, false));
	        		if(i < fbdsDataRowsPartitions.size() -1) {
	        			document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
	        		}
	        	}
	        }
	        document.close();
	        pdfDoc.close();
	        
	        return out.toByteArray();
        }
        catch (Exception e) {
        	String errorMessage = e.getMessage();
        	try {
        	if(document != null && document.getPdfDocument().getNumberOfPages() != 0) document.close();
        	if(pdfDoc != null && pdfDoc.getNumberOfPages() != 0) pdfDoc.close();
        	if(out != null) out.close();
			} catch (Exception e1) {
				errorMessage = e1.getMessage() + " " + e.getMessage();
			}
        	throw com.xpo.ltl.api.exception.ExceptionBuilder
			.exception(com.xpo.ltl.api.exception.ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e)
			.moreInfo("Unable to generate PDF: ", errorMessage)
			.build();
        }
    }

	private Table buildDeliveryReceipt(
		final FBDSDocument fbdsDocumentData,
		final FbdsVersionCd version_DR,
		final String copyType,
		final TransactionContext txnContext,
		PdfDocument pdfDoc,
		final float height,
		final float width,
		PdfDeliveryReceiptUtils pdfDeliveryReceiptUtils,
		int pageTotal,
		int pageNum,
		List<FBDSDataRow> fbdsDataRows,
		final PdfPageTypeCd pageTypeCd,
		final boolean addTopMargin) throws Exception {
		Table body = new Table(1).useAllAvailableWidth();
		body.setMinHeight(height/2);
		body.addCell(new Cell().setBorder(Border.NO_BORDER)
			.add(createCustomerInfoTable(pdfDoc, version_DR, width, pageNum+1, pageTotal,
				fbdsDocumentData, fbdsDataRows, pdfDeliveryReceiptUtils, copyType, pageTypeCd, txnContext)));
		if(addTopMargin) {
			body.setMarginTop((version_DR == FbdsVersionCd.BILINGUAL)?MARGIN_TOP+5:MARGIN_TOP);
		}
		
		body.setFixedLayout();
		return body;
	}
	

	private Table createDeliveryReceiptHeader(final PdfDocument pdfDoc, FbdsVersionCd version_DR, int pageNum, int pagesTotal, final PdfDeliveryReceiptUtils pdfDeliveryReceiptUtils,
		final String proNbr, String copyType, TransactionContext txnContext) throws Exception {
		Table table = null; 
		String cleanProNbr = verifyProNbr(proNbr);
		if(version_DR == FbdsVersionCd.BILINGUAL)
		{
			table = new Table(UnitValue.createPercentArray(new float[] {2,2,1,3,4,2})).useAllAvailableWidth();
			String docTypeTitle = pdfDeliveryReceiptUtils.isFbds() ? "DELIVERY RECEIPT" : "COPY BILL";
			table.addCell(pdfDeliveryReceiptUtils.getXPOLogoPngFrench(1, 1, docTypeTitle).setBorder(Border.NO_BORDER));
			if(copyType != null) {
				table.addCell(pdfDeliveryReceiptUtils.getHeaderCellFrench(copyType, null, TextAlignment.LEFT, 1, 1, 8).setPaddingTop(5f).setBorder(Border.NO_BORDER));
			}
			table.addCell(pdfDeliveryReceiptUtils.getHeaderCellFrench(null, "PAGE " + pageNum +" OF "+ pagesTotal, TextAlignment.LEFT, 1, 1, 6).setPaddingTop(5f).setBorder(Border.NO_BORDER)
						);
			table.addCell(pdfDeliveryReceiptUtils.createBarcode(null, "CWQC", "CWQC", 1, 1, 15f, 1.1f, null, pdfDoc).setPaddingLeft(12f).setHorizontalAlignment(HorizontalAlignment.CENTER).setPaddingTop(4f).setBorder(Border.NO_BORDER));
			table.addCell(pdfDeliveryReceiptUtils.createBarcode(null, ProNumberHelper.toNineDigitPro(cleanProNbr, txnContext), " ", 1, 1, 22f, 1.2f, 0.01f, pdfDoc).setPaddingLeft(5f).setPaddingTop(4f).setBorder(Border.NO_BORDER)
					);
			table.addCell(pdfDeliveryReceiptUtils.getHeaderCellFrench("PRO NUMBER", proNbr, TextAlignment.LEFT, 1, 1, 8).setPaddingLeft(5f).setBorder(Border.NO_BORDER));
		}
		
		else
		{
			table = new Table(UnitValue.createPercentArray(new float[] {3,2,3,1,4,2})).useAllAvailableWidth();
			table.addCell(pdfDeliveryReceiptUtils.getXPOLogoPng(1, 1).setBorder(Border.NO_BORDER));
			table.addCell(pdfDeliveryReceiptUtils.createBarcode(null, (version_DR == FbdsVersionCd.FRENCH)?"CWQC":"CNWY", (version_DR == FbdsVersionCd.FRENCH)?"CWQC":"CNWY", 1, 1, 15f, 1.1f, null, pdfDoc).setBorder(Border.NO_BORDER));
        
			String docTypeTitle = pdfDeliveryReceiptUtils.isFbds() ? checkDRlanguage(version_DR,"DELIVERY RECEIPT", pdfDeliveryReceiptUtils) 
																   : checkDRlanguage(version_DR,"COPY BILL", pdfDeliveryReceiptUtils);
        
			if(copyType != null) {
				copyType = checkDRlanguage(version_DR, copyType, pdfDeliveryReceiptUtils);
				table.addCell(pdfDeliveryReceiptUtils.getHeaderCell(docTypeTitle, copyType, TextAlignment.CENTER, 1, 1, 7).setBorder(Border.NO_BORDER));
				table.addCell(pdfDeliveryReceiptUtils.getHeaderCell("PAGE ", pageNum +" OF "+ pagesTotal, TextAlignment.CENTER, 1, 1, 7)
						.setBorder(Border.NO_BORDER));
			}
			else {
				table.addCell(pdfDeliveryReceiptUtils.getHeaderCell(docTypeTitle, "PAGE " + pageNum +" OF "+ pagesTotal, TextAlignment.CENTER, 1, 2, 8)
						.setBorder(Border.NO_BORDER));
			}
			table.addCell(pdfDeliveryReceiptUtils.createBarcode(null, ProNumberHelper.toNineDigitPro(cleanProNbr, txnContext), " ", 1, 1, 22f, 1.2f, 0.01f, pdfDoc)
					.setBorder(Border.NO_BORDER));
			table.addCell(pdfDeliveryReceiptUtils.getHeaderCell(checkDRlanguage(version_DR,"PRO NUMBER", pdfDeliveryReceiptUtils), proNbr, TextAlignment.CENTER, 1, 1, 8).setBorder(Border.NO_BORDER));
		}
        return table;
	}
    
	private String checkDRlanguage(final FbdsVersionCd version_DR, final String header, final PdfDeliveryReceiptUtils pdfDeliveryReceiptUtils ) {

		return (version_DR==FbdsVersionCd.FRENCH)? translatorUtil.getFrenchTranslation(pdfDeliveryReceiptUtils.firstLetterCaps(header)).toUpperCase():header;
	}

	private Table createCustomerInfoTable(PdfDocument pdfDoc, FbdsVersionCd version_DR, float width, int pageNum, int pagesTotal, FBDSDocument fbdsDocumentData,
		List<FBDSDataRow> fbdsDataRows, PdfDeliveryReceiptUtils pdfDeliveryReceiptUtils, String copyType, PdfPageTypeCd pageTypeCd, TransactionContext txnContext) throws Exception {
		
        Table table = new Table(UnitValue.createPercentArray(new float[] {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1})).useAllAvailableWidth();
        Cell cell = new Cell(1,28);
        cell.add(createDeliveryReceiptHeader(pdfDoc, version_DR, pageNum, pagesTotal, pdfDeliveryReceiptUtils, fbdsDocumentData.getProNbr(), copyType, txnContext));
        table.addCell(cell.setPaddingBottom((version_DR==FbdsVersionCd.BILINGUAL)?-4f:0));
        if(pageTypeCd == PdfPageTypeCd.SINGLE || pageTypeCd == PdfPageTypeCd.FIRST) {
        	addBasicShipmentInfo(fbdsDocumentData, version_DR, pdfDeliveryReceiptUtils, table);
            
            addCustomerInfo(pdfDoc, version_DR, fbdsDocumentData, pdfDeliveryReceiptUtils, table);
            
            table.addCell(pdfDeliveryReceiptUtils.getShipperNumbersCell(StringUtils.isNotBlank(fbdsDocumentData.getShipperNbr()) ?
            		fbdsDocumentData.getShipperNbr() : " ", TextAlignment.LEFT , version_DR));
        }        

        addArticlesAndRemarksSection(table, version_DR, fbdsDocumentData, fbdsDataRows, pdfDeliveryReceiptUtils, pageTypeCd);        
        
        if(pageTypeCd == PdfPageTypeCd.SINGLE || pageTypeCd == PdfPageTypeCd.LAST) {
        	addSignatureSection(table, version_DR, pdfDeliveryReceiptUtils);
        	table.addCell(pdfDeliveryReceiptUtils.getFooter(version_DR));
        }
        
        return table;
	}

	private void addBasicShipmentInfo(
		FBDSDocument fbdsDocumentData,
		FbdsVersionCd version_DR,
		PdfDeliveryReceiptUtils pdfDeliveryReceiptUtils,
		Table table) {
		table.addCell(pdfDeliveryReceiptUtils.getTitleValueCell(checkDRlanguage(version_DR,"EQUIP NUMBER", pdfDeliveryReceiptUtils), version_DR, fbdsDocumentData.getEquipmentNbr(), TextAlignment.CENTER, TextAlignment.CENTER, 1, 5));
        table.addCell(pdfDeliveryReceiptUtils.getTitleValueCell(checkDRlanguage(version_DR,"DATE", pdfDeliveryReceiptUtils), version_DR, fbdsDocumentData.getPickupDate(), TextAlignment.CENTER, TextAlignment.CENTER, 1, 3));
        table.addCell(pdfDeliveryReceiptUtils.getTitleValueCell(checkDRlanguage(version_DR,"ORIGIN", pdfDeliveryReceiptUtils), version_DR, fbdsDocumentData.getOriginSic(), TextAlignment.CENTER, TextAlignment.CENTER, 1, 3));
        table.addCell(pdfDeliveryReceiptUtils.getTitleValueCell(checkDRlanguage(version_DR,"DESTINATION", pdfDeliveryReceiptUtils), version_DR, fbdsDocumentData.getDestinationSic(), TextAlignment.CENTER, TextAlignment.CENTER, 1, 3));
        table.addCell(pdfDeliveryReceiptUtils.getTitleValueCell(checkDRlanguage(version_DR,"OUR REVENUE", pdfDeliveryReceiptUtils), version_DR,
        	fbdsDocumentData.getCtsRevenueAmount() != null ? pdfDeliveryReceiptUtils.
        		retrieveBigDecimalString(fbdsDocumentData.getCtsRevenueAmount(), false) : "\n",
        	TextAlignment.CENTER, TextAlignment.CENTER, 1, 4));
        table.addCell(pdfDeliveryReceiptUtils.getTitleValueCell(checkDRlanguage(version_DR,"ADVANCE", pdfDeliveryReceiptUtils), version_DR, pdfDeliveryReceiptUtils.buildAdvanceCarrierCell(fbdsDocumentData),
        	TextAlignment.CENTER, TextAlignment.CENTER, 1, 4));
        table.addCell(pdfDeliveryReceiptUtils.getTitleValueCell(checkDRlanguage(version_DR,"BEYOND", pdfDeliveryReceiptUtils), version_DR,
        	fbdsDocumentData.getBeyondRevenueAmount() != null ? pdfDeliveryReceiptUtils.
        		retrieveBigDecimalString(fbdsDocumentData.getBeyondRevenueAmount(), false) : "\n",
        		TextAlignment.CENTER, TextAlignment.CENTER, 1, 3));
        table.addCell(pdfDeliveryReceiptUtils.getTitleValueCell(checkDRlanguage(version_DR,"ROUTE", pdfDeliveryReceiptUtils), version_DR, fbdsDocumentData.getDeliveryRoute(),
        	TextAlignment.CENTER, TextAlignment.CENTER, 1, 3));
        
        
        
	}

	private void addCustomerInfo(
		PdfDocument pdfDoc,
		FbdsVersionCd version_DR,
		FBDSDocument fbdsDocumentData,
		PdfDeliveryReceiptUtils pdfDeliveryReceiptUtils,
		Table table) {
		table.addCell(pdfDeliveryReceiptUtils.getInterfaceAccountCell(fbdsDocumentData, version_DR, MatchedPartyTypeCd.CONS, TextAlignment.LEFT, TextAlignment.LEFT, 1, 8));
		table.addCell(pdfDeliveryReceiptUtils.getInterfaceAccountCell(fbdsDocumentData, version_DR, MatchedPartyTypeCd.SHPR, TextAlignment.LEFT, TextAlignment.LEFT, 1, 10));
		table.addCell(pdfDeliveryReceiptUtils.getInterfaceAccountCell(fbdsDocumentData, version_DR,MatchedPartyTypeCd.BILL_TO_INB, TextAlignment.LEFT, TextAlignment.LEFT, 1, 7));
        table.addCell(pdfDeliveryReceiptUtils.getTitleValueCell((version_DR!=FbdsVersionCd.DEFAULT)? checkDRlanguage(version_DR,"APPOINTMENT", pdfDeliveryReceiptUtils):"APPT", version_DR, pdfDeliveryReceiptUtils.buildAppointmentCell(fbdsDocumentData), null, TextAlignment.LEFT, 1, 5));
	}

	private void addSignatureSection(Table table, FbdsVersionCd version_DR, PdfDeliveryReceiptUtils pdfDeliveryReceiptUtils) throws IOException {
		float bottomPadding = (version_DR==FbdsVersionCd.BILINGUAL)?-2f:0;
		final String receivedLine = (version_DR!=FbdsVersionCd.DEFAULT)?lineSeparator:StringUtils.EMPTY;
		table.addCell(pdfDeliveryReceiptUtils.getTitleValueCellwithEmptyParagraph("RECEIVED",  receivedLine +    "PIECES ABOVE.\n"
        		+ "DESCRIBED FREIGHT\nIN GOOD ORDER\nEXCEPT AS NOTED.", version_DR, TextAlignment.LEFT, TextAlignment.RIGHT, 2, (version_DR==FbdsVersionCd.BILINGUAL)?2:3, false).setPaddingBottom(bottomPadding));
		table.addCell(pdfDeliveryReceiptUtils.getShrinkWrapIntactCell(1, 6, version_DR).setPaddingBottom(bottomPadding));
        table.addCell(pdfDeliveryReceiptUtils.getSignatureSectionCell("DELIVERED", "     PIECES", version_DR, TextAlignment.LEFT, TextAlignment.RIGHT, 1, 5, false).setPaddingBottom(bottomPadding));
        table.addCell(pdfDeliveryReceiptUtils.getSignatureSectionCell("TIME", "  :  ", version_DR, TextAlignment.LEFT, TextAlignment.CENTER, 1, (version_DR==FbdsVersionCd.BILINGUAL)?5:4, true).setPaddingBottom(bottomPadding));
        table.addCell(pdfDeliveryReceiptUtils.getSignatureSectionCell("DRIVER SIGNATURE", null, version_DR, TextAlignment.LEFT, null, 1, 7, false).setPaddingBottom(bottomPadding));
        table.addCell(pdfDeliveryReceiptUtils.getSignatureSectionCell("DATE", "/       /", version_DR, TextAlignment.LEFT, TextAlignment.CENTER, 1, 3, true).setPaddingBottom(bottomPadding));
        
        table.addCell(pdfDeliveryReceiptUtils.addCheckBoxSectionCell(pdfDeliveryReceiptUtils, version_DR).setPaddingBottom(bottomPadding));
        table.addCell(pdfDeliveryReceiptUtils.getSignatureSectionCell("CONSIGNEE SIGNATURE", null, version_DR, TextAlignment.LEFT, null, 1, (version_DR==FbdsVersionCd.BILINGUAL)?5:6, false).setPaddingBottom(bottomPadding));
        table.addCell(pdfDeliveryReceiptUtils.getSignatureSectionCell("PRINT CONSIGNEE NAME", null, version_DR, TextAlignment.LEFT, null, 1, 7, false).setPaddingBottom(bottomPadding));
        table.addCell(pdfDeliveryReceiptUtils.getSignatureSectionCell("DATE", "/       /", version_DR, TextAlignment.LEFT, TextAlignment.CENTER, 1, 3, true).setPaddingBottom(bottomPadding));
	}

	private void addArticlesAndRemarksSection(Table table, FbdsVersionCd version_DR, FBDSDocument fbdsDocumentData,
		List<FBDSDataRow> fbdsDataRows, PdfDeliveryReceiptUtils pdfDeliveryReceiptUtils, PdfPageTypeCd pageTypeCd) {
		boolean drawBottomLine = false;
        table.addCell(pdfDeliveryReceiptUtils.getTitleValueCell("HM", version_DR, null, TextAlignment.CENTER, null, 1, 1)
        	.setPaddingLeft(-2f).setPaddingRight(-2f));
		table.addCell(pdfDeliveryReceiptUtils.getTitleValueCell("PCS", version_DR, null, TextAlignment.CENTER, null, 1, 1)
			.setPaddingLeft(-4f).setPaddingRight(-4f));
        table.addCell(pdfDeliveryReceiptUtils.getTitleValueCell(checkDRlanguage(version_DR,"DESCRIPTION OF ARTICLES AND REMARKS", pdfDeliveryReceiptUtils), version_DR, null, TextAlignment.LEFT, null, 1, 18));
        table.addCell(pdfDeliveryReceiptUtils.getTitleValueCell((version_DR!=FbdsVersionCd.DEFAULT)?checkDRlanguage(version_DR,"WEIGHT", pdfDeliveryReceiptUtils):"WEIGHT (LBS.)", version_DR, null, TextAlignment.RIGHT, null, 1, 3))
        .setPaddingLeft(-2f).setPaddingRight(-2f);
        table.addCell(pdfDeliveryReceiptUtils.getTitleValueCell(checkDRlanguage(version_DR,"RATE", pdfDeliveryReceiptUtils), version_DR, null, TextAlignment.CENTER, null, 1, 3));
        table.addCell(pdfDeliveryReceiptUtils.getTitleValueCell(checkDRlanguage(version_DR,"CHARGES", pdfDeliveryReceiptUtils), version_DR, null, TextAlignment.CENTER, null, 1, 2));
        
        for (int i = 0; i < fbdsDataRows.size(); ++ i) {
        	drawBottomLine = i == fbdsDataRows.size() - 1
        			&& ((pageTypeCd == PdfPageTypeCd.FIRST
        			&& !pdfDeliveryReceiptUtils.getInBetweenSingleAndFirstPageLimit() && version_DR != FbdsVersionCd.BILINGUAL)
        			|| (pageTypeCd == PdfPageTypeCd.MIDDLE && (version_DR!=FbdsVersionCd.BILINGUAL || fbdsDataRows.size() == FBDS_DATA_ROW_LIMIT_MIDDLE_PAGE)))
        			? true : false;
        	FBDSDataRow row = fbdsDataRows.get(i);
            table.addCell(pdfDeliveryReceiptUtils.getDataRowCell(row.getHazardousInd(), version_DR, TextAlignment.CENTER, 1, 1, drawBottomLine, false)
            	.setPaddingLeft(-2f).setPaddingRight(-2f));
            table.addCell(pdfDeliveryReceiptUtils.getDataRowCell(row.getPiecesNum(), version_DR, TextAlignment.CENTER, 1, 1, drawBottomLine, false)
            	.setPaddingLeft(-4f).setPaddingRight(-4f));
            table.addCell(pdfDeliveryReceiptUtils.getDescriptionDataRowCell(row.getDescription(), row.getAdditionalDescription(),
            	retrieveDescriptionCellAlignment(row, fbdsDocumentData, pdfDeliveryReceiptUtils), 1, 18, drawBottomLine, version_DR));
            table.addCell(pdfDeliveryReceiptUtils.getDataRowCell(row.getWeight(), version_DR, TextAlignment.RIGHT, 1, 3, drawBottomLine, false));
            table.addCell(pdfDeliveryReceiptUtils.getDataRowCell(row.getRate(), version_DR, TextAlignment.RIGHT, 1, 3, drawBottomLine, false));
            if(StringUtils.contains(row.getDescription(), fbdsDocumentData.getTotalChargeAmountTextLine1())) {
            	table.addCell(pdfDeliveryReceiptUtils.getDataRowCell(row.getCharges(), version_DR, TextAlignment.RIGHT, 1, 2, drawBottomLine, true));
            }
            else {
            	table.addCell(pdfDeliveryReceiptUtils.getDataRowCell(row.getCharges(), version_DR, TextAlignment.RIGHT, 1, 2, drawBottomLine, false));
            }
            
		}
        
        //Fill the free space
        int currentPageLimit = articlesAndRemarksGridLimit;
        

        if(pageTypeCd == PdfPageTypeCd.FIRST) {
        	currentPageLimit = 16;
        	if(pdfDeliveryReceiptUtils.getInBetweenSingleAndFirstPageLimit()) {
        		drawBottomLine = true;
        	}
        }
        else if(pageTypeCd == PdfPageTypeCd.LAST) {
        	currentPageLimit = 18;
        }
        else if(pageTypeCd == PdfPageTypeCd.MIDDLE && version_DR == FbdsVersionCd.BILINGUAL) {
        	currentPageLimit = 25;
        }
        if(version_DR == FbdsVersionCd.BILINGUAL && (pageTypeCd == PdfPageTypeCd.FIRST) ) {
        	int leftoverRows = 6;
        		for (int i = 0; i < leftoverRows; ++i) {
        			if(i == leftoverRows - 1) {
        				generateEmptyCell(table, pdfDeliveryReceiptUtils, true);
        			}
        			else {
        				generateEmptyCell(table, pdfDeliveryReceiptUtils, false);
        			}
        		}
        }
        else {
        	int leftoverRows = currentPageLimit - fbdsDataRows.size();
        	if(leftoverRows != 0) {
        		for (int i = 0; i < leftoverRows; ++i) {
        			if(i == leftoverRows - 1) {
        				generateEmptyCell(table, pdfDeliveryReceiptUtils, true);
        			}
        			else {
        				generateEmptyCell(table, pdfDeliveryReceiptUtils, false);
        			}
        		}
        	}
        	
        }
	}
	
	private void generateEmptyCell(Table table, PdfDeliveryReceiptUtils pdfDeliveryReceiptUtils, boolean drawBottomLine) {
		table.addCell(pdfDeliveryReceiptUtils.generateEmptyCell(1,1, drawBottomLine));
    	table.addCell(pdfDeliveryReceiptUtils.generateEmptyCell(1,1, drawBottomLine));
    	table.addCell(pdfDeliveryReceiptUtils.generateEmptyCell(1,18, drawBottomLine));
    	table.addCell(pdfDeliveryReceiptUtils.generateEmptyCell(1,3, drawBottomLine));
    	table.addCell(pdfDeliveryReceiptUtils.generateEmptyCell(1,3, drawBottomLine));
    	table.addCell(pdfDeliveryReceiptUtils.generateEmptyCell(1,2, drawBottomLine));
	}

	private TextAlignment retrieveDescriptionCellAlignment(FBDSDataRow row, FBDSDocument fbdsDocumentData, PdfDeliveryReceiptUtils pdfDeliveryReceiptUtils) {
		TextAlignment textAlignment = TextAlignment.LEFT;
		boolean totalDescCheck = false;
		if((StringUtils.equals(fbdsDocumentData.getTotalChargeAmountTextLine1(), row.getDescription()))) {
			totalDescCheck = true;
			if((pdfDeliveryReceiptUtils.isMovrPro(fbdsDocumentData)
					&& pdfDeliveryReceiptUtils.isClearance(fbdsDocumentData)
					&& pdfDeliveryReceiptUtils.isFbds())
					|| (fbdsDocumentData.getBillClassCd() == BillClassCd.NORMAL_MVMT
					&& fbdsDocumentData.getDeliveryQualifierCd() == DeliveryQualifierCd.PARTIAL_SHORT)) {
				totalDescCheck = false;
			}
		} 
		
		if(totalDescCheck || (fbdsDocumentData.getRatesAndCharges() != null && fbdsDocumentData.getRatesAndCharges().getCashOnDelivery() != null
				   && StringUtils.equals(fbdsDocumentData.getRatesAndCharges().getCashOnDelivery().getDescription1(), row.getDescription()))
				|| StringUtils.equals(fbdsDocumentData.getMovrClearanceBillText(), row.getDescription())
				|| StringUtils.equals(fbdsDocumentData.getDriverCollectDescription(), row.getDescription())
				|| StringUtils.equals("**** COPY BILL ****", row.getDescription())) {
			textAlignment = TextAlignment.CENTER;
		}
		
		return textAlignment;
	}

	private String verifyProNbr(String proNbr) {
		return proNbr != null && proNbr.endsWith("*") ?  proNbr.substring(0, proNbr.length() - 1) : proNbr;
	}

	public byte[] generateProNbrErrorLog(
		List<DataValidationError> errorList,
		boolean reprintInd,
		Boolean isRequestByRoute,
		TransactionContext txnContext,
		EntityManager entityManager) 
		        throws ServiceException {
		
	    if (CollectionUtils.isNotEmpty(errorList)) {

		ByteArrayOutputStream out = new ByteArrayOutputStream();

    	PdfDocument pdfDoc = new PdfDocument(new PdfWriter(out));
        pdfDoc.setDefaultPageSize(PageSize.LETTER);

        Document document = new Document(pdfDoc);

        document.setMargins(MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM, MARGIN_LEFT);  
        
        PdfDeliveryReceiptUtils pdfDeliveryReceiptUtils = new PdfDeliveryReceiptUtils(null, FBDS_DATA_ROW_LIMIT_SINGLE_PAGE,
        	FBDS_DATA_ROW_LIMIT_FIRST_PAGE,FBDS_DATA_ROW_LIMIT_MIDDLE_PAGE,FBDS_DATA_ROW_LIMIT_LAST_PAGE);
        
        //Get font and image resources
        byte[] fontContent = null;
		try {
			fontContent = IOUtils.toByteArray(Resources.getResource(appContext.getFontsResourcePath() + CB_FONT_DEST));
		} catch (IOException e) {
			LOGGER.info("PdfDeliveryReceiptImpl.generateProNbrErrorLog:: unable to retrieve font contents for generating error log document.");
			if(document != null && document.getPdfDocument().getNumberOfPages() != 0) document.close();
        	
			return null;
		}

        pdfDeliveryReceiptUtils.initializeFonts(fontContent, null, txnContext, false);

        if (BooleanUtils.isTrue(isRequestByRoute)) {
            Map<String, List<DataValidationError>> errorsByRoute = buildErrorsByRoute(errorList, txnContext, entityManager);
            for (Entry<String, List<DataValidationError>> entry : errorsByRoute.entrySet()) {
                String routeName = entry.getKey();
                printErrorRpt(pdfDeliveryReceiptUtils, document, entry.getValue(), routeName);
            }
        } else {
            printErrorRpt(pdfDeliveryReceiptUtils, document, errorList, StringUtils.EMPTY);
        }

        document.close();
        pdfDoc.close();

        return out.toByteArray();
		}
		return null;
	}
	
	private Map<String, List<DataValidationError>> buildErrorsByRoute(
	        List<DataValidationError> errorList, 
	        TransactionContext txnContext,
	        EntityManager entityManager) throws ValidationException {
	    
	    Map<String, List<DataValidationError>> errorsByRoute = new HashMap<>();
	    Map<String, String> routeNameByProMap = getRouteNameForPro(errorList, txnContext, entityManager);
	    
	    for (DataValidationError error : errorList) {
	        String routeName = getDeliveryRouteByPro(error.getFieldValue(), routeNameByProMap, txnContext);
	        if (errorsByRoute.containsKey(routeName)) {
	            errorsByRoute.get(routeName).add(error);
	        } else {
	            List<DataValidationError> errorsThisRoute = new ArrayList<>();
	            errorsThisRoute.add(error);
	            errorsByRoute.put(routeName, errorsThisRoute);
	        }
	    }	    
	    return errorsByRoute;
	}
	
	private Map<String, String> getRouteNameForPro(
	        List<DataValidationError> errorList, 
	        TransactionContext txnContext,
	        EntityManager entityManager) throws ValidationException {
	    
	    Map<String, String> routeNameMapByPro = new HashMap<>();
	    List<String> proList = new ArrayList<>();
	    for (DataValidationError error : errorList) {
            proList.add(ProNumberHelper.toElevenDigitPro(error.getFieldValue(), txnContext));
	    }
	    if (CollectionUtils.isNotEmpty(proList)) {
            Set<ShipmentDetailCd> shipmentDetailCds = new HashSet<>();
            shipmentDetailCds.add(ShipmentDetailCd.SHIPMENT_ONLY);
            List<ShmShipment> shipmentList =
                shmShipmentSubDAO.listShipmentsByProNbrs
                    (proList,
                     new ShmShipmentEagerLoadPlan(),
                     entityManager);
	        if (CollectionUtils.isNotEmpty(shipmentList)) {
	            for (ShmShipment shipment : shipmentList) {
	                routeNameMapByPro.put(shipment.getProNbrTxt(), getRouteName(shipment));
	            }
	        }
	    }
	    return routeNameMapByPro;
	}
	
	private String getRouteName(ShmShipment shipment) {
	    
	    String routeName = " ";
	    if (StringUtils.isNotBlank(shipment.getRtePfxTxt()) && StringUtils.isNotBlank(shipment.getRteSfxTxt())) {
	        routeName = shipment.getRtePfxTxt() + "-" + shipment.getRteSfxTxt();
	    }
	    return routeName;
	}
	
	private String getDeliveryRouteByPro(String proNumber, Map<String, String> routeNameByProMap, TransactionContext txnContext) throws ValidationException {
	    
	    String routeName = " ";
	    String elevenPro = ProNumberHelper.toElevenDigitPro(proNumber, txnContext);
	    if (routeNameByProMap.containsKey(elevenPro)) {
	        routeName = routeNameByProMap.get(elevenPro);
	    }
	    return routeName;
	}
	
	private void printErrorRpt(
	        PdfDeliveryReceiptUtils pdfDeliveryReceiptUtils, 
	        Document document,
	        List<DataValidationError> errorList,
            String routeName) {
	    
        Table table = pdfDeliveryReceiptUtils.buildErrorLogHeader(routeName);
        document.add(table);

        for (DataValidationError error : errorList) {
            String proNbr = FBDSCopyBillUtil.formatFBDSPro(error.getFieldValue());
            String message = error.getMessage();
            document.add(pdfDeliveryReceiptUtils.buildErrorLogMessage(String.format("%s: %s", proNbr, message), false));
        }
	}

}
