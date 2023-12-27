package com.xpo.ltl.shipment.service.dao;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;

import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.dao.ShmSvcOvrdDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmSvcOvrd;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShmSvcOvrdSubDAO extends ShmSvcOvrdDAO<ShmSvcOvrd> {

	public ShmSvcOvrd findById(final Long shipmentInstId,
			final TransactionContext txnContext,
			final EntityManager entityManager) throws ServiceException, ValidationException, NotFoundException
	{
		return findById(shipmentInstId, entityManager);

	}

}