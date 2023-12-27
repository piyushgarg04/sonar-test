package com.xpo.ltl.shipment.service.dao;

import com.xpo.ltl.api.shipment.service.dao.ShmCustomsBondDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmCustomsBond;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@LogExecutionTime
public class ShipmentCustomsSubDAO extends ShmCustomsBondDAO<ShmCustomsBond> {

	private static final  String CUSTOMS_BOND_BY_SHP_ID_SQL  = "SELECT customsBond FROM ShmCustomsBond customsBond WHERE customsBond.id.shpInstId in :shpIdList ";
	
	public List<ShmCustomsBond> listCustomsBondByShipmentIdList(List<Long> shipmentInstIds, EntityManager entityManager) {
		// For performance reasons DO NOT change this from a native query to use critiera builder.
		TypedQuery<Tuple> query = entityManager.createQuery(CUSTOMS_BOND_BY_SHP_ID_SQL, Tuple.class);
		query.setParameter("shpIdList", shipmentInstIds);
		List<Tuple> results = query.getResultList();
		List<ShmCustomsBond> customsBondList = new ArrayList<>();
		for (Tuple oneResult : results) {
			ShmCustomsBond oneCustomsBond = (ShmCustomsBond)oneResult.get(0);
			customsBondList.add(oneCustomsBond);
		}
		return customsBondList;
	}

}