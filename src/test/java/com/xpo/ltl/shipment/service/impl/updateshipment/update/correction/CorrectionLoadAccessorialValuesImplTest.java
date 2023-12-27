package com.xpo.ltl.shipment.service.impl.updateshipment.update.correction;

import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.v2.AccessorialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CorrectionLoadAccessorialValuesImplTest {

	@Mock
	private AccessorialService mockAccessorialService;

	@Mock
	private ShmAcSvc mockShmAcSvc;

	@InjectMocks
	private CorrectionLoadAccessorialValuesImpl correctionLoadAccessorialValuesImpl;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testLoadShipmentValues() {
		// Mock behavior and interactions
		when(mockAccessorialService.getAmount()).thenReturn(100.0);
		when(mockAccessorialService.getTariffsRate()).thenReturn(10.0);
		when(mockAccessorialService.getDescription()).thenReturn("Test Description");

		// Call the method under test
		correctionLoadAccessorialValuesImpl.loadtValues(mockAccessorialService, mockShmAcSvc);

		// Verify the expected interactions
		verify(mockShmAcSvc).setAmt(BigDecimal.valueOf(100.0));
		verify(mockShmAcSvc).setTrfRt(BigDecimal.valueOf(10.0));
		verify(mockShmAcSvc).setDescTxt("Test Description");

	}

}