package com.xpo.ltl.shipment.service.ejb.v1;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.v2.ShipmentOrderCaptureServiceIF;
import com.xpo.ltl.api.shipment.v2.CreateOcAuditControlResp;
import com.xpo.ltl.api.shipment.v2.CreateOcAuditControlRqst;
import com.xpo.ltl.api.shipment.v2.ListOcAuditControlsResp;
import com.xpo.ltl.api.shipment.v2.UpdateOcAuditControlResp;
import com.xpo.ltl.api.shipment.v2.UpdateOcAuditControlRqst;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@Stateless
@Local(ShipmentOrderCaptureServiceIF.class)
@TransactionManagement(TransactionManagementType.CONTAINER)
@LogExecutionTime
public class ShipmentOrderCaptureServiceBean implements ShipmentOrderCaptureServiceIF{

    @Override
    public CreateOcAuditControlResp createOcAuditControl(CreateOcAuditControlRqst createOcAuditControlRqst,
            TransactionContext txnContext) throws ServiceException, ValidationException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createOcAuditControl'");
    }

    @Override
    public UpdateOcAuditControlResp updateOcAuditControl(UpdateOcAuditControlRqst updateOcAuditControlRqst,
            TransactionContext txnContext) throws ServiceException, ValidationException, NotFoundException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateOcAuditControl'");
    }

    @Override
    public void deleteOcAuditControls(Long shipmentInstId, TransactionContext txnContext)
            throws ServiceException, ValidationException, NotFoundException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteOcAuditControls'");
    }

    @Override
    public ListOcAuditControlsResp listOcAuditControls(Long shipmentInstId, TransactionContext txnContext)
            throws ServiceException, ValidationException, NotFoundException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listOcAuditControls'");
    }
    
}
