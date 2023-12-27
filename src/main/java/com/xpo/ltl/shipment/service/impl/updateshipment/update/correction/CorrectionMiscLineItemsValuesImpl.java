package com.xpo.ltl.shipment.service.impl.updateshipment.update.correction;

import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem;
import com.xpo.ltl.api.shipment.v2.MiscLineItem;
import com.xpo.ltl.api.shipment.v2.MiscLineItemCd;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.LoadValues;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractMiscLineItems;

import java.math.BigDecimal;
import java.util.Objects;

public class CorrectionMiscLineItemsValuesImpl extends AbstractMiscLineItems implements LoadValues<MiscLineItem, ShmMiscLineItem> {

	@Override
	public void loadtValues(MiscLineItem miscLineItem, ShmMiscLineItem shmMiscLineItem) {

		if (Objects.nonNull(miscLineItem.getAmount())) {
			shmMiscLineItem.setAmt(BigDecimal.valueOf(miscLineItem.getAmount()));
		}

		if (!shmMiscLineItem.getLnTypCd().equals(getMiscLineItemCd(MiscLineItemCd.DISC_LN.value()))) {
			shmMiscLineItem.setDescTxt(miscLineItem.getDescription());
		}
		if (shmMiscLineItem.getLnTypCd().equals(getMiscLineItemCd(MiscLineItemCd.AS_WGT.value()))
				|| shmMiscLineItem.getLnTypCd().equals(getMiscLineItemCd(MiscLineItemCd.DEFICIT_WGT.value()))) {

			if (Objects.nonNull(miscLineItem.getTariffsRate())) {
				shmMiscLineItem.setTrfRt(BigDecimal.valueOf(miscLineItem.getTariffsRate()));
			}
			if (Objects.nonNull(miscLineItem.getQuantity())) {
				shmMiscLineItem.setQty(BigDecimal.valueOf(miscLineItem.getQuantity()));
			}

		}
		if (Objects.nonNull(miscLineItem.getChrgToCd())) {
			shmMiscLineItem.setChrgToCd(getChargeToCdAlt(miscLineItem.getChrgToCd().value()));
		}
		if (shmMiscLineItem.getLnTypCd().equals(getMiscLineItemCd(MiscLineItemCd.DISC_LN.value())) && (Objects.nonNull(
				miscLineItem.getPrepaidPercentage()))) {
			shmMiscLineItem.setPpdPct(BigDecimal.valueOf(miscLineItem.getPrepaidPercentage()));

		}
		if (Objects.nonNull(miscLineItem.getLineTypeCd())) {
			shmMiscLineItem.setLnTypCd(getMiscLineItemCdAlt(miscLineItem.getLineTypeCd().value()));
		}

	}
}
