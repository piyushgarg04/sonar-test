package com.xpo.ltl.shipment.service.dao;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.jpa.QueryHints;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.dao.ShmHandlingUnitDAO;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitPK;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit_;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmHandlingUnitSubDAO extends ShmHandlingUnitDAO<ShmHandlingUnit> {

	@Inject
	private AppContext appContext;

	@Inject
    private ShmHandlingUnitMvmtSubDAO shmHandlingUnitMvmtSubDAO;

	public ShmHandlingUnit findByTrackingProNumber(String trackingProNumber,
			EntityManager entityManager) {

		checkNotNull(trackingProNumber, "trackingProNumber is required");
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<ShmHandlingUnit> cq = cb.createQuery(ShmHandlingUnit.class);
		Root<ShmHandlingUnit> from = cq.from(ShmHandlingUnit.class);

		Predicate eqTrackingPro = cb.equal(from.get(ShmHandlingUnit_.childProNbrTxt),
				trackingProNumber);

		cq.select(from).where(eqTrackingPro);

		return getSingleResultOrNull(cq, entityManager);
	}

	public List<ShmHandlingUnit> findByParentProNumber(String parentProNumber,
			EntityManager entityManager) {

		checkNotNull(parentProNumber, "parentProNumber is required");
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<ShmHandlingUnit> cq = cb.createQuery(ShmHandlingUnit.class);
		Root<ShmHandlingUnit> from = cq.from(ShmHandlingUnit.class);

		Predicate eqParentPro = cb.equal(from.get(ShmHandlingUnit_.parentProNbrTxt),
				parentProNumber);

		cq.select(from).where(eqParentPro);

		return getResultList(cq, entityManager);
	}

    /**
     * <b> DO NOT send more than 1000 parentProNumberInList</b>
     * <i>In case you need to send more that 1k proNumbers, we recommend to improve this query using parallelStream for
     * each query with less
     * than 1k pros and then aggregate/merge the result.</i>
     *
     * @param parentProNumberList
     * @param entityManager
     * @return
     */
    public List<ShmHandlingUnit> findByParentProNumberList(List<String> parentProNumberList,
        EntityManager entityManager) {

        if (CollectionUtils.isEmpty(parentProNumberList)) {
            return new ArrayList<>();
        }

        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<ShmHandlingUnit> query = builder.createQuery(ShmHandlingUnit.class);
        final Root<ShmHandlingUnit> rootEntity = query.from(ShmHandlingUnit.class);

        query
            .select(rootEntity)
            .where(rootEntity.get(ShmHandlingUnit_.parentProNbrTxt.getName()).in(parentProNumberList));
        

        TypedQuery<ShmHandlingUnit> typedQuery = entityManager.createQuery(query);
        EntityGraph<ShmHandlingUnit> entityGraph = entityManager.createEntityGraph(ShmHandlingUnit.class);
        entityGraph.addSubgraph("shmHandlingUnitMvmts");
        typedQuery.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);

        final List<ShmHandlingUnit> result = getResultList(query, entityManager);

        return result;
    }

    /**
     * <b> DO NOT send more than 1000 childProNumberInList</b>
     * <i>In case you need to send more that 1k proNumbers, we recommend to improve this query using parallelStream for
     * each query with less
     * than 1k pros and then aggregate/merge the result.</i>
     *
     * @param childProNumberList
     * @param entityManager
     * @return
     */
    public List<ShmHandlingUnit> findByChildProNumberList(Set<String> childProNumberList,
        EntityManager entityManager) {

        if (CollectionUtils.isEmpty(childProNumberList)) {
            return new ArrayList<>();
        }

        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<ShmHandlingUnit> query = builder.createQuery(ShmHandlingUnit.class);
        final Root<ShmHandlingUnit> rootEntity = query.from(ShmHandlingUnit.class);

        query
            .select(rootEntity)
            .where(rootEntity.get(ShmHandlingUnit_.childProNbrTxt.getName()).in(childProNumberList));
        final List<ShmHandlingUnit> result = getResultList(query, entityManager);

        return result;
    }

	/**
	 * Returns a list of handling units for a given parent shipment instance Id
	 *
	 * @param parentShipmentInstId
	 * @param entityManager
	 * @return
	 * @throws ServiceException
	 */
	public List<ShmHandlingUnit> findByParentShipmentInstanceId(
			final Long parentShipmentInstId,
			final EntityManager entityManager) {
		final CriteriaBuilder cb = checkNotNull(entityManager, "The EntityManager is required.").getCriteriaBuilder();
		final CriteriaQuery<ShmHandlingUnit> cq = cb.createQuery(ShmHandlingUnit.class);
		final Root<ShmHandlingUnit> from = cq.from(ShmHandlingUnit.class);

		final Path<Long> shipmentInstIdPath = from.get(ShmHandlingUnit_.id).get(ShmHandlingUnitPK_.shpInstId);
		final Path<Long> seqNbrPath = from.get(ShmHandlingUnit_.id).get(ShmHandlingUnitPK_.seqNbr);

		cq.select(from).where(cb.and(cb.equal(shipmentInstIdPath, parentShipmentInstId)))
				.orderBy(cb.asc(seqNbrPath));

		List<ShmHandlingUnit> handlingUnitList = entityManager.createQuery(cq).getResultList();
		return handlingUnitList.size() > 0 ? handlingUnitList : null;
	}

	/**
	 * Updates the DB2 entity with values from Oracle entity.
	 *
	 * @param shmHandlingUnit
	 * @param exadataLstUpdtTmst
	 * @param txnContext
	 * @param db2EntityManager
	 * @throws ServiceException
	 */
	public void updateDB2ShmHandlingUnit(ShmHandlingUnit shmHandlingUnit, Timestamp exadataLstUpdtTmst,
			TransactionContext txnContext, EntityManager db2EntityManager) throws ServiceException {

		Function<DB2ShmHandlingUnit, Boolean> checkVersionFunction =
				getCheckVersionFunction(exadataLstUpdtTmst);
		updateDB2ShmHandlingUnit(shmHandlingUnit, checkVersionFunction, db2EntityManager,
				txnContext);
		db2EntityManager.flush();
	}

	private Function<DB2ShmHandlingUnit, Boolean> getCheckVersionFunction(Timestamp exadataLstUpdtTmst) {
		return db2Entity -> db2Entity.getLstUpdtTmst().compareTo(exadataLstUpdtTmst) <= 0;
	}

    public void deleteDB2(@NotNull ShmHandlingUnitPK id, Timestamp exadataLstUpdtTmst,
        @NotNull EntityManager db2EntityManager, TransactionContext txnContext)
            throws ValidationException, NotFoundException {

        Function<DB2ShmHandlingUnit, Boolean> checkVersionFunction = getCheckVersionFunction(exadataLstUpdtTmst);

        this.deleteDB2ShmHandlingUnit(id, checkVersionFunction, db2EntityManager, txnContext);
    }

	public long getNextSeqNbrByShpInstId(final Long shpInstId, EntityManager entityManager) {
		checkNotNull(shpInstId, "shpInstId is required");

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<ShmHandlingUnit> from = cq.from(ShmHandlingUnit.class);

		Path<Long> seqNbrPath = from.get(ShmHandlingUnit_.id).get(ShmHandlingUnitPK_.seqNbr);
		Path<Long> shpInstIdPath = from.get(ShmHandlingUnit_.id).get(ShmHandlingUnitPK_.shpInstId);

		cq.select(cb.max(seqNbrPath));
		cq.where(cb.equal(shpInstIdPath, shpInstId));

		Long maxId = entityManager.createQuery(cq).getSingleResult();

		return (maxId != null) ? maxId + 1 : 1L;
	}

	/**
	 * Returns a list of handling units for a given parent shipment instance Id including the handling unit movements
	 *
	 * @param childProNumbers
	 * @param entityManager
	 * @return
	 * @throws ServiceException
	 */
	public List<ShmHandlingUnit> listByChildProNumbers(
		final List<String> childProNumbers,
		final EntityManager entityManager) {
		CriteriaBuilder criteriaBuilder = checkNotNull(entityManager, "The EntityManager is required.").getCriteriaBuilder();
		CriteriaQuery<ShmHandlingUnit> criteriaQuery = criteriaBuilder.createQuery(ShmHandlingUnit.class);
		Root<ShmHandlingUnit> shmHandlingUnitRoot = criteriaQuery.from(ShmHandlingUnit.class);
		Path<String> childProNumberPath = shmHandlingUnitRoot.get(ShmHandlingUnit_.childProNbrTxt);

		List<Predicate> predicates = Lists.partition(childProNumbers, appContext.getMaxCountForInClause()).stream()
				.map(proNbrListPartition -> childProNumberPath.in(proNbrListPartition))
				.collect(Collectors.toList());

		criteriaQuery.where(predicates.toArray(new Predicate[predicates.size()]));
		TypedQuery<ShmHandlingUnit> query = entityManager.createQuery(criteriaQuery);
		EntityGraph<ShmHandlingUnit> entityGraph = entityManager.createEntityGraph(ShmHandlingUnit.class);
		entityGraph.addSubgraph("shmHandlingUnitMvmts");
		query.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);

		return query.getResultList();
	}

	public List<ShmHandlingUnit> findByShpInstIds(List<BigDecimal> shipmentInstIds, EntityManager entityManager) {
		checkNotNull(shipmentInstIds, "shipmentInstId");

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmHandlingUnit> query = cb.createQuery(ShmHandlingUnit.class);
		final Root<ShmHandlingUnit> from = query.from(ShmHandlingUnit.class);

		final List<Predicate> predicates = new ArrayList<>();

		final Path<Long> shipmentInstIdPath = from.get(ShmHandlingUnit_.id).get(ShmHandlingUnitPK_.shpInstId);
		predicates
		.add(shipmentInstIdPath.in(shipmentInstIds));

		query.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
		final List<ShmHandlingUnit> records = entityManager.createQuery(query).getResultList();

		if (CollectionUtils.isEmpty(records))
			return new ArrayList<>();

		return records;
	}

	public List<ShmHandlingUnit> findByIds(List<ShmHandlingUnitPK> pkIds, EntityManager entityManager) {
		checkNotNull(pkIds, "shipmentInstId");

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmHandlingUnit> criteriaQuery = cb.createQuery(ShmHandlingUnit.class);
		final Root<ShmHandlingUnit> from = criteriaQuery.from(ShmHandlingUnit.class);

		final Path<ShmHandlingUnitPK> shmHandlingUnitPKPath = from.get(ShmHandlingUnit_.id);

		List<Predicate> predicates = Lists.partition(pkIds, appContext.getMaxCountForInClause()).stream()
				.map(pkIdsPartition -> shmHandlingUnitPKPath.in(pkIdsPartition))
				.collect(Collectors.toList());

		criteriaQuery.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
		TypedQuery<ShmHandlingUnit> query = entityManager.createQuery(criteriaQuery);
		EntityGraph<ShmHandlingUnit> entityGraph = entityManager.createEntityGraph(ShmHandlingUnit.class);
		entityGraph.addSubgraph("shmHandlingUnitMvmts");
		query.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);

		return query.getResultList();
	}
	
	public List<ShmHandlingUnit> getAllShmHandlingUnit(EntityManager entityManager) {

	    final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
	    final CriteriaQuery<ShmHandlingUnit> query = cb.createQuery(ShmHandlingUnit.class);
	    final Root<ShmHandlingUnit> from = query.from(ShmHandlingUnit.class);
	    
	    query.select(from);
	    final List<ShmHandlingUnit> records = entityManager.createQuery(query).getResultList();

	    if (CollectionUtils.isEmpty(records))
	        return new ArrayList<>();
	    
	    return records;
	}

	public ShmHandlingUnit updateHandlingUnitRecords(ShmHandlingUnit handlingUnitOriginal, ShmHandlingUnit handlingUnit,
			ShmHandlingUnitPK idDeleteDB2, EntityManager entityManager, EntityManager db2EntityManager, TransactionContext txnContext) throws ValidationException, NotFoundException {
			shmHandlingUnitMvmtSubDAO.remove(handlingUnitOriginal.getShmHandlingUnitMvmts(), entityManager);
            remove(handlingUnitOriginal, entityManager);
            persist(handlingUnit, entityManager);
            shmHandlingUnitMvmtSubDAO.deleteDB2ShmHandlingUnitMvmtByShmHUPK(idDeleteDB2, db2EntityManager);
            deleteDB2(idDeleteDB2, handlingUnit.getLstUpdtTmst(), db2EntityManager, txnContext);
			db2EntityManager.flush();
            this.createDB2ShmHandlingUnit(handlingUnit, db2EntityManager);
			entityManager.flush();
            return handlingUnit;
	}
}
