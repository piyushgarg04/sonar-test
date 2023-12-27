package com.xpo.ltl.shipment.service.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;

import com.xpo.ltl.api.shipment.service.dao.ShmSalvageBrandDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageBrand;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmSalvageBrandSubDAO extends ShmSalvageBrandDAO<ShmSalvageBrand> {

	public List<ShmSalvageBrand> getShmSalvageBrandList(
			EntityManager entityManager) {
		List<ShmSalvageBrand> salvageBrands = entityManager.createNamedQuery("ShmSalvageBrand.findAll", ShmSalvageBrand.class).getResultList();
		if (CollectionUtils.isEmpty(salvageBrands)) {
			return null;
		}
		return salvageBrands;
	}
}
