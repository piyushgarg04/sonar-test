package com.xpo.ltl.shipment.service.dao;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.shipment.service.dao.ShmOpsShipmentDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmOpsShipment;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShipmentOpsShmSubDAO extends ShmOpsShipmentDAO<ShmOpsShipment> {

	public ShmOpsShipment create(
			final ShmOpsShipment entity,
			final EntityManager entityManager) throws ValidationException {

			return super.save(entity, entityManager);
		}
}
