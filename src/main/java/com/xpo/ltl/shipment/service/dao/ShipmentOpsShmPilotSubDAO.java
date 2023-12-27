package com.xpo.ltl.shipment.service.dao;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.shipment.service.dao.ShmOpsShipmentPilotDAO;
import com.xpo.ltl.api.shipment.service.entity.ShmOpsShipmentPilot;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;

@ApplicationScoped
@LogExecutionTime
public class ShipmentOpsShmPilotSubDAO extends ShmOpsShipmentPilotDAO<ShmOpsShipmentPilot> {

	public ShmOpsShipmentPilot create(
			final ShmOpsShipmentPilot entity,
			final EntityManager entityManager) throws ValidationException {

			return super.save(entity, entityManager);
		}
}
