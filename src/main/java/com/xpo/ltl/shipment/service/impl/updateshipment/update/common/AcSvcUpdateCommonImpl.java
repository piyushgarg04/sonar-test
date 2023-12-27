package com.xpo.ltl.shipment.service.impl.updateshipment.update.common;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvcPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.v2.AccessorialService;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.shipment.service.dao.ShmMiscLineItemSubDAO;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.AcSvcUpdate;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.LoadValues;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractAcSvc;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.LoadValFactory;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.LoadValuesFactory;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
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
public class AcSvcUpdateCommonImpl extends AbstractAcSvc implements AcSvcUpdate {

	@Inject
	private ShmMiscLineItemSubDAO shmMiscLineItemSubDAO;

	@Inject
	private LoadValFactory loadValFactory;

	private static void setUpdateSystemValues(String racfId, String transactionCode, ShmAcSvc shmAcSvc) {
		shmAcSvc.setLstUpdtTmst(new Timestamp(new Date().getTime()));
		shmAcSvc.setLstUpdtTranCd(transactionCode);
		shmAcSvc.setLstUpdtUid(racfId);
	}

	@LogExecutionTime
	@Override
	public Map<ActionCd, List<ShmAcSvc>> getItemsForTransactions(
			Long shipmentInstId,
			List<AccessorialService> accessorialServiceList,
			List<ShmAcSvc> shmAcSvcsOriginal,
			ShmShipment shipment,
			String userId,
			ShipmentUpdateActionCd shipmentUpdateActionCd,
			String transactionCode,
			TransactionContext transactionContext) throws ServiceException {

		EnumMap<ActionCd, List<ShmAcSvc>> result = new EnumMap<>(ActionCd.class);
		List<ShmAcSvc> shmAcSvcListToDelete = this.getAbcShmAcSvcListToDelete(accessorialServiceList, shmAcSvcsOriginal);

		long seqNumber = getMaxSequenceNumberLong(shmAcSvcsOriginal
				.stream()
				.map(shmAcSvc -> shmAcSvc.getId().getSeqNbr())
				.collect(Collectors.toList()));

		List<ShmAcSvc> shmAcSvcListToUpdate = this.getShmAcSvcListToUpdate(accessorialServiceList,
				shmAcSvcsOriginal,
				CollectionUtils.isNotEmpty(shmAcSvcListToDelete),
				userId,
				shipmentUpdateActionCd,
				transactionCode,
				transactionContext);

		List<ShmAcSvc> shmAcSvcListToAdd = this.getAbcShmAcSvcListToAdd(shipmentInstId,
				accessorialServiceList,
				shmAcSvcsOriginal,
				shipment,
				seqNumber,
				userId);

		if (CollectionUtils.isNotEmpty(shmAcSvcListToDelete)) {
			List<ShmAcSvc> shmAcSvcListResetSeqNumber = this.resetAbcSeqNumberShmAcSvcList(shipmentInstId,
					shmAcSvcListToDelete,
					shmAcSvcsOriginal,
					shmAcSvcListToUpdate,
					shmAcSvcListToAdd,
					true,
					userId,
					shipmentUpdateActionCd);

			result.put(ActionCd.DELETE, shmAcSvcsOriginal);
			shmAcSvcListToAdd = new ArrayList<>(shmAcSvcListResetSeqNumber);
			shmAcSvcListToUpdate = new ArrayList<>();

		}
		result.put(ActionCd.ADD, shmAcSvcListToAdd);
		result.put(ActionCd.UPDATE, shmAcSvcListToUpdate);
		return result;
	}

	@Override
	public CompletableFuture<Map<ActionCd, List<ShmAcSvc>>> cfGetItemsForTransactions(
			Long shipmentInstId,
			List<AccessorialService> accessorialServiceList,
			List<ShmAcSvc> shmAcSvcsOriginal,
			ShmShipment shipment,
			String userId,
			ShipmentUpdateActionCd shipmentUpdateActionCd,
			String transactionCode,
			TransactionContext transactionContext) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return getItemsForTransactions(shipmentInstId,
						accessorialServiceList,
						shmAcSvcsOriginal,
						shipment,
						userId,
						shipmentUpdateActionCd,
						transactionCode,
						transactionContext);
			} catch (Exception e) {
				getException(ServiceErrorMessage.AC_SVC_UPDATE_FAILED,
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
			List<ShmAcSvc> accessorialServiceToAddList,
			String transactionCd,
			String userId) throws ValidationException {
		this.addAccessorials(entityManager,
				db2EntityManager,
				transactionContext,
				accessorialServiceToAddList,
				transactionCd,
				userId);
	}

	@Override
	public void update(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAcSvc> accessorialServiceToUpdateList,
			String transactionCd,
			String userId) throws ValidationException {
		this.updateAccessorials(entityManager,
				db2EntityManager,
				transactionContext,
				accessorialServiceToUpdateList,
				transactionCd,
				userId);

	}

	@Override
	public void delete(
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			List<ShmAcSvc> accessorialServiceToDeleteList) {

		this.deleteAccessorials(entityManager, db2EntityManager, transactionContext, accessorialServiceToDeleteList);

	}

	private List<ShmAcSvc> getShmAcSvcListToUpdate(
			final List<AccessorialService> accessorialServices,
			final List<ShmAcSvc> shmAcSvcs,
			boolean fromDelete,
			String racfId,
			ShipmentUpdateActionCd shipmentUpdateActionCd,
			String transactionCode,
			TransactionContext transactionContext) {
		List<ShmAcSvc> result = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(accessorialServices)) {

			List<String> acCdDbList = shmAcSvcs.stream().map(ShmAcSvc::getAcCd).collect(Collectors.toList());
			List<String> accessorialServicesCdList = accessorialServices
					.stream()
					.map(AccessorialService::getAccessorialCd)
					.collect(Collectors.toList());
			List<String> sameCodes = acCdDbList
					.stream()
					.filter(accessorialServicesCdList::contains)
					.collect(Collectors.toList());

			if (CollectionUtils.isNotEmpty(sameCodes)) {

				LoadValuesFactory loadValuesFactory = loadValFactory.getFactoryImplementation(shipmentUpdateActionCd);
				LoadValues<AccessorialService, ShmAcSvc> acSvcLoadValues = loadValuesFactory.getFactoryImplementation(
						ShmAcSvc.class);

				sameCodes.forEach(s -> {
					Optional<AccessorialService> optionalAccessorialService = accessorialServices
							.stream()
							.filter(accessorialService -> s.equals(accessorialService.getAccessorialCd()))
							.findAny();

					if (optionalAccessorialService.isPresent() && Objects.nonNull(optionalAccessorialService
							.get()
							.getShipmentInstId()) && Objects.nonNull(optionalAccessorialService
							.get()
							.getSequenceNbr())) {
						AccessorialService accessorialService = optionalAccessorialService.get();
						Optional<ShmAcSvc> optionalShmAcSvc = shmAcSvcs
								.stream()
								.filter(shmAcSvc -> s.equals(shmAcSvc.getAcCd())
										&& shmAcSvc.getId().getSeqNbr() == accessorialService
										.getSequenceNbr()
										.longValue()
										&& shmAcSvc.getId().getShpInstId() == accessorialService.getShipmentInstId())
								.findAny();

						if (optionalShmAcSvc.isPresent()) {

							ShmAcSvc shmAcSvcToCheck = new ShmAcSvc();
							ShmAcSvc shmAcSvc = optionalShmAcSvc.get();

							copyFields(shmAcSvc, shmAcSvcToCheck);
							acSvcLoadValues.loadtValues(accessorialService, shmAcSvc);

							List<String> diff = this.compareShmAcSvc(shmAcSvc, shmAcSvcToCheck);

							if (CollectionUtils.isNotEmpty(diff)) {
								setUpdateSystemValues(racfId, transactionCode, shmAcSvc);
								if (!fromDelete) {
									result.add(shmAcSvc);
								} else {
									accessorialService.setListActionCd(ActionCd.ADD);
									ShmAcSvc shmAcSvcToNew = new ShmAcSvc();
									try {
										shmAcSvcToNew = DtoTransformer.toShmAcSvc(accessorialService, shmAcSvcToNew);
									} catch (ServiceException e) {
										getException(ServiceErrorMessage.UNEXPECTED_EXCEPTION,
												"Accessorials",
												e,
												transactionContext);
									}
									copyFields(shmAcSvc, shmAcSvcToNew);
									ShmAcSvcPK id = new ShmAcSvcPK();
									id.setShpInstId(shmAcSvc.getId().getShpInstId());
									id.setSeqNbr(shmAcSvc.getId().getSeqNbr());
									shmAcSvcToNew.setId(id);

									result.add(shmAcSvcToNew);
								}

							}

						}

					}
				});
			}
		}

		return result;
	}




}