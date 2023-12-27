package com.xpo.ltl.shipment.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.DmtShmShiplify;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.v2.AccessorialSync;
import com.xpo.ltl.api.shipment.v2.CreateShipmentRemarkResp;
import com.xpo.ltl.api.shipment.v2.CreateShipmentRemarkRqst;
import com.xpo.ltl.api.shipment.v2.ListUnprocessedShiplifyRecordsResp;
import com.xpo.ltl.api.shipment.v2.Remark;
import com.xpo.ltl.api.shipment.v2.ShipmentId;
import com.xpo.ltl.api.shipment.v2.ShipmentRemarkTypeCd;
import com.xpo.ltl.api.shipment.v2.ShmShiplify;
import com.xpo.ltl.api.shipment.v2.SynchronizeShiplifyAccessorialsResp;
import com.xpo.ltl.api.shipment.v2.SynchronizeShiplifyAccessorialsRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.DmtShmShiplifySubDAO;

import com.xpo.ltl.shipment.service.impl.MaintainShipmentRemarkImpl;

@RequestScoped
public class SynchronizeShiplifyAccessorialsImpl {

    @Inject
    private DmtShmShiplifySubDAO shmShiplifySubDAO;

    @Inject
    private MaintainShipmentRemarkImpl maintainShipmentRemarkImpl; 

    private static final Logger LOGGER = LogManager.getLogger(SynchronizeShiplifyAccessorialsImpl.class);

    public SynchronizeShiplifyAccessorialsResp synchronizeAccessorials(SynchronizeShiplifyAccessorialsRqst synchronizeShiplifyAccessorialsRqst,
            TransactionContext txnContext, EntityManager entityManager) throws ServiceException {
        List<ShmShiplify> unprocessedShipments = synchronizeShiplifyAccessorialsRqst.getShiplifyAccessorials();
        Set<Long> processedSet = new HashSet<Long>();
        List<AccessorialSync> successfulShipments = new ArrayList<AccessorialSync>();
        List<AccessorialSync> unsuccessfulShipments = new ArrayList<AccessorialSync>();
        if(unprocessedShipments != null) {
            for(ShmShiplify shipment: unprocessedShipments) {
                if(shipment != null && shipment.getShipmentInstId() != null) {
                    AccessorialSync syncRes = getSyncAccessorial(shipment.getShipmentInstId(), shipment.getProNbr());
                    if(!shipment.getConsigneeDockAccessInd()
                        && !shipment.getConsigneeForkliftInd()
                        && StringUtils.equalsIgnoreCase(shipment.getConsigneeDockAccessConfidence(), "A1")
                        && StringUtils.equalsIgnoreCase(shipment.getConsigneeForkliftConfidence(), "A1")) {
                            Remark rmk = new Remark();
                            rmk.setShipmentInstId(shipment.getShipmentInstId());
                            rmk.setTypeCd(ShipmentRemarkTypeCd.SHIPLIFY_RMK);
                            rmk.setRemark("Liftgate Required");
                            rmk.setArchiveControlCd(" ");
                            CreateShipmentRemarkRqst rqst = new CreateShipmentRemarkRqst();
                            rqst.setShipmentRemark(rmk);
                            try {
                                CreateShipmentRemarkResp resp = maintainShipmentRemarkImpl.createShipmentRemark(rqst, txnContext, entityManager);
                                processedSet.add(shipment.getShipmentInstId());
                                successfulShipments.add(syncRes);
                            }
                            catch (ServiceException e) {
                                LOGGER.info(String.format("Remark creation unsuccessful for ShipmentInstId: %s, processedInd will remain as false.", rmk.getShipmentInstId()));
                                syncRes.setError(e.getMessage());
                                unsuccessfulShipments.add(syncRes);
                            }
                        }
                    else {
                        processedSet.add(shipment.getShipmentInstId());
                        successfulShipments.add(syncRes);
                    }
                }
            }
            if(!CollectionUtils.isEmpty(processedSet)) {
                shmShiplifySubDAO.updateProcessedInd(processedSet, entityManager);
            }
        }
        entityManager.flush();
        SynchronizeShiplifyAccessorialsResp resp = new SynchronizeShiplifyAccessorialsResp();
        resp.setSuccessfulShipments(successfulShipments);
        resp.setUnsuccessfulShipments(unsuccessfulShipments);
        return resp;
    }

    private AccessorialSync getSyncAccessorial(Long shpInstId, String proNbrTxt) {
        AccessorialSync sync = new AccessorialSync();
        ShipmentId id = new ShipmentId();
        id.setShipmentInstId(BasicTransformer.toString(shpInstId));
        id.setProNumber(proNbrTxt);
        sync.setShipmentId(id);
        return sync;
    }

    public ListUnprocessedShiplifyRecordsResp listAccessorials(TransactionContext txnContext,
            EntityManager entityManager) {
        List<DmtShmShiplify> listDmtShmShipments = shmShiplifySubDAO.listUnprocessedshipments(entityManager);
        ListUnprocessedShiplifyRecordsResp resp = new ListUnprocessedShiplifyRecordsResp();
        resp.setUnprocessedShiplifyShipments(EntityTransformer.toShmShiplify(listDmtShmShipments));
        return resp;
    }
}