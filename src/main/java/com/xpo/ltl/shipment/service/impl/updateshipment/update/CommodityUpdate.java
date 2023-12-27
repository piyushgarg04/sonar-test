package com.xpo.ltl.shipment.service.impl.updateshipment.update;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmCommodity;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CommodityUpdate {

	List<ShmCommodity> getShmCommoditiesToUpdate(
			UpdateShipmentRqst updateShipmentRqst, String userId, List<ShmCommodity> commodities, String transactionCd);

	CompletableFuture<List<ShmCommodity>> getShmCommoditiesToUpdateCf(
			UpdateShipmentRqst updateShipmentRqst, String userId, List<ShmCommodity> commodities, String transactionCd);

	void update(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmCommodity> commodities,
			String transactionCd,
			String userId) throws ValidationException;

}
