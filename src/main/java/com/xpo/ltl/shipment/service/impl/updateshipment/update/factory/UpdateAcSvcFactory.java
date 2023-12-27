package com.xpo.ltl.shipment.service.impl.updateshipment.update.factory;

import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.AcSvcUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.autorate.AutoRateAcSvcUpdateImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.common.AcSvcUpdateCommonImpl;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class UpdateAcSvcFactory {

	@Inject
	private AcSvcUpdateCommonImpl acSvcUpdateCommon;
	@Inject
	private AutoRateAcSvcUpdateImpl autoRateAcSvcUpdate;

	public AcSvcUpdate getUpdateImplementation(ShipmentUpdateActionCd command) {
		if (command.equals(ShipmentUpdateActionCd.CORRECTION)
				|| command.equals(ShipmentUpdateActionCd.MANUAL_RATE)) {
			return acSvcUpdateCommon;
		} else if (command.equals(ShipmentUpdateActionCd.AUTO_RATE)) {
			return autoRateAcSvcUpdate;
		} else {
			throw new IllegalArgumentException("Not implemetation for " + command);
		}

	}
}
