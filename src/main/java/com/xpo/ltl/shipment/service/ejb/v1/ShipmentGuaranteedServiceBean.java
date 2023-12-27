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
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.service.v2.ShipmentGuaranteedServiceIF;
import com.xpo.ltl.api.shipment.v2.CreateGuaranteedFailureRerateResp;
import com.xpo.ltl.api.shipment.v2.CreateGuaranteedFailureRerateRqst;
import com.xpo.ltl.shipment.service.impl.CreateGuaranteedFailureRerateImpl;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@Stateless
@Local(ShipmentGuaranteedServiceIF.class)
@TransactionManagement(TransactionManagementType.CONTAINER)
@LogExecutionTime
public class ShipmentGuaranteedServiceBean implements ShipmentGuaranteedServiceIF {

	@Resource
	private EJBContext ejbContext;

	@PersistenceContext(unitName = "ltl-java-shipment-jaxrs")
	private EntityManager entityManager;
	@Inject
	private CreateGuaranteedFailureRerateImpl createGuaranteedFailureRerateImpl;

	@Override
	public CreateGuaranteedFailureRerateResp createGuaranteedFailureRerate(
			CreateGuaranteedFailureRerateRqst createGuaranteedFailureRerateRqst, TransactionContext txnContext)
			throws ServiceException {
		try {
			return createGuaranteedFailureRerateImpl.createGuaranteedFailureRerate(createGuaranteedFailureRerateRqst,
					txnContext, entityManager);
		} catch (final ServiceException e) {
			ejbContext.setRollbackOnly();
			throw e;
		} catch (final RuntimeException e) {
			ejbContext.setRollbackOnly();
			throw ExceptionBuilder.exception(ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext)
					.contextValues(String.format("Run time exception during insert :")).log(e).build();
		}

	}
}
