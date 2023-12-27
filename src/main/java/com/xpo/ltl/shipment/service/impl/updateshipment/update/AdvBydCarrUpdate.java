package com.xpo.ltl.shipment.service.impl.updateshipment.update;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmAdvBydCarr;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AdvBydCarrUpdate {

	List<ShmAdvBydCarr> getShmAdvBydCarrsToUpdate(
			UpdateShipmentRqst updateShipmentRqst,
			String userId,
			List<ShmAdvBydCarr> shmAdvBydCarrs,
			String transactionCd);

	CompletableFuture<List<ShmAdvBydCarr>> cfGetShmAdvBydCarrsToUpdate(
			UpdateShipmentRqst updateShipmentRqst,
			String userId,
			List<ShmAdvBydCarr> shmAdvBydCarrs,
			String transactionCd);

	void update(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAdvBydCarr> shmAdvBydCarrs,
			String transactionCd) throws ValidationException;
}
