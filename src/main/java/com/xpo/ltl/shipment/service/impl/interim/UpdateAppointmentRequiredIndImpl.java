package com.xpo.ltl.shipment.service.impl.interim;

import java.beans.PropertyVetoException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ProxyExceptionBuilder;
import com.xpo.ltl.api.shipment.v2.UpdateAppointmentRequiredIndRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.validators.UpdateAppointmentRequiredIndValidator;

import Sh62l000.Abean.Sh62ShpmtUpdateApptInd;

@RequestScoped
@LogExecutionTime("info")
public class UpdateAppointmentRequiredIndImpl {

	@Inject
    private AppContext appContext;

	@Inject
	private UpdateAppointmentRequiredIndValidator updateAppointmentRequiredIndValidator;

    public static final String PROXY_VALUE_FOR_FALSE = "N";
    public static final String PROXY_VALUE_FOR_TRUE = "Y";

	private static final Log LOGGER = LogFactory.getLog(UpdateAppointmentRequiredIndImpl.class);

	/**
	 * Call SH62 to update Appointment Required Indicator on DB2 SHM_SHIPMENT.
	 */
    public void updateAppointmentRequiredInd(UpdateAppointmentRequiredIndRqst request,
        TransactionContext txnContext)
			throws ServiceException {

    	updateAppointmentRequiredIndValidator.validate(request, txnContext);


    	Sh62ShpmtUpdateApptInd proxyBean = new Sh62ShpmtUpdateApptInd();

		try {
			proxyBean.clear();
            proxyBean.setComCfg(appContext.getComCfgDistGen());
			if (txnContext != null && txnContext.getUser() != null && txnContext.getUser().getUserId() != null) {
				proxyBean.setInControlIshs1SharedServicesLongUserid(StringUtils.substring(txnContext.getUser().getUserId(), 0, 15)); // Proxy accepts max 15 length
			} else {
				proxyBean.setInControlIshs1SharedServicesLongUserid("testUser");
			}

			proxyBean.setInIshm1ShipmentInstId(request.getShipmentInstId());
			proxyBean.setInIshm1ShipmentApptRqrdInd(BasicTransformer.toString(request.getAppointmentRequiredInd()));

			proxyBean.execute();

			ProxyExceptionBuilder
			.exception(proxyBean.getOutErrorIshs1SharedServicesOriginServerId(),
					proxyBean.getOutErrorIshs1SharedServicesReturnCd(),
					proxyBean.getOutErrorIshs1SharedServicesReasonCd(), txnContext)
			.contextString(proxyBean.getOutErrorIshs1SharedServicesContextStringTx())
			.log()
			.throwIfException();

			LOGGER.info(String.format("SH62 call finished for shpInstId: ", request.getShipmentInstId()));
		} catch (PropertyVetoException e) {
			throw com.xpo.ltl.api.exception.ExceptionBuilder
			.exception(com.xpo.ltl.api.exception.ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e)
			.moreInfo("ProxyHelper.Sh62ShpmtUpdateApptInd: property veto exception occurred", e.getMessage())
			.build();
		}

	}

}
