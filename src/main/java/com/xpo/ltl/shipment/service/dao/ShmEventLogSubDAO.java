package com.xpo.ltl.shipment.service.dao;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.shipment.service.dao.ShmEventLogDAO;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmEventLog;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmEventLogPK;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmEventLogPK_;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmEventLog_;
import com.xpo.ltl.api.shipment.service.entity.ShmEventLog;
import com.xpo.ltl.api.shipment.service.entity.ShmEventLogPK;
import com.xpo.ltl.api.shipment.service.entity.ShmEventLogPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmEventLog_;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import org.apache.commons.collections4.CollectionUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@LogExecutionTime
public class ShmEventLogSubDAO extends ShmEventLogDAO<ShmEventLog> {

	public ShmEventLog create(ShmEventLog entity, EntityManager entityManager) throws ValidationException {
		return super.save(entity, entityManager);
	}

    public long getLastUsedSeqNbr(long shpInstId, EntityManager entityManager, EntityManager db2EntityManager) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<ShmEventLog> shmEventLogRoot = criteriaQuery.from(ShmEventLog.class);
        Path<ShmEventLogPK> idPath = shmEventLogRoot.get(ShmEventLog_.id);
        Path<Long> shpInstIdPath = idPath.get(ShmEventLogPK_.shpInstId);
        Path<Long> seqNbrPath = idPath.get(ShmEventLogPK_.seqNbr);
        criteriaQuery
            .select(criteriaBuilder.max(seqNbrPath))
            .where(criteriaBuilder.equal(shpInstIdPath, shpInstId));
        TypedQuery<Long> typedQuery = entityManager.createQuery(criteriaQuery);
        Long maxSeqNbrExadata = typedQuery.getSingleResult() != null ? typedQuery.getSingleResult() : 0L;

        CriteriaBuilder criteriaBuilderDB2 = db2EntityManager.getCriteriaBuilder();
        CriteriaQuery<Short> criteriaQueryDB2 = criteriaBuilderDB2.createQuery(Short.class);
        Root<DB2ShmEventLog> shmEventLogRootDB2 = criteriaQueryDB2.from(DB2ShmEventLog.class);
        Path<DB2ShmEventLogPK> idPathDB2 = shmEventLogRootDB2.get(DB2ShmEventLog_.id);
        Path<Long> shpInstIdPathDB2 = idPathDB2.get(DB2ShmEventLogPK_.shpInstId);
        Path<Short> seqNbrPathDB2 = idPathDB2.get(DB2ShmEventLogPK_.seqNbr);
        criteriaQueryDB2.select(criteriaBuilderDB2.max(seqNbrPathDB2)).where(criteriaBuilderDB2.equal(shpInstIdPathDB2, shpInstId));
        TypedQuery<Short> typedQueryDB2 = db2EntityManager.createQuery(criteriaQueryDB2);
        Long maxSeqNbrDB2 = typedQueryDB2.getSingleResult() != null ? BasicTransformer.toLong(typedQueryDB2.getSingleResult()) : 0L;

        return Math.max(maxSeqNbrExadata, maxSeqNbrDB2);
    }

    public ShmEventLog getMostRecentEventLog(final Long shpInstId, final EntityManager entityManager) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<ShmEventLog> query = cb.createQuery(ShmEventLog.class);
        final Root<ShmEventLog> from = query.from(ShmEventLog.class);
        final Path<Long> shpInstIdPath = from.get(ShmEventLog_.id).get(ShmEventLogPK_.shpInstId);
        final Path<Long> seqNbrPath = from.get(ShmEventLog_.id).get(ShmEventLogPK_.seqNbr);

        query.where(cb.equal(shpInstIdPath, shpInstId)).orderBy(cb.desc(seqNbrPath));

        return entityManager.createQuery(query).getResultStream().findFirst().orElse(null);
    }

	public void deleteByShipmentId(final Long shpInstId, final EntityManager entityManager) {

		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaDelete<ShmEventLog> criteriaDelete = criteriaBuilder.createCriteriaDelete(ShmEventLog.class);
		final Root<ShmEventLog> from = criteriaDelete.from(ShmEventLog.class);
		final Path<Long> shpInstIdPath = from.get(ShmEventLog_.id).get(ShmEventLogPK_.shpInstId);

		final List<Predicate> predicates = new ArrayList<>();
		predicates.add(criteriaBuilder.equal(shpInstIdPath, shpInstId));

		criteriaDelete.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
		final Query query = entityManager.createQuery(criteriaDelete);
		query.executeUpdate();

	}

	public List<ShmEventLog> findByShmInstId(Long shipmentInstId, EntityManager entityManager) {
		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmEventLog> query = cb.createQuery(ShmEventLog.class);
		final Root<ShmEventLog> from = query.from(ShmEventLog.class);

		final List<Predicate> predicates = new ArrayList<>();

		final Path<Long> shipmentInstIdPath = from.get(ShmEventLog_.id).get(ShmEventLogPK_.shpInstId);
		predicates.add(cb.equal (shipmentInstIdPath, shipmentInstId));

		query.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
		final List<ShmEventLog> records = entityManager.createQuery(query).getResultList();

		if (CollectionUtils.isEmpty(records))
			return new ArrayList<>();

		return records;
	}
}
