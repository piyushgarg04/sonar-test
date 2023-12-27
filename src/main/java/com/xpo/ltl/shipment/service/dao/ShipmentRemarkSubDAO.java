package com.xpo.ltl.shipment.service.dao;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.dao.ShmRemarkDAO;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmRemarkPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmRemark_;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment_;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShipmentRemarkSubDAO extends ShmRemarkDAO<ShmRemark> {

	private static String REMARKS_BY_SHP_ID_SQL  = "SELECT remarks FROM ShmRemark remarks WHERE remarks.id.shpInstId in :shpIdList ";

	/**
	 * Return all Remarks for the shipment using either a shipment instance id, or a pro number and pickup date
	 * @param shipmentInstanceId
	 * @param proNumber
	 * @param pickupDate
	 * @param entityManager
	 * @return
	 */
	public ShmShipment getShipmentRemarks(long shipmentInstanceId, String proNumber, Date pickupDate, EntityManager entityManager) {
		
		final CriteriaBuilder cb = checkNotNull(entityManager, "The EntityManager is required.").getCriteriaBuilder();
		final CriteriaQuery<ShmShipment> cq = cb.createQuery(ShmShipment.class);
		final Root<ShmShipment> from = cq.from(ShmShipment.class);
		from.fetch(ShmShipment_.shmRemarks);

		final List<Predicate> predicates = new ArrayList<>();
		if (shipmentInstanceId > 0) {
			final Expression<Long> shmShpInstIdPath = from.get(ShmShipment_.shpInstId);
			final Predicate shpInstIdPred = cb.equal(shmShpInstIdPath, shipmentInstanceId);
			predicates.add(shpInstIdPred);
		}else {
			final Expression<String> shmProNbrPath = from.get(ShmShipment_.proNbrTxt);
			final Expression<Date> shmPickupDatePath = from.get(ShmShipment_.pkupDt);
			final Predicate proNbrPred = cb.equal(shmProNbrPath, proNumber);
			final Predicate pickupDatePred = cb.equal(shmPickupDatePath, pickupDate);
			predicates.add(proNbrPred);
			predicates.add(pickupDatePred);
		} 
		
		cq.select(from).where(predicates.toArray(new Predicate[CollectionUtils.size(predicates)]));
		List<ShmShipment> shmShipmentList = entityManager.createQuery(cq).getResultList(); 
		return (shmShipmentList.size() > 0 ? shmShipmentList.get(0) : null);
	}

	public List<ShmRemark> listShpRemarksShipmentIdList(List<Long> shipmentInstIds, EntityManager entityManager) {
		// For performance reasons DO NOT change this from a native query to use critiera builder.
		TypedQuery<Tuple> query = entityManager.createQuery(REMARKS_BY_SHP_ID_SQL, Tuple.class);
		query.setParameter("shpIdList", shipmentInstIds);
		List<Tuple> results = query.getResultList();
		List<ShmRemark> remarksList = new ArrayList<>();
		for (Tuple oneResult : results) {
			ShmRemark oneRemark = (ShmRemark)oneResult.get(0);
			remarksList.add(oneRemark);
		}
		return remarksList;
	}

    /**
     * Updates the DB2 entity with values from Oracle entity.
     *
     * @param shmRemark
     * @param exadataLstUpdtTmst
     * @param txnContext
     * @param db2EntityManager
     * @throws ServiceException
     */
    public void updateDB2ShmRemark(final ShmRemark shmRemark, final Timestamp exadataLstUpdtTmst,
        final TransactionContext txnContext, final EntityManager db2EntityManager) throws ServiceException {
        final Function<DB2ShmRemark, Boolean> checkVersionFunction = getCheckVersionFunction(exadataLstUpdtTmst);
        updateDB2ShmRemark(shmRemark, checkVersionFunction, db2EntityManager, txnContext);
    }

    private Function<DB2ShmRemark, Boolean> getCheckVersionFunction(Timestamp exadataLstUpdtTmst) {
        return db2Entity -> db2Entity.getLstUpdtTmst().compareTo(exadataLstUpdtTmst) <= 0;
    }

	public List<ShmRemark> findByShpInstIds(List<Long> shipmentInstIds, EntityManager entityManager) {
		checkNotNull(shipmentInstIds, "shipmentInstId");

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmRemark> query = cb.createQuery(ShmRemark.class);
		final Root<ShmRemark> from = query.from(ShmRemark.class);

		final List<Predicate> predicates = new ArrayList<>();

		final Path<Long> shipmentInstIdPath = from.get(ShmRemark_.id).get(ShmRemarkPK_.shpInstId);
		predicates
		.add(shipmentInstIdPath.in(shipmentInstIds));

		query.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
		final List<ShmRemark> records = entityManager.createQuery(query).getResultList();

		if (CollectionUtils.isEmpty(records))
			return new ArrayList<>();

		return records;
	}
}
