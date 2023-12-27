package com.xpo.ltl.shipment.service.dao;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.dao.ShmXdockExcpDAO;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmXdockExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmTmDtCritical;
import com.xpo.ltl.api.shipment.service.entity.ShmXdockExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmXdockExcpPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmXdockExcp_;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmXdockExcpSubDAO extends ShmXdockExcpDAO<ShmXdockExcp> {

	private static String TDC_SQL = "SELECT tdc FROM ShmTmDtCritical tdc WHERE tdc.id.shpInstId in :shpIdList ";

	public List<ShmTmDtCritical> listTdcForShipmentIdList(List<Long> shipmentInstIds, EntityManager entityManager) {
		//For performance reasons DO NOT change this from a native query to use criteria builder.
		TypedQuery<Tuple> query = entityManager.createQuery(TDC_SQL, Tuple.class);
		query.setParameter("shpIdList", shipmentInstIds);
		List<Tuple> results = query.getResultList();
		List<ShmTmDtCritical> tdcList = new ArrayList<>();
		for (Tuple tuple : results) {
			tdcList.add((ShmTmDtCritical)tuple.get(0));
		}
		return tdcList;
	}

	public List<ShmXdockExcp> findByShipmentInstId(Long shipmentInstId, EntityManager entityManager) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<ShmXdockExcp> query = cb.createQuery(ShmXdockExcp.class);
		Root<ShmXdockExcp> from = query.from(ShmXdockExcp.class);

		final Expression<Long> shipmentInstIdPath = from.get(ShmXdockExcp_.id).get(ShmXdockExcpPK_.shpInstId);

		query.select(from).where(cb.equal(shipmentInstIdPath, shipmentInstId));
		List<ShmXdockExcp> result = entityManager.createQuery(query).getResultList();
		return result;
	}

    /**
     * Updates the DB2 entity with values from Oracle entity.
     *
     * @param shmXdockExcp
     * @param exadataLstUpdtTmst
     * @param txnContext
     * @param db2EntityManager
     * @throws ServiceException
     */
    public void updateDB2ShmXdockExcp(ShmXdockExcp shmXdockExcp, Timestamp exadataLstUpdtTmst,
        TransactionContext txnContext, EntityManager db2EntityManager) throws ServiceException {

        Function<DB2ShmXdockExcp, Boolean> checkVersionFunction = getCheckVersionFunction(exadataLstUpdtTmst);
        updateDB2ShmXdockExcp(shmXdockExcp, checkVersionFunction, db2EntityManager, txnContext);
        db2EntityManager.flush();
    }

    private Function<DB2ShmXdockExcp, Boolean> getCheckVersionFunction(Timestamp exadataLstUpdtTmst) {
        return db2Entity -> db2Entity.getCrteTmst().compareTo(exadataLstUpdtTmst) <= 0;
    }

	public long getNextSeqNbrByShpInstId(final Long shpInstId, EntityManager entityManager) {
		checkNotNull(shpInstId, "shpInstId is required");
		checkNotNull(entityManager, "entityManager is required");

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<ShmXdockExcp> from = cq.from(ShmXdockExcp.class);

		Path<Long> seqNbrPath = from.get(ShmXdockExcp_.id).get(ShmXdockExcpPK_.seqNbr);
		Path<Long> shpInstIdPath = from.get(ShmXdockExcp_.id).get(ShmXdockExcpPK_.shpInstId);

		cq.select(cb.max(seqNbrPath));
		cq.where(cb.equal(shpInstIdPath, shpInstId));

		Long maxId = entityManager.createQuery(cq).getSingleResult();

		return (maxId != null) ? maxId + 1 : 1L;
	}
}