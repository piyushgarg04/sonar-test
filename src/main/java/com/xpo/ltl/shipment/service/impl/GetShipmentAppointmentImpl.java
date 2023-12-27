package com.xpo.ltl.shipment.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;
import com.xpo.ltl.api.cityoperations.v1.CustomerOperationsNote;
import com.xpo.ltl.api.cityoperations.v1.DeliveryNotification;
import com.xpo.ltl.api.cityoperations.v1.DeliveryShipmentSearchFilter;
import com.xpo.ltl.api.cityoperations.v1.DeliveryShipmentSearchRecord;
import com.xpo.ltl.api.cityoperations.v1.GetDeliveryNotificationResp;
import com.xpo.ltl.api.cityoperations.v1.NotificationLog;
import com.xpo.ltl.api.cityoperations.v1.SearchDeliveryShipmentsResp;
import com.xpo.ltl.api.cityoperations.v1.SearchDeliveryShipmentsRqst;
import com.xpo.ltl.api.cityoperations.v1.XrtAttributeFilter;
import com.xpo.ltl.api.customer.v2.ContactPerson;
import com.xpo.ltl.api.customer.v2.CustomerContact;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.humanresource.v2.InterfaceEmployee;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.BillClassCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ChargeToCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MiscLineItemCdTransformer;
import com.xpo.ltl.api.shipment.v2.AsMatchedParty;
import com.xpo.ltl.api.shipment.v2.BillClassCd;
import com.xpo.ltl.api.shipment.v2.ChargeToCd;
import com.xpo.ltl.api.shipment.v2.GetShipmentAppointmentResp;
import com.xpo.ltl.api.shipment.v2.ListShipmentsResp;
import com.xpo.ltl.api.shipment.v2.ListShipmentsRqst;
import com.xpo.ltl.api.shipment.v2.MiscLineItem;
import com.xpo.ltl.api.shipment.v2.MiscLineItemCd;
import com.xpo.ltl.api.shipment.v2.Shipment;
import com.xpo.ltl.api.shipment.v2.ShipmentAppointment;
import com.xpo.ltl.api.shipment.v2.ShipmentDetailCd;
import com.xpo.ltl.api.shipment.v2.ShipmentDetails;
import com.xpo.ltl.api.shipment.v2.ShipmentId;
import com.xpo.ltl.api.shipment.v2.ShmNotification;
import com.xpo.ltl.api.shipment.v2.TimeDateCritical;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.java.util.cityoperations.DateUtils;
import com.xpo.ltl.java.util.cityoperations.DozerMapper;
import com.xpo.ltl.java.util.cityoperations.NumberUtil;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShmNotificationSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.dto.ShipmentAppointmentDTO;
import com.xpo.ltl.shipment.service.enums.NotificationEventCdEnum;
import com.xpo.ltl.shipment.service.enums.ShipmentAppointmentStatusEnum;
import com.xpo.ltl.shipment.service.enums.ShmNotificationCatgCdEnum;
import com.xpo.ltl.shipment.service.util.AsMatchedPartyUtil;
import com.xpo.ltl.shipment.service.util.CustomerUtil;
import com.xpo.ltl.shipment.service.util.MiscLineItemUtil;
import com.xpo.ltl.shipment.service.util.ShmNotificationUtil;
import com.xpo.ltl.shipment.service.validators.Validator;

public class GetShipmentAppointmentImpl extends Validator {

    private static final String PICKUP_DT_HIGH = "3999-12-31";
    
    private static final List<String> FIELDS = Arrays.asList("shipmentInstId" ,"proNbr", "routeInstId");
    private static final Set<BillClassCd> CHILD_BILL_CLS = Sets.newHashSet(BillClassCd.MASTER_SEGMENT, BillClassCd.PARTIAL_SEGMENT, BillClassCd.ASTRAY_FRT_SEGMENT);
    public static final String TIME_FORMAT = "HH:mm:ss";

    // Variable indexes
    private static final int SHP_INST_ID = 0;
    private static final int PRO_NBR = 1;
    private static final int EST_DL_DT = 2;
    private static final int TOT_PCS = 3;
    private static final int TOT_WGT = 4;
    private static final int TOT_CHG = 5;
    private static final int BILL_CLS = 6;
    private static final int CHRG_TO_CD = 7;
    private static final int PARENT_SHP_ID = 8;
    private static final int TDC_DT_TYP_CD = 9;
    private static final int TDC_DT_1 = 10;
    private static final int TDC_DT_2 = 11;
    private static final int TDC_TM_TYP_CD = 12;
    private static final int TDC_TM_1 = 13;
    private static final int TDC_TM_2 = 14;
    private static final int LN_TYP_CD = 15;
    private static final int LN_AMT = 16;

    private @Inject ShmShipmentSubDAO shmShipmentSubDAO;
    
    private @Inject ShmNotificationSubDAO shmNotificationSubDAO;
    
    private @Inject ExternalRestClient externalRestClient;
    
    private @Inject ListShipmentsImpl listShipmentsImpl;
    
    public GetShipmentAppointmentResp getShipmentAppointment(String proNbr, Long shpInstId, TransactionContext txnContext, EntityManager entityManager) throws ServiceException {

        validate(proNbr, shpInstId, txnContext);
        
        ShipmentAppointmentDTO shipmentAppointmentDataHolder = initShipmentAppointmentDataHolder(proNbr, shpInstId, txnContext);
        
        getShipmentDetails(shipmentAppointmentDataHolder, entityManager);
        
        getAppointmentDetails(shipmentAppointmentDataHolder, entityManager);
        
        getOperationsNotification(shipmentAppointmentDataHolder);
        
        getNoaDetails(shipmentAppointmentDataHolder);
        
        handleAppointmentIndicatorsCases(shipmentAppointmentDataHolder);
        
        getShipmentsWithAppointments(shipmentAppointmentDataHolder, entityManager);
        
        getShipmentsWithoutAppointmentsThisConsignee(shipmentAppointmentDataHolder, entityManager);
        
        getRouteInstId(shipmentAppointmentDataHolder, txnContext);
        
        return buildResponse(shipmentAppointmentDataHolder);
    }
    
    private void validate(String proNbr, Long shpInstId, TransactionContext txnContext) throws ValidationException {

        List<MoreInfo> moreInfos = new ArrayList<>();
    
        if (StringUtils.isBlank(proNbr) && NumberUtil.isNullOrZero(shpInstId)) {
            addMoreInfo(moreInfos, "proNbrs", "Must specify either a PRO number or shipment id on the same request.");
        } else {
            if (StringUtils.isNotBlank(proNbr) && NumberUtil.isNonZero(shpInstId)) {
                addMoreInfo(moreInfos, "proNbrs", "Cannot have both pro number and shipment id on the same request");
            }        
        }
        checkMoreInfo(txnContext, moreInfos);
    }
    
    private ShipmentAppointmentDTO initShipmentAppointmentDataHolder(String proNbr, Long shpInstId, TransactionContext txnContext) {
        
        ShipmentAppointmentDTO shipmentAppointmentDataHolder = new ShipmentAppointmentDTO();
        shipmentAppointmentDataHolder.setProNbr(proNbr);
        shipmentAppointmentDataHolder.setShpInstId(shpInstId);
        shipmentAppointmentDataHolder.setTxnContext(txnContext);
        
        return shipmentAppointmentDataHolder;
    }
    
    /**
     * Gets the shipment header, consignee and any TDC information for that shipment for the response.
     */
    private void getShipmentDetails(ShipmentAppointmentDTO shipmentAppointmentDataHolder, EntityManager entityManager) throws ServiceException {
        
        String shpInstIdStr = StringUtils.SPACE;
        String proNbrStr = StringUtils.SPACE;
        List<ShipmentId> shipmentIds = new ArrayList<>();

        if (NumberUtil.isNonZero(shipmentAppointmentDataHolder.getShpInstId())) {
            shpInstIdStr = BasicTransformer.toString(shipmentAppointmentDataHolder.getShpInstId());
        }
        if (StringUtils.isNotBlank(shipmentAppointmentDataHolder.getProNbr())) {
            proNbrStr = shipmentAppointmentDataHolder.getProNbr();
        }
        ShipmentId shipmentId = new ShipmentId();
        shipmentId.setShipmentInstId(shpInstIdStr);
        shipmentId.setProNumber(proNbrStr);
        shipmentIds.add(shipmentId);

        ListShipmentsResp listShipmentsResp = listShipmentsForShipment(shipmentIds, shipmentAppointmentDataHolder.getTxnContext(), entityManager);
        
        if (null != listShipmentsResp && CollectionUtils.isNotEmpty(listShipmentsResp.getShipmentDetails())) {
            ShipmentDetails shipmentDetails = listShipmentsResp.getShipmentDetails().get(0);
            if (null == shipmentDetails.getShipment()) {
                createShipmentNotFoundError(shipmentAppointmentDataHolder);
            }
            Shipment shipment = listShipmentsResp.getShipmentDetails().get(0).getShipment();
            shipmentAppointmentDataHolder.setShipment(shipment);
            if (NumberUtil.isNullOrZero(shipmentAppointmentDataHolder.getShpInstId())) {
                shipmentAppointmentDataHolder.setShpInstId(shipment.getShipmentInstId());
            }
            if (StringUtils.isBlank(shipmentAppointmentDataHolder.getProNbr())) {
                shipmentAppointmentDataHolder.setProNbr(shipment.getProNbr());
            }
            getConsignee(shipmentAppointmentDataHolder, shipmentDetails);
            getTdc(shipmentAppointmentDataHolder, shipmentDetails, entityManager);
        } else {
            createShipmentNotFoundError(shipmentAppointmentDataHolder);
        }
    }
    
    private ListShipmentsResp listShipmentsForShipment(List<ShipmentId> shipmentIds, TransactionContext txnContext, EntityManager entityManager) throws ServiceException {
        
        ListShipmentsRqst listShipmentsRqst = new ListShipmentsRqst();
        List<ShipmentDetailCd> shipmentDetailCds = new ArrayList<>();
        shipmentDetailCds.add(ShipmentDetailCd.SHIPMENT_PARTIES);
        shipmentDetailCds.add(ShipmentDetailCd.TIME_DATE_CRITICAL);
        listShipmentsRqst.setShipmentDetailCd(shipmentDetailCds);
        listShipmentsRqst.setShipmentIds(shipmentIds);
        return listShipmentsImpl.listShipments(listShipmentsRqst, txnContext, entityManager);
    }
    
    private void createShipmentNotFoundError(ShipmentAppointmentDTO shipmentAppointmentDataHolder) throws NotFoundException {
        
        String errMsg = StringUtils.SPACE;
        String errorKey = StringUtils.SPACE;
        if (StringUtils.isNotBlank(shipmentAppointmentDataHolder.getProNbr())) {
            errorKey = shipmentAppointmentDataHolder.getProNbr();
            errMsg = " for PRO number ";
        } else {
            errorKey = BasicTransformer.toString(shipmentAppointmentDataHolder.getShpInstId());
            errMsg = " for shipment instance ID ";
        }
        throw ExceptionBuilder.exception(NotFoundErrorMessage.SHIPMENT_NF, shipmentAppointmentDataHolder.getTxnContext()).contextValues(errMsg, errorKey).build();
    }

    private void getConsignee(ShipmentAppointmentDTO shipmentAppointmentDataHolder, ShipmentDetails shipmentDetails) throws NotFoundException {

        AsMatchedParty consignee = null;
        if (CollectionUtils.isNotEmpty(shipmentDetails.getAsMatchedParty())) {
            consignee = AsMatchedPartyUtil.getConsigneeFromList(shipmentDetails.getAsMatchedParty());
            shipmentAppointmentDataHolder.setConsignee(consignee);
        }
        if (null == consignee) {
            throw ExceptionBuilder.exception(NotFoundErrorMessage.AS_ENTD_CUST_NF, shipmentAppointmentDataHolder.getTxnContext()).build();
        }
    }
    
    private void getTdc(ShipmentAppointmentDTO shipmentAppointmentDataHolder, ShipmentDetails shipmentDetail, EntityManager entityManager) throws ServiceException {
        
        List<ShipmentDetails> parentShipmentDetails = null;
        if (shipmentDetail.getParentShipmentId() != null && StringUtils.isNotBlank(shipmentDetail.getParentShipmentId().getShipmentInstId())) {
            List<ShipmentId> shipmentIds = new ArrayList<>();
            ShipmentId shipmentId = new ShipmentId();
            shipmentId.setShipmentInstId(shipmentDetail.getParentShipmentId().getShipmentInstId());
            shipmentIds.add(shipmentId);
            ListShipmentsResp listShipmentsResp = listShipmentsForShipment(shipmentIds, shipmentAppointmentDataHolder.getTxnContext(), entityManager);
            if (null != listShipmentsResp && CollectionUtils.isNotEmpty(listShipmentsResp.getShipmentDetails())) {
                parentShipmentDetails = listShipmentsResp.getShipmentDetails();
            }
        }
        TimeDateCritical tdcDetails = getTdcDetails(shipmentDetail, parentShipmentDetails);
        if (null != tdcDetails) {
            shipmentAppointmentDataHolder.setTdc(tdcDetails);
        }        
    }
    
    private void getNoaDetails(ShipmentAppointmentDTO shipmentAppointmentDataHolder) {
        
        String custInstIdStr = BasicTransformer.toString(AsMatchedPartyUtil.getValidCustomerInstId(shipmentAppointmentDataHolder.getConsignee()).intValue());
        if (StringUtils.isNotBlank(custInstIdStr)) {
            if (null == shipmentAppointmentDataHolder.getAppointmentDetails()) {
                // If no appointment has been made yet, return the customer NOA contact party for that consignee, if it exists
                List<CustomerContact> contacts = externalRestClient.listCustomerContacts(custInstIdStr, shipmentAppointmentDataHolder.getTxnContext());
                CustomerContact noaContact = CustomerUtil.getCustomerNoaContact(contacts);
                shipmentAppointmentDataHolder.setNoaContact(noaContact);
            }            
            // Get the NOA notes
            List<CustomerOperationsNote> customerOperationsNotes = externalRestClient.listCustomerOperationsNotes(BasicTransformer.toLong(custInstIdStr), shipmentAppointmentDataHolder.getTxnContext());
            if (CollectionUtils.isNotEmpty(customerOperationsNotes)) {
                shipmentAppointmentDataHolder.setNoaNotes(CustomerUtil.getNoaContactNotes(customerOperationsNotes));
            }
        }
    }
    
    /**
     * Get Shipment most recent Notification appointment; this could either be an appointment or an NOA notification.  
     * Only returns the notification when present that is in either Set or Pending status
     */
    private void getAppointmentDetails(ShipmentAppointmentDTO shipmentAppointmentDataHolder, EntityManager entityManager) throws ServiceException {
     
        com.xpo.ltl.api.shipment.service.entity.ShmNotification shmNotification = shmNotificationSubDAO.getMostRecentNotification(shipmentAppointmentDataHolder.getShpInstId(), entityManager);
        ShmNotification appointmentDetails = null;
        if (null != shmNotification) {
            appointmentDetails = EntityTransformer.toShmNotification(shmNotification);
        } 
        shipmentAppointmentDataHolder.setAppointmentDetails(appointmentDetails);
    }
    
    private void getOperationsNotification(ShipmentAppointmentDTO shipmentAppointmentDataHolder) throws ServiceException {

        if (null != shipmentAppointmentDataHolder.getAppointmentDetails()) {
            if (NumberUtil.isNonZero(shipmentAppointmentDataHolder.getAppointmentDetails().getScoNtficnInstId())) {
                GetDeliveryNotificationResp response = 
                        externalRestClient.getDeliveryNotification(shipmentAppointmentDataHolder.getAppointmentDetails().getScoNtficnInstId(), shipmentAppointmentDataHolder.getTxnContext());
                if (null != response) {
                    if (null != response.getDeliveryNotification()) {
                        shipmentAppointmentDataHolder.setDeliveryNotification(response.getDeliveryNotification());
                        formatNoaContact(shipmentAppointmentDataHolder);
                        getCurrentAppointmentNotes(response.getDeliveryNotification(), shipmentAppointmentDataHolder);
                    }
                    if (CollectionUtils.isNotEmpty(response.getNotificationLogs())) {
                        shipmentAppointmentDataHolder.setChangeLog(getNotificationLogs(response.getNotificationLogs(), shipmentAppointmentDataHolder));
                    }
                }
            }
        }        
    }
    
    /**
     * Returns the NOA contact from the appointment notification independently of any NOA contacts lists that may exist for the consignee in the Customer system.
     */
    private void formatNoaContact(ShipmentAppointmentDTO shipmentAppointmentDataHolder) {
        
        if (null != shipmentAppointmentDataHolder.getDeliveryNotification()) {
            CustomerContact noaContact = new CustomerContact();
            ContactPerson contactPerson = new ContactPerson();

            if (StringUtils.isNotBlank(shipmentAppointmentDataHolder.getDeliveryNotification().getContactPhoneNbr())) {
                ContactPerson contactPhoneNumber = ShmNotificationUtil.getContactPhoneNumberFromNotificationPhone(shipmentAppointmentDataHolder.getDeliveryNotification().getContactPhoneNbr());
                if (null != contactPhoneNumber) {
                    contactPerson.setBusinessPhoneAreaCd(contactPhoneNumber.getBusinessPhoneAreaCd());
                    contactPerson.setBusinessPhoneNbr(contactPhoneNumber.getBusinessPhoneNbr());
                }
            }
            contactPerson.setBusinessPhoneExt(shipmentAppointmentDataHolder.getDeliveryNotification().getContactPhoneExtensionNbr());

            ContactPerson nameFromAppointment = ShmNotificationUtil.getContactPersonFromNotificationName(shipmentAppointmentDataHolder.getDeliveryNotification().getContactName());
            if (null != nameFromAppointment) {
                contactPerson.setFirstName(nameFromAppointment.getFirstName());
                contactPerson.setLastName(nameFromAppointment.getLastName());
            }
            noaContact.setContactPerson(contactPerson);
            shipmentAppointmentDataHolder.setNoaContact(noaContact);
        }        
    }
    
    /**
     * Returns the current appointment and FBDS notes from the Operations delivery notification if they are not present on the Shipment Notification.  The
     * legacy SCO system does not support FBDS or appointment notes on SHM_NOTIFICATION, so if the appointment is created/updated in SCO, return
     * the notes from the Operations delivery notification when not present in SHM_NOTIFICATION.
     */
    private void getCurrentAppointmentNotes(DeliveryNotification deliveryNotification, ShipmentAppointmentDTO shipmentAppointmentDataHolder) {
     
        if (null == shipmentAppointmentDataHolder.getAppointmentDetails().getFbdsNote() && null != deliveryNotification.getFbdsNote()) {
            shipmentAppointmentDataHolder.getAppointmentDetails().setFbdsNote(deliveryNotification.getFbdsNote());
        }
        if (null == shipmentAppointmentDataHolder.getAppointmentDetails().getAppointmentNote() && null != deliveryNotification.getInternalNote()) {
            shipmentAppointmentDataHolder.getAppointmentDetails().setAppointmentNote(deliveryNotification.getInternalNote());
        }
    }
    
    private List<com.xpo.ltl.api.shipment.v2.NotificationLog> getNotificationLogs(List<NotificationLog> notificationLogsResponse, ShipmentAppointmentDTO shipmentAppointmentDataHolder) throws ServiceException {

        List<com.xpo.ltl.api.shipment.v2.NotificationLog> notificationLogs =
            notificationLogsResponse.stream()
                .map(
                    log -> DozerMapper.getInstance().map(log, com.xpo.ltl.api.shipment.v2.NotificationLog.class))
                .collect(Collectors.toList());
        getEventDescFromCode(notificationLogs);
        getUserNameFromId(notificationLogs, shipmentAppointmentDataHolder);
        
        return notificationLogs;
    }
    
    private void getEventDescFromCode(List<com.xpo.ltl.api.shipment.v2.NotificationLog> notificationLogs) {
        
        if (CollectionUtils.isNotEmpty(notificationLogs)) {
            for (com.xpo.ltl.api.shipment.v2.NotificationLog notificationLog : notificationLogs) {
                try {
                    NotificationEventCdEnum notificationEventCode = NotificationEventCdEnum.fromValue(notificationLog.getEventCd());
                    notificationLog.setEventCd(notificationEventCode.getDesc());
                } catch (IllegalArgumentException e) {
                }
            }
        }
    }
    
    private void getUserNameFromId(List<com.xpo.ltl.api.shipment.v2.NotificationLog> notificationLogs, ShipmentAppointmentDTO shipmentAppointmentDataHolder) throws ServiceException {
        
        TransactionContext txnContext = shipmentAppointmentDataHolder.getTxnContext();
        Map<String, InterfaceEmployee> employeesByCallerId = new HashMap<>();
        
        // The callerId in the logs could be either an employee id or a RACF id of that user, we can't tell, so we're going to assume first that the callerId is an employee id,
        // and if not, search again by RACF id to find them.
        
        // Get all the ids from the logs; these could be either employee ids or RACF ids, we assume first they are employee ids
        List<String> employeeIds = notificationLogs.stream()
            .map(com.xpo.ltl.api.shipment.v2.NotificationLog::getCallerId)
            .collect(Collectors.toList());
        Map<String, InterfaceEmployee> employeesByIdMap = externalRestClient.getEmployeeDetailsMap(employeeIds, txnContext);
        if (null == employeesByIdMap) {
            employeesByIdMap = new HashMap<>();
        }
        List<String> racfIds = new ArrayList<>();
        
        // Look through the logs into the map and see if you found the employee by employee id; if not, it must be a RACF id, so toss into the RACF id list and we'll search again.
        for (com.xpo.ltl.api.shipment.v2.NotificationLog notificationLog : notificationLogs) {
            if (!employeesByIdMap.containsKey(notificationLog.getCallerId())) {
                racfIds.add(notificationLog.getCallerId());
            }
        }
        Map<String, InterfaceEmployee> employeesByRacfIdMap = new HashMap<>();
        
        if (CollectionUtils.isNotEmpty(racfIds)) {
            employeesByRacfIdMap= externalRestClient.listEmployeesByRacfId(racfIds, txnContext);
        }
        // At this stage we're assuming we got all the InterfaceEmployee objects we need, so combine them in one collection and finally format the name for them on the response.
        if (MapUtils.isNotEmpty(employeesByIdMap)) {
            employeesByCallerId.putAll(employeesByIdMap);
        }
        if (MapUtils.isNotEmpty(employeesByRacfIdMap)) {
            employeesByCallerId.putAll(employeesByRacfIdMap);
        }
        
        for (com.xpo.ltl.api.shipment.v2.NotificationLog notificationLog : notificationLogs) {
            InterfaceEmployee employee = employeesByCallerId.get(notificationLog.getCallerId());
            if (null != employee) {
                notificationLog.setCallerId(employee.getFirstName() + " " + employee.getLastName());
            }
        }
    }
    
    private void handleAppointmentIndicatorsCases(ShipmentAppointmentDTO shipmentAppointmentDataHolder) {
        
        if (null != shipmentAppointmentDataHolder.getAppointmentDetails() 
                && StringUtils.equals(shipmentAppointmentDataHolder.getAppointmentDetails().getCategoryCd(), ShmNotificationCatgCdEnum.APPOINTMENT.getCode())) {
            ShipmentAppointmentStatusEnum appointmentStatus = ShipmentAppointmentStatusEnum.fromValue(shipmentAppointmentDataHolder.getAppointmentDetails().getStatusCd());
            if (appointmentStatus == ShipmentAppointmentStatusEnum.PENDING) {
                shipmentAppointmentDataHolder.setPendingAppointmentInd(Boolean.TRUE);
                shipmentAppointmentDataHolder.setAllDayInd(Boolean.FALSE);
            } else {
                shipmentAppointmentDataHolder.setPendingAppointmentInd(Boolean.FALSE);
                ShmNotification appointmentDetails = shipmentAppointmentDataHolder.getAppointmentDetails();
                // If the to/from times are zero, then the appointment window applies to the entire day.
                shipmentAppointmentDataHolder.setAllDayInd(ShmNotificationUtil.doesAppointmentApplyEntireDay(appointmentDetails));
            }
        } else {
            shipmentAppointmentDataHolder.setAllDayInd(Boolean.FALSE);
            shipmentAppointmentDataHolder.setPendingAppointmentInd(Boolean.FALSE);
        }
        // If the storage indicator is on, then the user expect storage fees to apply to the shipment's appointment.
        if (null != shipmentAppointmentDataHolder.getDeliveryNotification() && BooleanUtils.isTrue(shipmentAppointmentDataHolder.getDeliveryNotification().getStorageInd())) {
            shipmentAppointmentDataHolder.setStorageInd(Boolean.TRUE);
        } else {
            shipmentAppointmentDataHolder.setStorageInd(Boolean.FALSE);
        }
    }
    
    /**
     * Gets all shipments that are on the same appointment.
     */
    private void getShipmentsWithAppointments(ShipmentAppointmentDTO shipmentAppointmentDataHolder, EntityManager entityManager) throws ServiceException {
        
        if (null != shipmentAppointmentDataHolder.getAppointmentDetails() && NumberUtil.isNonZero(shipmentAppointmentDataHolder.getAppointmentDetails().getScoNtficnInstId())) {
            // Get all shipments that have the same delivery appointment notification. 
            List<ShipmentDetails> shipmentDetails = getShipmentsForSameDeliveryNotification(shipmentAppointmentDataHolder, entityManager);
            List<ShipmentDetails> parentShipmentDetails = getParentShipmentsShipmentList(shipmentDetails, entityManager);
        
            if (CollectionUtils.isNotEmpty(shipmentDetails)) {
                List<ShipmentAppointment> shipmentsWithExistingAppointments = new ArrayList<>();
                for (ShipmentDetails shipmentDetail : shipmentDetails) {
                    Shipment shipment = shipmentDetail.getShipment();
                    ShipmentAppointment shipmentAppointment = new ShipmentAppointment();
                    
                    getCodAndTotalCharges(shipmentDetail, shipmentAppointment);

                    shipmentAppointment.setEstimatedDeliveryDate(shipment.getEstimatedDeliveryDate());
                    shipmentAppointment.setPiecesCount(shipment.getTotalPiecesCount());
                    shipmentAppointment.setProNbr(shipment.getProNbr());
                    shipmentAppointment.setShipmentInstId(shipment.getShipmentInstId());
                    shipmentAppointment.setTotalWeightLbs(shipment.getTotalWeightLbs());
                    TimeDateCritical tdcDetails = getTdcDetails(shipmentDetail, parentShipmentDetails);
                    if (null != tdcDetails) {
                        shipmentAppointment.setTimeDateCritical(tdcDetails);
                    }
                    shipmentsWithExistingAppointments.add(shipmentAppointment);
                }                    
                shipmentAppointmentDataHolder.setShipmentsWithExistingAppointments(shipmentsWithExistingAppointments);
            }
        }
    }
    
    private List<ShipmentDetails> getShipmentsForSameDeliveryNotification(ShipmentAppointmentDTO shipmentAppointmentDataHolder, EntityManager entityManager) throws ServiceException {
        
        Long notificationInstId = shipmentAppointmentDataHolder.getAppointmentDetails().getScoNtficnInstId();            
        List<com.xpo.ltl.api.shipment.service.entity.ShmNotification> shmNotifications = shmNotificationSubDAO.listNotificationsForNotificationId(notificationInstId, entityManager);            
        
        List<ShipmentDetails> results = null;
        
        if (CollectionUtils.isNotEmpty(shmNotifications)) {                
            // Get the unique shipment ids
            Set<Long> shpInstIds = shmNotifications.stream()
                .map(com.xpo.ltl.api.shipment.service.entity.ShmNotification::getId)
                .map(com.xpo.ltl.api.shipment.service.entity.ShmNotificationPK::getShpInstId)
                .collect(Collectors.toSet());

            results = listShpDetailsWithTdcForAppointmentByShpId(shpInstIds, Boolean.FALSE, entityManager);
        }
        return results;
    }
    
    private List<ShipmentDetails> getParentShipmentsShipmentList(List<ShipmentDetails> shipmentDetails, EntityManager entityManager) 
            throws ServiceException {
        
        List<ShipmentDetails> results = null;

        if (CollectionUtils.isNotEmpty(shipmentDetails)) {
            Set<Long> parentShpInstIds = shipmentDetails.stream()
                .filter(shipmentDetail -> shipmentDetail.getParentShipmentId() != null && StringUtils.isNotBlank(shipmentDetail.getParentShipmentId().getShipmentInstId()))
                .map(ShipmentDetails::getParentShipmentId)
                .map(parentId -> BasicTransformer.toLong(parentId.getShipmentInstId()))
                .collect(Collectors.toSet());
            results = listShpDetailsWithTdcForAppointmentByShpId(parentShpInstIds, Boolean.TRUE, entityManager);
        }
        return results;
    } 
    
    private List<ShipmentDetails> listShpDetailsWithTdcForAppointmentByShpId(Set<Long> shpInstIds, Boolean isParent, EntityManager entityManager) {
        
        List<Tuple> results = shmShipmentSubDAO.listShpDetailsWithTdcForAppointmentByShpId(shpInstIds, isParent, entityManager);
        
        return transformTuplesIntoShipmentDetails(results, isParent);
    }
    
    private List<ShipmentDetails> transformTuplesIntoShipmentDetails(Collection<Tuple> results, Boolean isParent) {
        
        Map<Long, ShipmentDetails> shipmentDetailsByShpId = new HashMap<>();
        
        for (Tuple oneResult : results) {
            Long shpInstId = (Long)oneResult.get(SHP_INST_ID);
            ShipmentDetails shipmentDetails = shipmentDetailsByShpId.get(shpInstId);
            if (null == shipmentDetails) {
                shipmentDetails = new ShipmentDetails();
            }
            if (null == shipmentDetails.getShipment()) {
                processShipmentHeaderResult(shipmentDetails, shpInstId, oneResult);
            }
            if (null == shipmentDetails.getTimeDateCritical()) {
                processTdcResult(shipmentDetails, shpInstId, oneResult);
            }
            if (!isParent) {  // Only collect the miscellaneous line items that are not the parent shipments.
                processMiscLineItemResult(shipmentDetails, shpInstId, oneResult);
            }
            shipmentDetailsByShpId.put(shpInstId, shipmentDetails);
        }
        List<ShipmentDetails> returnList = new ArrayList<>();
        if (MapUtils.isNotEmpty(shipmentDetailsByShpId)) {
            returnList = new ArrayList<>(shipmentDetailsByShpId.values());
        }
        return returnList;
    }
    
    private void processShipmentHeaderResult(ShipmentDetails shipmentDetails, Long shpInstId, Tuple oneResult) {
        
        Shipment shipment = new Shipment();
        shipment.setShipmentInstId(shpInstId);
        if (null != oneResult.get(PRO_NBR)) {
            shipment.setProNbr((String)oneResult.get(PRO_NBR));
        }
        if (null != oneResult.get(EST_DL_DT)) {
            shipment.setEstimatedDeliveryDate(new SimpleDateFormat(DateUtils.DATE_FORMAT_SHORT).format((Date)oneResult.get(EST_DL_DT)));
        }
        if (null != oneResult.get(TOT_PCS)) {
            shipment.setTotalPiecesCount(BasicTransformer.toBigInteger((BigDecimal)oneResult.get(TOT_PCS)));
        }
        if (null != oneResult.get(TOT_WGT)) {
            shipment.setTotalWeightLbs(BasicTransformer.toDouble((BigDecimal)oneResult.get(TOT_WGT)));
        }
        if (null != oneResult.get(TOT_CHG)) {
            shipment.setTotalChargeAmount(BasicTransformer.toDouble((BigDecimal)oneResult.get(TOT_CHG)));
        }
        if (null != oneResult.get(BILL_CLS)) {
            try {
                BillClassCd billClassCd = BillClassCdTransformer.toEnum((String)oneResult.get(BILL_CLS));
                shipment.setBillClassCd(billClassCd);
            } catch (IllegalArgumentException e) {
                shipment.setBillClassCd(null);
            }
        }
        if (null != oneResult.get(CHRG_TO_CD)) {
            try {
                ChargeToCd chargeToCd = ChargeToCdTransformer.toEnum((String)oneResult.get(CHRG_TO_CD));
                shipment.setChargeToCd(chargeToCd);
            } catch (IllegalArgumentException e) {
                shipment.setChargeToCd(null);
            }
        }        
        if (null != oneResult.get(PARENT_SHP_ID)) {
            ShipmentId parentShpId = new ShipmentId();
            parentShpId.setShipmentInstId(((BigDecimal)oneResult.get(PARENT_SHP_ID)).toString());
            shipmentDetails.setParentShipmentId(parentShpId);
        }  
        shipmentDetails.setShipment(shipment);
    }
    
    private void processTdcResult(ShipmentDetails shipmentDetails, Long shpInstId, Tuple oneResult) {
        
        TimeDateCritical tdc = null;
        if (null != oneResult.get(TDC_DT_TYP_CD)) {
            tdc = new TimeDateCritical();
            tdc.setTdcDateTypeCd((String)oneResult.get(TDC_DT_TYP_CD));
        }
        if (null != oneResult.get(TDC_DT_1)) {
            if (null == tdc) {
                tdc = new TimeDateCritical();
            }
            tdc.setTdcDate1(new SimpleDateFormat(DateUtils.DATE_FORMAT_SHORT).format((Date)oneResult.get(TDC_DT_1)));
        }
        if (null != oneResult.get(TDC_DT_2)) {
            if (null == tdc) {
                tdc = new TimeDateCritical();
            }
            tdc.setTdcDate2(new SimpleDateFormat(DateUtils.DATE_FORMAT_SHORT).format((Date)oneResult.get(TDC_DT_2)));
        }
        if (null != oneResult.get(TDC_TM_TYP_CD)) {
            if (null == tdc) {
                tdc = new TimeDateCritical();
            }
            tdc.setTdcTimeTypeCd((String)oneResult.get(TDC_TM_TYP_CD));
        }
        if (null != oneResult.get(TDC_TM_1)) {
            if (null == tdc) {
                tdc = new TimeDateCritical();
            }
            tdc.setTdcTime1(new SimpleDateFormat(TIME_FORMAT).format((Date)oneResult.get(TDC_TM_1)));
        }
        if (null != oneResult.get(TDC_TM_2)) {
            if (null == tdc) {
                tdc = new TimeDateCritical();
            }
            tdc.setTdcTime2(new SimpleDateFormat(TIME_FORMAT).format((Date)oneResult.get(TDC_TM_2)));
        }
        if (null != tdc) {
            tdc.setShipmentInstId(shpInstId);
        }
        shipmentDetails.setTimeDateCritical(tdc);
    }
    
    private void processMiscLineItemResult(ShipmentDetails shipmentDetails, Long shpInstId, Tuple oneResult) {
        
        if (null == shipmentDetails.getMiscLineItem()) {
            shipmentDetails.setMiscLineItem(new ArrayList<>());
        }
        
        MiscLineItem misc = null;
        if (null != oneResult.get(LN_TYP_CD)) {
            misc = new MiscLineItem();
            try {
                MiscLineItemCd miscLineItemCd = MiscLineItemCdTransformer.toEnum((String)oneResult.get(LN_TYP_CD));
                misc.setLineTypeCd(miscLineItemCd);
            } catch (IllegalArgumentException e) {
                misc.setLineTypeCd(null);
            }
        }
        if (null != oneResult.get(LN_AMT)) {
            if (null == misc) {
                misc = new MiscLineItem();
            }
            misc.setAmount(BasicTransformer.toDouble((BigDecimal)oneResult.get(LN_AMT)));
        }
        if (null != misc) {
            misc.setShipmentInstId(shpInstId);
            shipmentDetails.getMiscLineItem().add(misc);
        }
    }

    private void getCodAndTotalCharges(ShipmentDetails shipmentDetail, ShipmentAppointment shipmentAppointment) {
        
        if (null != shipmentDetail.getShipment()) {
            Shipment shipment = shipmentDetail.getShipment();
            Double codAmount = NumberUtils.DOUBLE_ZERO;
            Double totalFreightCharges = shipment.getTotalChargeAmount();
            if (CollectionUtils.isNotEmpty(shipmentDetail.getMiscLineItem())) {
                // Get the COD (cash-on-delivery) amount expected; this is the amount from billing on the shipment, not the amount actually collected at time of delivery.
                MiscLineItem codLineItem = MiscLineItemUtil.getLineItemByType(MiscLineItemCd.COD_AMT, shipmentDetail.getMiscLineItem());
                if (null != codLineItem) {
                    shipmentAppointment.setCodAmount(codLineItem.getAmount());
                    codAmount = codLineItem.getAmount();
                }
                if (null != shipment.getChargeToCd() && shipment.getChargeToCd() == ChargeToCd.BOTH) {
                    MiscLineItem partCollectLineItem = MiscLineItemUtil.getLineItemByType(MiscLineItemCd.PART_COLL_LN, shipmentDetail.getMiscLineItem());
                    if (null != partCollectLineItem) {
                        if (partCollectLineItem.getAmount().doubleValue() > NumberUtils.DOUBLE_ZERO) {
                            totalFreightCharges = partCollectLineItem.getAmount();
                        }
                    }
                }
            }
            // Total charges are the total freight charges on the shipment minus COD amount.  If the shipment is a Both bill, the total freight charges
            // are the part collect portion of the total freight charges minus the COD amount.
            if (totalFreightCharges.doubleValue() > codAmount.doubleValue()) {
                DecimalFormat df = new DecimalFormat("###########.00");
                double frtCharges = totalFreightCharges.doubleValue() - codAmount.doubleValue();                
                shipmentAppointment.setFreightChargesAmount(new Double(df.format(frtCharges)));
            } else {
                shipmentAppointment.setFreightChargesAmount(totalFreightCharges.doubleValue());
            }
        }
    }
    
    /**
     * Return the correct time-date-critical information for the shipment.  If the shipment is of the type PSEG, MSEG or MOVR, return
     * the TDC information for the parent, if present, otherwise return the TDC information for the child pro, if present, which will
     * override the parent TDC information when present.
     */
    private TimeDateCritical getTdcDetails(ShipmentDetails shipmentDetail, List<ShipmentDetails> parentShipmentDetails) {

        TimeDateCritical returnTdc = null;
        TimeDateCritical shipmentTdc = null;
        TimeDateCritical parentTdc = null;
        
        shipmentTdc = shipmentDetail.getTimeDateCritical();
        
        if (null != shipmentDetail.getShipment().getBillClassCd() && CHILD_BILL_CLS.contains(shipmentDetail.getShipment().getBillClassCd()) 
                && null != shipmentDetail.getParentShipmentId() 
                && StringUtils.isNotBlank(shipmentDetail.getParentShipmentId().getShipmentInstId())) {
            long parentShpInstId = BasicTransformer.toLong(shipmentDetail.getParentShipmentId().getShipmentInstId());
            ShipmentDetails parentShipmentDetail = parentShipmentDetails.stream()
                .filter(shipment -> shipment.getShipment().getShipmentInstId().longValue() == parentShpInstId)
                .findFirst()
                .orElse(null);
            if (null != parentShipmentDetail) {
                parentTdc = parentShipmentDetail.getTimeDateCritical();
            }
        }
        if (null == shipmentTdc && null != parentTdc) {
            returnTdc = parentTdc;
        } else {
            returnTdc = shipmentTdc;
        }
        return returnTdc;
    }

    /**
     * Gets all shipments that do not have an appointment for the same consignee of the shipment on this appointment; these are called outstanding shipments.
     */
    private void getShipmentsWithoutAppointmentsThisConsignee(ShipmentAppointmentDTO shipmentAppointmentDataHolder, EntityManager entityManager) {

        Long custInstId = BasicTransformer.toLong(AsMatchedPartyUtil.getValidCustomerInstId(shipmentAppointmentDataHolder.getConsignee()).intValue());

        // Calculate the estimated delivery date search range
        Calendar estDelCalStart = BasicTransformer.toCalendar(BasicTransformer.toDate(shipmentAppointmentDataHolder.getShipment().getEstimatedDeliveryDate()));
        estDelCalStart.set(Calendar.MONTH, estDelCalStart.get(Calendar.MONTH) - 3);
        Date estDeliveryDtStart = estDelCalStart.getTime();
        
        Calendar estDelCalEnd = BasicTransformer.toCalendar(BasicTransformer.toDate(shipmentAppointmentDataHolder.getShipment().getEstimatedDeliveryDate()));
        estDelCalEnd.set(Calendar.MONTH, estDelCalEnd.get(Calendar.MONTH) + 6);
        Date estDeliveryDtEnd = estDelCalEnd.getTime();

        // Calculate the pickup date range
        Calendar pickupDtStartCal = BasicTransformer.toCalendar(new Date());
        pickupDtStartCal.set(Calendar.MONTH, pickupDtStartCal.get(Calendar.MONTH) - 3);
        Date pickupDtStart = pickupDtStartCal.getTime();

        Calendar pickupDtEndCal = BasicTransformer.toCalendar(BasicTransformer.toDate(PICKUP_DT_HIGH));
        Date pickupDtEnd = pickupDtEndCal.getTime();

        List<Shipment> shipments = shmShipmentSubDAO.listShipmentsNeedingAppointmentForConsignee(
            custInstId, 
            shipmentAppointmentDataHolder.getShipment().getDestinationTerminalSicCd(),
            pickupDtStart,
            pickupDtEnd,
            estDeliveryDtStart,
            estDeliveryDtEnd,
            entityManager);        
        shipmentAppointmentDataHolder.setOutstandingShipments(shipments);
    }
    private GetShipmentAppointmentResp buildResponse(ShipmentAppointmentDTO shipmentAppointmentDataHolder) {
        
        GetShipmentAppointmentResp getShipmentAppointmentResp = new GetShipmentAppointmentResp();
        getShipmentAppointmentResp.setShipment(shipmentAppointmentDataHolder.getShipment());
        getShipmentAppointmentResp.setConsignee(shipmentAppointmentDataHolder.getConsignee());
        getShipmentAppointmentResp.setTdc(shipmentAppointmentDataHolder.getTdc());
        if (null != shipmentAppointmentDataHolder.getNoaContact()) {
            getShipmentAppointmentResp.setNoaContact(DozerMapper.getInstance().map(shipmentAppointmentDataHolder.getNoaContact(), com.xpo.ltl.api.shipment.v2.CustomerContact.class));
        }
        if (CollectionUtils.isNotEmpty(shipmentAppointmentDataHolder.getNoaNotes())) {
            getShipmentAppointmentResp.setNoaNotes(
                shipmentAppointmentDataHolder.getNoaNotes().stream()
                    .map(
                        note -> DozerMapper.getInstance().map(note, com.xpo.ltl.api.shipment.v2.CustomerOperationsNote.class))
                    .collect(Collectors.toList()));
        }
        getShipmentAppointmentResp.setAppointment(shipmentAppointmentDataHolder.getAppointmentDetails());
        getShipmentAppointmentResp.setAllDayInd(shipmentAppointmentDataHolder.getAllDayInd());
        getShipmentAppointmentResp.setPendingAppointmentInd(shipmentAppointmentDataHolder.getPendingAppointmentInd());
        getShipmentAppointmentResp.setStorageInd(shipmentAppointmentDataHolder.getStorageInd());
        getShipmentAppointmentResp.setChangeLogs(shipmentAppointmentDataHolder.getChangeLog());
        getShipmentAppointmentResp.setShipmentsWithExistingAppointments(shipmentAppointmentDataHolder.getShipmentsWithExistingAppointments());
        getShipmentAppointmentResp.setOutstandingShipments(shipmentAppointmentDataHolder.getOutstandingShipments());
        getShipmentAppointmentResp.setRouteInstIdForShipment(shipmentAppointmentDataHolder.getRouteInstId());
        
        return getShipmentAppointmentResp;
    }
    
    private void getRouteInstId(ShipmentAppointmentDTO shipmentAppointmentDataHolder, TransactionContext txnContext) 
            throws ServiceException {
        
        SearchDeliveryShipmentsRqst rqst = new SearchDeliveryShipmentsRqst();
        DeliveryShipmentSearchFilter filter = new DeliveryShipmentSearchFilter();
        List<String> shpInstIds = new ArrayList<>();
        shpInstIds.add(BasicTransformer.toString(shipmentAppointmentDataHolder.getShpInstId()));
        final XrtAttributeFilter xrtAttributeFilter = new XrtAttributeFilter();
        xrtAttributeFilter.setValues(shpInstIds);
        filter.setShipmentInstId(xrtAttributeFilter);
        rqst.setFilter(filter);
        rqst.setFields(FIELDS);
        rqst.setPageSize(BigInteger.valueOf(1000)); 
        
        SearchDeliveryShipmentsResp response = externalRestClient.searchDeliveryShipment(rqst, txnContext);
        
        if (response != null && CollectionUtils.isNotEmpty(response.getResult())) {
            DeliveryShipmentSearchRecord record = response.getResult().get(0);
            if (NumberUtil.isNonZero(record.getRouteInstId())) {
                shipmentAppointmentDataHolder.setRouteInstId(record.getRouteInstId());
            }
        }

    }
}
