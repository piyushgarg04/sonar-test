package com.xpo.ltl.shipment.service.client;

import java.io.Serializable;

import com.xpo.ltl.api.shipment.v2.ShipmentSkeletonResponse;

@SuppressWarnings("serial")
public class ShipmentSkeletonLegacyUpdatesResult implements Serializable {

	private ShipmentSkeletonResponse shipmentSkeletonResponse;
	private String message;

	public ShipmentSkeletonLegacyUpdatesResult() {
	}

	public ShipmentSkeletonLegacyUpdatesResult(ShipmentSkeletonResponse shipmentSkeletonResponse, String message) {
		this.shipmentSkeletonResponse = shipmentSkeletonResponse;
		this.message = message;

	}

	public ShipmentSkeletonResponse getShipmentSkeletonResponse() {
		return shipmentSkeletonResponse;
	}

	public String getMessage() {
		return message;
	}

}
