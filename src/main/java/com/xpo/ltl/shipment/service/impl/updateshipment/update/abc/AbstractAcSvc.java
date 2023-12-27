package com.xpo.ltl.shipment.service.impl.updateshipment.update.abc;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmAcSvc;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvcPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.v2.AccessorialService;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.shipment.service.dao.ShipmentAcSvcSubDAO;
import com.xpo.ltl.shipment.service.impl.updateshipment.comparator.Comparator;
import com.xpo.ltl.shipment.service.impl.updateshipment.comparator.ComparatorFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.functional.LoadValuesToUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.functional.LoadValuesToUpdateCorrectionAcSvcImpl;
import com.xpo.ltl.shipment.service.impl.updateshipment.functional.LoadValuesToUpdateManRateAcSvcImpl;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.persistence.EntityManager;
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

public abstract class AbstractAcSvc extends AbstractUpdate {

	@Inject
	private ShipmentAcSvcSubDAO shipmentAcSvcSubDAO;

	public static void setDefaultValuesAcSvc(
			String userId, Timestamp timestamp, ShmAcSvc shmAcSvc) {
		shmAcSvc.setDmlTmst(timestamp);
		shmAcSvc.setDtlCapxtimestamp(timestamp);
		shmAcSvc.setLstUpdtTmst(timestamp);
		shmAcSvc.setReplLstUpdtTmst(timestamp);

		shmAcSvc.setLstUpdtUid(userId);

		if (StringUtils.isEmpty(shmAcSvc.getChrgToCd())) {
			shmAcSvc.setChrgToCd(StringUtils.SPACE);
		}
		if (StringUtils.isEmpty(shmAcSvc.getArchiveCntlCd())) {
			shmAcSvc.setArchiveCntlCd(StringUtils.SPACE);
		}
		if (StringUtils.isEmpty(shmAcSvc.getMinChrgInd())) {
			shmAcSvc.setMinChrgInd(getFlag(false));
		}
		if (StringUtils.isEmpty(shmAcSvc.getAcUom())) {
			shmAcSvc.setAcUom(StringUtils.SPACE);
		}
		if (StringUtils.isEmpty(shmAcSvc.getDescTxt())) {
			shmAcSvc.setDescTxt(StringUtils.SPACE);
		}
		if (Objects.isNull(shmAcSvc.getTrfRt())) {
			shmAcSvc.setTrfRt(BigDecimal.ZERO);
		}
		if (Objects.isNull(shmAcSvc.getAcQty())) {
			shmAcSvc.setAcQty(BigDecimal.ZERO);
		}
		if (Objects.isNull(shmAcSvc.getPpdPct())) {
			shmAcSvc.setPpdPct(BigDecimal.ZERO);
		}

	}

	public static List<ShmAcSvc> resetSeqNumberAccessorialService(Long shipmentInstId, List<ShmAcSvc> shmAcSvcs) {
		List<ShmAcSvc> newList = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(shmAcSvcs)) {

			sortListByLongField(shmAcSvcs, ShmAcSvc::getId, ShmAcSvcPK::getSeqNbr);
			AtomicReference<Long> seq = new AtomicReference<>(1L);
			shmAcSvcs.forEach(shmAcSvc -> {
				ShmAcSvc shmAcSvcNew = new ShmAcSvc();
				copyFields(shmAcSvc, shmAcSvcNew);

				ShmAcSvcPK shmAcSvcPK = new ShmAcSvcPK();
				shmAcSvcPK.setShpInstId(shipmentInstId);
				shmAcSvcPK.setSeqNbr(seq.getAndSet(seq.get() + 1));
				shmAcSvcNew.setId(shmAcSvcPK);
				newList.add(shmAcSvcNew);

			});
		}
		return newList;
	}

	@LogExecutionTime
	public void addAccessorials(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAcSvc> accessorialServiceToAddList,
			String transactionCd,
			String userId) throws ValidationException {
		Timestamp timestamp = new Timestamp(new Date().getTime());

		if (CollectionUtils.isNotEmpty(accessorialServiceToAddList)) {
			accessorialServiceToAddList.forEach(shmAcSvc -> {
				shmAcSvc.setLstUpdtTmst(timestamp);
				shmAcSvc.setLstUpdtTranCd(transactionCd);
				shmAcSvc.setLstUpdtUid(userId);

			});

			try {
				shipmentAcSvcSubDAO.persist(accessorialServiceToAddList, entityManager);
				accessorialServiceToAddList.forEach(shmAcSvc -> {
					setDefaultValuesAcSvc(userId, timestamp, shmAcSvc);
					shmAcSvc.setLstUpdtTranCd(transactionCd);
					if (appContext.getDb2CommitEnabledForUpdateShipment()) {
						shipmentAcSvcSubDAO.createDB2ShmAcSvc(shmAcSvc, db2EntityManager);
					}
				});
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
	}

	@LogExecutionTime
	public void deleteAccessorials(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAcSvc> accessorialServiceToDeleteList) {
		if (CollectionUtils.isNotEmpty(accessorialServiceToDeleteList)) {
			shipmentAcSvcSubDAO.remove(accessorialServiceToDeleteList, entityManager);
			if (appContext.getDb2CommitEnabledForUpdateShipment()) {
				accessorialServiceToDeleteList.forEach(shmAcSvc -> {
					try {

						final Timestamp exadataLstUpdtTmst = new Timestamp(new Date().getTime());
						final Function<DB2ShmAcSvc, Boolean> checkVersionFunction = getCheckVersionFunction(
								exadataLstUpdtTmst);
						shipmentAcSvcSubDAO.deleteDB2ShmAcSvc(shmAcSvc.getId(),
								checkVersionFunction,
								db2EntityManager,
								transactionContext);
					} catch (Exception e) {
						throw new IllegalStateException(e);
					}

				});
			}
		}

	}

	public Function<DB2ShmAcSvc, Boolean> getCheckVersionFunction(final Timestamp exadataLstUpdtTmst) {
		return db2Entity -> db2Entity.getLstUpdtTmst().compareTo(exadataLstUpdtTmst) <= 0;
	}

	public List<ShmAcSvc> getAbcShmAcSvcListToDelete(
			final List<AccessorialService> accessorialServices, final List<ShmAcSvc> shmAcSvcs) {
		List<ShmAcSvc> result = new ArrayList<>();

		if (CollectionUtils.isNotEmpty(accessorialServices) && (CollectionUtils.isNotEmpty(shmAcSvcs))) {
			List<String> lnTypCdDbList = shmAcSvcs.stream().map(ShmAcSvc::getAcCd).collect(Collectors.toList());
			List<String> accessorialServiceCdList = accessorialServices
					.stream()
					.map(AccessorialService::getAccessorialCd)
					.collect(Collectors.toList());
			List<String> deletedCodes = lnTypCdDbList
					.stream()
					.filter(a -> !accessorialServiceCdList.contains(a))
					.collect(Collectors.toList());
			result = shmAcSvcs
					.stream()
					.filter(shmAcSvc -> deletedCodes.contains(shmAcSvc.getAcCd()))
					.collect(Collectors.toList());

		}else if (CollectionUtils.isEmpty(accessorialServices) && (CollectionUtils.isNotEmpty(shmAcSvcs))){
			return shmAcSvcs;
		}
		return result;
	}

	public List<ShmAcSvc> resetAbcSeqNumberShmAcSvcList(
			Long shipmentInstId,
			final List<ShmAcSvc> shmAcSvcsToDeleted,
			final List<ShmAcSvc> shmAcSvcsOriginal,
			final List<ShmAcSvc> shmAcSvcsToUpdate,
			final List<ShmAcSvc> shmAcSvcsToAdd,
			boolean fromDelete,
			String userId,
			ShipmentUpdateActionCd shipmentUpdateActionCd) {
		List<ShmAcSvc> result = new ArrayList<>();
		List<ShmAcSvc> notDeleted = new ArrayList<>();
		List<ShmAcSvc> notDeletedUpdated = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(shmAcSvcsToDeleted) && CollectionUtils.isNotEmpty(shmAcSvcsOriginal)) {
			shmAcSvcsOriginal.forEach(shmAcSvc -> {

				Optional<ShmAcSvc> optionalShmAcSvc = shmAcSvcsToDeleted
						.stream()
						.filter(shmAcSvcToDelete ->
								shmAcSvcToDelete.getId().getShpInstId() == shmAcSvc.getId().getShpInstId()
										&& shmAcSvcToDelete.getId().getSeqNbr() == shmAcSvc.getId().getSeqNbr())
						.findAny();
				if (!optionalShmAcSvc.isPresent()) {
					notDeleted.add(shmAcSvc);
				}

			});
			if (CollectionUtils.isNotEmpty(shmAcSvcsToUpdate)) {
				LoadValuesToUpdate<ShmAcSvc, ShmAcSvc> loadValuesToUpdate = null;
				if (ShipmentUpdateActionCd.MANUAL_RATE.equals(shipmentUpdateActionCd)) {
					loadValuesToUpdate = new LoadValuesToUpdateManRateAcSvcImpl();
				} else if (ShipmentUpdateActionCd.CORRECTION.equals(shipmentUpdateActionCd)){
					loadValuesToUpdate = new LoadValuesToUpdateCorrectionAcSvcImpl();
				}
				notDeletedUpdated.addAll(loadValuesToUpdate.load(notDeleted, shmAcSvcsToUpdate, fromDelete, userId, ShipmentUpdateActionCd.MANUAL_RATE.equals(shipmentUpdateActionCd) ? MANRATE_TRAN_CD : CORRECTION_TRAN_CD ));

			} else {
				notDeletedUpdated.addAll(notDeleted);
			}

			notDeletedUpdated.addAll(shmAcSvcsToAdd);

			result.addAll(resetSeqNumberAccessorialService(shipmentInstId, notDeletedUpdated));
		}

		return result;

	}

	public List<ShmAcSvc> getAbcShmAcSvcListToAdd(
			Long shipmentInstId,
			final List<AccessorialService> accessorialServices,
			final List<ShmAcSvc> shmAcSvcs,
			ShmShipment shmShipment,
			long seqNumber,
			String userId) throws ServiceException {
		List<ShmAcSvc> result = new ArrayList<>();
		Timestamp timestamp = new Timestamp(new Date().getTime());
		if (CollectionUtils.isNotEmpty(accessorialServices) && CollectionUtils.isNotEmpty(shmAcSvcs)) {

			List<String> lnTypCdDbList = shmAcSvcs.stream().map(ShmAcSvc::getAcCd).collect(Collectors.toList());
			List<String> accessorialServiceCdList = accessorialServices
					.stream()
					.map(AccessorialService::getAccessorialCd)
					.collect(Collectors.toList());
			List<String> newCodes = accessorialServiceCdList
					.stream()
					.filter(a -> !lnTypCdDbList.contains(a))
					.collect(Collectors.toList());
			if (CollectionUtils.isNotEmpty(newCodes)) {
				List<AccessorialService> accessorialServicesFiltered = accessorialServices
						.stream()
						.filter(accessorialService -> newCodes.contains((accessorialService.getAccessorialCd())))
						.collect(Collectors.toList());
				accessorialServicesFiltered.forEach(accessorialService -> accessorialService.setListActionCd(ActionCd.ADD));

				List<ShmAcSvc> toShmAcSvc = new ArrayList<>();
				toShmAcSvc = DtoTransformer.toShmAcSvc(accessorialServicesFiltered, toShmAcSvc);

				for (ShmAcSvc shmAcSvc : toShmAcSvc) {
					seqNumber++;
					shmAcSvc.getId().setSeqNbr(seqNumber);
					shmAcSvc.getId().setShpInstId(shipmentInstId);
					shmAcSvc.setShmShipment(shmShipment);
					setDefaultValuesAcSvc(userId, timestamp, shmAcSvc);
					result.add(shmAcSvc);

				}
			}

		} else {
			accessorialServices.forEach(accessorialService -> accessorialService.setListActionCd(ActionCd.ADD));
			List<ShmAcSvc> toShmAcSvc = DtoTransformer.toShmAcSvc(accessorialServices, result);

			for (ShmAcSvc shmAcSvc : toShmAcSvc) {
				seqNumber++;
				shmAcSvc.setId(new ShmAcSvcPK());
				shmAcSvc.getId().setShpInstId(shipmentInstId);
				shmAcSvc.getId().setSeqNbr(seqNumber);
				shmAcSvc.setShmShipment(shmShipment);
				setDefaultValuesAcSvc(userId, timestamp, shmAcSvc);
				result.add(shmAcSvc);
			}
		}

		return result;
	}

	@LogExecutionTime
	public void updateAccessorials(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAcSvc> accessorialServiceToUpdateList,
			String transactionCd,
			String userId) throws ValidationException {
		if (CollectionUtils.isNotEmpty(accessorialServiceToUpdateList)) {
			accessorialServiceToUpdateList.forEach(shmAcSvc -> {
				shmAcSvc.setLstUpdtTmst(new Timestamp(new Date().getTime()));
				shmAcSvc.setLstUpdtTranCd(transactionCd);
				shmAcSvc.setLstUpdtUid(userId);

			});
			shipmentAcSvcSubDAO.persist(accessorialServiceToUpdateList, entityManager);
			if (appContext.getDb2CommitEnabledForUpdateShipment()) {
				accessorialServiceToUpdateList.forEach(shmAcSvc -> {
					try {

						final Timestamp exadataLstUpdtTmst = new Timestamp(new Date().getTime());

						final Function<DB2ShmAcSvc, Boolean> checkVersionFunction = getCheckVersionFunction(
								exadataLstUpdtTmst);
						shipmentAcSvcSubDAO.updateDB2ShmAcSvc(shmAcSvc,
								checkVersionFunction,
								db2EntityManager,
								transactionContext);
					} catch (Exception e) {
						throw new IllegalStateException(e);

					}
				});
			}
		}
	}

	protected List<String> compareShmAcSvc(ShmAcSvc source, ShmAcSvc target) {
		Comparator<ShmAcSvc> comparator = ComparatorFactory.createStrictComparator();
		List<String> differences = findDifferences(source, target, comparator);
		logMsg(ShmAcSvc.class.getName(), differences, source.getId().getShpInstId());
		return differences;
	}
}
