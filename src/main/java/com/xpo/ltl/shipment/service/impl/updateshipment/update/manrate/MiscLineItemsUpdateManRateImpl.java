package com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate;

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
import com.xpo.ltl.shipment.service.dao.ShmMiscLineItemSubDAO;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.MiscLineItemsUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractMiscLineItems;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.math.BigDecimal;
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
public class MiscLineItemsUpdateManRateImpl extends AbstractMiscLineItems implements MiscLineItemsUpdate {

	@Inject
	private ShmMiscLineItemSubDAO shmMiscLineItemSubDAO;

	private static void setSystemTrxValues(String racfId, ShmMiscLineItem shmMiscLineItem) {
		shmMiscLineItem.setLstUpdtTmst(new Timestamp(new Date().getTime()));
		shmMiscLineItem.setLstUpdtTranCd(MANRATE_TRAN_CD);
		shmMiscLineItem.setLstUpdtUid(racfId);
	}

	private static void setValues(MiscLineItem miscLineItem, ShmMiscLineItem shmMiscLineItem) {
		if (Objects.nonNull(miscLineItem.getAmount())) {
			shmMiscLineItem.setAmt(BigDecimal.valueOf(miscLineItem.getAmount()));
		}

		if (shmMiscLineItem.getLnTypCd().equals(getMiscLineItemCd(MiscLineItemCd.AS_WGT.value()))
				|| shmMiscLineItem.getLnTypCd().equals(getMiscLineItemCd(MiscLineItemCd.DEFICIT_WGT.value()))
				&& (Objects.nonNull(miscLineItem.getTariffsRate()))) {
			shmMiscLineItem.setTrfRt(BigDecimal.valueOf(miscLineItem.getTariffsRate()));

		}
		if (shmMiscLineItem.getLnTypCd().equals(getMiscLineItemCd(MiscLineItemCd.DISC_LN.value())) && (Objects.nonNull(
				miscLineItem.getPrepaidPercentage()))) {
			shmMiscLineItem.setPpdPct(BigDecimal.valueOf(miscLineItem.getPrepaidPercentage()));

		}
		if (shmMiscLineItem.getLnTypCd().equals(getMiscLineItemCd(MiscLineItemCd.PART_PPD_LN.value()))
				|| shmMiscLineItem.getLnTypCd().equals(getMiscLineItemCd(MiscLineItemCd.PART_COLL_LN.value()))
				&& (StringUtils.isNotEmpty(miscLineItem.getDescription()))) {
			shmMiscLineItem.setDescTxt(miscLineItem.getDescription());

		}

		if (Objects.nonNull(miscLineItem.getLineTypeCd())) {
			shmMiscLineItem.setLnTypCd(getMiscLineItemCdAlt(miscLineItem.getLineTypeCd().value()));
		}
		shmMiscLineItem.setUom(getUnitOfMeasureByLineType(miscLineItem.getLineTypeCd()));
	}

	@LogExecutionTime
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
		List<String> miscLineItemCodes = new ArrayList<>();
		List<MiscLineItemCd> miscLineItemEnumCodes = new ArrayList<>();

		miscLineItemCodes.add(getMiscLineItemCd(MiscLineItemCd.AS_WGT.value()));
		miscLineItemCodes.add(getMiscLineItemCd(MiscLineItemCd.DEFICIT_WGT.value()));
		miscLineItemCodes.add(getMiscLineItemCd(MiscLineItemCd.DISC_LN.value()));
		miscLineItemCodes.add(getMiscLineItemCd(MiscLineItemCd.PART_COLL_LN.value()));
		miscLineItemCodes.add(getMiscLineItemCd(MiscLineItemCd.PART_PPD_LN.value()));

		miscLineItemEnumCodes.add((MiscLineItemCd.AS_WGT));
		miscLineItemEnumCodes.add((MiscLineItemCd.DEFICIT_WGT));
		miscLineItemEnumCodes.add((MiscLineItemCd.DISC_LN));
		miscLineItemEnumCodes.add((MiscLineItemCd.PART_COLL_LN));
		miscLineItemEnumCodes.add((MiscLineItemCd.PART_PPD_LN));

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
			List<ShmMiscLineItem> shmMiscLineItemsFiltered = getShmMiscLineItemListFilter(shmMiscLineItemsOriginal,
					miscLineItemCodes);

			List<MiscLineItem> miscLineItemList = getMiscLineItemListFilter(miscLineItems, miscLineItemEnumCodes);

			shmMiscLineItemListToDelete = this.shmMiscLineItemListToDelete(miscLineItemList, shmMiscLineItemsFiltered);

			long seqNumber = getMaxSequenceNumberLong(shmMiscLineItemsOriginal
					.stream()
					.map(shmMiscLineItem -> shmMiscLineItem.getId().getSeqNbr())
					.collect(Collectors.toList()));
			if (CollectionUtils.isNotEmpty(shmMiscLineItemListToDelete)) {
				shmMiscLineItemListToUpdate = this.getShmMiscLineItemListToUpdate(miscLineItemList,
						shmMiscLineItemsFiltered,
						true,
						userId,
						transactionContext);
				shmMiscLineItemListToAdd = this.shmMiscLineItemListToAdd(shipmentInstId,
						miscLineItemList,
						shmMiscLineItemsFiltered,
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
				shmMiscLineItemListToUpdate = this.getShmMiscLineItemListToUpdate(miscLineItemList,
						shmMiscLineItemsFiltered,
						false,
						userId,
						transactionContext);
				shmMiscLineItemListToAdd = this.shmMiscLineItemListToAdd(shipmentInstId,
						miscLineItemList,
						shmMiscLineItemsFiltered,
						shipment,
						seqNumber,
						userId);
				result.put(ActionCd.ADD, shmMiscLineItemListToAdd);
				result.put(ActionCd.UPDATE, shmMiscLineItemListToUpdate);
			}

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
				getException(ServiceErrorMessage.MISC_LINE_ITEM_UPDATE_FAILED,
						this.getClass().getSimpleName(),
						e,
						transactionContext);
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

	@LogExecutionTime
	public List<ShmMiscLineItem> getShmMiscLineItemListToUpdate(
			final List<MiscLineItem> miscLineItems,
			final List<ShmMiscLineItem> shmMiscLineItems,
			boolean fromDelete,
			String racfId,
			TransactionContext transactionContext) {
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
						transactionContext);
			} catch (ValidationException e) {
				throw new UnsupportedOperationException(e);
			}
		});
		return result;
	}

	@LogExecutionTime
	public void getItemsToUpdate(
			List<ShmMiscLineItem> shmMiscLineItems,
			boolean fromDelete,
			String racfId,
			List<ShmMiscLineItem> result,
			String s,
			Optional<MiscLineItem> optionalMiscLineItem,
			TransactionContext transactionContext) throws ValidationException {
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

				setValues(miscLineItem, shmMiscLineItem);

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