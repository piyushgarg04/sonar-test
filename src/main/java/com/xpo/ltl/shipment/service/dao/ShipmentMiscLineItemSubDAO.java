package com.xpo.ltl.shipment.service.dao;

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

import org.apache.commons.collections4.CollectionUtils;

import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItemPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem_;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShipmentMiscLineItemSubDAO {

	private static String MISClINE_ITEM_BY_SHP_ID_SQL  = "SELECT miscLineItem FROM ShmMiscLineItem miscLineItem WHERE miscLineItem.id.shpInstId in :shpIdList ";
	
	public List<ShmMiscLineItem> listMiscLineItemByShipmentIdList(List<Long> shipmentInstIds, EntityManager entityManager) {
		// For performance reasons DO NOT change this from a native query to use critiera builder.
		TypedQuery<Tuple> query = entityManager.createQuery(MISClINE_ITEM_BY_SHP_ID_SQL, Tuple.class);
		query.setParameter("shpIdList", shipmentInstIds);
		List<Tuple> results = query.getResultList();
		List<ShmMiscLineItem> miscLineItemList = new ArrayList<>();
		for (Tuple oneResult : results) {
			ShmMiscLineItem oneMiscLineItem = (ShmMiscLineItem)oneResult.get(0);
			miscLineItemList.add(oneMiscLineItem);
		}
		return miscLineItemList;
	}

	public List<ShmMiscLineItem> findByShpInstIds(List<Long> shipmentInstIds, EntityManager entityManager) {
		checkNotNull(shipmentInstIds, "shipmentInstId");

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmMiscLineItem> query = cb.createQuery(ShmMiscLineItem.class);
		final Root<ShmMiscLineItem> from = query.from(ShmMiscLineItem.class);

		final List<Predicate> predicates = new ArrayList<>();

		final Path<Long> shipmentInstIdPath = from.get(ShmMiscLineItem_.id).get(ShmMiscLineItemPK_.shpInstId);
		predicates
		.add(shipmentInstIdPath.in(shipmentInstIds));

		query.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
		final List<ShmMiscLineItem> records = entityManager.createQuery(query).getResultList();

		if (CollectionUtils.isEmpty(records))
			return new ArrayList<>();

		return records;
	}

}