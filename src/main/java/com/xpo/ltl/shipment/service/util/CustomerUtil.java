package com.xpo.ltl.shipment.service.util;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import com.xpo.ltl.api.cityoperations.v1.CustOperationNoteTypeCd;
import com.xpo.ltl.api.cityoperations.v1.CustomerOperationsNote;
import com.xpo.ltl.api.customer.v2.CustomerContact;
import com.xpo.ltl.api.customer.v2.CustomerContactRoleCd;

public class CustomerUtil {

    private CustomerUtil() {}
    
    /**
     * Using a valid list of Customer Contacts, returns the NOA (notify-on-arrival) contact party for a shipment, if it exists.
     * @return CustomerContact
     */
    public static CustomerContact getCustomerNoaContact(List<CustomerContact> contacts) {
        
        if (CollectionUtils.isEmpty(contacts)) {
            return null;
        }
        return contacts.stream()
                .filter(contact -> contact.getContactRoleCd() == CustomerContactRoleCd.NOA)
                .findFirst()
                .orElse(null);
    }
    
    public static List<CustomerOperationsNote> getNoaContactNotes(List<CustomerOperationsNote> customerOperationsNotes) {
        
        if (CollectionUtils.isEmpty(customerOperationsNotes)) {
            return null;
        }
        return customerOperationsNotes.stream()
            .filter(note -> note.getNoteTypeCd() == CustOperationNoteTypeCd.NOTIFY_ON_ARRIVAL_NOTE)
            .collect(Collectors.toList());
    }
}
