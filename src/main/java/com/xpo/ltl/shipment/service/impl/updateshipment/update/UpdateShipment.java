package com.xpo.ltl.shipment.service.impl.updateshipment.update;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst;

import javax.persistence.EntityManager;
import java.util.List;

public interface UpdateShipment {
	void update(
			UpdateShipmentRqst updateShipmentRqst,
			ShipmentUpdateActionCd shipmentUpdateActionCd,
			ShmShipment shipment,
			List<ShmMiscLineItem> shmMiscLineItems,
			EntityManager entityManager,
			TransactionContext transactionContext) throws ServiceException;

}
