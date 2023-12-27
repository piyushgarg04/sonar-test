package com.xpo.ltl.shipment.service.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;

/**
 * Formatting util.-
 *
 * @author skapcitzky
 *
 */
public class FormatHelper {

	private static final Log logger = LogFactory.getLog(FormatHelper.class);

	private static final char PADDING_ZERO = '0';
	private static final String SEPARATOR = "-";
	private static final int PRONUM_MAX_LENGTH = 11;
	private static final int PRONUM_MIN_LENGTH = 9;
	private static final int PRONUM_LENGTH_TO_VALIDATE = 10;

	/**
	 * Check proNumber length and validate it contains separator, add zeros when is necessary
	 *
	 * @param proNumber
	 * @return 11 digits when it is OK or Validate Exception when length is lower than 9 or greater than 11
	 */
	public static String formatProNbrNumber(
		String proNumber,
		final TransactionContext txnContext) throws ServiceException {

		switch (proNumber.length()) {
		case PRONUM_MAX_LENGTH:
			if (proNumber.contains(SEPARATOR)) {
				proNumber = proNumber.replace(SEPARATOR, String.valueOf(PADDING_ZERO));
			}
			break;
		case PRONUM_LENGTH_TO_VALIDATE:
			if (proNumber.contains(SEPARATOR)) {
				proNumber = PADDING_ZERO + proNumber.replace(SEPARATOR, String.valueOf(PADDING_ZERO));
			} else {
				proNumber = PADDING_ZERO + proNumber;
			}
			break;
		case PRONUM_MIN_LENGTH:
			proNumber = new StringBuilder(proNumber).insert(0, PADDING_ZERO).insert(4, PADDING_ZERO).toString();
			break;
		default:
			logger.error("proNumber lenght is not valid.");
			throw ExceptionBuilder
			.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
			.contextValues(String.format("proNumber: %s ", proNumber))
			.build();
		}

		return proNumber;
	}
}