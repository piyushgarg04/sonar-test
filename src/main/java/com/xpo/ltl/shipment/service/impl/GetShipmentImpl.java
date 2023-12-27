package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmCmdyDimension;
import com.xpo.ltl.api.shipment.service.entity.ShmEventLog;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmMvmtExcpAction;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.service.entity.ShmSrNbr;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.v2.ChildShipment;
import com.xpo.ltl.api.shipment.v2.ChildShipmentId;
import com.xpo.ltl.api.shipment.v2.Commodity;
import com.xpo.ltl.api.shipment.v2.GetShipmentResp;
import com.xpo.ltl.api.shipment.v2.Movement;
import com.xpo.ltl.api.shipment.v2.MovementException;
import com.xpo.ltl.api.shipment.v2.ShipmentDetailCd;
import com.xpo.ltl.api.shipment.v2.ShipmentDetails;
import com.xpo.ltl.api.shipment.v2.ShipmentId;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.java.util.cityoperations.executors.TaskProcessor;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.client.GetLocationReferenceDetailsResult;
import com.xpo.ltl.shipment.service.client.GetLocationReferenceDetailsTask;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.dao.ShipmentMovementExceptionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentSupRefSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentTdcSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmBillEntryStatsSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmCmdyDimensionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmEventLogSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmMvmtExcpActionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmNotificationSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentEagerLoadPlan;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmXdockExcpSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShipmentDetailsDelegate;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import com.xpo.ltl.shipment.service.util.ShipmentDetailCdUtil;
import com.xpo.ltl.shipment.service.validators.ShipmentRequestsValidator;

@ApplicationScoped
@LogExecutionTime
public class GetShipmentImpl {

	private static final Log logger = LogFactory.getLog(GetShipmentImpl.class);

	@Inject
	private ShmShipmentSubDAO shmShipmentSubDAO;

	@Inject
    private ShipmentTdcSubDAO shipmentTdcSubDAO;

	@Inject
    private ShmXdockExcpSubDAO shmXdockExcpSubDAO;

	@Inject
    private ShmNotificationSubDAO shmNotificationSubDAO;

	@Inject
    private ShmEventLogSubDAO shmEventLogSubDAO;

	@Inject
    private ShipmentRequestsValidator getShipmentsRequestValidator;

    @Inject
    private ShmBillEntryStatsSubDAO shmBillEntryStatsSubDAO;

    @Inject
    private ExternalRestClient externalRestClient;

    @Inject
    private ShipmentMovementExceptionSubDAO shipmentMovementExceptionSubDAO;

    @Inject
    private ShmMvmtExcpActionSubDAO shmMvmtExcpActionSubDAO;
    
    @Inject
    private ShipmentSupRefSubDAO shipmentSupRefSubDAO;

    @Inject
    private ShmCmdyDimensionSubDAO shmCmdyDimensionSubDAO;

    @Inject
    private ShipmentDetailsDelegate shipmentDetailsDelegate;
	
	@Inject
	private AppContext appContext;

	public GetShipmentResp getShipment(
		final String proNbr,
		final String pickupDate,
		final Long shipmentInstId,
		final ShipmentDetailCd[] shipmentDetailCds,
		final String[] suppRefNbrTypes,
		final Boolean handlingUnitMovementsForSplitOnlyInd, 
		final TransactionContext txnContext,
		final EntityManager entityManager) throws ServiceException {

		checkNotNull(txnContext, "TransactionContext is required");
		checkNotNull(entityManager, "EntityManager is required");

		getShipmentsRequestValidator.validateRequest(proNbr, pickupDate, shipmentInstId, shipmentDetailCds, txnContext);
		final Date pkupDt = !StringUtils.isBlank(pickupDate) ? BasicTransformer.toDate(pickupDate)
				: null;

		ShmShipment shmShipment = null;
		String proNumber = "";
		boolean yellowProInd =false;

		final List<ShipmentDetailCd> shipmentDetailCdsList =
				Optional.ofNullable(Arrays.asList(shipmentDetailCds))
						.orElse(null);
		EnumSet<ShipmentDetailCd> shipmentDetailCdsSet =
				CollectionUtils.isNotEmpty(shipmentDetailCdsList)
						? EnumSet.copyOf(shipmentDetailCdsList)
						: EnumSet.noneOf(ShipmentDetailCd.class);
		final List<String> shipmentSuppRefNbrTypes = suppRefNbrTypes == null ? new ArrayList<>() :
			Arrays.asList(suppRefNbrTypes);

        ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan =
            ShmShipmentEagerLoadPlan.from(shipmentDetailCdsSet, handlingUnitMovementsForSplitOnlyInd == null? true: handlingUnitMovementsForSplitOnlyInd);

		if(StringUtils.isNotBlank(proNbr)) {
			proNumber = ProNumberHelper.validateProNumber(proNbr, txnContext);
			yellowProInd = ProNumberHelper.isYellowPro(proNumber);
		}

        shmShipment =
            shmShipmentSubDAO.findByProOrShipmentId
                (proNumber,
                 pkupDt,
                 shipmentInstId,
                 yellowProInd,
                 shmShipmentEagerLoadPlan,
                 entityManager);

		if (yellowProInd && shmShipment == null) {
			throw ExceptionBuilder
					.exception(NotFoundErrorMessage.SHM_HANDLING_UNIT_NOT_FOUND, txnContext)
					.contextValues(String.format(
							"Shipment handling unit not found for ProNumber:%s", proNumber))
					.build();
		}

		if (shmShipment == null) {
			throw ExceptionBuilder.exception(NotFoundErrorMessage.SHIPMENT_NF, txnContext)
					.contextValues(String.format("ShipmentInstId:%s", shipmentInstId),
							String.format("ProNbr:%s", proNbr))
					.build();
		}

        GetShipmentResp resp =
            buildResponse(shmShipment,
                          shmShipmentEagerLoadPlan,
                          shipmentDetailCdsSet,
                          shipmentSuppRefNbrTypes,
                          entityManager,
                          txnContext);

		return resp;
	}

    private GetShipmentResp buildResponse(ShmShipment shmShipment,
                                          ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan,
                                          Set<ShipmentDetailCd> shipmentDetailCds,
                                          List<String> shmSrNbrTypCds,
                                          EntityManager entityManager,
                                          TransactionContext txnContext)
            throws ServiceException {
        GetShipmentResp response = new GetShipmentResp();

        ShipmentDetails detail =
            shipmentDetailsDelegate.buildDetails
                (shmShipment,
                 shmShipmentEagerLoadPlan);

        response.setShipment(detail.getShipment());
        response.setHazMat(detail.getHazMat());
        response.setHandlingUnits(detail.getShipment().getHandlingUnit());
        response.setOperationsShipment(detail.getOperationsShipment());
        response.setAsMatchedParty(detail.getAsMatchedParty());
        response.setAccessorialService(detail.getAccessorialService());
        response.setAdvanceBeyondCarrier(detail.getAdvanceBeyondCarrier());
        response.setCustomsBond(detail.getCustomsBond());
        response.setCustomsControl(detail.getCustomsControl());
        response.setMiscLineItem(detail.getMiscLineItem());
        response.setRemark(detail.getRemark());
        response.setSuppRefNbr(detail.getSuppRefNbr());
        response.setCommodity(detail.getCommodity());
        response.setLinehaulDimensions(detail.getLinehaulDimension());
        response.setManagementRemark(detail.getManagementRemark());
        response.setParentShipmentId(detail.getParentShipmentId());

        response.setCustPricingAgreementId(shmShipment.getPrcAgrmtId().toString());

		// AM2-375
		if(ShipmentDetailCdUtil.contains
			(shipmentDetailCds, true, ShipmentDetailCd.NO_SHIPMENT)
			&& shmShipmentEagerLoadPlan.isShmSrNbrs()
			&& CollectionUtils.isNotEmpty(shmSrNbrTypCds)) {
			final List<ShmSrNbr> foundSuppRefNbrs = shipmentSupRefSubDAO.findByShpInstIdAndShmSrNbrTypCds(shmShipment.getShpInstId(), shmSrNbrTypCds, entityManager);
			response.setSuppRefNbr(EntityTransformer.toSuppRefNbr(foundSuppRefNbrs));
		}

        // TODO Use eager-loaded relationship and move ShipmentDetailsDelegate?
        if (ShipmentDetailCdUtil.contains
                (shipmentDetailCds, true, ShipmentDetailCd.EVENT_LOG)) {
            ShmEventLog shmEventLog = shmEventLogSubDAO.getMostRecentEventLog(shmShipment.getShpInstId(), entityManager);
            response.setLastEventLog(EntityTransformer.toEventLog(shmEventLog));
        }

        // TODO Request relationship get added to ShmShipment entity
        if (ShipmentDetailCdUtil.contains
                (shipmentDetailCds, true, ShipmentDetailCd.TIME_DATE_CRITICAL)) {
            response.setTimeDateCritical
                (EntityTransformer.toTimeDateCritical
                     (shipmentTdcSubDAO.findById
                          (shmShipment.getShpInstId(), entityManager)));
        }

        // TODO Use eager-loaded relationship and move ShipmentDetailsDelegate?
        if (ShipmentDetailCdUtil.contains
                (shipmentDetailCds, true, ShipmentDetailCd.MOVEMENT_EXCEPTION)) {
            response.setMovement
                (getMovementWithExceptions
                     (shmShipment.getShpInstId(),
                      detail.getShipment().getMovement(),
                      entityManager));
        }

        if (ShipmentDetailCdUtil.contains
                (shipmentDetailCds, true, ShipmentDetailCd.COUNTRY_CODES)) {
            try {
                Pair<String, String> sltCountryCds =
                    createAndExecuteOrigAndDestLocReferenceTasks
                        (shmShipment.getOrigTrmnlSicCd(),
                         shmShipment.getDestTrmnlSicCd(),
                         txnContext);
                response.setOriginSicCountryCd(sltCountryCds.getLeft());
                response.setDestinationSicCountryCd(sltCountryCds.getRight());
            }
            catch (InterruptedException | ExecutionException | TimeoutException e) {
                logger.info("GetLocationReferenceDetails failed to execute: " + e.getMessage());
            }
        }

        // TODO Use eager-loaded relationship and move ShipmentDetailsDelegate?
        if (ShipmentDetailCdUtil.contains
                (shipmentDetailCds, true, ShipmentDetailCd.COMMODITY)) {
            response.setCommodity
                (getCommoditiesWithDimensions
                     (shmShipment.getShpInstId(),
                      detail.getCommodity(),
                      entityManager));
        }

        // TODO Investigate if getChildShipments can be improved to use
        // eager-loading
        if (ShipmentDetailCdUtil.contains
                (shipmentDetailCds, true, ShipmentDetailCd.CHILD_SHIPMENT)) {
            // this is a parent shipment, get child shipments matching on shipmentInstId
				// with parentShmInstId
			final List<ShmShipment> relatedShipmentsList = shmShipmentSubDAO
			.getRelatedShipments(shmShipment.getShpInstId(), entityManager);
			response.setChildShipmentIds(getChildShipmentIds(shmShipment, relatedShipmentsList, entityManager));
			response.setChildShipments(getChildShipments(shmShipment, relatedShipmentsList, entityManager));
        }

        // TODO Investigate if getSiblingShipments can be improved to use
        // eager-loading
        if (ShipmentDetailCdUtil.contains
                (shipmentDetailCds, true, ShipmentDetailCd.SIBLING_SHIPMENT)) {
            response.setSiblingShipmentIds
                (getSiblingShipments(shmShipment, entityManager));
        }

        // TODO Request relationship get added to ShmShipment entity
        if (ShipmentDetailCdUtil.contains
                (shipmentDetailCds, true, ShipmentDetailCd.XDOCK_EXCEPTION)) {
            response.setXdockExceptions
                (EntityTransformer.toXdockException
                     (shmXdockExcpSubDAO.findByShipmentInstId
                          (shmShipment.getShpInstId(), entityManager)));
        }

        // TODO Request relationship get added to ShmShipment entity
        if (ShipmentDetailCdUtil.contains
                (shipmentDetailCds, true, ShipmentDetailCd.NOTIFICATION)) {
            response.setNotifications
                (EntityTransformer.toShmNotification
                     (shmNotificationSubDAO.findByShipmentInstId
                          (shmShipment.getShpInstId(), entityManager)));
        }

        // TODO Use eager-loaded relationship and move ShipmentDetailsDelegate?
        if (ShipmentDetailCdUtil.contains
                (shipmentDetailCds, true, ShipmentDetailCd.BILL_ENTRY_STATS)) {
            response.setBillEntryStats
                (EntityTransformer.toBillEntryStats
                     (shmBillEntryStatsSubDAO.findByShipmentInstId
                          (shmShipment.getShpInstId(), entityManager)));
        }

        // To avoid the redundant data in the response, set the shipment level
        // HandlingUnits and movements to NULL
        detail.getShipment().setHandlingUnit(null);
        detail.getShipment().setMovement(null);
		
		if(CollectionUtils.isNotEmpty(shipmentDetailCds) && ShipmentDetailCdUtil.contains
		(shipmentDetailCds, true, ShipmentDetailCd.NO_SHIPMENT)){
			response.setShipment(null);
			response.setOperationsShipment(null);
			response.setHandlingUnits(null);
			response.setCustPricingAgreementId(null);
			response.setHazMat(null);
		}

        return response;
    }

	/**
	 * @param origTrmnlSicCd
	 * @param destTrmnlSicCd
	 * @param externalRestClient2
	 * @param txnContext
	 * @return
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private Pair<String, String> createAndExecuteOrigAndDestLocReferenceTasks(String origTrmnlSicCd, String destTrmnlSicCd,
				TransactionContext txnContext) throws InterruptedException, ExecutionException, TimeoutException {
				Collection<GetLocationReferenceDetailsTask> getLocationReferenceDetailsTaskList = new ArrayList<>();
				GetLocationReferenceDetailsTask taskOrigin = new GetLocationReferenceDetailsTask(origTrmnlSicCd, externalRestClient, txnContext);
				GetLocationReferenceDetailsTask taskDest = new GetLocationReferenceDetailsTask(destTrmnlSicCd, externalRestClient, txnContext);
				getLocationReferenceDetailsTaskList.add(taskOrigin);
				getLocationReferenceDetailsTaskList.add(taskDest);
				Collection<GetLocationReferenceDetailsResult> locationReferenceDetailsResult = 
	        		TaskProcessor.process(getLocationReferenceDetailsTaskList, 
	        				appContext.getLocationReferenceDetailsMaxConcurrent(), 
	        				appContext.getLocationReferenceDetailsMaxAttempts(), 
	        				appContext.getLocationReferenceDetailsRetryDelayMillis(), 
	        				appContext.getExecutor(), 
	        				appContext.getLocationReferenceDetailsTimeoutMillis());
				List<String> resList  = new ArrayList<>(2);
				String createErrorMsg = CollectionUtils.emptyIfNull(locationReferenceDetailsResult).stream().filter(result -> (null != result.getMessage()) 
									   &&(!"Success".equalsIgnoreCase(result.getMessage()))).map(result->result.getMessage()).collect(Collectors.joining(","));
				if(StringUtils.isNotEmpty(createErrorMsg)) {
					logger.info("locationClient.getLocationReferenceDetails :" + createErrorMsg);
                    return Pair.of(StringUtils.EMPTY, StringUtils.EMPTY);
				}
				if(!StringUtils.equals(destTrmnlSicCd, origTrmnlSicCd)){
					//add empty strings to define the size of the array
					//as ArrayList does not have a method or constructor to do that
					while(resList.size()<2)resList.add(StringUtils.EMPTY);
					
					CollectionUtils.emptyIfNull(locationReferenceDetailsResult).stream().collect(Collectors.toList()).forEach((locRef) ->{
							if(StringUtils.equals(origTrmnlSicCd, locRef.getSicCd())){
								resList.set(0, locRef.getSltCountryCd());
							}
							else{
								resList.set(1, locRef.getSltCountryCd());
							}
					});				
				}
				else{
					resList.clear();
					CollectionUtils.emptyIfNull(locationReferenceDetailsResult).stream().collect(Collectors.toList()).forEach((locRef) ->{
						resList.add(locRef.getSltCountryCd());
					});
				}
				if(resList.size() == 2){
                    return Pair.of(resList.get(0), resList.get(1));
				}
                return Pair.of(StringUtils.EMPTY, StringUtils.EMPTY);
	}

	private List<ShmMovementExcp> findByMovement(List<ShmMovementExcp> shmMovementExcps, Movement movement) {

		if(CollectionUtils.isEmpty(shmMovementExcps) || movement == null)
			Collections.emptyList();

		Comparator<ShmMovementExcp> movementExceptionComparator =
				(movement1, movement2) -> new CompareToBuilder().append(movement1.getId().getSeqNbr(),
				movement2.getId().getSeqNbr()).toComparison();

		return shmMovementExcps.stream()
				.filter(shmMovementExcp -> shmMovementExcp.getId().getMvmtSeqNbr() ==
						movement.getSequenceNbr().longValue()
						&& shmMovementExcp.getId().getShpInstId() == movement.getShipmentInstId().longValue()
				).sorted(movementExceptionComparator).collect(Collectors.toList());
	}

	private List<ShmMvmtExcpAction> findByMovementException(List<ShmMvmtExcpAction> shmMvmtExcpActions,
												   MovementException movementException) {

		if(CollectionUtils.isEmpty(shmMvmtExcpActions) || movementException == null)
			Collections.emptyList();

		return shmMvmtExcpActions.stream()
				.filter(shmMvmtExcpAction -> shmMvmtExcpAction.getId().getMvmtSeqNbr() ==
						movementException.getMovementSequenceNbr().longValue()
						&& shmMvmtExcpAction.getId().getMvmtExcpSeqNbr() ==
						movementException.getSequenceNbr().longValue()
						&& shmMvmtExcpAction.getId().getShpInstId() ==
						movementException.getShipmentInstId().longValue()
				).collect(Collectors.toList());
	}

	private List<Movement> getMovementWithExceptions(Long shipmentInstId,
													 List<Movement> movements,
													 EntityManager entityManager) {

		if (CollectionUtils.isNotEmpty(movements)) {

			List<ShmMovementExcp> shmMovementExcps =
					shipmentMovementExceptionSubDAO.findByShpInstIds(Arrays.asList(shipmentInstId), entityManager);

			List<ShmMvmtExcpAction> shmMvmtExcpActions =
					shmMvmtExcpActionSubDAO.findByShpInstIds(Arrays.asList(shipmentInstId), entityManager);

			return movements.stream()
					.map(movement -> {
						List<ShmMovementExcp> shmMovementExcpsByMovement =
								findByMovement(shmMovementExcps, movement);
						if (CollectionUtils.isNotEmpty(shmMovementExcpsByMovement)) {
							movement.setMovementException(shmMovementExcpsByMovement
									.stream()
									.map(exceptionEntity -> {
										final MovementException exception = EntityTransformer
												.toMovementException(exceptionEntity);
										List<ShmMvmtExcpAction> shmMvmtExcpActionsByMovementExcp =
												findByMovementException(shmMvmtExcpActions, exception);
										if (CollectionUtils.isNotEmpty(
												shmMvmtExcpActionsByMovementExcp)) {
											exception.setMovementExceptionAction(
													EntityTransformer.toMovementExceptionAction(
															shmMvmtExcpActionsByMovementExcp));
										}
										return exception;
									}).collect(Collectors.toList()));
						}
						return movement;
					}).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	private List<ShmCmdyDimension> findByCommodity(List<ShmCmdyDimension> shmCmdyDimensions, Commodity commodity) {

		if(CollectionUtils.isEmpty(shmCmdyDimensions) || commodity == null)
			Collections.emptyList();

		return shmCmdyDimensions.stream()
				.filter(shmCmdyDimension -> shmCmdyDimension.getId().getCmdySeqNbr() ==
						commodity.getSequenceNbr().longValue()
						&& shmCmdyDimension.getId().getShpInstId() == commodity.getShipmentInstId().longValue()
				).collect(Collectors.toList());
	}

	private List<Commodity> getCommoditiesWithDimensions(Long shipmentInstId,
														 List<Commodity> commodities,
														 EntityManager entityManager) {

		if (CollectionUtils.isNotEmpty(commodities)) {
			List<ShmCmdyDimension> shmCmdyDimensions = shmCmdyDimensionSubDAO
					.findByShpInstIds(Arrays.asList(shipmentInstId), entityManager);

			return commodities.stream().map(commodity -> {
				List<ShmCmdyDimension> shmCmdyDimensionsByCommodity = findByCommodity(shmCmdyDimensions, commodity);
				commodity.setCommodityDimension(EntityTransformer.toCommodityDimension(shmCmdyDimensionsByCommodity));
				return commodity;
			}).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	private List<ShipmentId> getSiblingShipments(final ShmShipment shmShipment,
			final EntityManager entityManager) throws ServiceException {

		if (shmShipment != null && shmShipment.getParentInstId() != null
				&& BasicTransformer.toLong(shmShipment.getParentInstId()) != 0L) {
			// this is a child shipment, get the siblings by using the parentInstId
			final List<ShmShipment> siblingShipmentsList = shmShipmentSubDAO
					.getRelatedShipments(BasicTransformer.toLong(shmShipment.getParentInstId()), entityManager);

			return CollectionUtils.emptyIfNull(siblingShipmentsList).stream()
					.map(relatedShipment -> {
						final ShipmentId shipmentId = new ShipmentId();
						shipmentId
								.setShipmentInstId(String.valueOf(relatedShipment.getShpInstId()));
						shipmentId.setProNumber(
								BasicTransformer.toTrimmedString(relatedShipment.getProNbrTxt()));
						shipmentId.setPickupDate(BasicTransformer
								.toXMLGregorianCalendar(relatedShipment.getPkupDt()));
						return shipmentId;
					}).collect(Collectors.toList());
		}
		return null;
	}

	private List<ShipmentId> getChildShipmentIds(final ShmShipment shmShipment,
			final List<ShmShipment> relatedShipmentsList,
			final EntityManager entityManager) throws ServiceException {

		if (shmShipment != null && shmShipment.getParentInstId() == null
			&& relatedShipmentsList != null) {

			return CollectionUtils.emptyIfNull(relatedShipmentsList).stream()
					.map(relatedShipment -> {
						final ShipmentId shipmentId = new ShipmentId();
						shipmentId
								.setShipmentInstId(String.valueOf(relatedShipment.getShpInstId()));
						shipmentId.setProNumber(
								BasicTransformer.toTrimmedString(relatedShipment.getProNbrTxt()));
						shipmentId.setPickupDate(BasicTransformer
								.toXMLGregorianCalendar(relatedShipment.getPkupDt()));
						return shipmentId;
					}).collect(Collectors.toList());
		}
		return null;
	}

	private List<ChildShipment> getChildShipments(final ShmShipment shmShipment,
				final List<ShmShipment> relatedShipmentsList,
				final EntityManager entityManager) throws ServiceException {
				if (shmShipment != null && shmShipment.getParentInstId() == null
					&& relatedShipmentsList != null) {
	
				return CollectionUtils.emptyIfNull(relatedShipmentsList).stream()
						.map(relatedShipment -> {
							final ChildShipmentId childShipmentId = new ChildShipmentId();
							childShipmentId
									.setShipmentInstId(relatedShipment.getShpInstId());
							childShipmentId.setChildProNbr(
									BasicTransformer.toTrimmedString(relatedShipment.getProNbrTxt()));
							final ChildShipment childShipment = new ChildShipment();
							childShipment.setChildShipmentId(childShipmentId);
							childShipment.setMoverSuffix(relatedShipment.getMovrSuffix());
							return childShipment;
						}).collect(Collectors.toList());
			}
			return null;
	}
}
