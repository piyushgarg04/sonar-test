package com.xpo.ltl.shipment.service.client;

import java.io.Serializable;
import java.util.List;

import com.xpo.ltl.api.freightflow.v2.PostalTransitTime;

@SuppressWarnings("serial")
public class CalculateTransitTimesResult implements Serializable {
	
	private List<PostalTransitTime> transitTimeResp;
	private String message;

	public CalculateTransitTimesResult(List<PostalTransitTime> transitTimeResp, String message) {
		this.transitTimeResp = transitTimeResp;
		this.message = message;
	}

	public List<PostalTransitTime> getTransitTimeResp() {
		return transitTimeResp;
	}

	public String getMessage() {
		return message;
	}
}