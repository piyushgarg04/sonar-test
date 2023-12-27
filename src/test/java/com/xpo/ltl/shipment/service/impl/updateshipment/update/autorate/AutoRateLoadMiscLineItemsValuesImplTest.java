package com.xpo.ltl.shipment.service.impl.updateshipment.update.autorate;

import com.xpo.ltl.api.shipment.v2.MiscLineItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutoRateLoadMiscLineItemsValuesImplTest {

	@Test
	void loadtValues() {
		AutoRateLoadMiscLineItemsValuesImpl autoRateLoadMiscLineItemsValues = new AutoRateLoadMiscLineItemsValuesImpl();
		MiscLineItem miscLineItem = new MiscLineItem();
		miscLineItem.setTariffsRate(20.6);

	}
}