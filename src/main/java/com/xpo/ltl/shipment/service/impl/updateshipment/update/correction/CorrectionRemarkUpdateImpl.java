package com.xpo.ltl.shipment.service.impl.updateshipment.update.correction;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.Remark;
import com.xpo.ltl.api.shipment.v2.ShipmentRemarkTypeCd;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.RemarkAllTxUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractRemark;
import org.apache.commons.collections4.CollectionUtils;

import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RequestScoped
public class CorrectionRemarkUpdateImpl extends AbstractRemark implements RemarkAllTxUpdate {

	@Override
	public void update(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmRemark> shmRemarkList,
			String transactionCd) throws ValidationException {
		this.updateRemarks(entityManager, db2EntityManager, transactionContext, shmRemarkList, transactionCd);
	}

	@Override
	public void insert(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmRemark> shmRemarkListToAdd,
			String transactionCd) throws ValidationException {
		this.addRemarks(entityManager, db2EntityManager, transactionContext, shmRemarkListToAdd, transactionCd);
	}

	@Override
	public CompletableFuture<Map<ActionCd, List<ShmRemark>>> cfGetRemarksForTransactions(
			List<Remark> shipmentRemarks,
			List<ShmRemark> shmRemarks,
			String userId,
			List<ShipmentRemarkTypeCd> shipmentRemarkTypeCds,
			ShmShipment shmShipment,
			String transactionCd,
			TransactionContext transactionContext) {
		return CompletableFuture.supplyAsync(() -> this.getRemarksForTransactions(
				shipmentRemarks,
				shmRemarks,
				userId,
				shipmentRemarkTypeCds,
				shmShipment,
				transactionCd,
				transactionContext));
	}

	@Override
	public Map<ActionCd, List<ShmRemark>> getRemarksForTransactions(
			List<Remark> shipmentRemarks,
			List<ShmRemark> shmRemarks,
			String userId,
			List<ShipmentRemarkTypeCd> shipmentRemarkTypeCds,
			ShmShipment shmShipment,
			String transactionCd,
			TransactionContext transactionContext) {
		Map<ActionCd, List<ShmRemark>> result = new HashMap<>();

		if (CollectionUtils.isNotEmpty(shipmentRemarks)) {
			result.put(
					ActionCd.UPDATE,
					getUpdateRemark(shipmentRemarks, shmRemarks, userId, transactionCd, shipmentRemarkTypeCds));
			result.put(ActionCd.ADD, getAddRemarks(
					shipmentRemarks,
					shmRemarks,
					shipmentRemarkTypeCds,
					shmShipment,
					userId,
					transactionContext,
					transactionCd));
			result.put(ActionCd.DELETE, getDeleteRemarks(shipmentRemarks, shmRemarks, shipmentRemarkTypeCds));
		} else {
			result.put(ActionCd.DELETE, getDeleteRemarks(shipmentRemarks, shmRemarks, shipmentRemarkTypeCds));
		}

		return result;
	}

	@Override
	public void delete(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmRemark> shmRemarkList) throws ValidationException {

		this.deleteRemarks(entityManager, db2EntityManager, transactionContext, shmRemarkList);

	}
}
