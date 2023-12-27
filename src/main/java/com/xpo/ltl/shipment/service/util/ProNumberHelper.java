package com.xpo.ltl.shipment.service.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;

/**
 * Pro Number Helper.-
 *
 * @author msanguinetti
 *
 */
public class ProNumberHelper {

	private static final Log logger = LogFactory.getLog(ProNumberHelper.class);

	private static final char PADDING_ZERO = '0';
	private static final String HYPHEN = "-";
	private static final String REGEX_9_DIGIT = "[0-9]\\d{8}";
	private static final String REGEX_9_DIGIT_HYPHEN_BLUE_PRO = "[0-9]\\d{2}-\\d{6}";
	private static final String REGEX_10_DIGIT_HYPHEN_BLUE_PRO = "0[0-9]\\d{2}-\\d{6}";
	private static final String REGEX_11_DIGIT_BLUE_PRO = "0[0-9]\\d{2}0\\d{6}";
	private static final String REGEX_11_DIGIT_YELLOW_PRO = "0[0-9]\\d{2}[1-9]\\d{6}";
	private static final String REGEX_10_DIGIT_YELLOW_PRO = "[0-9]\\d{2}[1-9]\\d{6}";
	private static final String REGEX_10_DIGIT_HYPHEN_YELLOW_PRO = "[0-9]\\d{2}[1-9]-\\d{6}";

	/**
	 * Check proNumber length and validate it contains separator, add zeros when is necessary
	 *
	 * @param proNumber
	 * @return 11 digits when it is OK or Validation Exception when input Pro Number is not valid
	 */
	public static String validateProNumber(String proNumber, final TransactionContext txnContext)
			throws ServiceException {

		if (StringUtils.isBlank(proNumber)){
			throw ExceptionBuilder
					.exception(ValidationErrorMessage.PRO_NBR_RQ, txnContext)
					.moreInfo("proNumber", proNumber)
					.build();
		}

		validateCheckDigit(proNumber, txnContext);
		return toElevenDigitPro(proNumber, txnContext);
	}

	public static void validateCheckDigit(String proNumber, TransactionContext txnContext)
			throws ValidationException {

		int end = 8, checkDigit, expectedCheckDigit;

		if (isBluePro(proNumber)) {
			String nineDigitPro = toNineDigitPro(proNumber, txnContext);
			checkDigit = Integer.parseInt(nineDigitPro.substring(end, end + 1));
			expectedCheckDigit = Integer
					.parseInt(nineDigitPro.substring(0, end)) % 7;

			if (checkDigit != expectedCheckDigit) {
				throw ExceptionBuilder
						.exception(ValidationErrorMessage.PRO_NBR_CHK_DIGIT_ERROR, txnContext)
						.moreInfo("proNumber", proNumber)
						.build();
			}


		} else if (isYellowPro(proNumber)){
			end = 10;
			String elevenDigitPro = toElevenDigitPro(proNumber, txnContext);
			checkDigit = Integer.parseInt(elevenDigitPro.substring(end, end + 1));
			expectedCheckDigit = Integer
					.parseInt(elevenDigitPro.substring(1, end)) % 7;

			if (checkDigit != expectedCheckDigit) {
				throw ExceptionBuilder
						.exception(ValidationErrorMessage.PRO_NBR_CHK_DIGIT_ERROR, txnContext)
						.moreInfo("proNumber", proNumber)
						.build();
			}

		} else {
			throw ExceptionBuilder
					.exception(ValidationErrorMessage.PRO_NBR_FORMAT_INVALID, txnContext)
					.moreInfo("proNumber", proNumber)
					.build();
		}

	}

	/**
	 * Check proNumber verify it has valid format for Blue Pro and convert to nine digit format
	 *
	 * @param proNumber
	 * @return String - the corresponding 9 digit formatted Pro Number
	 */
	public static String toNineDigitPro(String proNumber, TransactionContext txnContext)
			throws ValidationException {

		if (isBluePro(proNumber)){
			if (proNumber.matches(REGEX_9_DIGIT_HYPHEN_BLUE_PRO)){
				proNumber = proNumber.substring(0, 3) + proNumber.substring(4, 10);
			} else if (proNumber.matches(REGEX_10_DIGIT_HYPHEN_BLUE_PRO) || proNumber.matches(REGEX_11_DIGIT_BLUE_PRO)){
				proNumber = proNumber.substring(1, 4) + proNumber.substring(5, 11);
			}
			return proNumber;

		} else {
			throw ExceptionBuilder
					.exception(ValidationErrorMessage.PRO_NBR_FORMAT_INVALID, txnContext)
					.moreInfo("proNumber", proNumber)
					.build();
		}

	}

	/**
	 * Check proNumber verify it has valid format and convert to eleven digit format
	 *
	 * @param proNumber
	 * @return String - the corresponding eleven digit formatted Pro Number
	 */
	public static String toElevenDigitPro(String proNumber, TransactionContext txnContext)
			throws ValidationException {

		if (isElevenDigit(proNumber)) {
			return proNumber;
		}

		if (isBluePro(proNumber)){
			proNumber = StringUtils.trimToEmpty(proNumber);
			String nineDigitPro;
			if (proNumber.matches(REGEX_9_DIGIT_HYPHEN_BLUE_PRO) || proNumber.matches(REGEX_10_DIGIT_HYPHEN_BLUE_PRO)) {
				nineDigitPro = toNineDigitPro(proNumber, txnContext);
				proNumber = new StringBuilder(nineDigitPro).insert(0, PADDING_ZERO).insert(4,
						PADDING_ZERO).toString();
			} else if (proNumber.matches(REGEX_9_DIGIT)) {
				nineDigitPro = proNumber;
				proNumber = new StringBuilder(nineDigitPro).insert(0, PADDING_ZERO).insert(4,
						PADDING_ZERO).toString();
			}
			return proNumber;

		} else if (isYellowPro(proNumber)) {
			if (proNumber.matches(REGEX_10_DIGIT_HYPHEN_YELLOW_PRO)){
				proNumber = StringUtils.remove(proNumber, HYPHEN);
			}
			proNumber = PADDING_ZERO + proNumber;
			return proNumber;
		} else {
			throw ExceptionBuilder
					.exception(ValidationErrorMessage.PRO_NBR_FORMAT_INVALID, txnContext)
					.moreInfo("proNumber", proNumber)
					.build();
		}

	}

	public static boolean isElevenDigit(String proNumber) {
		proNumber = StringUtils.trimToEmpty(proNumber);
		return proNumber.matches(REGEX_11_DIGIT_YELLOW_PRO) || proNumber.matches(REGEX_11_DIGIT_BLUE_PRO);
	}

	/**
	 * Check proNumber verify it has valid format for Blue Pro or Parent Pro Numbers
	 *
	 * @param proNumber
	 * @return boolean
	 */
	public static boolean isBluePro(String proNumber) {
		proNumber = StringUtils.trimToEmpty(proNumber);
		return (proNumber.matches(REGEX_9_DIGIT) || proNumber.matches(REGEX_9_DIGIT_HYPHEN_BLUE_PRO)
				|| proNumber.matches(REGEX_10_DIGIT_HYPHEN_BLUE_PRO) || proNumber.matches(REGEX_11_DIGIT_BLUE_PRO));
	}

	/**
	 * Check proNumber verify it has valid format for Yellow Pro or Tracking Pro Numbers
	 *
	 * @param proNumber
	 * @return boolean
	 */
	public static boolean isYellowPro(String proNumber) {
		proNumber = StringUtils.trimToEmpty(proNumber);
		return (proNumber.matches(REGEX_10_DIGIT_YELLOW_PRO) || proNumber.matches(REGEX_10_DIGIT_HYPHEN_YELLOW_PRO)
				|| proNumber.matches(REGEX_11_DIGIT_YELLOW_PRO));
	}

	public static String toNineDigitProHyphen(String proNbr, TransactionContext txnContext) throws ValidationException {
		String proNumber = toNineDigitPro(proNbr, txnContext);
		proNumber = proNumber.substring(0, 3) + "-" + proNumber.substring(3, 9);
		return proNumber;
	}
	
	/**
	 * Format Nine Digit Pro Number 
	 *
	 * @param proNumber
	 * @return String - 9 digit formatted pro number if input > 9
	 */
		
	public static String formatProNineDigit(final String proNumber) {

		if (StringUtils.isEmpty(proNumber) || (proNumber.trim().length() < 9)) {
			return proNumber;
		}

		if (proNumber.trim().length() == 9) {
			return proNumber.trim();
		}

		if (proNumber.trim().length() == 11) {
			return proNumber.trim().substring(1, 4) + proNumber.trim().substring(5);
		}

		if (proNumber.trim().length() == 10) {
			return proNumber.trim().replace("-", "");
		}
		return proNumber.trim();

	}

	/**
	 * Format Eleven Digit Pro Number to Ten Digit Pro Number
	 * @param childProNbr
	 * @return String - the corresponding ten digit formatted Pro Number
	 */
	public static String toTenDigitPro(String proNumber) {
		String result = null;
		if (isElevenDigit(proNumber)) {
			result = proNumber.trim().substring(1, 5) + "-" + proNumber.trim().substring(5);
		}

		return result;
	}

	public static String isValidChildProNum(String proNumber) {
		String trimmedPro = proNumber.trim().replace("-", "");
		if (trimmedPro.length() > 9) {
			if(trimmedPro.length() == 11) {
				trimmedPro = trimmedPro.substring(1);
			}
			long childProF = Long.valueOf(trimmedPro.substring(0, 9));
			long childProS =  Long.valueOf(trimmedPro.substring(9));
			if(childProF%7 == childProS) {
				return String.valueOf(childProF).concat(String.valueOf(childProS));
			}
		}
		return null; 
	}
	
	public static String getProNumPrefix(String proNum) {
		String nineDigitProNum = formatProNineDigit(proNum);
		return nineDigitProNum.trim().substring(0, 3);
	}
	
	   /**
    *
    * @param proWithMvrSfx
    *            e.g. 01230123456A.
    * @return 123123456A
    */
   public static String formatMvrProNineDigit(String proWithMvrSfx) {
       int length = proWithMvrSfx.length();
       if (length < 1) {
           return StringUtils.EMPTY;
       }

       String proNineDigit = ProNumberHelper.formatProNineDigit(proWithMvrSfx.substring(0, length - 1));
       String mvrSuffix = proWithMvrSfx.substring(length - 1, length);
       return proNineDigit.concat(mvrSuffix);
   }

   /**
    * 01230123456 returns false;
    * 01230123456A returns true;
    */
   public static boolean isMvrProNumber(String proNumber) {
       if (StringUtils.isBlank(proNumber))
           return false;
       return !StringUtils.isNumeric(proNumber);
   }

   /**
    * @param proWithMvrSfx
    *            e.g. 01230123456A
    * @return 01230123456
    */
   public static String getMvrProNumberWithoutSuffix(String proWithMvrSfx) {
       int length = proWithMvrSfx.length();
       if (length < 1) {
           return StringUtils.EMPTY;
       }
       return proWithMvrSfx.substring(0, length - 1);
   }
}
