package com.xpo.ltl.shipment.service.impl.updateshipment.update.factory;

import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.AdvBydCarrUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.correction.CorrectionAdvBydCarrUpdateImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate.ManRateAdvBydCarrUpdateImpl;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class UpdateAdvBydCarrFactory {

	@Inject
	private ManRateAdvBydCarrUpdateImpl manRateAdvBydCarrUpdateImpl;

	@Inject
	private CorrectionAdvBydCarrUpdateImpl correctionAdvBydCarrUpdateImpl;

	public AdvBydCarrUpdate getUpdateImplementation(ShipmentUpdateActionCd command) {
		if (command.equals(ShipmentUpdateActionCd.MANUAL_RATE)) {
			return manRateAdvBydCarrUpdateImpl;
		} else if (command.equals(ShipmentUpdateActionCd.CORRECTION)
				|| command.equals(ShipmentUpdateActionCd.AUTO_RATE)) {
			return correctionAdvBydCarrUpdateImpl;
		} else {
			throw new IllegalArgumentException("Not implemetation for " + command);
		}
	}
}
