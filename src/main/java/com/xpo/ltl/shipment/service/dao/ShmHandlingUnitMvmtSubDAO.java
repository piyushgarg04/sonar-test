package com.xpo.ltl.shipment.service.dao;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;

import com.xpo.ltl.api.shipment.service.dao.ShmHandlingUnitMvmtDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmt;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmtPK;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmtPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmt_;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitPK;
import com.xpo.ltl.shipment.service.dao.dto.ShmHandlingUnitMvmtPKDTO;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmHandlingUnitMvmtSubDAO extends ShmHandlingUnitMvmtDAO<ShmHandlingUnitMvmt> {

	public List<ShmHandlingUnitMvmt> findByShipInstId(Long shpInstId, EntityManager entityManager) {

		if (shpInstId == null) {
			return null;
		}
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<ShmHandlingUnitMvmt> query = cb.createQuery(ShmHandlingUnitMvmt.class);
		Root<ShmHandlingUnitMvmt> from = query.from(ShmHandlingUnitMvmt.class);

		Path<Long> shipmentInstanceId = from.get(ShmHandlingUnitMvmt_.id)
				.get(ShmHandlingUnitMvmtPK_.shpInstId);
		CriteriaQuery<ShmHandlingUnitMvmt> filter = query.select(from)
				.where(cb.equal(shipmentInstanceId, shpInstId));

		return entityManager.createQuery(filter).getResultList();

	}

	public long getNextMvmtBySeqNbrAndShpInstId(final Long shpInstId, final Long seqNbr,
			EntityManager entityManager) {
		checkNotNull(shpInstId, "shpInstId is required");
		checkNotNull(seqNbr, "seqNbr is required");

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<ShmHandlingUnitMvmt> from = cq.from(ShmHandlingUnitMvmt.class);

		Path<Long> mvmtSeqNbrPath = from.get(ShmHandlingUnitMvmt_.id)
				.get(ShmHandlingUnitMvmtPK_.mvmtSeqNbr);
		Path<Long> seqNbrPath = from.get(ShmHandlingUnitMvmt_.id)
				.get(ShmHandlingUnitMvmtPK_.seqNbr);
		Path<Long> shpInstIdPath = from.get(ShmHandlingUnitMvmt_.id)
				.get(ShmHandlingUnitMvmtPK_.shpInstId);

		cq.select(cb.max(mvmtSeqNbrPath));
		cq.where(cb.and(cb.equal(shpInstIdPath, shpInstId), cb.equal(seqNbrPath, seqNbr)));

		Long maxId = entityManager.createQuery(cq).getSingleResult();

		return (maxId != null) ? maxId + 1 : 1L;
	}

    /**
     * the following query will return max mvmt sequence number for each of the given {@code ShmHandlingUnitPK}
     * received by param.
     * <br/>
     * if one of the pairs (shpInstId, seqNbr) has no movement yet, it will not be added to the response.
     *
     * @param shpInstIdAndSeqList
     * @param entityManager
     * @return
     */
    public List<ShmHandlingUnitMvmtPK> getMaxMvmtPKByShpInstIdAndSeqNbrPairs(
        List<ShmHandlingUnitPK> shmHandlingUnitPKList, EntityManager entityManager) {

        checkNotNull(shmHandlingUnitPKList, "shmHandlingUnitPKList is required");

        CriteriaBuilder criteria = entityManager.getCriteriaBuilder();
        CriteriaQuery<ShmHandlingUnitMvmtPK> query = criteria.createQuery(ShmHandlingUnitMvmtPK.class);
        Root<ShmHandlingUnitMvmt> from = query.from(ShmHandlingUnitMvmt.class);

        Path<Long> shpInstIdPath = from.get(ShmHandlingUnitMvmt_.id).get(ShmHandlingUnitMvmtPK_.shpInstId);
        Path<Long> seqNbrPath = from.get(ShmHandlingUnitMvmt_.id).get(ShmHandlingUnitMvmtPK_.seqNbr);
        query
            .select(criteria
                .construct(ShmHandlingUnitMvmtPKDTO.class,
                    shpInstIdPath, seqNbrPath,
                    criteria.max(from.get(ShmHandlingUnitMvmt_.id).get(ShmHandlingUnitMvmtPK_.mvmtSeqNbr))));

        List<Predicate> predicates = new ArrayList<>();
        for (ShmHandlingUnitPK shmHandlingUnitPK : shmHandlingUnitPKList) {
            predicates
                .add(criteria
                    .and(criteria.equal(shpInstIdPath, shmHandlingUnitPK.getShpInstId()),
                        criteria.equal(seqNbrPath, shmHandlingUnitPK.getSeqNbr())));
        }

        query.where(criteria.or(predicates.toArray(new Predicate[CollectionUtils.size(predicates)])));
        query.groupBy(shpInstIdPath, seqNbrPath);

        return entityManager.createQuery(query).getResultList();
    }

	public List<ShmHandlingUnitMvmt> listHandlingUnitMovementByShpInstIdAndSeqNbr(final Long shpInstId, final Long seqNbr,
			EntityManager entityManager) {
		checkNotNull(shpInstId, "shpInstId is required");
		checkNotNull(seqNbr, "seqNbr is required");

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<ShmHandlingUnitMvmt> cq = cb.createQuery(ShmHandlingUnitMvmt.class);
		Root<ShmHandlingUnitMvmt> from = cq.from(ShmHandlingUnitMvmt.class);

		Path<Long> shpInstIdPath =
				from.get(ShmHandlingUnitMvmt_.id).get(ShmHandlingUnitMvmtPK_.shpInstId);
		Path<Long> seqNbrPath = from.get(ShmHandlingUnitMvmt_.id).get(ShmHandlingUnitMvmtPK_.seqNbr);

		cq.select(from).where(cb.and(cb.equal(shpInstIdPath, shpInstId), cb.equal(seqNbrPath, seqNbr)));

		return getResultList(cq, entityManager);
    }

    public int deleteShmHandlingUnitMvmtByShmHUPK(ShmHandlingUnitPK id, EntityManager entityManager) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaDelete<ShmHandlingUnitMvmt> criteriaDelete = cb.createCriteriaDelete(ShmHandlingUnitMvmt.class);
        Root<ShmHandlingUnitMvmt> from = criteriaDelete.from(ShmHandlingUnitMvmt.class);

        Path<Long> shpInstIdPath = from.get(ShmHandlingUnitMvmt_.id).get(ShmHandlingUnitMvmtPK_.shpInstId);
        Path<Long> seqNbrPath = from.get(ShmHandlingUnitMvmt_.id).get(ShmHandlingUnitMvmtPK_.seqNbr);

        criteriaDelete
            .where(cb.and(cb.equal(shpInstIdPath, id.getShpInstId()), cb.equal(seqNbrPath, id.getSeqNbr())));

        return entityManager.createQuery(criteriaDelete).executeUpdate();
    }

    public int deleteDB2ShmHandlingUnitMvmtByShmHUPK(ShmHandlingUnitPK id, EntityManager db2EntityManager) {
        CriteriaBuilder cb = db2EntityManager.getCriteriaBuilder();
        CriteriaDelete<ShmHandlingUnitMvmt> criteriaDelete = cb.createCriteriaDelete(ShmHandlingUnitMvmt.class);
        Root<ShmHandlingUnitMvmt> from = criteriaDelete.from(ShmHandlingUnitMvmt.class);

        Path<Long> shpInstIdPath = from.get(ShmHandlingUnitMvmt_.id).get(ShmHandlingUnitMvmtPK_.shpInstId);
        Path<Long> seqNbrPath = from.get(ShmHandlingUnitMvmt_.id).get(ShmHandlingUnitMvmtPK_.seqNbr);

        criteriaDelete.where(cb.and(cb.equal(shpInstIdPath, id.getShpInstId()), cb.equal(seqNbrPath, id.getSeqNbr())));

        return db2EntityManager.createQuery(criteriaDelete).executeUpdate();
    }

    public List<ShmHandlingUnitMvmt> findByIds(List<ShmHandlingUnitMvmtPK> mvmtIds, EntityManager entityManager) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmHandlingUnitMvmt> query = cb.createQuery(ShmHandlingUnitMvmt.class);
		final Root<ShmHandlingUnitMvmt> from = query.from(ShmHandlingUnitMvmt.class);

		final Predicate predicate = from.get(ShmHandlingUnitMvmt_.id).in(mvmtIds);
		query.select(from).where(predicate);
		final List<ShmHandlingUnitMvmt> records = entityManager.createQuery(query).getResultList();

		if (CollectionUtils.isEmpty(records))
			return new ArrayList<>();

		return records;
    }
}
