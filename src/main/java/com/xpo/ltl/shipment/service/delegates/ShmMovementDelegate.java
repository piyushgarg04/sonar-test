package com.xpo.ltl.shipment.service.delegates;

import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementPK;
import com.xpo.ltl.api.shipment.transformer.v2.DeliveryQualifierCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentMovementTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.DeliveryQualifierCd;
import com.xpo.ltl.api.shipment.v2.EquipmentId;
import com.xpo.ltl.api.shipment.v2.ShipmentMovementTypeCd;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmMovementSubDAO;
import com.xpo.ltl.shipment.service.util.TimestampUtil;

@ApplicationScoped
public class ShmMovementDelegate {

    @PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
    private EntityManager db2EntityManager;

    @Inject
    private ShmMovementSubDAO shmMovementSubDAO;


    public ShmMovement createShmMvmt(Optional<EquipmentId> trailerOpt, String reportingMvmtSicCd, boolean reshipCredInd,
        ShipmentMovementTypeCd shmMvmtTypeCd, Optional<DeliveryQualifierCd> dlvryQfrCd,
        long shpInstId, AuditInfo auditInfo, EntityManager entityManager) throws ServiceException {

        ShmMovement mostRecentShmMovment = shmMovementSubDAO.findMostRecentByShpInstId(shpInstId, entityManager);
        long seqNbr = mostRecentShmMovment == null ? 1 : mostRecentShmMovment.getId().getSeqNbr() + 1;

        ShmMovement shmMovement = new ShmMovement();
        ShmMovementPK shmMovementPK = new ShmMovementPK();
        shmMovementPK.setShpInstId(shpInstId);
        shmMovementPK.setSeqNbr(seqNbr);
        shmMovement.setId(shmMovementPK);
        shmMovement.setRshpCredInd(BasicTransformer.toString(reshipCredInd));
        shmMovement.setMvmtRptgSicCd(reportingMvmtSicCd);
        shmMovement.setMvmtTmst(BasicTransformer.toTimestamp(auditInfo.getUpdatedTimestamp()));
        shmMovement.setTypCd(ShipmentMovementTypeCdTransformer.toCode(shmMvmtTypeCd));
        if (trailerOpt.isPresent()) {
            EquipmentId trailer = trailerOpt.get();
            String trailerInstId = trailer.getEquipmentInstId();
            String equipmentPrefix = trailer.getEquipmentPrefix();
            String equipmentSuffix = trailer.getEquipmentSuffix();
            shmMovement.setTrlrInstId(StringUtils.isNotBlank(trailerInstId) ? new BigDecimal(trailerInstId) : ZERO);
            shmMovement.setTrlrIdSfxNbr(StringUtils.isNotBlank(equipmentSuffix) ? new BigDecimal(equipmentSuffix) : ZERO);
            shmMovement.setTrlrIdPfxTxt(StringUtils.isNotBlank(equipmentPrefix) ? equipmentPrefix : StringUtils.SPACE);
        } else {
            shmMovement.setTrlrIdPfxTxt(StringUtils.SPACE);
            shmMovement.setTrlrIdSfxNbr(ZERO);
            shmMovement.setTrlrInstId(ZERO);
        }
        shmMovement.setTrlrLdSeqNbr(ZERO);
        shmMovement.setObcPkpDlvRteCd(StringUtils.SPACE);
        shmMovement.setScacCd(StringUtils.SPACE);
        shmMovement.setCurrSicCd(reportingMvmtSicCd);
        shmMovement.setArchiveCntlCd(StringUtils.SPACE);
        shmMovement.setCustLocArivTmst(TimestampUtil.getLowTimestamp());
        shmMovement.setQlfrRsnCd(StringUtils.SPACE);
        shmMovement.setDlvryQalfrCd(dlvryQfrCd.isPresent() ? DeliveryQualifierCdTransformer.toCode(dlvryQfrCd.get()) : StringUtils.SPACE);
        shmMovement.setRtePfxTxt(StringUtils.SPACE);
        shmMovement.setRteSfxTxt(StringUtils.SPACE);
        shmMovement.setCarrierId(ZERO);

        DtoTransformer.setAuditInfo(shmMovement, auditInfo);
        shmMovementSubDAO.save(shmMovement, entityManager);
        shmMovementSubDAO.createDB2ShmMovement(shmMovement, db2EntityManager);
        return shmMovement;
    }

}
