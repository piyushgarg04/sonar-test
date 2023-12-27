package com.xpo.ltl.shipment.service.impl.updateshipment.update.autorate;

import com.xpo.ltl.api.shipment.v2.Shipment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutoRateLoadShipmentValuesImplTest {

	@Test
	void loadtValues() {

		AutoRateLoadShipmentValuesImpl autoRateLoadShipmentValues = new AutoRateLoadShipmentValuesImpl();
		Shipment shipment = new Shipment();

		shipment.setAbsoluteMinimumChargeInd(true);
//		shipment.setCurrencyConversionFctr();
//		shipment.setDiscountCd();
//		shipment.setDiscountPercentage();
//		shipment.setInvoicingCurrencyCd();
//		shipment.setPriceAgreementId();
//		shipment.setPriceRulesetNbr();
//		shipment.setRatingCurrencyCd();
//		shipment.setShipperToConsigneeMiles();
//		shipment.setTotalChargeAmount();
//		shipment.setTotalUsdAmount();
//		shipment.setTotalWeightLbs();
//		shipment.setWarrantyInd();
//		shipment.setRatingTariffsId();

	}
}