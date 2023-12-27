	package com.xpo.ltl.shipment.service.validators;

import java.util.ArrayList;
import java.util.List;

import com.xpo.ltl.api.shipment.v2.EquipmentId;
import com.xpo.ltl.api.shipment.v2.RouteName;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.v2.PrintFBDSDocumentsRqst;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;

public class PrintFbdsDocumentsValidator extends Validator {

	public void validate(PrintFBDSDocumentsRqst rqst, TransactionContext txnContext) throws ServiceException {
		checkTransactionContext(txnContext);
 
		List<MoreInfo> moreInfos = new ArrayList<>();
		if (rqst == null) {
			addMoreInfo(moreInfos, "request", "Request is required.");
		}
		else {
			List<String> proNbrs = rqst.getProNbrs();
			List<EquipmentId> equipmentIds = rqst.getEquipmentIds();
			List<RouteName> routeNames = rqst.getRouteNames();

			if (CollectionUtils.isEmpty(proNbrs) && CollectionUtils.isEmpty(equipmentIds) && CollectionUtils.isEmpty(routeNames)) {
				addMoreInfo(moreInfos, "request.proNbrs, request.equipmentIds, request.routeNames",
						"ProNbrs, EquipmentIds or RouteNames are required.");
			}
			else {
				String duplicatedParamsMsg = "Only one of this parameters should be present: ProNumber, EquipmentIds or RouteNames";
				if (CollectionUtils.isNotEmpty(proNbrs)) {
					if (CollectionUtils.isNotEmpty(equipmentIds) || CollectionUtils.isNotEmpty(routeNames)){
						addMoreInfo(moreInfos, "request", duplicatedParamsMsg);
					}
				}
				else if (CollectionUtils.isNotEmpty(equipmentIds) && CollectionUtils.isNotEmpty(routeNames)){
					addMoreInfo(moreInfos, "request", duplicatedParamsMsg);
				}

				if (CollectionUtils.isNotEmpty(equipmentIds)) {
					if (equipmentIds.stream().anyMatch(equipmentId -> equipmentId.getEquipmentIdSuffixNbr()==null ||
																	StringUtils.isEmpty(equipmentId.getEquipmentPrefix()))){
						addMoreInfo(moreInfos, "request.equipmentIds",
					"EquipmentId.equipmentIdSuffixNbr and EquipmentId.equipmentPrefix should be present in all equipmentIds parameters");
					}
				}

				if (CollectionUtils.isNotEmpty(routeNames)) {
					if (routeNames.stream().anyMatch(routeName -> StringUtils.isEmpty(routeName.getRoutePrefix()) ||
							StringUtils.isEmpty(routeName.getRouteSuffix()))){
						addMoreInfo(moreInfos, "request.routeNames",
					"RouteName.routePrefix and RouteName.routeSuffix should be present in all routeNames parameters");
					}
				}

				if (CollectionUtils.isNotEmpty(proNbrs)) {
					for (String proNbr : proNbrs) {
						if (!ProNumberHelper.isElevenDigit(proNbr) && !ProNumberHelper.isBluePro(proNbr)) {
							addMoreInfo(moreInfos, "request.proNbrs", "ProNumber should be 11 digit or 9 digit");
						}
					}
				}
			}
			
			if(StringUtils.isBlank(rqst.getFormType())) {
				addMoreInfo(moreInfos, "request.formType", "FormType is required.");
			}

			if(StringUtils.isBlank(rqst.getReportType())) {
				addMoreInfo(moreInfos, "request.reportType", "ReportType is required.");
			}
			
			if(StringUtils.isBlank(rqst.getPrinterCd())) {
				addMoreInfo(moreInfos, "request.printerCd", "PrinterCd is required.");
			} else if(rqst.getPrinterCd().length() > 12) {
				addMoreInfo(moreInfos, "request.printerCd", "Invalid printer selection. Please select a valid printer for Two Part Paper");
			}
		}

		if (!moreInfos.isEmpty())
			throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo(moreInfos).build();
	}
}