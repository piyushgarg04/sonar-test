package com.xpo.ltl.shipment.service.dao;

import com.xpo.ltl.api.shipment.service.dao.ShmAdvBydCarrDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmAdvBydCarr;
import com.xpo.ltl.api.shipment.service.entity.ShmAdvBydCarrPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmAdvBydCarr_;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import org.apache.commons.collections4.CollectionUtils;

import static com.google.common.base.Preconditions.checkNotNull;

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
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@ApplicationScoped
@LogExecutionTime
public class ShipmentAdvBydSubDAO extends ShmAdvBydCarrDAO<ShmAdvBydCarr> {

	private static final String ADVBYD_BY_SHP_ID_SQL  = "SELECT advByd FROM ShmAdvBydCarr advByd WHERE advByd.id.shpInstId in :shpIdList ";
	
	public List<ShmAdvBydCarr> listAdvBydByShipmentIdList(List<Long> shipmentInstIds, EntityManager entityManager) {
		// For performance reasons DO NOT change this from a native query to use critiera builder.
		TypedQuery<Tuple> query = entityManager.createQuery(ADVBYD_BY_SHP_ID_SQL, Tuple.class);
		query.setParameter("shpIdList", shipmentInstIds);
		List<Tuple> results = query.getResultList();
		List<ShmAdvBydCarr> advBydList = new ArrayList<>();
		for (Tuple oneResult : results) {
			ShmAdvBydCarr oneAdvByd = (ShmAdvBydCarr)oneResult.get(0);
			advBydList.add(oneAdvByd);
		}
		return advBydList;
	}

	public List<ShmAdvBydCarr> findByShpInstIds(List<Long> shipmentInstIds, EntityManager entityManager) {
		checkNotNull(shipmentInstIds, "shipmentInstId");

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmAdvBydCarr> query = cb.createQuery(ShmAdvBydCarr.class);
		final Root<ShmAdvBydCarr> from = query.from(ShmAdvBydCarr.class);

		final List<Predicate> predicates = new ArrayList<>();

		final Path<Long> shipmentInstIdPath = from.get(ShmAdvBydCarr_.id).get(ShmAdvBydCarrPK_.shpInstId);
		predicates
		.add(shipmentInstIdPath.in(shipmentInstIds));

		query.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
		final List<ShmAdvBydCarr> records = entityManager.createQuery(query).getResultList();

		if (CollectionUtils.isEmpty(records))
			return new ArrayList<>();

		return records;
	}
}