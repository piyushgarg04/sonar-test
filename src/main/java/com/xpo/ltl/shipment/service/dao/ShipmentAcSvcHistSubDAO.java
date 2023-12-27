package com.xpo.ltl.shipment.service.dao;

import com.xpo.ltl.api.shipment.service.dao.ShmAcSvcHistDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvcHist;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvcHistPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmAcSvcHist_;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import org.apache.commons.collections4.CollectionUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@ApplicationScoped
public class ShipmentAcSvcHistSubDAO extends ShmAcSvcHistDAO<ShmAcSvcHist> {

	private static final String ACCESSORIAL_SQL = "SELECT accessorials FROM ShmAcSvcHist accessorials WHERE accessorials.id.shpInstId = :shipmentInstId ";

	@LogExecutionTime
	public List<ShmAcSvcHist> getAccessorialsForShipmentId(Long shipmentInstId, EntityManager entityManager) {
		//For performance reasons DO NOT change this from a native query to use criteria builder.
		TypedQuery<Tuple> acQuery = entityManager.createQuery(ACCESSORIAL_SQL, Tuple.class);
		acQuery.setParameter("shipmentInstId", shipmentInstId);
		List<Tuple> acResults = acQuery.getResultList();
		List<ShmAcSvcHist> acList = new ArrayList<>();
		for (Tuple tuple : acResults) {
			acList.add((ShmAcSvcHist)tuple.get(0));
		}
		return acList;
	}

	public List<ShmAcSvcHist> findByShpInstIds(List<BigDecimal> shipmentInstIds, EntityManager entityManager) {
		checkNotNull(shipmentInstIds, "shipmentInstId");

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmAcSvcHist> query = cb.createQuery(ShmAcSvcHist.class);
		final Root<ShmAcSvcHist> from = query.from(ShmAcSvcHist.class);

		final List<Predicate> predicates = new ArrayList<>();

		final Path<Long> shipmentInstIdPath = from.get(ShmAcSvcHist_.id).get(ShmAcSvcHistPK_.shpInstId);
		predicates
		.add(shipmentInstIdPath.in(shipmentInstIds));

		query.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
		final List<ShmAcSvcHist> records = entityManager.createQuery(query).getResultList();

		if (CollectionUtils.isEmpty(records))
			return new ArrayList<>();

		return records;
	}
}