package com.xpo.ltl.shipment.service.impl.updateshipment.functional;

import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItemPK;
import com.xpo.ltl.api.shipment.v2.MiscLineItemCd;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractMiscLineItems;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class LoadValuesToUpdateAutoRateMiscImpl extends AbstractMiscLineItems implements LoadValuesToUpdate<ShmMiscLineItem, ShmMiscLineItem> {

	private static void setValues(ShmMiscLineItem shmMiscLineItem, ShmMiscLineItem miscLineItemFound) {

		if (shmMiscLineItem.getLnTypCd().equals(getMiscLineItemCd(MiscLineItemCd.AS_WGT.value()))
				|| shmMiscLineItem.getLnTypCd().equals(getMiscLineItemCd(MiscLineItemCd.DEFICIT_WGT.value()))
				) {
			shmMiscLineItem.setTrfRt(miscLineItemFound.getTrfRt());

		}

	}

	@Override
	public List<ShmMiscLineItem> load(
			List<ShmMiscLineItem> shmMiscLineItemsToInsert,
			List<ShmMiscLineItem> shmMiscLineItemsUpdated,
			boolean fromDelete,
			String racfId, String tranCode) {

		List<ShmMiscLineItem> result = new ArrayList<>();

		shmMiscLineItemsToInsert.forEach(shmMiscLineItem -> {
			Optional<ShmMiscLineItem> optionalShmMiscLineItem = shmMiscLineItemsUpdated
					.stream()
					.filter(shmMiscLineItemToUpdate ->
							shmMiscLineItemToUpdate.getId().getSeqNbr() == shmMiscLineItem.getId().getSeqNbr()
									&& shmMiscLineItemToUpdate.getId().getShpInstId() == shmMiscLineItem
									.getId()
									.getShpInstId())
					.findAny();
			ShmMiscLineItem shmMiscLineItemToCheck = new ShmMiscLineItem();

			copyFields(shmMiscLineItem, shmMiscLineItemToCheck);
			if (optionalShmMiscLineItem.isPresent() && !fromDelete) {
				ShmMiscLineItem miscLineItemFound = optionalShmMiscLineItem.get();

				setValues(shmMiscLineItem, miscLineItemFound);

				List<String> diff = this.compareShmMiscLineItem(shmMiscLineItem, shmMiscLineItemToCheck);

				if (CollectionUtils.isNotEmpty(diff)) {
					shmMiscLineItem.setLstUpdtTmst(new Timestamp(new Date().getTime()));
					shmMiscLineItem.setLstUpdtTranCd(tranCode);
					shmMiscLineItem.setLstUpdtUid(racfId);
					result.add(shmMiscLineItem);
				}
			} else {
				ShmMiscLineItemPK id = new ShmMiscLineItemPK();
				id.setSeqNbr(shmMiscLineItem.getId().getSeqNbr());
				id.setShpInstId(shmMiscLineItem.getId().getShpInstId());
				shmMiscLineItemToCheck.setId(id);
				result.add(shmMiscLineItemToCheck);
			}

		});
		return result;
	}
}
