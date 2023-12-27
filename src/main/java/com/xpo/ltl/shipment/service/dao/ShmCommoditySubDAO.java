package com.xpo.ltl.shipment.service.dao;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;

import com.xpo.ltl.api.shipment.service.dao.ShmCommodityDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmCommodity;
import com.xpo.ltl.api.shipment.service.entity.ShmCommodityPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmCommodity_;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmCommoditySubDAO extends ShmCommodityDAO<ShmCommodity> {

	public List<ShmCommodity> findByShpInstIds(List<BigDecimal> shipmentInstIds, EntityManager entityManager) {
		checkNotNull(shipmentInstIds, "shipmentInstId");

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmCommodity> query = cb.createQuery(ShmCommodity.class);
		final Root<ShmCommodity> from = query.from(ShmCommodity.class);

		final List<Predicate> predicates = new ArrayList<>();

		final Path<Long> shipmentInstIdPath = from.get(ShmCommodity_.id).get(ShmCommodityPK_.shpInstId);
		predicates
		.add(shipmentInstIdPath.in(shipmentInstIds));

		query.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
		final List<ShmCommodity> records = entityManager.createQuery(query).getResultList();

		if (CollectionUtils.isEmpty(records))
			return new ArrayList<>();

		return records;
	}
}
