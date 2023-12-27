package com.xpo.ltl.shipment.service.dao;

import com.xpo.ltl.api.shipment.service.dao.ShmMscLnItmHstDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmMscLnItmHst;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ShmMscLnItmHstSubDAO extends ShmMscLnItmHstDAO<ShmMscLnItmHst> {

	private static final String MISCELLANEOUS_SQL = "SELECT miscellaneous FROM ShmMscLnItmHst miscellaneous WHERE miscellaneous.id.shpInstId = :shipmentInstId ";

	@LogExecutionTime
	public List<ShmMscLnItmHst> getMiscellaneousForShipmentId(Long shipmentInstId, EntityManager entityManager) {
		//For performance reasons DO NOT change this from a native query to use criteria builder.
		TypedQuery<Tuple> acQuery = entityManager.createQuery(MISCELLANEOUS_SQL, Tuple.class);
		acQuery.setParameter("shipmentInstId", shipmentInstId);
		List<Tuple> acResults = acQuery.getResultList();
		List<ShmMscLnItmHst> acList = new ArrayList<>();
		for (Tuple tuple : acResults) {
			acList.add((ShmMscLnItmHst)tuple.get(0));
		}
		return acList;
	}

}