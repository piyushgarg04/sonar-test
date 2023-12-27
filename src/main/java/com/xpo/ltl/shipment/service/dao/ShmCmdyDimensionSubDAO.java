package com.xpo.ltl.shipment.service.dao;

import com.xpo.ltl.api.shipment.service.dao.ShmCmdyDimensionDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmCmdyDimension;
import com.xpo.ltl.api.shipment.service.entity.ShmCmdyDimensionPK_;
import com.xpo.ltl.api.shipment.service.entity.ShmCmdyDimension_;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
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
public class ShmCmdyDimensionSubDAO extends ShmCmdyDimensionDAO<ShmCmdyDimension> {

    public List<ShmCmdyDimension> findByShpInstIds(List<Long> shipmentInstIds, EntityManager entityManager) {
        checkNotNull(shipmentInstIds, "shipmentInstId");

        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<ShmCmdyDimension> query = cb.createQuery(ShmCmdyDimension.class);
        final Root<ShmCmdyDimension> from = query.from(ShmCmdyDimension.class);

        final List<Predicate> predicates = new ArrayList<>();

        final Path<Long> shipmentInstIdPath = from.get(ShmCmdyDimension_.id).get(ShmCmdyDimensionPK_.shpInstId);
        predicates
                .add(shipmentInstIdPath.in(shipmentInstIds));

        query.select(from).where(predicates.toArray(new Predicate[predicates.size()]));
        return entityManager.createQuery(query).getResultList();
    }
}
