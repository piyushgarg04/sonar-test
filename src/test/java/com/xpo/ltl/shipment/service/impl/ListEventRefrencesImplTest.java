package com.xpo.ltl.shipment.service.impl;

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

import com.xpo.ltl.api.rest.ListMetadata;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.service.entity.ShmEventReference;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmEventReferenceSubDAO;

public class ListEventRefrencesImplTest {

    @Mock
    private TransactionContext txnContext;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ShmEventReferenceSubDAO shmEventReferenceSubDAO;
	
    @InjectMocks
    private ListEventRefrencesImpl listEventRefrencesImpl;

    
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
    public void testListEventRefrencesImpl_TransactionContextRequired() {
        try {
        	listEventRefrencesImpl.listEventReferences(null, null, entityManager);
            Assert.fail("Expected an exception.");
        } catch (Exception e) {
            Assert.assertEquals("Transaction Context is required", e.getMessage());
        }
    }

    @Test
    public void testListEventRefrencesImpl_EntityManagerRequired() {
        try {
            listEventRefrencesImpl.listEventReferences(null, txnContext, null);
            Assert.fail("Expected an exception.");
        } catch (Exception e) {
            Assert.assertEquals("EntityManager is required", e.getMessage());
        }
    }

    @Test
    public void testListEventRefrencesImpl_ListEventReferences() {
            List<ShmEventReference> eventReference = new ArrayList<>();
            ListMetadata listMetadata = new ListMetadata();
            listMetadata.setStartAt(BigInteger.ZERO);
            listMetadata.setTotalRowCount(BigInteger.valueOf(50));
            
            Integer start = listMetadata.getStartAt().intValue();
            Integer totalRows = listMetadata.getTotalRowCount().intValue();
            
            when(shmEventReferenceSubDAO.listEventReferences(totalRows, start, entityManager)).thenReturn(eventReference);
       
    }

}
