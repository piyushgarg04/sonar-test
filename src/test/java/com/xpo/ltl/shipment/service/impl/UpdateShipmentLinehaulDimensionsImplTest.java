package com.xpo.ltl.shipment.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.customer.v1.GetShipperProfileFreightCubeResp;
import com.xpo.ltl.api.customer.v1.GetShipperProfileFreightCubeRqst;
import com.xpo.ltl.api.dockoperations.v1.GetTrailerSpecificationResp;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCust;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.MatchedPartyTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.LnhDimension;
import com.xpo.ltl.api.shipment.v2.MatchedPartyTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentVolumeMethodCd;
import com.xpo.ltl.api.shipment.v2.ShipmentVolumeTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentWithDimension;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentLinehaulDimensionsRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShipmentAsEnteredCustomerDAO;
import com.xpo.ltl.shipment.service.dao.ShmLnhDimensionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmEventDelegate;

@RunWith(MockitoJUnitRunner.class)
public class UpdateShipmentLinehaulDimensionsImplTest {

    @Mock
    private TransactionContext txnContext;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ExternalRestClient restClient;

    @Mock
    private ShipmentAsEnteredCustomerDAO shipmentAsEnteredCustomerDAO;

    @Mock
    private ShmShipmentSubDAO shmShipmentSubDAO;

    @Mock
    private ShmEventDelegate shmEventDelegate;

    @Mock
    private ShmLnhDimensionSubDAO shmLnhDimensionSubDAO;

    @Mock
    private EntityManager db2EntityManager;

    @InjectMocks
    private UpdateShipmentLinehaulDimensionsImpl updateShipmentLinehaulDimensionsImpl;

    @Test
    public void updateShipmentLinehaulDimensionsTest() throws ServiceException {

        long shipmentInstanceId = 1L;
        String shipperZip6Txt = "90210";
        BigDecimal shipperCustomerNumber = BigDecimal.TEN;
        UpdateShipmentLinehaulDimensionsRqst updateShipmentLinehaulDimensionsRqst = new UpdateShipmentLinehaulDimensionsRqst();

        ShipmentWithDimension shipmentWithDimension = new ShipmentWithDimension();
        shipmentWithDimension.setShipmentInstId(shipmentInstanceId);

        LnhDimension dimension = new LnhDimension();
        dimension.setShipmentInstId(shipmentInstanceId);
        dimension.setPiecesCount(1L);
        dimension.setWidthNbr(13D);
        dimension.setLengthNbr(23D);
        dimension.setHeightNbr(43D);

        shipmentWithDimension.setLinehaulDimensions(Lists.newArrayList(dimension));

        updateShipmentLinehaulDimensionsRqst.setShipmentWithDimensions(Arrays.asList(shipmentWithDimension));

        ShmShipment shmShipment = new ShmShipment();
        doReturn(shmShipment)
            .when(shmShipmentSubDAO)
            .findByProOrShipmentId(any(),
                                   any(),
                                   eq(shipmentInstanceId),
                                   eq(Boolean.FALSE),
                                   any(),
                                   eq(entityManager));

        GetTrailerSpecificationResp trailerSpecification = new GetTrailerSpecificationResp();
        doReturn(trailerSpecification).when(restClient).getTrailerSpecification(eq("T"), eq("TTC28"),
            eq(null), eq(txnContext));

        ShmAsEntdCust shipper = new ShmAsEntdCust();
        shipper.setZip6Txt(shipperZip6Txt);
        shipper.setCisCustNbr(shipperCustomerNumber);
        doReturn(shipper).when(shipmentAsEnteredCustomerDAO).findByShipmentIdAndTypeCd(eq(shipmentInstanceId),
            eq(MatchedPartyTypeCdTransformer.toCode(MatchedPartyTypeCd.SHPR)), eq(entityManager));

        GetShipperProfileFreightCubeResp shipperProfileFreightCube = new GetShipperProfileFreightCubeResp();
        doReturn(shipperProfileFreightCube).when(restClient).getShipperProfileFreightCube(
            any(GetShipperProfileFreightCubeRqst.class), eq(BasicTransformer.toLong(shipperCustomerNumber)),
            eq(txnContext));

        doNothing().when(shmShipmentSubDAO).persist(eq(shmShipment), eq(entityManager));
        doReturn(1)
            .when(shmShipmentSubDAO)
            .updateDB2ShmShipmentDimensionCaptureInfo(any(Long.class), any(Optional.class), any(Optional.class),
                any(Optional.class), any(Optional.class), any(Optional.class), any(String.class), any(Timestamp.class),
                any(String.class), eq(db2EntityManager));

        Double pupVolumePercentage = 1D;
        Double totalVolumeCubicFeet = 1D;
        ShipmentVolumeMethodCd shipmentVolumeMethodCd = ShipmentVolumeMethodCd.ACTL_DIM;
        ShipmentVolumeTypeCd shipmentVolumeTypeCd = ShipmentVolumeTypeCd.DENSITY_SHPMT;

        updateShipmentLinehaulDimensionsImpl.updateShipmentLinehaulDimensions(updateShipmentLinehaulDimensionsRqst,
            pupVolumePercentage, totalVolumeCubicFeet, shipmentVolumeMethodCd, shipmentVolumeTypeCd, false,
            entityManager, txnContext);

        verify(shmShipmentSubDAO)
            .findByProOrShipmentId(any(),
                                   any(),
                                   eq(shipmentInstanceId),
                                   eq(Boolean.FALSE),
                                   any(),
                                   eq(entityManager));

        verify(shmShipmentSubDAO).persist(eq(shmShipment), eq(entityManager));
        verify(shmShipmentSubDAO)
            .updateDB2ShmShipmentDimensionCaptureInfo(any(Long.class), any(Optional.class), any(Optional.class),
                any(Optional.class), any(Optional.class), any(Optional.class), any(String.class), any(Timestamp.class),
                any(String.class), eq(db2EntityManager));

        verifyNoMoreInteractions(restClient);
        verifyNoMoreInteractions(shipmentAsEnteredCustomerDAO);
        verifyNoMoreInteractions(shmShipmentSubDAO);

    }

    @Test
    public void updateShipmentLinehaulDimensionsValidationTest() throws ServiceException, ParseException {

        UpdateShipmentLinehaulDimensionsRqst updateShipmentLinehaulDimensionsRqst = new UpdateShipmentLinehaulDimensionsRqst();

        ShipmentWithDimension shipmentWithDimension = new ShipmentWithDimension();
        shipmentWithDimension.setShipmentInstId(null);
        LnhDimension dimension = new LnhDimension();
        dimension.setShipmentInstId(null);
        shipmentWithDimension.setLinehaulDimensions(Lists.newArrayList(dimension));
        updateShipmentLinehaulDimensionsRqst.setShipmentWithDimensions(Lists.newArrayList(shipmentWithDimension));

        Double pupVolumePercentage = 1D;
        Double totalVolumeCubicFeet = 1D;
        ShipmentVolumeMethodCd shipmentVolumeMethodCd = ShipmentVolumeMethodCd.ACTL_DIM;
        ShipmentVolumeTypeCd shipmentVolumeTypeCd = ShipmentVolumeTypeCd.DENSITY_SHPMT;

        assertThrows(ValidationException.class, () -> {
            updateShipmentLinehaulDimensionsImpl.updateShipmentLinehaulDimensions(updateShipmentLinehaulDimensionsRqst,
                pupVolumePercentage, totalVolumeCubicFeet, shipmentVolumeMethodCd, shipmentVolumeTypeCd, false,
                entityManager, txnContext);
        });

        verifyNoMoreInteractions(shmShipmentSubDAO);

    }

}
