package com.xpo.ltl.shipment.service.impl.updateshipment.update.factory;

import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.MiscLineItemsUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.common.MiscLineItemsUpdateCommonImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate.MiscLineItemsUpdateManRateImpl;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class UpdateMiscLineItemFactory {

	@Inject
	private MiscLineItemsUpdateManRateImpl miscLineItemsUpdateManRate;

	@Inject
	private MiscLineItemsUpdateCommonImpl miscLineItemsUpdateCommon;

	public MiscLineItemsUpdate getUpdateImplementation(ShipmentUpdateActionCd shipmentUpdateActionCd) {
		if (ShipmentUpdateActionCd.MANUAL_RATE.equals(shipmentUpdateActionCd)) {
			return miscLineItemsUpdateManRate;
		} else if (ShipmentUpdateActionCd.CORRECTION.equals(shipmentUpdateActionCd)
				|| ShipmentUpdateActionCd.AUTO_RATE.equals(shipmentUpdateActionCd)) {
			return miscLineItemsUpdateCommon;
		} else {
			throw new IllegalArgumentException("Not implementation for " + shipmentUpdateActionCd);
		}
	}
}
