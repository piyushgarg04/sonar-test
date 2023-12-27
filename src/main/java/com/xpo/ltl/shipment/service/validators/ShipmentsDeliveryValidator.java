package com.xpo.ltl.shipment.service.validators;

import java.util.ArrayList;
import java.util.List;

import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.v2.ValidateShipmentsForDeliveryRqst;
import com.xpo.ltl.shipment.service.util.ShipmentUtil;

public class ShipmentsDeliveryValidator extends Validator {

	public void validateValidateShipmentsForDeliveryRqst(ValidateShipmentsForDeliveryRqst request,
			TransactionContext txnContext) throws ServiceException {
 
		List<MoreInfo> moreInfos = new ArrayList<>();
		Boolean isPRONumbersSetEmpty = ShipmentUtil.isPRONumbersSetEmpty(request.getShipmentIds());
		Boolean isShipmentPkupDateSetEmpty = ShipmentUtil.isShipmentPkupDateSetEmpty(request.getShipmentIds());
		Boolean anyHasShipmentInstIdSet = ShipmentUtil.anyHasShipmentInstIdSet(request.getShipmentIds());

		if (anyHasShipmentInstIdSet && isPRONumbersSetEmpty) {
			throw ExceptionBuilder.exception(ServiceErrorMessage.SHM_DLVR_INST_ID_NOT_SUPPORTED, txnContext)
					.moreInfo(moreInfos).build();
		}
		if (isPRONumbersSetEmpty) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.PRO_NBR_RQ, txnContext).moreInfo(moreInfos).build();
		}
		if (!isPRONumbersSetEmpty && isShipmentPkupDateSetEmpty) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.PKUP_DT_REQ, txnContext).moreInfo(moreInfos)
					.build();
		}

		if (!moreInfos.isEmpty())
			throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo(moreInfos).build();
	}
}