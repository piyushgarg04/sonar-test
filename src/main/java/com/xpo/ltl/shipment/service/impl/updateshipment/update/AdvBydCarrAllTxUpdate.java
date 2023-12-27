package com.xpo.ltl.shipment.service.impl.updateshipment.update;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmAdvBydCarr;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface AdvBydCarrAllTxUpdate extends AdvBydCarrUpdate {

	List<ShmAdvBydCarr> getShmAdvBydCarrsToInsert(
			UpdateShipmentRqst updateShipmentRqst,
			List<ShmAdvBydCarr> shmAdvBydCarrs,
			ShmShipment shmShipment,
			String transactionCd,
			TransactionContext transactionContext);

	CompletableFuture<List<ShmAdvBydCarr>> cfGetShmAdvBydCarrsToInsert(
			UpdateShipmentRqst updateShipmentRqst,
			List<ShmAdvBydCarr> shmAdvBydCarrs,
			ShmShipment shmShipment,
			String transactionCd,
			TransactionContext transactionContext);

	List<ShmAdvBydCarr> getShmAdvBydCarrsToDelete(
			UpdateShipmentRqst updateShipmentRqst, String userId, List<ShmAdvBydCarr> shmAdvBydCarrs);

	CompletableFuture<List<ShmAdvBydCarr>> cfGetShmAdvBydCarrsToDelete(
			UpdateShipmentRqst updateShipmentRqst, String userId, List<ShmAdvBydCarr> shmAdvBydCarrs);

	void insert(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAdvBydCarr> shmAdvBydCarrs,
			String transactionCd) throws ValidationException;

	void delete(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAdvBydCarr> shmAdvBydCarrs) throws ValidationException;

	Map<ActionCd, List<ShmAdvBydCarr>> getAllTx(
			ShmShipment shipment,
			UpdateShipmentRqst updateShipmentRqst,
			String userId,
			List<ShmAdvBydCarr> shmAdvBydCarrs,
			String transactionCd,
			TransactionContext transactionContext);

	CompletableFuture<Map<ActionCd, List<ShmAdvBydCarr>>> cfGetAllTx(
			ShmShipment shipment,
			UpdateShipmentRqst updateShipmentRqst,
			String userId,
			List<ShmAdvBydCarr> shmAdvBydCarrs,
			String transactionCd,
			TransactionContext transactionContext);
}
