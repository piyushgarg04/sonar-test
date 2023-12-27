package com.xpo.ltl.shipment.service.client;

import java.io.Serializable;

@SuppressWarnings("serial")
public class GetLocationReferenceDetailsResult implements Serializable{
    private String sicCd;
    private String sltCountryCd;
	private String message;

	public GetLocationReferenceDetailsResult() {
	}
	public GetLocationReferenceDetailsResult(String sicCd, String sltCountryCd, String message) {
		this.sicCd = sicCd;
        this.sltCountryCd = sltCountryCd;
		this.message = message;
    }
	public String getSicCd() {
		return sicCd;
	}
    public String getSltCountryCd(){
        return sltCountryCd;
    }
	public String getMessage() {
		return message;
	}
}
