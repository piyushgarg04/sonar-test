package com.xpo.ltl.shipment.service.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.service.entity.ShmAdvBydCarr;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCust;
import com.xpo.ltl.api.shipment.service.entity.ShmCustomsBond;
import com.xpo.ltl.api.shipment.service.entity.ShmCustomsCntrl;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmNotification;
import com.xpo.ltl.api.shipment.service.entity.ShmRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.service.entity.ShmSrNbr;
import com.xpo.ltl.api.shipment.service.entity.ShmTmDtCritical;
import com.xpo.ltl.api.shipment.transformer.v2.DeliveryQualifierCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.RouteTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.DeliveryQualifierCd;
import com.xpo.ltl.api.shipment.v2.RouteTypeCd;
import com.xpo.ltl.api.shipment.v2.Shipment;
import com.xpo.ltl.api.shipment.v2.ShipmentId;
import com.xpo.ltl.api.transformer.BasicTransformer;

public class ShipmentUtil {

	private static final RouteTypeCd NOT_ROUTE = RouteTypeCd.NOT_ROUTED;
	private static final DeliveryQualifierCd PARTIAL_SHORT = DeliveryQualifierCd.PARTIAL_SHORT;
	private static final DeliveryQualifierCd ALL_SHORT = DeliveryQualifierCd.ALL_SHORT;
	private static final String DEFAULT_BLANK = " ";  // keep this strictly a blank, do not change to an empty length string, which is translated differently downstream in legacy SCO
	private static final String DEFAULT_SPACE = "";
	private static final String FINAL_DELIVERED = "Z";
	private static final String ON_SPOTTED_TRAILER = "F";
	private static double CUBIC_INCHES_CUBIC_FEET_FACTOR = 1728;
	
	private ShipmentUtil() {}
	
	/**
	 * Sets the Route Type Code to be used for a shipment update.  Based on the current route type code setting
	 * and the current state of the delivery qualifier code for the shipment.
	 */
	public static String determineRouteTypeCode(String routeTypeCd, String deliveryQualfrCode) {
		
		//PCT-4750 - Route type depends on the delivery qualifier code of the shipment at that time
		String returnRouteTypeCode = DEFAULT_BLANK;   // Keep this as a blank, do not change to an empty length string
		
		if (StringUtils.equals(routeTypeCd, RouteTypeCdTransformer.toCode(RouteTypeCd.ROUTED))
				&& (!StringUtils.equals(deliveryQualfrCode.trim(), DEFAULT_SPACE)
				&&  !StringUtils.equals(deliveryQualfrCode, FINAL_DELIVERED))
				&&  !StringUtils.equals(deliveryQualfrCode, ON_SPOTTED_TRAILER)) {
			returnRouteTypeCode = RouteTypeCd.STAGED.name(); 
		}
		return returnRouteTypeCode;
	}

	public static List<ShmShipment> getShmShipmentsById(List<Long> shpInstIdList, List<ShmShipment> shmShipments) {
		if(CollectionUtils.isEmpty(shmShipments) || CollectionUtils.isEmpty(shpInstIdList)) {
			return Lists.newArrayList();
		}
		List<ShmShipment> returnShipmentList = Lists.newArrayList();
		for (Long oneShipmentId : shpInstIdList) {
			for (ShmShipment oneShmShipment : shmShipments) {
				if (oneShipmentId == oneShmShipment.getShpInstId())  {
					returnShipmentList.add(oneShmShipment);
					break;
				}
			}
		}
		return returnShipmentList;
	}
	
	public static ShmShipment findShipmentByShpInstId(Long shpInstId, List<ShmShipment> shmShipments) {
	    
	    if (CollectionUtils.isEmpty(shmShipments)) {
	        return null;
	    }
	    return shmShipments.stream()
	            .filter(shp -> shp.getShpInstId() == shpInstId.longValue())
	            .findFirst()
	            .orElse(null);
	}

	public static List<ShmAsEntdCust> getShipmentPartiesByShpId(List<ShmAsEntdCust> asEntdCustList, long shpInstanceId) {
		if (CollectionUtils.isEmpty(asEntdCustList)) {
			return Lists.newArrayList();
		} else {
			return asEntdCustList.stream()
					.filter(asEntdCust -> asEntdCust.getId().getShpInstId() == shpInstanceId)
					.collect(Collectors.toList());
		}
	}

	public static List<ShmMovement> getMovementsByShipmentId(List<ShmMovement> shmMovements, Long shipmentId) {
		return shmMovements.stream().filter(movement -> movement.getId().getShpInstId() == shipmentId).collect(Collectors.toList());
	}

	public static boolean hasNotShipmentInstIds(List<ShipmentId> shipmentIds) {
		return CollectionUtils.isEmpty(shipmentIds) || CollectionUtils.emptyIfNull(shipmentIds).stream().anyMatch(shipmentId -> StringUtils.isEmpty(shipmentId.getShipmentInstId()));
	}

	public static boolean anyHasShipmentInstIdSet(List<ShipmentId> shipmentIds) {
		return CollectionUtils.isEmpty(shipmentIds) || CollectionUtils.emptyIfNull(shipmentIds).stream().anyMatch(shipmentId -> StringUtils.isNotEmpty(shipmentId.getShipmentInstId()));
	}

	public static boolean isPRONumbersSetEmpty(List<ShipmentId> shipmentIds) {
		return CollectionUtils.isEmpty(shipmentIds) || CollectionUtils.emptyIfNull(shipmentIds).stream()
				.anyMatch(shipmentId -> StringUtils.isBlank(shipmentId.getProNumber()));
	}

	public static boolean isShipmentPkupDateSetEmpty(List<ShipmentId> shipmentIds) {
		return CollectionUtils.isEmpty(shipmentIds) || CollectionUtils.emptyIfNull(shipmentIds).stream()
				.anyMatch(shipmentId -> shipmentId.getPickupDate() == null);
	}

	public static ShmMovement getLastCreatedMovement(List<ShmMovement> shmMovements, Long shipmentId) {
		return Collections.max(getMovementsByShipmentId(shmMovements, shipmentId), Comparator.comparing(ShmMovement::getCrteTmst));
	}

	public static ShmMovement getLastSequenceId(List<ShmMovement> shmMovements, Long shipmentId) {
		return Collections.max(getMovementsByShipmentId(shmMovements, shipmentId), Comparator.comparing(c -> c.getId().getSeqNbr()));
	}


	public static List<String> getProNumbers(List<ShipmentId> shipmentIds) {
		return shipmentIds.stream().map(ShipmentId::getProNumber).collect(Collectors.toList());
	}

	public static List<String> getProNumbersFromShipment(List<Shipment> shipments) {
		return CollectionUtils.emptyIfNull(shipments).stream().map(Shipment::getProNbr).collect(Collectors.toList());
	}


	public static List<Long> getParentIds(List<ShmShipment> shmShipments) {
		return shmShipments.stream()
				.filter(shmShipment -> isValidParentShipment(shmShipment.getShmShipment()))
				.map(shmShipment -> BasicTransformer.toLong(shmShipment.getParentInstId()))
				.collect(Collectors.toList());
	}

	public static boolean isValidParentShipment(ShmShipment parentShipment) {
		if (Objects.isNull(parentShipment)) {
			return false;
		}
		RouteTypeCd routeTypeCd = RouteTypeCdTransformer.toEnum(parentShipment.getRteTypCd());
		DeliveryQualifierCd deliveryQualifierCd = DeliveryQualifierCdTransformer.toEnum(parentShipment.getDlvryQalfrCd());
		return (deliveryQualifierCd == PARTIAL_SHORT || deliveryQualifierCd == ALL_SHORT) &&
				routeTypeCd == NOT_ROUTE;
	}

	public static List<String> getProNbr(List<ShmShipment> shmShipments) {
		return shmShipments.stream()
				.map(ShmShipment::getProNbrTxt)
				.collect(Collectors.toList());
	}

	public static Set<Long> getShipmentIds(List<ShipmentId> shmShipmentIds) {
		return CollectionUtils.emptyIfNull(shmShipmentIds)
				.stream()
				.filter(shmShipmentId -> StringUtils.isNotEmpty(shmShipmentId.getShipmentInstId()))
				.map(shmShipmentId -> BasicTransformer.toLong(shmShipmentId.getShipmentInstId()))
				.collect(Collectors.toSet());
	}


	public static ShmShipment getParentShipmentByShipmentId(List<ShmShipment> parentShmShipments, Long parentId) {
		return parentShmShipments.stream()
				.filter(shmShipment -> shmShipment.getShmShipment().getShpInstId() == parentId)
				.findFirst().orElse(null);
	}

	public static ShmMovementExcp getExceptionByShipmentId(List<ShmMovementExcp> shipmentExceptions, Long shipmentId) {
		return shipmentExceptions.stream()
				.filter(shipmentException -> shipmentException.getId().getShpInstId() == shipmentId)
				.findFirst().orElse(null);
	}

	public static List<ShmAcSvc> getShipmentAccessorialsByShpId(List<ShmAcSvc> acList, long shpInstanceId) {
		if (CollectionUtils.isEmpty(acList)) {
			return Lists.newArrayList();
		} else {
			return acList.stream()
					.filter(accessorial -> accessorial.getId().getShpInstId() == shpInstanceId)
					.collect(Collectors.toList());
		}
	}

	public static List<ShmAdvBydCarr> getShipmentAdvBydByShpId(List<ShmAdvBydCarr> advBydList, long shpInstanceId) {
		if (CollectionUtils.isEmpty(advBydList)) {
			return Lists.newArrayList();
		} else {
			return advBydList.stream()
					.filter(advByd -> advByd.getId().getShpInstId() == shpInstanceId)
					.collect(Collectors.toList());
		}
	}

	public static List<ShmCustomsBond> getShipmentCustomsBondByShpId(List<ShmCustomsBond> customsBondList, long shpInstanceId) {
		if (CollectionUtils.isEmpty(customsBondList)) {
			return Lists.newArrayList();
		} else {
			return customsBondList.stream()
					.filter(customsBond -> customsBond.getId().getShpInstId() == shpInstanceId)
					.collect(Collectors.toList());
		}
	}

	public static List<ShmCustomsCntrl> getShipmentCustomsCntlByShpId(List<ShmCustomsCntrl> customsCntlList, long shpInstanceId) {
		if (CollectionUtils.isEmpty(customsCntlList)) {
			return Lists.newArrayList();
		} else {
			return customsCntlList.stream()
					.filter(customsCntl -> customsCntl.getId().getShpInstId() == shpInstanceId)
					.collect(Collectors.toList());
		}
	}

	public static List<ShmMiscLineItem> getShipmentMiscLineItemByShpId(List<ShmMiscLineItem> miscLineItemList, long shpInstanceId) {
		if (CollectionUtils.isEmpty(miscLineItemList)) {
			return Lists.newArrayList();
		} else {
			return miscLineItemList.stream()
					.filter(miscLineItem -> miscLineItem.getId().getShpInstId() == shpInstanceId)
					.collect(Collectors.toList());
		}
	}

	public static List<ShmRemark> getShipmentRemarksByShpId(List<ShmRemark> remarksList, long shpInstanceId) {
		if (CollectionUtils.isEmpty(remarksList)) {
			return Lists.newArrayList();
		} else {
			return remarksList.stream()
					.filter(remark -> remark.getId().getShpInstId() == shpInstanceId)
					.collect(Collectors.toList());
		}
	}

	public static List<ShmSrNbr> getShipmentSupRefNumbersByShpId(List<ShmSrNbr> supRefList, long shpInstanceId) {
		if (CollectionUtils.isEmpty(supRefList)) {
			return Lists.newArrayList();
		} else {
			return supRefList.stream()
					.filter(supRef -> supRef.getId().getShpInstId() == shpInstanceId)
					.collect(Collectors.toList());
		}
	}
	
	public static ShmTmDtCritical getShipmentTdcByShpId(List<ShmTmDtCritical> tdcList, long shpInstanceId) {
		List<ShmTmDtCritical> tdcThisShipment = tdcList.stream()
					.filter(tdc -> tdc.getShpInstId() == shpInstanceId)
					.collect(Collectors.toList());
		return CollectionUtils.isNotEmpty(tdcThisShipment) ? tdcThisShipment.get(0) : null;   // TDC is only one row per shipment, not a list
	}
	
	public static List<ShmNotification> getShipmentNotificationyShpId(List<ShmNotification> notificationList, long shpInstanceId) {
		List<ShmNotification> shmNotifications = notificationList.stream()
					.filter(ntf -> ntf.getId().getShpInstId() == shpInstanceId)
					.collect(Collectors.toList());
		return CollectionUtils.isNotEmpty(shmNotifications) ? shmNotifications : new ArrayList<ShmNotification>();   
	}

    public static ShmMovement getLatestMovement(Collection<ShmMovement> movements) {
        if (CollectionUtils.isEmpty(movements))
            return null;
        return movements.stream()
            .max(Comparator.comparing(movement -> movement.getId().getSeqNbr()))
            .orElse(null);
    }

    public static List<ShipmentId> convertToShipmentIdList(List<Long> shipmentInstIds) {
        return CollectionUtils.emptyIfNull(shipmentInstIds).stream().map(shipment -> {
            final ShipmentId shipmentId = new ShipmentId();
            shipmentId.setShipmentInstId(BasicTransformer.toString(shipment));
            return shipmentId;
        }).collect(Collectors.toList());
    }

	public static Set<Long> convertToLongSetShipmentInstId(List<Shipment> shipments) {
		return CollectionUtils.emptyIfNull(shipments)
				.stream()
				.map(Shipment::getShipmentInstId)
				.collect(Collectors.toSet());
	}

	/**
	 * Calculates volume of handling unit and convert cubic inches to cubic Feet
	 *
	 * @param length in inches
	 * @param width  in inches
	 * @param height in inches
	 * @return volume of handling unit in cubic feet
	 */
	public static double calculateVolCubFt(double length, double width, double height){
		return (length * width * height) / CUBIC_INCHES_CUBIC_FEET_FACTOR;
	}
}
