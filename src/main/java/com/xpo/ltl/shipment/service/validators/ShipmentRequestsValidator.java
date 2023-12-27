package com.xpo.ltl.shipment.service.validators;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.v2.ShipmentDetailCd;

public class ShipmentRequestsValidator extends Validator {

	public void validateRequest(final String proNbr, final String pickupDate,
			final Long shipmentInstId, final boolean previousLatestMovementInd,
			final TransactionContext txnContext) throws ValidationException, ServiceException {

		// We need a PRO or shipment instance ID.
		if (proNbr == null && shipmentInstId == null) {
			throw addMoreInfo(ExceptionBuilder
					.exception(ValidationErrorMessage.PRO_OR_SHP_INST_REQD, txnContext), proNbr,
					pickupDate, shipmentInstId, previousLatestMovementInd).log().build();
			// If there is a PRO and a shipment instance ID, throw an error -- you can give
			// one or the other, but not both.
		} else if (proNbr != null && shipmentInstId != null && shipmentInstId != 0) {
			throw addMoreInfo(ExceptionBuilder
					.exception(ValidationErrorMessage.PRO_PICKUPDT_AND_OR_SHIP_RQD, txnContext),
					proNbr, pickupDate, shipmentInstId, previousLatestMovementInd).log().build();
		}
		// If you give a shipment instance ID you can't give a pickup date.
		else if (pickupDate != null && shipmentInstId != null && shipmentInstId != 0) {
			throw addMoreInfo(ExceptionBuilder
					.exception(ValidationErrorMessage.PICKUPDT_ON_SHIP_INST_ID_SRCH, txnContext),
					proNbr, pickupDate, shipmentInstId, previousLatestMovementInd).log().build();
		} else if (txnContext == null) {
			throw new NullPointerException("The TransactionContext is required.");
		}
	}
	
	public void validateRequest(final String proNbr, final String pickupDate,
			final Long shipmentInstId, final ShipmentDetailCd[] shipmentDetailCds,
			final TransactionContext txnContext) throws ValidationException, ServiceException {

		// We need a PRO or shipment instance ID.
		if (proNbr == null && shipmentInstId == null) {
			throw addMoreInfo(ExceptionBuilder
					.exception(ValidationErrorMessage.PRO_OR_SHP_INST_REQD, txnContext), proNbr,
					pickupDate, shipmentInstId, shipmentDetailCds).log().build();
			// If there is a PRO and a shipment instance ID, throw an error -- you can give
			// one or the other, but not both.
		} else if (proNbr != null && shipmentInstId != null && shipmentInstId != 0) {
			throw addMoreInfo(ExceptionBuilder
					.exception(ValidationErrorMessage.PRO_PICKUPDT_AND_OR_SHIP_RQD, txnContext),
					proNbr, pickupDate, shipmentInstId, shipmentDetailCds).log().build();
		}
		// If you give a shipment instance ID you can't give a pickup date.
		else if (pickupDate != null && shipmentInstId != null && shipmentInstId != 0) {
			throw addMoreInfo(ExceptionBuilder
					.exception(ValidationErrorMessage.PICKUPDT_ON_SHIP_INST_ID_SRCH, txnContext),
					proNbr, pickupDate, shipmentInstId, shipmentDetailCds).log().build();
		} else if (txnContext == null) {
			throw new NullPointerException("The TransactionContext is required.");
		}
	}
	
	public void validateRequest(
			final List<String> proNumbers,
			final List<Long> shipmentInstIds,
			final List<ShipmentDetailCd> shipmentDetailCds,
			final TransactionContext txnContext
		) throws ValidationException, ServiceException {

			// If there is a PRO and a shipment instance ID, throw an error -- you can give
			// one or the other, but not both.
			if (CollectionUtils.isNotEmpty(proNumbers) && CollectionUtils.isNotEmpty(shipmentInstIds)) {
				throw addMoreInfo(ExceptionBuilder.exception(ValidationErrorMessage.PRO_OR_SHP_INST_REQD, txnContext),
						shipmentInstIds, shipmentDetailCds).log().build();
				// We need a PRO or shipment instance ID.
			} else if (CollectionUtils.isEmpty(shipmentInstIds) && CollectionUtils.isEmpty(proNumbers)) {
				throw addMoreInfo(
					ExceptionBuilder.exception(
						ValidationErrorMessage.PRO_OR_SHP_INST_REQD, txnContext
					),
					shipmentInstIds,
					shipmentDetailCds
				).log().build();
			}

			if (CollectionUtils.size(shipmentDetailCds) >= 2
					&& shipmentDetailCds.contains(ShipmentDetailCd.SHIPMENT_ONLY)) {
				throw addMoreInfo(ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext),
						shipmentInstIds, shipmentDetailCds).moreInfo(
						"reason",
						String.format(
								"The shipment detail code %s cannot be used with other shipment detail codes.",
								ShipmentDetailCd.SHIPMENT_ONLY.value()
						)
				).log().build();
			}
		}
	
	public void validateRequest(
			final List<String> proNumbers,
			final List<Long> shipmentInstIds,
			final TransactionContext txnContext
		) throws ValidationException, ServiceException {

			// If there is a PRO and a shipment instance ID, throw an error -- you can give
			// one or the other, but not both.
			if (CollectionUtils.isNotEmpty(proNumbers) && CollectionUtils.isNotEmpty(shipmentInstIds)) {
				throw addMoreInfo(ExceptionBuilder.exception(ValidationErrorMessage.PRO_OR_SHP_INST_REQD, txnContext),
						shipmentInstIds).log().build();
				// We need a PRO or shipment instance ID.
			} else if (CollectionUtils.isEmpty(shipmentInstIds) && CollectionUtils.isEmpty(proNumbers)) {
				throw addMoreInfo(
					ExceptionBuilder.exception(
						ValidationErrorMessage.PRO_OR_SHP_INST_REQD, txnContext
					),
					shipmentInstIds
				).log().build();
			}
		}
	
	public void validateShipmentDetailCds(final Set<ShipmentDetailCd> include, final String proNbr, final String pickupDate,
			final Long shipmentInstId, final ShipmentDetailCd[] shipmentDetailCds,
			final TransactionContext txnContext) throws ValidationException, ServiceException {
		if (include.size() > 1 && include.contains(ShipmentDetailCd.SHIPMENT_ONLY)) {
			throw addMoreInfo(
					ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND,
							txnContext),
					proNbr, pickupDate, shipmentInstId, shipmentDetailCds).moreInfo(
							"reason",
							String.format(
									"The shipment detail code %s cannot be used with other shipment detail codes.",
									ShipmentDetailCd.SHIPMENT_ONLY.value()))
							.log().build();
		}
	}
	
	private ExceptionBuilder<? extends ServiceException> addMoreInfo(
			final ExceptionBuilder<? extends ServiceException> builder, final String proNbr,
			final String pickupDate, final Long shipmentInstId,
			final Boolean previousLatestMovement) {
		if (builder == null) {
			throw new NullPointerException("The ExceptionBuilder is required.");
		}

		builder.moreInfo("proNbr", proNbr);
		builder.moreInfo("pickupDate", pickupDate != null ? pickupDate : null);
		builder.moreInfo("shipmentInstId",
				shipmentInstId != null ? shipmentInstId.toString() : null);
		builder.moreInfo("previousLatestMovement",
				previousLatestMovement.toString() != null ? previousLatestMovement.toString() : null);

		return builder;
	}
	
	
	
	private ExceptionBuilder<? extends ServiceException> addMoreInfo(
			final ExceptionBuilder<? extends ServiceException> builder, final String proNbr,
			final String pickupDate, final Long shipmentInstId,
			final ShipmentDetailCd[] shipmentDetailCds) {
		if (builder == null) {
			throw new NullPointerException("The ExceptionBuilder is required.");
		}

		builder.moreInfo("proNbr", proNbr);
		builder.moreInfo("pickupDate", pickupDate != null ? pickupDate : null);
		builder.moreInfo("shipmentInstId",
				shipmentInstId != null ? shipmentInstId.toString() : null);
		builder.moreInfo("shipmentDetailCds",
				ArrayUtils.isNotEmpty(shipmentDetailCds) ? Arrays.toString(shipmentDetailCds)
						: null);

		return builder;
	}
	
	public ExceptionBuilder<? extends ServiceException> addMoreInfo(ExceptionBuilder<? extends ServiceException> builder,
																	 List<Long> shipmentInstIds,
																	 Collection<ShipmentDetailCd> shipmentDetailCds) {
		builder.moreInfo("shipmentInstIds",
			CollectionUtils.isNotEmpty(shipmentInstIds)
				? String.join(",", shipmentInstIds.stream().map(shipmentInstId -> shipmentInstId.toString()).collect(Collectors.toList()))
				: null
		);
		
		builder.moreInfo("shipmentDetailCds",
			CollectionUtils.isNotEmpty(shipmentDetailCds)
				? String.join(",", shipmentDetailCds.stream().map(shipmentDetailCd -> shipmentDetailCd.name()).collect(Collectors.toList()))
				: null
		);
		
		return builder;
	}
	
	private ExceptionBuilder<? extends ServiceException> addMoreInfo(ExceptionBuilder<? extends ServiceException> builder,
					 List<Long> shipmentInstIds) {
		builder.moreInfo("shipmentInstIds",
			CollectionUtils.isNotEmpty(shipmentInstIds)
				? String.join(",", shipmentInstIds.stream().map(shipmentInstId -> shipmentInstId.toString()).collect(Collectors.toList()))
				: null
		);
		
		return builder;
	}
}
