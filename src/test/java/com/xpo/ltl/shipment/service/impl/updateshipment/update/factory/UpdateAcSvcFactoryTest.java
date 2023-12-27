package com.xpo.ltl.shipment.service.impl.updateshipment.update.factory;

import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.AcSvcUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.common.AcSvcUpdateCommonImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class UpdateAcSvcFactoryTest {

	@InjectMocks
	private UpdateAcSvcFactory updateAcSvcFactory;

	@Mock
	private AcSvcUpdateCommonImpl mockAcSvcUpdateCommon = mock(AcSvcUpdateCommonImpl.class);

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);

	}

	@Test
	public void testGetUpdateImplementationForManualRate() {
		ShipmentUpdateActionCd command = ShipmentUpdateActionCd.MANUAL_RATE;

		AcSvcUpdate result = updateAcSvcFactory.getUpdateImplementation(command);

		assertEquals(mockAcSvcUpdateCommon, result);
	}

	@Test
	public void testGetUpdateImplementationForCorrection() {
		ShipmentUpdateActionCd command = ShipmentUpdateActionCd.CORRECTION;

		AcSvcUpdate result = updateAcSvcFactory.getUpdateImplementation(command);

		assertEquals(mockAcSvcUpdateCommon, result);
	}

}
