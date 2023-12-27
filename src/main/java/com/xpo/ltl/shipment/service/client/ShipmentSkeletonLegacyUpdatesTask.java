package com.xpo.ltl.shipment.service.client;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.v2.BaseLog;
import com.xpo.ltl.api.shipment.v2.LateTenderCd;
import com.xpo.ltl.api.shipment.v2.ShipmentId;
import com.xpo.ltl.api.shipment.v2.ShipmentSkeletonResponse;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.java.util.cityoperations.executors.Task;
import com.xpo.ltl.shipment.service.enums.BaseLogTypeEnum;
import com.xpo.ltl.shipment.service.impl.interim.MaintainBolDocImpl;

public class ShipmentSkeletonLegacyUpdatesTask implements Task<ShipmentSkeletonLegacyUpdatesResult> {

	private final ShipmentSkeletonResponse shipmentSkeletonResponse;
	private final ExternalRestClient client;
	private  MaintainBolDocImpl maintainBolDocImpl;
	private final TransactionContext txnContext;

	public ShipmentSkeletonLegacyUpdatesTask(ShipmentSkeletonResponse shipmentSkeletonResponse,
			MaintainBolDocImpl maintainBolDocImpl, ExternalRestClient client, TransactionContext txnContext) {

		this.client = client;
		this.shipmentSkeletonResponse = shipmentSkeletonResponse;
		this.maintainBolDocImpl = maintainBolDocImpl;
		this.txnContext = txnContext;
	}

	@Override
	public String getName() {
		return BasicTransformer.toJson(shipmentSkeletonResponse.getShipmentInstId());
	}

	@Override
	public ShipmentSkeletonLegacyUpdatesResult execute() throws ServiceException {

		boolean LateTenderInd = shipmentSkeletonResponse.getLateTenderCd() == LateTenderCd.LATE_TENDER ? true : false;
		boolean bypassBo11Call = BooleanUtils
				.isFalse(LateTenderInd)
				&& (shipmentSkeletonResponse.getBolInstId() == null
						|| BigDecimal.ZERO.equals(shipmentSkeletonResponse.getBolInstId()));

		if (!bypassBo11Call){
			maintainBolDocImpl.ediBolUpdate(shipmentSkeletonResponse, txnContext);
		}

		if (BooleanUtils.isTrue(LateTenderInd)) {
			List<BaseLog> baseLogList = new ArrayList<>();
			BaseLog baseLog = new BaseLog();
			ShipmentId shmId = new ShipmentId();
			if (StringUtils.isNotEmpty(shipmentSkeletonResponse.getProNbr()))
				shmId.setProNumber(shipmentSkeletonResponse.getProNbr());
			baseLog.setShipmentId(shmId);
			baseLogList.add(baseLog);

			client.startCreateBaseLogChEnsemble(BaseLogTypeEnum.BASE_LOG_70, baseLogList, txnContext);
		}

		return new ShipmentSkeletonLegacyUpdatesResult(shipmentSkeletonResponse, "Success");
	}

	@Override
	public ShipmentSkeletonLegacyUpdatesResult handleFailure(Exception exception) {
		return new ShipmentSkeletonLegacyUpdatesResult(shipmentSkeletonResponse, exception.toString());
	}

}
