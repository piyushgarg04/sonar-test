package com.xpo.ltl.shipment.service.dao;

import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.apache.commons.collections.CollectionUtils;

import com.xpo.ltl.api.shipment.service.dao.ShmBillEntryStatDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmBillEntryStat;
import com.xpo.ltl.api.shipment.service.entity.ShmBillEntryStatPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmBillEntryStat_;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmBillEntryStatsSubDAO extends ShmBillEntryStatDAO<ShmBillEntryStat> {


    public ShmBillEntryStat findByShipmentInstId(Long shipmentInstId, EntityManager entityManager) {
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<ShmBillEntryStat> query = criteriaBuilder.createQuery(ShmBillEntryStat.class);
        final Root<ShmBillEntryStat> from = query.from(ShmBillEntryStat.class);

        final Expression<Long> shipmentInstIdPath = from.get(ShmBillEntryStat_.id).get(ShmBillEntryStatPK_.shpInstId);
        final Path<Date> startTmstOrderPath = from.get(ShmBillEntryStat_.id).get(ShmBillEntryStatPK_.billEntryStartTmst);

        query.select(from).where(criteriaBuilder.equal(shipmentInstIdPath, shipmentInstId)).orderBy(
            criteriaBuilder.desc(startTmstOrderPath));

        final List<ShmBillEntryStat> entryList = entityManager.createQuery(query).setMaxResults(1).getResultList();
        return CollectionUtils.isEmpty(entryList) ? null : entryList.get(0);
    }

}