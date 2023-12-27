package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.RouteTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.RouteTypeCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentsWithRouteDtlRqst;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.util.ShipmentUtil;

@RequestScoped
public class UpdateShipmentsWithRouteDtlImpl {
	private static final Log log = LogFactory.getLog(UpdateShipmentsWithRouteDtlImpl.class);

	private static final String DEFAULT_BLANK = " ";  // keep this strictly a blank, do not change to an empty length string, which is translated differently downstream in legacy SCO
	
	@PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
	private EntityManager db2EntityManager;

	@Inject
	private ShmShipmentSubDAO shipmentDAO;

	public void updateShipmentsWithRouteDtl(final UpdateShipmentsWithRouteDtlRqst updateShipmentsWithRouteDtlRqst,
			final TransactionContext txnContext, final EntityManager entityManager) throws ServiceException {

		Stopwatch sw = Stopwatch.createStarted();
		checkNotNull(updateShipmentsWithRouteDtlRqst, "The request is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
		
		//Validate the request
		List<MoreInfo> moreInfos =  Lists.newArrayList();
		validateRequest(updateShipmentsWithRouteDtlRqst, moreInfos);
		if (CollectionUtils.isNotEmpty(moreInfos)) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo(moreInfos)
					.build();
		}

		List<Long> shipmentInstIds = updateShipmentsWithRouteDtlRqst.getShipmentInstIds();
		log.info(String.format("Updating shipments with route detail for shipmentInstIds %s", shipmentInstIds));
		
		boolean requestIsUnassignmentOfShipments = determineUpdate(updateShipmentsWithRouteDtlRqst);
		
		final String rtePfxTxt = updateShipmentsWithRouteDtlRqst.getRoutePrefix();
		final String rteSfxTxt = updateShipmentsWithRouteDtlRqst.getRouteSuffix();
		final String rteTypCd = RouteTypeCdTransformer.toCode(updateShipmentsWithRouteDtlRqst.getRouteTypeCd());
		final AuditInfo auditInfo = updateShipmentsWithRouteDtlRqst.getAuditInfo();
		if (StringUtils.isBlank(auditInfo.getUpdateByPgmId())) {
			auditInfo.setUpdateByPgmId("SHMUPDT");
		}

		final List<ShmShipment> shmShipmentList = listShmShipments(shipmentInstIds, entityManager);
		
		if (requestIsUnassignmentOfShipments) {
			List<Long> shipmentsToStageList = listShipmentsByType(shmShipmentList, RouteTypeCd.STAGED.name());
			List<Long> shipmentsToResetList = listShipmentsByType(shmShipmentList, DEFAULT_BLANK);

			if (CollectionUtils.isNotEmpty(shipmentsToStageList)) {
				List<ShmShipment> shmShipmentStagedList = ShipmentUtil.getShmShipmentsById(shipmentsToStageList, shmShipmentList);
				updateShipment(shipmentsToStageList, shmShipmentStagedList, rtePfxTxt, rteSfxTxt, RouteTypeCdTransformer.toCode(RouteTypeCd.STAGED), auditInfo, txnContext, entityManager);
			}
			if (CollectionUtils.isNotEmpty(shipmentsToResetList)) {
				List<ShmShipment> shmShipmentToResetList = ShipmentUtil.getShmShipmentsById(shipmentsToResetList, shmShipmentList);
				updateShipment(shipmentsToResetList, shmShipmentToResetList, rtePfxTxt, rteSfxTxt, RouteTypeCdTransformer.toCode(RouteTypeCd.NOT_ROUTED), auditInfo, txnContext, entityManager);
			}
		} else {
			updateShipment(shipmentInstIds, shmShipmentList, rtePfxTxt, rteSfxTxt, rteTypCd, auditInfo, txnContext, entityManager);
		}
		
		sw.stop();
		log.info(
				String.format("updateShipmentsWithRouteDtl call to updated %s shipments for route %s-%s in %s ms",
						CollectionUtils.size(shmShipmentList), rtePfxTxt, rteSfxTxt, sw.elapsed(TimeUnit.MILLISECONDS)));
	}

	private List<ShmShipment> listShmShipments(final List<Long> shipmentInstIds, final EntityManager entityManager) throws ServiceException {
		if(CollectionUtils.isEmpty(shipmentInstIds)) return null;
		return shipmentDAO.listShipmentsByShipmentIds(shipmentInstIds, entityManager);
	}
	
	protected void validateRequest(UpdateShipmentsWithRouteDtlRqst updateShipmentsWithRouteDtlRqst, List<MoreInfo> moreInfos) {
		if(updateShipmentsWithRouteDtlRqst == null) {
			moreInfos.add(createMoreInfo("updateShipmentsWithRouteDtlRqst", "request is required"));
		} else {
			// Route prefix and suffix can be blank but not null or an empty length string
			if(null == updateShipmentsWithRouteDtlRqst.getRoutePrefix()) {
				moreInfos.add(createMoreInfo("routePrefix", "valid routePrefix is required"));
			} else if (updateShipmentsWithRouteDtlRqst.getRoutePrefix().length() == 0) {
				moreInfos.add(createMoreInfo("routePrefix", "valid routePrefix is required"));
			}
			if(null == updateShipmentsWithRouteDtlRqst.getRouteSuffix()) {
				moreInfos.add(createMoreInfo("routeSuffix", "valid routeSuffix is required"));
			} else if (updateShipmentsWithRouteDtlRqst.getRouteSuffix().length() == 0) {
				moreInfos.add(createMoreInfo("routeSuffix", "valid routeSuffix is required"));
			}
			if(updateShipmentsWithRouteDtlRqst.getRouteTypeCd() == null) {
				moreInfos.add(createMoreInfo("routeTypeCd", "valid routeTypeCd is required"));
			}
			if (CollectionUtils.isNotEmpty(updateShipmentsWithRouteDtlRqst.getShipmentInstIds())) {
				validateShipmentIds(moreInfos, updateShipmentsWithRouteDtlRqst.getShipmentInstIds());
			} else {
				moreInfos.add(createMoreInfo("shipmentInstIds", "At least one shipment must be provided"));
			}
			if(updateShipmentsWithRouteDtlRqst.getAuditInfo() == null) {
				moreInfos.add(createMoreInfo("auditInfo", "auditInfo is required"));
			}
		}
	}
	
	protected void validateShipmentIds(List<MoreInfo> moreInfo, List<Long> shipmentInstIds) {
		Set<Long> shipmentIdsSet = new HashSet<>();
		for (Long shipmentInstId : shipmentInstIds) {
			if (shipmentInstId <= 0) {
				moreInfo.add(createMoreInfo("shipmentInstId", ValidationErrorMessage.SHIPMENT_INST_ID_RQ.message()));
			} else {
				if(shipmentIdsSet.contains(shipmentInstId)) {
					moreInfo.add(createMoreInfo("shipmentInstId", String.format("duplicate shipmentInstId:%s is found", shipmentInstId)));
				} else {
					shipmentIdsSet.add(shipmentInstId);
				}
			}
		}
	}
	
	private List<Long> collectShipmentInstIds(List<ShmShipment> shmShipmentList) {
		return shmShipmentList.stream()
		.map(ShmShipment :: getShpInstId)
		.collect(Collectors.toList());
	}
	
	/**
	 * Shipments in this service can either be assigned to a route or unassigned.  When shipments
	 * are being unassigned, there will be no route information on the request.  This indicator
	 * is used to determine what the route type code value should be for each shipment on the request 
	 * when they are being unassigned from a route.
	 */
	private boolean determineUpdate(UpdateShipmentsWithRouteDtlRqst updateShipmentsWithRouteDtlRqst) {
		
		boolean requestIsUnassignmentOfShipments = false;
		
		if (StringUtils.equals(updateShipmentsWithRouteDtlRqst.getRoutePrefix(), DEFAULT_BLANK)
				&& StringUtils.equals(updateShipmentsWithRouteDtlRqst.getRouteSuffix(), DEFAULT_BLANK)
				&& updateShipmentsWithRouteDtlRqst.getRouteTypeCd() == RouteTypeCd.NOT_ROUTED) {
			requestIsUnassignmentOfShipments = true;
		}
		return requestIsUnassignmentOfShipments;
	}

	/**
	 * Selects shipments based on the business rules for the route type code and the delivery qualifier by shipment.
	 */
	private List<Long> listShipmentsByType(List<ShmShipment> shmShipmentList, String routeType) {
		List<Long> returnShipmentIdList = Lists.newArrayList();
		shmShipmentList.stream().forEach(shmShipment -> {
			String result = ShipmentUtil.determineRouteTypeCode(shmShipment.getRteTypCd(), shmShipment.getDlvryQalfrCd());
			if (StringUtils.equals(result, routeType)) {
				returnShipmentIdList.add(shmShipment.getShpInstId());
			}
		});
		return returnShipmentIdList;
	}

	private void updateShipment(List<Long> shipmentInstIds, List<ShmShipment> shmShipmentList, String rtePfxTxt, String rteSfxTxt, String rteTypCd, 
			AuditInfo auditInfo, TransactionContext txnContext, EntityManager entityManager) throws NotFoundException, ValidationException {

		shipmentDAO.update(shipmentInstIds, rtePfxTxt, rteSfxTxt, rteTypCd, auditInfo, entityManager);

		if (CollectionUtils.isEmpty(shmShipmentList)) {
			throw ExceptionBuilder.exception(NotFoundErrorMessage.SHIPMENT_NOT_FOUND, txnContext)
			.moreInfo("Shipment entities", String.format("Shipments not found for the requested shipmentIds: %s", shipmentInstIds))
			.build();
		} else {
			List<Long> notFoundShipments = (List<Long>) CollectionUtils.subtract(shipmentInstIds, collectShipmentInstIds(shmShipmentList));
			if(CollectionUtils.isNotEmpty(notFoundShipments)) {
				throw ExceptionBuilder.exception(NotFoundErrorMessage.SHIPMENT_NOT_FOUND, txnContext)
				.moreInfo("Shipment entities", String.format("Shipments not found for the requested shipmentIds: %s", notFoundShipments))
				.build();
			}
		}

		Stopwatch sw2 = Stopwatch.createStarted();
		shipmentDAO.updateDb2(shipmentInstIds, rtePfxTxt, rteSfxTxt, rteTypCd, auditInfo, txnContext, db2EntityManager);
		db2EntityManager.flush(); // MOVED the flush call out side of the loop to perform better
		sw2.stop();
		log.info(
				String.format("updateShipmentsWithRouteDtl DAO call to updated %s shipments for route %s-%s in %s ms",
						CollectionUtils.size(shmShipmentList), rtePfxTxt, rteSfxTxt, sw2.elapsed(TimeUnit.MILLISECONDS)));
		
		entityManager.flush();
	}
	
	private MoreInfo createMoreInfo(String location, String message) {
		MoreInfo moreInfo = new MoreInfo();
		moreInfo.setItemNbr(null);
		moreInfo.setMessage(message);
		moreInfo.setLocation(location);
		return moreInfo;
	}
}
