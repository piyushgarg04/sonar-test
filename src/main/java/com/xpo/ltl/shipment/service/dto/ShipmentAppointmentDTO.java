package com.xpo.ltl.shipment.service.dto;

import java.util.List;

import com.xpo.ltl.api.cityoperations.v1.CustomerOperationsNote;
import com.xpo.ltl.api.cityoperations.v1.DeliveryNotification;
import com.xpo.ltl.api.shipment.v2.AsMatchedParty;
import com.xpo.ltl.api.shipment.v2.NotificationLog;
import com.xpo.ltl.api.shipment.v2.Shipment;
import com.xpo.ltl.api.shipment.v2.ShipmentAppointment;
import com.xpo.ltl.api.shipment.v2.ShmNotification;
import com.xpo.ltl.api.shipment.v2.TimeDateCritical;
import com.xpo.ltl.api.customer.v2.CustomerContact;
import com.xpo.ltl.api.rest.TransactionContext;

public class ShipmentAppointmentDTO {

    private Long shpInstId;
    private String proNbr;
    private TransactionContext txnContext;
    private Shipment shipment;
    private AsMatchedParty consignee;
    private TimeDateCritical tdc;
    private CustomerContact noaContact;
    private List<CustomerOperationsNote> noaNotes;
    private ShmNotification appointmentDetails;
    private Boolean allDayInd;
    private Boolean pendingAppointmentInd;
    private Boolean storageInd;
    private DeliveryNotification deliveryNotification;
    private List<NotificationLog> changeLog;
    private List<ShipmentAppointment> shipmentsWithExistingAppointments;
    private List<Shipment> outstandingShipments;
    private Long routeInstId;
    
    public Long getShpInstId() {
        return shpInstId;
    }
    
    public void setShpInstId(Long shpInstId) {
        this.shpInstId = shpInstId;
    }
    
    public String getProNbr() {
        return proNbr;
    }
    
    public void setProNbr(String proNbr) {
        this.proNbr = proNbr;
    }
    
    public Shipment getShipment() {
        return shipment;
    }
    
    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }
    
    public AsMatchedParty getConsignee() {
        return consignee;
    }
    
    public void setConsignee(AsMatchedParty consignee) {
        this.consignee = consignee;
    }
    
    public CustomerContact getNoaContact() {
        return noaContact;
    }
    
    public void setNoaContact(CustomerContact noaContact) {
        this.noaContact = noaContact;
    }

    public ShmNotification getAppointmentDetails() {
        return appointmentDetails;
    }
    
    public void setAppointmentDetails(ShmNotification appointmentDetails) {
        this.appointmentDetails = appointmentDetails;
    }
    
    public Boolean getAllDayInd() {
        return allDayInd;
    }
    
    public void setAllDayInd(Boolean allDayInd) {
        this.allDayInd = allDayInd;
    }
    
    public Boolean getPendingAppointmentInd() {
        return pendingAppointmentInd;
    }
    
    public void setPendingAppointmentInd(Boolean pendingAppointmentInd) {
        this.pendingAppointmentInd = pendingAppointmentInd;
    }
    
    public Boolean getStorageInd() {
        return storageInd;
    }
    
    public void setStorageInd(Boolean storageInd) {
        this.storageInd = storageInd;
    }
    
    public List<NotificationLog> getChangeLog() {
        return changeLog;
    }
    
    public void setChangeLog(List<NotificationLog> changeLog) {
        this.changeLog = changeLog;
    }
    
    public List<ShipmentAppointment> getShipmentsWithExistingAppointments() {
        return shipmentsWithExistingAppointments;
    }
    
    public void setShipmentsWithExistingAppointments(List<ShipmentAppointment> shipmentsWithExistingAppointments) {
        this.shipmentsWithExistingAppointments = shipmentsWithExistingAppointments;
    }
    
    public List<Shipment> getOutstandingShipments() {
        return outstandingShipments;
    }
    
    public void setOutstandingShipments(List<Shipment> outstandingShipments) {
        this.outstandingShipments = outstandingShipments;
    }
    
    public TransactionContext getTxnContext() {
        return txnContext;
    }

    public void setTxnContext(TransactionContext txnContext) {
        this.txnContext = txnContext;
    }

    public DeliveryNotification getDeliveryNotification() {
        return deliveryNotification;
    }

    public void setDeliveryNotification(DeliveryNotification deliveryNotification) {
        this.deliveryNotification = deliveryNotification;
    }

    
    public List<CustomerOperationsNote> getNoaNotes() {
        return noaNotes;
    }

    
    public void setNoaNotes(List<CustomerOperationsNote> noaNotes) {
        this.noaNotes = noaNotes;
    }

    
    public Long getRouteInstId() {
        return routeInstId;
    }

    
    public void setRouteInstId(Long routeInstId) {
        this.routeInstId = routeInstId;
    }

    
    public TimeDateCritical getTdc() {
        return tdc;
    }

    
    public void setTdc(TimeDateCritical tdc) {
        this.tdc = tdc;
    }
}
