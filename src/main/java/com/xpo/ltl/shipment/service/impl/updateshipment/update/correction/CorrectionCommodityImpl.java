//package com.xpo.ltl.shipment.service.impl.updateshipment.update.correction;
//
//import com.xpo.ltl.api.exception.ServiceException;
//import com.xpo.ltl.api.exception.ValidationException;
//import com.xpo.ltl.api.rest.TransactionContext;
//import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
//import com.xpo.ltl.api.shipment.service.entity.ShmCommodity;
//import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
//import com.xpo.ltl.api.shipment.v2.ActionCd;
//import com.xpo.ltl.api.shipment.v2.Commodity;
//import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
//import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
//import com.xpo.ltl.shipment.service.impl.updateshipment.update.CommodityTransactions;
//import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractCommodity;
//import org.apache.commons.collections4.CollectionUtils;
//
//import javax.enterprise.context.RequestScoped;
//import javax.persistence.EntityManager;
//import java.math.BigDecimal;
//import java.sql.Timestamp;
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Optional;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.atomic.AtomicReference;
//import java.util.stream.Collectors;
//
//@RequestScoped
//public class CorrectionCommodityImpl extends AbstractCommodity implements CommodityTransactions {
//
//	@Override
//	public List<ShmCommodity> getCommoditiesToDelete(List<Commodity> commodities, List<ShmCommodity> shmCommodities) {
//		return this.getAbcCommoditiesForDelete(commodities, shmCommodities);
//	}
//
//	@Override
//	public CompletableFuture<List<ShmCommodity>> cfGetCommoditiesToDelete(
//			List<Commodity> commodities, List<ShmCommodity> shmCommodities) throws ServiceException {
//		return CompletableFuture.supplyAsync(() -> this.getCommoditiesToDelete(commodities, shmCommodities));
//	}
//
//	@Override
//	public List<ShmCommodity> getCommoditiesToInsert(
//			List<Commodity> commodities,
//			List<ShmCommodity> shmCommodities,
//			ShmShipment shipment) throws ServiceException {
//		return getAbcCommoditiesForInsert(shipment, commodities);
//	}
//
//	@Override
//	public CompletableFuture<List<ShmCommodity>> cfGetCommoditiesToInsert(
//			List<Commodity> commodities,
//			List<ShmCommodity> shmCommodities,
//			ShmShipment shipment,
//			TransactionContext transactionContext) throws ServiceException {
//		return CompletableFuture.supplyAsync(() -> {
//			try {
//				return this.getCommoditiesToInsert(commodities, shmCommodities, shipment);
//			} catch (ServiceException e) {
//				getException(ServiceErrorMessage.COMMODITY_UPDATE_FAILED,
//						this.getClass().getSimpleName(),
//						e,
//						transactionContext);
//			}
//			return null;
//		});
//	}
//
//	@Override
//	public Map<ActionCd, List<ShmCommodity>> getAllTx(
//			List<ShmCommodity> shmCommoditiesOriginal,
//			ShmShipment shipment,
//			UpdateShipmentRqst updateShipmentRqst,
//			String userId,
//			String transactionCd) throws ServiceException {
//
//		Map<ActionCd, List<ShmCommodity>> result = new HashMap<>();
//		List<ShmCommodity> shmCommoditiesToInsert = getCommoditiesToInsert(updateShipmentRqst.getCommodities(),
//				shmCommoditiesOriginal,
//				shipment);
//		List<ShmCommodity> shmCommoditiesToDelete = getCommoditiesToDelete(updateShipmentRqst.getCommodities(),
//				shmCommoditiesOriginal);
//		List<ShmCommodity> shmCommoditiesToUpdate = getShmCommoditiesToUpdate(updateShipmentRqst,
//				userId,
//				shmCommoditiesOriginal,
//				transactionCd);
//		List<ShmCommodity> notDeleted = new ArrayList<>();
//		List<ShmCommodity> notDeletedUpdated = new ArrayList<>();
//
//		if (CollectionUtils.isNotEmpty(shmCommoditiesToDelete)) {
//			if (CollectionUtils.isNotEmpty(shmCommoditiesToDelete)
//					&& CollectionUtils.isNotEmpty(shmCommoditiesOriginal)) {
//				shmCommoditiesOriginal.forEach(shmCommodity -> {
//
//					Optional<ShmCommodity> optionalShmCommodity = shmCommoditiesToDelete
//							.stream()
//							.filter(shmCommodityToDelete ->
//									shmCommodityToDelete.getId().getShpInstId() == shmCommodity.getId().getShpInstId()
//											&& shmCommodityToDelete.getId().getSeqNbr() == shmCommodity
//											.getId()
//											.getSeqNbr())
//							.findAny();
//					if (!optionalShmCommodity.isPresent()) {
//						notDeleted.add(shmCommodity);
//					}
//
//				});
//				if (CollectionUtils.isNotEmpty(shmCommoditiesToUpdate)) {
//					List<ShmCommodity> diff = notDeleted
//							.stream()
//							.filter(shmCommodity1 -> shmCommoditiesToUpdate
//									.stream()
//									.noneMatch(shmCommodity2 ->
//											shmCommodity1.getId().getShpInstId() == shmCommodity2.getId().getShpInstId()
//													&& shmCommodity1.getId().getSeqNbr() == shmCommodity2
//													.getId()
//													.getSeqNbr()))
//							.collect(Collectors.toList());
//					notDeletedUpdated.addAll(shmCommoditiesToUpdate);
//					notDeletedUpdated.addAll(diff);
//
//				} else {
//					notDeletedUpdated.addAll(notDeleted);
//				}
//
//				notDeletedUpdated.addAll(shmCommoditiesToInsert);
//
//				result.put(ActionCd.DELETE, shmCommoditiesOriginal);
//				result.put(ActionCd.ADD, resetSeqNumberCommodities(shipment.getShpInstId(), notDeletedUpdated, shipment));
//
//			}
//		} else {
//			Optional<ShmCommodity> optionalShmCommodity = shmCommoditiesOriginal
//					.stream()
//					.max(Comparator.comparingLong(o -> o.getId().getSeqNbr()));
//			AtomicReference<Long> seqNumber = new AtomicReference<>(optionalShmCommodity
//					.map(commodity -> commodity.getId().getSeqNbr())
//					.orElse(0L));
//			shmCommoditiesToInsert.forEach(shmCommodity -> {
//				seqNumber.getAndSet(seqNumber.get() + 1);
//				shmCommodity.getId().setSeqNbr(seqNumber.get());
//			});
//			result.put(ActionCd.ADD, shmCommoditiesToInsert);
//			result.put(ActionCd.UPDATE, shmCommoditiesToUpdate);
//		}
//
//		return result;
//	}
//
//	@Override
//	public CompletableFuture<Map<ActionCd, List<ShmCommodity>>> cfGetAllTx(
//			List<ShmCommodity> shmCommodities,
//			ShmShipment shipment,
//			UpdateShipmentRqst updateShipmentRqst,
//			String userId,
//			String transactionCd,
//			TransactionContext transactionContext) throws ServiceException {
//		return CompletableFuture.supplyAsync(() -> {
//			try {
//				return getAllTx(shmCommodities, shipment, updateShipmentRqst, userId, transactionCd);
//			} catch (ServiceException e) {
//				getException(ServiceErrorMessage.COMMODITY_UPDATE_FAILED,
//						this.getClass().getSimpleName(),
//						e,
//						transactionContext);
//			}
//			return null;
//		});
//	}
//
//	@Override
//	public void delete(
//			EntityManager entityManager,
//			EntityManager db2EntityManager,
//			TransactionContext transactionContext,
//			List<ShmCommodity> commodities) {
//
//		this.deleteCommodities(entityManager, db2EntityManager, transactionContext, commodities);
//	}
//
//	@Override
//	public List<ShmCommodity> getShmCommoditiesToUpdate(
//			UpdateShipmentRqst updateShipmentRqst,
//			String userId,
//			List<ShmCommodity> commodities,
//			String transactionCd) {
//		List<ShmCommodity> result = new ArrayList<>();
//		updateShipmentRqst.getCommodities().forEach(commodity -> {
//
//			Optional<ShmCommodity> shmCommodityOptional = commodities
//					.stream()
//					.filter(shmCommodity -> Objects.nonNull(commodity.getSequenceNbr())
//							&& shmCommodity.getId().getSeqNbr() == commodity.getSequenceNbr().longValue()
//							&& shmCommodity.getId().getShpInstId() == commodity.getShipmentInstId())
//					.findAny();
//
//			if (shmCommodityOptional.isPresent()) {
//
//				ShmCommodity shmCommodity = shmCommodityOptional.get();
//				ShmCommodity shmCommodityToCheck = new ShmCommodity();
//
//				copyFields(shmCommodity, shmCommodityToCheck);
//
//				setValues(commodity, shmCommodity);
//
//				List<String> diff = this.compareCommodity(shmCommodity, shmCommodityToCheck);
//
//				if (CollectionUtils.isNotEmpty(diff)) {
//					shmCommodity.setLstUpdtTranCd(transactionCd);
//					shmCommodity.setLstUpdtUid(userId);
//					shmCommodity.setLstUpdtTmst(new Timestamp(new Date().getTime()));
//					result.add(shmCommodity);
//				}
//			}
//		});
//
//		return result;
//	}
//
//	private void setValues(Commodity commodity, ShmCommodity shmCommodity) {
//		if (Objects.nonNull(commodity.getAmount())) { //AMT
//			shmCommodity.setAmt(BigDecimal.valueOf(commodity.getAmount()));
//		}
//
//		if (Objects.nonNull(commodity.getTariffsRate())) {//TRF_RT
//			shmCommodity.setTrfRt(BigDecimal.valueOf(commodity.getTariffsRate()));
//		}
//		if (Objects.nonNull(commodity.getPiecesCount())) {//PCS
//			shmCommodity.setPcsCnt(BigDecimal.valueOf(commodity.getPiecesCount().longValue()));
//		}
//		if (Objects.nonNull(commodity.getPackageCd())) {//PKG_CD
//			shmCommodity.setPkgCd(getCommodityPackageCd(commodity.getPackageCd().value()));
//		}
//		if (Objects.nonNull(commodity.getHazardousMtInd())) {//HZ_MT_IND
//			shmCommodity.setHzMtInd(getFlag(commodity.getHazardousMtInd()));
//		}
//
//		shmCommodity.setDescTxt(commodity.getDescription());//DESC_TXT
//		shmCommodity.setNmfcItmCd(commodity.getNmfcItemCd());//NMFC_ITM_CD
//
//		if (Objects.nonNull(commodity.getClassType())) {//CLASS_TYP
//			shmCommodity.setClassTyp(getCommodityClassCd(commodity.getClassType().value()));
//		}
//
//		if (Objects.nonNull(commodity.getWeightLbs())) {//WGT_LBS
//			shmCommodity.setWgtLbs(BigDecimal.valueOf(commodity.getWeightLbs()));
//		}
//	}
//
//	@Override
//	public CompletableFuture<List<ShmCommodity>> getShmCommoditiesToUpdateCf(
//			UpdateShipmentRqst updateShipmentRqst,
//			String userId,
//			List<ShmCommodity> commodities,
//			String transactionCd,
//			ShipmentUpdateActionCd shipmentUpdateActionCd) {
//		return CompletableFuture.supplyAsync(() -> getShmCommoditiesToUpdate(updateShipmentRqst,
//				userId,
//				commodities,
//				transactionCd));
//	}
//
//	@Override
//	public void update(
//			EntityManager entityManager,
//			EntityManager db2EntityManager,
//			TransactionContext transactionContext,
//			List<ShmCommodity> commodities,
//			String transactionCd,
//			String userId) throws ValidationException {
//
//		this.updateCommodities(entityManager, db2EntityManager, transactionContext, commodities, transactionCd, userId);
//
//	}
//
//	@Override
//	public void insert(
//			EntityManager entityManager,
//			EntityManager db2EntityManager,
//			TransactionContext transactionContext,
//			List<ShmCommodity> shmCommodities,
//			String transactionCd,
//			String userId) throws ValidationException {
//		this.insertCommodities(entityManager, db2EntityManager, transactionContext, shmCommodities, transactionCd,userId);
//	}
//}
