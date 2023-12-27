package com.xpo.ltl.shipment.service.dao;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;

import com.xpo.ltl.api.shipment.service.dao.ShmTmDtCriticalDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmTmDtCritical;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShipmentTdcSubDAO extends ShmTmDtCriticalDAO<ShmTmDtCritical> {

	private static String TDC_SQL = "SELECT tdc FROM ShmTmDtCritical tdc WHERE tdc.id.shpInstId in :shpIdList ";
	
	public List<ShmTmDtCritical> listTdcForShipmentIdList(List<Long> shipmentInstIds, EntityManager entityManager) {
		//For performance reasons DO NOT change this from a native query to use criteria builder.
		TypedQuery<Tuple> query = entityManager.createQuery(TDC_SQL, Tuple.class);
		query.setParameter("shpIdList", shipmentInstIds);
		List<Tuple> results = query.getResultList();
		List<ShmTmDtCritical> tdcList = new ArrayList<>();
		for (Tuple tuple : results) {
			tdcList.add((ShmTmDtCritical)tuple.get(0));
		}
		return tdcList;
	}
}