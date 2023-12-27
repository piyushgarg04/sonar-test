package com.xpo.ltl.shipment.service.validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.v2.BaseLog;
import com.xpo.ltl.api.shipment.v2.CreateBaseLogRqst;
import com.xpo.ltl.shipment.service.enums.BaseLogTypeEnum;

public class CreateBaseLogValidator extends Validator {

	public void validate(CreateBaseLogRqst request, String logId, TransactionContext txnContext)
			throws ServiceException {
		checkTransactionContext(txnContext);

		List<MoreInfo> moreInfos = new ArrayList<>();
		if (request == null) {
			addMoreInfo(moreInfos, "request", "Request is null");
		} else {
			if (StringUtils.isBlank(logId)) {
				addMoreInfo(moreInfos, "request.logId", "LogId is null or empty");
			}
			try {
				BaseLogTypeEnum logtype = BaseLogTypeEnum.fromValue(logId);
			} catch (final IllegalArgumentException e) {
				addMoreInfo(moreInfos, "request.logId", "Invalid baseLog type " + logId);
			}
			List<BaseLog> baseLogs = request.getBaseLogs();
			if (CollectionUtils.isEmpty(baseLogs)) {
				addMoreInfo(moreInfos, "request.baseLog", "BaseLog is null or empty");
			} else {
				Set<String> proNbrs = new HashSet<>();
				Set<String> shpInstIds = new HashSet<>();
				Set<String> parentProNbrs = new HashSet<>();
				Set<String> parentShpInstIds = new HashSet<>();
				for (BaseLog baseLog : baseLogs) {
					if (ObjectUtils.isEmpty(baseLog.getShipmentId())) {
						addMoreInfo(moreInfos, "request.shipmentIds", "ShipmentId is blank");
					} else {

						String proNbr = baseLog.getShipmentId().getProNumber();
						if (proNbr != null) {
							if (StringUtils.isBlank(proNbr)) {
								addMoreInfo(moreInfos, "request.shipmentIds.proNumber", "ProNumber is blank");
							} else if (!proNbrs.add(proNbr)) {
								addMoreInfo(moreInfos, "request.shipmentIds.proNumber",
										"Duplicate ProNumber " + proNbr);
							}
						}

						String shpInstId = baseLog.getShipmentId().getShipmentInstId();
						if (shpInstId != null) {
							if (StringUtils.isEmpty(shpInstId)) {
								addMoreInfo(moreInfos, "request.shipmentIds.shipmentInstId",
										"ShipmentInstId is required");
							} else if (!NumberUtils.isParsable(shpInstId)) {
								addMoreInfo(moreInfos, "request.shipmentIds.shipmentInstId",
										"Invalid ShipmentInstId " + shpInstId);
							} else if (!shpInstIds.add(shpInstId)) {
								addMoreInfo(moreInfos, "request.shipmentIds.shipmentInstId",
										"Duplicate ShipmentInstId " + shpInstId);
							}
						}
					}
					if (StringUtils.isNotBlank(logId) && BaseLogTypeEnum.BASE_LOG_41.getCode().equals(logId)) {

						if (ObjectUtils.isEmpty(baseLog.getParentShipmentId())) {
							addMoreInfo(moreInfos, "request.parentShipmentIds", "Parent ShipmentId is blank");
						} else {

							String parentProNbr = baseLog.getParentShipmentId().getProNumber();
							if (parentProNbr != null) {
								if (StringUtils.isBlank(parentProNbr)) {
									addMoreInfo(moreInfos, "request.parentShipmentIds.proNumber",
											"Parent ProNumber is blank");
								} else if (!parentProNbrs.add(parentProNbr)) {
									addMoreInfo(moreInfos, "request.parentShipmentIds.proNumber",
											"Parent Duplicate ProNumber " + parentProNbr);
								}
							}
							String parentShmInstId = baseLog.getParentShipmentId().getShipmentInstId();
							if (parentShmInstId != null) {
								if (StringUtils.isBlank(parentShmInstId)) {
									addMoreInfo(moreInfos, "request.parentShipmentIds.shipmentInstId",
											"Parent shipmentInstId is blank");
								} else if (!NumberUtils.isParsable(parentShmInstId)) {
									addMoreInfo(moreInfos, "request.parentShipmentIds.shipmentInstId",
											"Invalid Parent ShipmentInstId " + parentShmInstId);
								} else if (!parentShpInstIds.add(parentShmInstId)) {
									addMoreInfo(moreInfos, "request.parentShipmentIds.shipmentInstId",
											"Duplicate Parent ShipmentInstId " + parentShmInstId);
								}
							}
						}
					}
				}

				if (!proNbrs.isEmpty() && !shpInstIds.isEmpty())
					addMoreInfo(moreInfos, "request.shipmentIds",
							"ProNumbers and ShipmentInstIds are mutually exclusive");

				if (StringUtils.isNotBlank(logId) && BaseLogTypeEnum.BASE_LOG_41.getCode().equals(logId)
						&& !parentProNbrs.isEmpty() && !parentShpInstIds.isEmpty())
					addMoreInfo(moreInfos, "request.parentShipmentIds",
							"Parent ProNumbers and Parent ShipmentInstIds are mutually exclusive");
			}

		}

		if (!moreInfos.isEmpty())
			throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo(moreInfos).build();
	}
}