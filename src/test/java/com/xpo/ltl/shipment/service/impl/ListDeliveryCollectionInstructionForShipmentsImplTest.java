package com.xpo.ltl.shipment.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.CollectMoneyAtDeliveryCd;
import com.xpo.ltl.api.shipment.v2.ListDeliveryCollectionInstructionForShipmentsResp;
import com.xpo.ltl.api.shipment.v2.ListDeliveryCollectionInstructionForShipmentsRqst;
import com.xpo.ltl.api.shipment.v2.ShipmentId;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;

public class ListDeliveryCollectionInstructionForShipmentsImplTest {

    @Mock
    private TransactionContext txnContext;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ShmShipmentSubDAO shmShipmentSubDAO;

    @Mock
	private AppContext appContext;
	
    @InjectMocks
    private ListDeliveryCollectionInstructionForShipmentsImpl listDeliveryCollectionInstructionForShipmentsImpl;

    
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
    public void testListDeliveryCollectionInstructionForShipments_TransactionContextRequired() {
        try {
        	listDeliveryCollectionInstructionForShipmentsImpl.listDeliveryCollectionInstructionForShipments(null, null, entityManager);
            Assert.fail("Expected an exception.");
        } catch (Exception e) {
            Assert.assertEquals("TransactionContext is required", e.getMessage());
        }
    }

    @Test
    public void testListDeliveryCollectionInstructionForShipments_EntityManagerRequired() {
        try {
        	listDeliveryCollectionInstructionForShipmentsImpl.listDeliveryCollectionInstructionForShipments(null, txnContext, null);
            Assert.fail("Expected an exception.");
        } catch (Exception e) {
            Assert.assertEquals("EntityManager is required", e.getMessage());
        }
    }

    @Test
    public void testListDeliveryCollectionInstructionForShipments_MixedInputNotAllowed() {
        try {
        	ListDeliveryCollectionInstructionForShipmentsRqst rqst = new ListDeliveryCollectionInstructionForShipmentsRqst();
        	List<ShipmentId> ids = new ArrayList<ShipmentId>();
        	ShipmentId id1 = new ShipmentId();
        	id1.setShipmentInstId("123456");
        	ids.add(id1);
        	ShipmentId id2 = new ShipmentId();
        	id2.setProNumber("123456");
        	ids.add(id2);
        	rqst.setShipmentIds(ids);
        	assertThrows(ValidationException.class, () -> {
        	listDeliveryCollectionInstructionForShipmentsImpl.listDeliveryCollectionInstructionForShipments(rqst, txnContext, entityManager);
        	});
        } catch (Exception e) {
            //do nothing
        }
    }

   @Test
    public void testListDeliveryCollectionInstructionForShipments_ShipmentNotFound() {
        try {
        	ListDeliveryCollectionInstructionForShipmentsRqst rqst = new ListDeliveryCollectionInstructionForShipmentsRqst();
        	
        	listDeliveryCollectionInstructionForShipmentsImpl.listDeliveryCollectionInstructionForShipments(rqst, txnContext, entityManager);
            Assert.fail("Expected an exception.");
        } catch (Exception e) {
            Assert.assertEquals("ShipmentIds are required", e.getMessage());
        }
    }

   @Test
   public void testListDeliveryCollectionInstructionForShipments_PrepaidShipmentFound() {
       try {
       	
    	   
    	List<ShmShipment> shipments = new ArrayList<ShmShipment>();
    	ShmShipment shipment = new ShmShipment();
    	shipment.setShpInstId(123456);
    	shipment.setProNbrTxt("01110123456");
    	shipment.setBillClassCd("A");
    	shipment.setBillStatCd("4");
    	shipment.setChrgToCd("P");
    	shipments.add(shipment);

        when(shmShipmentSubDAO.listShipmentsByShpInstIds
                 (any(), any(), any()))
            .thenReturn(shipments);

      	when(appContext.getMaxCountForInClause()).thenReturn(5);
    	
    	ListDeliveryCollectionInstructionForShipmentsRqst rqst = new ListDeliveryCollectionInstructionForShipmentsRqst();
       	List<ShipmentId> ids = new ArrayList<ShipmentId>();
       	ShipmentId id1 = new ShipmentId();
       	id1.setShipmentInstId("123456");
       	ids.add(id1);
       	rqst.setShipmentIds(ids);
       	
       	assertDoesNotThrow(() -> {
       		final ListDeliveryCollectionInstructionForShipmentsResp  response = 
           			listDeliveryCollectionInstructionForShipmentsImpl.listDeliveryCollectionInstructionForShipments(rqst, txnContext, entityManager);
       		assertNotNull(response);
       		//PPD chrg to cd has no delivery collection
           	assertEquals(response.getDeliveryCollectionInstructions().get(0).getAcceptCashOnlyInd(), false);
           	assertEquals(response.getDeliveryCollectionInstructions().get(0).getCollectMoneyAtDeliveryCd(), CollectMoneyAtDeliveryCd.NONE);
           	}
       			);
       } catch (Exception e) {
           Assert.assertEquals("ShipmentIds are required", e.getMessage());
       }
   }

   @Test
   public void testListDeliveryCollectionInstructionForShipments_BothShipmentFound() {
       try {
       	
    	   
    	List<ShmShipment> shipments = new ArrayList<ShmShipment>();
    	ShmShipment shipment = new ShmShipment();
    	shipment.setShpInstId(123456);
    	shipment.setProNbrTxt("01110123456");
    	shipment.setBillClassCd("A");
    	shipment.setBillStatCd("4");
    	shipment.setChrgToCd("B");
    	shipments.add(shipment);

        when(shmShipmentSubDAO.listShipmentsByShpInstIds
                 (any(), any(), any()))
            .thenReturn(shipments);

      	when(appContext.getMaxCountForInClause()).thenReturn(5);
    	
    	ListDeliveryCollectionInstructionForShipmentsRqst rqst = new ListDeliveryCollectionInstructionForShipmentsRqst();
       	List<ShipmentId> ids = new ArrayList<ShipmentId>();
       	ShipmentId id1 = new ShipmentId();
       	id1.setShipmentInstId("123456");
       	ids.add(id1);
       	rqst.setShipmentIds(ids);
       	
       	assertDoesNotThrow(() -> {
       		final ListDeliveryCollectionInstructionForShipmentsResp  response = 
           			listDeliveryCollectionInstructionForShipmentsImpl.listDeliveryCollectionInstructionForShipments(rqst, txnContext, entityManager);
       		assertNotNull(response);
       		//BOTH chrg to cd but shipment has null child (misc, ac and as entd)
           	assertEquals(response.getDeliveryCollectionInstructions().get(0).getAcceptCashOnlyInd(), false);
           	assertEquals(response.getDeliveryCollectionInstructions().get(0).getCollectMoneyAtDeliveryCd(), CollectMoneyAtDeliveryCd.NONE);
           	}
       			);
       } catch (Exception e) {
           Assert.assertEquals("ShipmentIds are required", e.getMessage());
       }
   }

}
