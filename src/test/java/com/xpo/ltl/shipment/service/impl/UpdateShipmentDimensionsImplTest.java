package com.xpo.ltl.shipment.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.ShipmentDimension;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentDimensionsRqst;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.validators.UpdateHandlingUnitDimensionsValidator;

public class UpdateShipmentDimensionsImplTest {

    private static final String PRO_NBR = "011011111";

    @Mock
	private TransactionContext txnContext;

	@Mock
	private EntityManager entityManager;

    @Mock
    private EntityManager db2EntityManager;

    @Mock
    private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

	@Spy
	private UpdateHandlingUnitDimensionsValidator validator;

	@InjectMocks
    private UpdateShipmentDimensionsImpl updateShipmentDimensionsImpl;

    private AuditInfo auditInfo;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

        auditInfo = new AuditInfo();
        AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);
	}

	@Test
    public void testUpdateShimentHandlingUnitDimensionWhenExemption_SameQty() throws ServiceException {

        List<ShmHandlingUnit> findByParentResultMock = findByParentResult();
        when(shmHandlingUnitSubDAO.findByParentProNumber(eq(PRO_NBR), eq(entityManager)))
            .thenReturn(findByParentResultMock);

        UpdateShipmentDimensionsRqst updateShipmentDimensionsRqst = buildUpdateShmDimensionRequest(3);

        updateShipmentDimensionsImpl
            .updateShimentHandlingUnitDimensionWhenExemption(updateShipmentDimensionsRqst, getShmShipment(PRO_NBR),
                PRO_NBR,
                auditInfo, entityManager, txnContext);

        verify(shmHandlingUnitSubDAO, times(2))
            .updateDB2ShmHandlingUnit(any(ShmHandlingUnit.class), any(Timestamp.class), eq(txnContext),
                eq(db2EntityManager));

        // asserts
        assertEquals(2, findByParentResultMock.size());
        for (int i = 0; i < findByParentResultMock.size(); i++) {
            ShipmentDimension shipmentDimension = updateShipmentDimensionsRqst.getDimensions().get(i);
            assertEquals(shipmentDimension.getLength(), findByParentResultMock.get(i).getLengthNbr());
            assertEquals(shipmentDimension.getWidth(), findByParentResultMock.get(i).getWidthNbr());
            assertEquals(shipmentDimension.getHeight(), findByParentResultMock.get(i).getHeightNbr());
            assertEquals(BigDecimal.ONE, findByParentResultMock.get(i).getWgtLbs());
        }
	}

    @Test
    public void testUpdateShimentHandlingUnitDimensionWhenExemption_HUSizeGreaterThanDimensions()
            throws ServiceException {


        List<ShmHandlingUnit> findByParentResultMock = findByParentResult();
        when(shmHandlingUnitSubDAO.findByParentProNumber(eq(PRO_NBR), eq(entityManager)))
            .thenReturn(findByParentResultMock);

        UpdateShipmentDimensionsRqst updateShipmentDimensionsRqst = buildUpdateShmDimensionRequest(1);


        updateShipmentDimensionsImpl
            .updateShimentHandlingUnitDimensionWhenExemption(updateShipmentDimensionsRqst, getShmShipment(PRO_NBR),
                PRO_NBR,
                auditInfo, entityManager, txnContext);

        verify(shmHandlingUnitSubDAO, times(1))
            .updateDB2ShmHandlingUnit(any(ShmHandlingUnit.class), any(Timestamp.class), eq(txnContext),
                eq(db2EntityManager));

        // asserts
        assertEquals(2, findByParentResultMock.size());
        // 1st modified.
        ShipmentDimension shipmentDimension = updateShipmentDimensionsRqst.getDimensions().get(0);
        assertEquals(shipmentDimension.getLength(), findByParentResultMock.get(0).getLengthNbr());
        assertEquals(shipmentDimension.getWidth(), findByParentResultMock.get(0).getWidthNbr());
        assertEquals(shipmentDimension.getHeight(), findByParentResultMock.get(0).getHeightNbr());
        assertEquals(BigDecimal.ONE, findByParentResultMock.get(0).getWgtLbs());
        // 2nd keep the original.
        assertEquals(BigDecimal.valueOf(20.1), findByParentResultMock.get(1).getLengthNbr());
        assertEquals(BigDecimal.valueOf(21.1), findByParentResultMock.get(1).getWidthNbr());
        assertEquals(BigDecimal.valueOf(22.1), findByParentResultMock.get(1).getHeightNbr());
    }

    @Test
    public void testUpdateShimentHandlingUnitDimensionWhenExemption_HUSizeLessThanDimensions() throws ServiceException {

        List<ShmHandlingUnit> findByParentResultMock = findByParentResult();
        when(shmHandlingUnitSubDAO.findByParentProNumber(eq(PRO_NBR), eq(entityManager)))
            .thenReturn(findByParentResultMock);

        UpdateShipmentDimensionsRqst updateShipmentDimensionsRqst = buildUpdateShmDimensionRequest(3);

        updateShipmentDimensionsImpl
            .updateShimentHandlingUnitDimensionWhenExemption(updateShipmentDimensionsRqst, getShmShipment(PRO_NBR),
                PRO_NBR, auditInfo, entityManager, txnContext);

        verify(shmHandlingUnitSubDAO, times(2))
            .updateDB2ShmHandlingUnit(any(ShmHandlingUnit.class), any(Timestamp.class), eq(txnContext),
                eq(db2EntityManager));

        assertEquals(2, findByParentResultMock.size());
        // update 2 and ignore the 3rd one.
        for (int i = 0; i < findByParentResultMock.size(); i++) {
            ShipmentDimension shipmentDimension = updateShipmentDimensionsRqst.getDimensions().get(i);
            assertEquals(shipmentDimension.getLength(), findByParentResultMock.get(i).getLengthNbr());
            assertEquals(shipmentDimension.getWidth(), findByParentResultMock.get(i).getWidthNbr());
            assertEquals(shipmentDimension.getHeight(), findByParentResultMock.get(i).getHeightNbr());
            assertEquals(BigDecimal.ONE, findByParentResultMock.get(i).getWgtLbs());
        }
    }

	private ShmShipment getShmShipment(String proNumber) {
		ShmShipment shmShipment = new ShmShipment();
		shmShipment.setProNbrTxt(proNumber);
		shmShipment.setCurrSicCd("UPO");
		shmShipment.setHandlingUnitExemptionInd("Y");

		return shmShipment;
	}

    private List<ShmHandlingUnit> findByParentResult(){
	    ShmHandlingUnit shm1 = new ShmHandlingUnit();
        shm1.setLengthNbr(BigDecimal.valueOf(10.1));
        shm1.setWidthNbr(BigDecimal.valueOf(11.1));
        shm1.setHeightNbr(BigDecimal.valueOf(12.1));

	    ShmHandlingUnit shm2 = new ShmHandlingUnit();
        shm2.setLengthNbr(BigDecimal.valueOf(20.1));
        shm2.setWidthNbr(BigDecimal.valueOf(21.1));
        shm2.setHeightNbr(BigDecimal.valueOf(22.1));

        return Arrays.asList(shm1, shm2);
	}

    private UpdateShipmentDimensionsRqst buildUpdateShmDimensionRequest(int size) {
        UpdateShipmentDimensionsRqst updateShipmentDimensionsRqst = new UpdateShipmentDimensionsRqst();
        List<ShipmentDimension> shmDimList = new ArrayList<ShipmentDimension>();
        if (size >= 1) {
            ShipmentDimension dim1 = new ShipmentDimension();
            dim1.setLength(BigDecimal.valueOf(100.1));
            dim1.setWidth(BigDecimal.valueOf(110.1));
            dim1.setHeight(BigDecimal.valueOf(120.1));
            shmDimList.add(dim1);
        }
        if (size >= 2) {
            ShipmentDimension dim2 = new ShipmentDimension();
            dim2.setLength(BigDecimal.valueOf(200.1));
            dim2.setWidth(BigDecimal.valueOf(210.1));
            dim2.setHeight(BigDecimal.valueOf(220.1));
            shmDimList.add(dim2);
        }
        if (size >= 3) {
            ShipmentDimension dim3 = new ShipmentDimension();
            dim3.setLength(BigDecimal.valueOf(300.1));
            dim3.setHeight(BigDecimal.valueOf(310.1));
            dim3.setWidth(BigDecimal.valueOf(320.1));
            shmDimList.add(dim3);
        }

        updateShipmentDimensionsRqst.setDimensions(shmDimList);
        return updateShipmentDimensionsRqst;
    }

}
