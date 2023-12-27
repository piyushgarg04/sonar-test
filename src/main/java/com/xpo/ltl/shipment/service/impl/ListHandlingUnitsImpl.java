package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitPK;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.v2.ChildShipmentId;
import com.xpo.ltl.api.shipment.v2.HandlingUnit;
import com.xpo.ltl.api.shipment.v2.ListHandlingUnitsResp;
import com.xpo.ltl.api.shipment.v2.ListHandlingUnitsRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ListHandlingUnitsImpl {

	private static final Log logger = LogFactory.getLog(ListHandlingUnitsImpl.class);

	@Inject
	private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;


	@Inject
	private AppContext appContext;
	
	
	public ListHandlingUnitsResp listHandlingUnits(
		final ListHandlingUnitsRqst listHandlingUnitsRqst,
		final TransactionContext txnContext,
		final EntityManager entityManager
	) throws ServiceException {

		final Stopwatch methodStopwatch = Stopwatch.createStarted();

		checkNotNull(txnContext, "TransactionContext is required");
		checkNotNull(entityManager, "EntityManager is required");
		checkNotNull(listHandlingUnitsRqst.getChildShipmentIds(), "ShipmentIds are required");

		List<ShmHandlingUnit> shmHandlingUnits = Lists.newArrayList();

		final List<ChildShipmentId> childShipmentIds = listHandlingUnitsRqst.getChildShipmentIds().stream()
				.filter(shipmentId -> (ObjectUtils.defaultIfNull(shipmentId.getShipmentInstId(), 0l)) > 0
						&& (BasicTransformer.toInteger(ObjectUtils.defaultIfNull(shipmentId.getSequenceNbr(), BigInteger.ZERO)) > 0 )
				)
				.collect(Collectors.toList());

		final List<String> childProNumbers = listHandlingUnitsRqst.getChildShipmentIds().stream()
				.filter(shipmentId -> !StringUtils.isBlank(shipmentId.getChildProNbr()))
				.map(shipmentId -> shipmentId.getChildProNbr())
				.collect(Collectors.toList());


		validateRequest(childProNumbers, childShipmentIds, txnContext);

		final Stopwatch daoStopwatch = Stopwatch.createStarted();

		if (CollectionUtils.isNotEmpty(childShipmentIds)) {
			List<ShmHandlingUnitPK> pkIds = new ArrayList<>();
			for (ChildShipmentId childShipmentId : childShipmentIds) {
				ShmHandlingUnitPK shmHandlingUnitPK = new ShmHandlingUnitPK();
				shmHandlingUnitPK.setShpInstId(childShipmentId.getShipmentInstId());
				shmHandlingUnitPK.setSeqNbr(BasicTransformer.toLong(childShipmentId.getSequenceNbr()));
				pkIds.add(shmHandlingUnitPK);
			}
			shmHandlingUnits.addAll(shmHandlingUnitSubDAO.findByIds(pkIds, entityManager));
		}
		else {
			shmHandlingUnits.addAll(shmHandlingUnitSubDAO.listByChildProNumbers(childProNumbers, entityManager));
			// populate the list of ids for later use
//			shipmentInstIds = handlingUnits.stream().map(ShmHandlingUnit::getShpInstId).collect(Collectors.toList());
		}

		daoStopwatch.stop();
		logger.info(String.format(
			"listShipmentsByShipmentIds call to get %s shipments completed in %s ms",
			shmHandlingUnits.size(),
			daoStopwatch.elapsed(TimeUnit.MILLISECONDS)
		));

		List<HandlingUnit> handlingUnits = Lists.newArrayList();
		HandlingUnit handlingUnit;
		if (CollectionUtils.isNotEmpty(shmHandlingUnits)) {
			for (ShmHandlingUnit shmHandlingUnit: shmHandlingUnits) {
				handlingUnit = EntityTransformer.toHandlingUnit(shmHandlingUnit);
				handlingUnit.setHandlingUnitMovement(EntityTransformer.toHandlingUnitMovement(shmHandlingUnit.getShmHandlingUnitMvmts()));
				handlingUnits.add(handlingUnit);
			}
		}
		final ListHandlingUnitsResp response = new ListHandlingUnitsResp();
		response.setHandlingUnits(handlingUnits);
		
		methodStopwatch.stop();
		logger.info(String.format(
			"listShipments completed in %s ms",
			methodStopwatch.elapsed(TimeUnit.MILLISECONDS)
		));

		return response;
	}

	/**
	 * Validate the data on the request
	 *
	 * @param childProNumbers
	 * @param shipmentInstIds
	 * @param shipmentDetailCds
	 * @param txnContext
	 * @throws ValidationException
	 * @throws ServiceException
	 */
	protected void validateRequest(
		final List<String> childProNumbers,
		final List<ChildShipmentId> childShipmentIds,
		final TransactionContext txnContext
	) throws ValidationException, ServiceException {

		// If there is a PRO and a shipment instance ID, throw an error -- you can give
		// one or the other, but not both.
		if (CollectionUtils.isNotEmpty(childProNumbers) && CollectionUtils.isNotEmpty(childShipmentIds)) {
			throw addMoreInfo(ExceptionBuilder.exception(ValidationErrorMessage.PRO_OR_SHP_INST_REQD, txnContext),
					childShipmentIds).log().build();
			// We need a PRO or shipment instance ID.
		} else if (CollectionUtils.isEmpty(childShipmentIds) && CollectionUtils.isEmpty(childProNumbers)) {
			throw addMoreInfo(
				ExceptionBuilder.exception(
					ValidationErrorMessage.PRO_OR_SHP_INST_REQD, txnContext
				),
				childShipmentIds
			).log().build();
		}
	}


	/**
     * Facade for shipment exception creation
     */
	private ExceptionBuilder<? extends ServiceException> addMoreInfo(ExceptionBuilder<? extends ServiceException> builder,
																	 List<ChildShipmentId> childShipmentIds) {
		builder.moreInfo("shipmentInstIds",
			CollectionUtils.isNotEmpty(childShipmentIds)
				? String.join(",", childShipmentIds.stream().map(shipmentInstId -> shipmentInstId.toString()).collect(Collectors.toList()))
				: null
		);

        return builder;
    }


}
