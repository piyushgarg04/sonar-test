package com.xpo.ltl.shipment.service.delegates;

import java.math.BigDecimal;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcpPK;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.shipment.service.dao.ShipmentMovementExceptionSubDAO;

@ApplicationScoped
public class ShmMovementExceptionDelegate {

    @PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
    private EntityManager db2EntityManager;

    @Inject
    private ShipmentMovementExceptionSubDAO shipmentMovementExceptionSubDAO;

    public void createShmMovementException(ShmMovement shmMovement, String excpRptgSicCd, AuditInfo auditInfo, EntityManager entityManager)
            throws ServiceException {

        ShmMovementExcpPK id = new ShmMovementExcpPK();
        id.setShpInstId(shmMovement.getId().getShpInstId());
        id.setMvmtSeqNbr(shmMovement.getId().getSeqNbr());
        Long maxSeqNbr = shipmentMovementExceptionSubDAO.findMaxSeqNbr(id, entityManager);
        id.setSeqNbr(maxSeqNbr + 1);

        ShmMovementExcp shmMovementExcep = new ShmMovementExcp();
        shmMovementExcep.setId(id);
        shmMovementExcep.setArchiveCntlCd(StringUtils.SPACE);
        shmMovementExcep.setExcpRptgSicCd(excpRptgSicCd);
        shmMovementExcep.setExcpSeqNbr(BigDecimal.ZERO);
        shmMovementExcep.setPcsCnt(BigDecimal.ZERO);
        shmMovementExcep.setRfsdRsnCd(StringUtils.SPACE);
        shmMovementExcep.setRmrkTxt(StringUtils.SPACE);
        shmMovementExcep.setShmMovement(shmMovement);
        shmMovementExcep.setTypCd(StringUtils.SPACE);
        shmMovementExcep.setUndlvdRsnCd(StringUtils.SPACE);

        DtoTransformer.setAuditInfo(shmMovementExcep, auditInfo);
        shipmentMovementExceptionSubDAO.save(shmMovementExcep, entityManager);
        shipmentMovementExceptionSubDAO.createDB2ShmMovementExcp(shmMovementExcep, db2EntityManager);
    }

}
