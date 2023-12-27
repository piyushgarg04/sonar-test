package com.xpo.ltl.shipment.service.dao;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;

import com.xpo.ltl.api.shipment.service.entity.ShmCustomsCntrl;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShipmentCustomsCntlSubDAO {

	private static String CUSTOMS_CNTL_BY_SHP_ID_SQL  = "SELECT customsCntl FROM ShmCustomsCntrl customsCntl WHERE customsCntl.id.shpInstId in :shpIdList ";
	
	public List<ShmCustomsCntrl> listCustomsControlByShipmentIdList(List<Long> shipmentInstIds, EntityManager entityManager) {
		// For performance reasons DO NOT change this from a native query to use critiera builder.
		TypedQuery<Tuple> query = entityManager.createQuery(CUSTOMS_CNTL_BY_SHP_ID_SQL, Tuple.class);
		query.setParameter("shpIdList", shipmentInstIds);
		List<Tuple> results = query.getResultList();
		List<ShmCustomsCntrl> customsCntlList = new ArrayList<>();
		for (Tuple oneResult : results) {
			ShmCustomsCntrl oneCustomsControl = (ShmCustomsCntrl)oneResult.get(0);
			customsCntlList.add(oneCustomsControl);
		}
		return customsCntlList;
	}
}