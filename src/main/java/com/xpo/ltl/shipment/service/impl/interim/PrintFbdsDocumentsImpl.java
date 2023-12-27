package com.xpo.ltl.shipment.service.impl.interim;

import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.PropertyVetoException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ProxyExceptionBuilder;
import com.xpo.ltl.api.shipment.v2.DocumentFormTypeCd;
import com.xpo.ltl.api.shipment.v2.EquipmentId;
import com.xpo.ltl.api.shipment.v2.FBDSDocument;
import com.xpo.ltl.api.shipment.v2.FbdsVersionCd;
import com.xpo.ltl.api.shipment.v2.ListFBDSDocumentsResp;
import com.xpo.ltl.api.shipment.v2.PrintFBDSDocumentsResp;
import com.xpo.ltl.api.shipment.v2.PrintFBDSDocumentsRqst;
import com.xpo.ltl.api.shipment.v2.PrintFBDSDocumentsTypeCd;
import com.xpo.ltl.api.shipment.v2.RouteName;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.context.AppContext;
import com.xpo.ltl.shipment.service.impl.ListFBDSCopyBillDocumentsImpl;
import com.xpo.ltl.shipment.service.impl.PdfDeliveryReceiptImpl;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.validators.PrintFbdsDocumentsValidator;

import Mf2al000.Abean.Mf2aSFbdsProPrint;

@RequestScoped
@LogExecutionTime("info")
public class PrintFbdsDocumentsImpl {
	
	private static final Log LOGGER = LogFactory.getLog(PrintFbdsDocumentsImpl.class);
	
	public static final String DEST = "C:/Users/dmitriev.alexander1/Desktop/dr_print_test/dr_test_file.pdf";
	
	private static final String YES_IND = "Y";
	private static final String NO_IND = "N";

	@Inject
	private AppContext appContext;
	
	@Inject
	private PrintFbdsDocumentsValidator printFbdsDocumentsValidator;
	
	@Inject
	private PdfDeliveryReceiptImpl pdfDeliveryReceiptImpl; 
	
	@Inject
	private	ListFBDSCopyBillDocumentsImpl listFBDSCopyBillDocumentsImpl;
	
	public PrintFBDSDocumentsResp printFbdsDocuments(
		final PrintFBDSDocumentsRqst rqst,
		final TransactionContext txnContext,
		final EntityManager entityManager) throws ServiceException {
		PrintFBDSDocumentsResp resp = null;
		if(rqst.getPrintFBDSDocumentsTypeCd() == PrintFBDSDocumentsTypeCd.DOT_MATRIX) {
			printFBDSDocumentOnDotMatrix(rqst, txnContext);			
		}
		else if(rqst.getPrintFBDSDocumentsTypeCd() == PrintFBDSDocumentsTypeCd.LASER) {

			checkNotNull(CollectionUtils.isNotEmpty(rqst.getProNbrs()), "Pro Numbers required for printing on laser printer");
			List<byte[]> documentData = new ArrayList<>();
			List<String> proNbrs = new ArrayList<>(); 
	        resp = new PrintFBDSDocumentsResp();
	        String[] pros = rqst.getProNbrs().stream().toArray(String[]::new);
	        DocumentFormTypeCd docType = DocumentFormTypeCd.fromValue(rqst.getReportType());
            ListFBDSDocumentsResp fbdsDocumentsData =
                listFBDSCopyBillDocumentsImpl.listFBDSDocuments
                    (pros,
                     null,
                     docType,
                     false,
                     true,
                     txnContext,
                     entityManager);
        	if(CollectionUtils.isNotEmpty(fbdsDocumentsData.getFbdsDocuments())) {
        		for (FBDSDocument fbdsDocumentData : fbdsDocumentsData.getFbdsDocuments()) {
        			byte[] byteOutput = pdfDeliveryReceiptImpl.generateFBDSDocumentPdf(fbdsDocumentData, FbdsVersionCd.DEFAULT
        					, docType, txnContext, entityManager);

        			documentData.add(byteOutput);
        			proNbrs.add(fbdsDocumentData.getProNbr());
    			}
        	}
        	
        	try {
				FileOutputStream fo = new FileOutputStream(DEST);
				fo.write(documentData.get(0));
				fo.close();
			} catch (Exception e) {
                // Don't spam stacktrace everywhere except Alex's PC
                //e.printStackTrace();
			}
	        
			resp.setDocumentData(documentData);
			resp.setProNbrs(proNbrs);
		}
		
		return resp;
	}

	private void printFBDSDocumentOnDotMatrix(PrintFBDSDocumentsRqst rqst, TransactionContext txnContext) throws ServiceException {
		printFbdsDocumentsValidator.validate(rqst, txnContext);

		final List<String> printingParams = getPrintingParams(rqst);
		final Mf2aSFbdsProPrint proxyBean = new Mf2aSFbdsProPrint();
		
		try {
			String includeProsNotDestinedInd = NO_IND;
			if (rqst.isIncludeProsNotDestinedInd() != null){
				includeProsNotDestinedInd = BasicTransformer.toString(rqst.isIncludeProsNotDestinedInd());
			}
			String includeProsDestinedInd = NO_IND;
			if (rqst.isIncludeProsDestinedInd() != null) {
				includeProsDestinedInd = BasicTransformer.toString(rqst.isIncludeProsDestinedInd());
			}
			String reprintInd = NO_IND;
			if (rqst.getReprintInd() != null) {
				reprintInd = BasicTransformer.toString(rqst.isReprintInd());
			}

			proxyBean.clear();
			proxyBean.setCommandSent(PrintCommand.getCommand(rqst).getValue());
			proxyBean.setComCfg(appContext.getPrintFbdsComBridgeConfiguration());
			if (txnContext != null && txnContext.getUser() != null && txnContext.getUser().getUserId() != null) {
				proxyBean.setInControlIshs1SharedServicesLongUserid(StringUtils.substring(txnContext.getUser().getUserId(), 0, 15)); // Proxy accepts max 15 length
			} else {
				proxyBean.setInControlIshs1SharedServicesLongUserid("testUser");
			}

			// set input parameters for ProxyBean
			if(rqst.getDomicileSicCd() != null) {
				proxyBean.setInDomicileSicCdIshs1SharedServicesString3Tx(rqst.getDomicileSicCd());
			}
			proxyBean.setInFormTypIshs1SharedServicesString8Tx(rqst.getFormType());
			proxyBean.setInPrinterCodeIshs1SharedServicesString12Tx(rqst.getPrinterCd());
			proxyBean.setInReportTypIshs1SharedServicesString8Tx(rqst.getReportType());
			proxyBean.setInIncludeClosedTrlrFlagIshs1SharedServicesFlagFg(NO_IND);
			proxyBean.setInIncludeProsDestinedIndIshs1SharedServicesFlagFg(includeProsDestinedInd);
			proxyBean.setInIncludeProsNotDestinedIndIshs1SharedServicesFlagFg(includeProsNotDestinedInd);
			proxyBean.setInReprintIndIshs1SharedServicesFlagFg(reprintInd);
			proxyBean.setInValidateIndIshs1SharedServicesFlagFg(YES_IND);
			if (rqst.getHostDestinationSicCd() != null) {
				proxyBean.setInDestSicCdIshs1SharedServicesString3Tx(rqst.getHostDestinationSicCd());
			}

			if (CollectionUtils.isNotEmpty(printingParams)) {
			    proxyBean.setInRPrintGroupCount((short) printingParams.size());
			    for (int i = 0; i < printingParams.size(); i++) {
			      proxyBean.setInRPrintRequestKeyIshs1SharedServicesString20Tx(i, printingParams.get(i));
			    }
			}
			
			proxyBean.execute();
			
			ProxyExceptionBuilder
					.exception(proxyBean.getOutErrorIshs1SharedServicesOriginServerId(),
							proxyBean.getOutErrorIshs1SharedServicesReturnCd(),
							proxyBean.getOutErrorIshs1SharedServicesReasonCd(), txnContext)
					.contextString(proxyBean.getOutErrorIshs1SharedServicesContextStringTx())
					.log()
					.throwIfException();
			
			LOGGER.info(String.format("Finished printing FBDS documents for proNbrs: ", String.join(",", printingParams)));
		} catch (PropertyVetoException e) {
			throw com.xpo.ltl.api.exception.ExceptionBuilder
					.exception(com.xpo.ltl.api.exception.ServiceErrorMessage.UNEXPECTED_EXCEPTION, txnContext).log(e)
					.moreInfo("PrintFbdsImpl.printFbds: property veto exception occurred", e.getMessage())
					.build();
		}
	}

	private List<String> getPrintingParams(PrintFBDSDocumentsRqst rqst) {
		final List<String> printingParams;
		String paramName;

		if (CollectionUtils.isNotEmpty(rqst.getProNbrs())){
			printingParams = rqst.getProNbrs();
			paramName = "proNbrs";
		}
		else if (CollectionUtils.isNotEmpty(rqst.getEquipmentIds())){
			printingParams = rqst.getEquipmentIds().stream()
									.map(equipmentId -> getPrintingParameter(equipmentId))
									.collect(Collectors.toList());
			paramName = "equipmentIds";
		}
		else{
			printingParams = rqst.getRouteNames().stream()
									.map(routeName -> getPrintingParameter(routeName))
									.collect(Collectors.toList());
			paramName = "routeNames";
		}
		LOGGER.info(String.format("Started printing FBDS documents for " + paramName + ": ", String.join(",", printingParams)));
		return printingParams;
	}

	private String getPrintingParameter(EquipmentId equipmentId){
		return getPrintingParameter(equipmentId.getEquipmentPrefix(), BasicTransformer.toString(equipmentId.getEquipmentIdSuffixNbr()));
	}

	private String getPrintingParameter(RouteName routeName){
		return getPrintingParameter(routeName.getRoutePrefix(), routeName.getRouteSuffix());
	}

	private String getPrintingParameter(String prefix, String suffix){
		return String.format("%s-%s", prefix, suffix);
	}

	private enum PrintCommand{
		PRO_PRINT_COMMAND("PROPRT"),
		ROUTE_PRINT_COMMAND("ROUTEPRT"),
		TRAILER_PRINT_COMMAND("TRLRPRT");

		private String value;

		PrintCommand(String value){
			this.value = value;
		}

		String getValue(){
			return this.value;
		}

		static PrintCommand getCommand(PrintFBDSDocumentsRqst rqst){
			if (CollectionUtils.isNotEmpty(rqst.getProNbrs())){
				return PRO_PRINT_COMMAND;
			}
			if (CollectionUtils.isNotEmpty(rqst.getRouteNames())){
				return ROUTE_PRINT_COMMAND;
			}
			if (CollectionUtils.isNotEmpty(rqst.getEquipmentIds())) {
				return TRAILER_PRINT_COMMAND;
			}
			throw new IllegalArgumentException("proNbrs, routeNames or equipmentIds should be not empty");
		}
	}
}

