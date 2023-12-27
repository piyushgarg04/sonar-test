package com.xpo.ltl.shipment.service.impl.updateshipment.update.common;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.EventLogSubTypeCd;
import com.xpo.ltl.api.shipment.v2.EventLogTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractShipment;

import javax.persistence.EntityManager;

public class ShipmentUpdateCommonImpl extends AbstractShipment {

	public void update(
			UpdateShipmentRqst updateShipmentRqst,
			EntityManager entityManager,
			EntityManager db2EntityManager,
			TransactionContext transactionContext,
			ShmShipment shmShipment,
			String transactionCd,
			EventLogTypeCd eventLogTypeCd,
			EventLogSubTypeCd eventLogSubTypeCd,
			ShipmentUpdateActionCd shipmentUpdateActionCd) throws ServiceException {
		this.updateShipment(
				updateShipmentRqst,
				entityManager,
				db2EntityManager,
				transactionContext,
				shmShipment,
				transactionCd,
				eventLogTypeCd,
				eventLogSubTypeCd,
				shipmentUpdateActionCd);
	}

}
