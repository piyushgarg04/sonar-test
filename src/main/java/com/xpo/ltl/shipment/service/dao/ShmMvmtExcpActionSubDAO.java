package com.xpo.ltl.shipment.service.dao;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.xpo.ltl.api.shipment.service.dao.ShmMvmtExcpActionDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmMvmtExcpAction;
import com.xpo.ltl.api.shipment.service.entity.ShmMvmtExcpActionPK;
import com.xpo.ltl.api.shipment.service.entity.ShmMvmtExcpActionPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmMvmtExcpAction_;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmMvmtExcpActionSubDAO extends ShmMvmtExcpActionDAO<ShmMvmtExcpAction> {

	public long findMaxSeqNbr(final ShmMvmtExcpActionPK actionId, final EntityManager entityManager) {
		final CriteriaBuilder cb = checkNotNull(entityManager, "The EntityManager is required.").getCriteriaBuilder();
		final CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		final Root<ShmMvmtExcpAction> from = cq.from(ShmMvmtExcpAction.class);

		final List<Predicate> predicates = new ArrayList<>();

		predicates.add(
			cb.equal(from.get(ShmMvmtExcpAction_.id).get(ShmMvmtExcpActionPK_.shpInstId), actionId.getShpInstId()));
		predicates.add(
			cb.equal(from.get(ShmMvmtExcpAction_.id).get(ShmMvmtExcpActionPK_.mvmtSeqNbr), actionId.getMvmtSeqNbr()));
		predicates.add(
			cb.equal(
				from.get(ShmMvmtExcpAction_.id).get(ShmMvmtExcpActionPK_.mvmtExcpSeqNbr),
				actionId.getMvmtExcpSeqNbr()));

		cq.select(cb.max(from.get(ShmMvmtExcpAction_.id).get(ShmMvmtExcpActionPK_.seqNbr))).where(
			predicates.toArray(new Predicate[predicates.size()]));

		final Long sequenceNumber = entityManager.createQuery(cq).getSingleResult();
		if (sequenceNumber != null) {
			return sequenceNumber;
		}

		return 0l;
	}

	public List<ShmMvmtExcpAction> findByShpInstIds(List<Long> shipmentInstIds, EntityManager entityManager) {
		checkNotNull(shipmentInstIds, "shipmentInstId");

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmMvmtExcpAction> query = cb.createQuery(ShmMvmtExcpAction.class);
		final Root<ShmMvmtExcpAction> from = query.from(ShmMvmtExcpAction.class);

		final List<Predicate> predicates = new ArrayList<>();

		final Path<Long> shipmentInstIdPath = from.get(ShmMvmtExcpAction_.id).get(ShmMvmtExcpActionPK_.shpInstId);
		predicates
				.add(shipmentInstIdPath.in(shipmentInstIds));

		query.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
		return entityManager.createQuery(query).getResultList();
	}

}
