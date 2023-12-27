package com.xpo.ltl.shipment.service.client;

import com.xpo.ltl.api.freightflow.v2.CalculateTransitTimeResp;
import com.xpo.ltl.api.freightflow.v2.CalculateTransitTimeRqst;
import com.xpo.ltl.api.freightflow.v2.PostalTransitTime;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.java.util.cityoperations.executors.Task;

public class CalculateTransitTimeTask implements Task<CalculateTransitTimeResult> {
	
	private final CalculateTransitTimeRqst calculateTransitTimeRqst; 
	private final String proNbr;
	private final ExternalRestClient client;
	private final TransactionContext txnContext;
	

	public CalculateTransitTimeTask(CalculateTransitTimeRqst calculateTransitTimeRqst, String proNbr,
			ExternalRestClient client, TransactionContext txnContext) {

		this.calculateTransitTimeRqst = calculateTransitTimeRqst;
		this.proNbr = proNbr;
		this.client = client;
		this.txnContext = txnContext;
	}


	@Override
	public CalculateTransitTimeResult execute() throws Exception {
		
		PostalTransitTime transiteTimeResp = null;
		CalculateTransitTimeResp response = new CalculateTransitTimeResp();
		
		response =client.calculateTransitTime(calculateTransitTimeRqst, txnContext);
		
		if(null != response && null != response.getTransitTime()) {
			transiteTimeResp =  response.getTransitTime().get(0);
        }
		return new CalculateTransitTimeResult(proNbr,transiteTimeResp ,"Success");
	}

	@Override
	public String getName() {
		return BasicTransformer.toJson(proNbr);
	}

	@Override
	public CalculateTransitTimeResult handleFailure(Exception exception) {
		return new CalculateTransitTimeResult(proNbr, null, exception.toString());
	}
}