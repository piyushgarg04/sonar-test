package com.xpo.ltl.shipment.service.client;

import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.location.v2.GetLocationReferenceDetailsResp;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.java.util.cityoperations.executors.Task;

public class GetLocationReferenceDetailsTask implements Task<GetLocationReferenceDetailsResult>  {
	private final ExternalRestClient client;
	private final TransactionContext txnContext;
    private final String sicCd;

	public GetLocationReferenceDetailsTask(String sicCd, 
    ExternalRestClient client, TransactionContext txnContext) {

		this.client = client;
		this.sicCd = sicCd;
		this.txnContext = txnContext;
	}

	@Override
	public String getName() {
		return BasicTransformer.toJson(sicCd);
	}

	@Override
	public GetLocationReferenceDetailsResult execute() throws ServiceException {
        String sltCountryCd = StringUtils.EMPTY;
        GetLocationReferenceDetailsResp resp = null;
        resp = client.getLocationReferenceDetails(sicCd, txnContext);
        if(null != resp 
        && null != resp.getLocationReference()
        && null != resp.getLocationReference().getSltCountryCd()){
            sltCountryCd = resp.getLocationReference().getSltCountryCd();
        }
        return new GetLocationReferenceDetailsResult(sicCd, sltCountryCd, "Success");
	}

	@Override
	public GetLocationReferenceDetailsResult handleFailure(Exception exception) {
		return new GetLocationReferenceDetailsResult(sicCd, null, exception.toString());
	}
}
