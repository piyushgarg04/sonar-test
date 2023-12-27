package com.xpo.ltl.shipment.service.impl.updateshipment.update.common;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItemPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.MiscLineItem;
import com.xpo.ltl.api.shipment.v2.MiscLineItemCd;
import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.LoadValues;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.MiscLineItemsUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractMiscLineItems;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.LoadValFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.LoadValuesFactory;
import org.apache.commons.collections4.CollectionUtils;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RequestScoped
public class MiscLineItemsUpdateCommonImpl extends AbstractMiscLineItems implements MiscLineItemsUpdate {
	@Inject
	private LoadValFactory loadValFactory;
	private static void setSystemTrxValues(String racfId, ShmMiscLineItem shmMiscLineItem) {
		shmMiscLineItem.setLstUpdtTmst(new Timestamp(new Date().getTime()));
		shmMiscLineItem.setLstUpdtTranCd(CORRECTION_TRAN_CD);
		shmMiscLineItem.setLstUpdtUid(racfId);
	}



	/**
	 * https://xpo.atlassian.net/browse/LECS-233
	 * @param shipmentInstId
	 * @param miscLineItems
	 * @param shmMiscLineItemsOriginal
	 * @param shipment
	 * @param userId
	 * @param transactionContext
	 * @return
	 * @throws ServiceException
	 */
	@Override
	public Map<ActionCd, List<ShmMiscLineItem>> getItemsForTransactions(
			Long shipmentInstId,
			List<MiscLineItem> miscLineItems,
			List<ShmMiscLineItem> shmMiscLineItemsOriginal,
			ShmShipment shipment,
			String userId,
			TransactionContext transactionContext,
			ShipmentUpdateActionCd shipmentUpdateActionCd) throws ServiceException {

		EnumMap<ActionCd, List<ShmMiscLineItem>> result = new EnumMap<>(ActionCd.class);
		List<String> miscLineItemCodesCanBeDeleted = new ArrayList<>();
		List<MiscLineItemCd> miscLineItemEnumCodesCanBeDeleted = new ArrayList<>();
		List<String> miscLineItemCodesUpdateInsert = new ArrayList<>();
		List<MiscLineItemCd> miscLineItemEnumCodesUpdateInsert = new ArrayList<>();

		if (ShipmentUpdateActionCd.CORRECTION.equals(shipmentUpdateActionCd)) {
			miscLineItemCodesCanBeDeleted.add(getMiscLineItemCd(MiscLineItemCd.AS_WGT.value()));
			miscLineItemCodesCanBeDeleted.add(getMiscLineItemCd(MiscLineItemCd.DEFICIT_WGT.value()));
			miscLineItemCodesCanBeDeleted.add(getMiscLineItemCd(MiscLineItemCd.DISC_LN.value()));
			miscLineItemCodesCanBeDeleted.add(getMiscLineItemCd(MiscLineItemCd.CASH_PPD_LN.value()));
			miscLineItemCodesCanBeDeleted.add(getMiscLineItemCd(MiscLineItemCd.COD_AMT.value()));

			miscLineItemEnumCodesCanBeDeleted.add((MiscLineItemCd.AS_WGT));
			miscLineItemEnumCodesCanBeDeleted.add((MiscLineItemCd.DEFICIT_WGT));
			miscLineItemEnumCodesCanBeDeleted.add((MiscLineItemCd.DISC_LN));
			miscLineItemEnumCodesCanBeDeleted.add((MiscLineItemCd.CASH_PPD_LN));
			miscLineItemEnumCodesCanBeDeleted.add((MiscLineItemCd.COD_AMT));

			miscLineItemEnumCodesUpdateInsert.addAll(miscLineItemEnumCodesCanBeDeleted);
			miscLineItemEnumCodesUpdateInsert.add(MiscLineItemCd.PART_PPD_LN);
			miscLineItemEnumCodesUpdateInsert.add(MiscLineItemCd.PART_COLL_LN);

			miscLineItemCodesUpdateInsert.addAll(miscLineItemCodesCanBeDeleted);
			miscLineItemCodesUpdateInsert.add(getMiscLineItemCd(MiscLineItemCd.PART_PPD_LN.value()));
			miscLineItemCodesUpdateInsert.add(getMiscLineItemCd(MiscLineItemCd.PART_COLL_LN.value()));
		} else if (ShipmentUpdateActionCd.AUTO_RATE.equals(shipmentUpdateActionCd)){
			miscLineItemCodesCanBeDeleted.add(getMiscLineItemCd(MiscLineItemCd.AS_WGT.value()));
			miscLineItemCodesCanBeDeleted.add(getMiscLineItemCd(MiscLineItemCd.DEFICIT_WGT.value()));
			miscLineItemCodesCanBeDeleted.add(getMiscLineItemCd(MiscLineItemCd.DISC_LN.value()));


			miscLineItemEnumCodesCanBeDeleted.add((MiscLineItemCd.AS_WGT));
			miscLineItemEnumCodesCanBeDeleted.add((MiscLineItemCd.DEFICIT_WGT));
			miscLineItemEnumCodesCanBeDeleted.add((MiscLineItemCd.DISC_LN));
			miscLineItemEnumCodesUpdateInsert.addAll(miscLineItemEnumCodesCanBeDeleted);
			miscLineItemCodesUpdateInsert.addAll(miscLineItemCodesCanBeDeleted);

		}

		List<ShmMiscLineItem> shmMiscLineItemListToDelete;
		List<ShmMiscLineItem> shmMiscLineItemListResetSeqNumber;

		List<ShmMiscLineItem> shmMiscLineItemListToAdd;
		List<ShmMiscLineItem> shmMiscLineItemListToUpdate;
		if (CollectionUtils.isNotEmpty(miscLineItems)) {
			miscLineItems.forEach(miscLineItem -> {
				if (Objects.isNull(miscLineItem.getShipmentInstId())) {
					miscLineItem.setShipmentInstId(shipmentInstId);
				}
			});

			List<ShmMiscLineItem> shmMiscLineItemsFilterToDelete = getShmMiscLineItemListFilter(shmMiscLineItemsOriginal,
					miscLineItemCodesCanBeDeleted);
			List<ShmMiscLineItem> shmMiscLineItemsFilterToUpdated = getShmMiscLineItemListFilter(shmMiscLineItemsOriginal,
					miscLineItemCodesUpdateInsert);

			List<MiscLineItem> miscLineItemListToDeleted = getMiscLineItemListFilter(miscLineItems,
					miscLineItemEnumCodesCanBeDeleted);
			List<MiscLineItem> miscLineItemListToUpdated = getMiscLineItemListFilter(miscLineItems,
					miscLineItemEnumCodesUpdateInsert);

			shmMiscLineItemListToDelete = this.shmMiscLineItemListToDelete(miscLineItemListToDeleted,
					shmMiscLineItemsFilterToDelete);

			long seqNumber = getMaxSequenceNumberLong(shmMiscLineItemsOriginal
					.stream()
					.map(shmMiscLineItem -> shmMiscLineItem.getId().getSeqNbr())
					.collect(Collectors.toList()));
			if (CollectionUtils.isNotEmpty(shmMiscLineItemListToDelete)) {
				shmMiscLineItemListToUpdate = this.getShmMiscLineItemListToUpdate(miscLineItemListToUpdated,
						shmMiscLineItemsFilterToUpdated,
						true,
						userId,
						transactionContext,
						shipmentUpdateActionCd);
				shmMiscLineItemListToAdd = this.shmMiscLineItemListToAdd(shipmentInstId,
						miscLineItems,
						shmMiscLineItemsOriginal,
						shipment,
						seqNumber,
						userId);

				shmMiscLineItemListResetSeqNumber = this.resetSeqNumberShmMiscLineItemList(shipmentInstId,
						shmMiscLineItemListToDelete,
						shmMiscLineItemsOriginal,
						shmMiscLineItemListToUpdate,
						shmMiscLineItemListToAdd,
						true,
						userId,
						shipmentUpdateActionCd);

				shmMiscLineItemListToDelete = new ArrayList<>();
				shmMiscLineItemListToDelete.addAll(shmMiscLineItemsOriginal);
				result.put(ActionCd.DELETE, shmMiscLineItemListToDelete);
				shmMiscLineItemListToAdd = new ArrayList<>();
				shmMiscLineItemListToAdd.addAll(shmMiscLineItemListResetSeqNumber);
				result.put(ActionCd.ADD, shmMiscLineItemListToAdd);
				result.put(ActionCd.UPDATE, new ArrayList<>());

			} else {
				shmMiscLineItemListToUpdate = this.getShmMiscLineItemListToUpdate(miscLineItemListToUpdated,
						shmMiscLineItemsFilterToUpdated,
						false,
						userId,
						transactionContext,
						shipmentUpdateActionCd);
				shmMiscLineItemListToAdd = this.shmMiscLineItemListToAdd(shipmentInstId,
						miscLineItems,
						shmMiscLineItemsOriginal,
						shipment,
						seqNumber,
						userId);
				result.put(ActionCd.ADD, shmMiscLineItemListToAdd);
				result.put(ActionCd.UPDATE, shmMiscLineItemListToUpdate);
			}

		}else{
			List<ShmMiscLineItem> shmMiscLineItemsFilterToDelete = getShmMiscLineItemListFilter(shmMiscLineItemsOriginal,
					miscLineItemCodesCanBeDeleted);
			List<MiscLineItem> miscLineItemListToDeleted = getMiscLineItemListFilter(miscLineItems,
					miscLineItemEnumCodesCanBeDeleted);

			shmMiscLineItemListToDelete = this.shmMiscLineItemListToDelete(miscLineItemListToDeleted,
					shmMiscLineItemsFilterToDelete);
			result.put(ActionCd.DELETE, shmMiscLineItemListToDelete);
		}
		return result;
	}

	@Override
	public CompletableFuture<Map<ActionCd, List<ShmMiscLineItem>>> cfGetItemsForTransactions(
			Long shipmentInstId,
			List<MiscLineItem> miscLineItems,
			List<ShmMiscLineItem> shmMiscLineItemsOriginal,
			ShmShipment shipment,
			String userId,
			TransactionContext transactionContext,
			ShipmentUpdateActionCd shipmentUpdateActionCd) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return getItemsForTransactions(shipmentInstId,
						miscLineItems,
						shmMiscLineItemsOriginal,
						shipment,
						userId,
						transactionContext,
						shipmentUpdateActionCd);
			} catch (ServiceException e) {
				getException(ServiceErrorMessage.MISC_LINE_ITEM_UPDATE_FAILED, "MiscLineItems", e, transactionContext);
			}
			return null;
		});
	}

	@Override
	public void update(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmMiscLineItem> shmMiscLineItemListToUpdate,
			String transactionCd,
			String userId) throws ValidationException {
		this.updateMiscLineItem(entityManager,
				db2EntityManager,
				transactionContext,
				shmMiscLineItemListToUpdate,
				transactionCd,
				userId);

	}

	@Override
	public void delete(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmMiscLineItem> shmMiscLineItemListToDelete) {
		this.deleteMiscLineItems(entityManager, db2EntityManager, transactionContext, shmMiscLineItemListToDelete);

	}

	@Override
	public void insert(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmMiscLineItem> shmMiscLineItemListToAdd,
			String transactionCd,
			String userId) throws ValidationException {
		this.addMiscLineItem(entityManager,
				db2EntityManager,
				transactionContext,
				shmMiscLineItemListToAdd,
				transactionCd,
				userId);

	}

	private List<ShmMiscLineItem> getShmMiscLineItemListToUpdate(
			final List<MiscLineItem> miscLineItems,
			final List<ShmMiscLineItem> shmMiscLineItems,
			boolean fromDelete,
			String racfId,
			TransactionContext transactionContext,
			ShipmentUpdateActionCd shipmentUpdateActionCd) {
		List<ShmMiscLineItem> result = new ArrayList<>();
		List<String> lnTypCdDbList = CollectionUtils
				.emptyIfNull(shmMiscLineItems)
				.stream()
				.map(ShmMiscLineItem::getLnTypCd)
				.collect(Collectors.toList());
		List<String> miscLineItemCdList = CollectionUtils
				.emptyIfNull(miscLineItems)
				.stream()
				.map(miscLineItem -> getMiscLineItemCd(miscLineItem.getLineTypeCd().value()))
				.collect(Collectors.toList());
		List<String> sameCodes = lnTypCdDbList
				.stream()
				.filter(miscLineItemCdList::contains)
				.collect(Collectors.toList());
		CollectionUtils.emptyIfNull(sameCodes).forEach(s -> {
			Optional<MiscLineItem> optionalMiscLineItem = miscLineItems
					.stream()
					.filter(miscLineItem -> s.equals(getMiscLineItemCdAlt(miscLineItem.getLineTypeCd().value())))
					.findAny();

			try {
				getItemsToUpdate(shmMiscLineItems,
						fromDelete,
						racfId,
						result,
						s,
						optionalMiscLineItem,
						transactionContext,
						shipmentUpdateActionCd
						);
			} catch (ValidationException e) {
				throw new UnsupportedOperationException(e);
			}
		});
		return result;
	}

	private void getItemsToUpdate(
			List<ShmMiscLineItem> shmMiscLineItems,
			boolean fromDelete,
			String racfId,
			List<ShmMiscLineItem> result,
			String s,
			Optional<MiscLineItem> optionalMiscLineItem,
			TransactionContext transactionContext,
			ShipmentUpdateActionCd shipmentUpdateActionCd) throws ValidationException {
		if (optionalMiscLineItem.isPresent() && Objects.nonNull(optionalMiscLineItem.get().getShipmentInstId())
				&& Objects.nonNull(optionalMiscLineItem.get().getSequenceNbr())) {

			MiscLineItem miscLineItem = optionalMiscLineItem.get();

			Optional<ShmMiscLineItem> optionalShmMiscLineItems = shmMiscLineItems
					.stream()
					.filter(shmMiscLineItem -> s.equals(shmMiscLineItem.getLnTypCd())
							&& shmMiscLineItem.getId().getSeqNbr() == miscLineItem.getSequenceNbr().longValue()
							&& shmMiscLineItem.getId().getShpInstId() == miscLineItem.getShipmentInstId())
					.findAny();

			if (optionalShmMiscLineItems.isPresent()) {

				ShmMiscLineItem shmMiscLineItemToCheck = new ShmMiscLineItem();
				ShmMiscLineItem shmMiscLineItem = optionalShmMiscLineItems.get();

				copyFields(shmMiscLineItem, shmMiscLineItemToCheck);
				LoadValuesFactory loadValuesFactory = loadValFactory.getFactoryImplementation(shipmentUpdateActionCd);
				LoadValues<MiscLineItem, ShmMiscLineItem> shmMiscLineItemLoadValues = loadValuesFactory.getFactoryImplementation(
						ShmMiscLineItem.class);
				shmMiscLineItemLoadValues.loadtValues(miscLineItem, shmMiscLineItem);


				List<String> diff = this.compareShmMiscLineItem(shmMiscLineItem, shmMiscLineItemToCheck);

				if (CollectionUtils.isNotEmpty(diff)) {
					setSystemTrxValues(racfId, shmMiscLineItem);
					if (!fromDelete) {
						result.add(shmMiscLineItem);
					} else {
						miscLineItem.setListActionCd(ActionCd.ADD);
						ShmMiscLineItem shmMiscLineItemToNew = new ShmMiscLineItem();
						try {
							shmMiscLineItemToNew = DtoTransformer.toShmMiscLineItem(miscLineItem, shmMiscLineItemToNew);
							shmMiscLineItemToNew.setUom(getUnitOfMeasureByLineType(shmMiscLineItem.getLnTypCd()));
						} catch (ServiceException e) {
							throw ExceptionBuilder
									.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, transactionContext)
									.moreInfo("MiscLineItemsUpdateManRateImpl", "Errors has been detected: " + e)
									.build();
						}
						copyFields(shmMiscLineItem, shmMiscLineItemToNew);
						ShmMiscLineItemPK id = new ShmMiscLineItemPK();
						id.setShpInstId(shmMiscLineItem.getId().getShpInstId());
						id.setSeqNbr(shmMiscLineItem.getId().getSeqNbr());
						shmMiscLineItemToNew.setId(id);
						result.add(shmMiscLineItemToNew);
					}
				}
			}
		}
	}
}