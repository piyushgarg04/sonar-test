package com.xpo.ltl.shipment.service.dao;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;

import com.xpo.ltl.api.shipment.service.dao.ShmAcSvcDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvcPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc_;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShipmentAcSvcSubDAO extends ShmAcSvcDAO<ShmAcSvc> {

	private static String ACCESSORIAL_SQL = "SELECT accessorials FROM ShmAcSvc accessorials WHERE accessorials.id.shpInstId in :shpIdList ";
	
	public List<ShmAcSvc> listAccessorialsForShipmentIdList(List<Long> shipmentInstIds, EntityManager entityManager) {
		//For performance reasons DO NOT change this from a native query to use criteria builder.
		TypedQuery<Tuple> acQuery = entityManager.createQuery(ACCESSORIAL_SQL, Tuple.class);
		acQuery.setParameter("shpIdList", shipmentInstIds);
		List<Tuple> acResults = acQuery.getResultList();
		List<ShmAcSvc> acList = new ArrayList<>();
		for (Tuple tuple : acResults) {
			acList.add((ShmAcSvc)tuple.get(0));
		}
		return acList;
	}

	public List<ShmAcSvc> findByShpInstIds(List<BigDecimal> shipmentInstIds, EntityManager entityManager) {
		checkNotNull(shipmentInstIds, "shipmentInstId");

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmAcSvc> query = cb.createQuery(ShmAcSvc.class);
		final Root<ShmAcSvc> from = query.from(ShmAcSvc.class);

		final List<Predicate> predicates = new ArrayList<>();

		final Path<Long> shipmentInstIdPath = from.get(ShmAcSvc_.id).get(ShmAcSvcPK_.shpInstId);
		predicates
		.add(shipmentInstIdPath.in(shipmentInstIds));

		query.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
		final List<ShmAcSvc> records = entityManager.createQuery(query).getResultList();

		if (CollectionUtils.isEmpty(records))
			return new ArrayList<>();

		return records;
	}
}