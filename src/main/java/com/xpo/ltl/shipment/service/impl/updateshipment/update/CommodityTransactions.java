package com.xpo.ltl.shipment.service.impl.updateshipment.update;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmCommodity;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.Commodity;
import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface CommodityTransactions {

	List<ShmCommodity> getCommoditiesToDelete(
			List<Commodity> commodities, List<ShmCommodity> shmCommodities) throws ServiceException;
	CompletableFuture<List<ShmCommodity>> getShmCommoditiesToUpdateCf(
			UpdateShipmentRqst updateShipmentRqst, String userId, List<ShmCommodity> commodities, String transactionCd,
			ShipmentUpdateActionCd shipmentUpdateActionCd);
	CompletableFuture<List<ShmCommodity>> cfGetCommoditiesToDelete(
			List<Commodity> commodities, List<ShmCommodity> shmCommodities) throws ServiceException;

	List<ShmCommodity> getCommoditiesToInsert(
			List<Commodity> commodities,
			List<ShmCommodity> shmCommodities,
			ShmShipment shipment) throws ServiceException;

	CompletableFuture<List<ShmCommodity>> cfGetCommoditiesToInsert(
			List<Commodity> commodities,
			List<ShmCommodity> shmCommodities,
			ShmShipment shipment,
			TransactionContext transactionContext) throws ServiceException;

	Map<ActionCd, List<ShmCommodity>> getAllTx(
			List<ShmCommodity> shmCommodities,
			ShmShipment shipment,
			UpdateShipmentRqst updateShipmentRqst,
			String userId,
			String transactionCd,
			ShipmentUpdateActionCd shipmentUpdateActionCd) throws ServiceException;

	CompletableFuture<Map<ActionCd, List<ShmCommodity>>> cfGetAllTx(
			List<ShmCommodity> shmCommodities,
			ShmShipment shipment,
			UpdateShipmentRqst updateShipmentRqst,
			String userId,
			String transactionCd,
			TransactionContext transactionContext,
			ShipmentUpdateActionCd shipmentUpdateActionCd) throws ServiceException;

	void delete(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmCommodity> commodities);

	void insert(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmCommodity> shmCommodities,
			String transactionCd,
			String userId) throws ValidationException;
	List<ShmCommodity> getShmCommoditiesToUpdate(
			UpdateShipmentRqst updateShipmentRqst, String userId, List<ShmCommodity> commodities, String transactionCd,
			ShipmentUpdateActionCd shipmentUpdateActionCd);
	void update(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmCommodity> commodities,
			String transactionCd,
			String userId) throws ValidationException;
}
