package com.xpo.ltl.shipment.service.dao;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

import com.xpo.ltl.api.shipment.service.dao.ShmSrNbrDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmSrNbr;
import com.xpo.ltl.api.shipment.service.entity.ShmSrNbrPK;
import com.xpo.ltl.api.shipment.service.entity.ShmSrNbrPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmSrNbr_;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShipmentSupRefSubDAO extends ShmSrNbrDAO<ShmSrNbr> {

	private static String SUPREF_SQL = "SELECT supRefNbrs FROM ShmSrNbr supRefNbrs WHERE supRefNbrs.id.shpInstId in :shpIdList ";
	
	public List<ShmSrNbr> listSupRefNumbersForShipmentIdList(List<Long> shipmentInstIds, EntityManager entityManager) {
		//For performance reasons DO NOT change this from a native query to use criteria builder.
		TypedQuery<Tuple> query = entityManager.createQuery(SUPREF_SQL, Tuple.class);
		query.setParameter("shpIdList", shipmentInstIds);
		List<Tuple> results = query.getResultList();
		List<ShmSrNbr> supRefList = new ArrayList<>();
		for (Tuple tuple : results) {
			supRefList.add((ShmSrNbr)tuple.get(0));
		}
		return supRefList;
	}

	public List<ShmSrNbr> findByShpInstIds(List<BigDecimal> shipmentInstIds, EntityManager entityManager) {
		checkNotNull(shipmentInstIds, "shipmentInstId");

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmSrNbr> query = cb.createQuery(ShmSrNbr.class);
		final Root<ShmSrNbr> from = query.from(ShmSrNbr.class);

		final List<Predicate> predicates = new ArrayList<>();

		final Path<Long> shipmentInstIdPath = from.get(ShmSrNbr_.id).get(ShmSrNbrPK_.shpInstId);
		predicates
		.add(shipmentInstIdPath.in(shipmentInstIds));

		query.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
		final List<ShmSrNbr> records = entityManager.createQuery(query).getResultList();

		if (CollectionUtils.isEmpty(records))
			return new ArrayList<>();

		return records;
	}
	
	public List<ShmSrNbr> findByShpInstIdAndShmSrNbrTypCds(
		final Long shipmentInstId,
		final List<String> shmSrNbrTypCds,
		EntityManager entityManager) {

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Tuple> query = cb.createQuery(Tuple.class);
		final Root<ShmSrNbr> from = query.from(ShmSrNbr.class);

		final List<Predicate> predicates = new ArrayList<>();

		if (shipmentInstId != null) {
			predicates.add(cb.equal(from.get(ShmSrNbr_.id).get(ShmSrNbrPK_.shpInstId), shipmentInstId));
		}
		if (CollectionUtils.isNotEmpty(shmSrNbrTypCds)) {
			predicates.add(from.get(ShmSrNbr_.id).get(ShmSrNbrPK_.typCd).in(shmSrNbrTypCds));
		}

		query
			.multiselect(
				from.get(ShmSrNbr_.id).get(ShmSrNbrPK_.shpInstId).alias(ShmSrNbrPK_.shpInstId.getName()),
				from.get(ShmSrNbr_.id).get(ShmSrNbrPK_.seqNbr).alias(ShmSrNbrPK_.seqNbr.getName()),
				from.get(ShmSrNbr_.id).get(ShmSrNbrPK_.typCd).alias(ShmSrNbrPK_.typCd.getName()),
				from.get(ShmSrNbr_.nbrTxt).alias(ShmSrNbr_.nbrTxt.getName()))
			.where(predicates.toArray(new Predicate[predicates.size()]));
		final List<ShmSrNbr> records = entityManager
			.createQuery(query)
			.getResultStream()
			.map(this::buildShmSrNbrForNoShipmentDetailCd)
			.collect(Collectors.toList());

		if (CollectionUtils.isEmpty(records)) {
			return new ArrayList<>();
		}

		return records;
	}

	private ShmSrNbr buildShmSrNbrForNoShipmentDetailCd(Tuple srNbrTuple) {
		final ShmSrNbr srNbr = new ShmSrNbr();
		final ShmSrNbrPK pk = new ShmSrNbrPK();
		pk.setShpInstId(srNbrTuple.get(ShmSrNbrPK_.shpInstId.getName(), Long.class));
		pk.setSeqNbr(srNbrTuple.get(ShmSrNbrPK_.seqNbr.getName(), Long.class));
		pk.setTypCd(srNbrTuple.get(ShmSrNbrPK_.typCd.getName(), String.class));
		srNbr.setId(pk);
		srNbr.setNbrTxt(srNbrTuple.get(ShmSrNbr_.nbrTxt.getName(), String.class));

		return srNbr;
	}
}