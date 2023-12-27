package com.xpo.ltl.shipment.service.dao;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.jpa.QueryHints;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.dao.ShmMovementDAO;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmMovement;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmMovementPK_;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmMovement_;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementPK;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement_;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.enums.MovementTypeEnum;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmMovementSubDAO extends ShmMovementDAO<ShmMovement> {

	private static final String TCON = MovementTypeEnum.TCON.getCode();

	@Inject
	private AppContext appContext;

	public List<ShmMovement> bulkReadBy(
			Set<Long> shpInstIds, MovementTypeEnum movementTypeEnum, EntityManager entityManager) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<ShmMovement> query = criteriaBuilder.createQuery(ShmMovement.class);
		Root<ShmMovement> from = query.from(ShmMovement.class);

		Path<Long> shpInstIdPath = from.get(ShmMovement_.id).get(ShmMovementPK_.shpInstId);
		Path<String> typeCdPath = from.get(ShmMovement_.typCd);
		Path<Timestamp> createTmstPath = from.get(ShmMovement_.crteTmst);

		List<ShmMovement> shmMovements = new ArrayList<>();
		List<Long> shipmentInstId = new ArrayList<>(shpInstIds);
		for (List<Long> splitShpInstIds : Lists.partition(shipmentInstId, appContext.getMaxCountForInClause())) {
			List<Predicate> predicates = Lists.newArrayList();
			predicates.add(shpInstIdPath.in(splitShpInstIds));
			predicates.add(createTmstPath.in(getMaxCreateTmstPerShipment(splitShpInstIds, criteriaBuilder, query)));
			predicates.add(criteriaBuilder.equal(typeCdPath, movementTypeEnum.getCode()));

			query.select(from).where(predicates.toArray(new Predicate[CollectionUtils.size(predicates)]));
			shmMovements.addAll(entityManager.createQuery(query).getResultList());
		}
		return shmMovements;
	}

	private Subquery getMaxCreateTmstPerShipment(List<Long> tripInstIds, CriteriaBuilder criteriaBuilder, CriteriaQuery<ShmMovement> query) {
		Subquery subquery = query.subquery(BigDecimal.class);
		Root<ShmMovement> subRoot = subquery.from(ShmMovement.class);
		Path<Long> shpInstIdPath = subRoot.get(ShmMovement_.id).get(ShmMovementPK_.shpInstId);
		subquery.select(criteriaBuilder.greatest(subRoot.get(ShmMovement_.crteTmst)))
				.where(shpInstIdPath.in(tripInstIds))
				.groupBy(shpInstIdPath);
		return subquery;
	}

	public void bulkDelete(Collection<? extends Number> shipmentIds, MovementTypeEnum movementTypeEnum, EntityManager entityManager) {
		if (CollectionUtils.isEmpty(shipmentIds)) {
			return;
		}

		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaDelete<ShmMovement> criteriaDelete = criteriaBuilder.createCriteriaDelete(ShmMovement.class);
		Root<ShmMovement> movementRoot = criteriaDelete.from(ShmMovement.class);

		Path<Long> shipmentInstIdPath = movementRoot.get(ShmMovement_.id).get(ShmMovementPK_.shpInstId);
		Path<String> typeCdPath = movementRoot.get(ShmMovement_.typCd);

		List<Predicate> predicates = new ArrayList<>();
		predicates.add(shipmentInstIdPath.in(shipmentIds));
		predicates.add(criteriaBuilder.equal(typeCdPath, movementTypeEnum.getCode()));

		criteriaDelete.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
		Query query = entityManager.createQuery(criteriaDelete);
		query.executeUpdate();
	}

	public void bulkDeleteFromDB2(Collection<? extends Number> shipmentIds, MovementTypeEnum movementTypeEnum, EntityManager db2EntityManager) {
		if (CollectionUtils.isEmpty(shipmentIds)) {
			return;
		}

		CriteriaBuilder criteriaBuilder = db2EntityManager.getCriteriaBuilder();
		CriteriaDelete<DB2ShmMovement> criteriaDelete = criteriaBuilder.createCriteriaDelete(DB2ShmMovement.class);
		Root<DB2ShmMovement> movementRoot = criteriaDelete.from(DB2ShmMovement.class);

		Path<Long> shipmentInstIdPath = movementRoot.get(DB2ShmMovement_.id).get(DB2ShmMovementPK_.shpInstId);
		Path<String> typeCdPath = movementRoot.get(DB2ShmMovement_.typCd);

		List<Predicate> predicates = new ArrayList<>();
		predicates.add(shipmentInstIdPath.in(shipmentIds));
		predicates.add(criteriaBuilder.equal(typeCdPath, movementTypeEnum.getCode()));

		criteriaDelete.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
		Query query = db2EntityManager.createQuery(criteriaDelete);
		query.executeUpdate();
	}

	public ShmMovement findMostRecentByShpInstId(Long shpInstId, EntityManager entityManager){
		checkNotNull(shpInstId, "shpInstId is required");
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<ShmMovement> query = criteriaBuilder.createQuery(ShmMovement.class);

		Root<ShmMovement> from = query.from(ShmMovement.class);
		Path<Long> shpInstIdPath = from.get(ShmMovement_.id).get(ShmMovementPK_.shpInstId);
		Path<Long> seqNbrPath = from.get(ShmMovement_.id).get(ShmMovementPK_.seqNbr);
		query.select(from)
				.where(criteriaBuilder.equal(shpInstIdPath, shpInstId))
				.orderBy(criteriaBuilder.desc(seqNbrPath));
		return getSingleResultOrNull(query, entityManager);
	}

	public List<ShmMovement> findCloseMovementsByTrailer(BigDecimal trlrInstId, Timestamp mvmtTmst,
			EntityManager entityManager) {

		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<ShmMovement> query = criteriaBuilder.createQuery(ShmMovement.class);

		Root<ShmMovement> from = query.from(ShmMovement.class);
		Path<BigDecimal> trlrInstIdPath = from.get(ShmMovement_.trlrInstId);
		Path<Timestamp> mvmtTmstPath = from.get(ShmMovement_.mvmtTmst);
		Path<String> typCdPath = from.get(ShmMovement_.typCd);
		Path<Long> shpInstIdPath = from.get(ShmMovement_.id).get(ShmMovementPK_.shpInstId);
		Path<Long> seqNbrPath = from.get(ShmMovement_.id).get(ShmMovementPK_.seqNbr);

		List<Predicate> predicates = new ArrayList<>();
		predicates.add(criteriaBuilder.equal(trlrInstIdPath, trlrInstId));
		predicates.add(criteriaBuilder.equal(typCdPath, TCON));

		query.select(from)
				.where(predicates.toArray(new Predicate[predicates.size()]))
				.orderBy(criteriaBuilder.asc(shpInstIdPath), criteriaBuilder.desc(seqNbrPath));

		return entityManager.createQuery(query).getResultList();
	}

	public List<ShmMovement> getLatestMovementsForShipmentIdSetByMovementType(
			Set<Long> shpInsIds,
			MovementTypeEnum outForDelivery,
			EntityManager entityManager) {

		if (CollectionUtils.isEmpty(shpInsIds))
			return Lists.newArrayList();

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmMovement> query = cb.createQuery(ShmMovement.class);
		final Root<ShmMovement> from = query.from(ShmMovement.class);

		Path<Long> shpInstIdPath = from.get(ShmMovement_.id).get(ShmMovementPK_.shpInstId);
		Path<Long> seqNbrPath = from.get(ShmMovement_.id).get(ShmMovementPK_.seqNbr);
		Path<String> typCdPath = from.get(ShmMovement_.typCd);

		final List<Predicate> predicates = Lists.newArrayList();

		predicates.add(shpInstIdPath.in(shpInsIds));
		predicates.add(cb.equal(typCdPath, outForDelivery.getCode()));
		predicates.add(cb.equal(seqNbrPath, getMaxSequenceNbr(shpInstIdPath, cb, query)));

		query
			.select(from)
			.where(predicates.toArray(new Predicate[CollectionUtils.size(predicates)]));

		return entityManager.createQuery(query).getResultList();
	}

	private Subquery<Long> getMaxSequenceNbr(Path<Long> shpInstIdPath,
									  CriteriaBuilder cb,
									  CriteriaQuery<ShmMovement> query) {

		Subquery<Long> subQuery = query.subquery(Long.class);
		Root<ShmMovement> subRoot = subQuery.from(ShmMovement.class);

		subQuery.select(cb.greatest(subRoot.get(ShmMovement_.id).get(ShmMovementPK_.seqNbr)))
				.where(cb.equal(shpInstIdPath, subRoot.get(ShmMovement_.id).get(ShmMovementPK_.shpInstId)));

		return subQuery;
	}

	public Timestamp deleteShmMovement(ShmMovementPK shmMovementPK, EntityManager entityManager) throws ServiceException {
		ShmMovement shmMovementToDelete = findById(shmMovementPK, entityManager);
		Timestamp createdTimestamp = shmMovementToDelete.getCrteTmst();
		entityManager.remove(shmMovementToDelete);
		return createdTimestamp;
	}

	public void deleteShmMovementFromDB2(ShmMovementPK shmMovementPK,
									 	Timestamp exadataLstUpdtTmst,
									 	TransactionContext txnContext,
									 	EntityManager db2EntityManager) throws ServiceException {

		Function<DB2ShmMovement, Boolean> versionCheck = getCheckVersionFunction(exadataLstUpdtTmst);
		deleteDB2ShmMovement(shmMovementPK, versionCheck, db2EntityManager, txnContext);
	}

	private Function<DB2ShmMovement, Boolean> getCheckVersionFunction(Timestamp exadataLstUpdtTmst) {
		return db2Entity -> db2Entity.getCrteTmst().compareTo(exadataLstUpdtTmst) <= 0;
	}

	public List<ShmMovement> findByShpInstIdsWithShmMovementExcps(List<Long> shipmentInstIds, EntityManager entityManager) {
		checkNotNull(shipmentInstIds, "shipmentInstId");

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmMovement> query = cb.createQuery(ShmMovement.class);
		final Root<ShmMovement> from = query.from(ShmMovement.class);

		final List<Predicate> predicates = new ArrayList<>();

		final Path<Long> shipmentInstIdPath = from.get(ShmMovement_.id).get(ShmMovementPK_.shpInstId);
		predicates
		.add(shipmentInstIdPath.in(shipmentInstIds));
		query.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
		EntityGraph<ShmMovement> entityGraph = entityManager.createEntityGraph(ShmMovement.class);
		entityGraph.addAttributeNodes("shmMovementExcps");
		TypedQuery<ShmMovement> typedQuery = entityManager.createQuery(query);
		typedQuery.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);
		final List<ShmMovement> records = typedQuery.getResultList();

		if (CollectionUtils.isEmpty(records))
			return new ArrayList<>();

		return records;
	}

}
