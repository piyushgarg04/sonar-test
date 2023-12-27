package com.xpo.ltl.shipment.service.dao;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;

import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.dao.ShmCustomsBondDAO;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmCustomsBond;
import com.xpo.ltl.api.shipment.service.entity.ShmCustomsBond;
import com.xpo.ltl.api.shipment.service.entity.ShmCustomsBondPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmCustomsBond_;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmCustomsBondSubDAO extends ShmCustomsBondDAO<ShmCustomsBond> {

	public List<ShmCustomsBond> findByShpInstIds(List<Long> shipmentInstIds, EntityManager entityManager) {
		checkNotNull(shipmentInstIds, "shipmentInstId");

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmCustomsBond> query = cb.createQuery(ShmCustomsBond.class);
		final Root<ShmCustomsBond> from = query.from(ShmCustomsBond.class);

		final List<Predicate> predicates = new ArrayList<>();

		final Path<Long> shipmentInstIdPath = from.get(ShmCustomsBond_.id).get(ShmCustomsBondPK_.shpInstId);
		predicates
		.add(shipmentInstIdPath.in(shipmentInstIds));

		query.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
		final List<ShmCustomsBond> records = entityManager.createQuery(query).getResultList();

		if (CollectionUtils.isEmpty(records))
			return new ArrayList<>();

		return records;
	}


	public void updateDB2ShmCustomsBond(final ShmCustomsBond record, final EntityManager db2EntityManager, TransactionContext txnContext) 
		throws  ValidationException, NotFoundException {
			final Function<DB2ShmCustomsBond, Boolean> checkVersionFunction = getCheckVersionFunction(record.getLstUpdtTmst());
			updateDB2ShmCustomsBond(record, checkVersionFunction, db2EntityManager, txnContext);
			db2EntityManager.flush();
	}

	private Function<DB2ShmCustomsBond, Boolean> getCheckVersionFunction(final Timestamp exadataLstUpdtTmst) {
		return db2Entity -> db2Entity.getLstUpdtTmst().compareTo(exadataLstUpdtTmst) <= 0;
	}
}
