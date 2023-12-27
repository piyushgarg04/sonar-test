package com.xpo.ltl.shipment.service.client;

import java.io.Serializable;

import com.xpo.ltl.api.freightflow.v2.PostalTransitTime;

@SuppressWarnings("serial")
public class CalculateTransitTimeResult implements Serializable {
	
	private String proNbr;
	private PostalTransitTime transitTimeResp;
	private String message;

	public CalculateTransitTimeResult(String proNbr, PostalTransitTime transitTimeResp, String message) {
		this.proNbr = proNbr;
		this.transitTimeResp = transitTimeResp;
		this.message = message;
	}

	public String getProNbr() {
		return proNbr;
	}
	
	public PostalTransitTime getTransitTimeResp() {
		return transitTimeResp;
	}

	public String getMessage() {
		return message;
	}
}