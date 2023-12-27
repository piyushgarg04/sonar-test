package com.xpo.ltl.shipment.service.impl.updateshipment.update;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCust;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.AsMatchedParty;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface AsMatchedPartyUpdate {

	void update(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAsEntdCust> shmAsEntdCusts,
			String transactionCd) throws ValidationException;

	void insert(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAsEntdCust> shmAsEntdCustsToAdd,
			String transactionCd) throws ValidationException;

	void delete(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAsEntdCust> shmAsEntdCustsToDelete) throws ValidationException;

	CompletableFuture<Map<ActionCd, List<ShmAsEntdCust>>> cfGetAsMatchedPartiesForTransactions(
			List<AsMatchedParty> asMatchedParties,
			List<ShmAsEntdCust> shmAsEntdCusts,
			String userId,
			ShmShipment shmShipment,
			String transactionCd,
			TransactionContext transactionContext);

	Map<ActionCd, List<ShmAsEntdCust>> getAsMatchedPartiesForTransactions(
			List<AsMatchedParty> asMatchedParties,
			List<ShmAsEntdCust> shmAsEntdCusts,
			String userId,
			ShmShipment shmShipment,
			String transactionCd,
			TransactionContext transactionContext);
}
