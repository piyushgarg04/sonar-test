package com.xpo.ltl.shipment.service.dao;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;

import com.google.common.collect.Sets;
import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.dao.ShmNotificationDAO;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmNotification;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmNotificationPK_;
import com.xpo.ltl.api.shipment.service.db2.entity.DB2ShmNotification_;
import com.xpo.ltl.api.shipment.service.entity.ShmNotification;
import com.xpo.ltl.api.shipment.service.entity.ShmNotificationPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmNotification_;
import com.xpo.ltl.shipment.service.enums.ShipmentAppointmentStatusEnum;
import com.xpo.ltl.shipment.service.enums.ShmNotificationCatgCdEnum;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmNotificationSubDAO extends ShmNotificationDAO<ShmNotification> {

    private static final Set<String> APPT_STATUSES = 
            Sets.newHashSet(
                ShipmentAppointmentStatusEnum.APPOINTMENT_SET.getCode(), 
                ShipmentAppointmentStatusEnum.PENDING.getCode(),
                ShipmentAppointmentStatusEnum.CANCELLED.getCode(),
                ShipmentAppointmentStatusEnum.DELIVERY_COMPLETED.getCode(),
                ShipmentAppointmentStatusEnum.RESCHEDULE_REQD.getCode(),
                ShipmentAppointmentStatusEnum.APPOINTMENT_NOT_REQD.getCode(),
                ShipmentAppointmentStatusEnum.RESCHEDULED.getCode());

    private static final Set<String> CATG_CODES = 
            Sets.newHashSet(
                ShmNotificationCatgCdEnum.APPOINTMENT.getCode(), 
                ShmNotificationCatgCdEnum.NOTIFICATION.getCode());

	public List<ShmNotification> findByShipmentInstId(Long shipmentInstId, EntityManager entityManager) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<ShmNotification> query = cb.createQuery(ShmNotification.class);
		Root<ShmNotification> from = query.from(ShmNotification.class);

		final Expression<Long> shipmentInstIdPath = from.get(ShmNotification_.id).get(ShmNotificationPK_.shpInstId);
		final Path<Long> seqNbrPath = from.get(ShmNotification_.id).get(ShmNotificationPK_.ntfictnSeqNbr);

		query.select(from).where(cb.equal(shipmentInstIdPath, shipmentInstId)).orderBy(cb.desc(seqNbrPath));

		List<ShmNotification> result = entityManager.createQuery(query).getResultList();
		return result;
	}
	
	public List<ShmNotification> listNotificationsForShipmentIdList(List<Long> shipmentInstIds,
			EntityManager entityManager) {
		if (CollectionUtils.isEmpty(shipmentInstIds))
			return Collections.emptyList();
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<ShmNotification> criteriaQuery = criteriaBuilder.createQuery(ShmNotification.class);
		Root<ShmNotification> shmNotificationRoot = criteriaQuery.from(ShmNotification.class);
		Path<Long> shipmentInstIdPath = shmNotificationRoot.get(ShmNotification_.id).get(ShmNotificationPK_.shpInstId);

		criteriaQuery.select(shmNotificationRoot).where(shipmentInstIdPath.in(shipmentInstIds));

		TypedQuery<ShmNotification> typedQuery = entityManager.createQuery(criteriaQuery);

		return typedQuery.getResultList();
	}
	
	public ShmNotification getMostRecentNotification(Long shipmentInstId, EntityManager entityManager) {
	    
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ShmNotification> query = cb.createQuery(ShmNotification.class);
        Root<ShmNotification> from = query.from(ShmNotification.class);

        Expression<Long> shipmentInstIdPath = from.get(ShmNotification_.id).get(ShmNotificationPK_.shpInstId);
        Expression<String> statusCdPath = from.get(ShmNotification_.statCd);
        Expression<String> catgCdPath = from.get(ShmNotification_.catgCd);
        Expression<Timestamp> lstUpdtTmstPath = from.get(ShmNotification_.lstUpdtTmst);
        
        List<Predicate> predicates = new ArrayList<>();
        Predicate shipmentInstIdPred = cb.equal(shipmentInstIdPath, shipmentInstId);
        predicates.add(shipmentInstIdPred);
        predicates.add(catgCdPath.in(CATG_CODES));
        predicates.add(statusCdPath.in(APPT_STATUSES));

        query.select(from).where(predicates.toArray(new Predicate[CollectionUtils.size(predicates)])).orderBy(cb.desc(lstUpdtTmstPath));
        List<ShmNotification> results = entityManager.createQuery(query).getResultList();
        
        return (CollectionUtils.isNotEmpty(results) ? results.get(0) : null); 
	}
	
	public List<ShmNotification> listNotificationsForNotificationId(Long notificationInstId, EntityManager entityManager) {
	    
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ShmNotification> query = cb.createQuery(ShmNotification.class);
        Root<ShmNotification> from = query.from(ShmNotification.class);

        Expression<BigDecimal> notificationInstIdPath = from.get(ShmNotification_.scoNtficnInstId);
        Expression<String> statusCdPath = from.get(ShmNotification_.statCd);
        Expression<String> catgCdPath = from.get(ShmNotification_.catgCd);
        
        List<Predicate> predicates = new ArrayList<>();
        Predicate notificationInstIdPred = cb.equal(notificationInstIdPath, notificationInstId);
        predicates.add(notificationInstIdPred);
        Predicate catgCdPred = cb.equal(catgCdPath, ShmNotificationCatgCdEnum.APPOINTMENT.getCode());
        predicates.add(catgCdPred);
        predicates.add(statusCdPath.in(APPT_STATUSES));

        query.select(from).where(predicates.toArray(new Predicate[CollectionUtils.size(predicates)]));
        return entityManager.createQuery(query).getResultList();
	}
	
	public DB2ShmNotification findDB2ByShipmentIdAndSeqNbr(Long shpInstId, Long ntfnSeqNbr, EntityManager db2EntityManager) {
	    
        CriteriaBuilder cb = db2EntityManager.getCriteriaBuilder();
        CriteriaQuery<DB2ShmNotification> query = cb.createQuery(DB2ShmNotification.class);
        Root<DB2ShmNotification> from = query.from(DB2ShmNotification.class);

        Expression<Long> shipmentInstIdPath = from.get(DB2ShmNotification_.id).get(DB2ShmNotificationPK_.shpInstId);
        Path<Short> seqNbrPath = from.get(DB2ShmNotification_.id).get(DB2ShmNotificationPK_.ntfictnSeqNbr);
        List<Predicate> predicates = new ArrayList<>();
        Predicate shipmentInstIdPred = cb.equal(shipmentInstIdPath, shpInstId);
        Predicate seqNbrPred = cb.equal(seqNbrPath, ntfnSeqNbr);
        predicates.add(shipmentInstIdPred);
        predicates.add(seqNbrPred);
        query.select(from).where(predicates.toArray(new Predicate[CollectionUtils.size(predicates)]));

        List<DB2ShmNotification> results = db2EntityManager.createQuery(query).getResultList();
        return CollectionUtils.isNotEmpty(results) ? results.get(0) : null;
	}
	
    public void updateDB2ShmNotification(ShmNotification record, EntityManager db2EntityManager, TransactionContext txnContext) throws  ValidationException, NotFoundException {

        final Function<DB2ShmNotification, Boolean> checkVersionFunction = getCheckVersionFunction(record.getLstUpdtTmst());
        updateDB2ShmNotification(record, checkVersionFunction, db2EntityManager, txnContext);
    }

    private Function<DB2ShmNotification, Boolean> getCheckVersionFunction(Timestamp exadataLstUpdtTmst) {
        return db2Entity -> db2Entity.getLstUpdtTmst().compareTo(exadataLstUpdtTmst) <= 0;
    }
    
}