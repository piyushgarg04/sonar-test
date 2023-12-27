package com.xpo.ltl.shipment.service.impl.updateshipment.update.correction;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCust;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.AsMatchedParty;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.AsMatchedPartyUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractAsMatchedParty;
import org.apache.commons.collections4.CollectionUtils;

import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@RequestScoped
public class CorrectionAsMatchedPartyUpdateImpl extends AbstractAsMatchedParty implements AsMatchedPartyUpdate {
	@Override
	public void update(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAsEntdCust> shmAsEntdCusts,
			String transactionCd) throws ValidationException {

		this.updateAsMatchedParties(entityManager, db2EntityManager, transactionContext, shmAsEntdCusts, transactionCd);

	}

	@Override
	public void insert(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAsEntdCust> shmAsEntdCustsToAdd,
			String transactionCd) throws ValidationException {

		this.insertAsMatchedParties(entityManager,
				db2EntityManager,
				transactionContext,
				shmAsEntdCustsToAdd,
				transactionCd);
	}

	@Override
	public void delete(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAsEntdCust> shmAsEntdCustsToDelete) throws ValidationException {

		this.deleteAsMatchedParties(entityManager, db2EntityManager, transactionContext, shmAsEntdCustsToDelete);

	}

	@Override
	public CompletableFuture<Map<ActionCd, List<ShmAsEntdCust>>> cfGetAsMatchedPartiesForTransactions(
			List<AsMatchedParty> asMatchedParties,
			List<ShmAsEntdCust> shmAsEntdCusts,
			String userId,
			ShmShipment shmShipment,
			String transactionCd,
			TransactionContext transactionContext) {
		return CompletableFuture.supplyAsync(() -> getAsMatchedPartiesForTransactions(asMatchedParties,
				shmAsEntdCusts,
				userId,
				shmShipment,
				transactionCd,
				transactionContext));
	}

	@Override
	public Map<ActionCd, List<ShmAsEntdCust>> getAsMatchedPartiesForTransactions(
			List<AsMatchedParty> asMatchedParties,
			List<ShmAsEntdCust> shmAsEntdCustsOriginal,
			String userId,
			ShmShipment shmShipment,
			String transactionCd,
			TransactionContext transactionContext) {
		Map<ActionCd, List<ShmAsEntdCust>> result = new HashMap<>();

		List<ShmAsEntdCust> shmAsEntdCustsForDelete = this.getAbcAsMatchedPartiesForDelete(asMatchedParties,
				shmAsEntdCustsOriginal);
		List<ShmAsEntdCust> shmAsEntdCustsForInsert = this.getAbcAsMatchedPartiesForInsert(shmShipment.getShpInstId(),
				asMatchedParties,
				shmAsEntdCustsOriginal,
				transactionContext);
		List<ShmAsEntdCust> shmAsEntdCustsForUpdate = this.getAbcAsMatchedPartiesForUpdate(asMatchedParties,
				shmAsEntdCustsOriginal);

		List<ShmAsEntdCust> notDeleted = new ArrayList<>();
		List<ShmAsEntdCust> notDeletedUpdated = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(shmAsEntdCustsForDelete)) {
			if (CollectionUtils.isNotEmpty(shmAsEntdCustsForDelete)
					&& CollectionUtils.isNotEmpty(shmAsEntdCustsOriginal)) {
				shmAsEntdCustsOriginal.forEach(shmAsEntdCust -> {

					Optional<ShmAsEntdCust> optionalShmAsEntdCust = shmAsEntdCustsForDelete
							.stream()
							.filter(shmAsEntdCustToDelete ->
									shmAsEntdCustToDelete.getId().getShpInstId() == shmAsEntdCust.getId().getShpInstId()
											&& shmAsEntdCustToDelete.getId().getSeqNbr() == shmAsEntdCust
											.getId()
											.getSeqNbr())
							.findAny();
					if (!optionalShmAsEntdCust.isPresent()) {
						notDeleted.add(shmAsEntdCust);
					}

				});
				if (CollectionUtils.isNotEmpty(shmAsEntdCustsForUpdate)) {

					notDeletedUpdated.addAll(shmAsEntdCustsForUpdate);

				} else {
					notDeletedUpdated.addAll(notDeleted);
				}

				notDeletedUpdated.addAll(shmAsEntdCustsForInsert);

				result.put(ActionCd.DELETE, shmAsEntdCustsOriginal);
				result.put(ActionCd.ADD, resetSeqNumberShmAsEntdCust(shmShipment.getShpInstId(), notDeletedUpdated));

			}
		} else {

			Optional<ShmAsEntdCust> optionalShmAsEntdCust = shmAsEntdCustsOriginal
					.stream()
					.max(Comparator.comparingLong(o -> o.getId().getSeqNbr()));
			AtomicReference<Long> seqNumber = new AtomicReference<>(optionalShmAsEntdCust
					.map(shmAsEntdCust -> shmAsEntdCust.getId().getSeqNbr())
					.orElse(0L));
			shmAsEntdCustsForInsert.forEach(shmAdvBydCarr -> {
				seqNumber.getAndSet(seqNumber.get() + 1);
				shmAdvBydCarr.getId().setSeqNbr(seqNumber.get());
			});
			result.put(ActionCd.ADD, shmAsEntdCustsForInsert);
			result.put(ActionCd.UPDATE, shmAsEntdCustsForUpdate);
		}

		return result;
	}

}
