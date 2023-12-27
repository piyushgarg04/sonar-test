package com.xpo.ltl.shipment.service.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.freightflow.v2.PostalTransitTime;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentAcquiredTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.CalculateServiceStandardForShipmentsResp;
import com.xpo.ltl.api.shipment.v2.CalculateServiceStandardForShipmentsRqst;
import com.xpo.ltl.api.shipment.v2.ProAndDestination;
import com.xpo.ltl.api.shipment.v2.ShipmentAcquiredTypeCd;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.CalculateTransitTimeResult;
import com.xpo.ltl.shipment.service.client.CalculateTransitTimeTask;
import com.xpo.ltl.shipment.service.client.CalculateTransitTimesResult;
import com.xpo.ltl.shipment.service.client.CalculateTransitTimesTask;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShmShipmentEagerLoadPlan;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;

public class CalculateServiceStandardForShipmentsImplTest {

    @Mock
    private TransactionContext txnContext;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ShmShipmentSubDAO shipmentDAO;

    @Mock
    private ExternalRestClient restClient;

    @InjectMocks
    private CalculateServiceStandardForShipmentsImpl calculateServiceStandardForShipmentsImpl;

    @Before
    public void setUp() {
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
            calculateServiceStandardForShipmentsImpl.calculateServiceStandardForShipments(null, null, entityManager);
            Assert.fail("Expected an exception.");
        } catch (Exception e) {
            Assert.assertEquals("The TransactionContext is required.", e.getMessage());
        }
    }

    @Test
    public void testEntityManagerRequired() {
        try {
            calculateServiceStandardForShipmentsImpl.calculateServiceStandardForShipments(null, txnContext, null);
            Assert.fail("Expected an exception.");
        } catch (Exception e) {
            Assert.assertEquals("The EntityManager is required.", e.getMessage());
        }
    }

    @Test
    public void testCalcSvcStdForShm_requestRequired() {
        try {
            calculateServiceStandardForShipmentsImpl.calculateServiceStandardForShipments(null, txnContext, entityManager);
            Assert.fail("Expected an exception.");
        } catch (Exception e) {
            Assert.assertEquals("The CalculateServiceStandardForShipmentsRqst is required.", e.getMessage());
        }
    }

    @Test(expected = ValidationException.class)
    public void testCalcSvcStdForShm_emptyRequest() throws ServiceException {
        CalculateServiceStandardForShipmentsRqst rqst = new CalculateServiceStandardForShipmentsRqst();
        calculateServiceStandardForShipmentsImpl.calculateServiceStandardForShipments(rqst, txnContext, entityManager);
    }

    @Test(expected = ValidationException.class)
    public void testCalcSvcStdForShm_proNbrListGT40() throws ServiceException {
        CalculateServiceStandardForShipmentsRqst rqst = new CalculateServiceStandardForShipmentsRqst();

        List<ProAndDestination> proAndDestinationList = new ArrayList<ProAndDestination>(41);

        for (int i = 0; i < 41; i++) {
            ProAndDestination pad = new ProAndDestination();
            pad.setProNbr("0111011111");
            proAndDestinationList.add(pad);
        }
        rqst.setProAndDestinations(proAndDestinationList);
        assertEquals(41, rqst.getProAndDestinations().size());
        calculateServiceStandardForShipmentsImpl.calculateServiceStandardForShipments(rqst, txnContext, entityManager);
    }

    @Test
    public void testListProStatus_NoShipmentsFound() throws ServiceException {
        when(shipmentDAO.listShipmentsByProNbrs(anyListOf(String.class), any(ShmShipmentEagerLoadPlan.class), eq(entityManager)))
            .thenReturn(Lists.newArrayList());

        CalculateServiceStandardForShipmentsRqst rqst = new CalculateServiceStandardForShipmentsRqst();
        ProAndDestination pad1 = new ProAndDestination();
        pad1.setProNbr("01110123456");
        ProAndDestination pad2 = new ProAndDestination();
        pad2.setProNbr("01230123453");
        rqst.setProAndDestinations(Arrays.asList(pad1, pad2));

        CalculateServiceStandardForShipmentsResp resp = calculateServiceStandardForShipmentsImpl
            .calculateServiceStandardForShipments(rqst, txnContext, entityManager);

        assertNotNull(resp);
        assertNotNull(resp.getShipmentServiceStandards());
        assertEquals(0, resp.getShipmentServiceStandards().size());
        assertNotNull(resp.getWarnings());
        assertEquals(2, resp.getWarnings().size());
        assertEquals(NotFoundErrorMessage.SHIPMENT_NF.errorCode(), resp.getWarnings().get(0).getErrorCd());
        assertEquals(NotFoundErrorMessage.SHIPMENT_NF.errorCode(), resp.getWarnings().get(1).getErrorCd());
        assertEquals("ProNbr", resp.getWarnings().get(0).getFieldName());
        assertEquals("ProNbr", resp.getWarnings().get(1).getFieldName());
        assertTrue(resp.getWarnings().stream().anyMatch(dve -> dve.getFieldValue().equals("01110123456")));
        assertTrue(resp.getWarnings().stream().anyMatch(dve -> dve.getFieldValue().equals("01230123453")));
        assertTrue(resp.getWarnings().stream().anyMatch(dve -> dve.getMessage().equals("Shipment not found. ProNbr 01110123456")));
        assertTrue(resp.getWarnings().stream().anyMatch(dve -> dve.getMessage().equals("Shipment not found. ProNbr 01230123453")));
    }

    @Ignore
    public void testListProStatus_NoPickupShipments() throws ServiceException {
        List<ShmShipment> mockedShmListDB = mockedShmListDB();
        mockedShmListDB.forEach(shm -> shm.setShpmtAcqrTypCd(ShipmentAcquiredTypeCdTransformer.toCode(ShipmentAcquiredTypeCd.HOOK_LD)));

        when(shipmentDAO.listShipmentsByProNbrs(anyListOf(String.class), any(ShmShipmentEagerLoadPlan.class), eq(entityManager)))
            .thenReturn(mockedShmListDB);

        CalculateServiceStandardForShipmentsRqst rqst = new CalculateServiceStandardForShipmentsRqst();
        ProAndDestination pad1 = new ProAndDestination();
        pad1.setProNbr("01110123456");
        ProAndDestination pad2 = new ProAndDestination();
        pad2.setProNbr("01230123453");
        rqst.setProAndDestinations(Arrays.asList(pad1, pad2));

        CalculateServiceStandardForShipmentsResp resp = calculateServiceStandardForShipmentsImpl
            .calculateServiceStandardForShipments(rqst, txnContext, entityManager);

        assertNotNull(resp);
        assertNotNull(resp.getShipmentServiceStandards());
        assertEquals(0, resp.getShipmentServiceStandards().size());
        assertNotNull(resp.getWarnings());
        assertEquals(2, resp.getWarnings().size());
        assertEquals("", resp.getWarnings().get(0).getErrorCd());
        assertEquals("", resp.getWarnings().get(1).getErrorCd());
        assertEquals("ProNbr", resp.getWarnings().get(0).getFieldName());
        assertEquals("ProNbr", resp.getWarnings().get(1).getFieldName());
        assertTrue(resp.getWarnings().stream().anyMatch(dve -> dve.getFieldValue().equals("01110123456")));
        assertTrue(resp.getWarnings().stream().anyMatch(dve -> dve.getFieldValue().equals("01230123453")));
        assertTrue(resp.getWarnings().stream().allMatch(dve -> dve.getMessage().equals("Shipment Acquired Type is not Regular Pkup.")));

    }

    @Test
    public void testListProStatus_ok() throws ServiceException {
        List<ShmShipment> mockedShmListDB = mockedShmListDB();

        when(shipmentDAO.listShipmentsByProNbrs(anyListOf(String.class), any(ShmShipmentEagerLoadPlan.class), eq(entityManager)))
            .thenReturn(mockedShmListDB);
        when(restClient.getCalculateTransitTimes(anyListOf(CalculateTransitTimesTask.class), eq(txnContext))).thenReturn(mockedCalcTransitTime());

        CalculateServiceStandardForShipmentsRqst rqst = new CalculateServiceStandardForShipmentsRqst();
        ProAndDestination pad1 = new ProAndDestination();
        pad1.setProNbr("01110123456");
        ProAndDestination pad2 = new ProAndDestination();
        pad2.setProNbr("01230123453");
        rqst.setProAndDestinations(Arrays.asList(pad1, pad2));

        CalculateServiceStandardForShipmentsResp resp = calculateServiceStandardForShipmentsImpl
            .calculateServiceStandardForShipments(rqst, txnContext, entityManager);

        verify(shipmentDAO, times(1)).listShipmentsByProNbrs(any(), any(), any());
        verify(shipmentDAO, times(2)).save(any(), any());
        verify(shipmentDAO, times(2)).updateDb2ShmShipmentForUpdServiceStdInfo(any(), any());

        assertNotNull(resp);
        assertNotNull(resp.getWarnings());
        assertEquals(0, resp.getWarnings().size());
        assertNotNull(resp.getShipmentServiceStandards());
        assertEquals(2, resp.getShipmentServiceStandards().size());
    }

    private Collection<CalculateTransitTimesResult> mockedCalcTransitTime() {
        List<CalculateTransitTimesResult> resp = new ArrayList<CalculateTransitTimesResult>();
        PostalTransitTime postalTransitTime1 = new PostalTransitTime();
        PostalTransitTime postalTransitTime2 = new PostalTransitTime();
        postalTransitTime1.setOrigSicCd("UCO");
        postalTransitTime1.setDestSicCd("PTO");
        postalTransitTime1.setRequestedPkupDate("12/28/2022");
        postalTransitTime1.setEstdDlvrDate("2023-02-05");
        postalTransitTime1.setTransitDays(BigInteger.TEN);

        postalTransitTime2.setOrigSicCd("XME");
        postalTransitTime2.setDestSicCd("PTO");
        postalTransitTime2.setRequestedPkupDate("12/23/2020");
        postalTransitTime2.setEstdDlvrDate("2023-02-05");
        List<PostalTransitTime> ptts = Lists.newArrayList(postalTransitTime1, postalTransitTime2);
        CalculateTransitTimesResult calc1 = new CalculateTransitTimesResult(ptts, "");
        resp.add(calc1);
        return resp;
    }

    private List<ShmShipment> mockedShmListDB() {
        List<ShmShipment> mockedShmListDB = new ArrayList<ShmShipment>();

        ShmShipment shm1 = new ShmShipment();
        shm1.setProNbrTxt("01110123456");
        shm1.setOrigTrmnlSicCd("UCO");
        shm1.setDestTrmnlSicCd("PTO");
        shm1.setLstMvmtTmst(Timestamp.valueOf(LocalDateTime.of(2022, 12, 28, 15, 15, 25)));
        shm1.setShpmtAcqrTypCd(ShipmentAcquiredTypeCdTransformer.toCode(ShipmentAcquiredTypeCd.REGULAR_PKUP));
        shm1.setSvcStrtDt(BasicTransformer.toDate("2022-06-22"));
        shm1.setEstTrnstDays(BigDecimal.ZERO);
        shm1.setPkupDt(BasicTransformer.toDate("2022-06-22"));

        ShmShipment shm2 = new ShmShipment();
        shm2.setProNbrTxt("01230123453");
        shm2.setOrigTrmnlSicCd("XME");
        shm2.setDestTrmnlSicCd("PTO");
        shm2.setLstMvmtTmst(Timestamp.valueOf(LocalDateTime.of(2020, 12, 23, 15, 15, 25)));
        shm2.setShpmtAcqrTypCd(ShipmentAcquiredTypeCdTransformer.toCode(ShipmentAcquiredTypeCd.REGULAR_PKUP));
        shm2.setSvcStrtDt(BasicTransformer.toDate("2022-06-22"));
        shm2.setEstTrnstDays(BigDecimal.ZERO);
        shm2.setPkupDt(BasicTransformer.toDate("2022-06-22"));
        
        mockedShmListDB.add(shm1);
        mockedShmListDB.add(shm2);
        return mockedShmListDB;
    }

}
