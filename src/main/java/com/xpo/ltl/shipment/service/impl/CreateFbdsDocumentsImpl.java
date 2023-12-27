package com.xpo.ltl.shipment.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.api.client.util.Base64;
import com.itextpdf.forms.PdfPageFormCopier;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationErrorMessage;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.v2.CreateFBDSDocumentsResp;
import com.xpo.ltl.api.shipment.v2.CreateFBDSDocumentsRqst;
import com.xpo.ltl.api.shipment.v2.DataValidationError;
import com.xpo.ltl.api.shipment.v2.FBDSDocument;
import com.xpo.ltl.api.shipment.v2.FbdsVersionCd;
import com.xpo.ltl.shipment.pdf.FBDSDocumentsWithValidations;
import com.xpo.ltl.shipment.service.delegates.ShipmentDetailsDelegate;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.FBDSCopyBillUtil;
import com.xpo.ltl.shipment.service.validators.Validator;

@ApplicationScoped
@LogExecutionTime
public class CreateFbdsDocumentsImpl extends Validator {

    private static final Logger LOGGER = LogManager.getLogger(CreateFbdsDocumentsImpl.class);

	@Inject
	private PdfDeliveryReceiptImpl pdfDeliveryReceiptImpl; 
	
	@Inject
	private	ListFBDSCopyBillDocumentsImpl listFBDSCopyBillDocumentsImpl;

	public static final String LOCAL_HOST_TEST_DIR = "/Users/sarthaksingh/XPO/liberty_development/myWorkingFiles/dr_test_file";

	public CreateFBDSDocumentsResp createFbdsDocuments(
		final CreateFBDSDocumentsRqst rqst,
		final TransactionContext txnContext,
		final EntityManager entityManager) throws ServiceException {
		if(CollectionUtils.isEmpty(rqst.getShipmentIds())) {
    		throw com.xpo.ltl.api.exception.ExceptionBuilder
			.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
			.moreInfo("createFbdsDocuments", "One or more PRO numbers are required.")
			.build();
    	}
		
		if(Objects.isNull(rqst.getFormType())) {
    		throw com.xpo.ltl.api.exception.ExceptionBuilder
			.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
			.moreInfo("createFbdsDocuments", "Form type is required.")
			.build();
    	}
		
		List<byte[]> documentData = new ArrayList<>();
		List<DataValidationError> errorList = null;
		List<String> proNbrsResp = new ArrayList<>();
        boolean includeWarningPage = BooleanUtils.toBoolean(rqst.getIncludeWarningsPageInd());
		boolean reprintInd = BooleanUtils.toBoolean(rqst.getReprintInd());
		boolean singleDocInd = BooleanUtils.toBoolean(rqst.getGenerateSingleDocInd());
		
		FbdsVersionCd version_DR = FbdsVersionCd.DEFAULT;
		if(rqst.getLanguage()!=null) {
			version_DR = rqst.getLanguage();
		}
						
		List<String> proNbrsRqst = CollectionUtils.emptyIfNull(rqst.getShipmentIds())
        		.stream().map(i -> i.getProNumber()).collect(Collectors.toList());
        List<String> shipmentIdsRqst = CollectionUtils.emptyIfNull(rqst.getShipmentIds())
        		.stream().map(i -> i.getShipmentInstId()).collect(Collectors.toList());
    	if(CollectionUtils.isNotEmpty(shipmentIdsRqst) && CollectionUtils.isEmpty(proNbrsRqst)) {
    		throw com.xpo.ltl.api.exception.ExceptionBuilder
			.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
			.moreInfo("createFbdsDocuments", "ShipmentInstIds are not yet supported in this operation.")
			.build();
    	}

    	ByteArrayOutputStream out = new ByteArrayOutputStream();
		PdfDocument pdfCombined = new PdfDocument(new PdfWriter(out));
    	pdfCombined.initializeOutlines();
    	PdfPageFormCopier formCopier = new PdfPageFormCopier();

		FBDSDocumentsWithValidations fbdsDocumentsData = listFBDSCopyBillDocumentsImpl
				.processAndGenerateFBDSCopyBillDocuments(proNbrsRqst, rqst.getFormType(), reprintInd, txnContext, entityManager);
		errorList = fbdsDocumentsData.getValidationErrors();
		
		if(CollectionUtils.isNotEmpty(fbdsDocumentsData.getFbdsDocuments())) {
			for (FBDSDocument fbdsDocumentData : fbdsDocumentsData.getFbdsDocuments()) {
				try {
					byte[] byteOutput = pdfDeliveryReceiptImpl.generateFBDSDocumentPdf(fbdsDocumentData, version_DR, rqst.getFormType(), txnContext, entityManager);	
					
					if(singleDocInd) {
						copyPdfToCombinedPdf(pdfCombined, formCopier, byteOutput);
					}
					else {
						documentData.add(byteOutput);
					}
					
					proNbrsResp.add(fbdsDocumentData.getProNbr());
				}
    			catch (Exception e) {
    				errorList.add(FBDSCopyBillUtil.createDataValidationError(fbdsDocumentData.getProNbr(), e.getMessage()));
       			}
			}
		}

    	if(includeWarningPage && CollectionUtils.isNotEmpty(errorList)) {
			byte[] byteOutput = pdfDeliveryReceiptImpl.generateProNbrErrorLog(errorList, reprintInd, rqst.isRequestByRouteInd(), txnContext, entityManager);
				try {
				if (byteOutput != null) {
					if(singleDocInd) {
						copyPdfToCombinedPdf(pdfCombined, formCopier, byteOutput);
					}
					else {
						documentData.add(byteOutput);
					}
				}
				} catch (IOException e) {
					throw com.xpo.ltl.api.exception.ExceptionBuilder
					.exception(com.xpo.ltl.api.exception.ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e)
					.moreInfo("Unable to generate error log PDF: ", e.getMessage())
					.build();
				}
		}
	
		if(pdfCombined != null && pdfCombined.getNumberOfPages() != 0) {
			pdfCombined.close();
			documentData.add(out.toByteArray());
		}
    
        //For local testing
        if(System.getenv("HOSTNAME") == null) {
        	try {
        		if(CollectionUtils.isNotEmpty(documentData)) {
                    for (int i = 0; i < documentData.size(); i++) {
                        byte[] document = documentData.get(i);
                        if (document != null) {
                            // File file =
                            //     new File(FileUtils.getTempDirectory(),
                            //              "dr_test_file_" + i + ".pdf");
                            // LOGGER.info("createFbdsDocuments: Storing PDF locally for testing in {}",
                            //             file);
                            // FileUtils.writeByteArrayToFile(file, document);
							FileOutputStream fo = new FileOutputStream(LOCAL_HOST_TEST_DIR + "_" + i + ".pdf");
							fo.write(document);
							fo.close();
                        }
					}
        		}
            }
            catch (Exception e) {
                LOGGER.warn("createFbdsDocuments: Failed to store PDF locally for testing", e);
    		}
        }
	
        
    	CreateFBDSDocumentsResp resp = new CreateFBDSDocumentsResp();
		resp.setDocumentData(CollectionUtils.emptyIfNull(documentData).stream().map(d -> Base64.encodeBase64String(d)).collect(Collectors.toList()));
		resp.setProNbrs(proNbrsResp);
		resp.setWarnings(errorList);

		return resp;
	}

	/**
	 * Merge multiple pdf docs onto one
	 * @param pdfCombined
	 * @param formCopier
	 * @param byteOutput
	 * @throws IOException
	 */
	private void copyPdfToCombinedPdf(
		PdfDocument pdfCombined,
		PdfPageFormCopier formCopier,
		byte[] byteOutput) throws IOException {
		PdfDocument generatedDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(byteOutput)));
		generatedDoc.copyPagesTo(1, generatedDoc.getNumberOfPages(), pdfCombined, formCopier);
		generatedDoc.close();
	}

}

