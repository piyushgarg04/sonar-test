package com.xpo.ltl.shipment.service.impl;

import java.sql.Time;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import javax.enterprise.context.RequestScoped;

import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.exception.ExceptionBuilder;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationErrorMessage;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.v2.AccessorialCheckCd;
import com.xpo.ltl.api.shipment.v2.DetermineAccessorialServiceConflictResp;
import com.xpo.ltl.api.shipment.v2.DetermineAccessorialServiceConflictRqst;
import com.xpo.ltl.api.shipment.v2.ShmNotification;
import com.xpo.ltl.api.shipment.v2.TimeDateCritical;
import com.xpo.ltl.api.transformer.BasicTransformer;

@RequestScoped
public class DetermineAccessorialServiceConflictImpl {

	private static final String DT_TYP_CD_BY = "BY";
	private static final String DT_TYP_CD_ON = "ON";
    private static final String TM_TYP_CD_AFTER = "AFT";
    private static final String TM_TYP_CD_BEFORE = "BEF";
    private static final Long MIN_OVERLAP_HOURS = 1L;

	public DetermineAccessorialServiceConflictResp determineAccessorialServiceConflict(
			final DetermineAccessorialServiceConflictRqst determineAccessorialServiceConflictRqst,
			final TransactionContext txnContext) throws ServiceException {

		Boolean conflictFoundInd = Boolean.FALSE;

		if (AccessorialCheckCd.TDC_VS_APT.equals(determineAccessorialServiceConflictRqst.getAccessorialCheckCd())) {
			conflictFoundInd = determineConflictForTDCvsAPT(determineAccessorialServiceConflictRqst, txnContext);
		}

		DetermineAccessorialServiceConflictResp response = new DetermineAccessorialServiceConflictResp();
		response.setConflictFoundInd(conflictFoundInd);
		return response;
	}

	private Boolean determineConflictForTDCvsAPT(
			final DetermineAccessorialServiceConflictRqst determineAccessorialServiceConflictRqst,
			final TransactionContext txnContext) throws ValidationException {

		Boolean conflictFoundInd = Boolean.FALSE;

		// when the Notication date is not within the TDC from/to date. And when the
		// Notification date is within the TDC from/to date, there's a conflict if the
		// Notification from/to time does not overlap for at least an hour.

		final ShmNotification notification = determineAccessorialServiceConflictRqst.getNotification();
		final TimeDateCritical timeDateCritical = determineAccessorialServiceConflictRqst.getTimeDateCritical();

		if (notification == null && timeDateCritical == null) {
			MoreInfo moreInfo = new MoreInfo();
			moreInfo.setMessage("Notification and Time Date Critical cannot be both null");
			moreInfo.setLocation(
					"determineAccessorialServiceConflictRqst.notification and determineAccessorialServiceConflictRqst.timeDateCritical");
			throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo(moreInfo).build();
		} else if (notification == null || timeDateCritical == null) {
			return conflictFoundInd;
		}

		if (timeDateCritical != null) {
	        setupTimeDateCriticalRange(timeDateCritical);
		}

		if (notification != null) {
			if (StringUtils.isNotBlank(notification.getScheduledDeliveryFromTime())
					&& ("00:00:00").equals(notification.getScheduledDeliveryFromTime())) {
				notification.setScheduledDeliveryFromTime(null);
			}

			if (StringUtils.isNotBlank(notification.getScheduledDeliveryToTime())
					&& ("00:00:00").equals(notification.getScheduledDeliveryToTime())) {
				notification.setScheduledDeliveryToTime(null);
			}
		}

		Date notificationDate = BasicTransformer.toDate(notification.getScheduledDeliveryDate());
		Date tdcFromDate = BasicTransformer.toDate(timeDateCritical.getTdcDate1());
		Date tdcToDate = BasicTransformer.toDate(timeDateCritical.getTdcDate2());
		Time notificationFromTime = BasicTransformer.toTime(notification.getScheduledDeliveryFromTime());
		Time notificationToTime = BasicTransformer.toTime(notification.getScheduledDeliveryToTime());
		Time tdcFromTime = BasicTransformer.toTime(timeDateCritical.getTdcTime1());
		Time tdcToTime = BasicTransformer.toTime(timeDateCritical.getTdcTime2());

		if (notificationFromTime == null && notificationToTime == null) {
			conflictFoundInd = isOutsideDateRange(notificationDate, tdcFromDate, tdcToDate);
		} else {

			if (notificationToTime == null) {
				notificationToTime = notificationFromTime;
			} else if (notificationFromTime == null) {
				notificationFromTime = notificationToTime;
			}

			conflictFoundInd = isNotOverlapping(
					notificationDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
							.atTime(notificationFromTime.toLocalTime()),
					notificationDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
							.atTime(notificationToTime.toLocalTime()),
					tdcFromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
							.atTime(tdcFromTime.toLocalTime()),
					tdcToDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().atTime(tdcToTime.toLocalTime()));

		}

		return conflictFoundInd;
	}

	private void setupTimeDateCriticalRange(TimeDateCritical timeDateCritical) {
	    
	    if(DT_TYP_CD_BY.equalsIgnoreCase(timeDateCritical.getTdcDateTypeCd())) {
            //Date range is from earliest possible date to TDC BY date
	        if (timeDateCritical.getTdcDate2().equals("0001-01-01")) {
	            timeDateCritical.setTdcDate2(timeDateCritical.getTdcDate1());
	            timeDateCritical.setTdcDate1("0001-01-01");
	        }
	    } else if(DT_TYP_CD_ON.equalsIgnoreCase(timeDateCritical.getTdcDateTypeCd())) {
	      //Date range is for the exact TDC ON date
            if (timeDateCritical.getTdcDate2().equals("0001-01-01")) {
                timeDateCritical.setTdcDate2(timeDateCritical.getTdcDate1());
            }
        }
	    
	    if(TM_TYP_CD_BEFORE.equalsIgnoreCase(timeDateCritical.getTdcTimeTypeCd())) {
	      //Time range is from earliest possible time to TDC BEFORE time
            if (timeDateCritical.getTdcTime2().equals("00:00:00")) {
	            timeDateCritical.setTdcTime2(timeDateCritical.getTdcTime1());
                timeDateCritical.setTdcTime1("00:00:00");

	        }
	    } else if(TM_TYP_CD_AFTER.equalsIgnoreCase(timeDateCritical.getTdcTimeTypeCd())) {
	      //Time range is from TDC BEFORE time to latest possible time
	        if (timeDateCritical.getTdcTime2().equals("00:00:00")) {
                timeDateCritical.setTdcTime2("23:59:00");
            }
        } else if (StringUtils.isBlank(timeDateCritical.getTdcTimeTypeCd())) {
          //No time range defined, assume all day
            timeDateCritical.setTdcTime1("00:00:00");
            timeDateCritical.setTdcTime2("23:59:00");
        }
    }

    private Boolean isOutsideDateRange(final Date date, final Date from, final Date to) {
		return (date.after(to) || date.before(from)) && date.compareTo(from) != 0 && date.compareTo(to) != 0;
	}

	private Boolean isNotOverlapping(final LocalDateTime aptFrom, final LocalDateTime aptTo,
			final LocalDateTime tdcFrom, final LocalDateTime tdcTo) {

		if (aptFrom.isBefore(tdcFrom)) {
			if (aptTo.isBefore(tdcFrom)) {
				// APT Range is before than TDC range, NO OVERLAPPING - CONFLICT
				return Boolean.TRUE;
			} else if (aptTo.isAfter(tdcTo) || aptTo.compareTo(tdcTo) == 0) {
				// APT Range includes whole TDC Range, THERE IS OVERLAPPING
				// CONFLICT is the overlaps is least that 1 hour
				return Duration.between(tdcFrom, tdcTo).toHours() < MIN_OVERLAP_HOURS;
			} else {
				// tdcFrom is between aptFrom and aptTo, THERE IS OVERLAPPING
				// CONFLICT is the overlaps is least that 1 hour
				return Duration.between(tdcFrom, aptTo).toHours() < MIN_OVERLAP_HOURS;
			}
		} else if (tdcFrom.isBefore(aptFrom)) {
			if (tdcTo.isBefore(aptFrom)) {
				// TDC Range is before than APT Range, NO OVERLAPPING - CONFLICT
				return Boolean.TRUE;
			} else if (aptTo.isBefore(tdcTo) || aptTo.compareTo(tdcTo) == 0) {
				// TDC Range includes whole APT Range, THERE IS OVERLAPPING - NO CONFLICT
				return Boolean.FALSE;
			} else {
				// aptFrom between tdcFrom and tdcTo, THERE IS OVERLAPPING
				// CONFLICT is the overlaps is least that 1 hour
				return Duration.between(aptFrom, tdcTo).toHours() < MIN_OVERLAP_HOURS;
			}
		} else if (aptFrom.compareTo(tdcFrom) == 0) {
			if (aptTo.compareTo(tdcTo) == 0) {
				// Both ranges are equals, THERE IS OVERLAPPING
				return Boolean.FALSE;
			} else if (aptTo.isBefore(tdcTo)) {
				// TDC Range includes whole APT Range, THERE IS OVERLAPPING - NO CONFLICT
				return Boolean.FALSE;
			} else {
				// APT Range includes whole TDC Range, THERE IS OVERLAPPING
				// CONFLICT is the overlaps is least that 1 hour
				return Duration.between(tdcFrom, tdcTo).toHours() < MIN_OVERLAP_HOURS;
			}
		}

		return Boolean.FALSE;

	}
}
