package com.xpo.ltl.shipment.service.dao;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

import org.apache.commons.collections4.CollectionUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.dao.ShmHazMatDAO;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmHazMat;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmHazMatPK_;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmHazMat_;
import com.xpo.ltl.api.shipment.service.entity.ShmHazMat;
import com.xpo.ltl.api.shipment.service.entity.ShmHazMatPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmHazMat_;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmHazMatSubDAO extends ShmHazMatDAO<ShmHazMat> {

	public List<ShmHazMat> getShipmentLevelHazMats(final long shipmentInstId,
			final EntityManager entityManager) {
		final CriteriaBuilder cb = checkNotNull(entityManager, "The EntityManager is required.")
				.getCriteriaBuilder();
		final CriteriaQuery<ShmHazMat> cq = cb.createQuery(ShmHazMat.class);
		final Root<ShmHazMat> from = cq.from(ShmHazMat.class);

		final Path<Long> shipmentInstIdPath = from.get(ShmHazMat_.id).get(ShmHazMatPK_.shpInstId);
		final Path<Long> hazmatSeqNbrPath = from.get(ShmHazMat_.id).get(ShmHazMatPK_.hmSeqNbr);

		cq.select(from).where(cb.and(cb.equal(shipmentInstIdPath, shipmentInstId)))
				.orderBy(cb.asc(hazmatSeqNbrPath));

		List<ShmHazMat> hazMatList = entityManager.createQuery(cq).getResultList();
		return hazMatList.size() > 0 ? hazMatList : null;
	}

	public List<ShmHazMat> listShipmentsLevelHazMats(final List<Long> shipmentInstIds,
			final EntityManager entityManager) {
		final CriteriaBuilder cb = checkNotNull(entityManager, "The EntityManager is required.")
				.getCriteriaBuilder();
		final CriteriaQuery<ShmHazMat> cq = cb.createQuery(ShmHazMat.class);
		final Root<ShmHazMat> from = cq.from(ShmHazMat.class);

		final Path<Long> shipmentInstIdPath = from.get(ShmHazMat_.id).get(ShmHazMatPK_.shpInstId);
		final Path<Long> hazmatSeqNbrPath = from.get(ShmHazMat_.id).get(ShmHazMatPK_.hmSeqNbr);

		cq.select(from).where(shipmentInstIdPath.in(shipmentInstIds))
				.orderBy(cb.asc(hazmatSeqNbrPath));

		List<ShmHazMat> hazMatList = entityManager.createQuery(cq).getResultList();
		return hazMatList.size() > 0 ? hazMatList : null;
	}

	public List<ShmHazMat> findAllByShpInstId(final Long shpInstId, EntityManager entityManager) {
		checkNotNull(shpInstId, "shpInstId is required");
		checkNotNull(entityManager, "entityManager is required");

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<ShmHazMat> cq = cb.createQuery(ShmHazMat.class);
		Root<ShmHazMat> from = cq.from(ShmHazMat.class);

		Predicate eqShpInstId = cb.equal(from.get(ShmHazMat_.id).get(ShmHazMatPK_.shpInstId),
				shpInstId);

		cq.select(from).where(eqShpInstId);

		return getResultList(cq, entityManager);
	}

	public long getNextSeqNbrByShpInstId(final Long shpInstId, EntityManager entityManager) {
		checkNotNull(shpInstId, "shpInstId is required");
		checkNotNull(entityManager, "entityManager is required");

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<ShmHazMat> from = cq.from(ShmHazMat.class);

		Path<Long> hmSeqNbrPath = from.get(ShmHazMat_.id).get(ShmHazMatPK_.hmSeqNbr);
		Path<Long> shpInstIdPath = from.get(ShmHazMat_.id).get(ShmHazMatPK_.shpInstId);

		cq.select(cb.max(hmSeqNbrPath));
		cq.where(cb.equal(shpInstIdPath, shpInstId));

		Long maxId = entityManager.createQuery(cq).getSingleResult();

		return (maxId != null) ? maxId + 1 : 1L;
	}

	public void removeDB2ShmHazMats(List<ShmHazMat> shmHazMats, EntityManager db2EntityManager,
			TransactionContext txnContext) throws ServiceException {
		checkNotNull(shmHazMats, "shmHazMats is required");
		checkNotNull(db2EntityManager, "db2EntityManager is required");

		if (CollectionUtils.isNotEmpty(shmHazMats)) {
			for (ShmHazMat shmHazMat : shmHazMats) {
				final Function<DB2ShmHazMat, Boolean> checkVersionFunction = getCheckVersionFunction(
						shmHazMat.getLstUpdtTmst());
				deleteDB2ShmHazMat(shmHazMat.getId(), checkVersionFunction, db2EntityManager,
						txnContext);
			}
		}
	}

	public DB2ShmHazMat insertDB2ShmHazMat(ShmHazMat shmHazMat, EntityManager db2EntityManager) {
		checkNotNull(shmHazMat, "shmHazMat is required");
		checkNotNull(db2EntityManager, "db2EntityManager is required");

		if (Objects.isNull(shmHazMat.getTransIndex())) {
			shmHazMat.setTransIndex(new BigDecimal(0));
		}
		return createDB2ShmHazMat(shmHazMat, null, (short) 0, db2EntityManager);
	}

	private Function<DB2ShmHazMat, Boolean> getCheckVersionFunction(Timestamp exadataLstUpdtTmst) {
		return db2Entity -> db2Entity.getLstUpdtTmst().compareTo(exadataLstUpdtTmst) <= 0;
	}

    public void bulkDeleteByShipmentInstId(final Long shipInstId, final EntityManager entityManager) {

        if (shipInstId == null) {
            return;
        }

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaDelete<ShmHazMat> criteriaDelete = criteriaBuilder.createCriteriaDelete(ShmHazMat.class);
        Root<ShmHazMat> scoTripNodeFrom = criteriaDelete.from(ShmHazMat.class);

        Path<Long> shipInstIdPath = scoTripNodeFrom.get(ShmHazMat_.id).get(ShmHazMatPK_.shpInstId);
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(criteriaBuilder.equal(shipInstIdPath, shipInstId));

        criteriaDelete.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
        Query query = entityManager.createQuery(criteriaDelete);
        query.executeUpdate();
    }

    public void bulkDeleteByShipmentInstIdFromDB2(final Long shipInstId, final EntityManager db2EntityManager) {

        if (shipInstId == null) {
            return;
        }

        CriteriaBuilder criteriaBuilder = db2EntityManager.getCriteriaBuilder();
        CriteriaDelete<DB2ShmHazMat> criteriaDelete = criteriaBuilder.createCriteriaDelete(DB2ShmHazMat.class);
        Root<DB2ShmHazMat> db2ScoTripNodeFrom = criteriaDelete.from(DB2ShmHazMat.class);

        Path<Long> shipInstIdPath = db2ScoTripNodeFrom.get(DB2ShmHazMat_.id).get(DB2ShmHazMatPK_.shpInstId);
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(criteriaBuilder.equal(shipInstIdPath, shipInstId));

        criteriaDelete.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
        Query query = db2EntityManager.createQuery(criteriaDelete);
        query.executeUpdate();
    }
}