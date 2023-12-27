package com.xpo.ltl.shipment.service.client;

import java.util.List;

import com.xpo.ltl.api.freightflow.v2.CalculateTransitTimeResp;
import com.xpo.ltl.api.freightflow.v2.CalculateTransitTimeRqst;
import com.xpo.ltl.api.freightflow.v2.PostalTransitTime;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.java.util.cityoperations.executors.Task;

public class CalculateTransitTimesTask implements Task<CalculateTransitTimesResult> {
	
	private final CalculateTransitTimeRqst calculateTransitTimeRqst;
	private final ExternalRestClient client;
	private final TransactionContext txnContext;
	

	public CalculateTransitTimesTask(CalculateTransitTimeRqst calculateTransitTimeRqst, 
			ExternalRestClient client, TransactionContext txnContext) {

		this.calculateTransitTimeRqst = calculateTransitTimeRqst;
		this.client = client;
		this.txnContext = txnContext;
	}


	@Override
	public CalculateTransitTimesResult execute() throws Exception {
		
		List<PostalTransitTime> transiteTimeResp = null;
		CalculateTransitTimeResp response = new CalculateTransitTimeResp();
		
		response =client.calculateTransitTime(calculateTransitTimeRqst, txnContext);
		
		if(null != response && null != response.getTransitTime()) {
			transiteTimeResp =  response.getTransitTime();
        }
		return new CalculateTransitTimesResult(transiteTimeResp ,"Success");
	}

//	@Override
//	public String getName() {
//		return BasicTransformer.toJson(proNbr);
//	}

	@Override
	public CalculateTransitTimesResult handleFailure(Exception exception) {
		return new CalculateTransitTimesResult(null, exception.toString());
	}


    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

}