package com.xpo.ltl.shipment.service.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.rest.User;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitPK;
import com.xpo.ltl.api.shipment.service.entity.ShmLnhDimension;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.ShipmentVolumeMethodCd;
import com.xpo.ltl.api.shipment.v2.ShipmentVolumeTypeCd;
import com.xpo.ltl.api.shipment.v2.UpdateHandlingUnitDimensionsRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmLnhDimensionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmEventDelegate;
import com.xpo.ltl.shipment.service.delegates.ShmHandlingUnitDelegate;
import com.xpo.ltl.shipment.service.delegates.ShmLnhDimensionDelegate;
import com.xpo.ltl.shipment.service.validators.UpdateHandlingUnitDimensionsValidator;

public class UpdateHandlingUnitDimensionsImplTestCase {

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
	private ShmLnhDimensionSubDAO shmLnhDimensionSubDAO;

    @Mock
    private ShmHandlingUnitDelegate shmHandlingUnitDelegate;

    @Mock
    private ShmLnhDimensionDelegate shmLnhDimensionDelegate;

    @Mock
    private ShmEventDelegate shmEventDelegate;

	@Spy
	private UpdateHandlingUnitDimensionsValidator validator;

	@InjectMocks
	private UpdateHandlingUnitDimensionsImpl updateHandlingUnitDimensionsImpl;

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
	public void testUpdateHandlingUnitDimensions_HandlingUnitNotFound() {

		String trackingProNumber = "4541210282";
		UpdateHandlingUnitDimensionsRqst rqst = new UpdateHandlingUnitDimensionsRqst();
		rqst.setHeightNbr(50);
		rqst.setLengthNbr(25);
		rqst.setWidthNbr(40);

		when(shmHandlingUnitSubDAO.findByTrackingProNumber(trackingProNumber, entityManager))
				.thenReturn(null);

		NotFoundException e = Assertions.assertThrows(NotFoundException.class, () ->
        updateHandlingUnitDimensionsImpl.updateHandlingUnitDimensions(rqst, new Double(1), new Double(1),
            ShipmentVolumeTypeCd.DENSITY_SHPMT, ShipmentVolumeMethodCd.SVC_CENTER_PRFL,
            trackingProNumber, Optional.empty(), txnContext, entityManager)
		);

		Assertions.assertTrue(e.getMessage().contains("Shipment handling unit not found"));
	}

	@Test
	public void testUpdateHandlingUnitDimensions_ParentShipmentNotFound() throws Exception {
		String trackingProNumber = "02251000275";
		UpdateHandlingUnitDimensionsRqst rqst = new UpdateHandlingUnitDimensionsRqst();
		rqst.setHeightNbr(50);
		rqst.setLengthNbr(25);
		rqst.setWidthNbr(40);
		rqst.setRequestingSicCd("UPO");

		ShmHandlingUnit shmHandlingUnit = getShipmentHandlingUnit(trackingProNumber,
				txnContext);
		when(shmShipmentSubDAO.findByIdOrProNumber(anyString(), anyLong(), any()))
				.thenReturn(null);
		when(shmHandlingUnitSubDAO.findByTrackingProNumber(trackingProNumber, entityManager))
				.thenReturn(shmHandlingUnit);

		NotFoundException e = Assertions.assertThrows(NotFoundException.class, () ->
				updateHandlingUnitDimensionsImpl
            .updateHandlingUnitDimensions(rqst, new Double(1), new Double(1), ShipmentVolumeTypeCd.DENSITY_SHPMT,
                        ShipmentVolumeMethodCd.SVC_CENTER_PRFL, trackingProNumber, Optional.empty(),
								txnContext, entityManager));

		Assertions.assertTrue(e.getMessage().contains("Shipment not found"));

		verify(shmShipmentSubDAO, times(0)).save(any(), any());
        verify(shmShipmentSubDAO, times(0))
            .updateDB2ShmShipmentDimensionCaptureInfo(any(Long.class), any(Optional.class), any(Optional.class),
                any(Optional.class), any(Optional.class), any(Optional.class), any(String.class), any(Timestamp.class),
                any(String.class), eq(db2EntityManager));

		verify(shmHandlingUnitSubDAO, times(0)).save(any(), any());
		verify(shmHandlingUnitSubDAO, times(0)).updateDB2ShmHandlingUnit(any(ShmHandlingUnit.class),
				any(Timestamp.class), eq(txnContext), eq(db2EntityManager));
	}

	@Test
	public void testUpdateHandlingUnitDimensions_ValidHandlingUnitUpdate() throws Exception {
		String trackingProNumber = "02251000275";
		String parentProNumber = "06470122980";
		UpdateHandlingUnitDimensionsRqst rqst = new UpdateHandlingUnitDimensionsRqst();
        double height = 50, width = 40, length = 25;
        String requestingSicCd = "UPO";
        String userId = "LKIM";
        String dimensionTye = "ROM";
        rqst.setHeightNbr(height);
        rqst.setLengthNbr(length);
        rqst.setWidthNbr(width);
        rqst.setRequestingSicCd(requestingSicCd);
        rqst.setCapturedByUserId(userId);
        rqst.setDimensionTypeCd(dimensionTye);
        Double totalVolumeCubicFeet = new Double(1);
        Double pupVolPct = new Double(1);
        ShmLnhDimension dimension = getShmLnhDimension();

		ShmHandlingUnit shmHandlingUnit = getShipmentHandlingUnit(trackingProNumber, txnContext);
		ShmShipment shmShipment = getShmShipment(parentProNumber);
		when(shmShipmentSubDAO.findByIdOrProNumber(anyString(), anyLong(), any()))
				.thenReturn(shmShipment);
		when(shmShipmentSubDAO.save(any(), any())).thenReturn(shmShipment);
		when(shmHandlingUnitSubDAO.findByTrackingProNumber(trackingProNumber, entityManager))
				.thenReturn(shmHandlingUnit);
		when(shmHandlingUnitSubDAO.findByParentProNumber(anyString(), any()))
				.thenReturn(Collections.singletonList(shmHandlingUnit));
        when(shmLnhDimensionSubDAO.findById(any(), any())).thenReturn(dimension);
        when(shmHandlingUnitDelegate.updateShmHandlingUnitDimensions(eq(shmHandlingUnit), eq(userId), eq(requestingSicCd), eq(height), eq(width), eq(length), eq(totalVolumeCubicFeet),
            eq(pupVolPct), eq(dimensionTye), any(AuditInfo.class), eq(txnContext), eq(entityManager))).thenReturn(shmHandlingUnit);
        when(shmEventDelegate.createEvent(anyLong(), any(), any(), any(), any(), anyString(), any(), anyString(), eq(db2EntityManager), any(AuditInfo.class))).thenReturn(1L);
        
        updateHandlingUnitDimensionsImpl.updateHandlingUnitDimensions(rqst, totalVolumeCubicFeet, new Double(1),
            ShipmentVolumeTypeCd.DENSITY_SHPMT, ShipmentVolumeMethodCd.SVC_CENTER_PRFL, trackingProNumber,
            Optional.empty(),
				txnContext, entityManager);

		verify(shmShipmentSubDAO, times(1)).save(any(), any());
		verify(shmShipmentSubDAO, times(1)).updateDB2ShmShipment(any(ShmShipment.class),
				any(Timestamp.class), eq(txnContext), eq(db2EntityManager));

        verify(shmHandlingUnitDelegate, times(1)).updateShmHandlingUnitDimensions(eq(shmHandlingUnit), eq(userId), eq(requestingSicCd), eq(height), eq(width), eq(length), eq(totalVolumeCubicFeet),
            eq(pupVolPct), eq(dimensionTye), any(AuditInfo.class), eq(txnContext), eq(entityManager));

        verify(shmLnhDimensionDelegate, times(1)).updateShmLnhDimension(eq(dimension), eq(BasicTransformer.toBigDecimal(height)), eq(BasicTransformer.toBigDecimal(width)),
            eq(BasicTransformer.toBigDecimal(length)), eq(userId), eq(dimensionTye), any(AuditInfo.class),
            eq(entityManager), eq(txnContext));

	}

	@Test
	public void testUpdateHandlingUnitDimensions_ValidShipmentUpdate() throws Exception {
		String parentProNumber = "06470122980";
		UpdateHandlingUnitDimensionsRqst rqst = new UpdateHandlingUnitDimensionsRqst();
		rqst.setHeightNbr(50);
		rqst.setLengthNbr(25);
		rqst.setWidthNbr(40);
		rqst.setRequestingSicCd("UPO");
		ShmShipment shmShipment = getShmShipment(parentProNumber);
		when(shmShipmentSubDAO.findByIdOrProNumber(anyString(), anyLong(), any()))
				.thenReturn(shmShipment);
		when(shmShipmentSubDAO.save(any(), any())).thenReturn(shmShipment);

        updateHandlingUnitDimensionsImpl.updateHandlingUnitDimensions(rqst, new Double(1), new Double(1),
            ShipmentVolumeTypeCd.DENSITY_SHPMT, ShipmentVolumeMethodCd.SVC_CENTER_PRFL, parentProNumber,
            Optional.empty(),
				txnContext, entityManager);

		verify(shmShipmentSubDAO, times(1)).save(any(), any());
		verify(shmShipmentSubDAO, times(1)).updateDB2ShmShipment(any(ShmShipment.class),
				any(Timestamp.class), eq(txnContext), eq(db2EntityManager));
		verify(shmHandlingUnitSubDAO, times(0)).save(any(), any());
		verify(shmHandlingUnitSubDAO, times(0)).updateDB2ShmHandlingUnit(any(ShmHandlingUnit.class),
				any(Timestamp.class), eq(txnContext), eq(db2EntityManager));
	}

	@Ignore
    @Test
	public void testUpdateHandlingUnitDimensions_RequestingSicNotHandlingUnitSic() {
		String trackingProNumber = "4541210282";
		String parentProNumber = "06470122980";
		UpdateHandlingUnitDimensionsRqst rqst = new UpdateHandlingUnitDimensionsRqst();
		rqst.setHeightNbr(50);
		rqst.setLengthNbr(25);
		rqst.setWidthNbr(40);
		rqst.setRequestingSicCd("LDA");

		ShmShipment shmShipment = getShmShipment(parentProNumber);
		ShmHandlingUnit shmHandlingUnit = getShipmentHandlingUnit(trackingProNumber, txnContext);
		when(shmHandlingUnitSubDAO.findByTrackingProNumber(anyString(), any()))
				.thenReturn(shmHandlingUnit);
		when(shmShipmentSubDAO.findByIdOrProNumber(anyString(), anyLong(), any()))
				.thenReturn(shmShipment);

        ValidationException e = Assertions.assertThrows(ValidationException.class, () ->
				updateHandlingUnitDimensionsImpl
            .updateHandlingUnitDimensions(rqst, new Double(1), new Double(1), ShipmentVolumeTypeCd.DENSITY_SHPMT,
                        ShipmentVolumeMethodCd.SVC_CENTER_PRFL, trackingProNumber, Optional.empty(),
								txnContext, entityManager)
		);

		Assertions.assertTrue(e.getMessage()
				.contains("Requesting SIC LDA not the same as shipment SIC UPO"),
				"Error was: " + e.getMessage());
	}

    @Ignore
	@Test
	public void testUpdateHandlingUnitDimensions_RequestingSicNotParentSic() {
		String parentProNumber = "06470122980";
		UpdateHandlingUnitDimensionsRqst rqst = new UpdateHandlingUnitDimensionsRqst();
		rqst.setHeightNbr(50);
		rqst.setLengthNbr(25);
		rqst.setWidthNbr(40);
		rqst.setRequestingSicCd("LDA");
		ShmShipment shmShipment = getShmShipment(parentProNumber);

		when(shmShipmentSubDAO.findByIdOrProNumber(anyString(), anyLong(), any()))
				.thenReturn(shmShipment);

		ValidationException e = Assertions.assertThrows(ValidationException.class, () ->
        updateHandlingUnitDimensionsImpl.updateHandlingUnitDimensions(rqst, new Double(1), new Double(1),
            ShipmentVolumeTypeCd.DENSITY_SHPMT, ShipmentVolumeMethodCd.SVC_CENTER_PRFL, parentProNumber,
            Optional.empty(),
					txnContext, entityManager)
		);

		Assertions.assertTrue(e.getMessage().contains("Requesting SIC LDA not the same as shipment SIC UPO"));

	}

	private ShmHandlingUnit getShipmentHandlingUnit(String trackingProNumber,
			TransactionContext txnContext) {
		AuditInfo auditInfo = new AuditInfo();
		auditInfo.setUpdatedTimestamp(txnContext.getTransactionTimestamp());
		ShmHandlingUnit shmHandlingUnit = new ShmHandlingUnit();
		shmHandlingUnit.setId(new ShmHandlingUnitPK());
		shmHandlingUnit.getId().setSeqNbr(1);
		shmHandlingUnit.getId().setShpInstId(12345L);
		shmHandlingUnit.setChildProNbrTxt(trackingProNumber);
		shmHandlingUnit.setCurrentSicCd("UPO");
		DtoTransformer.setAuditInfo(shmHandlingUnit, auditInfo);
		return shmHandlingUnit;
	}

	private ShmShipment getShmShipment(String proNumber) {
		ShmShipment shmShipment = new ShmShipment();
		shmShipment.setProNbrTxt(proNumber);
		shmShipment.setCurrSicCd("UPO");
		shmShipment.setHandlingUnitExemptionInd("Y");
		shmShipment.setPupVolPct(new BigDecimal(10));
		shmShipment.setShpInstId(1234L);
		return shmShipment;
	}

	private ShmLnhDimension getShmLnhDimension() {
		ShmLnhDimension shmLnhDimension = new ShmLnhDimension();

		return shmLnhDimension;
	}
}
