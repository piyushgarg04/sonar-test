package com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmAdvBydCarr;
import com.xpo.ltl.api.shipment.v2.AdvanceBeyondCarrier;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.AdvBydCarrUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractAdvBydCarr;
import org.apache.commons.collections4.CollectionUtils;

import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RequestScoped
public class ManRateAdvBydCarrUpdateImpl extends AbstractAdvBydCarr implements AdvBydCarrUpdate {
	@Override
	public List<ShmAdvBydCarr> getShmAdvBydCarrsToUpdate(
			UpdateShipmentRqst updateShipmentRqst,
			String userId,
			List<ShmAdvBydCarr> shmAdvBydCarrs,
			String transactionCd) {

		if (CollectionUtils.isNotEmpty(updateShipmentRqst.getAdvanceBeyondCarriers())) {

			shmAdvBydCarrs = loadAdvanceBeyondCarrier(updateShipmentRqst.getAdvanceBeyondCarriers(),
					shmAdvBydCarrs,
					userId,
					transactionCd);

		}
		return shmAdvBydCarrs;
	}

	@Override
	public CompletableFuture<List<ShmAdvBydCarr>> cfGetShmAdvBydCarrsToUpdate(
			UpdateShipmentRqst updateShipmentRqst,
			String userId,
			List<ShmAdvBydCarr> shmAdvBydCarrs,
			String transactionCd) {
		return CompletableFuture.supplyAsync(() -> getShmAdvBydCarrsToUpdate(updateShipmentRqst,
				userId,
				shmAdvBydCarrs,
				transactionCd));
	}

	@Override
	public void update(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAdvBydCarr> shmAdvBydCarrs,
			String transactionCd) throws ValidationException {
		this.updateShmAdvBydCarr(entityManager, db2EntityManager, transactionContext, shmAdvBydCarrs, transactionCd);

	}

	private List<ShmAdvBydCarr> loadAdvanceBeyondCarrier(
			List<AdvanceBeyondCarrier> advanceBeyondCarriers,
			List<ShmAdvBydCarr> shmAdvBydCarrs,
			String userId,
			String transactionCd) {
		List<ShmAdvBydCarr> result = new ArrayList<>();
		advanceBeyondCarriers.forEach(advanceBeyondCarrier -> {

			Optional<ShmAdvBydCarr> optionalShmAdvBydCarr = shmAdvBydCarrs
					.stream()
					.filter(shmAdvBydCarr -> shmAdvBydCarr
							.getTypCd()
							.equals(getAdvanceBeyondTypeCd(advanceBeyondCarrier.getTypeCd().value()))
							&& advanceBeyondCarrier.getShipmentInstId().compareTo(shmAdvBydCarr.getId().getShpInstId())
							== 0)
					.findAny();
			if (optionalShmAdvBydCarr.isPresent()) {

				ShmAdvBydCarr shmAdvBydCarrToCheck = new ShmAdvBydCarr();
				ShmAdvBydCarr shmAdvBydCarr = optionalShmAdvBydCarr.get();
				copyFields(shmAdvBydCarr, shmAdvBydCarrToCheck);
				shmAdvBydCarr.setChgAmt(BigDecimal.valueOf(advanceBeyondCarrier.getChargeAmount()));
				List<String> diff = this.compareShmAdvBydCarr(shmAdvBydCarr, shmAdvBydCarrToCheck);

				if (CollectionUtils.isNotEmpty(diff)) {
					shmAdvBydCarr.setLstUpdtTmst(new Timestamp(new Date().getTime()));
					shmAdvBydCarr.setLstUpdtTranCd(transactionCd);
					shmAdvBydCarr.setLstUpdtUid(userId);
					result.add(shmAdvBydCarr);
				}

			}
		});
		return result;
	}
}
