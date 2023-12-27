package com.xpo.ltl.shipment.service.impl.updateshipment.update.factory;

import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.RemarkUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.correction.CorrectionRemarkUpdateImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate.ManRateRemarkUpdateImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class UpdateRemarkFactoryTest {

	@Mock
	private ManRateRemarkUpdateImpl mockManRateRemarkUpdate = mock(ManRateRemarkUpdateImpl.class);

	@Mock
	private CorrectionRemarkUpdateImpl mockCorrectionRemarkUpdate = mock(CorrectionRemarkUpdateImpl.class);

	@InjectMocks
	private UpdateRemarkFactory remarkFactory;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);

	}

	@Test
	public void testGetUpdateImplementation() {
		ShipmentUpdateActionCd manRateCommand = ShipmentUpdateActionCd.MANUAL_RATE;
		ShipmentUpdateActionCd correctionCommand = ShipmentUpdateActionCd.CORRECTION;
		RemarkUpdate manRateRemarkUpdate = remarkFactory.getUpdateImplementation(manRateCommand);
		RemarkUpdate correctionRemarkUpdate = remarkFactory.getUpdateImplementation(correctionCommand);
		assertNotNull(manRateRemarkUpdate);
		assertNotNull(correctionRemarkUpdate);

	}
}