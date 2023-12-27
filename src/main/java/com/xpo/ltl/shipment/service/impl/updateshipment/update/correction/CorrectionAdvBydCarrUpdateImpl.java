package com.xpo.ltl.shipment.service.impl.updateshipment.update.correction;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmAdvBydCarr;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.AdvanceBeyondCarrier;
import com.xpo.ltl.api.shipment.v2.AdvanceBeyondTypeCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.AdvBydCarrAllTxUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractAdvBydCarr;
import org.apache.commons.collections4.CollectionUtils;

import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@RequestScoped
public class CorrectionAdvBydCarrUpdateImpl extends AbstractAdvBydCarr implements AdvBydCarrAllTxUpdate {

	private static void setValues(AdvanceBeyondCarrier advanceBeyondCarrier, ShmAdvBydCarr shmAdvBydCarr) {
		if (Objects.nonNull(advanceBeyondCarrier.getChargeAmount())) {
			shmAdvBydCarr.setChgAmt(BigDecimal.valueOf(advanceBeyondCarrier.getChargeAmount()));
		}

		if (Objects.nonNull(advanceBeyondCarrier.getCarrierScacCd())) {
			shmAdvBydCarr.setCarrScacCd(advanceBeyondCarrier.getCarrierScacCd());
		}

		if (advanceBeyondCarrier.getTypeCd().equals(AdvanceBeyondTypeCd.ADV_CARR)) {
			if (Objects.nonNull(advanceBeyondCarrier.getCarrierProNbr())) {
				shmAdvBydCarr.setCarrProNbrTxt(advanceBeyondCarrier.getCarrierProNbr());
			}
			if (Objects.nonNull(advanceBeyondCarrier.getCarrierPickupDate())) {
				shmAdvBydCarr.setCarrPkupDt(BasicTransformer.toDate(advanceBeyondCarrier.getCarrierPickupDate()));
			}
		}
	}

	@Override
	public List<ShmAdvBydCarr> getShmAdvBydCarrsToInsert(
			UpdateShipmentRqst updateShipmentRqst,
			List<ShmAdvBydCarr> shmAdvBydCarrs,
			ShmShipment shmShipment,
			String transactionCd,
			TransactionContext transactionContext) {

		return this.getAbcShmAdvBydCarrsToInsert(updateShipmentRqst,
				shmAdvBydCarrs,
				shmShipment,
				transactionCd,
				transactionContext);

	}

	@Override
	public CompletableFuture<List<ShmAdvBydCarr>> cfGetShmAdvBydCarrsToInsert(
			UpdateShipmentRqst updateShipmentRqst,
			List<ShmAdvBydCarr> shmAdvBydCarrs,
			ShmShipment shmShipment,
			String transactionCd,
			TransactionContext transactionContext) {
		return CompletableFuture.supplyAsync(() -> getShmAdvBydCarrsToInsert(updateShipmentRqst,
				shmAdvBydCarrs,
				shmShipment,
				transactionCd,
				transactionContext));
	}

	@Override
	public List<ShmAdvBydCarr> getShmAdvBydCarrsToDelete(
			UpdateShipmentRqst updateShipmentRqst, String userId, List<ShmAdvBydCarr> shmAdvBydCarrs) {

		return this.getAbcShmAdvBydCarrsToDelete(updateShipmentRqst, shmAdvBydCarrs);

	}

	@Override
	public CompletableFuture<List<ShmAdvBydCarr>> cfGetShmAdvBydCarrsToDelete(
			UpdateShipmentRqst updateShipmentRqst, String userId, List<ShmAdvBydCarr> shmAdvBydCarrs) {
		return CompletableFuture.supplyAsync(() -> getShmAdvBydCarrsToDelete(updateShipmentRqst,
				userId,
				shmAdvBydCarrs));
	}

	@Override
	public void insert(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAdvBydCarr> shmAdvBydCarrs,
			String transactionCd) throws ValidationException {
		this.insertAdvBydCarr(entityManager, db2EntityManager, transactionContext, shmAdvBydCarrs, transactionCd);

	}

	@Override
	public void delete(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAdvBydCarr> shmAdvBydCarrs) throws ValidationException {
		this.deleteAdvBydCarr(entityManager, db2EntityManager, transactionContext, shmAdvBydCarrs);

	}

	@Override
	public Map<ActionCd, List<ShmAdvBydCarr>> getAllTx(
			ShmShipment shipment,
			UpdateShipmentRqst updateShipmentRqst,
			String userId,
			List<ShmAdvBydCarr> shmAdvBydCarrs,
			String transactionCd,
			TransactionContext transactionContext) {
		Map<ActionCd, List<ShmAdvBydCarr>> result = new HashMap<>();

		List<ShmAdvBydCarr> shmAdvBydCarrsToInsert = getShmAdvBydCarrsToInsert(updateShipmentRqst,
				shmAdvBydCarrs,
				shipment,
				transactionCd,
				transactionContext);
		List<ShmAdvBydCarr> shmAdvBydCarrsToDelete = getShmAdvBydCarrsToDelete(updateShipmentRqst,
				userId,
				shmAdvBydCarrs);
		List<ShmAdvBydCarr> shmAdvBydCarrsToUpdate = getShmAdvBydCarrsToUpdate(updateShipmentRqst,
				userId,
				shmAdvBydCarrs,
				transactionCd);
		List<ShmAdvBydCarr> notDeleted = new ArrayList<>();
		List<ShmAdvBydCarr> notDeletedUpdated = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(shmAdvBydCarrsToDelete)) {
			if (CollectionUtils.isNotEmpty(shmAdvBydCarrsToDelete) && CollectionUtils.isNotEmpty(shmAdvBydCarrs)) {
				shmAdvBydCarrs.forEach(shmAdvBydCarr -> {

					Optional<ShmAdvBydCarr> optionalShmAdvBydCarr = shmAdvBydCarrsToDelete
							.stream()
							.filter(shmAdvBydCarrToDelete ->
									shmAdvBydCarrToDelete.getId().getShpInstId() == shmAdvBydCarr.getId().getShpInstId()
											&& shmAdvBydCarrToDelete.getId().getSeqNbr() == shmAdvBydCarr
											.getId()
											.getSeqNbr())
							.findAny();
					if (!optionalShmAdvBydCarr.isPresent()) {
						notDeleted.add(shmAdvBydCarr);
					}

				});
				if (CollectionUtils.isNotEmpty(shmAdvBydCarrsToUpdate)) {
					List<ShmAdvBydCarr> diff = notDeleted
							.stream()
							.filter(shmAdvBydCarr1 -> shmAdvBydCarrsToUpdate
									.stream()
									.noneMatch(shmAdvBydCarr2 -> shmAdvBydCarr1.getId().getShpInstId() == shmAdvBydCarr2
											.getId()
											.getShpInstId() && shmAdvBydCarr1.getId().getSeqNbr() == shmAdvBydCarr2
											.getId()
											.getSeqNbr()))
							.collect(Collectors.toList());
					notDeletedUpdated.addAll(shmAdvBydCarrsToUpdate);
					notDeletedUpdated.addAll(diff);

				} else {
					notDeletedUpdated.addAll(notDeleted);
				}

				notDeletedUpdated.addAll(shmAdvBydCarrsToInsert);

				result.put(ActionCd.DELETE, shmAdvBydCarrs);
				result.put(ActionCd.ADD, resetSeqNumberAdvBydCarr(shipment.getShpInstId(), notDeletedUpdated));

			}
		} else {
			Optional<ShmAdvBydCarr> optionalShmAdvBydCarr = shmAdvBydCarrs
					.stream()
					.max(Comparator.comparingLong(o -> o.getId().getSeqNbr()));
			AtomicReference<Long> seqNumber = new AtomicReference<>(optionalShmAdvBydCarr
					.map(shmAdvBydCarr -> shmAdvBydCarr.getId().getSeqNbr())
					.orElse(0L));
			shmAdvBydCarrsToInsert.forEach(shmAdvBydCarr -> {
				seqNumber.getAndSet(seqNumber.get() + 1);
				shmAdvBydCarr.getId().setSeqNbr(seqNumber.get());
			});
			result.put(ActionCd.ADD, shmAdvBydCarrsToInsert);
			result.put(ActionCd.UPDATE, shmAdvBydCarrsToUpdate);
		}

		return result;
	}

	@Override
	public CompletableFuture<Map<ActionCd, List<ShmAdvBydCarr>>> cfGetAllTx(
			ShmShipment shipment,
			UpdateShipmentRqst updateShipmentRqst,
			String userId,
			List<ShmAdvBydCarr> shmAdvBydCarrs,
			String transactionCd,
			TransactionContext transactionContext) {
		return CompletableFuture.supplyAsync(() -> getAllTx(shipment,
				updateShipmentRqst,
				userId,
				shmAdvBydCarrs,
				transactionCd,
				transactionContext));
	}

	/*
	* For TYP_CD 1 (Advance), deletion and insertion are allowed. For updates, the following fields can be updated:

		CARR_SCAC_CD
		CARR_PRO_NBR_TXT
		CHG_AMT

		For TYP_CD 2 (Beyond), deletion and insertion are allowed. For updates, the following fields can be updated:

		CARR_SCAC_CD
		CHG_AMT
		CARR_PKUP_DT
	* */
	@Override
	public List<ShmAdvBydCarr> getShmAdvBydCarrsToUpdate(
			UpdateShipmentRqst updateShipmentRqst,
			String userId,
			List<ShmAdvBydCarr> shmAdvBydCarrs,
			String transactionCd) {
		List<ShmAdvBydCarr> result = new ArrayList<>();
		updateShipmentRqst.getAdvanceBeyondCarriers().forEach(advanceBeyondCarrier -> {

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

				setValues(advanceBeyondCarrier, shmAdvBydCarr);

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
}
