package com.xpo.ltl.shipment.service.validators;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.location.v2.GetLocationReferenceDetailsResp;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.v2.DataStoreUseCd;
import com.xpo.ltl.api.shipment.v2.ShmNotification;
import com.xpo.ltl.api.shipment.v2.UpsertShipmentNotificationsRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.java.util.cityoperations.NumberUtil;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.enums.ShmNotificationCatgCdEnum;
import com.xpo.ltl.shipment.service.enums.ShmNotificationStatusCdEnum;
import com.xpo.ltl.shipment.service.enums.ShmNotificationTypeReasonCd;
import com.xpo.ltl.shipment.service.util.TimestampUtil;

public class UpsertShipmentNotificationsValidator extends Validator {

    private static final String TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    private static final Set<String> VALID_CATG_CODES = 
            Sets.newHashSet(
                ShmNotificationCatgCdEnum.APPOINTMENT.getCode(),
                ShmNotificationCatgCdEnum.NOTIFICATION.getCode());
    private static final Set<String> VALID_STATUS_CODES = 
            Sets.newHashSet(
                ShmNotificationStatusCdEnum.APPOINTMENT_SET.getCode(),
                ShmNotificationStatusCdEnum.PENDING.getCode(),
                ShmNotificationStatusCdEnum.CANCELLED.getCode(),
                ShmNotificationStatusCdEnum.DELIVERY_COMPLETED.getCode(),
                ShmNotificationStatusCdEnum.RESCHEDULE_REQUIRED.getCode(),
                ShmNotificationStatusCdEnum.APPOINTMENT_NOT_REQUIRED.getCode(),
                ShmNotificationStatusCdEnum.RESCHEDULE.getCode(),
                ShmNotificationStatusCdEnum.REMOVED_FROM_APPOINTMENT.getCode());
    private static final Set<String> VALID_TYPE_REASON_CODES = 
            Sets.newHashSet(
                ShmNotificationTypeReasonCd.DRIVER_COLLECT.getCode(),
                ShmNotificationTypeReasonCd.NOA_OR_DNC.getCode(),
                ShmNotificationTypeReasonCd.CONSTRUCTION_SITE.getCode(),
                ShmNotificationTypeReasonCd.RESIDENTIAL.getCode(),
                ShmNotificationTypeReasonCd.OTHER.getCode());
    
    @Inject private ExternalRestClient externalRestClient;
    
    
    /**
     * Validates the data on the request.  <BR>Note that the consumer takes the responsibility that the integrity of the scoNtficnInstId on the request is valid before
     * calling this API as there is not a way of validating it here to ensure it is a valid foreign key to SCO, especially for consumers outside of this transaction
     * boundary which have not committed their changes to save scoNtficnInstId before calling this API.  Thus, this scenario prevents this API from validating scoNtficnInstId
     * as a proper foreign key.
     */
    public void validate(UpsertShipmentNotificationsRqst request, TransactionContext txnContext) throws ValidationException {
        
        List<MoreInfo> moreInfo = new ArrayList<>();

        if (request == null || CollectionUtils.isEmpty(request.getShipmentNotifications())) {
            moreInfo.add(createMoreInfo("shipmentNotificationUpserts", ValidationErrorMessage.GENERIC_VAL_ERR.message("The shipment notification data was missing from the request.")));
        } else {
            if (null == request.getDataStoreUseCd()) {
                moreInfo.add(createMoreInfo("dataStoreUseCd", ValidationErrorMessage.GENERIC_VAL_ERR.message("The data store use code was missing from the request.")));
            }
            for (ShmNotification shmNotification : request.getShipmentNotifications()) {
                if (NumberUtil.isNullOrZero(shmNotification.getShipmentInstId())) {
                    moreInfo.add(createMoreInfo("shipmentInstId", ValidationErrorMessage.GENERIC_VAL_ERR.message("The shipment instance ID is required for the request.")));
                }
                if (NumberUtil.isNullOrZero(shmNotification.getScoNtficnInstId())) {
                    moreInfo.add(createMoreInfo("scoNtficnInstId", ValidationErrorMessage.GENERIC_VAL_ERR.message("The operations notification instance ID is required for the request.")));
                }
                if (request.getDataStoreUseCd() != null && request.getDataStoreUseCd() == DataStoreUseCd.LEGACY_ONLY) {
                    // Oracle is the source of reference and DB2 requests are regarded as backsync requests using Oracle data, so the sequence number is required when 
                    // backsyncing to DB2, even for an insert of a new notification.
                    if (NumberUtil.isNullOrZero(shmNotification.getNotificationSequenceNbr())) {
                        moreInfo.add(createMoreInfo("NotificationSequenceNbr", ValidationErrorMessage.GENERIC_VAL_ERR.message("Notification sequence number is required for DB2 requests.")));
                    }
                }
                if (StringUtils.isBlank(shmNotification.getCategoryCd())) {
                    moreInfo.add(createMoreInfo("categoryCd", ValidationErrorMessage.GENERIC_VAL_ERR.message("The category code for the notification is required for the request.")));
                } else {
                    if (!VALID_CATG_CODES.contains(shmNotification.getCategoryCd())) {
                        moreInfo.add(createMoreInfo("categoryCd", ValidationErrorMessage.GENERIC_VAL_ERR.message("The category code was not a valid category type.")));
                    }
                }
                if (StringUtils.isBlank(shmNotification.getSicCd())) {
                    moreInfo.add(createMoreInfo("sicCd", ValidationErrorMessage.GENERIC_VAL_ERR.message("The SIC code is required for the request.")));
                } else {
                    try {
                        GetLocationReferenceDetailsResp resp = externalRestClient.getLocationReferenceDetails(shmNotification.getSicCd(), txnContext);
                    } catch (ServiceException e) {
                        moreInfo.add(createMoreInfo("sicCd", ValidationErrorMessage.GENERIC_VAL_ERR.message("The SIC code was not a valid value.")));
                    }
                }
                if (StringUtils.isBlank(shmNotification.getStatusCd())) {
                    moreInfo.add(createMoreInfo("statusCd", ValidationErrorMessage.GENERIC_VAL_ERR.message("The status code is required for the request.")));
                } else {
                    if (!VALID_STATUS_CODES.contains(shmNotification.getStatusCd())) {
                        moreInfo.add(createMoreInfo("statusCd", ValidationErrorMessage.GENERIC_VAL_ERR.message("The status code was not a valid value.")));
                    }
                }
                if (StringUtils.isBlank(shmNotification.getScheduledDeliveryDate())) {
                    moreInfo.add(createMoreInfo("scheduledDeliveryDate", ValidationErrorMessage.GENERIC_VAL_ERR.message("The scheduled delivery date is required for the request.")));
                }
                if (StringUtils.isBlank(shmNotification.getScheduledDeliveryFromTime())) {
                    moreInfo.add(createMoreInfo("scheduledDeliveryFromTime", ValidationErrorMessage.GENERIC_VAL_ERR.message("The scheduled delivery from-time is required for the request.")));
                } else if (StringUtils.isBlank(shmNotification.getScheduledDeliveryToTime())) {
                    moreInfo.add(createMoreInfo("scheduledDeliveryToTime", ValidationErrorMessage.GENERIC_VAL_ERR.message("The scheduled delivery to-time is required for the request.")));
                } else {
                    Date scheduledFromDateTime = BasicTransformer.toDate(TimestampUtil.stringToXmlGregorianCalendar(shmNotification.getScheduledDeliveryFromTime(), TIME_PATTERN));
                    Date scheduledToDateTime = BasicTransformer.toDate(TimestampUtil.stringToXmlGregorianCalendar(shmNotification.getScheduledDeliveryToTime(), TIME_PATTERN));
                    if (scheduledToDateTime.before(scheduledFromDateTime)) {
                        moreInfo.add(createMoreInfo("scheduledDeliveryToTime", ValidationErrorMessage.GENERIC_VAL_ERR.message("The scheduled delivery to-time cannot be before the from-time.")));
                    }
                }
                if (StringUtils.isBlank(shmNotification.getCallerRacfId())) {
                    moreInfo.add(createMoreInfo("callerRacfId", ValidationErrorMessage.GENERIC_VAL_ERR.message("The employee ID of the caller setting the notification is required for the request.")));
                }
                if (null == shmNotification.getCallDateTime()) {
                    moreInfo.add(createMoreInfo("callDateTime", ValidationErrorMessage.GENERIC_VAL_ERR.message("The call date and time is required for the request.")));
                }
                if (StringUtils.isNotBlank(shmNotification.getTypeCd())) {
                    if (!VALID_TYPE_REASON_CODES.contains(shmNotification.getTypeCd())) {
                        moreInfo.add(createMoreInfo("callDateTime", ValidationErrorMessage.GENERIC_VAL_ERR.message("The type code on the request was not a valid value.")));
                    }
                }
            }
        }
        checkMoreInfo(txnContext, moreInfo);
    }
}
