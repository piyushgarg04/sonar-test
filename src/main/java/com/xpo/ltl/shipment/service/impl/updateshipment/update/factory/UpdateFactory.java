package com.xpo.ltl.shipment.service.impl.updateshipment.update.factory;

import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.UpdateShipment;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.autorate.UpdateAutoRateImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.correction.UpdateCorrectionImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate.UpdateManRateImpl;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class UpdateFactory {

	@Inject
	private UpdateManRateImpl updateManRate;

	@Inject
	private UpdateCorrectionImpl updateCorrection;

	@Inject
	private UpdateAutoRateImpl updateAutoRate;

	public UpdateShipment getUpdateImplementation(ShipmentUpdateActionCd shipmentUpdateActionCd) {
		if (ShipmentUpdateActionCd.MANUAL_RATE.equals(shipmentUpdateActionCd)) {
			return updateManRate;
		} else if (ShipmentUpdateActionCd.CORRECTION.equals(shipmentUpdateActionCd)) {
			return updateCorrection;
		} else if (ShipmentUpdateActionCd.AUTO_RATE.equals(shipmentUpdateActionCd)) {
			return updateAutoRate;
		} else {
			throw new IllegalArgumentException("Not implementation for " + shipmentUpdateActionCd);
		}
	}
}
