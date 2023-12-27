package com.xpo.ltl.shipment.service.dao;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.dao.ShmAsEntdCustDAO;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmAsEntdCust;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCust;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCustPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCust_;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment_;
import com.xpo.ltl.shipment.service.dto.ShipmentCntMonthDTO;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShipmentAsEnteredCustomerDAO extends ShmAsEntdCustDAO<ShmAsEntdCust> {

	private static String AS_ENTD_CUST_SQL = "SELECT parties FROM ShmAsEntdCust parties WHERE parties.id.shpInstId in :shpIdList ";

	/**
	 * Returns all the parties on a shipment using either a shipment instance id, or a pro number and pickup date.
	 * @param shipmentInstanceId
	 * @param proNumber
	 * @param pickupDate
	 * @param entityManager
	 * @return
	 */
	public ShmShipment getShipmentBillParties(long shipmentInstanceId, String proNumber, Date pickupDate, EntityManager entityManager) {

		final CriteriaBuilder cb = checkNotNull(entityManager, "The EntityManager is required.").getCriteriaBuilder();
		final CriteriaQuery<ShmShipment> cq = cb.createQuery(ShmShipment.class);
		final Root<ShmShipment> from = cq.from(ShmShipment.class);
		from.fetch(ShmShipment_.shmAsEntdCusts);

		final List<Predicate> predicates = new ArrayList<>();
		if (shipmentInstanceId > 0) {
			final Expression<Long> shmShpInstIdPath = from.get(ShmShipment_.shpInstId);
			final Predicate shpInstIdPred = cb.equal(shmShpInstIdPath, shipmentInstanceId);
			predicates.add(shpInstIdPred);
		} else {
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

	public List<ShmAsEntdCust> listAsEntdCustByShpId(List<Long> shipmentInstIds, EntityManager entityManager) {
		// For performance reasons, leave this as a native query and do not use criteria builder
		TypedQuery<Tuple> partyQuery = entityManager.createQuery(AS_ENTD_CUST_SQL, Tuple.class);
		partyQuery.setParameter("shpIdList", shipmentInstIds);
		List<Tuple> partyResults = partyQuery.getResultList();
		List<ShmAsEntdCust> partiesList = new ArrayList<>();
		for (Tuple tuple : partyResults) {
			partiesList.add((ShmAsEntdCust)tuple.get(0));
		}
		return partiesList;
	}

	/**
     * this class 'for the most part' was copied from
     * RAD as part of ticket PCT-2511. (old comment).
     *
     * <p>
     * This method will return a list of CNT, YEAR, MONTH group by YEAR, MONTH. for the latest 12 month filter by customer.
     * </p>
     * <p>
     * <b>IMPORTANT:</b> if a year-month pair has 0 shipments, it will not return that row.
     * </p>
     *
     * @param proNumber
     * @param pickupDate
     * @param shipInstanceId
     * @param entityManager
     * @return
     */
    public List<ShipmentCntMonthDTO> getShipmentCountByShipperCustNbr(
			final Integer shipperCustNbr,
        final Date pkupDateFrom, final Date pkupDateTo,
			final EntityManager entityManager){

		checkNotNull(shipperCustNbr, "The shipperCustNbr is required.");

		final CriteriaBuilder cb = checkNotNull(entityManager, "The EntityManager is required.").getCriteriaBuilder();

        final CriteriaQuery<ShipmentCntMonthDTO> criteria = cb.createQuery(ShipmentCntMonthDTO.class);
		final Root<ShmShipment> shmShipment = criteria.from(ShmShipment.class);
		final Join<ShmShipment, ShmAsEntdCust> shmAsEntdCust = shmShipment.join(ShmShipment_.shmAsEntdCusts, JoinType.INNER);

		final Path<Date> pkupDtPath = shmShipment.get(ShmShipment_.pkupDt);
		final Path<String> typCdPath = shmAsEntdCust.get(ShmAsEntdCust_.typCd);
		final Path<BigDecimal> custNbrPath = shmAsEntdCust.get(ShmAsEntdCust_.cisCustNbr);


        // restrictions
		final List<Predicate> predicates = new ArrayList<Predicate>();
		predicates.add(cb.between(pkupDtPath,
            pkupDateFrom, pkupDateTo));
		predicates.add(cb.equal(custNbrPath, shipperCustNbr));
		predicates.add(cb.equal(typCdPath, "1"));

        Expression<Integer> yearFn = cb.function("year", Integer.class, shmShipment.get(ShmShipment_.pkupDt));
        Expression<Integer> monthFn = cb.function("month", Integer.class, shmShipment.get(ShmShipment_.pkupDt));

        criteria.select(cb.construct(ShipmentCntMonthDTO.class, cb.count(pkupDtPath), yearFn, monthFn));
		criteria.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        criteria.groupBy(yearFn, monthFn);
        criteria.orderBy(cb.desc(yearFn), cb.desc(monthFn));

        try {
            final List<ShipmentCntMonthDTO> dtoList = entityManager.createQuery(criteria).getResultList();
            return dtoList;
        } catch (final NoResultException nre) {
            return Collections.emptyList();
        }

    }

    public ShmAsEntdCust findByShipmentIdAndTypeCd(long shipmentInstanceId, String typeCd,
        final EntityManager entityManager) {

        final CriteriaBuilder cb = checkNotNull(entityManager, "The EntityManager is required.").getCriteriaBuilder();
        final CriteriaQuery<ShmAsEntdCust> cq = cb.createQuery(ShmAsEntdCust.class);
        final Root<ShmAsEntdCust> from = cq.from(ShmAsEntdCust.class);
        final Join<ShmAsEntdCust, ShmShipment> shmShipmentJoin = from.join(ShmAsEntdCust_.shmShipment, JoinType.INNER);

        final List<Predicate> predicates = new ArrayList<>();
        final Expression<Long> shmShpInstIdPath = shmShipmentJoin.get(ShmShipment_.shpInstId);
        final Expression<String> shmAsEntdCustTypePath = from.get(ShmAsEntdCust_.typCd);

        final Predicate shpInstIdPred = cb.equal(shmShpInstIdPath, shipmentInstanceId);
        predicates.add(shpInstIdPred);

        final Predicate typePred = cb.equal(shmAsEntdCustTypePath, typeCd);
        predicates.add(typePred);

        cq.select(from).where(predicates.toArray(new Predicate[CollectionUtils.size(predicates)]));
        List<ShmAsEntdCust> shmAsEntdCustList = entityManager.createQuery(cq).getResultList();
        return (shmAsEntdCustList.size() > 0 ? shmAsEntdCustList.get(0) : null);
    }

    public long getNextSeqNbrByShpInstId(Long shipmentInstId, EntityManager entityManager) {

        checkNotNull(shipmentInstId, "shipmentInstId is required");

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<ShmAsEntdCust> from = cq.from(ShmAsEntdCust.class);

        Path<Long> seqNbrPath = from.get(ShmAsEntdCust_.id).get(ShmAsEntdCustPK_.seqNbr);
        Path<Long> shipmentInstIdPath = from.get(ShmAsEntdCust_.id).get(ShmAsEntdCustPK_.shpInstId);

        cq.select(cb.max(seqNbrPath));
        cq.where(cb.equal(shipmentInstIdPath, shipmentInstId));

        Long maxId = entityManager.createQuery(cq).getSingleResult();

        return (maxId != null) ? maxId + 1 : 1L;
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
    public void updateDB2ShmAsEnteredCustomer(final ShmAsEntdCust shmAsEntdCust, final Timestamp exadataLstUpdtTmst,
        final TransactionContext txnContext, final EntityManager db2EntityManager) throws ServiceException {
        final Function<DB2ShmAsEntdCust, Boolean> checkVersionFunction = getCheckVersionFunction(exadataLstUpdtTmst);
        updateDB2ShmAsEntdCust(shmAsEntdCust, checkVersionFunction, db2EntityManager, txnContext);
    }

    private Function<DB2ShmAsEntdCust, Boolean> getCheckVersionFunction(Timestamp exadataLstUpdtTmst) {
        return db2Entity -> db2Entity.getLstUpdtTmst().compareTo(exadataLstUpdtTmst) <= 0;
    }

	public List<ShmAsEntdCust> findByShpInstIds(List<Long> shipmentInstIds, EntityManager entityManager) {
		checkNotNull(shipmentInstIds, "shipmentInstId");

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmAsEntdCust> query = cb.createQuery(ShmAsEntdCust.class);
		final Root<ShmAsEntdCust> from = query.from(ShmAsEntdCust.class);

		final List<Predicate> predicates = new ArrayList<>();

		final Path<Long> shipmentInstIdPath = from.get(ShmAsEntdCust_.id).get(ShmAsEntdCustPK_.shpInstId);
		predicates
		.add(shipmentInstIdPath.in(shipmentInstIds));

		query.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
		final List<ShmAsEntdCust> records = entityManager.createQuery(query).getResultList();

		if (CollectionUtils.isEmpty(records))
			return new ArrayList<>();

		return records;

	}
}
