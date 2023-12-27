package com.xpo.ltl.shipment.service.impl;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementPK;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmMovementSubDAO;
import java.sql.Timestamp;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@RequestScoped
public class DeleteShipmentMovementImpl {

    @PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
    private EntityManager db2EntityManager;

    @Inject
    private ShmMovementSubDAO shmMovementSubDAO;

    public void deleteShipmentMovement(Long shipmentInstId, Integer movementSeqNbr, TransactionContext txnContext, EntityManager entityManager) throws ServiceException {
        ShmMovementPK id = new ShmMovementPK();
        id.setShpInstId(shipmentInstId);
        id.setSeqNbr(BasicTransformer.toLong(movementSeqNbr));

        //TODO: since ShmMovement has no lstUpdtTmst attribute, so crteTmst attribute is used to
        // excecute checkVersion for DB2 databese call.
        Timestamp createdTimestamp = shmMovementSubDAO.deleteShmMovement(id, entityManager);
        shmMovementSubDAO.deleteShmMovementFromDB2(id, createdTimestamp, txnContext, db2EntityManager);
    }
}
