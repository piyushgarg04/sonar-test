package com.xpo.ltl.shipment.service.impl.updateshipment.update;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.AccessorialService;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface AcSvcUpdate {

	Map<ActionCd, List<ShmAcSvc>> getItemsForTransactions(
			Long shipmentInstId,
			List<AccessorialService> accessorialServiceList,
			List<ShmAcSvc> shmAcSvcsOriginal,
			ShmShipment shipment,
			String userId,
			ShipmentUpdateActionCd shipmentUpdateActionCd,
			String transactionCode,
			TransactionContext transactionContext) throws ServiceException;

	CompletableFuture<Map<ActionCd, List<ShmAcSvc>>> cfGetItemsForTransactions(
			Long shipmentInstId,
			List<AccessorialService> accessorialServiceList,
			List<ShmAcSvc> shmAcSvcsOriginal,
			ShmShipment shipment,
			String userId,
			ShipmentUpdateActionCd shipmentUpdateActionCd,
			String transactionCode,
			TransactionContext transactionContext) throws ServiceException;

	void insert(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAcSvc> accessorialServiceToAddList,
			String transactionCd,
			String userId) throws ValidationException;

	void update(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAcSvc> accessorialServiceToUpdateList,
			String transactionCd,
			String userId) throws ValidationException;

	void delete(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAcSvc> accessorialServiceToDeleteList);

}
