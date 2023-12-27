package com.xpo.ltl.shipment.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
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
import com.xpo.ltl.api.shipment.v2.ChildShipmentId;
import com.xpo.ltl.api.shipment.v2.ListHandlingUnitsRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;

public class ListHandlingUnitsImplTest {

    @Mock
    private TransactionContext txnContext;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ShmShipmentSubDAO shmShipmentSubDAO;

    @Mock
	private AppContext appContext;
	
    @InjectMocks
    private ListHandlingUnitsImpl listHandlingUnitsImpl;

    
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
    public void testListHandlingUnitsImpl_TransactionContextRequired() {
        try {
        	listHandlingUnitsImpl.listHandlingUnits(null, null, entityManager);
            Assert.fail("Expected an exception.");
        } catch (Exception e) {
            Assert.assertEquals("TransactionContext is required", e.getMessage());
        }
    }

    @Test
    public void testListHandlingUnitsImpl_EntityManagerRequired() {
        try {
        	listHandlingUnitsImpl.listHandlingUnits(null, txnContext, null);
            Assert.fail("Expected an exception.");
        } catch (Exception e) {
            Assert.assertEquals("EntityManager is required", e.getMessage());
        }
    }

    @Test
    public void testListHandlingUnitsImpl_MixedInputNotAllowed() {
        try {
        	ListHandlingUnitsRqst rqst = new ListHandlingUnitsRqst();
        	List<ChildShipmentId> ids = new ArrayList<ChildShipmentId>();
        	ChildShipmentId id1 = new ChildShipmentId();
        	id1.setShipmentInstId(new Long(123456));
        	id1.setSequenceNbr(BigInteger.ONE);
        	ids.add(id1);
        	ChildShipmentId id2 = new ChildShipmentId();
        	id2.setChildProNbr("123456");
        	ids.add(id2);
        	rqst.setChildShipmentIds(ids);
        	assertThrows(ValidationException.class, () -> {
        	listHandlingUnitsImpl.listHandlingUnits(rqst, txnContext, entityManager);
        	});
        } catch (Exception e) {
            //do nothing
        }
    }

   @Test
    public void testListHandlingUnitsImpl_ShipmentNotFound() {
        try {
        	ListHandlingUnitsRqst rqst = new ListHandlingUnitsRqst();
        	
        	listHandlingUnitsImpl.listHandlingUnits(rqst, txnContext, entityManager);
            Assert.fail("Expected an exception.");
        } catch (Exception e) {
            Assert.assertEquals("ShipmentIds are required", e.getMessage());
        }
    }

//   @Test
//   public void testListHandlingUnitsImpl_PrepaidShipmentFound() {
//       try {
//       	
//    	   
//    	List<ShmShipment> shipments = new ArrayList<ShmShipment>();
//    	ShmShipment shipment = new ShmShipment();
//    	shipment.setShpInstId(123456);
//    	shipment.setProNbrTxt("01110123456");
//    	shipment.setBillClassCd("A");
//    	shipment.setBillStatCd("4");
//    	shipment.setChrgToCd("P");
//    	shipments.add(shipment);
//    	when(shmShipmentSubDAO.listShipmentsByShpInstIds(any(List.class), any(EnumSet.class), any(EntityManager.class))).thenReturn(shipments);   
//      	when(appContext.getMaxCountForInClause()).thenReturn(5);
//    	
//    	ListHandlingUnitsImplRqst rqst = new ListHandlingUnitsImplRqst();
//       	List<ShipmentId> ids = new ArrayList<ShipmentId>();
//       	ShipmentId id1 = new ShipmentId();
//       	id1.setShipmentInstId("123456");
//       	ids.add(id1);
//       	rqst.setShipmentIds(ids);
//       	
//       	assertDoesNotThrow(() -> {
//       		final ListHandlingUnitsImplResp  response = 
//           			listHandlingUnitsImplImpl.listHandlingUnitsImpl(rqst, txnContext, entityManager);
//       		assertNotNull(response);
//       		//PPD chrg to cd has no delivery collection
//           	assertEquals(response.getDeliveryCollectionInstructions().get(0).getAcceptCashOnlyInd(), false);
//           	assertEquals(response.getDeliveryCollectionInstructions().get(0).getCollectMoneyAtDeliveryCd(), CollectMoneyAtDeliveryCd.NONE);
//           	}
//       			);
//       } catch (Exception e) {
//           Assert.assertEquals("ShipmentIds are required", e.getMessage());
//       }
//   }


}
