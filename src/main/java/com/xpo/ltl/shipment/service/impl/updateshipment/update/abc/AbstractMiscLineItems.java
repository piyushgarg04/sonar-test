package com.xpo.ltl.shipment.service.impl.updateshipment.update.abc;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmMiscLineItem;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItemPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.MiscLineItem;
import com.xpo.ltl.api.shipment.v2.MiscLineItemCd;
import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.shipment.service.dao.ShmMiscLineItemSubDAO;
import com.xpo.ltl.shipment.service.impl.updateshipment.comparator.Comparator;
import com.xpo.ltl.shipment.service.impl.updateshipment.comparator.ComparatorFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.functional.LoadValuesToUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.functional.LoadValuesToUpdateAutoRateMiscImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.functional.LoadValuesToUpdateCorrectionMiscImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.functional.LoadValuesToUpdateManRateMiscImpl;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.xpo.ltl.shipment.service.impl.updateshipment.comparator.EntityComparer.findDifferences;

public abstract class AbstractMiscLineItems extends AbstractUpdate {

	private static final String MISC_LINE_ITEMS = "MiscLineItems";
	@Inject
	private ShmMiscLineItemSubDAO shmMiscLineItemSubDAO;

	private static final String UOM_POUNDS_CD = "1";
	private static final String UOM_COUNT_CD = "3";

	private static final String UOM_COUNT_NONE = StringUtils.SPACE;

	@LogExecutionTime
	public static void setDefaultValuesMiscLineItem(
			String userId, Timestamp timestamp, ShmMiscLineItem shmMiscLineItem) {
		shmMiscLineItem.setDmlTmst(timestamp);
		shmMiscLineItem.setDtlCapxtimestamp(timestamp);
		shmMiscLineItem.setLstUpdtTmst(timestamp);
		shmMiscLineItem.setReplLstUpdtTmst(timestamp);
		shmMiscLineItem.setLstUpdtUid(userId);
		if (StringUtils.isEmpty(shmMiscLineItem.getPmtMethCd())) {
			shmMiscLineItem.setPmtMethCd(StringUtils.SPACE);
		}
		if (StringUtils.isEmpty(shmMiscLineItem.getChrgToCd())) {
			shmMiscLineItem.setChrgToCd(StringUtils.SPACE);
		}
		if (StringUtils.isEmpty(shmMiscLineItem.getArchiveCntlCd())) {
			shmMiscLineItem.setArchiveCntlCd(StringUtils.SPACE);
		}
		if (StringUtils.isEmpty(shmMiscLineItem.getCheckNbrTxt())) {
			shmMiscLineItem.setCheckNbrTxt(StringUtils.SPACE);
		}
		if (StringUtils.isEmpty(shmMiscLineItem.getMinChrgInd())) {
			shmMiscLineItem.setMinChrgInd(getFlag(false));
		}
		if (StringUtils.isEmpty(shmMiscLineItem.getUom())) {
			shmMiscLineItem.setUom(StringUtils.SPACE);
		}
		if (Objects.isNull(shmMiscLineItem.getTrfRt())) {
			shmMiscLineItem.setTrfRt(BigDecimal.ZERO);
		}
		if (Objects.isNull(shmMiscLineItem.getQty())) {
			shmMiscLineItem.setQty(BigDecimal.ZERO);
		}
		if (Objects.isNull(shmMiscLineItem.getDescTxt()) && (Objects.nonNull(shmMiscLineItem.getLnTypCd()))) {

			shmMiscLineItem.setDescTxt(getMiscLineItemCd(shmMiscLineItem.getLnTypCd()));
			if (Objects.isNull(shmMiscLineItem.getDescTxt())) {
				shmMiscLineItem.setDescTxt(StringUtils.SPACE);
			}

		}
		if (Objects.isNull(shmMiscLineItem.getPpdPct())) {
			shmMiscLineItem.setPpdPct(BigDecimal.ZERO);
		}


	}

	public static void resetSeqNumberMiscLineItem(Long shipmentInstId, List<ShmMiscLineItem> shmMiscLineItems) {

		if (CollectionUtils.isNotEmpty(shmMiscLineItems)) {

			sortListByLongField(shmMiscLineItems, ShmMiscLineItem::getId, ShmMiscLineItemPK::getSeqNbr);
			AtomicReference<Long> seq = new AtomicReference<>(1L);
			shmMiscLineItems.forEach(shmMiscLineItem -> {
				if (Objects.nonNull(shmMiscLineItem.getId())) {
					shmMiscLineItem.getId().setSeqNbr(seq.getAndSet(seq.get() + 1));
					shmMiscLineItem.getId().setShpInstId(shipmentInstId);
				} else {

					ShmMiscLineItemPK shmMiscLineItemPK = new ShmMiscLineItemPK();
					shmMiscLineItemPK.setShpInstId(shipmentInstId);
					shmMiscLineItemPK.setSeqNbr(seq.getAndSet(seq.get() + 1));
					shmMiscLineItem.setId(shmMiscLineItemPK);
				}

			});
		}
	}

	public static String getMiscLineItemCdAlt(String value) {
		Field[] statusDeclaredFields = MiscLineItemCd.class.getDeclaredFields();
		return getAlternateValue(value, statusDeclaredFields);
	}

	public static String getMiscLineItemCd(String name) {
		Field[] statusDeclaredFields = MiscLineItemCd.class.getDeclaredFields();
		return getAlternateValueByVal(name, statusDeclaredFields);
	}

	@LogExecutionTime
	public void addMiscLineItem(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmMiscLineItem> shmMiscLineItemListToAdd,
			String transactionCd,
			String userId) throws ValidationException {
		if (CollectionUtils.isNotEmpty(shmMiscLineItemListToAdd)) {

			shmMiscLineItemListToAdd.forEach(shmMiscLineItem -> {
				shmMiscLineItem.setLstUpdtTmst(new Timestamp(new Date().getTime()));
				shmMiscLineItem.setLstUpdtTranCd(transactionCd);
				shmMiscLineItem.setLstUpdtUid(userId);
			});
			try {
				shmMiscLineItemSubDAO.persist(shmMiscLineItemListToAdd, entityManager);
				if (appContext.getDb2CommitEnabledForUpdateShipment()) {
					shmMiscLineItemListToAdd.forEach(shmMiscLineItem -> shmMiscLineItemSubDAO.createDB2ShmMiscLineItem(shmMiscLineItem,
							db2EntityManager));
				}
			} catch (Exception e) {
				getException(ServiceErrorMessage.MISC_LINE_ITEM_CREATE_FAILED, MISC_LINE_ITEMS, e, transactionContext);
			}

		}
	}

	@LogExecutionTime
	public void deleteMiscLineItems(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmMiscLineItem> shmMiscLineItemListToDelete) {
		if (CollectionUtils.isNotEmpty(shmMiscLineItemListToDelete)) {

			shmMiscLineItemSubDAO.remove(shmMiscLineItemListToDelete, entityManager);

			if (appContext.getDb2CommitEnabledForUpdateShipment()) {
				shmMiscLineItemListToDelete.forEach(shmMiscLineItem -> {
					try {
						final Function<DB2ShmMiscLineItem, Boolean> checkVersionFunctionMiscLineItem = getCheckVersionFunctionShmMiscLineItem(
								new Timestamp(new Date().getTime()));
						shmMiscLineItemSubDAO.deleteDB2ShmMiscLineItem(shmMiscLineItem.getId(),
								checkVersionFunctionMiscLineItem,
								db2EntityManager,
								transactionContext);
					} catch (Exception e) {

						getException(ServiceErrorMessage.MISC_LINE_ITEM_DELETE_FAILED,
								MISC_LINE_ITEMS,
								e,
								transactionContext);
					}
				});
			}

		}

	}

	public Function<DB2ShmMiscLineItem, Boolean> getCheckVersionFunctionShmMiscLineItem(final Timestamp exadataLstUpdtTmst) {
		return db2Entity -> db2Entity.getLstUpdtTmst().compareTo(exadataLstUpdtTmst) <= 0;
	}

	@LogExecutionTime
	public void updateMiscLineItem(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmMiscLineItem> shmMiscLineItemListToUpdate,
			String transactionCd,
			String userId) throws ValidationException {
		if (CollectionUtils.isNotEmpty(shmMiscLineItemListToUpdate)) {
			shmMiscLineItemListToUpdate.forEach(shmMiscLineItem -> {

				shmMiscLineItem.setLstUpdtTmst(new Timestamp(new Date().getTime()));
				shmMiscLineItem.setLstUpdtTranCd(transactionCd);
				shmMiscLineItem.setLstUpdtUid(userId);

			});
			shmMiscLineItemSubDAO.persist(shmMiscLineItemListToUpdate, entityManager);
			if (appContext.getDb2CommitEnabledForUpdateShipment()) {
				shmMiscLineItemListToUpdate.forEach(shmMiscLineItem -> {
					try {
						final Timestamp exadataLstUpdtTmst = new Timestamp(new Date().getTime());

						final Function<DB2ShmMiscLineItem, Boolean> checkVersionFunctionMiscLineItem = getCheckVersionFunctionShmMiscLineItem(
								exadataLstUpdtTmst);
						shmMiscLineItemSubDAO.updateDB2ShmMiscLineItem(shmMiscLineItem,
								checkVersionFunctionMiscLineItem,
								db2EntityManager,
								transactionContext);
					} catch (Exception e) {

						getException(ServiceErrorMessage.MISC_LINE_ITEM_UPDATE_FAILED,
								MISC_LINE_ITEMS,
								e,
								transactionContext);
					}
				});
			}

		}
	}

	@LogExecutionTime
	public List<ShmMiscLineItem> shmMiscLineItemListToDelete(
			final List<MiscLineItem> miscLineItems, final List<ShmMiscLineItem> shmMiscLineItems) {
		List<ShmMiscLineItem> result = new ArrayList<>();

//		if (CollectionUtils.isNotEmpty(miscLineItems) && (CollectionUtils.isNotEmpty(shmMiscLineItems))) {
			List<String> lnTypCdDbList = shmMiscLineItems
					.stream()
					.map(ShmMiscLineItem::getLnTypCd)
					.collect(Collectors.toList());
			List<String> miscLineItemCdList = miscLineItems
					.stream()
					.map(miscLineItem -> getMiscLineItemCd(miscLineItem.getLineTypeCd().value()))
					.collect(Collectors.toList());
			List<String> deletedCodes = lnTypCdDbList
					.stream()
					.filter(a -> !miscLineItemCdList.contains(a))
					.collect(Collectors.toList());
			result = shmMiscLineItems
					.stream()
					.filter(shmMiscLineItem -> deletedCodes.contains(shmMiscLineItem.getLnTypCd()))
					.collect(Collectors.toList());

//		}
		return result;
	}

	@LogExecutionTime
	public List<ShmMiscLineItem> resetSeqNumberShmMiscLineItemList(
			Long shipmentInstId,
			final List<ShmMiscLineItem> shmMiscLineItemsToDeleted,
			final List<ShmMiscLineItem> shmMiscLineItemsOriginal,
			final List<ShmMiscLineItem> shmMiscLineItemsToUpdate,
			final List<ShmMiscLineItem> shmMiscLineItemsToAdd,
			boolean fromDelete,
			String userId,
			ShipmentUpdateActionCd shipmentUpdateActionCd) {

		List<ShmMiscLineItem> notDeleted = new ArrayList<>();
		List<ShmMiscLineItem> notDeletedUpdated = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(shmMiscLineItemsToDeleted)
				&& CollectionUtils.isNotEmpty(shmMiscLineItemsOriginal)) {
			shmMiscLineItemsOriginal.forEach(shmMiscLineItem -> {

				Optional<ShmMiscLineItem> optionalShmMiscLineItem = shmMiscLineItemsToDeleted
						.stream()
						.filter(shmMiscLineItemToDelete ->
								shmMiscLineItemToDelete.getId().getShpInstId() == shmMiscLineItem.getId().getShpInstId()
										&& shmMiscLineItemToDelete.getId().getSeqNbr() == shmMiscLineItem
										.getId()
										.getSeqNbr())
						.findAny();
				if (!optionalShmMiscLineItem.isPresent()) {
					notDeleted.add(shmMiscLineItem);
				}

			});
			String tranCode = "";
			if (CollectionUtils.isNotEmpty(shmMiscLineItemsToUpdate)) {
				LoadValuesToUpdate<ShmMiscLineItem, ShmMiscLineItem> loadValuesToUpdate = null;
				if (ShipmentUpdateActionCd.MANUAL_RATE.equals(shipmentUpdateActionCd)) {
					tranCode = MANRATE_TRAN_CD;
					loadValuesToUpdate = new LoadValuesToUpdateManRateMiscImpl();
				} else if (ShipmentUpdateActionCd.CORRECTION.equals(shipmentUpdateActionCd)){
					tranCode = CORRECTION_TRAN_CD;
					loadValuesToUpdate = new LoadValuesToUpdateCorrectionMiscImpl();
				}else if (ShipmentUpdateActionCd.AUTO_RATE.equals(shipmentUpdateActionCd)){
					loadValuesToUpdate = new LoadValuesToUpdateAutoRateMiscImpl();
					tranCode = AUTORATE_TRAN_CD;
				}
				notDeletedUpdated.addAll(loadValuesToUpdate.load(notDeleted,
						shmMiscLineItemsToUpdate,
						fromDelete,
						userId, tranCode));

			} else {
				notDeletedUpdated.addAll(notDeleted);
			}

			notDeletedUpdated.addAll(shmMiscLineItemsToAdd);

			resetSeqNumberMiscLineItem(shipmentInstId, notDeletedUpdated);
		}

		return notDeletedUpdated;

	}

	@LogExecutionTime
	public List<ShmMiscLineItem> shmMiscLineItemListToAdd(
			Long shipmentInstId,
			final List<MiscLineItem> miscLineItems,
			final List<ShmMiscLineItem> shmMiscLineItems,
			ShmShipment shmShipment,
			long seqNumber,
			String userId) throws ServiceException {
		List<ShmMiscLineItem> result = new ArrayList<>();
		Timestamp timestamp = new Timestamp(new Date().getTime());
		if (CollectionUtils.isNotEmpty(miscLineItems) && CollectionUtils.isNotEmpty(shmMiscLineItems)) {

			List<String> lnTypCdDbList = shmMiscLineItems
					.stream()
					.map(ShmMiscLineItem::getLnTypCd)
					.collect(Collectors.toList());
			List<String> miscLineItemCdList = miscLineItems
					.stream()
					.map(miscLineItem -> getMiscLineItemCd(miscLineItem.getLineTypeCd().value()))
					.collect(Collectors.toList());
			List<String> newCodes = miscLineItemCdList
					.stream()
					.filter(a -> !lnTypCdDbList.contains(a))
					.collect(Collectors.toList());
			if (CollectionUtils.isNotEmpty(newCodes)) {
				List<MiscLineItem> miscLineItemsFiltered = miscLineItems
						.stream()
						.filter(miscLineItem -> newCodes.contains(getMiscLineItemCdAlt(miscLineItem
								.getLineTypeCd()
								.value())))
						.collect(Collectors.toList());
				miscLineItemsFiltered.forEach(miscLineItem -> miscLineItem.setListActionCd(ActionCd.ADD));

				List<ShmMiscLineItem> toShmMiscLineItem = new ArrayList<>();
				toShmMiscLineItem = DtoTransformer.toShmMiscLineItem(miscLineItemsFiltered, toShmMiscLineItem);

				for (ShmMiscLineItem shmMiscLineItem : toShmMiscLineItem) {
					seqNumber++;
					shmMiscLineItem.getId().setSeqNbr(seqNumber);
					shmMiscLineItem.getId().setShpInstId(shipmentInstId);
					shmMiscLineItem.setShmShipment(shmShipment);
					shmMiscLineItem.setUom(getUnitOfMeasureByLineType(shmMiscLineItem.getLnTypCd()));
					setDefaultValuesMiscLineItem(userId, timestamp, shmMiscLineItem);
					result.add(shmMiscLineItem);

				}
			}

		} else {

			miscLineItems.forEach(miscLineItem -> miscLineItem.setListActionCd(ActionCd.ADD));
			List<ShmMiscLineItem> toShmMiscLineItem = DtoTransformer.toShmMiscLineItem(miscLineItems, result);

			for (ShmMiscLineItem shmMiscLineItem : toShmMiscLineItem) {
				seqNumber++;
				shmMiscLineItem.setId(new ShmMiscLineItemPK());
				shmMiscLineItem.getId().setShpInstId(shipmentInstId);
				shmMiscLineItem.getId().setSeqNbr(seqNumber);
				shmMiscLineItem.setShmShipment(shmShipment);
				shmMiscLineItem.setUom(getUnitOfMeasureByLineType(shmMiscLineItem.getLnTypCd()));
				setDefaultValuesMiscLineItem(userId, timestamp, shmMiscLineItem);
				result.add(shmMiscLineItem);
			}

		}

		return result;
	}

	@LogExecutionTime
	protected List<String> compareShmMiscLineItem(ShmMiscLineItem source, ShmMiscLineItem target) {
		Comparator<ShmMiscLineItem> comparator = ComparatorFactory.createStrictComparator();
		List<String> differences = findDifferences(source, target, comparator);
		logMsg(ShmMiscLineItem.class.getName(), differences, source.getId().getShpInstId());
		return differences;
	}

	protected List<MiscLineItem> getMiscLineItemListFilter(
			List<MiscLineItem> miscLineItems, List<MiscLineItemCd> miscLineItemCds) {
		return miscLineItems
				.stream()
				.filter(miscLineItem -> miscLineItemCds.contains(miscLineItem.getLineTypeCd()))
				.collect(Collectors.toList());
	}

	protected List<ShmMiscLineItem> getShmMiscLineItemListFilter(
			List<ShmMiscLineItem> shmMiscLineItemsOriginal, List<String> miscLineItemCds) {
		return shmMiscLineItemsOriginal
				.stream()
				.filter(shmMiscLineItem -> miscLineItemCds.contains(shmMiscLineItem.getLnTypCd()))
				.collect(Collectors.toList());

	}

	public static String getUnitOfMeasureByLineType(MiscLineItemCd lineTypeCd) {

		if (Objects.nonNull(lineTypeCd)) {
			switch (lineTypeCd) {
			case AS_WGT:
			case DEFICIT_WGT:
				return UOM_POUNDS_CD;
			case COD_AMT:
			case DISC_LN:
			case PART_COLL_LN:
			case PART_PPD_LN:
				return UOM_COUNT_CD;
			default:
				return UOM_COUNT_NONE;
			}

		}
		return UOM_COUNT_NONE;
	}
	public static String getUnitOfMeasureByLineType(String lineTypeCd) {
		if (Objects.nonNull(lineTypeCd)) {

			String miscLineItemCdName = getMiscLineItemCd(lineTypeCd);
			return getUnitOfMeasureByLineType(MiscLineItemCd.fromValue(miscLineItemCdName));
		}
		return UOM_COUNT_NONE;
	}
}
