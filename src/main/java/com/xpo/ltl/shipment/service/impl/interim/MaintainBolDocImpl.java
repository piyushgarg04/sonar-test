package com.xpo.ltl.shipment.service.impl.interim;

import java.beans.PropertyVetoException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ProxyExceptionBuilder;
import com.xpo.ltl.api.shipment.v2.LateTenderCd;
import com.xpo.ltl.api.shipment.v2.ShipmentSkeletonResponse;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;

import Bo11l000.Abean.Bo11SMaintBolDocHanlding;



@LogExecutionTime("info")
public class MaintainBolDocImpl {

	@Inject
    private AppContext appContext;

	private static final String PROUPDT = "PROUPDT";

	private static final Log LOGGER = LogFactory.getLog(MaintainBolDocImpl.class);

	/**
	 * Call BO11 to update BOL table from DB2 and EAB to update EDI SHP_DIRECT_ID.
	 */
    public void ediBolUpdate(ShipmentSkeletonResponse shipmentSkeletonResponse,
        TransactionContext txnContext)
			throws ServiceException {

		Bo11SMaintBolDocHanlding proxyBean = new Bo11SMaintBolDocHanlding();

		try {
			proxyBean.clear();
			proxyBean.setCommandSent(PROUPDT);
			proxyBean.setComCfg(appContext.getComBridgeConfiguration());
			if (txnContext != null && txnContext.getUser() != null && txnContext.getUser().getUserId() != null) {
				proxyBean.setInControlIshs1SharedServicesLongUserid(StringUtils.substring(txnContext.getUser().getUserId(), 0, 15)); // Proxy accepts max 15 length
			} else {
				proxyBean.setInControlIshs1SharedServicesLongUserid("testUser");
			}

            proxyBean.setInIbol1DocBolInstId(shipmentSkeletonResponse.getBolInstId() != null ? BasicTransformer.toDouble(BasicTransformer.toBigDecimal(shipmentSkeletonResponse.getBolInstId())) : 0);
			proxyBean.setInIbol1DocProNbrTxt(ProNumberHelper.toNineDigitPro(shipmentSkeletonResponse.getProNbr(), txnContext));
			proxyBean.setInIbol1DocCustSuppliedProNbrTxt(BasicTransformer.toString(BooleanUtils.isTrue(shipmentSkeletonResponse.getLateTenderCd() == LateTenderCd.LATE_TENDER ? true : false)));

			proxyBean.execute();

			ProxyExceptionBuilder
			.exception(proxyBean.getOutErrorIshs1SharedServicesOriginServerId(),
					proxyBean.getOutErrorIshs1SharedServicesReturnCd(),
					proxyBean.getOutErrorIshs1SharedServicesReasonCd(), txnContext)
			.contextString(proxyBean.getOutErrorIshs1SharedServicesContextStringTx())
			.log()
			.throwIfException();

			LOGGER.info(String.format("Bo11 call finished for proNbrs: ", shipmentSkeletonResponse.getProNbr()));
		} catch (PropertyVetoException e) {
			throw com.xpo.ltl.api.exception.ExceptionBuilder
			.exception(com.xpo.ltl.api.exception.ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e)
			.moreInfo("ProxyHelper.ediBolUpdate: property veto exception occurred", e.getMessage())
			.build();
		}

	}
}
