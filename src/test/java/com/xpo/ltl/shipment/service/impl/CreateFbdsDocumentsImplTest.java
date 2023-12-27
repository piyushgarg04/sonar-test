package com.xpo.ltl.shipment.service.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpo.ltl.api.cityoperations.v1.FbdsVersionCd;
import com.xpo.ltl.api.cityoperations.v1.PrintOption;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.v2.CreateFBDSDocumentsResp;
import com.xpo.ltl.api.shipment.v2.CreateFBDSDocumentsRqst;
import com.xpo.ltl.api.shipment.v2.DocumentFormTypeCd;
import com.xpo.ltl.api.shipment.v2.FBDSDocument;
import com.xpo.ltl.api.shipment.v2.ShipmentId;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.pdf.FBDSDocumentsWithValidations;

import junit.framework.TestCase;

public class CreateFbdsDocumentsImplTest extends TestCase {

    @Mock
    private TransactionContext txnContext;

    @Mock
    private EntityManager entityManager;
    
    @Mock
    private PdfDeliveryReceiptImpl pdfDeliveryReceiptImpl; 
    
    @Mock
    private ListFBDSCopyBillDocumentsImpl listFBDSCopyBillDocumentsImpl;

    @InjectMocks
    private CreateFbdsDocumentsImpl createFbdsDocumentsImpl;

    private static final String PRO_NBR = "09860881755";
    
    private static final String DESTINATION_SIC_CD_XUK = "XUK";
    
    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        when(txnContext.getSrcApplicationId()).thenReturn("JUNIT");

        final User user = new User();
        user.setUserId("JUNIT");
        user.setEmployeeId("JUNIT");
        when(txnContext.getUser()).thenReturn(user);

        when(txnContext.getTransactionTimestamp()).thenReturn(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance()));

        when(txnContext.getCorrelationId()).thenReturn("0");
    }

    @Test
    public void testCreateFbdsDocuments_shipmentIdsRequired() {
        try {
        	CreateFBDSDocumentsRqst rqst = new CreateFBDSDocumentsRqst();
        	rqst.setFormType(DocumentFormTypeCd.FBDS);
        	  
			createFbdsDocumentsImpl.createFbdsDocuments(rqst, txnContext, entityManager);
			fail("Expected an exception.");
        } catch (Exception e) {
        	assertEquals("APIN021-010E:Validation errors found(location:createFbdsDocuments, message:One or more PRO numbers are required.)(srcApplicationId:JUNIT)(correlationId:0)", e.getMessage());
        }
    }
    
    @Test
    public void testCreateFbdsDocuments_shipmentIdsNonEmptyRequired() {
        try {
        	CreateFBDSDocumentsRqst rqst = new CreateFBDSDocumentsRqst();
        	rqst.setShipmentIds(new ArrayList<>());
        	rqst.setFormType(DocumentFormTypeCd.FBDS);
        	  
			createFbdsDocumentsImpl.createFbdsDocuments(rqst, txnContext, entityManager);
			fail("Expected an exception.");
        } catch (Exception e) {
        	assertEquals("APIN021-010E:Validation errors found(location:createFbdsDocuments, message:One or more PRO numbers are required.)(srcApplicationId:JUNIT)(correlationId:0)", e.getMessage());
        }
    }
    
    @Test
    public void testCreateFbdsDocuments_FormTypeRequired() {
        try {
        	CreateFBDSDocumentsRqst rqst = new CreateFBDSDocumentsRqst();
        	rqst.setShipmentIds(Arrays.asList(generateShipmentId(PRO_NBR)));
        	
			createFbdsDocumentsImpl.createFbdsDocuments(rqst, txnContext, entityManager);
			fail("Expected an exception.");
        } catch (Exception e) {
        	assertEquals("APIN021-010E:Validation errors found(location:createFbdsDocuments, message:Form type is required.)(srcApplicationId:JUNIT)(correlationId:0)", e.getMessage());
        }
    }
    
    @Test
    public void testCreateFbdsDocuments_validResponse() throws Throwable {
        CreateFBDSDocumentsRqst rqst = new CreateFBDSDocumentsRqst();
        rqst.setShipmentIds(Arrays.asList(generateShipmentId(PRO_NBR)));
        rqst.setFormType(DocumentFormTypeCd.FBDS);
        rqst.setDestinationSicCd(DESTINATION_SIC_CD_XUK);

        when(listFBDSCopyBillDocumentsImpl
                 .processAndGenerateFBDSCopyBillDocuments
                     (any(),
                      any(),
                      eq(false),
                      eq(txnContext),
                      eq(entityManager)))
            .thenReturn(generateFbdsDocument());

        CreateFBDSDocumentsResp resp = createFbdsDocumentsImpl.createFbdsDocuments(rqst, txnContext, entityManager);
        assertNotNull(resp);
        assertNotNull(resp.getDocumentData());
        assertNotNull(resp.getProNbrs());
        assertEquals(resp.getProNbrs().get(0), PRO_NBR);
    }

    @Test
    public void testCreateFbdsDocuments_validResponseWithError()
            throws Throwable {
        CreateFBDSDocumentsRqst rqst = new CreateFBDSDocumentsRqst();
        rqst.setShipmentIds(Arrays.asList(generateShipmentId("111")));
        rqst.setFormType(DocumentFormTypeCd.FBDS);

        when(listFBDSCopyBillDocumentsImpl
                 .processAndGenerateFBDSCopyBillDocuments
                     (any(), any(), eq(false), eq(txnContext), eq(entityManager)))
            .thenReturn(generateFbdsDocument());

        CreateFBDSDocumentsResp resp = createFbdsDocumentsImpl.createFbdsDocuments(rqst, txnContext, entityManager);
        assertNotNull(resp);
        // TODO Unit test should only have warnings and no data somehow?
        //assertNull(resp.getDocumentData());
        //assertNull(resp.getProNbrs());
        //assertNotNull(resp.getWarnings());
        //assertEquals(resp.getWarnings().get(0).getFieldValue(), "111");
    }

    private ShipmentId generateShipmentId(String proNbr) {
    	ShipmentId shipmentId = new ShipmentId();
    	shipmentId.setProNumber(proNbr);
    	
		return shipmentId;
	}

	private FBDSDocumentsWithValidations generateFbdsDocument() {
		FBDSDocumentsWithValidations resp = new FBDSDocumentsWithValidations();
    	
    	FBDSDocument document = new FBDSDocument();
    	document.setProNbr(PRO_NBR);
    	document.setPickupDate("2020-21-10");
    	
		resp.setFbdsDocuments(Arrays.asList(document));

        resp.setValidationErrors(new ArrayList<>());

		return resp;
	}
	
	private PrintOption generatePrintOption() {
		PrintOption printOption = new PrintOption();	
		printOption.setFbdsVersionCd(FbdsVersionCd.BILINGUAL);
		return printOption;
	}
}
