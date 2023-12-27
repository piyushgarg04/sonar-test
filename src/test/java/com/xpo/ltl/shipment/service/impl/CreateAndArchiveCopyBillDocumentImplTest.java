package com.xpo.ltl.shipment.service.impl;

import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.v2.CreateAndArchiveCopyBillDocumentRqst;
import com.xpo.ltl.api.shipment.v2.ShipmentId;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.mockito.Mockito.when;

public class CreateAndArchiveCopyBillDocumentImplTest extends TestCase {

    @Mock
    private TransactionContext txnContext;

    @Mock
    private CreateAndArchiveCopyBillDocumentRqst createAndArchiveCopyBillDocumentRqst;

    @Mock
    private ShipmentId shipmentId;

    @InjectMocks
    private CreateAndArchiveCopyBillDocumentImpl createAndArchiveCopyBillDocument;
    
    @PersistenceContext(unitName = "ltl-java-shipment-jaxrs")
    private EntityManager entityManager;
    
    @Override
    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRequestIsRequired() {
        try {
            createAndArchiveCopyBillDocument.createAndArchiveCopyBillDocument(null, txnContext, entityManager);
            fail("Expected an exception");
        }
        catch (Exception e) {
            assertEquals("The request is required.", e.getMessage());
        }
    }

    @Test
    public void testTxnContextIsRequired() {

        try {
            createAndArchiveCopyBillDocument.createAndArchiveCopyBillDocument(createAndArchiveCopyBillDocumentRqst, null, entityManager);
            fail("Expected an exception");
        } catch (Exception e) {
            assertEquals("TransactionContext is required", e.getMessage());
        }
    }

    @Test
    public void testProNbrOrInstanceIdIsRequired() {

        when(createAndArchiveCopyBillDocumentRqst.getShipmentId()).thenReturn(shipmentId);
        when(shipmentId.getProNumber()).thenReturn(null);
        when(shipmentId.getShipmentInstId()).thenReturn(null);

        try {
            createAndArchiveCopyBillDocument.createAndArchiveCopyBillDocument(createAndArchiveCopyBillDocumentRqst, txnContext, entityManager);
            fail("Expected an exception");
        } catch (Exception e) {
            assertEquals("A ProNumber or InstanceID is required", e.getMessage());
        }
    }
}
