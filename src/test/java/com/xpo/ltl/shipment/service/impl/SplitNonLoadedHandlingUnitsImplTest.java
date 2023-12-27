package com.xpo.ltl.shipment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.location.v2.GetRefSicAddressResp;
import com.xpo.ltl.api.location.v2.LocationAddress;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmt;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmtPK;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.NonLoadedHandlingUnit;
import com.xpo.ltl.api.shipment.v2.SplitNonLoadedHandlingUnitsResp;
import com.xpo.ltl.api.shipment.v2.SplitNonLoadedHandlingUnitsRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShmEventLogSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitMvmtSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmEventDelegate;

public class SplitNonLoadedHandlingUnitsImplTest {

    @Mock
    private TransactionContext txnContext;

    @Mock
    private EntityManager entityManager;

    @Mock
    private EntityManager db2EntityManager;

    @Mock
    private ExternalRestClient externalRestClient;

    @Mock
    private ShmShipmentSubDAO shmShipmentSubDAO;

    @Mock
    private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

    @Mock
    private ShmHandlingUnitMvmtSubDAO shmHandlingUnitMvmtSubDAO;

    @InjectMocks
    private SplitNonLoadedHandlingUnitsImpl splitNonLoadedHandlingUnitsImpl;

    @Mock
    private ShmEventLogSubDAO shmEventLogSubDAO;

    @Mock
    private ShmEventDelegate shmEventDelegate;

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

    @Test(expected = NullPointerException.class)
    public void testSplitNonLoadedHandlingUnits_RequestRequired() throws ServiceException {
        splitNonLoadedHandlingUnitsImpl.splitNonLoadedHandlingUnits(null, txnContext, entityManager);
    }


    @Test
    public void testSplitNonLoadedHandlingUnits_EmptySplitAuthBy() throws ServiceException {

        SplitNonLoadedHandlingUnitsRqst req = new SplitNonLoadedHandlingUnitsRqst();
        req.setNonLoadedHandlingUnits(Collections.emptyList());

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            splitNonLoadedHandlingUnitsImpl.splitNonLoadedHandlingUnits(req, txnContext, entityManager);
        });

        assertEquals(1, exception.getFault().getMoreInfo().size());
        assertEquals("N/A", exception.getFault().getMoreInfo().get(0).getLocation());
        assertEquals("Split authorized by param is required.", exception.getFault().getMoreInfo().get(0).getMessage());
    }

    @Test
    public void testSplitNonLoadedHandlingUnits_EmptySplitAuthDateTime() throws ServiceException {

        SplitNonLoadedHandlingUnitsRqst req = new SplitNonLoadedHandlingUnitsRqst();
        req.setNonLoadedHandlingUnits(Collections.emptyList());
        req.setSplitAuthorizedBy("U1234");

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            splitNonLoadedHandlingUnitsImpl.splitNonLoadedHandlingUnits(req, txnContext, entityManager);
        });

        assertEquals(1, exception.getFault().getMoreInfo().size());
        assertEquals("N/A", exception.getFault().getMoreInfo().get(0).getLocation());
        assertEquals("Split authorized date time param is required.",
            exception.getFault().getMoreInfo().get(0).getMessage());
    }

    @Test
    public void testSplitNonLoadedHandlingUnits_EmptyProList() throws ServiceException {

        SplitNonLoadedHandlingUnitsRqst req = new SplitNonLoadedHandlingUnitsRqst();
        req.setNonLoadedHandlingUnits(Collections.emptyList());
        req.setSplitAuthorizedBy("U1234");
        req.setSplitAuthorizedDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            splitNonLoadedHandlingUnitsImpl.splitNonLoadedHandlingUnits(req, txnContext, entityManager);
        });

        assertEquals(1, exception.getFault().getMoreInfo().size());
        assertEquals("N/A", exception.getFault().getMoreInfo().get(0).getLocation());
        assertEquals("At least one NonLoadedHandlingUnit is required.",
            exception.getFault().getMoreInfo().get(0).getMessage());
    }

    @Test
    public void testSplitNonLoadedHandlingUnits_OneButEmptyNonLoadedHU() throws ServiceException {

        SplitNonLoadedHandlingUnitsRqst req = new SplitNonLoadedHandlingUnitsRqst();
        req.setNonLoadedHandlingUnits(Arrays.asList(new NonLoadedHandlingUnit()));
        req.setSplitAuthorizedBy("U1234");
        req.setSplitAuthorizedDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            splitNonLoadedHandlingUnitsImpl.splitNonLoadedHandlingUnits(req, txnContext, entityManager);
        });

        assertEquals(1, exception.getFault().getMoreInfo().size());
        assertEquals("N/A", exception.getFault().getMoreInfo().get(0).getLocation());
        assertEquals("At least one NonLoadedHandlingUnit is required.",
            exception.getFault().getMoreInfo().get(0).getMessage());
    }

    @Test
    public void testSplitNonLoadedHandlingUnits_MissingChildProAndTrailer() throws ServiceException {

        SplitNonLoadedHandlingUnitsRqst req = new SplitNonLoadedHandlingUnitsRqst();
        NonLoadedHandlingUnit nonLoadedHandlingUnit = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit.setParentProNbr("06470492775");
        List<NonLoadedHandlingUnit> nonLoadedHUList = new ArrayList<NonLoadedHandlingUnit>();
        nonLoadedHUList.add(nonLoadedHandlingUnit);
        req.setNonLoadedHandlingUnits(nonLoadedHUList);
        req.setSplitAuthorizedBy("U1234");
        req.setSplitAuthorizedDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            splitNonLoadedHandlingUnitsImpl.splitNonLoadedHandlingUnits(req, txnContext, entityManager);
        });

        assertEquals(1, exception.getFault().getMoreInfo().size());
        assertEquals("647-492775", exception.getFault().getMoreInfo().get(0).getLocation());
        assertEquals("Child PRO is required.",
            exception.getFault().getMoreInfo().get(0).getMessage());
    }

    @Test
    public void testSplitNonLoadedHandlingUnits_MissingParentProAndOneNotRelated() throws ServiceException {

        SplitNonLoadedHandlingUnitsRqst req = new SplitNonLoadedHandlingUnitsRqst();
        NonLoadedHandlingUnit nonLoadedHandlingUnit = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit.setChildProNbr("06481009990");
        nonLoadedHandlingUnit.setTrailerNbr(100L);
        NonLoadedHandlingUnit nonLoadedHandlingUnit2 = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit2.setParentProNbr("06470492775");
        nonLoadedHandlingUnit2.setChildProNbr("09572015073");
        nonLoadedHandlingUnit2.setTrailerNbr(100L);
        List<NonLoadedHandlingUnit> nonLoadedHUList = new ArrayList<NonLoadedHandlingUnit>();
        nonLoadedHUList.add(nonLoadedHandlingUnit);
        nonLoadedHUList.add(nonLoadedHandlingUnit2);
        req.setNonLoadedHandlingUnits(nonLoadedHUList);
        req.setSplitAuthorizedBy("U1234");
        req.setSplitAuthorizedDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            splitNonLoadedHandlingUnitsImpl.splitNonLoadedHandlingUnits(req, txnContext, entityManager);
        });
        assertEquals(1, exception.getFault().getMoreInfo().size());
        assertEquals("N/A", exception.getFault().getMoreInfo().get(0).getLocation());
        assertEquals("Parent PRO is required for input child PRO 6481-009990.",
            exception.getFault().getMoreInfo().get(0).getMessage());
    }

    @Test
    public void testSplitNonLoadedHandlingUnits_ParentProInvalidAndMissingChildProAndTrailer() throws ServiceException {

        SplitNonLoadedHandlingUnitsRqst req = new SplitNonLoadedHandlingUnitsRqst();
        NonLoadedHandlingUnit nonLoadedHandlingUnit = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit.setParentProNbr("123");
        List<NonLoadedHandlingUnit> nonLoadedHUList = new ArrayList<NonLoadedHandlingUnit>();
        nonLoadedHUList.add(nonLoadedHandlingUnit);
        req.setNonLoadedHandlingUnits(nonLoadedHUList);
        req.setSplitAuthorizedBy("U1234");
        req.setSplitAuthorizedDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            splitNonLoadedHandlingUnitsImpl.splitNonLoadedHandlingUnits(req, txnContext, entityManager);
        });

        assertEquals(1, exception.getFault().getMoreInfo().size());
        assertEquals("123", exception.getFault().getMoreInfo().get(0).getLocation());
        assertEquals("Parent PRO is invalid.",
            exception.getFault().getMoreInfo().get(0).getMessage());
    }

    @Test
    public void testSplitNonLoadedHandlingUnits_MissingChildPro() throws ServiceException {

        SplitNonLoadedHandlingUnitsRqst req = new SplitNonLoadedHandlingUnitsRqst();
        NonLoadedHandlingUnit nonLoadedHandlingUnit = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit.setParentProNbr("06470492775");
        nonLoadedHandlingUnit.setTrailerNbr(10L);
        List<NonLoadedHandlingUnit> nonLoadedHUList = new ArrayList<NonLoadedHandlingUnit>();
        nonLoadedHUList.add(nonLoadedHandlingUnit);
        req.setNonLoadedHandlingUnits(nonLoadedHUList);
        req.setSplitAuthorizedBy("U1234");
        req.setSplitAuthorizedDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            splitNonLoadedHandlingUnitsImpl.splitNonLoadedHandlingUnits(req, txnContext, entityManager);
        });

        assertEquals(1, exception.getFault().getMoreInfo().size());
        assertEquals("647-492775", exception.getFault().getMoreInfo().get(0).getLocation());
        assertEquals("Child PRO is required.",
            exception.getFault().getMoreInfo().get(0).getMessage());
    }

    @Test
    public void testSplitNonLoadedHandlingUnits_InvalidChildPro() throws ServiceException {

        SplitNonLoadedHandlingUnitsRqst req = new SplitNonLoadedHandlingUnitsRqst();
        NonLoadedHandlingUnit nonLoadedHandlingUnit = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit.setParentProNbr("06470492775");
        nonLoadedHandlingUnit.setChildProNbr("123");
        nonLoadedHandlingUnit.setTrailerNbr(10L);
        List<NonLoadedHandlingUnit> nonLoadedHUList = new ArrayList<NonLoadedHandlingUnit>();
        nonLoadedHUList.add(nonLoadedHandlingUnit);
        req.setNonLoadedHandlingUnits(nonLoadedHUList);
        req.setSplitAuthorizedBy("U1234");
        req.setSplitAuthorizedDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            splitNonLoadedHandlingUnitsImpl.splitNonLoadedHandlingUnits(req, txnContext, entityManager);
        });

        assertEquals(1, exception.getFault().getMoreInfo().size());
        assertEquals("647-492775", exception.getFault().getMoreInfo().get(0).getLocation());
        assertEquals("Child PRO 123 is invalid.",
            exception.getFault().getMoreInfo().get(0).getMessage());
    }

    @Test
    public void testSplitNonLoadedHandlingUnits_OneInvalidChildProAndOneMissingChildPro() throws ServiceException {

        SplitNonLoadedHandlingUnitsRqst req = new SplitNonLoadedHandlingUnitsRqst();
        NonLoadedHandlingUnit nonLoadedHandlingUnit = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit.setParentProNbr("06470492775");
        nonLoadedHandlingUnit.setChildProNbr("123");
        nonLoadedHandlingUnit.setTrailerNbr(10L);
        NonLoadedHandlingUnit nonLoadedHandlingUnit2 = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit2.setParentProNbr("06470492775");
        nonLoadedHandlingUnit2.setTrailerNbr(10L);
        List<NonLoadedHandlingUnit> nonLoadedHUList = new ArrayList<NonLoadedHandlingUnit>();
        nonLoadedHUList.add(nonLoadedHandlingUnit);
        nonLoadedHUList.add(nonLoadedHandlingUnit2);
        req.setNonLoadedHandlingUnits(nonLoadedHUList);
        req.setSplitAuthorizedBy("U1234");
        req.setSplitAuthorizedDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            splitNonLoadedHandlingUnitsImpl.splitNonLoadedHandlingUnits(req, txnContext, entityManager);
        });

        assertEquals(2, exception.getFault().getMoreInfo().size());
        assertEquals("647-492775", exception.getFault().getMoreInfo().get(0).getLocation());
        assertEquals("Child PRO 123 is invalid.",
            exception.getFault().getMoreInfo().get(0).getMessage());
        assertEquals("647-492775", exception.getFault().getMoreInfo().get(1).getLocation());
        assertEquals("Child PRO is required.",
            exception.getFault().getMoreInfo().get(1).getMessage());
    }

    @Test
    public void testSplitNonLoadedHandlingUnits_OneChildAtSameTrailer() throws ServiceException {

        SplitNonLoadedHandlingUnitsRqst req = new SplitNonLoadedHandlingUnitsRqst();
        NonLoadedHandlingUnit nonLoadedHandlingUnit = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit.setParentProNbr("06470492775");
        nonLoadedHandlingUnit.setChildProNbr("06481008041");
        nonLoadedHandlingUnit.setTrailerNbr(10L);
        nonLoadedHandlingUnit.setRequestingSicCd("UPO");
        NonLoadedHandlingUnit nonLoadedHandlingUnit2 = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit2.setParentProNbr("06470493184");
        nonLoadedHandlingUnit2.setChildProNbr("06481009990");
        nonLoadedHandlingUnit2.setTrailerNbr(20L);
        nonLoadedHandlingUnit2.setRequestingSicCd("UPO");
        NonLoadedHandlingUnit nonLoadedHandlingUnit3 = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit3.setParentProNbr("06480008222");
        nonLoadedHandlingUnit3.setChildProNbr("09572015073");
        nonLoadedHandlingUnit3.setTrailerNbr(85396354L);
        nonLoadedHandlingUnit3.setRequestingSicCd("UPO");
        List<NonLoadedHandlingUnit> nonLoadedHUList = new ArrayList<NonLoadedHandlingUnit>();
        nonLoadedHUList.add(nonLoadedHandlingUnit);
        nonLoadedHUList.add(nonLoadedHandlingUnit2);
        nonLoadedHUList.add(nonLoadedHandlingUnit3);
        req
            .setNonLoadedHandlingUnits(
                nonLoadedHUList);
        req.setSplitAuthorizedBy("U1234");
        req.setSplitAuthorizedDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));

        // Mocks.
        ShmHandlingUnitPK pk1 = new ShmHandlingUnitPK();
        pk1.setShpInstId(1234L);
        pk1.setSeqNbr(10L);
        ShmHandlingUnit hu1 = new ShmHandlingUnit();
        hu1.setId(pk1);
        hu1.setParentProNbrTxt(nonLoadedHandlingUnit.getParentProNbr());
        hu1.setChildProNbrTxt(nonLoadedHandlingUnit.getChildProNbr());
        hu1.setCurrentTrlrInstId(BigDecimal.valueOf(1000L));

        ShmHandlingUnitPK pk2 = new ShmHandlingUnitPK();
        pk2.setShpInstId(1234L);
        pk2.setSeqNbr(11L);
        ShmHandlingUnit hu2 = new ShmHandlingUnit();
        hu2.setId(pk2);
        hu2.setParentProNbrTxt(nonLoadedHandlingUnit2.getParentProNbr());
        hu2.setChildProNbrTxt(nonLoadedHandlingUnit2.getChildProNbr());
        hu2.setCurrentTrlrInstId(BigDecimal.valueOf(1000L));

        ShmHandlingUnitPK pk3 = new ShmHandlingUnitPK();
        pk3.setShpInstId(1234L);
        pk3.setSeqNbr(12L);
        ShmHandlingUnit hu3 = new ShmHandlingUnit();
        hu3.setId(pk3);
        hu3.setParentProNbrTxt(nonLoadedHandlingUnit3.getParentProNbr());
        hu3.setChildProNbrTxt(nonLoadedHandlingUnit3.getChildProNbr());
        hu3.setCurrentTrlrInstId(BigDecimal.valueOf(85396354L));
        when(shmHandlingUnitSubDAO.findByParentProNumberList(anyListOf(String.class), eq(entityManager)))
            .thenReturn(Arrays.asList(hu1, hu2, hu3));

        splitNonLoadedHandlingUnitsImpl.splitNonLoadedHandlingUnits(req, txnContext, entityManager);

        verify(shmHandlingUnitSubDAO, times(1)).findByParentProNumberList(anyListOf(String.class), eq(entityManager));

    }

    @Test
    public void testSplitNonLoadedHandlingUnits_OKTwoDiffProNbrs() throws ServiceException {

        SplitNonLoadedHandlingUnitsRqst req = new SplitNonLoadedHandlingUnitsRqst();
        NonLoadedHandlingUnit nonLoadedHandlingUnit = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit.setParentProNbr("06470492775");
        nonLoadedHandlingUnit.setChildProNbr("06481008041");
        nonLoadedHandlingUnit.setTrailerNbr(10L);
        nonLoadedHandlingUnit.setRequestingSicCd("UPO");
        NonLoadedHandlingUnit nonLoadedHandlingUnit2 = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit2.setParentProNbr("06470493184");
        nonLoadedHandlingUnit2.setChildProNbr("06481009990");
        nonLoadedHandlingUnit2.setTrailerNbr(20L);
        nonLoadedHandlingUnit.setRequestingSicCd("XDE");
        List<NonLoadedHandlingUnit> nonLoadedHUList = new ArrayList<NonLoadedHandlingUnit>();
        nonLoadedHUList.add(nonLoadedHandlingUnit);
        nonLoadedHUList.add(nonLoadedHandlingUnit2);
        req.setNonLoadedHandlingUnits(nonLoadedHUList);
        req.setSplitAuthorizedBy("U1234");
        req.setSplitAuthorizedDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));

        // Mocks.
        ShmHandlingUnitPK pk1 = new ShmHandlingUnitPK();
        pk1.setShpInstId(1234L);
        pk1.setSeqNbr(10L);
        ShmHandlingUnit hu1 = new ShmHandlingUnit();
        hu1.setId(pk1);
         hu1.setParentProNbrTxt(nonLoadedHandlingUnit.getParentProNbr());
        hu1.setChildProNbrTxt(nonLoadedHandlingUnit.getChildProNbr());
        hu1.setCurrentTrlrInstId(BigDecimal.valueOf(1000L));
        hu1.setCurrentSicCd("UCO");

        ShmHandlingUnitPK pk2 = new ShmHandlingUnitPK();
        pk2.setShpInstId(4321L);
        pk2.setSeqNbr(100L);
        ShmHandlingUnit hu2 = new ShmHandlingUnit();
        hu2.setId(pk2);
        hu2.setParentProNbrTxt(nonLoadedHandlingUnit2.getParentProNbr());
        hu2.setChildProNbrTxt(nonLoadedHandlingUnit2.getChildProNbr());
        hu2.setCurrentTrlrInstId(BigDecimal.valueOf(1000L));
        hu2.setCurrentSicCd("UCO");
        when(shmHandlingUnitSubDAO.findByParentProNumberList(anyListOf(String.class), eq(entityManager)))
            .thenReturn(Arrays.asList(hu1, hu2));

        ShmHandlingUnitMvmtPK mvmtPK1 = new ShmHandlingUnitMvmtPK();
        mvmtPK1.setShpInstId(pk1.getShpInstId());
        mvmtPK1.setSeqNbr(pk1.getSeqNbr());

        // only one movement pk found.
        when(shmHandlingUnitMvmtSubDAO
            .getMaxMvmtPKByShpInstIdAndSeqNbrPairs(anyListOf(ShmHandlingUnitPK.class), eq(entityManager)))
                .thenReturn(Arrays.asList(mvmtPK1));
        when(shmHandlingUnitMvmtSubDAO.save(any(ShmHandlingUnitMvmt.class), eq(entityManager))).thenReturn(null);
        when(shmHandlingUnitMvmtSubDAO
            .createDB2ShmHandlingUnitMvmt(any(ShmHandlingUnitMvmt.class), eq(db2EntityManager))).thenReturn(null);

        SplitNonLoadedHandlingUnitsResp splitNonLoadedHandlingUnits = splitNonLoadedHandlingUnitsImpl
            .splitNonLoadedHandlingUnits(req, txnContext, entityManager);


        assertNotNull(splitNonLoadedHandlingUnits);

        verify(shmHandlingUnitSubDAO, times(1)).findByParentProNumberList(anyListOf(String.class), eq(entityManager));
        verify(shmHandlingUnitMvmtSubDAO, times(2)).save(any(ShmHandlingUnitMvmt.class), eq(entityManager));
        verify(shmHandlingUnitMvmtSubDAO, times(2))
            .createDB2ShmHandlingUnitMvmt(any(ShmHandlingUnitMvmt.class), eq(db2EntityManager));
    }

    @Test
    public void testSplitNonLoadedHandlingUnits_OKTwoForSamePro() throws ServiceException {

        SplitNonLoadedHandlingUnitsRqst req = new SplitNonLoadedHandlingUnitsRqst();
        NonLoadedHandlingUnit nonLoadedHandlingUnit = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit.setParentProNbr("06470492775");
        nonLoadedHandlingUnit.setChildProNbr("06481008041");
        nonLoadedHandlingUnit.setTrailerNbr(10L);
        nonLoadedHandlingUnit.setRequestingSicCd("UPO");
        NonLoadedHandlingUnit nonLoadedHandlingUnit2 = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit2.setParentProNbr("06470492775");
        nonLoadedHandlingUnit2.setChildProNbr("06481009990");
        nonLoadedHandlingUnit2.setTrailerNbr(20L);
        nonLoadedHandlingUnit2.setRequestingSicCd("XDE");
        List<NonLoadedHandlingUnit> nonLoadedHUList = new ArrayList<NonLoadedHandlingUnit>();
        nonLoadedHUList.add(nonLoadedHandlingUnit);
        nonLoadedHUList.add(nonLoadedHandlingUnit2);
        req.setNonLoadedHandlingUnits(nonLoadedHUList);
        req.setSplitAuthorizedBy("U1234");
        req.setSplitAuthorizedDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));

        // Mocks.
        ShmHandlingUnitPK pk1 = new ShmHandlingUnitPK();
        pk1.setShpInstId(1234L);
        pk1.setSeqNbr(10L);
        ShmHandlingUnit hu1 = new ShmHandlingUnit();
        hu1.setId(pk1);
        hu1.setParentProNbrTxt(nonLoadedHandlingUnit.getParentProNbr());
        hu1.setChildProNbrTxt(nonLoadedHandlingUnit.getChildProNbr());
        hu1.setCurrentTrlrInstId(BigDecimal.valueOf(1000L));
        hu1.setCurrentSicCd("UCO");

        ShmHandlingUnitPK pk2 = new ShmHandlingUnitPK();
        pk2.setShpInstId(1234L);
        pk2.setSeqNbr(14L);
        ShmHandlingUnit hu2 = new ShmHandlingUnit();
        hu2.setId(pk2);
        hu2.setParentProNbrTxt(nonLoadedHandlingUnit2.getParentProNbr());
        hu2.setChildProNbrTxt(nonLoadedHandlingUnit2.getChildProNbr());
        hu2.setCurrentTrlrInstId(BigDecimal.valueOf(1000L));
        hu2.setCurrentSicCd("UPO");
        when(shmHandlingUnitSubDAO.findByParentProNumberList(anyListOf(String.class), eq(entityManager))).thenReturn(Arrays.asList(hu1, hu2));

        ShmHandlingUnitMvmtPK mvmtPK1 = new ShmHandlingUnitMvmtPK();
        mvmtPK1.setShpInstId(pk1.getShpInstId());
        mvmtPK1.setSeqNbr(pk1.getSeqNbr());
        ShmHandlingUnitMvmtPK mvmtPK2 = new ShmHandlingUnitMvmtPK();
        mvmtPK2.setShpInstId(pk2.getShpInstId());
        mvmtPK2.setSeqNbr(pk2.getSeqNbr());

        // only one movement pk found.
        when(shmHandlingUnitMvmtSubDAO.save(any(ShmHandlingUnitMvmt.class), eq(entityManager))).thenReturn(null);
        when(shmHandlingUnitMvmtSubDAO.createDB2ShmHandlingUnitMvmt(any(ShmHandlingUnitMvmt.class), eq(db2EntityManager))).thenReturn(null);

        SplitNonLoadedHandlingUnitsResp splitNonLoadedHandlingUnits = splitNonLoadedHandlingUnitsImpl
            .splitNonLoadedHandlingUnits(req, txnContext, entityManager);

        assertNotNull(splitNonLoadedHandlingUnits);

        verify(shmHandlingUnitSubDAO, times(1)).findByParentProNumberList(anyListOf(String.class), eq(entityManager));
        verify(shmHandlingUnitMvmtSubDAO, times(2)).save(any(ShmHandlingUnitMvmt.class), eq(entityManager));
        verify(shmHandlingUnitMvmtSubDAO, times(2)).createDB2ShmHandlingUnitMvmt(any(ShmHandlingUnitMvmt.class), eq(db2EntityManager));
    }

    @Test
    public void testSplitNonLoadedHandlingUnits_FailAllForSameSIC() throws ServiceException {

        SplitNonLoadedHandlingUnitsRqst req = new SplitNonLoadedHandlingUnitsRqst();
        NonLoadedHandlingUnit nonLoadedHandlingUnit = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit.setParentProNbr("06470492775");
        nonLoadedHandlingUnit.setChildProNbr("06481008041");
        nonLoadedHandlingUnit.setTrailerNbr(10L);
        nonLoadedHandlingUnit.setRequestingSicCd("UPO");
        List<NonLoadedHandlingUnit> nonLoadedHUList = new ArrayList<NonLoadedHandlingUnit>();
        nonLoadedHUList.add(nonLoadedHandlingUnit);
        req.setNonLoadedHandlingUnits(nonLoadedHUList);
        req.setSplitAuthorizedBy("U1234");
        req.setSplitAuthorizedDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));

        // Mocks.
        ShmHandlingUnitPK pk1 = new ShmHandlingUnitPK();
        pk1.setShpInstId(1234L);
        pk1.setSeqNbr(10L);
        ShmHandlingUnit hu1 = new ShmHandlingUnit();
        hu1.setId(pk1);
        hu1.setParentProNbrTxt(nonLoadedHandlingUnit.getParentProNbr());
        hu1.setChildProNbrTxt(nonLoadedHandlingUnit.getChildProNbr());
        hu1.setCurrentTrlrInstId(BigDecimal.valueOf(0L));
        hu1.setCurrentSicCd("UCO");

        ShmHandlingUnitPK pk2 = new ShmHandlingUnitPK();
        pk2.setShpInstId(1234L);
        pk2.setSeqNbr(14L);
        ShmHandlingUnit hu2 = new ShmHandlingUnit();
        hu2.setId(pk2);
        hu2.setParentProNbrTxt(nonLoadedHandlingUnit.getParentProNbr());
        hu2.setChildProNbrTxt("06481009990");
        hu2.setCurrentTrlrInstId(BigDecimal.valueOf(0L));
        hu2.setCurrentSicCd("UCO");
        when(shmHandlingUnitSubDAO.findByParentProNumberList(anyListOf(String.class), eq(entityManager)))
            .thenReturn(Arrays.asList(hu1, hu2));

        ShmHandlingUnitMvmtPK mvmtPK1 = new ShmHandlingUnitMvmtPK();
        mvmtPK1.setShpInstId(pk1.getShpInstId());
        mvmtPK1.setSeqNbr(pk1.getSeqNbr());
        ShmHandlingUnitMvmtPK mvmtPK2 = new ShmHandlingUnitMvmtPK();
        mvmtPK2.setShpInstId(pk2.getShpInstId());
        mvmtPK2.setSeqNbr(pk2.getSeqNbr());

        // only one movement pk found.
        when(shmHandlingUnitMvmtSubDAO.save(any(ShmHandlingUnitMvmt.class), eq(entityManager))).thenReturn(null);
        when(shmHandlingUnitMvmtSubDAO
            .createDB2ShmHandlingUnitMvmt(any(ShmHandlingUnitMvmt.class), eq(db2EntityManager))).thenReturn(null);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            splitNonLoadedHandlingUnitsImpl.splitNonLoadedHandlingUnits(req, txnContext, entityManager);
        });

        assertEquals(1, exception.getFault().getMoreInfo().size());
        assertEquals("All Child PRO are at the same Sic or in dock location for Parent PRO 647-492775.",
            exception.getFault().getMoreInfo().get(0).getMessage());

    }

    @Test
    public void testSplitNonLoadedHandlingUnits_OKFreezableShipments() throws ServiceException {

        SplitNonLoadedHandlingUnitsRqst req = new SplitNonLoadedHandlingUnitsRqst();
        NonLoadedHandlingUnit nonLoadedHandlingUnit = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit.setParentProNbr("06470492775");
        nonLoadedHandlingUnit.setChildProNbr("06481008041");
        nonLoadedHandlingUnit.setTrailerNbr(10L);
        nonLoadedHandlingUnit.setRequestingSicCd("UPO");
        List<NonLoadedHandlingUnit> nonLoadedHUList = new ArrayList<NonLoadedHandlingUnit>();
        nonLoadedHUList.add(nonLoadedHandlingUnit);
        req.setNonLoadedHandlingUnits(nonLoadedHUList);
        req.setSplitAuthorizedBy("U1234");
        req.setSplitAuthorizedDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));

        // Mocks.
        ShmHandlingUnitPK pk1 = new ShmHandlingUnitPK();
        pk1.setShpInstId(1234L);
        pk1.setSeqNbr(10L);
        ShmHandlingUnit hu1 = new ShmHandlingUnit();
        hu1.setId(pk1);
        hu1.setParentProNbrTxt(nonLoadedHandlingUnit.getParentProNbr());
        hu1.setChildProNbrTxt(nonLoadedHandlingUnit.getChildProNbr());
        hu1.setCurrentTrlrInstId(BigDecimal.valueOf(1000L));
        hu1.setCurrentSicCd("UCO");

        ShmHandlingUnitMvmtPK mvmtPK1 = new ShmHandlingUnitMvmtPK();
        mvmtPK1.setShpInstId(pk1.getShpInstId());
        mvmtPK1.setSeqNbr(pk1.getSeqNbr());

        ShmShipment shm = new ShmShipment();
        shm.setProNbrTxt("06470492775");
        shm.setFrzbleInd("Y");
        shm.setOrigTrmnlSicCd("USE");
        shm.setDestTrmnlSicCd("XCW");

        when(shmShipmentSubDAO.listShipmentsByProNbrs
                 (any(), any(), eq(entityManager)))
            .thenReturn(Lists.newArrayList(shm));

        when(shmHandlingUnitSubDAO.findByParentProNumberList(any(), eq(entityManager))).thenReturn(Lists.newArrayList(hu1));

        GetRefSicAddressResp getRefSicAddressResp = new GetRefSicAddressResp();
        LocationAddress addr = new LocationAddress();
        getRefSicAddressResp.setLocAddress(addr);
        when(externalRestClient.getRefSicAddress(shm.getOrigTrmnlSicCd(),
            txnContext)).thenReturn(getRefSicAddressResp);

        GetRefSicAddressResp getRefSicAddressResp2 = new GetRefSicAddressResp();
        LocationAddress addr2 = new LocationAddress();
        getRefSicAddressResp2.setLocAddress(addr2);
        when(externalRestClient.getRefSicAddress(shm.getDestTrmnlSicCd(),
            txnContext)).thenReturn(getRefSicAddressResp2);

        // only one movement pk found.
        when(shmHandlingUnitMvmtSubDAO.save(any(ShmHandlingUnitMvmt.class), eq(entityManager))).thenReturn(null);
        when(shmHandlingUnitMvmtSubDAO
            .createDB2ShmHandlingUnitMvmt(any(ShmHandlingUnitMvmt.class), eq(db2EntityManager))).thenReturn(null);

        when(shmEventDelegate.createEvent(anyLong(), any(), any(), any(), any(), any(), any(), any(), eq(entityManager), any())).thenReturn(1L);

        SplitNonLoadedHandlingUnitsResp splitNonLoadedHandlingUnits = splitNonLoadedHandlingUnitsImpl
            .splitNonLoadedHandlingUnits(req, txnContext, entityManager);

        assertNotNull(splitNonLoadedHandlingUnits);

        verify(shmHandlingUnitSubDAO, times(1)).findByParentProNumberList(anyListOf(String.class), eq(entityManager));
        verify(shmHandlingUnitMvmtSubDAO, times(1)).save(any(ShmHandlingUnitMvmt.class), eq(entityManager));
        verify(shmHandlingUnitMvmtSubDAO, times(1))
            .createDB2ShmHandlingUnitMvmt(any(ShmHandlingUnitMvmt.class), eq(db2EntityManager));
    }

    @Test
    public void testSplitNonLoadedHandlingUnits_FailHazMatShipments() throws ServiceException {

        SplitNonLoadedHandlingUnitsRqst req = new SplitNonLoadedHandlingUnitsRqst();
        NonLoadedHandlingUnit nonLoadedHandlingUnit = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit.setParentProNbr("06470492775");
        nonLoadedHandlingUnit.setChildProNbr("06481008041");
        nonLoadedHandlingUnit.setTrailerNbr(10L);
        nonLoadedHandlingUnit.setRequestingSicCd("UPO");
        List<NonLoadedHandlingUnit> nonLoadedHUList = new ArrayList<NonLoadedHandlingUnit>();
        nonLoadedHUList.add(nonLoadedHandlingUnit);
        req.setNonLoadedHandlingUnits(nonLoadedHUList);
        req.setSplitAuthorizedBy("U1234");
        req.setSplitAuthorizedDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));

        // Mocks.
        ShmHandlingUnitPK pk1 = new ShmHandlingUnitPK();
        pk1.setShpInstId(1234L);
        pk1.setSeqNbr(10L);
        ShmHandlingUnit hu1 = new ShmHandlingUnit();
        hu1.setId(pk1);
        hu1.setParentProNbrTxt(nonLoadedHandlingUnit.getParentProNbr());
        hu1.setChildProNbrTxt(nonLoadedHandlingUnit.getChildProNbr());
        hu1.setCurrentTrlrInstId(BigDecimal.valueOf(1000L));
        hu1.setCurrentSicCd("UCO");

        ShmHandlingUnitMvmtPK mvmtPK1 = new ShmHandlingUnitMvmtPK();
        mvmtPK1.setShpInstId(pk1.getShpInstId());
        mvmtPK1.setSeqNbr(pk1.getSeqNbr());

        ShmShipment shm = new ShmShipment();
        shm.setProNbrTxt("06470492775");
        shm.setHazmatInd("Y");
        shm.setOrigTrmnlSicCd("USE");
        shm.setDestTrmnlSicCd("XCW");

        when(shmShipmentSubDAO.listShipmentsByProNbrs
                 (any(), any(), eq(entityManager)))
            .thenReturn(Lists.newArrayList(shm));

        when(shmHandlingUnitSubDAO.findByParentProNumberList(any(), eq(entityManager))).thenReturn(Lists.newArrayList(hu1));

        GetRefSicAddressResp getRefSicAddressResp = new GetRefSicAddressResp();
        LocationAddress addr = new LocationAddress();
        getRefSicAddressResp.setLocAddress(addr);
        when(externalRestClient.getRefSicAddress(shm.getOrigTrmnlSicCd(),
            txnContext)).thenReturn(getRefSicAddressResp);

        GetRefSicAddressResp getRefSicAddressResp2 = new GetRefSicAddressResp();
        LocationAddress addr2 = new LocationAddress();
        getRefSicAddressResp2.setLocAddress(addr2);
        when(externalRestClient.getRefSicAddress(shm.getDestTrmnlSicCd(),
            txnContext)).thenReturn(getRefSicAddressResp2);

        // only one movement pk found.
        when(shmHandlingUnitMvmtSubDAO.save(any(ShmHandlingUnitMvmt.class), eq(entityManager))).thenReturn(null);
        when(shmHandlingUnitMvmtSubDAO
            .createDB2ShmHandlingUnitMvmt(any(ShmHandlingUnitMvmt.class), eq(db2EntityManager))).thenReturn(null);

        when(shmEventDelegate.createEvent(anyLong(), any(), any(), any(), any(), any(), any(), any(), eq(entityManager), any())).thenReturn(1L);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            splitNonLoadedHandlingUnitsImpl.splitNonLoadedHandlingUnits(req, txnContext, entityManager);
        });

        assertEquals(1, exception.getFault().getMoreInfo().size());
        assertEquals("Shipment Validation", exception.getFault().getMoreInfo().get(0).getLocation());
        assertEquals("Parent PRO 647-492775 is a HazMat shipment. Split is not allowed.",
            exception.getFault().getMoreInfo().get(0).getMessage());
    }

    @Test
    public void testSplitNonLoadedHandlingUnits_FailGuaranteedhipments() throws ServiceException {

        SplitNonLoadedHandlingUnitsRqst req = new SplitNonLoadedHandlingUnitsRqst();
        NonLoadedHandlingUnit nonLoadedHandlingUnit = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit.setParentProNbr("06470492775");
        nonLoadedHandlingUnit.setChildProNbr("06481008041");
        nonLoadedHandlingUnit.setTrailerNbr(10L);
        nonLoadedHandlingUnit.setRequestingSicCd("UPO");
        List<NonLoadedHandlingUnit> nonLoadedHUList = new ArrayList<NonLoadedHandlingUnit>();
        nonLoadedHUList.add(nonLoadedHandlingUnit);
        req.setNonLoadedHandlingUnits(nonLoadedHUList);
        req.setSplitAuthorizedBy("U1234");
        req.setSplitAuthorizedDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));

        // Mocks.
        ShmHandlingUnitPK pk1 = new ShmHandlingUnitPK();
        pk1.setShpInstId(1234L);
        pk1.setSeqNbr(10L);
        ShmHandlingUnit hu1 = new ShmHandlingUnit();
        hu1.setId(pk1);
        hu1.setParentProNbrTxt(nonLoadedHandlingUnit.getParentProNbr());
        hu1.setChildProNbrTxt(nonLoadedHandlingUnit.getChildProNbr());
        hu1.setCurrentTrlrInstId(BigDecimal.valueOf(1000L));
        hu1.setCurrentSicCd("UCO");

        ShmHandlingUnitMvmtPK mvmtPK1 = new ShmHandlingUnitMvmtPK();
        mvmtPK1.setShpInstId(pk1.getShpInstId());
        mvmtPK1.setSeqNbr(pk1.getSeqNbr());

        ShmShipment shm = new ShmShipment();
        shm.setProNbrTxt("06470492775");
        shm.setGarntdInd("Y");
        shm.setOrigTrmnlSicCd("USE");
        shm.setDestTrmnlSicCd("XCW");

        when(shmShipmentSubDAO.listShipmentsByProNbrs
                 (any(), any(), eq(entityManager)))
            .thenReturn(Lists.newArrayList(shm));

        when(shmHandlingUnitSubDAO.findByParentProNumberList(any(), eq(entityManager))).thenReturn(Lists.newArrayList(hu1));

        GetRefSicAddressResp getRefSicAddressResp = new GetRefSicAddressResp();
        LocationAddress addr = new LocationAddress();
        getRefSicAddressResp.setLocAddress(addr);
        when(externalRestClient.getRefSicAddress(shm.getOrigTrmnlSicCd(),
            txnContext)).thenReturn(getRefSicAddressResp);

        GetRefSicAddressResp getRefSicAddressResp2 = new GetRefSicAddressResp();
        LocationAddress addr2 = new LocationAddress();
        getRefSicAddressResp2.setLocAddress(addr2);
        when(externalRestClient.getRefSicAddress(shm.getDestTrmnlSicCd(),
            txnContext)).thenReturn(getRefSicAddressResp2);

        // only one movement pk found.
        when(shmHandlingUnitMvmtSubDAO.save(any(ShmHandlingUnitMvmt.class), eq(entityManager))).thenReturn(null);
        when(shmHandlingUnitMvmtSubDAO
            .createDB2ShmHandlingUnitMvmt(any(ShmHandlingUnitMvmt.class), eq(db2EntityManager))).thenReturn(null);

        when(shmEventDelegate.createEvent(anyLong(), any(), any(), any(), any(), any(), any(), any(), eq(entityManager), any())).thenReturn(1L);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            splitNonLoadedHandlingUnitsImpl.splitNonLoadedHandlingUnits(req, txnContext, entityManager);
        });

        assertEquals(1, exception.getFault().getMoreInfo().size());
        assertEquals("Shipment Validation", exception.getFault().getMoreInfo().get(0).getLocation());
        assertEquals("Parent PRO 647-492775 is a Guaranteed shipment. Split is not allowed.",
            exception.getFault().getMoreInfo().get(0).getMessage());
    }

    @Test
    public void testSplitNonLoadedHandlingUnits_FailRRSShipments() throws ServiceException {

        SplitNonLoadedHandlingUnitsRqst req = new SplitNonLoadedHandlingUnitsRqst();
        NonLoadedHandlingUnit nonLoadedHandlingUnit = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit.setParentProNbr("06470492775");
        nonLoadedHandlingUnit.setChildProNbr("06481008041");
        nonLoadedHandlingUnit.setTrailerNbr(10L);
        nonLoadedHandlingUnit.setRequestingSicCd("UPO");
        List<NonLoadedHandlingUnit> nonLoadedHUList = new ArrayList<NonLoadedHandlingUnit>();
        nonLoadedHUList.add(nonLoadedHandlingUnit);
        req.setNonLoadedHandlingUnits(nonLoadedHUList);
        req.setSplitAuthorizedBy("U1234");
        req.setSplitAuthorizedDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));

        // Mocks.
        ShmHandlingUnitPK pk1 = new ShmHandlingUnitPK();
        pk1.setShpInstId(1234L);
        pk1.setSeqNbr(10L);
        ShmHandlingUnit hu1 = new ShmHandlingUnit();
        hu1.setId(pk1);
        hu1.setParentProNbrTxt(nonLoadedHandlingUnit.getParentProNbr());
        hu1.setChildProNbrTxt(nonLoadedHandlingUnit.getChildProNbr());
        hu1.setCurrentTrlrInstId(BigDecimal.valueOf(1000L));
        hu1.setCurrentSicCd("UCO");

        ShmHandlingUnitMvmtPK mvmtPK1 = new ShmHandlingUnitMvmtPK();
        mvmtPK1.setShpInstId(pk1.getShpInstId());
        mvmtPK1.setSeqNbr(pk1.getSeqNbr());

        ShmShipment shm = new ShmShipment();
        shm.setProNbrTxt("06470492775");
        shm.setSvcTypCd("3");
        shm.setOrigTrmnlSicCd("USE");
        shm.setDestTrmnlSicCd("XCW");

        when(shmShipmentSubDAO.listShipmentsByProNbrs
                 (any(), any(), eq(entityManager)))
            .thenReturn(Lists.newArrayList(shm));

        when(shmHandlingUnitSubDAO.findByParentProNumberList(any(), eq(entityManager))).thenReturn(Lists.newArrayList(hu1));

        GetRefSicAddressResp getRefSicAddressResp = new GetRefSicAddressResp();
        LocationAddress addr = new LocationAddress();
        getRefSicAddressResp.setLocAddress(addr);
        when(externalRestClient.getRefSicAddress(shm.getOrigTrmnlSicCd(),
            txnContext)).thenReturn(getRefSicAddressResp);

        GetRefSicAddressResp getRefSicAddressResp2 = new GetRefSicAddressResp();
        LocationAddress addr2 = new LocationAddress();
        getRefSicAddressResp2.setLocAddress(addr2);
        when(externalRestClient.getRefSicAddress(shm.getDestTrmnlSicCd(),
            txnContext)).thenReturn(getRefSicAddressResp2);

        // only one movement pk found.
        when(shmHandlingUnitMvmtSubDAO.save(any(ShmHandlingUnitMvmt.class), eq(entityManager))).thenReturn(null);
        when(shmHandlingUnitMvmtSubDAO
            .createDB2ShmHandlingUnitMvmt(any(ShmHandlingUnitMvmt.class), eq(db2EntityManager))).thenReturn(null);

        when(shmEventDelegate.createEvent(anyLong(), any(), any(), any(), any(), any(), any(), any(), eq(entityManager), any())).thenReturn(1L);


        ValidationException exception = assertThrows(ValidationException.class, () -> {
            splitNonLoadedHandlingUnitsImpl.splitNonLoadedHandlingUnits(req, txnContext, entityManager);
        });

        assertEquals(1, exception.getFault().getMoreInfo().size());
        assertEquals("Shipment Validation", exception.getFault().getMoreInfo().get(0).getLocation());
        assertEquals("Parent PRO 647-492775 is a Rapid Remote Service shipment. Split is not allowed.",
            exception.getFault().getMoreInfo().get(0).getMessage());
    }

    @Test
    public void testSplitNonLoadedHandlingUnits_FailG12Shipments() throws ServiceException {

        SplitNonLoadedHandlingUnitsRqst req = new SplitNonLoadedHandlingUnitsRqst();
        NonLoadedHandlingUnit nonLoadedHandlingUnit = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit.setParentProNbr("06470492775");
        nonLoadedHandlingUnit.setChildProNbr("06481008041");
        nonLoadedHandlingUnit.setTrailerNbr(10L);
        nonLoadedHandlingUnit.setRequestingSicCd("UPO");
        List<NonLoadedHandlingUnit> nonLoadedHUList = new ArrayList<NonLoadedHandlingUnit>();
        nonLoadedHUList.add(nonLoadedHandlingUnit);
        req.setNonLoadedHandlingUnits(nonLoadedHUList);
        req.setSplitAuthorizedBy("U1234");
        req.setSplitAuthorizedDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));

        // Mocks.
        ShmHandlingUnitPK pk1 = new ShmHandlingUnitPK();
        pk1.setShpInstId(1234L);
        pk1.setSeqNbr(10L);
        ShmHandlingUnit hu1 = new ShmHandlingUnit();
        hu1.setId(pk1);
        hu1.setParentProNbrTxt(nonLoadedHandlingUnit.getParentProNbr());
        hu1.setChildProNbrTxt(nonLoadedHandlingUnit.getChildProNbr());
        hu1.setCurrentTrlrInstId(BigDecimal.valueOf(1000L));
        hu1.setCurrentSicCd("UCO");

        ShmHandlingUnitMvmtPK mvmtPK1 = new ShmHandlingUnitMvmtPK();
        mvmtPK1.setShpInstId(pk1.getShpInstId());
        mvmtPK1.setSeqNbr(pk1.getSeqNbr());

        ShmShipment shm = new ShmShipment();
        shm.setProNbrTxt("06470492775");
        shm.setSvcTypCd("4");
        shm.setOrigTrmnlSicCd("USE");
        shm.setDestTrmnlSicCd("XCW");

        when(shmShipmentSubDAO.listShipmentsByProNbrs
                 (any(), any(), eq(entityManager)))
            .thenReturn(Lists.newArrayList(shm));

        when(shmHandlingUnitSubDAO.findByParentProNumberList(any(), eq(entityManager))).thenReturn(Lists.newArrayList(hu1));

        GetRefSicAddressResp getRefSicAddressResp = new GetRefSicAddressResp();
        LocationAddress addr = new LocationAddress();
        getRefSicAddressResp.setLocAddress(addr);
        when(externalRestClient.getRefSicAddress(shm.getOrigTrmnlSicCd(),
            txnContext)).thenReturn(getRefSicAddressResp);

        GetRefSicAddressResp getRefSicAddressResp2 = new GetRefSicAddressResp();
        LocationAddress addr2 = new LocationAddress();
        getRefSicAddressResp2.setLocAddress(addr2);
        when(externalRestClient.getRefSicAddress(shm.getDestTrmnlSicCd(),
            txnContext)).thenReturn(getRefSicAddressResp2);

        // only one movement pk found.
        when(shmHandlingUnitMvmtSubDAO.save(any(ShmHandlingUnitMvmt.class), eq(entityManager))).thenReturn(null);
        when(shmHandlingUnitMvmtSubDAO
            .createDB2ShmHandlingUnitMvmt(any(ShmHandlingUnitMvmt.class), eq(db2EntityManager))).thenReturn(null);

        when(shmEventDelegate.createEvent(anyLong(), any(), any(), any(), any(), any(), any(), any(), eq(entityManager), any())).thenReturn(1L);


        ValidationException exception = assertThrows(ValidationException.class, () -> {
            splitNonLoadedHandlingUnitsImpl.splitNonLoadedHandlingUnits(req, txnContext, entityManager);
        });

        assertEquals(1, exception.getFault().getMoreInfo().size());
        assertEquals("Shipment Validation", exception.getFault().getMoreInfo().get(0).getLocation());
        assertEquals("Parent PRO 647-492775 is a Guaranteed By Noon shipment. Split is not allowed.",
            exception.getFault().getMoreInfo().get(0).getMessage());
    }

    @Test
    public void testSplitNonLoadedHandlingUnits_FailCrossBorderShipments() throws ServiceException {

        SplitNonLoadedHandlingUnitsRqst req = new SplitNonLoadedHandlingUnitsRqst();
        NonLoadedHandlingUnit nonLoadedHandlingUnit = new NonLoadedHandlingUnit();
        nonLoadedHandlingUnit.setParentProNbr("06470492775");
        nonLoadedHandlingUnit.setChildProNbr("06481008041");
        nonLoadedHandlingUnit.setTrailerNbr(10L);
        nonLoadedHandlingUnit.setRequestingSicCd("UPO");
        List<NonLoadedHandlingUnit> nonLoadedHUList = new ArrayList<NonLoadedHandlingUnit>();
        nonLoadedHUList.add(nonLoadedHandlingUnit);
        req.setNonLoadedHandlingUnits(nonLoadedHUList);
        req.setSplitAuthorizedBy("U1234");
        req.setSplitAuthorizedDateTime(BasicTransformer.toXMLGregorianCalendar(new Date()));

        // Mocks.
        ShmHandlingUnitPK pk1 = new ShmHandlingUnitPK();
        pk1.setShpInstId(1234L);
        pk1.setSeqNbr(10L);
        ShmHandlingUnit hu1 = new ShmHandlingUnit();
        hu1.setId(pk1);
        hu1.setParentProNbrTxt(nonLoadedHandlingUnit.getParentProNbr());
        hu1.setChildProNbrTxt(nonLoadedHandlingUnit.getChildProNbr());
        hu1.setCurrentTrlrInstId(BigDecimal.valueOf(1000L));
        hu1.setCurrentSicCd("UCO");

        ShmHandlingUnitMvmtPK mvmtPK1 = new ShmHandlingUnitMvmtPK();
        mvmtPK1.setShpInstId(pk1.getShpInstId());
        mvmtPK1.setSeqNbr(pk1.getSeqNbr());

        ShmShipment shm = new ShmShipment();
        shm.setProNbrTxt("06470492775");
        shm.setSvcTypCd("1");
        shm.setOrigTrmnlSicCd("USE");
        shm.setDestTrmnlSicCd("XLO");

        when(shmShipmentSubDAO.listShipmentsByProNbrs
                 (any(), any(), eq(entityManager)))
            .thenReturn(Lists.newArrayList(shm));

        when(shmHandlingUnitSubDAO.findByParentProNumberList(any(), eq(entityManager))).thenReturn(Lists.newArrayList(hu1));

        GetRefSicAddressResp getRefSicAddressResp = new GetRefSicAddressResp();
        LocationAddress addr = new LocationAddress();
        addr.setCountryCd("US");
        getRefSicAddressResp.setLocAddress(addr);
        when(externalRestClient.getRefSicAddress(shm.getOrigTrmnlSicCd(),
            txnContext)).thenReturn(getRefSicAddressResp);

        GetRefSicAddressResp getRefSicAddressResp2 = new GetRefSicAddressResp();
        LocationAddress addr2 = new LocationAddress();
        getRefSicAddressResp2.setLocAddress(addr2);
        addr2.setCountryCd("CA");
        when(externalRestClient.getRefSicAddress(shm.getDestTrmnlSicCd(),
            txnContext)).thenReturn(getRefSicAddressResp2);

        // only one movement pk found.
        when(shmHandlingUnitMvmtSubDAO.save(any(ShmHandlingUnitMvmt.class), eq(entityManager))).thenReturn(null);
        when(shmHandlingUnitMvmtSubDAO
            .createDB2ShmHandlingUnitMvmt(any(ShmHandlingUnitMvmt.class), eq(db2EntityManager))).thenReturn(null);

        when(shmEventDelegate.createEvent(anyLong(), any(), any(), any(), any(), any(), any(), any(), eq(entityManager), any())).thenReturn(1L);


        ValidationException exception = assertThrows(ValidationException.class, () -> {
            splitNonLoadedHandlingUnitsImpl.splitNonLoadedHandlingUnits(req, txnContext, entityManager);
        });

        assertEquals(1, exception.getFault().getMoreInfo().size());
        assertEquals("Shipment Validation", exception.getFault().getMoreInfo().get(0).getLocation());
        assertEquals("Parent PRO 647-492775 is a cross-border shipment. Split is not allowed.",
            exception.getFault().getMoreInfo().get(0).getMessage());
    }

    @Test
    public void testGetAndIncreaseMvmtSeqNbr_mvmtFound() {
        ShmHandlingUnit handlingUnit = new ShmHandlingUnit();
        ShmHandlingUnitMvmt mvmt = new ShmHandlingUnitMvmt();
        ShmHandlingUnitMvmtPK id = new ShmHandlingUnitMvmtPK();
        id.setMvmtSeqNbr(10L);
        mvmt.setId(id);
        handlingUnit.setShmHandlingUnitMvmts(Arrays.asList(mvmt));
        Map<ShmHandlingUnit, Long> nextMvmtSeqNbrMap = new HashMap<ShmHandlingUnit, Long>();
        long mvmtSeqNbr = splitNonLoadedHandlingUnitsImpl.getAndIncreaseMvmtSeqNbr(handlingUnit, nextMvmtSeqNbrMap);
        assertEquals(11, mvmtSeqNbr);
    }

    @Test
    public void testGetAndIncreaseMvmtSeqNbr_mvmtNotFound() {
        ShmHandlingUnit handlingUnit = new ShmHandlingUnit();
        Map<ShmHandlingUnit, Long> nextMvmtSeqNbrMap = new HashMap<ShmHandlingUnit, Long>();
        long mvmtSeqNbr = splitNonLoadedHandlingUnitsImpl.getAndIncreaseMvmtSeqNbr(handlingUnit, nextMvmtSeqNbrMap);
        assertEquals(1, mvmtSeqNbr);
    }

    @Test
    public void testGetAndIncreaseMvmtSeqNbr_mvmtFoundCallTwice() {
        ShmHandlingUnit handlingUnit = new ShmHandlingUnit();
        ShmHandlingUnitMvmt mvmt = new ShmHandlingUnitMvmt();
        ShmHandlingUnitMvmtPK id = new ShmHandlingUnitMvmtPK();
        id.setMvmtSeqNbr(10L);
        mvmt.setId(id);
        handlingUnit.setShmHandlingUnitMvmts(Arrays.asList(mvmt));
        Map<ShmHandlingUnit, Long> nextMvmtSeqNbrMap = new HashMap<ShmHandlingUnit, Long>();
        long mvmtSeqNbr = splitNonLoadedHandlingUnitsImpl.getAndIncreaseMvmtSeqNbr(handlingUnit, nextMvmtSeqNbrMap);
        long mvmtSeqNbr2 = splitNonLoadedHandlingUnitsImpl.getAndIncreaseMvmtSeqNbr(handlingUnit, nextMvmtSeqNbrMap);
        assertEquals(11, mvmtSeqNbr);
        assertEquals(12, mvmtSeqNbr2);
    }

}
