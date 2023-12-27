package com.xpo.ltl.shipment.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.UpdateHandlingUnitWeightRqst;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.validators.UpdateHandlingUnitWeightValidator;

public class UpdateHandlingUnitWeightImplTestCase {

    private static final BigDecimal TOT_WGT_LBS = BigDecimal.valueOf(500);

    private static final String A_PARENT_PRO = "4541210282";

    private static final String A_CHILD_PRO = "01110111111";

    @Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

	@Mock
	private EntityManager db2EntityManager;

	@Mock
	private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

	@Mock
	private ShmShipmentSubDAO shmShipmentSubDAO;

	@Spy
    private UpdateHandlingUnitWeightValidator validator;

    @Captor
    private ArgumentCaptor<ShmHandlingUnit> handlingUnitCaptor;

    @Captor
    private ArgumentCaptor<List<ShmHandlingUnit>> handlingUnitListCaptor;

    @Captor
    private ArgumentCaptor<ShmShipment> shipmentCaptor;

	@InjectMocks
    private UpdateHandlingUnitWeightImpl updateHandlingUnitWeightImpl;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = NotFoundException.class)
    public void testUpdateHandlingWeight_HandlingUnitNotFound() throws ServiceException {
        UpdateHandlingUnitWeightRqst rqst = new UpdateHandlingUnitWeightRqst();
        rqst.setWeightLbs(25.487);

        when(shmHandlingUnitSubDAO.findByTrackingProNumber(A_CHILD_PRO, entityManager))
				.thenReturn(null);

        updateHandlingUnitWeightImpl.updateHandlingUnitWeight(rqst, A_CHILD_PRO, txnContext, entityManager);

    }

    @Test(expected = ValidationException.class)
    public void testUpdateHandlingWeight_ProNbrIsBlank() throws ServiceException {
        UpdateHandlingUnitWeightRqst rqst = new UpdateHandlingUnitWeightRqst();
        rqst.setWeightLbs(25.487);

        updateHandlingUnitWeightImpl.updateHandlingUnitWeight(rqst, null, txnContext, entityManager);
    }

    @Test
    public void testUpdateHandlingWeight_AllToReWeightIntegerCase() throws ServiceException {
        UpdateHandlingUnitWeightRqst rqst = new UpdateHandlingUnitWeightRqst();
        rqst.setWeightLbs(200);

        mocks(100.0, "N", 200.0, "N", 200.0, "N");

        updateHandlingUnitWeightImpl.updateHandlingUnitWeight(rqst, A_CHILD_PRO, txnContext, entityManager);

        verifications(1);

        assertReweightedHU(rqst);

        // Assert list hu.
        List<ShmHandlingUnit> huList = handlingUnitListCaptor.getValue();
        assertEquals(2, huList.size());
        huList.forEach(hu -> {
            assertEquals(A_PARENT_PRO, hu.getParentProNbrTxt());
            // 500.00 - 200 = 300 / 2 = 150 each
            assertTrue(BigDecimal.valueOf(150).compareTo(hu.getWgtLbs()) == 0);
            assertEquals("N", hu.getReweighInd());
        });

        assertShipment(TOT_WGT_LBS);
    }

    @Test
    public void testUpdateHandlingWeight_AllToReweight() throws ServiceException {
        UpdateHandlingUnitWeightRqst rqst = new UpdateHandlingUnitWeightRqst();
        rqst.setWeightLbs(200.25);

        mocks(100.33, "N", 200.33, "N", 200.33, "N");

        updateHandlingUnitWeightImpl.updateHandlingUnitWeight(rqst, A_CHILD_PRO, txnContext, entityManager);

        verifications(1);

        assertReweightedHU(rqst);

        // Assert list hu.
        List<ShmHandlingUnit> huList = handlingUnitListCaptor.getValue();
        assertEquals(2, huList.size());
        huList.forEach(hu -> {
            assertEquals(A_PARENT_PRO, hu.getParentProNbrTxt());
            // 500.00 - 200.25 = 299.75 / 2 = 149.87 each
            assertTrue(BigDecimal.valueOf(149.87).compareTo(hu.getWgtLbs()) == 0);
            assertEquals("N", hu.getReweighInd());
        });

        // 149.87 + 149.87 + 200.25 = 499.99
        assertShipment(BigDecimal.valueOf(499.99));
    }

    @Test
    public void testUpdateHandlingWeight_OneToReWeight() throws ServiceException {
        UpdateHandlingUnitWeightRqst rqst = new UpdateHandlingUnitWeightRqst();
        rqst.setWeightLbs(200.25);

        mocks(100.33, "N", 200.33, "Y", 200.33, "N");

        updateHandlingUnitWeightImpl.updateHandlingUnitWeight(rqst, A_CHILD_PRO, txnContext, entityManager);

        verifications(1);

        assertReweightedHU(rqst);

        // Assert list hu.
        List<ShmHandlingUnit> huList = handlingUnitListCaptor.getValue();
        assertEquals(1, huList.size());
        huList.forEach(hu -> {
            assertEquals(A_PARENT_PRO, hu.getParentProNbrTxt());
            // 500.00 - 200.33 - 200.25 = 99.42 / 1 = 99.42 for 2nd one.
            assertTrue(BigDecimal.valueOf(99.42).compareTo(hu.getWgtLbs()) == 0);
            assertEquals("N", hu.getReweighInd());
        });

        // 99.42 + 200.33 + 200.25 = 500
        assertShipment(BigDecimal.valueOf(500));
    }

    @Test
    public void testUpdateHandlingWeight_NoneToReWeight() throws ServiceException {
        UpdateHandlingUnitWeightRqst rqst = new UpdateHandlingUnitWeightRqst();
        rqst.setWeightLbs(120.25);

        mocks(100.33, "N", 220.43, "Y", 200.33, "Y");

        updateHandlingUnitWeightImpl.updateHandlingUnitWeight(rqst, A_CHILD_PRO, txnContext, entityManager);

        verifications(0);
        assertReweightedHU(rqst);
        // 220.43 + 200.33 + 120.25 = 541.01
        assertShipment(BigDecimal.valueOf(541.01));
    }

    @Test
    public void testUpdateHandlingWeight_ReWeightAReWeighted() throws ServiceException {
        UpdateHandlingUnitWeightRqst rqst = new UpdateHandlingUnitWeightRqst();
        rqst.setWeightLbs(120.25);

        mocks(100.33, "Y", 220.43, "Y", 200.33, "Y");

        updateHandlingUnitWeightImpl.updateHandlingUnitWeight(rqst, A_CHILD_PRO, txnContext, entityManager);

        verifications(0);
        assertReweightedHU(rqst);
        // 220.43 + 200.33 + 120.25 = 541.01
        assertShipment(BigDecimal.valueOf(541.01));
    }

    @Test
    public void testUpdateHandlingWeight_ReWeightGreaterThanTotWgtAndNoneToReWeight() throws ServiceException {
        UpdateHandlingUnitWeightRqst rqst = new UpdateHandlingUnitWeightRqst();
        rqst.setWeightLbs(1200.25);

        mocks(100.33, "N", 220.43, "Y", 200.33, "Y");

        updateHandlingUnitWeightImpl.updateHandlingUnitWeight(rqst, A_CHILD_PRO, txnContext, entityManager);

        verifications(0);
        assertReweightedHU(rqst);
        // 220.43 + 200.33 + 1200.25 = 1621.01
        assertShipment(BigDecimal.valueOf(1621.01));
    }

    @Test
    public void testUpdateHandlingWeight_ReWeightGreaterThanTotWgtAndOneToReWeight() throws ServiceException {
        UpdateHandlingUnitWeightRqst rqst = new UpdateHandlingUnitWeightRqst();
        rqst.setWeightLbs(301.22);

        mocks(100.33, "N", 220.43, "Y", 200.33, "N");

        updateHandlingUnitWeightImpl.updateHandlingUnitWeight(rqst, A_CHILD_PRO, txnContext, entityManager);

        verifications(1);
        assertReweightedHU(rqst);

        // Assert list hu.
        List<ShmHandlingUnit> huList = handlingUnitListCaptor.getValue();
        assertEquals(1, huList.size());
        huList.forEach(hu -> {
            assertEquals(A_PARENT_PRO, hu.getParentProNbrTxt());
            // 500.00 - 220.43 - 301.22 = 0 / 2 = 0.00 for each.
            assertTrue(BigDecimal.valueOf(1).compareTo(hu.getWgtLbs()) == 0);
            assertEquals("N", hu.getReweighInd());
        });

        // 0 + 220.43 + 301.22 = 521.65
        assertShipment(BigDecimal.valueOf(522.65));
    }

    @Test
    public void testUpdateHandlingWeight_ReWeightGreaterThanTotWgtAndAllToReWeight() throws ServiceException {
        UpdateHandlingUnitWeightRqst rqst = new UpdateHandlingUnitWeightRqst();
        rqst.setWeightLbs(505.25);

        mocks(100.33, "N", 220.43, "N", 200.33, "N");

        updateHandlingUnitWeightImpl.updateHandlingUnitWeight(rqst, A_CHILD_PRO, txnContext, entityManager);

        verifications(1);

        assertReweightedHU(rqst);

        // Assert list hu.
        List<ShmHandlingUnit> huList = handlingUnitListCaptor.getValue();
        assertEquals(2, huList.size());
        huList.forEach(hu -> {
            assertEquals(A_PARENT_PRO, hu.getParentProNbrTxt());
            // 500.00 - 505.25 = 0 / 2 = 0.00 for each.
            assertTrue(BigDecimal.valueOf(1).compareTo(hu.getWgtLbs()) == 0);
            assertEquals("N", hu.getReweighInd());
        });


        // 0 + 0 + 505.25 = 505.25
        assertShipment(BigDecimal.valueOf(507.25));
    }

    private void assertShipment(BigDecimal newReweight) {
        ShmShipment shm = shipmentCaptor.getValue();
        assertEquals(A_PARENT_PRO, shm.getProNbrTxt());
        assertTrue(TOT_WGT_LBS.compareTo(shm.getTotWgtLbs()) == 0);
        assertTrue(newReweight.compareTo(shm.getReweighWgtLbs()) == 0);
    }

    private void assertReweightedHU(UpdateHandlingUnitWeightRqst rqst) {
        ShmHandlingUnit handlingUnit = handlingUnitCaptor.getValue();
        assertEquals(A_CHILD_PRO, handlingUnit.getChildProNbrTxt());
        assertEquals(A_PARENT_PRO, handlingUnit.getParentProNbrTxt());
        assertTrue(BigDecimal.valueOf(rqst.getWeightLbs()).compareTo(handlingUnit.getWgtLbs()) == 0);
        assertEquals("Y", handlingUnit.getReweighInd());
    }

    private void mocks(double previousWgt, String previousRWInd, double firstWgt, String firstRWgtInd, double scndWgt,
        String scndRWgtInd) {
        ShmHandlingUnit aShmHandlingUnitMocked = aShmHandlingUnitMocked(previousWgt, previousRWInd);
        when(shmHandlingUnitSubDAO.findByTrackingProNumber(A_CHILD_PRO, entityManager))
            .thenReturn(aShmHandlingUnitMocked);
        when(shmShipmentSubDAO.findByIdOrProNumber(A_PARENT_PRO, null, entityManager)).thenReturn(aShmShipmentMocked());
        when(shmHandlingUnitSubDAO.findByParentProNumber(A_PARENT_PRO, entityManager))
            .thenReturn(
                aShmHandlingUnitListMocked(aShmHandlingUnitMocked, firstWgt, firstRWgtInd, scndWgt, scndRWgtInd));
    }

    private void verifications(int timesForHUList) throws ServiceException {
        // only capture params in exadata.
        verify(shmHandlingUnitSubDAO).persist(handlingUnitCaptor.capture(), eq(entityManager));
        verify(shmHandlingUnitSubDAO, times(timesForHUList))
            .persist(handlingUnitListCaptor.capture(), eq(entityManager));
        verify(shmShipmentSubDAO).save(shipmentCaptor.capture(), eq(entityManager));

        // db2
        verify(shmHandlingUnitSubDAO, atLeast(1))
            .updateDB2ShmHandlingUnit(any(ShmHandlingUnit.class), any(Timestamp.class), eq(txnContext),
                eq(db2EntityManager));
        verify(shmShipmentSubDAO)
            .updateDb2ShmShipmentForUpdHUWeight(any(ShmShipment.class), eq(db2EntityManager));
    }

    private List<ShmHandlingUnit> aShmHandlingUnitListMocked(ShmHandlingUnit aShmHandlingUnitMocked, Double firstWgt, String firstRWgtInd, Double scndWgt,
        String scndRWgtInd) {

        ShmHandlingUnit hu1 = new ShmHandlingUnit();
        hu1.setChildProNbrTxt("01110222222");
        hu1.setParentProNbrTxt(A_PARENT_PRO);
        hu1.setReweighInd(firstRWgtInd);
        hu1.setWgtLbs(BigDecimal.valueOf(firstWgt));

        ShmHandlingUnit hu2 = new ShmHandlingUnit();
        hu2.setChildProNbrTxt("01110333333");
        hu2.setParentProNbrTxt(A_PARENT_PRO);
        hu2.setReweighInd(scndRWgtInd);
        hu2.setWgtLbs(BigDecimal.valueOf(scndWgt));
        return Arrays.asList(hu1, hu2, aShmHandlingUnitMocked);
    }


    private ShmHandlingUnit aShmHandlingUnitMocked(Double wgt, String reweighInd) {
        ShmHandlingUnit hu = new ShmHandlingUnit();
        hu.setChildProNbrTxt(A_CHILD_PRO);
        hu.setParentProNbrTxt(A_PARENT_PRO);
        hu.setWgtLbs(BigDecimal.valueOf(wgt));
        hu.setReweighInd(reweighInd);
        return hu;
    }

    private ShmShipment aShmShipmentMocked() {
        ShmShipment shipment = new ShmShipment();
        shipment.setProNbrTxt(A_PARENT_PRO);
        shipment.setTotWgtLbs(TOT_WGT_LBS);
        return shipment;
    }

}
