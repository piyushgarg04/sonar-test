package com.xpo.ltl.shipment.service.impl.interim;

import Sh61l000.Abean.Sh61SBaseLogCreate;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ProxyExceptionBuilder;
import com.xpo.ltl.api.shipment.v2.BaseLog;
import com.xpo.ltl.api.shipment.v2.CreateBaseLogRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.enums.BaseLogTypeEnum;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import com.xpo.ltl.shipment.service.validators.CreateBaseLogValidator;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.List;

@RequestScoped
@LogExecutionTime("info")
public class CreateBaseLogImpl {
	
	@Inject
	private AppContext appContext;
	
	@Inject
	private CreateBaseLogValidator createBaseLogValidator;

	private static final Logger LOGGER = LogManager.getLogger(CreateBaseLogImpl.class);

	public void createBaseLog(CreateBaseLogRqst createBaseLogRqst, String logId, TransactionContext txnContext) throws ServiceException {
		
		createBaseLogValidator.validate(createBaseLogRqst,logId, txnContext);
		final List<BaseLog> baseLogs =createBaseLogRqst.getBaseLogs();
		final Sh61SBaseLogCreate proxyBean = new Sh61SBaseLogCreate();
		
		try {
			proxyBean.clear();
			final String commandBase = getCommandBase(logId);
			proxyBean.setCommandSent(commandBase);
			
			if (BaseLogTypeEnum.BASE_LOG_30.getCode().equals(logId) || BaseLogTypeEnum.BASE_LOG_38.getCode().equals(logId) ||BaseLogTypeEnum.BASE_LOG_3A.getCode().equals(logId)) {
				proxyBean.setInEventIshm1EventSubTypCd(String.valueOf(logId));
			}
			
			proxyBean.setComCfg(appContext.getComBridgeConfiguration());
			if (txnContext != null && txnContext.getUser() != null && txnContext.getUser().getUserId() != null) {
				proxyBean.setInControlIshs1SharedServicesLongUserid(StringUtils.substring(txnContext.getUser().getUserId(), 0, 15)); // Proxy accepts max 15 length
			} else {
				proxyBean.setInControlIshs1SharedServicesLongUserid("testUser");
			}
			for (BaseLog baseLog : baseLogs)
			{
				if(ObjectUtils.isNotEmpty(baseLog.getShipmentId())) {
					if (StringUtils.isNotBlank(baseLog.getShipmentId().getShipmentInstId())) {
						proxyBean.setInIshm1ShipmentInstId(Double.parseDouble(baseLog.getShipmentId().getShipmentInstId()));
					} else {
						proxyBean.setInIshm1ShipmentProNbrTxt(ProNumberHelper.validateProNumber(baseLog.getShipmentId().getProNumber(), txnContext));
					}
				}
				
				if((BaseLogTypeEnum.BASE_LOG_41.getCode().equals(logId) || BaseLogTypeEnum.BASE_LOG_42.getCode().equals(logId)) && ObjectUtils.isNotEmpty(baseLog.getParentShipmentId())) {
					if (StringUtils.isNotBlank(baseLog.getParentShipmentId().getShipmentInstId())) {
						proxyBean.setInParentIshm1ShipmentInstId(Double.parseDouble(baseLog.getParentShipmentId().getShipmentInstId()));
					} else {
						proxyBean.setInParentIshm1ShipmentProNbrTxt(ProNumberHelper.validateProNumber(baseLog.getParentShipmentId().getProNumber(), txnContext));
					}
				}
				
				if (BaseLogTypeEnum.BASE_LOG_44.getCode().equals(logId) && baseLog.getEquipmentInstId() !=null) {
					proxyBean.setInTrailerXrefIopt1TrlrLoadInstTrlrLdMbfEqpId(BasicTransformer.toString(baseLog.getEquipmentInstId()));
				}
				
				if (BaseLogTypeEnum.BASE_LOG_42.getCode().equals(logId) && baseLog.getShortPiecesCount() != null) {
					proxyBean.setAsStringInShortIshm1MovementExceptionPcsCnt(BasicTransformer.toString(baseLog.getShortPiecesCount().intValue()));
				}
				
				proxyBean.execute();
				
				ProxyExceptionBuilder
						.exception(proxyBean.getOutErrorIshs1SharedServicesOriginServerId(),
								proxyBean.getOutErrorIshs1SharedServicesReturnCd(),
								proxyBean.getOutErrorIshs1SharedServicesReasonCd(), txnContext)
						.contextString(proxyBean.getOutErrorIshs1SharedServicesContextStringTx())
						.log()
						.throwIfException();
			}
			
		} catch (Exception e) {
			LOGGER.warn("Failed createBaseLog: " + e.getMessage(), e);
		}

	}

	private String getCommandBase(String logId) {
		String command= StringUtils.EMPTY;
		if (BaseLogTypeEnum.BASE_LOG_30.getCode().equals(logId) || BaseLogTypeEnum.BASE_LOG_38.getCode().equals(logId) ||BaseLogTypeEnum.BASE_LOG_3A.getCode().equals(logId)) {
			command ="LOGSTRIP";
		}else
			command="BASE" + logId;
		return command;
	}
}
