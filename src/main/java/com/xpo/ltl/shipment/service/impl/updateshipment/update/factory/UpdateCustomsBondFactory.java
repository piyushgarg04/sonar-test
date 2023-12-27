package com.xpo.ltl.shipment.service.impl.updateshipment.update.factory;

import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.CustomsBondUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.correction.CorrectionCustomsBondUpdateImpl;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class UpdateCustomsBondFactory {

	@Inject
	private CorrectionCustomsBondUpdateImpl correctionCustomsBondUpdate;

	public CustomsBondUpdate getUpdateImplementation(ShipmentUpdateActionCd command) {
		if (command.equals(ShipmentUpdateActionCd.CORRECTION)) {
			return correctionCustomsBondUpdate;
		} else if (command.equals(ShipmentUpdateActionCd.AUTO_RATE)) {
			return correctionCustomsBondUpdate;
		} else {
			throw new IllegalArgumentException("Not implemetation for " + command);
		}
	}
}
