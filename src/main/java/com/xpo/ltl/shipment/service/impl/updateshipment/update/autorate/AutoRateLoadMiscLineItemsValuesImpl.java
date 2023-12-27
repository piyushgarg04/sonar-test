package com.xpo.ltl.shipment.service.impl.updateshipment.update.autorate;

import com.xpo.ltl.api.shipment.service.entity.ShmCommodity;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem;
import com.xpo.ltl.api.shipment.v2.Commodity;
import com.xpo.ltl.api.shipment.v2.MiscLineItem;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.LoadValues;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractCommodity;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractMiscLineItems;

import java.math.BigDecimal;
import java.util.Objects;

public class AutoRateLoadMiscLineItemsValuesImpl extends AbstractMiscLineItems implements LoadValues<MiscLineItem, ShmMiscLineItem> {

	@Override
	public void loadtValues(MiscLineItem miscLineItem, ShmMiscLineItem shmMiscLineItem) {

		if (Objects.nonNull(miscLineItem.getTariffsRate())){
			shmMiscLineItem.setTrfRt(BigDecimal.valueOf(miscLineItem.getTariffsRate()) );
		}

	}
}
