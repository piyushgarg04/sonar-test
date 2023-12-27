package com.xpo.ltl.shipment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.BillStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.BillStatusCd;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmLnhDimensionDelegate;

public class DeleteHandlingUnitImplTest {

	@InjectMocks
    private DeleteHandlingUnitImpl deleteHandlingUnitImpl;

    @Mock
    private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

    @Mock
    private ShmShipmentSubDAO shmShipmentSubDAO;

    @Mock
    private ShmLnhDimensionDelegate shmLnhDimensionDelegate;

	@Mock
    private EntityManager entityManager;

    @Mock
    private EntityManager db2EntityManager;

	private TransactionContext txnContext;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);

		txnContext = new TransactionContext();
		txnContext.setTransactionTimestamp(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance()));
		final User user = new User();
		user.setUserId("user");
		txnContext.setUser(user);
	}

    @Test(expected = ValidationException.class)
    public void deleteHandlingUnit_ProNbrInvalid() throws Exception {
        deleteHandlingUnitImpl.deleteHandlingUnit("1234", txnContext, entityManager);
	}

    @Test(expected = NotFoundException.class)
    public void deleteHandlingUnit_HUNotFound() throws Exception {
        String childPro = "06470223813";

        when(shmHandlingUnitSubDAO.findByTrackingProNumber(childPro, entityManager)).thenReturn(null);

        deleteHandlingUnitImpl.deleteHandlingUnit(childPro, txnContext, entityManager);
    }

    // validation removed on LPPLT-1521
    @Ignore
    @Test(expected = ValidationException.class)
    public void deleteHandlingUnit_CannotDeleteBilled() throws Exception {
        String childPro = "06470223813";
        ShmShipment dummyShm = new ShmShipment();
        dummyShm.setBillStatCd(BillStatusCdTransformer.toCode(BillStatusCd.BILLED));
        ShmHandlingUnit dummyHU = new ShmHandlingUnit();
        dummyHU.setChildProNbrTxt(childPro);
        dummyHU.setHandlingMvmtCd("NORMAL");
        dummyHU.setShmShipment(dummyShm);

        when(shmHandlingUnitSubDAO.findByTrackingProNumber(childPro, entityManager)).thenReturn(dummyHU);

        deleteHandlingUnitImpl.deleteHandlingUnit(childPro, txnContext, entityManager);
    }

    @Test
    public void deleteHandlingUnit_DeleteAstray() throws Exception {
        String childPro = "06470223813";
        ShmShipment dummyShm = new ShmShipment();
        dummyShm.setBillStatCd(BillStatusCdTransformer.toCode(BillStatusCd.BILLED));
        ShmHandlingUnit dummyHU = new ShmHandlingUnit();
        dummyHU.setChildProNbrTxt(childPro);
        dummyHU.setHandlingMvmtCd("ASTRAY");
        dummyHU.setShmShipment(dummyShm);

        when(shmHandlingUnitSubDAO.findByTrackingProNumber(childPro, entityManager)).thenReturn(dummyHU);

        deleteHandlingUnitImpl.deleteHandlingUnit(childPro, txnContext, entityManager);

        verify(shmHandlingUnitSubDAO, times(1)).remove(eq(dummyHU), any());
        verify(shmHandlingUnitSubDAO, times(1)).deleteDB2(any(), any(), any(), any());
    }

    @Test
    public void deleteHandlingUnit_DeleteNonAstrayAndNonReweight() throws Exception {
        String childPro = "06470223813";
        ShmShipment dummyShm = new ShmShipment();
        dummyShm.setBillStatCd(BillStatusCdTransformer.toCode(BillStatusCd.UNBILLED));
        dummyShm.setMtrzdPcsCnt(BigDecimal.TEN);
        dummyShm.setLoosePcsCnt(BigDecimal.TEN);
        dummyShm.setTotPcsCnt(BigDecimal.TEN);

        dummyShm.setPupVolPct(BigDecimal.valueOf(0.45));
        dummyShm.setTotVolCft(BigDecimal.valueOf(100.5));
        dummyShm.setTotWgtLbs(BigDecimal.valueOf(30.6));

        ShmHandlingUnitPK dummyId = new ShmHandlingUnitPK();
        dummyId.setShpInstId(1234554321);
        dummyId.setSeqNbr(2);

        ShmHandlingUnit dummyHU = new ShmHandlingUnit();
        dummyHU.setId(dummyId);
        dummyHU.setChildProNbrTxt(childPro);
        dummyHU.setHandlingMvmtCd("NORMAL");
        dummyHU.setTypeCd("MOTOR");
        dummyHU.setShmShipment(dummyShm);
        dummyHU.setPupVolPct(BigDecimal.valueOf(0.1));
        dummyHU.setVolCft(BigDecimal.valueOf(50.4));
        dummyHU.setWgtLbs(BigDecimal.valueOf(15.5));
        dummyHU.setReweighInd(BasicTransformer.toString(false));
        dummyHU.setLstUpdtTmst(BasicTransformer.toTimestamp(BasicTransformer.toXMLGregorianCalendar(new Date())));

        when(shmHandlingUnitSubDAO.findByTrackingProNumber(childPro, entityManager)).thenReturn(dummyHU);
        when(shmHandlingUnitSubDAO.findByParentShipmentInstanceId(dummyHU.getId().getShpInstId(), entityManager)).thenReturn(createDummyHUDBList());

        deleteHandlingUnitImpl.deleteHandlingUnit(childPro, txnContext, entityManager);

        verify(shmHandlingUnitSubDAO, times(1)).remove(eq(dummyHU), any());
        verify(shmHandlingUnitSubDAO, times(1)).deleteDB2(any(), any(), any(), any());
        verify(shmLnhDimensionDelegate, times(1)).deleteDimensions(any(), any());
        verify(entityManager, times(1)).flush();
        verify(db2EntityManager, times(1)).flush();
        verify(shmLnhDimensionDelegate, times(2)).createShmLnhDimension(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(shmShipmentSubDAO, times(1)).save(any(), any());
        verify(shmShipmentSubDAO, times(1)).updateDB2ShmShipment(eq(dummyShm), any(), any(), any());
//        verify(shmHandlingUnitSubDAO, times(2)).updateDB2ShmHandlingUnit(isA(ShmHandlingUnit.class), isA(Timestamp.class), any(), any());

        assertEquals(BigDecimal.valueOf(0.45), dummyShm.getPupVolPct());
        assertEquals(BigDecimal.valueOf(100.5), dummyShm.getTotVolCft());
        assertEquals(BigDecimal.valueOf(30.6), dummyShm.getTotWgtLbs());
        assertEquals(BigDecimal.valueOf(10), dummyShm.getTotPcsCnt());
        assertEquals(BigDecimal.valueOf(9), dummyShm.getMtrzdPcsCnt());
        assertEquals(BigDecimal.TEN, dummyShm.getLoosePcsCnt());

    }

    private List<ShmHandlingUnit> createDummyHUDBList() {
        ShmHandlingUnitPK dummyId1 = new ShmHandlingUnitPK();
        ShmHandlingUnitPK dummyId2 = new ShmHandlingUnitPK();
        ShmHandlingUnitPK dummyId3 = new ShmHandlingUnitPK();
        dummyId1.setSeqNbr(1);
        dummyId2.setSeqNbr(2);
        dummyId3.setSeqNbr(3);
        dummyId1.setShpInstId(1234554321);
        dummyId2.setShpInstId(1234554321);
        dummyId3.setShpInstId(1234554321);
        ShmHandlingUnit dummySibling1 = new ShmHandlingUnit();
        ShmHandlingUnit dummySibling2 = new ShmHandlingUnit();
        ShmHandlingUnit dummySibling3 = new ShmHandlingUnit();
        dummySibling1.setId(dummyId1);
        dummySibling2.setId(dummyId2);
        dummySibling3.setId(dummyId3);
        dummySibling1.setHeightNbr(BigDecimal.TEN);
        dummySibling2.setHeightNbr(BigDecimal.TEN);
        dummySibling3.setHeightNbr(BigDecimal.TEN);
        dummySibling1.setWidthNbr(BigDecimal.TEN);
        dummySibling2.setWidthNbr(BigDecimal.TEN);
        dummySibling3.setWidthNbr(BigDecimal.TEN);
        dummySibling1.setLengthNbr(BigDecimal.TEN);
        dummySibling2.setLengthNbr(BigDecimal.TEN);
        dummySibling3.setLengthNbr(BigDecimal.TEN);
        dummySibling1.setWgtLbs(BigDecimal.TEN);
        dummySibling2.setWgtLbs(BigDecimal.TEN);
        dummySibling3.setWgtLbs(BigDecimal.TEN);
        dummySibling1.setDimensionTypeCd("PICKUP");
        dummySibling2.setDimensionTypeCd("PICKUP");
        dummySibling3.setDimensionTypeCd("PICKUP");
        dummySibling1.setReweighInd(BasicTransformer.toString(false));
        dummySibling2.setReweighInd(BasicTransformer.toString(false));
        dummySibling3.setReweighInd(BasicTransformer.toString(false));
        dummySibling1.setLstUpdtTmst(BasicTransformer.toTimestamp(BasicTransformer.toXMLGregorianCalendar(new Date())));
        dummySibling2.setLstUpdtTmst(BasicTransformer.toTimestamp(BasicTransformer.toXMLGregorianCalendar(new Date())));
        dummySibling3.setLstUpdtTmst(BasicTransformer.toTimestamp(BasicTransformer.toXMLGregorianCalendar(new Date())));

        return Arrays.asList(dummySibling1, dummySibling2, dummySibling3);
    }
}
