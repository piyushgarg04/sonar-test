package com.xpo.ltl.shipment.service.impl.updateshipment.update;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.Remark;
import com.xpo.ltl.api.shipment.v2.ShipmentRemarkTypeCd;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface RemarkUpdate {
	void update(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmRemark> shmRemarkList,
			String transactionCd) throws ValidationException;

	void insert(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmRemark> shmRemarkListToAdd,
			String transactionCd) throws ValidationException;

	CompletableFuture<Map<ActionCd, List<ShmRemark>>> cfGetRemarksForTransactions(
			List<Remark> shipmentRemarks,
			List<ShmRemark> shmRemarks,
			String userId,
			List<ShipmentRemarkTypeCd> shipmentRemarkTypeCds,
			ShmShipment shmShipment,
			String transactionCd,
			TransactionContext transactionContext);

	Map<ActionCd, List<ShmRemark>> getRemarksForTransactions(
			List<Remark> shipmentRemarks,
			List<ShmRemark> shmRemarks,
			String userId,
			List<ShipmentRemarkTypeCd> shipmentRemarkTypeCds,
			ShmShipment shmShipment,
			String transactionCd,
			TransactionContext transactionContext);

}
