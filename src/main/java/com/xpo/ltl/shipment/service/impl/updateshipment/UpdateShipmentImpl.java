package com.xpo.ltl.shipment.service.impl.updateshipment;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmMiscLineItem;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.ShipmentDetailCd;
import com.xpo.ltl.api.shipment.v2.ShipmentUpdateActionCd;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentResp;
import com.xpo.ltl.shipment.service.dao.ShmMiscLineItemSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentEagerLoadPlan;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.UpdateShipment;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.UpdateFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

@ApplicationScoped
public class UpdateShipmentImpl {

	private static final String INST_ID_IS_REQUIRED = "The ShipmentInstId is required.";
	private static final String REQUIRED_STRING = " is required.";

	private static final String COLON = ":";

	private static final Logger logger = LogManager.getLogger(UpdateShipmentImpl.class);

	@Inject
	private UpdateFactory updateFactory;

	@Inject
	private ShmMiscLineItemSubDAO shmMiscLineItemSubDAO;
	@Inject
	private ShmShipmentSubDAO shmShipmentSubDAO;

	public UpdateShipmentResp updateShipment(
			Long shipmentInstId,
			ShipmentUpdateActionCd shipmentUpdateActionCd,
			com.xpo.ltl.api.shipment.v2.UpdateShipmentRqst updateShipmentRqst,
			TransactionContext txnContext,
			EntityManager entityManager) throws ServiceException {
		UpdateShipmentResp result = new UpdateShipmentResp();

		checkNotNull(txnContext, "The TransactionContext" + REQUIRED_STRING);
		checkNotNull(entityManager, "The EntityManager" + REQUIRED_STRING);
		checkNotNull(updateShipmentRqst, "The request" + REQUIRED_STRING);
		checkNotNull(shipmentUpdateActionCd, "The ActionCd" + REQUIRED_STRING);
		checkNotNull(updateShipmentRqst.getShipment(), "The Shipment" + REQUIRED_STRING);
		checkNotNull(updateShipmentRqst.getShipment().getShipmentInstId(), INST_ID_IS_REQUIRED);
		checkNotNull(shipmentInstId, INST_ID_IS_REQUIRED);

		if ((updateShipmentRqst.getShipment().getShipmentInstId() == 0)) {
			throw ExceptionBuilder
					.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo("ShipmentInstId", INST_ID_IS_REQUIRED)
					.build();
		}
		logger.info("{} {}{}{}{}{}{}{}",
				"INIT",
				this.getClass().getSimpleName(),
				COLON,
				"updateShipment",
				COLON,
				"ActionCd",
				COLON,
				shipmentUpdateActionCd);
		final List<ShipmentDetailCd> shipmentDetailCdsList = getShipmentDetails(shipmentUpdateActionCd);
		ShmShipmentEagerLoadPlan shmShipmentEagerLoadPlan = ShmShipmentEagerLoadPlan.from(shipmentDetailCdsList, false);
		ShmShipment shmShipment = shmShipmentSubDAO.findByProOrShipmentId(null,
				null,
				shipmentInstId,
				false,
				shmShipmentEagerLoadPlan,
				entityManager);
		if (Objects.isNull(shmShipment)) {
			throw ExceptionBuilder.exception(NotFoundErrorMessage.SHIPMENT_NOT_FOUND, txnContext).build();
		}
		List<ShmMiscLineItem> shmMiscLineItems = shmShipment.getShmMiscLineItems();

		UpdateShipment updateShipment = updateFactory.getUpdateImplementation(shipmentUpdateActionCd);
		updateShipment.update(updateShipmentRqst,
				shipmentUpdateActionCd,
				shmShipment,
				shmMiscLineItems,
				entityManager,
				txnContext);

		result.setMessage(("Shipment Update for " + shipmentUpdateActionCd + " has been Successful."));
		logger.info(result.getMessage());
		logger.info("{} {}{}{}{}{}{}{}",
				"END",
				this.getClass().getSimpleName(),
				COLON,
				"updateShipment",
				COLON,
				"ActionCd",
				COLON,
				shipmentUpdateActionCd);
		return result;
	}

	private List<ShipmentDetailCd> getShipmentDetails(ShipmentUpdateActionCd shipmentUpdateActionCd) {
		List<ShipmentDetailCd> result = new ArrayList<>();
		if (shipmentUpdateActionCd == null) {
			throw new IllegalArgumentException("shipmentUpdateActionCd cannot be null");
		}
		switch (shipmentUpdateActionCd) {
		case MANUAL_RATE:
			addCommonShipmentDetails(result);
			break;
		case CORRECTION:
			addCommonShipmentDetails(result);
			Collections.addAll(
					result,
					ShipmentDetailCd.CUSTOMS_BOND,
					ShipmentDetailCd.TIME_DATE_CRITICAL,
					ShipmentDetailCd.SHIPMENT_PARTIES);
			break;
		case AUTO_RATE:
			Collections.addAll(
					result,
					ShipmentDetailCd.COMMODITY,
					ShipmentDetailCd.ACCESSORIAL,
					ShipmentDetailCd.MISC_LINE_ITEM);
			break;
		}

		return result;
	}

	private void addCommonShipmentDetails(List<ShipmentDetailCd> result) {
		Collections.addAll(
				result,
				ShipmentDetailCd.COMMODITY,
				ShipmentDetailCd.ACCESSORIAL,
				ShipmentDetailCd.ADVANCE_BEYOND,
				ShipmentDetailCd.REMARKS,
				ShipmentDetailCd.MISC_LINE_ITEM);
	}
}
