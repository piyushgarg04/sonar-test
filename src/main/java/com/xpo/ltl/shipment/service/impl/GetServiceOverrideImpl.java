package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmSvcOvrd;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.v2.GetServiceOverrideResp;
import com.xpo.ltl.shipment.service.dao.ShmSvcOvrdSubDAO;

@RequestScoped
public class GetServiceOverrideImpl {

	private static final Log logger = LogFactory.getLog(GetServiceOverrideImpl.class);

	@Inject
	private ShmSvcOvrdSubDAO shmSvcOvrdSubDAO;

	/**
	 * Method to get Shipment Service Override information
	 * @param shipmentInstId
	 * @param txnContext
	 * @param entityManager
	 * @return GetServiceOverrideResp
	 * @throws ServiceException
	 */
	public GetServiceOverrideResp getServiceOverride(Long shipmentInstId, final TransactionContext txnContext,
			final EntityManager entityManager) throws ServiceException {

		checkNotNull(shipmentInstId, "The shipmentInstId is required.");

		final ShmSvcOvrd shmSvcOvrd = shmSvcOvrdSubDAO.findById(shipmentInstId, entityManager);
		GetServiceOverrideResp resp = new GetServiceOverrideResp();
		if (Objects.nonNull(shmSvcOvrd))
			resp.setShipmentServiceOverride(EntityTransformer.toServiceOverride(shmSvcOvrd));

		return resp;

	}
}
