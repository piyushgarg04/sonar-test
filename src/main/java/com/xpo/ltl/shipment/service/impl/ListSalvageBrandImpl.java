package com.xpo.ltl.shipment.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.ListMetadata;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmSalvageBrand;
import com.xpo.ltl.api.shipment.v2.ListBrandsForSalvageResp;
import com.xpo.ltl.shipment.service.dao.ShmSalvageBrandSubDAO;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ListSalvageBrandImpl {
	@Inject
	private ShmSalvageBrandSubDAO shmSalvageBrandSubDAO;
	

	@PersistenceContext(unitName = "ltl-java-shipment-rpt-jaxrs")
	private EntityManager rptEntityManager;

	public ListBrandsForSalvageResp listSalvageBrand(
		final ListMetadata listMetadata,
		final TransactionContext txnContext,
		final EntityManager entityManager
	) throws ServiceException {
		List<ShmSalvageBrand> shmSalvageBrandList =  shmSalvageBrandSubDAO.getShmSalvageBrandList(rptEntityManager);
		ListBrandsForSalvageResp listBrandsForSalvageResp = new ListBrandsForSalvageResp();
		listBrandsForSalvageResp.setBrands(shmSalvageBrandList.stream().map(e->e.getBrand()).collect(Collectors.toList()));
		return listBrandsForSalvageResp;
	}

}
