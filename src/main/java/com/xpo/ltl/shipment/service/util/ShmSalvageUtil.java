package com.xpo.ltl.shipment.service.util;

import java.util.HashMap;
import java.util.Map;

public class ShmSalvageUtil {
	
	public static String getOldDBRequestTypeValue(String code) {
		Map<String, String> requestTypeMap = getRequestTypeMap();
		return requestTypeMap.get(code);
	}
	
	public static String getOldDBRequestStausValue(String code) {
		Map<String, String> requestStatusMap = getRequestStatusMap();
		return requestStatusMap.get(code);
	}
	
	private static Map<String, String> getRequestStatusMap(){
		Map<String, String> requestStatusMap = new HashMap<>();
		requestStatusMap.put("APPROVED_CMK", "Approved for Salvage");
		requestStatusMap.put("APPROVED_CVS", "Approved for Salvage");
		requestStatusMap.put("RTN_TO_CUSTOMER", "Closed: return-to-customer");
		requestStatusMap.put("CLOSED", "Closed");
		requestStatusMap.put("APRVD_CMN_DSPL", "Approved for Common Disposal");
		requestStatusMap.put("APRVD_RGLD_DSPL", "CMK: Dangerous; Haz-Mat; Regulated Goods");
		requestStatusMap.put("APRVD_CO_USE", "Approved for Company Use");
		requestStatusMap.put("INSP_REPORT", "PHYSICAL INSPECTION REQUIRED (open request for instructions)");
		requestStatusMap.put("RQST_PHOTO", "Request for Photos");
		requestStatusMap.put("RQST_FEEDBACK", "Request for Feedback");
		requestStatusMap.put("SBMTD_FEEDBACK", "Request for Feedback");
		requestStatusMap.put("SUBMITTED", "Open");
		requestStatusMap.put("CLOSED_NOT_APPROVED", "Closed - Moved to CMK Without SAP Authority");
		return requestStatusMap;
	}
	
	private static Map<String, String> getRequestTypeMap(){
		Map<String, String> requestTypeMap = new HashMap<>();
		requestTypeMap.put("CA_SALVAGE", "Move to CSV (Canada)");
		requestTypeMap.put("COMMON_DISPOSAL", "Common Disposal");
		requestTypeMap.put("COMPANY_USE", "Company Use");
		requestTypeMap.put("REGULATED_DISPOSAL", "Haz-Mat/Regulated Disposal");
		requestTypeMap.put("US_SALVAGE", "Move to CMK (US)");
		
		return requestTypeMap;
	}
	
	
}
