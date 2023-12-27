package com.xpo.ltl.shipment.service.impl.updateshipment.update.abc;

import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.BillStatusCd;
import com.xpo.ltl.api.shipment.v2.DeliveryQualifierCd;
import com.xpo.ltl.api.shipment.v2.InvoiceCurrencyCd;
import com.xpo.ltl.api.shipment.v2.RatingCurrencyCd;
import com.xpo.ltl.api.shipment.v2.WarrantyStatusCd;
import com.xpo.ltl.shipment.service.impl.updateshipment.MockParent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AbstractShipmentTest extends MockParent {
	@InjectMocks
	private AbstractShipment abstractShipment = Mockito.spy(new AbstractShipment() {
	});

	AbstractShipmentTest() throws InstantiationException, IllegalAccessException {
	}

	@BeforeEach
	void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testSetDefaultShipmentValues() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		ShmShipment shmShipment = new ShmShipment();

		Method setDefaultShipmentValuesMethod = AbstractShipment.class.getDeclaredMethod("setDefaultShipmentValues",
				ShmShipment.class);
		setDefaultShipmentValuesMethod.setAccessible(true);
		setDefaultShipmentValuesMethod.invoke(abstractShipment, shmShipment);
		assertNotNull(shmShipment.getAbsMinChgInd());

	}

	@Test
	public void testGetShipmentDiscCdFromDiscPct() {
		BigDecimal discPct = new BigDecimal("15.75");
		String result = abstractShipment.getShipmentDiscCdFromDiscPct(discPct);
		assertEquals("157", result);

		discPct = new BigDecimal("5.5");
		result = abstractShipment.getShipmentDiscCdFromDiscPct(discPct);
		assertEquals("55", result);
	}

	@Test
	public void testGetDeliveryQualifierCd() {
		String result = abstractShipment.getDeliveryQualifierCd(DeliveryQualifierCd.ALL_SHORT.value());
		assertEquals("J", result);
	}

	@Test
	public void testGetInvoiceCurrencyCd() {

		String result = abstractShipment.getInvoiceCurrencyCd(InvoiceCurrencyCd.CANADIAN_DOLLAR.value());
		assertEquals("CAD", result);
	}

	@Test
	public void testGetRatingCurrencyCd() {

		String result = abstractShipment.getRatingCurrencyCd(RatingCurrencyCd.US_DOLLAR.value());
		assertEquals("USD", result);
	}

	@Test
	public void testGetBillStatusCd() {

		String result = AbstractShipment.getBillStatusCd(BillStatusCd.IN_FBES_SUSPENSE_QUEUE.value());
		assertEquals("2", result);
	}

	@Test
	void getWarrantyStatusCd() {
		String result = AbstractShipment.getWarrantyStatusCd(WarrantyStatusCd.DENIED.value());
		assertEquals("3", result);
	}
}