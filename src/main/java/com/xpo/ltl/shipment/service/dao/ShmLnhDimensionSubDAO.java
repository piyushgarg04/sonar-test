package com.xpo.ltl.shipment.service.dao;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.dao.ShmLnhDimensionDAO;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmLnhDimension;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmLnhDimensionPK_;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmLnhDimension_;
import com.xpo.ltl.api.shipment.service.entity.ShmLnhDimension;
import com.xpo.ltl.api.shipment.service.entity.ShmLnhDimensionPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmLnhDimension_;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmLnhDimensionSubDAO extends ShmLnhDimensionDAO<ShmLnhDimension> {

    public void bulkDeleteByShipmentInstId(final Long shipInstId, final EntityManager entityManager) {

        if (shipInstId == null) {
            return;
        }

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaDelete<ShmLnhDimension> criteriaDelete = criteriaBuilder.createCriteriaDelete(ShmLnhDimension.class);
        Root<ShmLnhDimension> scoTripNodeFrom = criteriaDelete.from(ShmLnhDimension.class);

        Path<Long> tripInstIdPath = scoTripNodeFrom.get(ShmLnhDimension_.id).get(ShmLnhDimensionPK_.shpInstId);
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(criteriaBuilder.equal(tripInstIdPath, shipInstId));

        criteriaDelete.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
        Query query = entityManager.createQuery(criteriaDelete);
        query.executeUpdate();
    }

    public void bulkDeleteByShipmentInstIdFromDB2(final Long shipInstId, final EntityManager db2EntityManager) {

        if (shipInstId == null) {
            return;
        }

        CriteriaBuilder criteriaBuilder = db2EntityManager.getCriteriaBuilder();
        CriteriaDelete<DB2ShmLnhDimension> criteriaDelete = criteriaBuilder
            .createCriteriaDelete(DB2ShmLnhDimension.class);
        Root<DB2ShmLnhDimension> db2ScoTripNodeFrom = criteriaDelete.from(DB2ShmLnhDimension.class);

        Path<Long> tripInstIdPath = db2ScoTripNodeFrom.get(DB2ShmLnhDimension_.id).get(DB2ShmLnhDimensionPK_.shpInstId);
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(criteriaBuilder.equal(tripInstIdPath, shipInstId));

        criteriaDelete.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
        Query query = db2EntityManager.createQuery(criteriaDelete);
        query.executeUpdate();
    }
    
	public void updateDB2ShmLnhDimension(final ShmLnhDimension shmLnhDimension,
			final Timestamp exadataLstUpdtTmst, final TransactionContext txnContext,
			final EntityManager db2EntityManager) throws ServiceException {
		final Function<DB2ShmLnhDimension, Boolean> checkVersionFunction = getCheckVersionFunction(
				exadataLstUpdtTmst);
		updateDB2ShmLnhDimension(shmLnhDimension, checkVersionFunction, db2EntityManager,
				txnContext);
	}

	private Function<DB2ShmLnhDimension, Boolean> getCheckVersionFunction(
			final Timestamp exadataLstUpdtTmst) {
		return db2Entity -> db2Entity.getLstUpdtTmst().compareTo(exadataLstUpdtTmst) <= 0;
	}

    public List<ShmLnhDimension> findByShipmentInstanceId(final Long shipmentInstId,
        final EntityManager entityManager) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<ShmLnhDimension> cq = cb.createQuery(ShmLnhDimension.class);
        final Root<ShmLnhDimension> from = cq.from(ShmLnhDimension.class);

        final Path<Long> shipmentInstIdPath = from.get(ShmLnhDimension_.id).get(ShmLnhDimensionPK_.shpInstId);
        final Path<Long> seqNbrPath = from.get(ShmLnhDimension_.id).get(ShmLnhDimensionPK_.dimSeqNbr);

        cq.select(from).where(cb.and(cb.equal(shipmentInstIdPath, shipmentInstId))).orderBy(cb.asc(seqNbrPath));

        return entityManager.createQuery(cq).getResultList();
    }

}