package com.xpo.ltl.shipment.service.impl.updateshipment.update.correction;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmCustomsBond;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.CustomsBond;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.CustomsBondUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractCustomsBond;
import org.apache.commons.collections4.CollectionUtils;

import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RequestScoped
public class CorrectionCustomsBondUpdateImpl extends AbstractCustomsBond implements CustomsBondUpdate {

	@Override
	public Map<ActionCd, List<ShmCustomsBond>> getCustomsBondForTransactions(
			Long shipmentInstId,
			UpdateShipmentRqst updateShipmentRqst,
			List<ShmCustomsBond> shmCustomsBondOriginal,
			ShmShipment shipment,
			String transactionCode,
			TransactionContext transactionContext) throws ServiceException {

		Map<ActionCd, List<ShmCustomsBond>> result = new HashMap<>();

		List<ShmCustomsBond> shmCustomsBondsToInsert = this.getAbcCustomsBondForInsert(shipment,
				updateShipmentRqst.getCustomsBonds(),
				shmCustomsBondOriginal,
				transactionContext);
		List<ShmCustomsBond> shmCustomsBondsToDelete = this.getAbcCustomsBondListToDelete(updateShipmentRqst.getCustomsBonds(),
				shmCustomsBondOriginal);
		List<ShmCustomsBond> shmCustomsBondsToUpdate = this.getCustomsBondListToUpdate(updateShipmentRqst.getCustomsBonds(),
				shmCustomsBondOriginal,
				transactionCode,
				transactionContext);

		List<ShmCustomsBond> notDeleted = new ArrayList<>();
		List<ShmCustomsBond> notDeletedUpdated = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(shmCustomsBondsToDelete)) {
			if (CollectionUtils.isNotEmpty(shmCustomsBondsToDelete)
					&& CollectionUtils.isNotEmpty(shmCustomsBondOriginal)) {
				shmCustomsBondOriginal.forEach(shmCustomsBond -> {

					Optional<ShmCustomsBond> optionalShmAsEntdCust = shmCustomsBondsToDelete
							.stream()
							.filter(shmCustomsBondToDelete ->
									shmCustomsBondToDelete.getId().getShpInstId() == shmCustomsBond
											.getId()
											.getShpInstId()
											&& shmCustomsBondToDelete.getId().getSeqNbr() == shmCustomsBond
											.getId()
											.getSeqNbr())
							.findAny();
					if (!optionalShmAsEntdCust.isPresent()) {
						notDeleted.add(shmCustomsBond);
					}

				});
				if (CollectionUtils.isNotEmpty(shmCustomsBondsToUpdate)) {

					notDeletedUpdated.addAll(shmCustomsBondsToUpdate);

				} else {
					notDeletedUpdated.addAll(notDeleted);
				}

				notDeletedUpdated.addAll(shmCustomsBondsToInsert);

				result.put(ActionCd.DELETE, shmCustomsBondOriginal);
				result.put(ActionCd.ADD, notDeletedUpdated);

			}
		} else {
			result.put(ActionCd.ADD, shmCustomsBondsToInsert);
			result.put(ActionCd.UPDATE, shmCustomsBondsToUpdate);
		}
		return result;
	}

	private List<ShmCustomsBond> getCustomsBondListToUpdate(
			List<CustomsBond> customsBonds,
			List<ShmCustomsBond> shmCustomsBondOriginal,
			String transactionCode,
			TransactionContext transactionContext) {

		List<ShmCustomsBond> result = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(customsBonds)) {
			customsBonds.forEach(customsBond -> {
				Optional<ShmCustomsBond> optionalShmCustomsBond = Optional.empty();
				if (CollectionUtils.isNotEmpty(shmCustomsBondOriginal)) {
					optionalShmCustomsBond = shmCustomsBondOriginal
							.stream()
							.filter(shmCustomsBond -> shmCustomsBond.getId().getShpInstId()
									== customsBond.getShipmentInstId())
							.findAny();
				}

				if (optionalShmCustomsBond.isPresent()) {

					ShmCustomsBond shmCustomsBond = optionalShmCustomsBond.get();
					ShmCustomsBond shmCustomsBondToCheck = new ShmCustomsBond();

					copyFields(shmCustomsBond, shmCustomsBondToCheck);

					shmCustomsBond.setBondNbrTxt(customsBond.getBondNbr()); // BOND_NBR_TXT
					shmCustomsBond.setCtyTxt(customsBond.getCity()); // CTY_TXT
					shmCustomsBond.setStCd(customsBond.getStateCd()); // ST_CD

					List<String> diff = this.compareCustomsBond(shmCustomsBond, shmCustomsBondToCheck);

					if (CollectionUtils.isNotEmpty(diff)) {
						shmCustomsBond.setLstUpdtTranCd(transactionCode);
						shmCustomsBond.setLstUpdtUid(this.getUserFromContext(transactionContext));
						shmCustomsBond.setLstUpdtTmst(new Timestamp(new Date().getTime()));
						result.add(shmCustomsBond);
					}
				}
			});
		}
		return result;

	}

	@Override
	public CompletableFuture<Map<ActionCd, List<ShmCustomsBond>>> cfGetCustomsBondForTransactions(
			Long shipmentInstId,
			UpdateShipmentRqst updateShipmentRqst,
			List<ShmCustomsBond> shmCustomsBondOriginal,
			ShmShipment shipment,
			String transactionCode,
			TransactionContext transactionContext) throws ServiceException {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return getCustomsBondForTransactions(shipmentInstId,
						updateShipmentRqst,
						shmCustomsBondOriginal,
						shipment,
						transactionCode,
						transactionContext);
			} catch (ServiceException e) {
				getException(ServiceErrorMessage.COMMODITY_UPDATE_FAILED,
						this.getClass().getSimpleName(),
						e,
						transactionContext);
			}
			return null;
		});
	}

	@Override
	public void insert(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmCustomsBond> shmCustomsBondToAddList,
			String transactionCd) throws ValidationException {

		this.insertCustomsBond(entityManager,
				db2EntityManager,
				transactionContext,
				shmCustomsBondToAddList,
				transactionCd);
	}

	@Override
	public void update(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmCustomsBond> shmCustomsBondToUpdateList,
			String transactionCd) throws ValidationException {

		this.updateCustomsBond(entityManager,
				db2EntityManager,
				transactionContext,
				shmCustomsBondToUpdateList,
				transactionCd);
	}

	@Override
	public void delete(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmCustomsBond> shmCustomsBondToDeleteList) {

		this.deleteCustomsBond(entityManager, db2EntityManager, transactionContext, shmCustomsBondToDeleteList);
	}
}
