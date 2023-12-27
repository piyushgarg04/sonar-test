package com.xpo.ltl.shipment.service.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.threeten.bp.LocalTime;

import com.xpo.ltl.api.customer.v2.ContactPerson;
import com.xpo.ltl.api.shipment.v2.ShmNotification;
import com.xpo.ltl.api.transformer.BasicTransformer;

public class ShmNotificationUtil {

    private ShmNotificationUtil() {}
    
    /**
     * Determine if the shipment appointment notification applies to the entire day or note.
     */
    public static Boolean doesAppointmentApplyEntireDay(ShmNotification appointmentNotifications) {
        
        Boolean doesAppointmentApplyEntireDay = Boolean.FALSE;
        
        // If the To and From times are zero, then the appointment applies to the entire day
        if (StringUtils.isNotBlank(appointmentNotifications.getScheduledDeliveryFromTime())
                && StringUtils.isNotBlank(appointmentNotifications.getScheduledDeliveryToTime())) {
            LocalTime localTimeFrom = LocalTime.parse(appointmentNotifications.getScheduledDeliveryFromTime());
            LocalTime localTimeTo = LocalTime.parse(appointmentNotifications.getScheduledDeliveryToTime());
            if (localTimeFrom.getHour() == NumberUtils.INTEGER_ZERO
                    && localTimeFrom.getMinute() == NumberUtils.INTEGER_ZERO
                    && localTimeTo.getHour() == NumberUtils.INTEGER_ZERO
                    && localTimeTo.getMinute() == NumberUtils.INTEGER_ZERO) {
                doesAppointmentApplyEntireDay = Boolean.TRUE;
            }
        }
        return doesAppointmentApplyEntireDay;
    }
    
    public static List<com.xpo.ltl.api.shipment.service.entity.ShmNotification> listNotificationsByShpId(
            Long shpInstId, 
            List<com.xpo.ltl.api.shipment.service.entity.ShmNotification> shmNotifications) {
        
        if (CollectionUtils.isEmpty(shmNotifications)) {
            return new ArrayList<>();
        }
        return shmNotifications.stream()
                .filter(shp -> shp.getId().getShpInstId() == shpInstId.longValue())
                .collect(Collectors.toList());
    }
    
    public static Long findLastSeqNbrForThisShipment(List<com.xpo.ltl.api.shipment.service.entity.ShmNotification> shmNotifications) {
        
        if (CollectionUtils.isEmpty(shmNotifications)) return NumberUtils.LONG_ZERO;
        
        return BasicTransformer.toLong(shmNotifications.stream()
                .max(Comparator.comparingLong(x -> BasicTransformer.toLong(x.getId().getNtfictnSeqNbr())))
                .get().getId().getNtfictnSeqNbr());
    }
    
    public static com.xpo.ltl.api.shipment.service.entity.ShmNotification getNotificationBySeqNbrAndShpInstId(
            Long shpInstId, 
            Long ntfnSeqNbr, 
            List<com.xpo.ltl.api.shipment.service.entity.ShmNotification> shmNotifications) {
        
        if (CollectionUtils.isEmpty(shmNotifications)) {
            return null;
        }
        return shmNotifications.stream()
                .filter(notification -> notification.getId().getShpInstId() == shpInstId.longValue() 
                            && notification.getId().getNtfictnSeqNbr() == ntfnSeqNbr.longValue())
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Extracts first and last name from the full name attribute of the delivery shipment notification.
     */
    public static ContactPerson getContactPersonFromNotificationName(String contactNameFull) {
        
        ContactPerson contactPerson = new ContactPerson();
        
        if (StringUtils.isNotBlank(contactNameFull)) {
            contactNameFull.trim();
            String firstName = StringUtils.SPACE;
            String lastName = StringUtils.SPACE;            
            int firstSpace = contactNameFull.indexOf(" ");
            
            // Some consuming applications only require a first name, so if no other identifiable parts exist in the full name, regard the whole string as the first name only.
            if (firstSpace == -1) {
                firstName = contactNameFull;
            } else {
                firstName = contactNameFull.substring(0, firstSpace);
                // Anything left?  Treat it as the last name, including any part of a name that might be hypenated
                if (contactNameFull.length() > firstSpace) {
                    lastName = contactNameFull.substring(firstSpace).trim();
                }
            }
            contactPerson.setFirstName(firstName);
            contactPerson.setLastName(lastName);
        } else {
            contactPerson = null;
        }
        return contactPerson;
    }
    
    /**
     * Phone numbers are stored as one attribute; this function will break the phone part into the area code and phone number attributes.
     */
    public static ContactPerson getContactPhoneNumberFromNotificationPhone(final String fullPhoneNumber) {
        
        ContactPerson contactPerson = new ContactPerson();
        
        if (StringUtils.isNotBlank(fullPhoneNumber)) {
            String phoneNbrAreaCd = StringUtils.SPACE;
            String phoneNbr = StringUtils.SPACE;
            String fullPhoneTxt = fullPhoneNumber.trim();
            fullPhoneTxt.replace("(", "");
            fullPhoneTxt.replace(")", "");
            fullPhoneTxt.replace("-", "");
            fullPhoneTxt.replace(".", "");
            fullPhoneTxt.trim();
            if (fullPhoneTxt.length() > 4) {
                phoneNbrAreaCd = (String)fullPhoneTxt.substring(0, 3);
                phoneNbr = (String)fullPhoneTxt.substring(3);
                contactPerson.setBusinessPhoneAreaCd(phoneNbrAreaCd);
                contactPerson.setBusinessPhoneNbr(phoneNbr);
            }
        } else {
            contactPerson = null;
        }
        return contactPerson;
    }
}