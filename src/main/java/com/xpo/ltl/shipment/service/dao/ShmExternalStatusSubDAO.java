package com.xpo.ltl.shipment.service.dao;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.shipment.service.dao.ShmExternalStatusDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmExternalStatus;
import com.xpo.ltl.api.shipment.service.entity.ShmExternalStatus_;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmExternalStatusSubDAO extends ShmExternalStatusDAO<ShmExternalStatus>{
	
	public ShmExternalStatus create(ShmExternalStatus entity, EntityManager entityManager) throws ValidationException{
		
		return super.save(entity, entityManager);
	}
	
	public long getLastUsedStatusId(EntityManager entityManager) {
		
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
		Root<ShmExternalStatus> shmExternalStatusRoot = query.from(ShmExternalStatus.class);
		
		Path<Long> statusIdPath = shmExternalStatusRoot.get(ShmExternalStatus_.statusId);
		query.select(criteriaBuilder.max(statusIdPath));
		TypedQuery<Long> typedQuery = entityManager.createQuery(query);
        Long maxStatusId = typedQuery.getSingleResult();
        if (maxStatusId == null)
            return 0;
        return maxStatusId;
	}

}
