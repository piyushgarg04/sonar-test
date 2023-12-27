package com.xpo.ltl.shipment.service.dao;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.shipment.service.dao.ShmMovementExcpDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcpPK;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcpPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcp_;
import com.xpo.ltl.api.shipment.transformer.v2.MovementExceptionTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.MovementExceptionTypeCd;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShipmentMovementExceptionSubDAO extends ShmMovementExcpDAO<ShmMovementExcp>{

	private static final String SHORT_EXCEPTION_TYPE = MovementExceptionTypeCdTransformer.toCode(MovementExceptionTypeCd.SHORT);
	private static final String DELIVERY_TYPE_CD = "7";

	@Inject
	private AppContext appContext;

	private static final String GET_VALID_EXCEPTIONS_BY_PARENTS_QUERY = "SELECT shmMovExcp.*" +
			"FROM SHM_MOVEMENT_EXCP shmMovExcp " +
			"         INNER JOIN SHM_SHIPMENT shm ON shm.shp_inst_id = shmMovExcp.shp_inst_id "+
			"         INNER JOIN SHM_MOVEMENT shmMov ON shmMovExcp.mvmt_seq_nbr = shmMov.seq_nbr AND shmMovExcp.shp_inst_id = shmMov.shp_inst_id " +
			"         INNER JOIN ( " +
			"  					  SELECT sme.shp_inst_id, MAX(sme.crte_tmst) AS maxCrteTmst " +
			"   				  FROM SHM_MOVEMENT_EXCP sme " +
			"    				  WHERE sme.shp_inst_id IN (:shipmentIds) " +
			"                     GROUP BY sme.shp_inst_id ) subQuery " +
			"		ON shmMovExcp.crte_tmst = subQuery.maxCrteTmst  AND shmMovExcp.shp_inst_id = subQuery.shp_inst_id " +
			" WHERE " +
			"      shmMovExcp.typ_cd = :excpTypeCd " +
			"  AND shmMov.typ_cd = :moveTypeCd " +
			"  AND shm.dlvry_qalfr_cd = shmMov.dlvry_qalfr_cd  " +
			"  AND shmMovExcp.shp_inst_id IN (:shipmentIds)";

	public Long findMaxSeqNbr(ShmMovementExcpPK id, EntityManager entityManager) {
		final CriteriaBuilder cb = checkNotNull(entityManager, "The EntityManager is required.").getCriteriaBuilder();
		final CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		final Root<ShmMovementExcp> from = cq.from(ShmMovementExcp.class);

		final List<Predicate> predicates = new ArrayList<>();

		predicates.add(
			cb.equal(from.get(ShmMovementExcp_.id).get(ShmMovementExcpPK_.shpInstId), id.getShpInstId()));
		predicates.add(
			cb.equal(from.get(ShmMovementExcp_.id).get(ShmMovementExcpPK_.mvmtSeqNbr), id.getMvmtSeqNbr()));

		cq.select(cb.max(from.get(ShmMovementExcp_.id).get(ShmMovementExcpPK_.seqNbr))).where(
			predicates.toArray(new Predicate[predicates.size()]));

		final Long sequenceNumber = entityManager.createQuery(cq).getSingleResult();
		if (sequenceNumber != null) {
			return sequenceNumber;
		}

		return 0l;
	}

	public List<ShmMovementExcp> findByShpInstIdForShortType(List<Long> shpInstIds, EntityManager entityManager) {

		List<ShmMovementExcp>  result = new ArrayList<>();
		for (List<Long> partitionedList : Lists.partition(shpInstIds, appContext.getMaxCountForInClause())) {
			Query query = entityManager.createNativeQuery(GET_VALID_EXCEPTIONS_BY_PARENTS_QUERY, ShmMovementExcp.class);

			query.setParameter("moveTypeCd", DELIVERY_TYPE_CD);
			query.setParameter("excpTypeCd", SHORT_EXCEPTION_TYPE);
			query.setParameter("shipmentIds", partitionedList);
			result.addAll(query.getResultList());
		}

		return result;
	}

	
	public List<ShmMovementExcp> findByShpInstIds(List<Long> shipmentInstIds, EntityManager entityManager) {
		checkNotNull(shipmentInstIds, "shipmentInstId");

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmMovementExcp> query = cb.createQuery(ShmMovementExcp.class);
		final Root<ShmMovementExcp> from = query.from(ShmMovementExcp.class);

		final List<Predicate> predicates = new ArrayList<>();

		final Path<Long> shipmentInstIdPath = from.get(ShmMovementExcp_.id).get(ShmMovementExcpPK_.shpInstId);
		predicates
		.add(shipmentInstIdPath.in(shipmentInstIds));

		query.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
		final List<ShmMovementExcp> records = entityManager.createQuery(query).getResultList();

		if (CollectionUtils.isEmpty(records))
			return new ArrayList<>();

		return records;
	}


}
