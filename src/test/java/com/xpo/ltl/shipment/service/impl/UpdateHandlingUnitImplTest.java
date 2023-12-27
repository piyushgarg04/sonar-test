package com.xpo.ltl.shipment.service.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmtPK;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.service.entity.ShmXdockExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmXdockExcpPK;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.HandlingUnit;
import com.xpo.ltl.api.shipment.v2.UpdateHandlingUnitsRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitMvmtSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmXdockExcpSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmHandlingUnitDelegate;

public class UpdateHandlingUnitImplTest {

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

    @Mock
    private ShmXdockExcpSubDAO shmXdockExcpSubDAO;

    @Mock
    private ShmHandlingUnitMvmtSubDAO shmHandlingUnitMvmtSubDAO;

    @Mock
    private ShmHandlingUnitDelegate shmHandlingUnitDelegate;

	@InjectMocks
    private UpdateHandlingUnitsImpl updateHandlingUnitsImpl;

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
    public void testUpdateHandlingUnits_NoRequest() {
        NullPointerException e = Assertions
            .assertThrows(NullPointerException.class,
                () -> updateHandlingUnitsImpl
                    .updateHandlingUnits(null, entityManager, txnContext));

        Assertions.assertTrue(e.getMessage().contains("updateHandlingUnitRqst Request is required."));

    }

    @Test
    public void testUpdateHandlingUnits_NoRequestingSicCd() {

        UpdateHandlingUnitsRqst rqst = new UpdateHandlingUnitsRqst();

        ValidationException e = Assertions
            .assertThrows(ValidationException.class,
                () -> updateHandlingUnitsImpl.updateHandlingUnits(rqst, entityManager, txnContext));

        Assertions
            .assertEquals(ValidationErrorMessage.VALIDATION_ERRORS_FOUND.errorCode(), e.getFault().getErrorCode());
        Assertions.assertEquals("N/A", e.getFault().getMoreInfo().get(0).getLocation());
        Assertions.assertEquals("RequestingSicCd param is required.", e.getFault().getMoreInfo().get(0).getMessage());
    }

    @Test
    public void testUpdateHandlingUnits_NoHandlingUnits() {

        UpdateHandlingUnitsRqst rqst = new UpdateHandlingUnitsRqst();
        rqst.setRequestingSicCd("UCO");

        ValidationException e = Assertions
            .assertThrows(ValidationException.class,
                () -> updateHandlingUnitsImpl.updateHandlingUnits(rqst, entityManager, txnContext));

        Assertions
            .assertEquals(ValidationErrorMessage.VALIDATION_ERRORS_FOUND.errorCode(), e.getFault().getErrorCode());
        Assertions.assertEquals("N/A", e.getFault().getMoreInfo().get(0).getLocation());
        Assertions
            .assertEquals("At least one HandlingUnit is required.", e.getFault().getMoreInfo().get(0).getMessage());
    }

    @Test
    public void testUpdateHandlingUnits_EmptyHandlingUnits() {

        UpdateHandlingUnitsRqst rqst = new UpdateHandlingUnitsRqst();
        rqst.setRequestingSicCd("UCO");
        rqst.setHandlingUnits(new ArrayList<>());

        ValidationException e = Assertions
            .assertThrows(ValidationException.class,
                () -> updateHandlingUnitsImpl.updateHandlingUnits(rqst, entityManager, txnContext));

        Assertions
            .assertEquals(ValidationErrorMessage.VALIDATION_ERRORS_FOUND.errorCode(), e.getFault().getErrorCode());
        Assertions.assertEquals(1, e.getFault().getMoreInfo().size());
        Assertions.assertEquals("N/A", e.getFault().getMoreInfo().get(0).getLocation());
        Assertions
            .assertEquals("At least one HandlingUnit is required.", e.getFault().getMoreInfo().get(0).getMessage());
    }

    @Test
    public void testUpdateHandlingUnits_NoChildPro() {

        UpdateHandlingUnitsRqst rqst = new UpdateHandlingUnitsRqst();
        rqst.setRequestingSicCd("UCO");

        HandlingUnit hu1 = new HandlingUnit();
        HandlingUnit hu2 = new HandlingUnit();
        hu1.setParentProNbr("01100795543");
        hu2.setChildProNbr("06481037080");
        hu2.setParentProNbr("01100795543");
        hu1.setHandlingMovementCd("NORMAL");
        hu2.setHandlingMovementCd("NORMAL");
        AuditInfo ai1 = new AuditInfo();
        AuditInfo ai2 = new AuditInfo();
        ai1.setUpdateById("U1234");
        ai2.setUpdateById("U2345");
        hu1.setAuditInfo(ai1);
        hu2.setAuditInfo(ai2);
        rqst.setHandlingUnits(Arrays.asList(hu1, hu2));

        ShmHandlingUnit mockedHuDB1 = new ShmHandlingUnit();
        mockedHuDB1.setChildProNbrTxt("06481037080");
//        when(shmHandlingUnitSubDAO.findByChildProNumberList(anySet(), eq(entityManager)))
//            .thenReturn(Arrays.asList(mockedHuDB1));
        when(shmHandlingUnitSubDAO.findByParentProNumberList(anyList(), eq(entityManager)))
        .thenReturn(Arrays.asList(mockedHuDB1));

        ValidationException e = Assertions
            .assertThrows(ValidationException.class,
                () -> updateHandlingUnitsImpl.updateHandlingUnits(rqst, entityManager, txnContext));

        Assertions
            .assertEquals(ValidationErrorMessage.VALIDATION_ERRORS_FOUND.errorCode(), e.getFault().getErrorCode());
        Assertions
            .assertEquals(1, e.getFault().getMoreInfo().size());
        Assertions.assertEquals("N/A", e.getFault().getMoreInfo().get(0).getLocation());
        Assertions
            .assertEquals("Child PRO is required.", e.getFault().getMoreInfo().get(0).getMessage());
    }

    @Test
    public void testUpdateHandlingUnits_InvalidProAndNoMvmCd() {

        UpdateHandlingUnitsRqst rqst = new UpdateHandlingUnitsRqst();
        rqst.setRequestingSicCd("UCO");

        HandlingUnit hu1 = new HandlingUnit();
        HandlingUnit hu2 = new HandlingUnit();
        hu1.setChildProNbr("06481037080");
        hu1.setParentProNbr("01100795543");
        hu2.setChildProNbr("06481011111137102"); // invalid.
        hu2.setParentProNbr("01100795543");
        hu1.setHandlingMovementCd("NORMAL");
        AuditInfo ai1 = new AuditInfo();
        AuditInfo ai2 = new AuditInfo();
        ai1.setUpdateById("U1234");
        ai2.setUpdateById("U2345");
        hu1.setAuditInfo(ai1);
        hu2.setAuditInfo(ai2);
        rqst.setHandlingUnits(Arrays.asList(hu1, hu2));

        ShmHandlingUnit mockedHuDB1 = new ShmHandlingUnit();
        mockedHuDB1.setChildProNbrTxt("06481037080");

//        when(shmHandlingUnitSubDAO.findByChildProNumberList(anySet(), eq(entityManager)))
//            .thenReturn(Arrays.asList(mockedHuDB1));
        when(shmHandlingUnitSubDAO.findByParentProNumberList(anyList(), eq(entityManager)))
        .thenReturn(Arrays.asList(mockedHuDB1));

        ValidationException e = Assertions
            .assertThrows(ValidationException.class,
                () -> updateHandlingUnitsImpl.updateHandlingUnits(rqst, entityManager, txnContext));

        Assertions
            .assertEquals(ValidationErrorMessage.VALIDATION_ERRORS_FOUND.errorCode(), e.getFault().getErrorCode());
        Assertions.assertEquals(1, e.getFault().getMoreInfo().size());
        Assertions.assertEquals("06481011111137102", e.getFault().getMoreInfo().get(0).getLocation());
        Assertions
            .assertEquals(
                "Child PRO 06481011111137102 is invalid. Handling Unit Movement Code must be NORMAL or MISSING.",
                e.getFault().getMoreInfo().get(0).getMessage());
    }

    @Test
    public void testUpdateHandlingUnits_NoUserId() {

        UpdateHandlingUnitsRqst rqst = new UpdateHandlingUnitsRqst();
        rqst.setRequestingSicCd("UCO");

        HandlingUnit hu1 = new HandlingUnit();
        hu1.setParentProNbr("01100795543");
        hu1.setChildProNbr("06481037080");
        hu1.setHandlingMovementCd("NORMAL");
        AuditInfo ai1 = new AuditInfo();
        hu1.setAuditInfo(ai1);
        rqst.setHandlingUnits(Arrays.asList(hu1));

        ShmHandlingUnit mockedHuDB1 = new ShmHandlingUnit();
        mockedHuDB1.setChildProNbrTxt("06481037080");
        ShmHandlingUnitPK id1 = new ShmHandlingUnitPK();
        id1.setShpInstId(101L);
        id1.setSeqNbr(1L);
        mockedHuDB1.setId(id1);

//        when(shmHandlingUnitSubDAO.findByChildProNumberList(anySet(), eq(entityManager)))
//            .thenReturn(Arrays.asList(mockedHuDB1));
        when(shmHandlingUnitSubDAO.findByParentProNumberList(anyList(), eq(entityManager)))
        .thenReturn(Arrays.asList(mockedHuDB1));

        ValidationException e = Assertions
            .assertThrows(ValidationException.class,
                () -> updateHandlingUnitsImpl.updateHandlingUnits(rqst, entityManager, new TransactionContext()));

        Assertions
            .assertEquals(ValidationErrorMessage.VALIDATION_ERRORS_FOUND.errorCode(), e.getFault().getErrorCode());
        Assertions.assertEquals(1, e.getFault().getMoreInfo().size());
        Assertions.assertEquals("N/A", e.getFault().getMoreInfo().get(0).getLocation());
        Assertions
            .assertEquals("userId is required.",
                e.getFault().getMoreInfo().get(0).getMessage());
    }

    @Test
    public void testUpdateHandlingUnits_NoHandlingMvmCds() {

        UpdateHandlingUnitsRqst rqst = new UpdateHandlingUnitsRqst();
        rqst.setRequestingSicCd("UCO");

        HandlingUnit hu1 = new HandlingUnit();
        HandlingUnit hu2 = new HandlingUnit();
        hu1.setParentProNbr("01100795543");
        hu1.setChildProNbr("06481037080");
        hu2.setParentProNbr("01100795543");
        hu2.setChildProNbr("06481037102");
        hu2.setHandlingMovementCd("OTHER CD");
        AuditInfo ai1 = new AuditInfo();
        AuditInfo ai2 = new AuditInfo();
        ai1.setUpdateById("U1234");
        ai2.setUpdateById("U2345");
        hu1.setAuditInfo(ai1);
        hu2.setAuditInfo(ai2);
        rqst.setHandlingUnits(Arrays.asList(hu1, hu2));

        ShmHandlingUnit mockedHuDB1 = new ShmHandlingUnit();
        ShmHandlingUnit mockedHuDB2 = new ShmHandlingUnit();
        mockedHuDB1.setChildProNbrTxt("06481037080");
        mockedHuDB2.setChildProNbrTxt("06481037102");

//        when(shmHandlingUnitSubDAO.findByChildProNumberList(anySet(), eq(entityManager)))
//            .thenReturn(Arrays.asList(mockedHuDB1, mockedHuDB2));
        when(shmHandlingUnitSubDAO.findByParentProNumberList(anyList(), eq(entityManager)))
        .thenReturn(Arrays.asList(mockedHuDB1, mockedHuDB2));

        ValidationException e = Assertions
            .assertThrows(ValidationException.class,
                () -> updateHandlingUnitsImpl.updateHandlingUnits(rqst, entityManager, txnContext));

        Assertions
            .assertEquals(ValidationErrorMessage.VALIDATION_ERRORS_FOUND.errorCode(), e.getFault().getErrorCode());
        Assertions.assertEquals(2, e.getFault().getMoreInfo().size());
        Assertions.assertEquals("06481037080", e.getFault().getMoreInfo().get(0).getLocation());
        Assertions
            .assertEquals("Handling Unit Movement Code must be NORMAL or MISSING.",
                e.getFault().getMoreInfo().get(0).getMessage());
        Assertions.assertEquals("06481037102", e.getFault().getMoreInfo().get(1).getLocation());
        Assertions
            .assertEquals("Handling Unit Movement Code must be NORMAL or MISSING.",
                e.getFault().getMoreInfo().get(1).getMessage());
    }

    @Test
    public void testUpdateHandlingUnits_ChildProNotFound() {

        UpdateHandlingUnitsRqst rqst = new UpdateHandlingUnitsRqst();
        rqst.setRequestingSicCd("UCO");
        rqst.setCurrentTrailerInstanceId(1L);
        rqst.setTrailerIdSuffixNbr(10L);
        rqst.setTrailerIdPrefix("PFX");

        HandlingUnit hu1 = new HandlingUnit();
        HandlingUnit hu2 = new HandlingUnit();
        hu1.setChildProNbr("06481037080");
        hu1.setParentProNbr("01100795543");
        hu2.setChildProNbr("06481037102"); // will not be found in DB.
        hu2.setParentProNbr("01100795543");
        hu1.setHandlingMovementCd("NORMAL");
        hu2.setHandlingMovementCd("MISSING");
        AuditInfo ai1 = new AuditInfo();
        AuditInfo ai2 = new AuditInfo();
        ai1.setUpdateById("U1234");
        ai2.setUpdateById("U2345");
        hu1.setAuditInfo(ai1);
        hu2.setAuditInfo(ai2);
        rqst.setHandlingUnits(Arrays.asList(hu1, hu2));

        ShmHandlingUnit mockedHuDB1 = new ShmHandlingUnit();
        mockedHuDB1.setChildProNbrTxt("06481037080");
        mockedHuDB1.setCurrentTrlrInstId(BigDecimal.ONE);

        when(shmHandlingUnitSubDAO.findByParentProNumberList(anyList(), eq(entityManager)))
        .thenReturn(Arrays.asList(mockedHuDB1));

        ValidationException e = Assertions
            .assertThrows(ValidationException.class,
                () -> updateHandlingUnitsImpl.updateHandlingUnits(rqst, entityManager, txnContext));

        Assertions
            .assertEquals(ValidationErrorMessage.VALIDATION_ERRORS_FOUND.errorCode(), e.getFault().getErrorCode());
        Assertions.assertEquals(1, e.getFault().getMoreInfo().size());
        Assertions.assertEquals("06481037102", e.getFault().getMoreInfo().get(0).getLocation());
        Assertions
            .assertEquals("Child PRO is not found.",
                e.getFault().getMoreInfo().get(0).getMessage());
    }

    @Test
    public void testUpdateHandlingUnits_Ok() throws ServiceException {

        UpdateHandlingUnitsRqst rqst = new UpdateHandlingUnitsRqst();
        rqst.setRequestingSicCd("UCO");
        rqst.setUserId("U1234");
        rqst.setCurrentTrailerInstanceId(1L);
        rqst.setTrailerIdSuffixNbr(10L);
        rqst.setTrailerIdPrefix("PFX");

        HandlingUnit hu1 = new HandlingUnit();
        HandlingUnit hu2 = new HandlingUnit();
        hu1.setChildProNbr("06481037080");
        hu1.setParentProNbr("01100795543");
        hu2.setChildProNbr("06481037102"); // will not be found in DB.
        hu2.setParentProNbr("01100795543");
        hu1.setHandlingMovementCd("NORMAL");
        hu2.setHandlingMovementCd("MISSING");
        AuditInfo ai1 = new AuditInfo();
        AuditInfo ai2 = new AuditInfo();
        ai1.setUpdateById("U1234");
        ai2.setUpdateById("U1234");
        hu1.setAuditInfo(ai1);
        hu2.setAuditInfo(ai2);
        rqst.setHandlingUnits(Arrays.asList(hu1, hu2));

        ShmShipment mockedShipment2 = new ShmShipment();
        mockedShipment2.setCurrSicCd("UCO");
        mockedShipment2.setShpInstId(101L);

        when(shmShipmentSubDAO.findByProOrShipmentId
                 (any(), any(), any(), eq(Boolean.FALSE), any(), eq(entityManager)))
            .thenReturn(mockedShipment2);


        ShmShipment mockedShipment = new ShmShipment();
        mockedShipment.setCurrSicCd("UCO");
        mockedShipment.setShpInstId(101L);
        ShmHandlingUnit mockedHuDB1 = new ShmHandlingUnit();
        ShmHandlingUnit mockedHuDB2 = new ShmHandlingUnit();
        mockedHuDB1.setParentProNbrTxt("01100795543");
        mockedHuDB1.setChildProNbrTxt("06481037080");
        mockedHuDB1.setCurrentSicCd("UCO");
        mockedHuDB1.setHandlingMvmtCd("MISSING");
        mockedHuDB1.setCurrentTrlrInstId(BigDecimal.ONE);
        mockedHuDB2.setParentProNbrTxt("01100795543");
        mockedHuDB2.setChildProNbrTxt("06481037102");
        mockedHuDB2.setCurrentSicCd("UCO");
        mockedHuDB2.setHandlingMvmtCd("MISSING");
        mockedHuDB2.setCurrentTrlrInstId(BigDecimal.TEN);
        ShmHandlingUnitPK id1 = new ShmHandlingUnitPK();
        id1.setShpInstId(101L);
        id1.setSeqNbr(1L);
        mockedHuDB1.setId(id1);
        mockedHuDB1.setShmShipment(mockedShipment);

        ShmHandlingUnitPK id2 = new ShmHandlingUnitPK();
        id2.setShpInstId(101L);
        id2.setSeqNbr(1L);
        mockedHuDB2.setId(id2);
        mockedHuDB2.setShmShipment(mockedShipment);

//        when(shmHandlingUnitSubDAO.findByChildProNumberList(anySet(), eq(entityManager)))
//            .thenReturn(Arrays.asList(mockedHuDB1, mockedHuDB2));
        when(shmHandlingUnitSubDAO.findByParentProNumberList(anyList(), eq(entityManager)))
        .thenReturn(Arrays.asList(mockedHuDB1, mockedHuDB2));

        ShmXdockExcp mockedXdockExcp = new ShmXdockExcp();
        mockedXdockExcp.setShortPcsCnt(BigDecimal.ONE);
        ShmXdockExcpPK excpId = new ShmXdockExcpPK();
        excpId.setSeqNbr(1L);
        excpId.setShpInstId(101L);
        mockedXdockExcp.setId(excpId);

        when(shmXdockExcpSubDAO.findByShipmentInstId(any(), eq(entityManager)))
            .thenReturn(Arrays.asList(mockedXdockExcp));

        ShmHandlingUnitMvmtPK mockedMaxSeq1 = new ShmHandlingUnitMvmtPK();
        ShmHandlingUnitMvmtPK mockedMaxSeq2 = new ShmHandlingUnitMvmtPK();
        mockedMaxSeq1.setShpInstId(101L);
        mockedMaxSeq2.setShpInstId(102L);
        mockedMaxSeq1.setSeqNbr(1L);
        mockedMaxSeq2.setSeqNbr(1L);
        mockedMaxSeq1.setMvmtSeqNbr(1L);
        mockedMaxSeq2.setMvmtSeqNbr(1L);

        when(shmHandlingUnitMvmtSubDAO.getMaxMvmtPKByShpInstIdAndSeqNbrPairs(anyList(), eq(entityManager)))
            .thenReturn(Arrays.asList(mockedMaxSeq1, mockedMaxSeq2));

        updateHandlingUnitsImpl.updateHandlingUnits(rqst, entityManager, txnContext);

        Assertions.assertEquals("UCO", mockedHuDB1.getCurrentSicCd());
        Assertions.assertEquals("UCO", mockedHuDB2.getCurrentSicCd());

        // missing to normal must update split
        verify(shmHandlingUnitDelegate, times(1))
            .updateShmHandlingUnit(eq(mockedHuDB1), any(), any(), any(), any(), eq(Boolean.FALSE), any(), eq(Boolean.FALSE), any(), eq(entityManager),
                eq(txnContext));
        //missing to missing cannot update split
        verify(shmHandlingUnitDelegate, times(1))
            .updateShmHandlingUnit(eq(mockedHuDB2), any(), any(), any(), any(), eq(null), any(), eq(Boolean.FALSE), any(), eq(entityManager),
                eq(txnContext));

    }

}
