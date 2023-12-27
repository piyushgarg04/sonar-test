package com.xpo.ltl.shipment.service.impl.updateshipment.update;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.service.entity.ShmTmDtCritical;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.TimeDateCritical;

import javax.persistence.EntityManager;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface TimeDateCriticalUpdate {
	void update(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmTmDtCritical> shmTmDtCriticals,
			String transactionCd) throws ValidationException;
	public void delete(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmTmDtCritical> shmTmDtCriticalsToAdd) throws ValidationException;
	void insert(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmTmDtCritical> shmTmDtCriticals,
			String transactionCd) throws ValidationException;

	CompletableFuture<Map<ActionCd, List<ShmTmDtCritical>>> cfGetTimeDateCriticalsForTransactions(
			TimeDateCritical timeDateCritical,
			ShmTmDtCritical shmTmDtCritical,
			String userId,
			ShmShipment shmShipment,
			String transactionCd,
			TransactionContext transactionContext);

	Map<ActionCd, List<ShmTmDtCritical>> getTimeDateCriticalsForTransactions(
			TimeDateCritical timeDateCritical,
			ShmTmDtCritical shmTmDtCritical,
			String userId,
			ShmShipment shmShipment,
			String transactionCd,
			TransactionContext transactionContext) throws ParseException;
}
