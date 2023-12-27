package com.xpo.ltl.shipment.service.impl.updateshipment.update.factory;

import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.RemarkUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.correction.CorrectionRemarkUpdateImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate.ManRateRemarkUpdateImpl;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class UpdateRemarkFactory {

	@Inject
	private ManRateRemarkUpdateImpl manRateRemarkUpdate;

	@Inject
	private CorrectionRemarkUpdateImpl correctionRemarkUpdate;

	public RemarkUpdate getUpdateImplementation(ShipmentUpdateActionCd command) {
		if (command.equals(ShipmentUpdateActionCd.MANUAL_RATE)) {
			return manRateRemarkUpdate;
		} else if (command.equals(ShipmentUpdateActionCd.CORRECTION)
				|| command.equals(ShipmentUpdateActionCd.AUTO_RATE)) {
			return correctionRemarkUpdate;
		} else {
			throw new IllegalArgumentException("Not implemetation for " + command);
		}
	}
}
