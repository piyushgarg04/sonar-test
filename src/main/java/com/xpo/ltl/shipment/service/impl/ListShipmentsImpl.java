package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.DataValidationErrorBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmNotification;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.service.entity.ShmTmDtCritical;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.v2.DataValidationError;
import com.xpo.ltl.api.shipment.v2.ListShipmentsResp;
import com.xpo.ltl.api.shipment.v2.ListShipmentsRqst;
import com.xpo.ltl.api.shipment.v2.ShipmentDetailCd;
import com.xpo.ltl.api.shipment.v2.ShipmentDetails;
import com.xpo.ltl.api.shipment.v2.TimeDateCritical;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.ShipmentTdcSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmNotificationSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentEagerLoadPlan;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShipmentDetailsDelegate;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.ShipmentUtil;
import com.xpo.ltl.shipment.service.validators.ShipmentRequestsValidator;

@ApplicationScoped
@LogExecutionTime
public class ListShipmentsImpl {

	@Inject
	private ShmShipmentSubDAO shmShipmentSubDAO;

	@Inject
	private ShipmentTdcSubDAO shipmentTdcSubDAO;

    @Inject
    private ShmNotificationSubDAO shmNotificationSubDAO;

    @Inject
    private ShipmentDetailsDelegate shipmentDetailsDelegate;

	@Inject
	private AppContext appContext;

	@Inject
	private ShipmentRequestsValidator shipmentsRequestValidator;

    public ListShipmentsResp listShipments(ListShipmentsRqst listShipmentsRqst,
                                           TransactionContext txnContext,
                                           EntityManager entityManager)
            throws ServiceException {
		checkNotNull(txnContext, "TransactionContext is required");
		checkNotNull(entityManager, "EntityManager is required");
		checkNotNull(listShipmentsRqst.getShipmentIds(), "ShipmentIds are required");

		List<ShmShipment> shipments = new ArrayList<>();
		final List<ShipmentDetailCd> shipmentDetailCds =
				Optional.ofNullable(listShipmentsRqst.getShipmentDetailCd())
						.orElse(Arrays.asList(ShipmentDetailCd.SHIPMENT_ONLY));

		List<Long> shipmentInstIds = listShipmentsRqst.getShipmentIds().stream()
				.filter(shipmentId -> StringUtils.isNotBlank(shipmentId.getShipmentInstId()))
				.map(shipmentId -> new Long(shipmentId.getShipmentInstId())).collect(Collectors.toList());


		final List<String> proNumbers = listShipmentsRqst.getShipmentIds()
			.stream()
				.filter(shipmentId -> StringUtils.isNotBlank(shipmentId.getProNumber()))
			.map(shipmentId -> shipmentId.getProNumber())
			.collect(Collectors.toList());

		EnumSet<ShipmentDetailCd> shipmentDetailCdsSet =
			CollectionUtils.isNotEmpty(shipmentDetailCds)
				? EnumSet.copyOf(shipmentDetailCds)
				: EnumSet.noneOf(ShipmentDetailCd.class);

        ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan =
            ShmShipmentEagerLoadPlan.from(shipmentDetailCdsSet, true);

		shipmentsRequestValidator.validateRequest(proNumbers, shipmentInstIds, shipmentDetailCds, txnContext);

		if (CollectionUtils.isNotEmpty(shipmentInstIds)) {
			for (List<Long> shipmentInstIdsPartition : Lists.partition(shipmentInstIds, appContext.getMaxCountForInClause())) {
                shipments.addAll
                    (shmShipmentSubDAO.listShipmentsByShpInstIds
                         (shipmentInstIdsPartition,
                          shmShipmentEagerLoadPlan,
                          entityManager));
			}
		}
		else {
			for (List<String> proNumbersPartition : Lists.partition(proNumbers, appContext.getMaxCountForInClause())) {
                shipments.addAll
                    (shmShipmentSubDAO.listShipmentsByProNbrs
                         (proNumbersPartition,
                          shmShipmentEagerLoadPlan,
                          entityManager));
			}
			// populate the list of ids for later use
			shipmentInstIds = shipments.stream().map(ShmShipment::getShpInstId).collect(Collectors.toList());
		}

        ListShipmentsResp response =
            buildResponse(shipmentInstIds,
                          shipments,
                          shmShipmentEagerLoadPlan,
                          shipmentDetailCdsSet,
                          entityManager);

		response.setWarnings(getShipmentWarnings(response.getShipmentDetails(), shipmentInstIds, proNumbers));

		return response;
	}

	private List<DataValidationError> getShipmentWarnings(List<ShipmentDetails> shipmentDetails,
														  List<Long> shipmentInstIds,
														  List<String> proNumbers) {
		List<DataValidationError> warnings = null;

		if(CollectionUtils.isNotEmpty(proNumbers)) {
			final Set<String> proShipmentsNotFound = new HashSet<>(proNumbers);
			proShipmentsNotFound.removeAll(shipmentDetails.stream()
					.filter(shipmentDetail -> shipmentDetail.getShipment() != null)
					.map(shipmentDetail -> shipmentDetail.getShipment().getProNbr())
					.collect(Collectors.toSet()));
			if (CollectionUtils.isNotEmpty(proShipmentsNotFound))
				warnings = getDataValidationErrors(proShipmentsNotFound, "proNumber");
		} else {
			Set<String> shipmentsNotFound = new HashSet<>(
					CollectionUtils.emptyIfNull(shipmentInstIds).stream()
							.map(String ::valueOf)
							.collect(Collectors.toSet())
			);
			shipmentsNotFound.removeAll(shipmentDetails.stream()
					.filter(shipmentDetail -> shipmentDetail.getShipment() != null)
					.map(shipmentDetail -> String.valueOf(shipmentDetail.getShipment().getShipmentInstId()))
					.collect(Collectors.toSet()));
			if(CollectionUtils.isNotEmpty(shipmentsNotFound))
				warnings = getDataValidationErrors(shipmentsNotFound, "shipmentInstId");
		}

		return warnings;
	}

	private List<DataValidationError> getDataValidationErrors(Set<String> shipmentsNotFound, String fieldName) {
		return CollectionUtils.emptyIfNull(shipmentsNotFound).stream()
				.map(shipment -> {
					return DataValidationErrorBuilder
							.error(NotFoundErrorMessage.SHIPMENT_NOT_FOUND)
							.fieldName(fieldName)
							.fieldValue(shipment)
							.build();
				}).collect(Collectors.toList());
	}

    private ListShipmentsResp buildResponse(List<Long> shipmentInstIds,
                                            List<ShmShipment> shipments,
                                            ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
                                            Set<ShipmentDetailCd> shipmentDetailCds,
                                            EntityManager entityManager) {
		final ListShipmentsResp response = new ListShipmentsResp();
		final List<ShipmentDetails> shipmentDetailsList = new ArrayList<>();

		List<ShmTmDtCritical> shipmentTdcList = new ArrayList<>();
		if (shipmentDetailCds.contains(ShipmentDetailCd.TIME_DATE_CRITICAL)) {
			for (List<Long> shipmentInstIdsPartition : Lists.partition(shipmentInstIds, appContext.getMaxCountForInClause())) {
				shipmentTdcList.addAll(shipmentTdcSubDAO.listTdcForShipmentIdList(shipmentInstIdsPartition, entityManager));
			}
		}

		// CCS-5627 - ShmShipment is missing @OneToMany to List<ShmNotification>
		List<ShmNotification> shmNotifications = new ArrayList<>();
		if (shipmentDetailCds.contains(ShipmentDetailCd.NOTIFICATION)) {
			for (List<Long> shipmentInstIdsPartition : Lists.partition(shipmentInstIds,
					appContext.getMaxCountForInClause())) {
				shmNotifications.addAll(shmNotificationSubDAO
						.listNotificationsForShipmentIdList(shipmentInstIdsPartition, entityManager));
			}
		}

		for (final ShmShipment shipment : shipments) {
            ShipmentDetails shipmentDetails =
                shipmentDetailsDelegate.buildDetails
                    (shipment,
                     shmShipmentEagerLoadPlan);

            shipmentDetails.setNotifications(getNotificationsByShipment(shmNotifications, shipment.getShpInstId()));
            shipmentDetails.setTimeDateCritical(getTimeDateCriticalByShipment(shipmentTdcList, shipment.getShpInstId()));
            shipmentDetailsList.add(shipmentDetails);
		}

		response.setShipmentDetails(shipmentDetailsList);

		return response;
	}

	private List<com.xpo.ltl.api.shipment.v2.ShmNotification> getNotificationsByShipment(List<ShmNotification>
																							   shmNotifications,
																					   Long shipmentInstId) {
		if (CollectionUtils.isNotEmpty(shmNotifications)) {
			return	EntityTransformer.toShmNotification
							(ShipmentUtil.getShipmentNotificationyShpId(shmNotifications, shipmentInstId));
		}
		return Collections.emptyList();
	}

	private TimeDateCritical getTimeDateCriticalByShipment(List<ShmTmDtCritical> shmTmDtCritical,
															Long shipmentInstId) {
		TimeDateCritical timeDateCriticalByShipment = null;
		if (CollectionUtils.isNotEmpty(shmTmDtCritical)) {
			timeDateCriticalByShipment = EntityTransformer.toTimeDateCritical
					(ShipmentUtil.getShipmentTdcByShpId(shmTmDtCritical, shipmentInstId));
		}
		return timeDateCriticalByShipment;
	}

}
