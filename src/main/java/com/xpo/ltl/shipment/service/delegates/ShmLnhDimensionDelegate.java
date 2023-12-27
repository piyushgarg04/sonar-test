package com.xpo.ltl.shipment.service.delegates;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmLnhDimension;
import com.xpo.ltl.api.shipment.service.entity.ShmLnhDimensionPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmLnhDimensionSubDAO;

@ApplicationScoped
public class ShmLnhDimensionDelegate {

    private static final Log LOGGER = LogFactory.getLog(ShmLnhDimensionDelegate.class);

    @Inject
    private ShmLnhDimensionSubDAO shmLnhDimensionSubDAO;

    @PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
    private EntityManager db2EntityManager;

    private static final Map<String, String> MAP_CAPTURED_BY_CD = new HashMap<String, String>() {

        private static final long serialVersionUID = 1L;
        {
            put("ACCURACY", "A");
            put("DOCK", "D");
            put("PICKUP", "P");
            put("PICKUP_DIMENSIONER", "U");
        }
    };

    public void updateShmLnhDimension(ShmLnhDimension shmLnhDimension, BigDecimal height, BigDecimal width,
        BigDecimal length, String capturedByUserId, String dimensionTypeCd, AuditInfo auditInfo,
            EntityManager entityManager, TransactionContext txnContext) throws ServiceException {

        shmLnhDimension.setLenNbr(length);
        shmLnhDimension.setHghtNbr(height);
        shmLnhDimension.setWdthNbr(width);

        if (StringUtils.isNotBlank(capturedByUserId)) {
            shmLnhDimension.setCapturedByUid(capturedByUserId);
        }
        if (StringUtils.isNotBlank(dimensionTypeCd)) {
            shmLnhDimension.setCapturedByCd(MAP_CAPTURED_BY_CD.get(dimensionTypeCd));
        }
        shmLnhDimension.setCapturedByTmst(new Timestamp(System.currentTimeMillis()));

        DtoTransformer.setLstUpdateAuditInfo(shmLnhDimension, auditInfo);

        shmLnhDimension = shmLnhDimensionSubDAO.save(shmLnhDimension, entityManager);
        shmLnhDimensionSubDAO.updateDB2ShmLnhDimension(shmLnhDimension,
                shmLnhDimension.getLstUpdtTmst(), txnContext, db2EntityManager);
    }

    public void createShmLnhDimension(ShmLnhDimensionPK pk, BigDecimal height, BigDecimal width,
        BigDecimal length, String capturedByUserId, String dimensionTypeCd, AuditInfo auditInfo,
        EntityManager entityManager, TransactionContext txnContext) throws ServiceException {

        if (BigDecimal.ZERO.compareTo(height) == 0 && BigDecimal.ZERO.compareTo(width) == 0 && BigDecimal.ZERO.compareTo(length) == 0) {
            LOGGER.info("Dimension (height, width, length) are 0.");
            return;
        }

        ShmLnhDimension shmLnhDimension = new ShmLnhDimension();
        shmLnhDimension.setId(pk);
        shmLnhDimension.setLenNbr(length);
        shmLnhDimension.setHghtNbr(height);
        shmLnhDimension.setWdthNbr(width);
        shmLnhDimension.setPcsCnt(BigDecimal.ONE);

        if (StringUtils.isNotBlank(capturedByUserId)) {
            shmLnhDimension.setCapturedByUid(capturedByUserId);
        }
        if (StringUtils.isNotBlank(dimensionTypeCd)) {
            shmLnhDimension.setCapturedByCd(MAP_CAPTURED_BY_CD.get(dimensionTypeCd));
        } else {
            shmLnhDimension.setCapturedByCd(StringUtils.SPACE);
        }
        shmLnhDimension.setCapturedByTmst(new Timestamp(System.currentTimeMillis()));
        shmLnhDimension.setStackableInd(BasicTransformer.toString(false));

        DtoTransformer.setAuditInfo(shmLnhDimension, auditInfo);

        shmLnhDimensionSubDAO.persist(shmLnhDimension, entityManager);
        shmLnhDimensionSubDAO.createDB2ShmLnhDimension(shmLnhDimension, db2EntityManager);
    }

    public void deleteDimensions(ShmShipment shmShipment, EntityManager entityManager) {

        shmLnhDimensionSubDAO.bulkDeleteByShipmentInstId(shmShipment.getShpInstId(), entityManager);
        shmLnhDimensionSubDAO.bulkDeleteByShipmentInstIdFromDB2(shmShipment.getShpInstId(), db2EntityManager);

    }

}
