package com.xpo.ltl.shipment.service.impl;

import static org.mockito.Mockito.when;
import static org.mockito.Matchers.eq;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.MoveChildProsResp;
import com.xpo.ltl.api.shipment.v2.MoveChildProsRqst;
import com.xpo.ltl.api.shipment.v2.ParentProNbrReplacement;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;


import junit.framework.TestCase;


public class MoveChildProsImplTest {
    @Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

	@Mock
	private AuditInfo auditInfo;

    @InjectMocks
    private MoveChildProsImpl moveChildProsImpl;

    @Mock
    private ShmShipmentSubDAO shmShipmentSubDAO;

    @Mock
    private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

    private ShmHandlingUnit hu = null;
     
    private ShmShipment shm = null;

    private ShmShipment shm1 = null;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		when(txnContext.getSrcApplicationId()).thenReturn("JUNIT");

		final User user = new User();
		user.setUserId("JUNIT");
		user.setEmployeeId("JUNIT");
		when(txnContext.getUser()).thenReturn(user);

		when(txnContext.getTransactionTimestamp())
				.thenReturn(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance()));

		when(txnContext.getCorrelationId()).thenReturn("0");
	}

    @Test
    public void testChildPro_OutForDelivery() throws Exception {
            MoveChildProsRqst rqst = new MoveChildProsRqst();
            ParentProNbrReplacement pp = new ParentProNbrReplacement();
            pp.setChildProNbr("06481465104");
            pp.setNewParentProNbr("05670523552");
            pp.setSplitInd(true);
            List<ParentProNbrReplacement> pplist = new ArrayList<ParentProNbrReplacement>();
            pplist.add(pp);
            rqst.setParentProNbrReplacements(pplist);
            hu = new ShmHandlingUnit();
            ShmHandlingUnitPK pk = new ShmHandlingUnitPK();
            pk.setShpInstId(2352L);
            pk.setSeqNbr(3L);
            hu.setId(pk);
            shm = new ShmShipment();
            shm1 = new ShmShipment();
            shm.setShpInstId(2);
            shm1.setShpInstId(3);
            List<ShmShipment> listShm = new ArrayList<ShmShipment>();
            hu.setChildProNbrTxt("06481465104");
            
            hu.setParentProNbrTxt("0987");
            hu.setCurrentTrlrInstId(BigDecimal.ZERO);
            hu.setMvmtStatCd("3");
            shm.setProNbrTxt("05670523552");
            shm1.setProNbrTxt("0987");
            listShm.add(shm);
            listShm.add(shm1);
            when(shmHandlingUnitSubDAO.listByChildProNumbers(any(), any())).thenReturn(Arrays.asList(hu));
            when(shmShipmentSubDAO.findByProNbrs(any(), any())).thenReturn(listShm);
        try{
            MoveChildProsResp resp = moveChildProsImpl.moveChildPros(rqst, txnContext, entityManager);
            fail("Expected an exception.");
        }
        catch (final ValidationException e){
            assertEquals("Child PRO# 06481465104 is out for delivery and cannot be moved to a new parent.", e.getFault().getMoreInfo().get(0).getMessage());
        }
    }

    @Test
    public void testChildPro_loadedOnTrailer() throws Exception {

            MoveChildProsRqst rqst = new MoveChildProsRqst();
            ParentProNbrReplacement pp = new ParentProNbrReplacement();
            pp.setChildProNbr("06481465104");
            pp.setNewParentProNbr("567-523552");
            pp.setSplitInd(true);
            List<ParentProNbrReplacement> pplist = new ArrayList<ParentProNbrReplacement>();
            pplist.add(pp);
            rqst.setParentProNbrReplacements(pplist);
            hu = new ShmHandlingUnit();
            ShmHandlingUnitPK pk = new ShmHandlingUnitPK();
            pk.setShpInstId(2352L);
            pk.setSeqNbr(3L);
            hu.setId(pk);
            shm = new ShmShipment();
            shm1 = new ShmShipment();
            shm1.setShpInstId(12);
            shm.setShpInstId(32);
            List<ShmShipment> listShm = new ArrayList<ShmShipment>();
            hu.setChildProNbrTxt("06481465104");
            hu.setParentProNbrTxt("0987");
            hu.setCurrentTrlrInstId(BigDecimal.ONE);
            shm.setProNbrTxt("05670523552");
            shm1.setProNbrTxt("0987");
            listShm.add(shm);
            listShm.add(shm1);
            when(shmHandlingUnitSubDAO.listByChildProNumbers(any(), any())).thenReturn(Arrays.asList(hu));
            when(shmShipmentSubDAO.findByProNbrs(any(), any())).thenReturn(listShm);
        try{
            MoveChildProsResp resp = moveChildProsImpl.moveChildPros(rqst, txnContext, entityManager);
            fail("Expected an exception.");
        }
        catch (final ValidationException e){
            assertEquals("Child PRO# 06481465104 is loaded on a trailer and cannot be moved to a new parent.", e.getFault().getMoreInfo().get(0).getMessage());
        }
    }

    @Test
    public void testChildPro_FinalDelivered() throws Exception {

            MoveChildProsRqst rqst = new MoveChildProsRqst();
            ParentProNbrReplacement pp = new ParentProNbrReplacement();
            pp.setChildProNbr("6481-465104");
            pp.setNewParentProNbr("567-523552");
            pp.setSplitInd(true);
            List<ParentProNbrReplacement> pplist = new ArrayList<ParentProNbrReplacement>();
            pplist.add(pp);
            rqst.setParentProNbrReplacements(pplist);
            hu = new ShmHandlingUnit();
            ShmHandlingUnitPK pk = new ShmHandlingUnitPK();
            pk.setShpInstId(2352L);
            pk.setSeqNbr(3L);
            hu.setId(pk);
            shm = new ShmShipment();
            shm1 = new ShmShipment();
            shm1.setShpInstId(12);
            shm.setShpInstId(32);
            List<ShmShipment> listShm = new ArrayList<ShmShipment>();
            hu.setChildProNbrTxt("06481465104");
            hu.setParentProNbrTxt("0987");
            hu.setCurrentTrlrInstId(BigDecimal.ZERO);
            hu.setMvmtStatCd("5");
            shm.setProNbrTxt("05670523552");
            shm1.setProNbrTxt("0987");
            listShm.add(shm);
            listShm.add(shm1);
            when(shmHandlingUnitSubDAO.listByChildProNumbers(any(), any())).thenReturn(Arrays.asList(hu));
            when(shmShipmentSubDAO.findByProNbrs(any(), any())).thenReturn(listShm);
        try{
            MoveChildProsResp resp = moveChildProsImpl.moveChildPros(rqst, txnContext, entityManager);
            fail("Expected an exception.");
        }
        catch (final ValidationException e){
            assertEquals("Child PRO# 06481465104 is final delivered and cannot be moved to a new parent.", e.getFault().getMoreInfo().get(0).getMessage());
        }
    }

    @Test
    public void testParentPro_FinalDelivered() throws Exception {
            MoveChildProsRqst rqst = new MoveChildProsRqst();
            ParentProNbrReplacement pp = new ParentProNbrReplacement();
            pp.setChildProNbr("06481465104");
            pp.setNewParentProNbr("05670523552");
            pp.setSplitInd(true);
            List<ParentProNbrReplacement> pplist = new ArrayList<ParentProNbrReplacement>();
            pplist.add(pp);
            rqst.setParentProNbrReplacements(pplist);
            hu = new ShmHandlingUnit();
            shm = new ShmShipment();
            ShmHandlingUnitPK pk = new ShmHandlingUnitPK();
            pk.setShpInstId(2352L);
            pk.setSeqNbr(3L);
            hu.setId(pk);
            shm1 = new ShmShipment();
            List<ShmShipment> listShm = new ArrayList<ShmShipment>();
            hu.setChildProNbrTxt("06481465104");
            hu.setParentProNbrTxt("0987");
            shm.setProNbrTxt("05670523552");
            shm1.setProNbrTxt("0987");
            
            hu.setCurrentTrlrInstId(BigDecimal.ZERO);
            shm.setDlvryQalfrCd("Z");
            shm1.setDlvryQalfrCd("Z");
            listShm.add(shm);
            listShm.add(shm1);
            when(shmHandlingUnitSubDAO.listByChildProNumbers(any(), any())).thenReturn(Arrays.asList(hu));
            when(shmShipmentSubDAO.findByProNbrs(any(), any())).thenReturn(listShm);
        try{
            MoveChildProsResp resp = moveChildProsImpl.moveChildPros(rqst, txnContext, entityManager);
            fail("Expected an exception.");

        }
        catch (final ValidationException e){
            assertEquals("The new parent pro# 05670523552 is final delivered and cannot be used.", e.getFault().getMoreInfo().get(0).getMessage());
        }
    }

    
}
