package com.xpo.ltl.shipment.service.impl.updateshipment.functional;

import java.util.List;

@FunctionalInterface
public interface LoadValuesToUpdate<T, K> {

	List<K> load(
			final List<T> shmMiscLineItemsToInsert,
			final List<K> shmMiscLineItemsUpdated,
			boolean fromDelete,
			String racfId,
			String tranCode);
}
