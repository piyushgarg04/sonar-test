package com.xpo.ltl.shipment.service.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import com.xpo.ltl.api.shipment.service.dao.ShmEventReferenceDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmEventReference;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmEventReferenceSubDAO extends ShmEventReferenceDAO<ShmEventReference> {

	/**
	 * Method to lists the Event References for Shipment.
	 * 
	 * @param maxResults
	 * @param firstResult
	 * @param entityManager
	 * @return
	 */
	public List<ShmEventReference> listEventReferences(
		final Integer maxResults,
		final Integer firstResult,
		final EntityManager entityManager) {

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ShmEventReference> criteriaQuery = cb.createQuery(ShmEventReference.class);
		final Root<ShmEventReference> from = criteriaQuery.from(ShmEventReference.class);
		
		TypedQuery<ShmEventReference> query = entityManager.createQuery(criteriaQuery);
		if (maxResults != null) {
			query.setMaxResults(maxResults);
		}
		if (firstResult != null) {
			query.setFirstResult(firstResult);
		}
		return query.getResultList();
	}
		
}