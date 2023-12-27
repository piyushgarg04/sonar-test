package com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmCommodity;
import com.xpo.ltl.api.shipment.v2.Commodity;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.CommodityUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractCommodity;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import org.apache.commons.collections4.CollectionUtils;

import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RequestScoped
public class ManRateCommodityUpdImpl extends AbstractCommodity implements CommodityUpdate {

	@Override
	public List<ShmCommodity> getShmCommoditiesToUpdate(
			UpdateShipmentRqst updateShipmentRqst,
			String userId,
			List<ShmCommodity> commodities,
			String transactionCd) {
		if (CollectionUtils.isNotEmpty(updateShipmentRqst.getCommodities())) {

			commodities = getModifiedCommodities(updateShipmentRqst.getCommodities(),
					commodities,
					userId,
					transactionCd);
		}
		return commodities;
	}

	@Override
	public CompletableFuture<List<ShmCommodity>> getShmCommoditiesToUpdateCf(
			UpdateShipmentRqst updateShipmentRqst,
			String userId,
			List<ShmCommodity> commodities,
			String transactionCd) {
		return CompletableFuture.supplyAsync(() -> getShmCommoditiesToUpdate(updateShipmentRqst,
				userId,
				commodities,
				transactionCd));
	}

	@Override
	public void update(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmCommodity> commodities,
			String transactionCd,
			String userId) throws ValidationException {
		this.updateCommodities(entityManager, db2EntityManager, transactionContext, commodities, transactionCd, userId);
	}

	@LogExecutionTime
	public List<ShmCommodity> getModifiedCommodities(
			List<Commodity> commodities, List<ShmCommodity> shmCommodities, String userId, String transactionCd) {
		List<ShmCommodity> result = new ArrayList<>();
		commodities.forEach(commodity -> {

			Optional<ShmCommodity> shmCommodityOptional = shmCommodities
					.stream()
					.filter(shmCommodity -> shmCommodity.getId().getSeqNbr() == commodity.getSequenceNbr().longValue()
							&& shmCommodity.getId().getShpInstId() == commodity.getShipmentInstId())
					.findAny();

			if (shmCommodityOptional.isPresent()) {

				ShmCommodity shmCommodity = shmCommodityOptional.get();
				ShmCommodity shmCommodityToCheck = new ShmCommodity();

				copyFields(shmCommodity, shmCommodityToCheck);

				if (Objects.nonNull(commodity.getAmount())) {
					shmCommodity.setAmt(BigDecimal.valueOf(commodity.getAmount()));
				}

				if (Objects.nonNull(commodity.getTariffsRate())) {
					shmCommodity.setTrfRt(BigDecimal.valueOf(commodity.getTariffsRate()));
				}

				List<String> diff = this.compareCommodity(shmCommodity, shmCommodityToCheck);

				if (CollectionUtils.isNotEmpty(diff)) {
					shmCommodity.setLstUpdtTranCd(transactionCd);
					shmCommodity.setLstUpdtUid(userId);
					shmCommodity.setLstUpdtTmst(new Timestamp(new Date().getTime()));
					result.add(shmCommodity);
				}
			}
		});

		return result;
	}
}
