package com.xpo.ltl.shipment.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitPK;
import com.xpo.ltl.api.shipment.service.entity.ShmHazMat;
import com.xpo.ltl.api.shipment.service.entity.ShmHazMatPK;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcpPK;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementPK;
import com.xpo.ltl.api.shipment.service.entity.ShmNotification;
import com.xpo.ltl.api.shipment.service.entity.ShmOpsShipment;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.service.entity.ShmSrNbr;
import com.xpo.ltl.api.shipment.service.entity.ShmSrNbrPK;
import com.xpo.ltl.api.shipment.service.entity.ShmTmDtCritical;
import com.xpo.ltl.api.shipment.v2.GetShipmentResp;
import com.xpo.ltl.api.shipment.v2.ShipmentDetailCd;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.ShipmentMovementExceptionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentSupRefSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentTdcSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmBillEntryStatsSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmCmdyDimensionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmEventLogSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmMvmtExcpActionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmNotificationSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmXdockExcpSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShipmentDetailsDelegate;
import com.xpo.ltl.shipment.service.validators.ShipmentRequestsValidator;

public class GetShipmentImplTest {

    @Mock
    private TransactionContext txnContext;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ShmShipmentSubDAO shmShipmentSubDAO;

    @Mock
    private ShipmentTdcSubDAO shipmentTdcSubDAO;

    @Mock
    private ShmXdockExcpSubDAO shmXdockExcpSubDAO;

    @Mock
    private ShmNotificationSubDAO shmNotificationSubDAO;

    @Mock
    private ShmEventLogSubDAO shmEventLogSubDAO;

    @Mock
    private ShipmentRequestsValidator getShipmentsRequestValidator;

    @Mock
    private ShmBillEntryStatsSubDAO shmBillEntryStatsSubDAO;

    @Mock
    private ExternalRestClient externalRestClient;

    @Mock
    private ShipmentMovementExceptionSubDAO shipmentMovementExceptionSubDAO;

    @Mock
    private ShmMvmtExcpActionSubDAO shmMvmtExcpActionSubDAO;

    @Mock
    private ShmCmdyDimensionSubDAO shmCmdyDimensionSubDAO;

    @Mock
    private AppContext appContext;

    @Spy
    private ShipmentDetailsDelegate shipmentDetailsDelegate;
    
    @Mock
    private ShipmentSupRefSubDAO shipmentSupRefSubDAO;

    @InjectMocks
    private GetShipmentImpl getShipmentImpl;


    private static final long shipmentIntsId = 123456L;
    private static final String SHM_SR_NBR_TV_TYP_CD = "TV";
    private static final String SHM_SR_NBR_GZHASH_TYP_CD = "GZ#";
    private static final String SHM_SR_NBR_POHASH_TYP_CD = "PO#";
    private static final String SHM_SR_NBR_LTL_OBP_NBR_TXT = "LTL_OBP";
    private static final String SHM_SR_NBR_GZHASH_NBR_TXT = "RefGZ123";
    private static final String SHM_SR_NBR_POHASH_NBR_TXT = "RefPO987";

    @Before
    public void setUp() throws Throwable {
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
    public void testTrnContextRequired() {
        try {
            getShipmentImpl.getShipment(null, null, 123456L, null, null, null, null, entityManager);
            Assert.fail("Expected an exception.");
        } catch (Exception e) {
            Assert.assertEquals("TransactionContext is required", e.getMessage());
        }
    }

    @Test
    public void testEntityManagerRequired() {
        try {
            getShipmentImpl.getShipment(null, null, 123456L, null, null, null, txnContext, null);
            Assert.fail("Expected an exception.");
        } catch (Exception e) {
            Assert.assertEquals("EntityManager is required", e.getMessage());
        }
    }

    @Test
    public void testGetShipmentWithMovmentException() throws ServiceException {

        ShmShipment shmShipment = getShipmentData(ShipmentDetailCd.MOVEMENT_EXCEPTION);

        when(shmShipmentSubDAO.findByProOrShipmentId
                 (any(), any(), any(), anyBoolean(), any(), any()))
            .thenReturn(shmShipment);

        when(shipmentMovementExceptionSubDAO.findByShpInstIds(any(), any()))
                .thenReturn(shmShipment.getShmMovements().get(0).getShmMovementExcps());
        when(shmMvmtExcpActionSubDAO.findByShpInstIds(any(), any())).thenReturn(Collections.EMPTY_LIST);

        GetShipmentResp resp = getShipmentImpl.getShipment(null, null, shipmentIntsId,
                new ShipmentDetailCd[] {ShipmentDetailCd.MOVEMENT_EXCEPTION}, null, null, txnContext, entityManager);

        Assert.assertEquals(BigInteger.ONE, resp.getMovement().get(0).getSequenceNbr());
        Assert.assertEquals(BigInteger.valueOf(2), resp.getMovement().get(1).getSequenceNbr());
        Assert.assertEquals(BigInteger.valueOf(3), resp.getMovement().get(2).getSequenceNbr());

        Assert.assertEquals(BigInteger.ONE, resp.getMovement().get(0).getMovementException().get(0).getSequenceNbr());
        Assert.assertEquals(BigInteger.valueOf(2), resp.getMovement().get(0).getMovementException().get(1).getSequenceNbr());
        Assert.assertEquals(BigInteger.valueOf(3), resp.getMovement().get(0).getMovementException().get(2).getSequenceNbr());
    }

    @Test
    public void testGetShipment9Digit() throws ServiceException {
        String proNbr = "208-966063";
        String pickupDate = String.valueOf("2020-08-24 11:00:11");
        Long shipmentInstId = null;
        ShipmentDetailCd[] shipmentDetailCds = { ShipmentDetailCd.ACCESSORIAL };
        ShmShipment shm = new ShmShipment();
        shm.setPrcAgrmtId(new BigDecimal("123"));
        shm.setShmHandlingUnits(Collections.emptyList());

        when(shmShipmentSubDAO.findByProOrShipmentId
                 (any(), any(), any(), anyBoolean(), any(), any()))
            .thenReturn(shm);

        GetShipmentResp result = this.getShipmentImpl.getShipment(proNbr, pickupDate,
                shipmentInstId, shipmentDetailCds, null, null, txnContext, entityManager);

        assertNotNull(result);
    }

    @Test
    public void testGetShipmentWithShipmentONLY() throws ServiceException {
        ShmShipment shmShipment = getShipmentData(ShipmentDetailCd.SHIPMENT_ONLY);

        when(shmShipmentSubDAO.findByProOrShipmentId
                 (any(), any(), any(), anyBoolean(), any(), any()))
            .thenReturn(shmShipment);

        GetShipmentResp resp = getShipmentImpl.getShipment(null, null, shipmentIntsId,
                new ShipmentDetailCd[] {ShipmentDetailCd.SHIPMENT_ONLY}, null, null, txnContext, entityManager);

        assertNotNull(resp);
        assertNotNull(resp.getShipment());
        assertNotNull(resp.getOperationsShipment());
        assertEquals(1, CollectionUtils.size(resp.getHandlingUnits()));
        assertEquals(1, CollectionUtils.size(resp.getHazMat()));
    }

    @Test
    public void testGetShipmentWithHandlingUnits() throws ServiceException {
        ShmShipment shmShipment = getShipmentData(ShipmentDetailCd.HANDLING_UNIT);

        when(shmShipmentSubDAO.findByProOrShipmentId
                 (any(), any(), any(), anyBoolean(), any(), any()))
            .thenReturn(shmShipment);

        GetShipmentResp resp = getShipmentImpl.getShipment(null, null, shipmentIntsId,
                new ShipmentDetailCd[] {ShipmentDetailCd.HANDLING_UNIT}, null, null, txnContext, entityManager);

        assertNotNull(resp);
        assertNotNull(resp.getShipment());
        assertEquals(1, CollectionUtils.size(resp.getHandlingUnits()));


    }

    @Test
    public void testGetShipmentWithTimeDateCritical() throws ServiceException {
        ShmShipment shmShipment = getShipmentData(ShipmentDetailCd.TIME_DATE_CRITICAL);

        when(shmShipmentSubDAO.findByProOrShipmentId
                 (any(), any(), any(), anyBoolean(), any(), any()))
            .thenReturn(shmShipment);

        when(shipmentTdcSubDAO.findById(shipmentIntsId, entityManager)).thenReturn(new ShmTmDtCritical());

        GetShipmentResp resp = getShipmentImpl.getShipment(null, null, shipmentIntsId,
                new ShipmentDetailCd[] {ShipmentDetailCd.TIME_DATE_CRITICAL}, null, null, txnContext, entityManager);

        assertNotNull(resp);
        assertNotNull(resp.getShipment());
        verify(shipmentTdcSubDAO,Mockito.times(1))
                .findById(shipmentIntsId, entityManager);
        assertNotNull(resp.getTimeDateCritical());
    }

    @Test
    public void testGetShipmentWithTimeNotifications() throws ServiceException {
        ShmShipment shmShipment = getShipmentData(ShipmentDetailCd.NOTIFICATION);

        when(shmShipmentSubDAO.findByProOrShipmentId
                 (any(), any(), any(), anyBoolean(), any(), any()))
            .thenReturn(shmShipment);

        when(shmNotificationSubDAO.findByShipmentInstId(shipmentIntsId, entityManager))
                .thenReturn(Arrays.asList(new ShmNotification()));

        GetShipmentResp resp = getShipmentImpl.getShipment(null, null, shipmentIntsId,
                new ShipmentDetailCd[] {ShipmentDetailCd.NOTIFICATION}, null, null, txnContext, entityManager);

        assertNotNull(resp);
        assertNotNull(resp.getShipment());
        verify(shmNotificationSubDAO,Mockito.times(1))
                .findByShipmentInstId(shipmentIntsId, entityManager);
        assertNotNull(resp.getNotifications());
        assertEquals(1, CollectionUtils.size(resp.getNotifications()));
    }
    
    @Test
    public void testGetShipmentWithNoShipmentAndNoSuppRefNbrProvided() throws ServiceException {
        ShmShipment shmShipment = getShipmentData(ShipmentDetailCd.SUPP_REF_NBR);


        when(shmShipmentSubDAO.findByProOrShipmentId(any(), any(), any(), anyBoolean(), any(),
                any())).thenReturn(shmShipment);

        GetShipmentResp resp = getShipmentImpl.getShipment(null, null, shipmentIntsId,
                new ShipmentDetailCd[] {ShipmentDetailCd.NO_SHIPMENT, ShipmentDetailCd.SUPP_REF_NBR}, null, null, txnContext, entityManager);

        assertNotNull(resp);
        assertEquals(null, resp.getShipment());
        assertNotNull(resp.getSuppRefNbr());
        assertEquals(3, CollectionUtils.size(resp.getSuppRefNbr()));
    }
    
    @Test
    public void testGetShipmentWithNoShipmentAndSuppRefNbr() throws ServiceException {
        ShmShipment shmShipment = getShipmentData(ShipmentDetailCd.NO_SHIPMENT);

        when(shmShipmentSubDAO.findByProOrShipmentId(any(), any(), any(), anyBoolean(), any(),
                any())).thenReturn(shmShipment);
        when(shipmentSupRefSubDAO.findByShpInstIdAndShmSrNbrTypCds(any(), any(), any())).thenReturn(buildShmSrNbrForNoShipmentDetailCd());

        GetShipmentResp resp = getShipmentImpl.getShipment(null, null, shipmentIntsId,
                new ShipmentDetailCd[] {ShipmentDetailCd.NO_SHIPMENT, ShipmentDetailCd.SUPP_REF_NBR}, new String[] {SHM_SR_NBR_TV_TYP_CD}, null, txnContext, entityManager);

        assertNotNull(resp);
        assertEquals(null, resp.getShipment());
        assertNotNull(resp.getSuppRefNbr());
        assertEquals(1, CollectionUtils.size(resp.getSuppRefNbr()));
        assertEquals(SHM_SR_NBR_LTL_OBP_NBR_TXT, resp.getSuppRefNbr().get(0).getRefNbr());
    }

	private ShmShipment getShipmentData(ShipmentDetailCd shipmentDetailCd) {
        ShmShipment shmShipment = new ShmShipment();
        shmShipment.setShpInstId(shipmentIntsId);
        shmShipment.setPrcAgrmtId(BigDecimal.ONE);

        shmShipment.setShmHandlingUnits(Collections.emptyList());

        if(shipmentDetailCd == ShipmentDetailCd.SHIPMENT_ONLY) {
            shmShipment.setShmHandlingUnits(buildShmHandlingUnits());
            shmShipment.setShmHazMats(buildShmHazmats());
            ShmOpsShipment shmOpsShipment = new ShmOpsShipment();
            shmOpsShipment.setShpInstId(shipmentIntsId);
            shmShipment.setShmOpsShipment(shmOpsShipment);
        }

        if(shipmentDetailCd == ShipmentDetailCd.HANDLING_UNIT) {
            shmShipment.setShmHandlingUnits(buildShmHandlingUnits());
        }

        if(shipmentDetailCd == ShipmentDetailCd.MOVEMENT_EXCEPTION) {
            shmShipment.setShmMovements(buildShmMovementsList());
        }
        if(shipmentDetailCd == ShipmentDetailCd.SUPP_REF_NBR) {
        	shmShipment.setShmSrNbrs(buildShmSrNbrs());
        }

        return shmShipment;
    }

    private List<ShmHandlingUnit> buildShmHandlingUnits() {
        List<ShmHandlingUnit> shmHandlingUnits = Lists.newArrayList();
        ShmHandlingUnit shmHandlingUnit = new ShmHandlingUnit();

        ShmHandlingUnitPK key = new ShmHandlingUnitPK();
        key.setShpInstId(shipmentIntsId);
        key.setSeqNbr(1);
        shmHandlingUnit.setId(key);
        shmHandlingUnits.add(shmHandlingUnit);

        return shmHandlingUnits;
    }

    private List<ShmHazMat> buildShmHazmats() {
        List<ShmHazMat> shmHazMats = Lists.newArrayList();
        ShmHazMat shmHazMat = new ShmHazMat();

        ShmHazMatPK key = new ShmHazMatPK();
        key.setShpInstId(shipmentIntsId);
        key.setHmSeqNbr(1);
        shmHazMat.setId(key);
        shmHazMats.add(shmHazMat);

        return shmHazMats;
    }

    private List<ShmMovement> buildShmMovementsList() {
        List<ShmMovement> shmMovementList = Lists.newArrayList();
        ShmMovement shmMovement1 = buildShmMovement(1L);
        ShmMovement shmMovement2 = buildShmMovement(2L);
        ShmMovement shmMovement3 = buildShmMovement(3L);

        shmMovementList.add(shmMovement1);
        shmMovementList.add(shmMovement2);
        shmMovementList.add(shmMovement3);
        return shmMovementList;
    }

    private ShmMovement buildShmMovement(long sequenceNumber) {
        ShmMovement shmMovement = new ShmMovement();
        ShmMovementPK shmMovementPK = new ShmMovementPK();
        shmMovementPK.setSeqNbr(sequenceNumber);
        shmMovementPK.setShpInstId(shipmentIntsId);
        shmMovement.setId(shmMovementPK);
        shmMovement.setShmMovementExcps(buildShmMovementExcpsList(sequenceNumber));
        return shmMovement;
    }

    private List<ShmMovementExcp> buildShmMovementExcpsList(long mvmtSeqNbr) {
        List<ShmMovementExcp> shmMovementList = Lists.newArrayList();
        ShmMovementExcp shmMovementExcp1 = buildShmMovementExcp(mvmtSeqNbr, 2L);
        ShmMovementExcp shmMovementExcp2 = buildShmMovementExcp(mvmtSeqNbr,1L);
        ShmMovementExcp shmMovementExcp3 = buildShmMovementExcp(mvmtSeqNbr, 3L);

        shmMovementList.add(shmMovementExcp1);
        shmMovementList.add(shmMovementExcp2);
        shmMovementList.add(shmMovementExcp3);
        return shmMovementList;
    }

    private ShmMovementExcp buildShmMovementExcp(long mvmtSeqNbr,  long sequenceNumber) {
        ShmMovementExcp shmMovementExcp = new ShmMovementExcp();
        ShmMovementExcpPK shmMovementExcpPK = new ShmMovementExcpPK();
        shmMovementExcpPK.setSeqNbr(sequenceNumber);
        shmMovementExcpPK.setMvmtSeqNbr(mvmtSeqNbr);
        shmMovementExcpPK.setShpInstId(shipmentIntsId);
        shmMovementExcp.setId(shmMovementExcpPK);
        return shmMovementExcp;
    }
    
    private List<ShmSrNbr> buildShmSrNbrs() {
    	final List<ShmSrNbr> srNbrs = Lists.newArrayList();
    	final String[] typCds = {SHM_SR_NBR_GZHASH_TYP_CD, SHM_SR_NBR_TV_TYP_CD, SHM_SR_NBR_POHASH_TYP_CD};
    	final String[] nbrTxts = {SHM_SR_NBR_GZHASH_NBR_TXT, SHM_SR_NBR_LTL_OBP_NBR_TXT, SHM_SR_NBR_POHASH_NBR_TXT};
    	for(int i=0; i < typCds.length; i++) {
	    	final ShmSrNbr entity = buildShmSrNbr(shipmentIntsId, (long) i+1, typCds[i], nbrTxts[i]);
	    	srNbrs.add(entity);
    	}
    	
		return srNbrs;
    }
    
    private List<ShmSrNbr> buildShmSrNbrForNoShipmentDetailCd() {
    	final List<ShmSrNbr> srNbrs = Lists.newArrayList();
    	final ShmSrNbr entity = buildShmSrNbr(shipmentIntsId, 1L, SHM_SR_NBR_TV_TYP_CD, SHM_SR_NBR_LTL_OBP_NBR_TXT);
    	srNbrs.add(entity);
    	
		return srNbrs;
	}
    
    private ShmSrNbr buildShmSrNbr(final long shpInstId, final long seqNbr, final String typCd, final String nbrTxt) {
    	final ShmSrNbr entity = new ShmSrNbr();
    	final ShmSrNbrPK pk = new ShmSrNbrPK();
    	pk.setShpInstId(shpInstId);
    	pk.setSeqNbr(seqNbr);
    	pk.setTypCd(typCd);
    	entity.setId(pk);
    	entity.setNbrTxt(nbrTxt);
    	
    	return entity;
    }
}
