package com.xpo.ltl.shipment.service.impl.updateshipment.update.factory;

import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.autorate.AutoRateLoadValuesFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.correction.CorrectionsLoadValuesFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate.ManRateLoadValuesFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class LoadValFactory {

	@Inject
	private ManRateLoadValuesFactory manRateLoadValuesFactory;

	@Inject
	private CorrectionsLoadValuesFactory correctionsLoadValuesFactory;

	@Inject
	private AutoRateLoadValuesFactory autoRateLoadValuesFactory;

	public LoadValuesFactory getFactoryImplementation(ShipmentUpdateActionCd command) {
		if (command.equals(ShipmentUpdateActionCd.MANUAL_RATE)) {
			return manRateLoadValuesFactory;
		} else if (command.equals(ShipmentUpdateActionCd.CORRECTION)) {
			return correctionsLoadValuesFactory;
		}else if (command.equals(ShipmentUpdateActionCd.AUTO_RATE)) {
			return autoRateLoadValuesFactory;
		} else {
			throw new IllegalArgumentException("Not implemetation for " + command);
		}
	}
}
