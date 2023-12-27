package com.xpo.ltl.shipment.service.util;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Objects;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import com.xpo.ltl.api.transformer.BasicTransformer;

public class TimestampUtil {

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd hh:mm:ss");
	private static FastDateFormat DATE_HYPHEN = FastDateFormat.getInstance("yyyy-MM-dd");
    private static final String LOW_TIMESTAMP = "0001-01-01 00:00:00";
    private static final String HIGH_TIMESTAMP = "2999-12-31 23:59:59";
    private static final Timestamp LAST_DAY_OF_YEAR_ONE_BCE = new Timestamp(-62135740800001L);
    private static final Timestamp LAST_DAY_OF_YEAR_ONE_CE = new Timestamp(-62104204800001L);

    private static Timestamp lowTimestamp;
    private static XMLGregorianCalendar lowCalendar;
    private static Timestamp highTimestamp;
    private static XMLGregorianCalendar highCalendar;

    private TimestampUtil() {
    }

    public static boolean hasInvalidTimestampFormat(XMLGregorianCalendar target) {
        if (target != null) {
            try {
                BasicTransformer.toTimestamp(target);
                return false;
            } catch (RuntimeException e) {
                return true;
            }
        }
        return false;
    }

    public static Timestamp getLowTimestamp() {
        if (lowTimestamp == null) {
            try {
                lowTimestamp =
                    new Timestamp(DATE_FORMAT.parse(LOW_TIMESTAMP).getTime());
            }
            catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        return lowTimestamp;
    }

    public static XMLGregorianCalendar getLowCalendar() {
        if (lowCalendar == null) {
            lowCalendar =
                BasicTransformer.toXMLGregorianCalendar(getLowTimestamp());
        }
        return lowCalendar;
    }

    public static Timestamp getHighTimestamp() {
        if (highTimestamp == null) {
            try {
                highTimestamp =
                    new Timestamp(DATE_FORMAT.parse(HIGH_TIMESTAMP).getTime());
            }
            catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        return highTimestamp;
    }

    public static XMLGregorianCalendar getHighCalendar() {
        if (highCalendar == null) {
            highCalendar =
                BasicTransformer.toXMLGregorianCalendar(getHighTimestamp());
        }
        return highCalendar;
    }

    public static String toStringDateFormat(String date, FastDateFormat fromFormat, FastDateFormat toFormat) throws ParseException {
		if (StringUtils.isBlank(date)) {
			return null;
		}
    	Date d = fromFormat.parse(date);
    	return toFormat.format(d);
    }

	public static XMLGregorianCalendar stringToXmlGregorianCalendar(final String strDate, final String inPattern) {
		XMLGregorianCalendar calendar = null;
		try {
			if (strDate == null)
				return null;

			final GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(new SimpleDateFormat(inPattern).parse(strDate));
			calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
		} catch (final ParseException e) {
			throw new RuntimeException(e);
		} catch (final DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}

		return calendar;
	}

    public static XMLGregorianCalendar toXmlGregorianCalendar(final Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }

		try {
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTime(timestamp);
			return DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

    /**
     * Creates a Timestamp object with the last day of first year, this correspond to the date "CommonEra 0001-12-31
     * 23:59:59".
     * Used to compare if a Timestamp belong to year 0001.
     */
    public static Timestamp getLastDayOfYearOneCommonEraTimestamp() {
        return LAST_DAY_OF_YEAR_ONE_CE;
    }

    /**
     * Creates a Timestamp object with the last day of first year, this correspond to the date "BeforeCommonEra
     * 0001-12-31 23:59:59".
     * Used to compare if a Timestamp belong to year 0001.
     */
    public static Timestamp getLastDayOfYearOneBeforeCommonEraTimestamp() {
        return LAST_DAY_OF_YEAR_ONE_BCE;
    }

    /**
     * Method to determine whether a timestamp belongs to year 0001
     */
    public static boolean isLowTimestamp(Timestamp ts) {
        return ts.after(getLastDayOfYearOneBeforeCommonEraTimestamp())
                && ts.before(getLastDayOfYearOneCommonEraTimestamp());
    }
	public static Date getDate(String date, String dateFormat) throws ParseException {
		if (StringUtils.isNotEmpty(date)) {
			SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
			return sdf.parse(date);
		}
		return null;
	}
	public static String getStringDate(Date date, String dateFormat) throws ParseException {
		if (Objects.nonNull(date)) {
			SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
			return sdf.format(date);
		}
		return null;
	}
}
