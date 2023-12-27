package com.xpo.ltl.shipment.service.impl.updateshipment.update;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.MiscLineItem;
import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface MiscLineItemsUpdate {

	Map<ActionCd, List<ShmMiscLineItem>> getItemsForTransactions(
			Long shipmentInstId,
			List<MiscLineItem> miscLineItems,
			List<ShmMiscLineItem> shmMiscLineItemsOriginal,
			ShmShipment shipment,
			String userId,
			TransactionContext transactionContext,
			ShipmentUpdateActionCd shipmentUpdateActionCd) throws ServiceException;

	CompletableFuture<Map<ActionCd, List<ShmMiscLineItem>>> cfGetItemsForTransactions(
			Long shipmentInstId,
			List<MiscLineItem> miscLineItems,
			List<ShmMiscLineItem> shmMiscLineItemsOriginal,
			ShmShipment shipment,
			String userId,
			TransactionContext transactionContext,
			ShipmentUpdateActionCd shipmentUpdateActionCd) throws ServiceException;

	void update(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmMiscLineItem> shmMiscLineItemListToUpdate,
			String transactionCd,
			String userId) throws ValidationException;

	void delete(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmMiscLineItem> shmMiscLineItemListToDelete);

	void insert(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmMiscLineItem> shmMiscLineItemListToAdd,
			String transactionCd,
			String userId) throws ValidationException;

}
