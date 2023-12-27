package com.xpo.ltl.shipment.service.impl.updateshipment.update.factory;

import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.CommodityTransactions;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.common.CommodityCommonImpl;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class UpdateCommodityFactory {

	@Inject
	private CommodityCommonImpl  commodityCommon;

	public CommodityTransactions getUpdateImplementation(ShipmentUpdateActionCd command) {
		if (command.equals(ShipmentUpdateActionCd.CORRECTION)
				|| command.equals(ShipmentUpdateActionCd.AUTO_RATE)) {
			return commodityCommon;
		} else {
			throw new IllegalArgumentException("Not implemetation for " + command);
		}
	}
}
