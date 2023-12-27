package com.xpo.ltl.shipment.service.ejb.v1;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.v2.InterimServiceIF;
import com.xpo.ltl.api.shipment.v2.CreateBaseLogRqst;
import com.xpo.ltl.shipment.service.impl.interim.CreateBaseLogImpl;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@Stateless
@Local(InterimServiceIF.class)
@TransactionManagement(TransactionManagementType.CONTAINER)
@LogExecutionTime
public class InterimServiceBean implements InterimServiceIF {

	@Resource
	private EJBContext ejbContext;

	@PersistenceContext(unitName = "ltl-java-shipment-jaxrs")
	private EntityManager entityManager;


	@Inject
	private CreateBaseLogImpl createBaseLogImpl;

	@Override
	public void createBaseLog(CreateBaseLogRqst createBaseLogRqst, String logId, TransactionContext txnContext)
			throws ServiceException, ValidationException {
		createBaseLogImpl.createBaseLog(createBaseLogRqst, logId, txnContext);
	}
}
