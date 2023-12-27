package com.xpo.ltl.shipment.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.api.client.util.Base64;
import com.google.gson.Gson;
import com.itextpdf.barcodes.BarcodeDataMatrix;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.forms.fields.PdfTextFormField;
import com.itextpdf.io.font.FontConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.v2.PrintProLabelsResp;
import com.xpo.ltl.api.shipment.v2.PrintProLabelsRqst;
import com.xpo.ltl.api.shipment.v2.ProLabel;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import com.xpo.ltl.shipment.service.validators.PrintProLabelsValidator;

@ApplicationScoped
@LogExecutionTime
public class PrintProLabelsImpl {

	@Inject
	private PrintProLabelsValidator printProLabelsValidator;

	private static final Logger LOGGER = LogManager.getLogger(PrintProLabelsImpl.class);
	private static final Integer NUMBER_OF_LABELS_PERPAGE = 18;
	private static final String XPO_LABEL = "XPO";
	private static final Float MARGIN = 36.0f;

	public PrintProLabelsResp printProLabels(PrintProLabelsRqst printProLabelsRqst, TransactionContext txnContext)
			throws ServiceException, ValidationException {

		LOGGER.info(String.format("Request Payload for printProLabels : %s ", new Gson().toJson(printProLabelsRqst)));

		printProLabelsValidator.validate(printProLabelsRqst, txnContext);

		setDefaultValue(printProLabelsRqst, txnContext);

		List<ProLabel> proLabels = printProLabelsRqst.getProLabels();

		Integer totalLabelCount = proLabels.stream().map(proLabel -> proLabel.getLabelCount())
				.mapToInt(BigInteger::intValue).sum();
		Integer startPosition = Integer.valueOf(printProLabelsRqst.getStartPosition());
		Integer totalPages = calculateTotalPages(totalLabelCount, startPosition);

		PdfDocument pdfDoc = null;
		Document doc = null;
		ByteArrayOutputStream out = null;
		try {
			out = new ByteArrayOutputStream();
			pdfDoc = new PdfDocument(new PdfWriter(out));
			pdfDoc.setDefaultPageSize(PageSize.LETTER);
			doc = new Document(pdfDoc);

			PdfAcroForm pdfDocform = PdfAcroForm.getAcroForm(pdfDoc, true);

			addPages(totalPages, pdfDoc, pdfDocform);

			LOGGER.info("Generated Blank PDF. Populating PDF Data.");

			doc = populateData(proLabels, startPosition, pdfDoc, pdfDocform, doc);

			doc.setMargins(PrintProLabelsImpl.MARGIN, PrintProLabelsImpl.MARGIN, PrintProLabelsImpl.MARGIN,
					PrintProLabelsImpl.MARGIN);
			doc.getPdfDocument();
			doc.close();
			pdfDoc.close();
			out.close();
		} catch (Exception e) {
			String errorMessage = e.getMessage();
			try {
				if (doc != null && doc.getPdfDocument().getNumberOfPages() != 0)
					doc.close();
				if (pdfDoc != null && pdfDoc.getNumberOfPages() != 0)
					pdfDoc.close();
				if (out != null)
					out.close();
			} catch (Exception e1) {
				errorMessage = e1.getMessage() + " " + e.getMessage();
			}
			throw com.xpo.ltl.api.exception.ExceptionBuilder
					.exception(com.xpo.ltl.api.exception.ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e)
					.moreInfo("Error Occured in printing Pro Lables : ", errorMessage).build();
		}

		LOGGER.info("Generating Response.");
		PrintProLabelsResp printProLabelsResp = buildResponse(out);
		return printProLabelsResp;

	}

	private Document populateData(List<ProLabel> proLabels, Integer startPosition, PdfDocument pdfDoc,
			PdfAcroForm pdfDocform, Document doc) throws IOException {
		List<String> proLabelListWithLabelCountList = getProLabelListConsideringLabelCount(startPosition, proLabels);

		for (int index = 1; index <= proLabelListWithLabelCountList.size(); index++) {

			String proNumberPageNumber = proLabelListWithLabelCountList.get(index - 1);
			String[] split = proNumberPageNumber.split(":");
			String proNumber = split[0];
			Integer pageNumber = Integer.valueOf(split[1]);

			if (proNumber.trim().length() > 0) {

				PdfFormField proField = pdfDocform.getField("pro" + index);
				proField.setValue(proNumber);
				proField.setReadOnly(true);
				proField.setFontSize(11);	
				proField.setVisibility(0);
				

				PdfFormField logoField = pdfDocform.getField("logo" + index);
				PdfFont bold = PdfFontFactory.createFont(FontConstants.HELVETICA_OBLIQUE);
				logoField.setFontSize(14);
				logoField.setValue(XPO_LABEL);
				logoField.setReadOnly(true);
				logoField.setFont(bold);
				logoField.setVisibility(0);


				int j = (index - 1) % PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE;
				int xPos = 30;
				int yPos = 635;
				if (j % 3 == 0)
					xPos = 30;
				if (j % 3 == 1)
					xPos = 227;
				if (j % 3 == 2)
					xPos = 427;
				if (j < 3)
					yPos = 635;
				if (j >= 3 && j < 6)
					yPos = 522;
				if (j >= 6 && j < 9)
					yPos = 410;
				if (j >= 9 && j < 12)
					yPos = 297;
				if (j >= 12 && j < 15)
					yPos = 184;
				if (j >= 15)
					yPos = 72;

				Image img = createCodeImage(new BarcodeDataMatrix(proNumber).createFormXObject(pdfDoc), xPos, yPos,
						pageNumber);
				doc.add(img);
			}

		}
		return doc;
	}

	private void addPages(Integer totalPages, PdfDocument pdfDoc, PdfAcroForm pdfDocform) {

		Float WIDTH_PRO_FIELD = 70f;
		Float HEIGHT_PRO_FIELD = 10f;

		Float WIDTH_LOGO_FIELD = 50f;
		Float HEIGHT_LOGO_FIELD = 12f;

		for (int pageCount = 1; pageCount <= totalPages; pageCount++) {

			pdfDoc.addNewPage();

			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(120.386f, 635.745f, WIDTH_PRO_FIELD, HEIGHT_PRO_FIELD),
							"pro" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 1), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(316.851f, 635.339f, WIDTH_PRO_FIELD, HEIGHT_PRO_FIELD),
							"pro" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 2), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(516.221f, 635.339f, WIDTH_PRO_FIELD, HEIGHT_PRO_FIELD),
							"pro" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 3), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(121.024f, 531.773f, WIDTH_PRO_FIELD, HEIGHT_PRO_FIELD),
							"pro" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 4), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(316.992f, 530.237f, WIDTH_PRO_FIELD, HEIGHT_PRO_FIELD),
							"pro" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 5), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(517.291f, 526.519f, WIDTH_PRO_FIELD, HEIGHT_PRO_FIELD),
							"pro" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 6), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(123.205f, 413.926f, WIDTH_PRO_FIELD, HEIGHT_PRO_FIELD),
							"pro" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 7), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(322.851f, 413.441f, WIDTH_PRO_FIELD, HEIGHT_PRO_FIELD),
							"pro" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 8), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(525.939f, 414.371f, WIDTH_PRO_FIELD, HEIGHT_PRO_FIELD),
							"pro" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 9), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc, new Rectangle(122.842f, 300.99f, WIDTH_PRO_FIELD, HEIGHT_PRO_FIELD),
							"pro" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 10), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(318.851f, 300.485f, WIDTH_PRO_FIELD, HEIGHT_PRO_FIELD),
							"pro" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 11), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc, new Rectangle(521.01f, 303.273f, WIDTH_PRO_FIELD, HEIGHT_PRO_FIELD),
							"pro" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 12), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(121.024f, 192.979f, WIDTH_PRO_FIELD, HEIGHT_PRO_FIELD),
							"pro" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 13), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(318.851f, 191.645f, WIDTH_PRO_FIELD, HEIGHT_PRO_FIELD),
							"pro" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 14), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc, new Rectangle(521.01f, 192.574f, WIDTH_PRO_FIELD, HEIGHT_PRO_FIELD),
							"pro" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 15), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc, new Rectangle(119.66f, 79.133f, WIDTH_PRO_FIELD, HEIGHT_PRO_FIELD),
							"pro" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 16), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc, new Rectangle(320.71f, 79.618f, WIDTH_PRO_FIELD, HEIGHT_PRO_FIELD),
							"pro" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 17), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc, new Rectangle(519.151f, 78.688f, WIDTH_PRO_FIELD, HEIGHT_PRO_FIELD),
							"pro" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 18), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));

			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(120.671f, 701.64f, WIDTH_LOGO_FIELD, HEIGHT_LOGO_FIELD),
							"logo" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 1), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(317.364f, 701.64f, WIDTH_LOGO_FIELD, HEIGHT_LOGO_FIELD),
							"logo" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 2), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(516.74f, 701.64f, WIDTH_LOGO_FIELD, HEIGHT_LOGO_FIELD),
							"logo" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 3), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(121.013f, 590.531f, WIDTH_LOGO_FIELD, HEIGHT_LOGO_FIELD),
							"logo" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 4), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(317.901f, 589.338f, WIDTH_LOGO_FIELD, HEIGHT_LOGO_FIELD),
							"logo" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 5), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(523.549f, 589.247f, WIDTH_LOGO_FIELD, HEIGHT_LOGO_FIELD),
							"logo" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 6), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(123.558f, 474.443f, WIDTH_LOGO_FIELD, HEIGHT_LOGO_FIELD),
							"logo" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 7), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(323.195f, 472.978f, WIDTH_LOGO_FIELD, HEIGHT_LOGO_FIELD),
							"logo" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 8), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(523.781f, 472.411f, WIDTH_LOGO_FIELD, HEIGHT_LOGO_FIELD),
							"logo" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 9), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(123.285f, 362.897f, WIDTH_LOGO_FIELD, HEIGHT_LOGO_FIELD),
							"logo" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 10), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(322.013f, 362.837f, WIDTH_LOGO_FIELD, HEIGHT_LOGO_FIELD),
							"logo" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 11), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(523.034f, 362.746f, WIDTH_LOGO_FIELD, HEIGHT_LOGO_FIELD),
							"logo" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 12), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(121.558f, 247.533f, WIDTH_LOGO_FIELD, HEIGHT_LOGO_FIELD),
							"logo" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 13), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(320.285f, 247.927f, WIDTH_LOGO_FIELD, HEIGHT_LOGO_FIELD),
							"logo" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 14), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(522.236f, 247.816f, WIDTH_LOGO_FIELD, HEIGHT_LOGO_FIELD),
							"logo" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 15), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(123.65f, 140.899f, WIDTH_LOGO_FIELD, HEIGHT_LOGO_FIELD),
							"logo" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 16), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(320.579f, 138.27f, WIDTH_LOGO_FIELD, HEIGHT_LOGO_FIELD),
							"logo" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 17), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));
			pdfDocform.addField(
					PdfFormField.createText(pdfDoc,
							new Rectangle(522.418f, 140.959f, WIDTH_LOGO_FIELD, HEIGHT_LOGO_FIELD),
							"logo" + (((pageCount - 1) * PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) + 18), "").setReadOnly(true),
					pdfDoc.getPage(pageCount));

		}
	}

	private PrintProLabelsResp buildResponse(ByteArrayOutputStream out) {
		PrintProLabelsResp printProLabelsResp = new PrintProLabelsResp();
		byte[] documentData = out.toByteArray();
		List<byte[]> documentDataList = new ArrayList<>();
		documentDataList.add(documentData);
		List<String> base64EncodedDataList = CollectionUtils.emptyIfNull(documentDataList).stream()
				.map(d -> Base64.encodeBase64String(d)).collect(Collectors.toList());
		printProLabelsResp.setDocumentData(documentDataList);
		return printProLabelsResp;
	}

	private void setDefaultValue(PrintProLabelsRqst printProLabelsRqst, TransactionContext txnContext)
			throws ValidationException {

		if (StringUtils.isBlank(printProLabelsRqst.getStartPosition())) {
			printProLabelsRqst.setStartPosition("1");
		}

		for (ProLabel proLabel : printProLabelsRqst.getProLabels()) {
			if(ProNumberHelper.isBluePro(proLabel.getProNbr())) {
				proLabel.setProNbr(ProNumberHelper.toNineDigitProHyphen(proLabel.getProNbr(), txnContext));
			} else if(ProNumberHelper.isYellowPro(proLabel.getProNbr())) {
				proLabel.setProNbr(ProNumberHelper.toTenDigitPro(ProNumberHelper.toElevenDigitPro(proLabel.getProNbr(), txnContext)));	
			}
			if (null == proLabel.getLabelCount()) {
				proLabel.setLabelCount(BigInteger.ONE);
			}
		}
	}

	private Integer calculateTotalPages(Integer totalLabelCount, Integer startPosition) {

		int numOfFullPages = 0;
		int firstPage = 1;
		int lastPage = 0;

		int upperBound = PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE + 1;
		int numOfLabelsOnFirstPage = upperBound - startPosition;

		int labelcountNotIncludeFirstPage = totalLabelCount - numOfLabelsOnFirstPage;
		// If the value is -ve, all labels will print on 1st Page
		if (labelcountNotIncludeFirstPage > 0) {
			if (labelcountNotIncludeFirstPage == PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) {
				// there are only 18 label left; this is the last page - i.e. No full pages
				lastPage = 1;
			} else {
				numOfFullPages = labelcountNotIncludeFirstPage / PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE;
				// If labelcountNotIncludeFirstPage is not divisible by 18, we will have some
				// label on last page.
				if ((labelcountNotIncludeFirstPage % PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) != 0) {
					lastPage = 1;
				}
			}
		}
		return firstPage + numOfFullPages + lastPage;
	}

	private Image createCodeImage(PdfFormXObject codeImage, int xPos, int yPos, int pageNumber) {
		Image codeQrImage = new Image(codeImage);
		codeQrImage.setFixedPosition(pageNumber, xPos, yPos, 84);
		return codeQrImage;
	}

	private static List<String> getProLabelListConsideringLabelCount(Integer startPosition, List<ProLabel> proLabels)
			throws NumberFormatException {
		List<String> list = new ArrayList<>();
		Integer pageNumber = 1;
		Integer totalCount = 0;

		for (int idx = 0; (idx + 1) < startPosition; idx++) {
			list.add("" + ":" + pageNumber);
			totalCount++;
			if (totalCount == PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) {
				pageNumber++;
				totalCount = 0;
			}
		}

		for (int i = 0; i < proLabels.size(); i++) {
			ProLabel proLabel = proLabels.get(i);
			for (int j = 0; j < proLabel.getLabelCount().intValue(); j++) {
				list.add(proLabel.getProNbr() + ":" + pageNumber);
				totalCount++;
				if (totalCount == PrintProLabelsImpl.NUMBER_OF_LABELS_PERPAGE) {
					pageNumber++;
					totalCount = 0;
				}
			}
		}
		return list;
	}

}
