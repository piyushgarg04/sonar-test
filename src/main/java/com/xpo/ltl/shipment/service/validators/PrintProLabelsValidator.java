package com.xpo.ltl.shipment.service.validators;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.v2.PrintProLabelsRqst;
import com.xpo.ltl.api.shipment.v2.ProLabel;

public class PrintProLabelsValidator extends Validator {

	public void validate(PrintProLabelsRqst printProLabelsRqst, TransactionContext txnContext) throws ServiceException {

		List<MoreInfo> moreInfos = new ArrayList<>();

		if (null == printProLabelsRqst.getProLabels() || printProLabelsRqst.getProLabels().isEmpty()) {
			addMoreInfo(moreInfos, "proLabels", "At least 1 proLabel information is required ");
			throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo(moreInfos).build();
		}

		for (ProLabel proLabel : printProLabelsRqst.getProLabels()) {
			if (StringUtils.isBlank(proLabel.getProNbr())) {
				addMoreInfo(moreInfos, "proNbr", "proNbr is required");
				throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
						.moreInfo(moreInfos).build();
			}

			if (null != proLabel.getLabelCount() && proLabel.getLabelCount().compareTo(BigInteger.ZERO) <= 0) {
				addMoreInfo(moreInfos, "labelCount", "labelCount value should be more than zero");
				throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
						.moreInfo(moreInfos).build();

			}

		}

	}

}