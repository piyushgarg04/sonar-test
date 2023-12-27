package com.xpo.ltl.shipment.service.helper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.freightflow.v2.CalculateTransitTimeRqst;
import com.xpo.ltl.api.freightflow.v2.PostalTransitTime;
import com.xpo.ltl.api.freightflow.v2.TransitTimeBasic;
import com.xpo.ltl.api.location.v2.DetermineOperationalServiceDateResp;
import com.xpo.ltl.api.location.v2.GetSicForPostalCodesResp;
import com.xpo.ltl.api.location.v2.PostalSicAssignment;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.LateTenderCdTransformer;
import com.xpo.ltl.api.shipment.v2.LateTenderCd;
import com.xpo.ltl.api.shipment.v2.ServiceTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentAcquiredTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentSkeleton;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.CalculateTransitTimeResult;
import com.xpo.ltl.shipment.service.client.CalculateTransitTimeTask;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import com.xpo.ltl.shipment.service.util.TimestampUtil;

public class ShipmentSkeletonHelper {

    public static final String DEFAULT_REQ_DELIVER_DATE = "12/31/2999";
    public static final String DEFAULT_START_TIME = "00:00:00";
    public static final String DEFAULT_END_TIME = "23:59:59";
	private static final String DATE_HYPHEN = "yyyy-MM-dd";
	private static final String DEFAULT_IND_VALUE = "N";
	private static final Log LOG = LogFactory.getLog(ShipmentSkeletonHelper.class);

	public static String getDestSicCodeForPostalCode(String destPostalCd,
			List<MoreInfo> moreInfoList, GetSicForPostalCodesResp sicForPostalCodes,ExternalRestClient client,
			TransactionContext txnContext) throws ServiceException {

		if (Objects.isNull(moreInfoList)){
			moreInfoList = Lists.newArrayList();
		}
		if (StringUtils.isBlank(destPostalCd)) {
			moreInfoList.add(createMoreInfo("destPostalCd",
					ValidationErrorMessage.DEST_POSTAL_CODE_REQUIRED.message()));
		} else {

            if (Objects.nonNull(sicForPostalCodes)) {

                Optional<PostalSicAssignment> postalSicAssignment = sicForPostalCodes
                    .getPostalSicAssignments()
                    .stream()
                    .filter(postalAssignment -> destPostalCd.equals(postalAssignment.getPostalCd()))
                    .findFirst();
                if (postalSicAssignment.isPresent()) {
                    return postalSicAssignment.get().getSicCd();
                } else {
                    moreInfoList.add(createMoreInfo("destPostalCd",
                        ValidationErrorMessage.CANT_GET_DEST_SIC.message() + " " + destPostalCd));
                }

			} else {
                String[] desPostalCdArray = { destPostalCd };
                
                GetSicForPostalCodesResp getSicForPostalCodesResp = client.getSicForPostalCodes(desPostalCdArray,
                    txnContext);

                if (Objects.nonNull(getSicForPostalCodesResp)
                        && CollectionUtils.isNotEmpty(getSicForPostalCodesResp.getPostalSicAssignments())) {
                    return getSicForPostalCodesResp.getPostalSicAssignments().get(0).getSicCd();
                } else {
                    moreInfoList.add(createMoreInfo("destPostalCd",
                        ValidationErrorMessage.CANT_GET_DEST_SIC.message() + " " + destPostalCd));
                }

			}
		}
		return null;
	}
	
	private static MoreInfo createMoreInfo(String location, String message) {
		MoreInfo moreInfo = new MoreInfo();
		moreInfo.setItemNbr(null);
		moreInfo.setMessage(message);
		moreInfo.setLocation(location);
		return moreInfo;
	}
	
	public static String incrementPickupDate(String pickupDate) throws ParseException {

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_HYPHEN);
        cal.setTime(sdf.parse(pickupDate));
        cal.add(Calendar.DATE, 1);
        return sdf.format(cal.getTime());
    }

	public static String getFormattedProNbr(String parentProNbr, TransactionContext txnContext) throws ServiceException {
		String formattedProNbr = ProNumberHelper.validateProNumber(parentProNbr, txnContext);
		if (!ProNumberHelper.isBluePro(formattedProNbr)) {
			throw ExceptionBuilder
					.exception(ValidationErrorMessage.PRO_NBR_FORMAT_INVALID, txnContext)
					.moreInfo("parentProNbr",
							ValidationErrorMessage.BLUE_NUMBER_FORMAT.message() +
							formattedProNbr)
					.build();
		}
		return formattedProNbr;
	}

	public static Map<String, PostalTransitTime> getCalculateTransitTimeMap(
			Collection<CalculateTransitTimeTask> calculateTransitTimeTaskList, ExternalRestClient restClient, TransactionContext txnContext) throws ServiceException {

    	Map<String, PostalTransitTime> calculateTransitTimeMap = new HashMap<>();
    	Collection<CalculateTransitTimeResult> calculateTransitTimeResp = restClient.getCalculateTransitTime(calculateTransitTimeTaskList, txnContext);
    	CollectionUtils.emptyIfNull(calculateTransitTimeResp)
		.stream()
		.forEach(resp -> calculateTransitTimeMap.put(resp.getProNbr(), resp.getTransitTimeResp()));
		return calculateTransitTimeMap;
	}
	
	public static void populateCalculateTransitTimeTaskList(List<ShipmentSkeleton> shipmentSkeletonsList,
			GetSicForPostalCodesResp sicForPostalCodes, Map<String, ShmShipment> existentShmShipmentMap,
			Collection<CalculateTransitTimeTask> calculateTransitTimeTaskList, ExternalRestClient restClient, TransactionContext txnContext, 
			EntityManager entityManager) throws ServiceException {
    	
		for(ShipmentSkeleton shipmentSkeleton : CollectionUtils.emptyIfNull(shipmentSkeletonsList)) {
			
			ShmShipment shmShipment = existentShmShipmentMap.get(shipmentSkeleton.getParentProNbr());
            String destSicCd = ShipmentSkeletonHelper.getDestSicCd(shipmentSkeleton, shmShipment, sicForPostalCodes, restClient, Lists.newArrayList(), txnContext);

			CalculateTransitTimeRqst calculateTransitTimeRqst = buildCalculateTransitTimeRqst(shipmentSkeleton, destSicCd);

			if(!onUpdate(shmShipment) && isPickup(shipmentSkeleton)) {
		    	CalculateTransitTimeTask calculateTransitTimeTask = new CalculateTransitTimeTask(calculateTransitTimeRqst,
		    			shipmentSkeleton.getParentProNbr(), restClient, txnContext);
		    	calculateTransitTimeTaskList.add(calculateTransitTimeTask);
			}
		}
	}

    public static XMLGregorianCalendar getPickupDate(ShipmentSkeleton shipmentSkeleton) {

        XMLGregorianCalendar lastMvmtDateTime = shipmentSkeleton.getLastMovementDateTime();

        Calendar calendar = BasicTransformer.toCalendar(lastMvmtDateTime);
        if (Objects.isNull(lastMvmtDateTime) || TimestampUtil.isLowTimestamp(BasicTransformer.toTimestamp(lastMvmtDateTime))) {
            lastMvmtDateTime = TimestampUtil.toXmlGregorianCalendar(Timestamp.from(Instant.now()));
            calendar = BasicTransformer.toCalendar(lastMvmtDateTime);
        }

        if (isPickup(shipmentSkeleton)) {
            lastMvmtDateTime = TimestampUtil.isLowTimestamp(BasicTransformer.toTimestamp(lastMvmtDateTime)) ?
                TimestampUtil.toXmlGregorianCalendar(Timestamp.from(Instant.now())) :
                lastMvmtDateTime;
            calendar = BasicTransformer.toCalendar(lastMvmtDateTime);
        }

        String lateTenderCd = isPickup(shipmentSkeleton) ? LateTenderCdTransformer.toCode(shipmentSkeleton.getLateTenderCd()) : DEFAULT_IND_VALUE;
        if (LateTenderCdTransformer.toCode(LateTenderCd.LATE_TENDER).equals(lateTenderCd)) {
            calendar = BasicTransformer.toCalendar(lastMvmtDateTime);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        lastMvmtDateTime = BasicTransformer.toXMLGregorianCalendar(calendar);

        return lastMvmtDateTime;
    }


	public static boolean isPickup(ShipmentSkeleton shipmentSkeleton) {
		boolean isPickup = false;
		if (ShipmentAcquiredTypeCd.REGULAR_PKUP.equals(shipmentSkeleton.getShipmentAcquiredTypeCd())) {
            isPickup = true;
        }
		return isPickup;
	}

	public static boolean onUpdate(ShmShipment shmShipment) {
		boolean onUpdate = false;
		
		if (Objects.nonNull(shmShipment)){
			onUpdate = true;
        }
		return onUpdate;
	}

	public static CalculateTransitTimeRqst buildCalculateTransitTimeRqst(ShipmentSkeleton shipmentSkeleton, String destSicCd) {

		CalculateTransitTimeRqst calculateTransitTimeRqst = new CalculateTransitTimeRqst();
        XMLGregorianCalendar pickupDate = getPickupDate(shipmentSkeleton);

		TransitTimeBasic transitTimeBasic = new TransitTimeBasic();
        transitTimeBasic.setOrigPostalCd(shipmentSkeleton.getRequestingSicCd());
        if(shipmentSkeleton.getServiceTypeCd() == ServiceTypeCd.RAPID_REMOTE_SERVICE && null != shipmentSkeleton.getDestPostalCd()) {
        	transitTimeBasic.setDestPostalCd(shipmentSkeleton.getDestPostalCd());
        } else {
        	transitTimeBasic.setDestPostalCd(destSicCd);
        }
        transitTimeBasic.setRequestedPkupDate(pickupDate);

        calculateTransitTimeRqst.setTransitTime(Lists.newArrayList(transitTimeBasic));
        
        return calculateTransitTimeRqst;
		
	}

	public static String getDestSicCd(ShipmentSkeleton shipmentSkeleton, ShmShipment shmShipment, GetSicForPostalCodesResp sicForPostalCodes,
        ExternalRestClient client, List<MoreInfo> moreInfos, TransactionContext txnContext) throws ServiceException {

		String destSicCd = null;
		String destPostalCd = shipmentSkeleton.getDestPostalCd();
        if ((onUpdate(shmShipment) && StringUtils.isBlank(destPostalCd))) {
            destSicCd = shmShipment.getDestTrmnlSicCd();
        } else {
            destSicCd = getDestSicCodeForPostalCode(destPostalCd, moreInfos, sicForPostalCodes, client, txnContext);
        }
	
		return destSicCd;
	}
	
	public static void populateProNbrList(List<ShipmentSkeleton> shipmentSkeletonsList, List<String> formattedProNbrList, 
			TransactionContext txnContext) throws ServiceException {
		for (ShipmentSkeleton shipmentSkeleton : CollectionUtils.emptyIfNull(shipmentSkeletonsList)) {
			String formattedProNbr = getFormattedProNbr(shipmentSkeleton.getParentProNbr(), txnContext);
			shipmentSkeleton.setParentProNbr(formattedProNbr);
			formattedProNbrList.add(formattedProNbr);	
		}
	}

	public static void populateShmShipmentListAndTransitTimeTaskList(
			List<ShipmentSkeleton> shipmentSkeletonList, GetSicForPostalCodesResp sicForPostalCodes,Map<String, ShmShipment> existentShmShipmentMap,
			Collection<CalculateTransitTimeTask> calculateTransitTimeTaskList, ShmShipmentSubDAO shipmentDAO, ExternalRestClient restClient,
			TransactionContext txnContext, EntityManager entityManager) throws ServiceException {

        populateShmShipmentMap(shipmentSkeletonList, sicForPostalCodes, existentShmShipmentMap, calculateTransitTimeTaskList, shipmentDAO, restClient,
            txnContext, entityManager);

        populateCalculateTransitTimeTaskList(shipmentSkeletonList, sicForPostalCodes,
					existentShmShipmentMap, calculateTransitTimeTaskList, restClient, txnContext, entityManager);
	}

    public static void populateShmShipmentMap(List<ShipmentSkeleton> shipmentSkeletonList, GetSicForPostalCodesResp sicForPostalCodes,
        Map<String, ShmShipment> existentShmShipmentMap, Collection<CalculateTransitTimeTask> calculateTransitTimeTaskList,
        ShmShipmentSubDAO shipmentDAO, ExternalRestClient restClient, TransactionContext txnContext, EntityManager entityManager)
            throws ServiceException {

        List<String> formattedProNbrList = new ArrayList<>();
        populateProNbrList(shipmentSkeletonList, formattedProNbrList, txnContext);

        List<ShmShipment> existingShipmentList = shipmentDAO.listShipmentsByProNumbers(formattedProNbrList, entityManager);
        CollectionUtils.emptyIfNull(existingShipmentList).stream().forEach(shp -> existentShmShipmentMap.put(shp.getProNbrTxt(), shp));

    }

    public static void populateServiceStandardInfo(ShmShipment shmShipment, ServiceTypeCd serviceTypeCd, boolean isPickup,
        Map<String, PostalTransitTime> calculateTransitTimeMap, TransactionContext txnContext, ExternalRestClient restClient)
            throws ServiceException {

        if (isPickup) {

            String serviceStartDate = null;
            BigInteger standardTrnstDays = null;
            BigInteger estimatedTrnstDays = null;
            String estimatedDlvryDate = null;
            String originSic = shmShipment.getOrigTrmnlSicCd();
            String destSic = shmShipment.getDestTrmnlSicCd();
            String pickupDate = BasicTransformer.toDateString(shmShipment.getPkupDt());
            PostalTransitTime calculateTransitTimeResp = null;

            com.xpo.ltl.api.location.v2.ServiceTypeCd locServiceTypeCode = com.xpo.ltl.api.location.v2.ServiceTypeCd
                .fromValue(serviceTypeCd.toString());

            if (calculateTransitTimeMap.containsKey(shmShipment.getProNbrTxt())) {
                calculateTransitTimeResp = calculateTransitTimeMap.get(shmShipment.getProNbrTxt());
            }

            if (LateTenderCdTransformer.toCode(LateTenderCd.LATE_TENDER).equals(shmShipment.getLateTenderCd()))
                try {
                    pickupDate = ShipmentSkeletonHelper.incrementPickupDate(pickupDate);
                } catch (ParseException e) {
                    LOG.error(ExceptionUtils.getMessage(e));
                }
            serviceStartDate = populateServiceStartDate(shmShipment, txnContext, originSic, destSic, pickupDate, locServiceTypeCode,
                calculateTransitTimeResp, restClient);

            if (null != calculateTransitTimeResp && null != calculateTransitTimeResp.getTransitDays()) {
                standardTrnstDays = calculateTransitTimeResp.getTransitDays();
            }

            if (shmShipment.getEstTrnstDays().compareTo(BigDecimal.ZERO) == 0)
                estimatedTrnstDays = standardTrnstDays;

            if (StringUtils.isNotBlank(BasicTransformer.toDateString(shmShipment.getEstimatedDlvrDt()))) {
                estimatedDlvryDate = BasicTransformer.toDateString(shmShipment.getEstimatedDlvrDt());
            } else {
                if (serviceTypeCd == ServiceTypeCd.RAPID_REMOTE_SERVICE) {
                    if (null != calculateTransitTimeResp && true == calculateTransitTimeResp.getRrsInd()
                            && null != calculateTransitTimeResp.getRrsEstimatedDeliveryDate()) {
                        estimatedDlvryDate = calculateTransitTimeResp.getRrsEstimatedDeliveryDate();
                    }
                } else {
                    if (null != calculateTransitTimeResp && null != calculateTransitTimeResp.getEstdDlvrDate()) {
                        estimatedDlvryDate = calculateTransitTimeResp.getEstdDlvrDate();
                    }
                }
                if (null == estimatedDlvryDate) {
                    // determine estimated delivery date
                    DetermineOperationalServiceDateResp estdDlvryDateResp = restClient
                        .getOperationalServiceDates(originSic, destSic, serviceStartDate, locServiceTypeCode,
                            (null != standardTrnstDays) ? standardTrnstDays.intValue() : 0, true, txnContext);
                    estimatedDlvryDate = estdDlvryDateResp.getServiceDate();
                }
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                estimatedDlvryDate = formatter.format(BasicTransformer.toDate(estimatedDlvryDate));
            }

            shmShipment.setStdTrnstDays(BasicTransformer.toBigDecimal(null != standardTrnstDays ? standardTrnstDays : BigInteger.ZERO));
            shmShipment.setEstTrnstDays(BasicTransformer.toBigDecimal(null != estimatedTrnstDays ? estimatedTrnstDays : BigInteger.ZERO));
            shmShipment
                .setEstimatedDlvrDt(
                    Objects.nonNull(estimatedDlvryDate) ? BasicTransformer.toDate(estimatedDlvryDate) : TimestampUtil.getLowTimestamp());
            shmShipment.setSvcStrtDt(Objects.nonNull(serviceStartDate) ? BasicTransformer.toDate(serviceStartDate) : TimestampUtil.getLowTimestamp());

        } else {

            shmShipment.setStdTrnstDays(BigDecimal.ZERO);
            shmShipment.setEstTrnstDays(BigDecimal.ZERO);
            shmShipment.setEstimatedDlvrDt(TimestampUtil.getLowTimestamp());
            shmShipment.setSvcStrtDt(TimestampUtil.getLowTimestamp());
        }

    }

    private static String populateServiceStartDate(ShmShipment shmShipment, TransactionContext txnContext, String originSic, String destSic,
        String pickupDate, com.xpo.ltl.api.location.v2.ServiceTypeCd serviceTypeCd, PostalTransitTime calculateTransitTimeResp,
        ExternalRestClient restClient)
            throws ServiceException {
        String serviceStartDate = null;
        if (StringUtils.isNotBlank(BasicTransformer.toDateString(shmShipment.getSvcStrtDt()))) {
            serviceStartDate = BasicTransformer.toDateString(shmShipment.getSvcStrtDt());
        } else {
            if (null != calculateTransitTimeResp && null != calculateTransitTimeResp.getEarliestPkupDate()) {
                serviceStartDate = calculateTransitTimeResp.getEarliestPkupDate();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                serviceStartDate = formatter.format(BasicTransformer.toDate(serviceStartDate));
            }
            if (null == serviceStartDate) {
                DetermineOperationalServiceDateResp opSrvcDateResp = restClient
                    .getOperationalServiceDates(originSic, destSic, pickupDate, serviceTypeCd, 0, true, txnContext);
                serviceStartDate = opSrvcDateResp.getServiceDate();
            }
        }

        return serviceStartDate;
    }

}