package com.xpo.ltl.shipment.service.impl.updateshipment.update;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmCustomsBond;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface CustomsBondUpdate {

	Map<ActionCd, List<ShmCustomsBond>> getCustomsBondForTransactions(
			Long shipmentInstId,
			UpdateShipmentRqst updateShipmentRqst,
			List<ShmCustomsBond> shmCustomsBondOriginal,
			ShmShipment shipment,
			String transactionCode,
			TransactionContext transactionContext) throws ServiceException;

	CompletableFuture<Map<ActionCd, List<ShmCustomsBond>>> cfGetCustomsBondForTransactions(
			Long shipmentInstId,
			UpdateShipmentRqst updateShipmentRqst,
			List<ShmCustomsBond> shmCustomsBondOriginal,
			ShmShipment shipment,
			String transactionCode,
			TransactionContext transactionContext) throws ServiceException;

	void insert(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmCustomsBond> shmCustomsBondToAddList,
			String transactionCd) throws ValidationException;

	void update(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmCustomsBond> shmCustomsBondToUpdateList,
			String transactionCd) throws ValidationException;

	void delete(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmCustomsBond> shmCustomsBondToDeleteList);
}
