package com.xpo.ltl.shipment.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.api.client.util.Lists;
import com.xpo.ltl.api.appointment.v1.AppointmentNotificationStatCd;
import com.xpo.ltl.api.appointment.v1.DeliveryNotification;
import com.xpo.ltl.api.appointment.v1.ListAppointmentNotificationsForShipmentsResp;
import com.xpo.ltl.api.cityoperations.v1.DeliveryShipmentSearchRecord;
import com.xpo.ltl.api.customer.v1.DetermineRestrictedBillToResp;
import com.xpo.ltl.api.exception.ErrorMessageIF;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.MoreInfoBuilder;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.location.v2.GetLocReferenceDetailsBySicResp;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmCustomsBond;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.EntityTransformer;
import com.xpo.ltl.api.shipment.v2.AccessorialService;
import com.xpo.ltl.api.shipment.v2.AdvanceBeyondCarrier;
import com.xpo.ltl.api.shipment.v2.AdvanceBeyondTypeCd;
import com.xpo.ltl.api.shipment.v2.AsMatchedParty;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.BillClassCd;
import com.xpo.ltl.api.shipment.v2.BillStatusCd;
import com.xpo.ltl.api.shipment.v2.BillToRelationCd;
import com.xpo.ltl.api.shipment.v2.ChargeToCd;
import com.xpo.ltl.api.shipment.v2.Commodity;
import com.xpo.ltl.api.shipment.v2.CustomsBond;
import com.xpo.ltl.api.shipment.v2.DataValidationError;
import com.xpo.ltl.api.shipment.v2.DeliveryQualifierCd;
import com.xpo.ltl.api.shipment.v2.DocumentFormTypeCd;
import com.xpo.ltl.api.shipment.v2.FBDSDocument;
import com.xpo.ltl.api.shipment.v2.HandlingUnit;
import com.xpo.ltl.api.shipment.v2.ListFBDSDocumentsResp;
import com.xpo.ltl.api.shipment.v2.MatchedPartyTypeCd;
import com.xpo.ltl.api.shipment.v2.MiscLineItem;
import com.xpo.ltl.api.shipment.v2.MiscLineItemCd;
import com.xpo.ltl.api.shipment.v2.Movement;
import com.xpo.ltl.api.shipment.v2.MovementException;
import com.xpo.ltl.api.shipment.v2.Remark;
import com.xpo.ltl.api.shipment.v2.Shipment;
import com.xpo.ltl.api.shipment.v2.ShipmentNotification;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.java.util.cityoperations.FormatterUtils;
import com.xpo.ltl.shipment.pdf.FBDSDocumentsWithValidations;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShipmentAcSvcSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentAdvBydSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentAsEnteredCustomerDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentMiscLineItemSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentRemarkSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmCommoditySubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmMovementSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentEagerLoadPlan;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.transformhandlers.AdvanceBeyondCarrierTransformHandler;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.FBDSCopyBillUtil;
import com.xpo.ltl.shipment.service.util.FormatHelper;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import com.xpo.ltl.shipment.service.util.TimestampUtil;
import com.xpo.ltl.shipment.service.validators.Validator;

@ApplicationScoped
@LogExecutionTime
public class ListFBDSCopyBillDocumentsImpl extends Validator {

    private static final String PGM_ID = "PRNTFBDS";
	private static final String MVMNT_DLVY_QLFR_CD_TRANSFER = "T";
	private static final String MVMNT_DLVY_QLFR_CD_RETURN_STATUS = "R";
	private static final String MVMNT_DLVY_QLFR_CD_DAMAGED_SHPMT = "M";
	private static final String MVMNT_DLVY_QLFR_CD_REFUSED_DLVY = "L";
	private static final String MVMNT_DLVY_QLFR_CD_ATTEMPTED_DLVY = "H";
	private static final String MVMNT_DLVY_QLFR_CD_CARTAGE = "G";
	private static final String MVMNT_DLVY_QLFR_CD_HOLD_FOR_APPT = "E";
	private static final String MVMNT_DLVY_QLFR_CD_HOLD_FOR_CUSTOMS = "C";
	private static final String MVMNT_DLVY_QLFR_CD_TRAPPED_SHIPMENT = "B";
	private static final String MVMNT_DLVY_QLFR_CD_PART_SHORT = "K";
	private static final String UNRATED = "UNRATED";
	private static final String TOTAL = "TOTAL";
	private static final String ASTERIKS_CONSTANT = "***";
	private static final String GUR_ACCS_CODE = "GUR";
	private static final String GFR_ACCS_CODE = "GFR";
	private static final String HAZMAT_COMMODITIES_MESSAGE = "* ORIGINAL BILL CONTAINED HAZMAT. VERIFY COMMODITIES. *";
	private static final String PART_LOT = "PART LOT";
	private static final String COMMA = ",";

	private static final String IN_BOND = "IN BOND ";
	private static final String COLLECTOR_OF_CUSTOMS = "COLLECTOR OF CUSTOMS " ;
	private static final String NOTIFY = "NOTIFY ";
	private static FastDateFormat DATE_HYPHEN = FastDateFormat.getInstance("yyyy-MM-dd");
	private static FastDateFormat DATE_SLASH = FastDateFormat.getInstance("MM/dd/yyyy");


	private static final Log LOGGER = LogFactory.getLog(ListFBDSCopyBillDocumentsImpl.class);

    private enum Mode { PRO_NBR, SHP_INST_ID }

	@Inject
	private ShmShipmentSubDAO shmShipmentSubDAO;

	@Inject
	private ShipmentAsEnteredCustomerDAO shmAsEntdCustsSubDAO;
	
	@Inject
	private ShmMovementSubDAO shmMovementSubDAO;
	
	@Inject
	private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;
	
	@Inject
	private ShipmentAcSvcSubDAO shmAcSvcSubDAO;
	
	@Inject
	private ShipmentAdvBydSubDAO shmAdvBydSubDAO;
	
	@Inject
	private ShmCommoditySubDAO shmCommoditySubDAO;

	
	@Inject
	private ShipmentMiscLineItemSubDAO shmMiscLineItemSubDAO;

	@Inject
	private ShipmentRemarkSubDAO shmRemarkSubDAO;
	
	@Inject
	private ExternalRestClient restClient;

	@PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
	private EntityManager db2EntityManager;
	
	@Inject
    private FBDSCopyBillUtil util;

    /**
     * Original operation.
     * Generates a FBDS/CopyBill document for list of pro numbers.
     */
    public ListFBDSDocumentsResp listFBDSDocuments
            (String[] proNbrs,
             Long[] shipmentInstIds,
             DocumentFormTypeCd documentRequestType,
             Boolean allowWarningsInd,
             Boolean shouldFbdsPrintCntIncremented,
             TransactionContext txnContext,
             EntityManager entityManager)
            throws ServiceException {
        List<String> proNbrsAsList;
        if (proNbrs != null) {
            proNbrsAsList = new ArrayList<>();
            for (String proNbr : proNbrs) {
                String elevenDigitPro =
                    ProNumberHelper.toElevenDigitPro(proNbr, txnContext);
                proNbrsAsList.add(elevenDigitPro);
            }
        }
        else {
            proNbrsAsList = Collections.emptyList();
        }

        List<Long> shpInstIdsAsList;
        if (shipmentInstIds != null) {
            shpInstIdsAsList = Arrays.asList(shipmentInstIds);
        }
        else {
            shpInstIdsAsList = Collections.emptyList();
        }

        Mode mode =
            !proNbrsAsList.isEmpty() ? Mode.PRO_NBR : Mode.SHP_INST_ID;

        validate(proNbrsAsList,
                 shpInstIdsAsList,
                 documentRequestType,
                 txnContext);

        List<ShmShipment> shmShipments = null;
        if (mode == Mode.PRO_NBR) {
            shmShipments =
                shmShipmentSubDAO.findNonArchivedByProNbrs
                    (proNbrsAsList, entityManager);
        }
        else {
            shmShipments =
                shmShipmentSubDAO.listShipmentsByShpInstIds
                    (shpInstIdsAsList,
                     // TODO Should probably eager load shmAsEntdCusts and
                     // shmSrNbrs relationships (see FBDSCopyBillUtil)
                     new ShmShipmentEagerLoadPlan(),
                     entityManager);
        }

        List<DataValidationError> validationWarnings = new ArrayList<>();

        // Compatibility with original implementation which did not throw error
        // for missing shipments
        if (BooleanUtils.isTrue(allowWarningsInd)) {
            List<Shipment> shipmentsMissing;

            if (mode == Mode.PRO_NBR) {
                List<String> proNbrsFound =
                    shmShipments.stream()
                        .map(ShmShipment::getProNbrTxt)
                        .collect(Collectors.toList());
                Collection<String> proNbrsMissing =
                    CollectionUtils.subtract(proNbrsAsList, proNbrsFound);
                shipmentsMissing =
                    proNbrsMissing.stream()
                        .map(proNbr -> {
                                 Shipment shipment = new Shipment();
                                 shipment.setProNbr(proNbr);
                                 return shipment;
                             })
                        .collect(Collectors.toList());
            }
            else {
                List<Long> shpInstIdsFound =
                    shmShipments.stream()
                        .map(ShmShipment::getShpInstId)
                        .collect(Collectors.toList());
                Collection<Long> shpInstIdsMissing =
                    CollectionUtils.subtract(shpInstIdsAsList, shpInstIdsFound);
                shipmentsMissing =
                    shpInstIdsMissing.stream()
                        .map(shpInstId -> {
                                 Shipment shipment = new Shipment();
                                 shipment.setShipmentInstId(shpInstId);
                                 return shipment;
                             })
                        .collect(Collectors.toList());
            }

            for (Shipment shipmentMissing : shipmentsMissing) {
                addValidationWarning
                    (validationWarnings,
                     mode,
                     shipmentMissing,
                     NotFoundErrorMessage.SHIPMENT_NF,
                     mode == Mode.PRO_NBR
                         ? "PRO"
                         : "ShipmentInstId",
                     mode == Mode.PRO_NBR
                         ? shipmentMissing.getProNbr()
                         : shipmentMissing.getShipmentInstId());
            }
        }

        FBDSDocumentsWithValidations documents =
            processFBDSCopyBillRequest
                (mode,
                 shmShipments,
                 documentRequestType,
                 true,
                 shouldFbdsPrintCntIncremented,
                 txnContext,
                 entityManager);

        ListFBDSDocumentsResp response = new ListFBDSDocumentsResp();

        if (documents != null) {
            response.setFbdsDocuments(documents.getFbdsDocuments());

            List<DataValidationError> validationErrors =
                documents.getValidationErrors();
            if (CollectionUtils.isNotEmpty(validationErrors))
                validationWarnings.addAll(validationErrors);
        }

        if (!validationWarnings.isEmpty()) {
            response.setWarnings(validationWarnings);

            if (BooleanUtils.isNotTrue(allowWarningsInd)) {
                List<MoreInfo> moreInfos =
                    validationWarnings.stream()
                        .map(this::toMoreInfo)
                        .collect(Collectors.toList());
                throw ExceptionBuilder
                    .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                    .moreInfo(moreInfos)
                    .build();
            }
        }

        return response;
    }

	/**
	 * Separate version of operation for PDF generator.
	 * Additionally to the original operation that builds FBDS document details
	 * this function will check which entities can be used for document generation
	 * and will modify FBDS count if applicable.
	 */
    public FBDSDocumentsWithValidations processAndGenerateFBDSCopyBillDocuments
            (List<String> proNbrs,
             DocumentFormTypeCd documentFormTypeCd,
             boolean reprintInd,
             TransactionContext txnContext,
             EntityManager entityManager)
        throws ServiceException {
	LOGGER.info("ListFBDSCopyBillDocumentsImpl::processAndGenerateFBDSCopyBillDocuments - Entering");

	FBDSDocumentsWithValidations response = new FBDSDocumentsWithValidations();
	List<DataValidationError> validationErrors = new ArrayList<>();
	List<Long> shipmentIdList = new ArrayList<>();
	List<ShmShipment> shipmentEntities = null;
	validate(proNbrs, shipmentIdList, documentFormTypeCd, txnContext);

	List<String> proNbrsList =  new ArrayList<>();
	if(CollectionUtils.isNotEmpty(proNbrs)) {
		for (String proNbr : proNbrs) {
			try {
				proNbrsList.add(ProNumberHelper.toElevenDigitPro(proNbr, txnContext));
			}
			catch (ValidationException e) {
				validationErrors.add(FBDSCopyBillUtil.createDataValidationError(proNbr, ValidationErrorMessage.PRO_NUMBER_FORMAT.message()));
				continue;
			}
		}
	}

	if(CollectionUtils.isNotEmpty(proNbrsList)) {
		shipmentEntities = shmShipmentSubDAO.findNonArchivedByProNbrs(proNbrsList, entityManager);
		validationErrors.addAll(processErrorOnMissingShipments(proNbrsList, shipmentEntities));
	}
        response =
            processFBDSCopyBillRequest
                (Mode.PRO_NBR,
                 shipmentEntities,
                 documentFormTypeCd,
                 reprintInd,
                 true,
                 txnContext,
                 entityManager);

	if(response != null) {
		validationErrors.addAll(response.getValidationErrors());
	}
	else {
		response = new FBDSDocumentsWithValidations();
	}
	response.setValidationErrors(validationErrors);


	LOGGER.info("ListFBDSCopyBillDocumentsImpl::processAndGenerateFBDSCopyBillDocuments - Finishing");

	return response;
}

	private List<DataValidationError> processErrorOnMissingShipments(List<String> proNbrsList, List<ShmShipment> shipments) {
		List<String> proNbrsFound = CollectionUtils.
				emptyIfNull(shipments).stream().map(s -> s.getProNbrTxt()).collect(Collectors.toList());
		List<DataValidationError> errorList = new ArrayList<>();
		CollectionUtils.subtract(proNbrsList, proNbrsFound).stream().forEach(p -> {
			errorList.add(FBDSCopyBillUtil.createDataValidationError(p, "Shipment was not found."));
		});
		return errorList;
	}

    private List<String> buildNonLegacyProPrefixUsage(TransactionContext txnContext) throws ServiceException {

        return restClient
            .listNonLegacyProBolPrefixMaster(txnContext)
				.getBolPrefixMaster().stream()
				.map(bolPrefixMaster -> bolPrefixMaster.getBolProPrefix())
				.collect(Collectors.toList());
	}

    /**
     * Method to validate input request to FBDS/CopyBill endpoint.
     */
    private void validate(List<String> proNbrs,
                          List<Long> shpInstIds,
                          DocumentFormTypeCd documentRequestType,
                          TransactionContext txnContext)
            throws ValidationException {
        List<MoreInfo> moreInfos = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(proNbrs)
            && CollectionUtils.isNotEmpty(shpInstIds))
            addMoreInfo(moreInfos,
                        "proNbrs",
                        "Cannot have both pro number and shipment id lists on the same request");

        if (CollectionUtils.isEmpty(proNbrs)
            && CollectionUtils.isEmpty(shpInstIds))
            addMoreInfo(moreInfos,
                        "proNbrs",
                        "There were no shipment ids or pro numbers present on the request");

        if (documentRequestType == null)
            addMoreInfo(moreInfos,
                        "documentRequestType",
                        "Document type is required");

        checkMoreInfo(txnContext, moreInfos);
    }

	/**
	 * Validates shipment information.
	 * @param reprintInd 
	 */
    private List<DataValidationError> validateShipment(Mode mode,
                                                       DocumentFormTypeCd documentFormTypeCd,
                                                       Shipment shipment,
                                                       boolean reprintInd) {
        List<DataValidationError> validationWarnings = new ArrayList<>();

        if(!reprintInd) {
			if(shipment.getFbdsPrintCount() != null && BasicTransformer.toInt(shipment.getFbdsPrintCount()) > 0) {
				validationWarnings.add(FBDSCopyBillUtil.createDataValidationError(shipment.getProNbr(), "Unable to print pro as it was printed before."));
			}
		}
        
		if (BillStatusCd.UNBILLED.equals(shipment.getBillStatusCd())
				|| BillStatusCd.IN_FBES_SUSPENSE_QUEUE.equals(shipment.getBillStatusCd())) {
            addValidationWarning
                (validationWarnings,
                 mode,
                 shipment,
                 ValidationErrorMessage.PRO_NUMBER_NOT_BILLED);
		}
        if (documentFormTypeCd == DocumentFormTypeCd.FBDS) {
			if (BillClassCd.EXPEDITE.equals(shipment.getBillClassCd())) {
                addValidationWarning
                    (validationWarnings,
                     mode,
                     shipment,
                     ValidationErrorMessage.PRO_NUMBER_EXPEDITED);
			} else if (BillClassCd.SPLIT_SEGMENT.equals(shipment.getBillClassCd())) {
                addValidationWarning
                    (validationWarnings,
                     mode,
                     shipment,
                     ValidationErrorMessage.FBDS_NOT_AVLBLE_FOR_PRTLT_PRO);
			}

			if ((BillStatusCd.UNBILLED.equals(shipment.getBillStatusCd())
					|| BillStatusCd.IN_FBES_SUSPENSE_QUEUE.equals(shipment.getBillStatusCd())
					|| BillStatusCd.BILLED.equals(shipment.getBillStatusCd()))
					&& shipment.getRevenueBillInd()) {
                addValidationWarning
                    (validationWarnings,
                     mode,
                     shipment,
                     ValidationErrorMessage.PRO_NOT_RATED);
			}

		}

		if (BillClassCd.CLAIMS_OVRG_RPTG_BILL.equals(shipment.getBillClassCd())) {
            addValidationWarning
                (validationWarnings,
                 mode,
                 shipment,
                 ValidationErrorMessage.PRO_NUMBER_OREP);
		}

        return validationWarnings;
	}

    private FBDSDocumentsWithValidations processFBDSCopyBillRequest
            (Mode mode,
             List<ShmShipment> shipments,
             DocumentFormTypeCd documentFormTypeCd,
             boolean reprintInd,
             boolean shouldFbdsPrintCntIncremented,
             TransactionContext txnContext,
             EntityManager entityManager)
            throws ServiceException {
		if (CollectionUtils.isEmpty(shipments))
			return null;

        AuditInfo auditInfo = AuditInfoHelper.getAuditInfoWithPgmId(PGM_ID, txnContext);
		
		FBDSDocumentsWithValidations response = new FBDSDocumentsWithValidations();
		List<FBDSDocument> documentList = new ArrayList<>();
        List<DataValidationError> validationWarnings = new ArrayList<>();
		FBDSDocument documentDetails = null;
		FBDSCopyBillUtil.FBDSDerivedAttributes fbdsAttributes = null;
        List<String> nonLegacyProPrefixes = null;
		List<ShmShipment> shipmentEntitiesToUpdate = new ArrayList<>();
		
		try {
            nonLegacyProPrefixes = buildNonLegacyProPrefixUsage(txnContext);
		} catch (final Exception e) {
            nonLegacyProPrefixes = Lists.newArrayList(); // if api call or building the list failed, assume blank list
		}

		List<Long> shipmentInstIdsLong = shipments.stream()
                .map(shm-> shm.getShpInstId())
                .collect(Collectors.toList());
		
		List<BigDecimal> shipmentInstIds = shipments.stream()
                .map(shm-> BigDecimal.valueOf(shm.getShpInstId()))
                .collect(Collectors.toList());
		
		List<Long> parentShipmentInstIds = CollectionUtils.emptyIfNull(shipments).stream().map(shm-> BasicTransformer.toLong(shm.getParentInstId())).filter(Objects::nonNull).collect(Collectors.toList());
		
		//Retrieve additional shipment data and map to shipmentId when appropriate and get the parent child shipments.
		List<ShmShipment> parentChildShipments = shmShipmentSubDAO.findByShpInstIdsOrParentInstIdsWithCustomsBonds(parentShipmentInstIds, shipmentInstIds, entityManager);
        
        List<ShmShipment> relatedShipments = CollectionUtils.emptyIfNull(parentChildShipments).stream().filter(shm -> shipmentInstIds.contains(shm.getParentInstId())).collect(Collectors.toList());
        
        Map<Object, ShmShipment> parentShipmentsByShmInstId = CollectionUtils.emptyIfNull(parentChildShipments).stream().filter(shm -> parentShipmentInstIds.contains(shm.getShpInstId())).collect(Collectors.toMap(ShmShipment::getShpInstId, Function.identity()));
        
        List<ShmCustomsBond> customBonds = parentChildShipments.stream().flatMap(shm -> shm.getShmCustomsBonds().stream()).collect(Collectors.toList());
        
		List<String> shipmentInstIdStrings = shipmentInstIds.stream().map(shmInstId -> shmInstId.toString()).collect(Collectors.toList());
		List<DeliveryShipmentSearchRecord> elasticRecords = restClient.getDeliveryShipmentElasticRecords(shipmentInstIdStrings, txnContext);
		Map<Long, DeliveryShipmentSearchRecord> elasticRecordsByShmInstId = CollectionUtils.emptyIfNull(elasticRecords)
				.stream().collect(Collectors.toMap(DeliveryShipmentSearchRecord::getShipmentInstId, Function.identity()));

		Map<Long, DeliveryNotification> recentAppointmentsByShmInstId = getMostRecentNotification(shipmentInstIdsLong, txnContext);
		
		List<Long> shipmentInstIdsWithParents = new ArrayList<>();
		shipmentInstIdsWithParents.addAll(shipmentInstIdsLong);
		shipmentInstIdsWithParents.addAll(parentShipmentInstIds);
		
		//Perform constant SQL queries to retrieve shipment supportive data
		Map<Long, List<AsMatchedParty>> matchedPartiesByShmInstId = CollectionUtils.emptyIfNull(
			EntityTransformer.toAsMatchedParty(shmAsEntdCustsSubDAO.findByShpInstIds(shipmentInstIdsWithParents, entityManager)))
				.stream().collect(Collectors.groupingBy(AsMatchedParty::getShipmentInstId));

        List<ShmMovement> shmMovements = shmMovementSubDAO.findByShpInstIdsWithShmMovementExcps(shipmentInstIdsWithParents, entityManager);
		Map<Long, List<Movement>> movementsByShmInstIds = CollectionUtils.emptyIfNull(
			EntityTransformer.toMovement(shmMovements))
				.stream().collect(Collectors.groupingBy(Movement::getShipmentInstId));

        List<ShmMovementExcp> shmMovementExcps = shmMovements.stream().flatMap(shm -> shm.getShmMovementExcps().stream()).collect(Collectors.toList());
        
		Map<Long, List<MovementException>> movementExceptionsByShmInstId = CollectionUtils.emptyIfNull(
			EntityTransformer.toMovementException(shmMovementExcps))
				.stream().collect(Collectors.groupingBy(MovementException::getShipmentInstId));

		Map<Long, List<HandlingUnit>> handlingUnitsByShmInstId = CollectionUtils.emptyIfNull(
			EntityTransformer.toHandlingUnit(shmHandlingUnitSubDAO.findByShpInstIds(shipmentInstIds, entityManager)))
				.stream().collect(Collectors.groupingBy(HandlingUnit::getShipmentInstanceId));

		Map<Long, List<AccessorialService>> accessorialServicesByShmInstId = CollectionUtils.emptyIfNull(
			EntityTransformer.toAccessorialService(shmAcSvcSubDAO.findByShpInstIds(shipmentInstIds, entityManager)))
				.stream().collect(Collectors.groupingBy(AccessorialService::getShipmentInstId));

		Map<Long, List<AdvanceBeyondCarrier>> advanceBeyondCarriersByShmInstId = CollectionUtils.emptyIfNull(
			EntityTransformer.toAdvanceBeyondCarrier(shmAdvBydSubDAO.findByShpInstIds(shipmentInstIdsWithParents, entityManager),
						AdvanceBeyondCarrierTransformHandler.getHandlerForFBDS()))
				.stream().collect(Collectors.groupingBy(AdvanceBeyondCarrier::getShipmentInstId));

		Map<Long, List<Commodity>> commoditiesByShmInstId = CollectionUtils.emptyIfNull(
			EntityTransformer.toCommodity(shmCommoditySubDAO.findByShpInstIds(shipmentInstIds, entityManager)))
				.stream().collect(Collectors.groupingBy(Commodity::getShipmentInstId));

		Map<Long, List<MiscLineItem>> miscLineItemsByShmInstId = CollectionUtils.emptyIfNull(
			EntityTransformer.toMiscLineItem(shmMiscLineItemSubDAO.findByShpInstIds(shipmentInstIdsWithParents, entityManager)))
				.stream().collect(Collectors.groupingBy(MiscLineItem::getShipmentInstId));

		Map<Long, List<CustomsBond>> customsBondsByShmInstId = CollectionUtils.emptyIfNull(
			EntityTransformer.toCustomsBond(customBonds))
				.stream().collect(Collectors.groupingBy(CustomsBond::getShipmentInstId));
		
		Map<Long, List<Remark>> remarksByShmInstId = CollectionUtils.emptyIfNull(
			EntityTransformer.toRemark(shmRemarkSubDAO.findByShpInstIds(shipmentInstIdsWithParents, entityManager)))
				.stream().collect(Collectors.groupingBy(Remark::getShipmentInstId));		
		
		for (ShmShipment shipment : shipments) {

			documentDetails = new FBDSDocument();
			String proNumber = shipment.getProNbrTxt();
			String formattedProNumber = FBDSCopyBillUtil.formatFBDSPro(proNumber);
			ShmShipment parentShipment = null;

			final Shipment shipmentDto = EntityTransformer.toShipment(shipment);

            List<DataValidationError> shipmentWarnings =
                validateShipment(mode, documentFormTypeCd, shipmentDto, reprintInd);
            validationWarnings.addAll(shipmentWarnings);

            // If shipment has errors we move to next shipment.
            // We don't want to generate document or update entity for it.
            if (CollectionUtils.isNotEmpty(shipmentWarnings)) {
            	continue;
            }
                
            shipmentEntitiesToUpdate.add(shipment);
            
			try {
				documentDetails.setPickupDate(TimestampUtil.toStringDateFormat(shipmentDto.getPickupDate(), DATE_HYPHEN, DATE_SLASH));
			} catch (ParseException e1) {
				LOGGER.error("Unable to parse PickupDate. ");
			}
			documentDetails.setDeliveryQualifierCd(shipmentDto.getDeliveryQualifierCd());
			documentDetails.setOriginSic(shipmentDto.getOriginTerminalSicCd());
			documentDetails.setDestinationSic(shipmentDto.getDestinationTerminalSicCd());
			documentDetails.setFbdsPrintCount(BasicTransformer.toBigInteger(shipmentDto.getFbdsPrintCount()));
			documentDetails.setProNbr(FBDSCopyBillUtil.formatFBDSPro(shipmentDto.getProNbr()));
			documentDetails.setBillClassCd(shipmentDto.getBillClassCd());
			documentDetails.setChargeToCd(shipmentDto.getChargeToCd());
			documentDetails.setBillStatusCd(shipmentDto.getBillStatusCd());

			List<AsMatchedParty> asMatchedPartyList = matchedPartiesByShmInstId.get(shipment.getShpInstId());

			List<Movement> movements = movementsByShmInstIds.get(shipment.getShpInstId());
			List<HandlingUnit> asHandlingUnitList = handlingUnitsByShmInstId.get(shipment.getShpInstId());
			documentDetails.setHandlingUnits(asHandlingUnitList);

			documentDetails.setTrackWithHandlingUnitsInd(
                nonLegacyProPrefixes.contains(shipment.getProNbrTxt().substring(1, 4)));

			Map<MatchedPartyTypeCd, List<AsMatchedParty>> shipmentAsMatchedPartyMap = CollectionUtils.emptyIfNull(asMatchedPartyList)
					.stream()
					.filter(
							cust -> MatchedPartyTypeCd.SHPR.equals(cust.getTypeCd())
							|| MatchedPartyTypeCd.CONS.equals(cust.getTypeCd())
							|| MatchedPartyTypeCd.BILL_TO_INB.equals(cust.getTypeCd())
							|| MatchedPartyTypeCd.BILL_TO_OTB.equals(cust.getTypeCd()))
					.collect(Collectors.groupingBy(AsMatchedParty::getTypeCd));

			if (StringUtils.isNotEmpty(shipmentDto.getRoutePrefix()) && StringUtils.isNotEmpty(shipmentDto.getRouteSuffix())) {
				documentDetails.setDeliveryRoute(String.format("%s%s%s", shipmentDto.getRoutePrefix(),
						StringUtils.LF, shipmentDto.getRouteSuffix()));
			}

			Map<MatchedPartyTypeCd, List<AsMatchedParty>> parentAsMatchedPartyMap = new HashMap<>();

			List<AccessorialService> accesorialServiceList = accessorialServicesByShmInstId.get(shipment.getShpInstId());
			double codAmount = 0d;
			Double ctsRevAmount = 0d;
			double advancedChargeAmount = 0d;
			double beyondChargeAmount = 0d;
			String clearanceBillText = null;
			String clearanceMovrPiecesText = null;
			MovementException mostRecentMovementException = null;
			Long exceptionPiecesCount = 0L;
			List<String> srnResponseList = new ArrayList<>();
			Boolean collectMoneyAtDeliveryBoth = false;
			boolean cashOnly = false;
			double frtChargeAmount = 0d;
			StringBuilder descriptions = new StringBuilder();
			String formattedParentPro = StringUtils.EMPTY;
			boolean supprRatesCharges = false;
			Shipment parentShipmentDto = null;
			List<Movement> parentMovements = null;
			List<String> childPros = null;
		
			List<AsMatchedParty> parentAsMatchedPartyList = null;

			//Get child shipments if any
			List<ShmShipment> childShipments = relatedShipments.stream()
					.filter(shm -> shm.getParentInstId() != null 
						 && shipment.getShpInstId() == shm.getParentInstId().longValue()).collect(Collectors.toList());

			childPros = (CollectionUtils.emptyIfNull(childShipments)
			.stream()
			.map(child -> FBDSCopyBillUtil.formatFBDSPro(child.getProNbrTxt()))
			.collect(Collectors.toList()));

			if (!Objects.isNull(shipment.getParentInstId())) {
				parentShipment = parentShipmentsByShmInstId.get(shipment.getParentInstId().longValue());
        
				if (!Objects.isNull(parentShipment)) {
					parentShipmentDto = EntityTransformer.toShipment(parentShipment);
					parentMovements = movementsByShmInstIds.get(parentShipment.getShpInstId());
					formattedParentPro = FormatHelper.formatProNbrNumber(parentShipmentDto.getProNbr(), txnContext);

					if (matchedPartiesByShmInstId.containsKey(parentShipment.getShpInstId())) {
						parentAsMatchedPartyList = matchedPartiesByShmInstId.get(parentShipment.getShpInstId());
						parentAsMatchedPartyMap = CollectionUtils.emptyIfNull(parentAsMatchedPartyList)
								.stream()
								.filter(
										cust -> MatchedPartyTypeCd.SHPR.equals(cust.getTypeCd())
										|| MatchedPartyTypeCd.CONS.equals(cust.getTypeCd())
										|| MatchedPartyTypeCd.BILL_TO_INB.equals(cust.getTypeCd())
										|| MatchedPartyTypeCd.BILL_TO_OTB.equals(cust.getTypeCd()))
								.collect(Collectors.groupingBy(AsMatchedParty::getTypeCd));
					}

					mostRecentMovementException = util.getMovementExceptionForShipment(parentMovements, movementExceptionsByShmInstId);

					if (DeliveryQualifierCd.PARTIAL_SHORT.equals(shipmentDto.getDeliveryQualifierCd())) {
						mostRecentMovementException = util.getMovementExceptionForShipment(movements, movementExceptionsByShmInstId);
					}
				}
			}

			boolean printClearanceBill = false;

            if (documentFormTypeCd == DocumentFormTypeCd.FBDS
                && (BillClassCd.ASTRAY_FRT_SEGMENT.equals(shipmentDto.getBillClassCd())
                    || checkShipmentPartialShortHistory(shipmentDto, movements))) {
				if (BillClassCd.ASTRAY_FRT_SEGMENT.equals(shipmentDto.getBillClassCd())) {
					if (parentShipmentDto != null && CollectionUtils.isNotEmpty(parentMovements)) {
						if (exceptionPiecesCount <= 0)
							mostRecentMovementException = util.getMovementExceptionForShipment(parentMovements, movementExceptionsByShmInstId);
						if (mostRecentMovementException != null && mostRecentMovementException.getPiecesCount() != null)
							exceptionPiecesCount = mostRecentMovementException.getPiecesCount();

						if (exceptionPiecesCount > 0)
							printClearanceBill = true;

					}
				} else if (checkShipmentPartialShortHistory(shipmentDto, movements)) {
					printClearanceBill = true;
				}

			}

			if (printClearanceBill) {
				if (exceptionPiecesCount <= 0) {
					mostRecentMovementException = util.getMovementExceptionForShipment(movements, movementExceptionsByShmInstId);
					if (mostRecentMovementException != null && mostRecentMovementException.getPiecesCount() != null) {
						exceptionPiecesCount = mostRecentMovementException.getPiecesCount();
					}
				}

				if (shipmentDto.getHazmatInd())
					documentDetails.setMovrClearanceBillHazMatText(HAZMAT_COMMODITIES_MESSAGE);

				clearanceBillText = FBDSCopyBillUtil.CLEARANCE_BILL;
				documentDetails.setMovrClearanceBillText(clearanceBillText);
				if (exceptionPiecesCount > 0) {
					clearanceMovrPiecesText = String.format("%s%d%s", FBDSCopyBillUtil.ORIGINAL_BILL_WAS,
							exceptionPiecesCount, FBDSCopyBillUtil.PIECES_SHORT);
					documentDetails.setMovrClearancePiecesOutstandingText(clearanceMovrPiecesText);
				}

				if (BillClassCd.ASTRAY_FRT_SEGMENT.equals(shipmentDto.getBillClassCd())
						&& parentShipmentDto != null && parentShipmentDto.getHazmatInd()) {
					documentDetails.setMovrClearanceBillHazMatText(HAZMAT_COMMODITIES_MESSAGE);
				}
			}

			if (!BillClassCd.ASTRAY_FRT_SEGMENT.equals(shipmentDto.getBillClassCd())
					|| DeliveryQualifierCd.PARTIAL_SHORT.equals(shipmentDto.getDeliveryQualifierCd())) {

                Map<Boolean, List<AsMatchedParty>> list = CollectionUtils
                    .emptyIfNull(asMatchedPartyList)
                    .stream()
                    .filter(e -> (MatchedPartyTypeCd.BILL_TO_INB.equals(e.getTypeCd())
                            || MatchedPartyTypeCd.BILL_TO_OTB.equals(e.getTypeCd())))
                    .collect(Collectors
                        .partitioningBy(x -> BillToRelationCd.RELATED_TO_BOTH.equals(x.getBillToRelationshipCd())
                                || BillToRelationCd.RELATED_TO_CONS.equals(x.getBillToRelationshipCd())));
                if (CollectionUtils.isNotEmpty(list.get(false))) {
                    supprRatesCharges = true;
                } else {
                    supprRatesCharges = CollectionUtils.emptyIfNull(list.get(true)).stream().anyMatch(
                        x -> (MatchedPartyTypeCd.CONS.equals(x.getTypeCd()) && !x.getDebtorInd()));
                }
			}

			List<MiscLineItem> miscLineItems = new ArrayList<>();

            if (BillClassCd.ASTRAY_FRT_SEGMENT.equals(shipmentDto.getBillClassCd())
                && printClearanceBill
                && documentFormTypeCd == DocumentFormTypeCd.FBDS) {
				miscLineItems = miscLineItemsByShmInstId.get(parentShipment.getShpInstId());
			} else {
				miscLineItems = miscLineItemsByShmInstId.get(shipment.getShpInstId());
			}

			final Optional<MiscLineItem> codAmountOptional = CollectionUtils.emptyIfNull(miscLineItems)
					.stream()
					.filter(miscLine -> MiscLineItemCd.COD_AMT.equals(miscLine.getLineTypeCd()))
					.findFirst();
			codAmount = codAmountOptional.isPresent() ? codAmountOptional.get().getAmount() : 0d;

			if (codAmount != 0d) {
				documentDetails.setConsigneeMessage("COD");
			}

			if (Objects.nonNull(shipmentAsMatchedPartyMap)) {
				fbdsAttributes = util.retrieveCollectMoneyAtDeliveryMethod(
						codAmount,
						shipmentDto,
						accesorialServiceList,
						asMatchedPartyList,
						miscLineItems,
						collectMoneyAtDeliveryBoth,
						cashOnly,
						frtChargeAmount);
			}

			if (StringUtils.isNotBlank(clearanceBillText)
					|| !BillClassCd.ASTRAY_FRT_SEGMENT.equals(shipmentDto.getBillClassCd())
					|| !DeliveryQualifierCd.PARTIAL_SHORT.equals(shipmentDto.getDeliveryQualifierCd())) {
				documentDetails.setTotalChargeAmountTextLine1(TOTAL);
				documentDetails.setTotalChargeAmountTextLine2(shipmentDto.getChargeToCd().value().toUpperCase());
				if ((BillStatusCd.UNBILLED.equals(shipmentDto.getBillStatusCd())
						|| BillStatusCd.UNBILLED.equals(shipmentDto.getBillStatusCd())
						|| BillStatusCd.UNBILLED.equals(shipmentDto.getBillStatusCd()))
						&& shipmentDto.getRevenueBillInd()) {
                    if (documentFormTypeCd == DocumentFormTypeCd.COPY_BILL) {
						documentDetails.setTotalAmount(UNRATED);
					} else if (!supprRatesCharges && !Objects.isNull(collectMoneyAtDeliveryBoth)
							&& ChargeToCd.COLL.equals(shipmentDto.getChargeToCd())) {
						documentDetails
						.setTotalAmount(String.valueOf(shipmentDto.getTotalChargeAmount().doubleValue() * 100d));
					}

					documentDetails.setTotalPieces(shipmentDto.getTotalPiecesCount());
					documentDetails
					.setTotalWeight(shipmentDto.getTotalWeightLbs().doubleValue() * 100d);
				}
			}

			// Format reference line text for PRO number
			String formattedProNbrNumber = FormatHelper.formatProNbrNumber(shipmentDto.getProNbr(), txnContext);
			formattedProNbrNumber = FBDSCopyBillUtil.formatFBDSPro(formattedProNbrNumber);

            if (documentFormTypeCd == DocumentFormTypeCd.COPY_BILL) {
				if (BillClassCd.SPLIT_SEGMENT.equals(shipmentDto.getBillClassCd())
						&& CollectionUtils.isNotEmpty(shipment.getShmShipments())) {
					descriptions.append("PART LOT SHIPMENT MOVING ON PROS".concat(StringUtils.LF));
				} else if (BillClassCd.SPLIT_SEGMENT.equals(shipmentDto.getBillClassCd())
						&& CollectionUtils.isEmpty(shipment.getShmShipments())) {
					descriptions.append("PART LOT SHIPMENT MOVING ON UNBILLED PROS".concat(StringUtils.LF));
				}

				if (BillClassCd.MASTER_SHPMT.equals(shipmentDto.getBillClassCd())
						&& CollectionUtils.isNotEmpty(shipment.getShmShipments())) {
					descriptions.append("MASTER BILL: POOL SHPMNT MOVING ON PROS".concat(StringUtils.LF));
				} else if (BillClassCd.MASTER_SHPMT.equals(shipmentDto.getBillClassCd())
						&& CollectionUtils.isEmpty(shipment.getShmShipments())) {
					descriptions.append("MASTER BILL: POOL SHPMNT MOVING ON UNBILLED PROS".concat(StringUtils.LF));
				}

				if (BillClassCd.SPLIT_SEGMENT.equals(shipmentDto.getBillClassCd())) {
					documentDetails.setEquipmentNbr(PART_LOT);
					//If BillClassCd = D, and there are children for the parent, display them below
					if (CollectionUtils.isNotEmpty(childPros)) {
						documentDetails.setChildProNbrs(childPros);
					}
				} else {
					DeliveryShipmentSearchRecord elasticRecord = elasticRecordsByShmInstId.get(shipmentDto.getShipmentInstId());
					if(elasticRecord != null)
						documentDetails.setEquipmentNbr(FormatterUtils.formatEquipment(
								elasticRecord.getEquipmentIdPrefix(), elasticRecord.getEquipmentIdSuffix()));
				}

				if (BillClassCd.ASTRAY_FRT_SEGMENT.equals(shipmentDto.getBillClassCd())) {
					descriptions.append(
							"TO MOVE FRT SHORT ON THIS PRO. MOVEMENT PRO "
							.concat(formattedProNbrNumber)
							.concat(StringUtils.LF));
					formattedParentPro = FBDSCopyBillUtil.formatFBDSPro(formattedParentPro);
					documentDetails.setProNbr(formattedParentPro.concat("*"));
					documentDetails.setChildProNbr(formattedProNumber);
				}

            }
            else if (documentFormTypeCd == DocumentFormTypeCd.FBDS
					&& BillClassCd.ASTRAY_FRT_SEGMENT.equals(shipmentDto.getBillClassCd())) {

				if (printClearanceBill) {
					descriptions.append(
							"MOVEMENT PRO ".concat(formattedProNbrNumber).concat(" IS FOR THE FOLLOWING ITEMS FOUND AT ")
							.concat(shipmentDto.getOriginTerminalSicCd())
							.concat(StringUtils.LF));
				} else {
					descriptions.append(
							"TO MOVE FRT SHORT ON THIS PRO. MOVEMENT PRO "
							.concat(formattedProNbrNumber)
							.concat(StringUtils.LF));
				}
				formattedParentPro = FBDSCopyBillUtil.formatFBDSPro(formattedParentPro);
				documentDetails.setProNbr(formattedParentPro.concat("*"));
				documentDetails.setChildProNbr(formattedProNumber);
			}

            if (documentFormTypeCd == DocumentFormTypeCd.FBDS) {
            	DeliveryShipmentSearchRecord elasticRecord = elasticRecordsByShmInstId.get(shipmentDto.getShipmentInstId());
				if(elasticRecord != null)					
					documentDetails.setEquipmentNbr(FormatterUtils.formatEquipment(elasticRecord.getEquipmentIdPrefix(),
							elasticRecord.getEquipmentIdSuffix()));
				if (shipmentDto.getFbdsPrintCount() > 1) {
					documentDetails.setNbrOfCopiesText(String.format("%d%s", shipmentDto.getFbdsPrintCount(), "C"));
				}
			}

			switch (shipmentDto.getBillClassCd()) {
			case PARTIAL_SEGMENT:
				formattedParentPro = FBDSCopyBillUtil.formatFBDSPro(formattedParentPro);
				documentDetails.setProNbr(formattedParentPro.concat("*"));
				documentDetails.setChildProNbr(formattedProNumber);
				descriptions.append("PART LOT SHPT-SEGMENT PRO ".concat(formattedProNumber).concat(StringUtils.LF));
				break;

			case MASTER_SEGMENT:
				descriptions.append("SHPT SEG COV BY M/B ".concat(formattedParentPro).concat(StringUtils.LF));
				break;

			case EXPEDITE:
				descriptions.append("EXPD-DO NOT DELIVER ON THIS BILL ".concat(StringUtils.LF));
				break;

			case CO_BUS_SHPMT:
				descriptions.append("DH CO BIZ ".concat(StringUtils.LF));
				break;

			case GENERAL_CLAIMS_BUS_SHPMT:
				descriptions.append("GENERAL CLAIMS BUSINESS ".concat(StringUtils.LF));
				break;

			default:
				break;
			}

			AsMatchedParty consignee = null;
			CustomsBond customsBond = null;
			AsMatchedParty shipper = null;
			Long cisCustNbr = null;

			String inBondDescription = null;

			if (BillClassCd.ASTRAY_FRT_SEGMENT.equals(shipmentDto.getBillClassCd()) && printClearanceBill) {
				//set parent shipment details for the following, on the response
				if (MapUtils.isNotEmpty(parentAsMatchedPartyMap)) {

					if (parentAsMatchedPartyMap.containsKey(MatchedPartyTypeCd.SHPR))
						shipper = parentAsMatchedPartyMap.get(MatchedPartyTypeCd.SHPR).get(0);

					if (parentAsMatchedPartyMap.containsKey(MatchedPartyTypeCd.CONS))
						consignee = parentAsMatchedPartyMap.get(MatchedPartyTypeCd.CONS).get(0);

                    // Set Bill-To info of parent shipment
                    util.setBillToCustomer
                        (matchedPartiesByShmInstId.get(parentShipment.getShpInstId()),
                         parentShipmentDto,
                         documentDetails,
                         documentFormTypeCd);

					cisCustNbr = getCustNbr(parentAsMatchedPartyList);
				}

				try {
					documentDetails.setPickupDate(TimestampUtil.toStringDateFormat(parentShipmentDto.getPickupDate(), DATE_HYPHEN, DATE_SLASH));
				} catch (ParseException e1) {
					LOGGER.error("Unable to parse PickupDate. ");
				}
				documentDetails.setOriginSic(parentShipmentDto.getOriginTerminalSicCd());
				documentDetails.setDestinationSic(parentShipmentDto.getDestinationTerminalSicCd());

				//setting the inbond text if parent is there

				if (!Objects.isNull(parentShipment)) {
					List<CustomsBond> parentCustomsBonds = customsBondsByShmInstId.get(parentShipment.getShpInstId());
					if(CollectionUtils.isNotEmpty(parentCustomsBonds)
						&& StringUtils.isNotBlank(parentCustomsBonds.get(0).getBondNbr())) {
						CustomsBond parentCustomsBond = parentCustomsBonds.get(0);
	
						documentDetails.setCollectorOfCustomMessage(
								COLLECTOR_OF_CUSTOMS.concat(parentCustomsBond.getCity())
								.concat(COMMA)
								.concat(StringUtils.SPACE)
								.concat(parentCustomsBond.getStateCd()));
						consignee.setName1(NOTIFY.concat(consignee.getName1()));
						inBondDescription = IN_BOND.concat(parentCustomsBond.getBondNbr());
					}
				}

			} else {
				//set input Pro shipment details
				if (Objects.nonNull(shipmentAsMatchedPartyMap)) {

					if(shipmentAsMatchedPartyMap.containsKey(MatchedPartyTypeCd.SHPR))
						shipper = shipmentAsMatchedPartyMap.get(MatchedPartyTypeCd.SHPR).get(0);

					if (shipmentAsMatchedPartyMap.containsKey(MatchedPartyTypeCd.CONS))
						consignee = shipmentAsMatchedPartyMap.get(MatchedPartyTypeCd.CONS).get(0);

                    // Set Bill-To info
                    util.setBillToCustomer
                        (asMatchedPartyList,
                         shipmentDto,
                         documentDetails,
                         documentFormTypeCd);

					cisCustNbr = getCustNbr(asMatchedPartyList);
				}

				List<CustomsBond> customsBonds = customsBondsByShmInstId.get(shipment.getShpInstId());
				if (CollectionUtils.isNotEmpty(customsBonds)) {
					customsBond = customsBonds.get(0);
				}

				if (!Objects.isNull(customsBond) && StringUtils.isNotBlank(customsBond.getBondNbr())) {
					documentDetails.setCollectorOfCustomMessage(
							COLLECTOR_OF_CUSTOMS.concat(customsBond.getCity())
							.concat(COMMA)
							.concat(StringUtils.SPACE)
							.concat(customsBond.getStateCd()));
					consignee.setName1(NOTIFY.concat(consignee.getName1()));
					inBondDescription = IN_BOND.concat(customsBond.getBondNbr());
				}

			}


			if (BillClassCd.PARTIAL_SEGMENT.equals(shipmentDto.getBillClassCd())) {
				String originalSicCountryCd = null;
				String destSicCountryCd = null;

				if (StringUtils.isNotBlank(shipmentDto.getOriginTerminalSicCd())) {
					GetLocReferenceDetailsBySicResp locationInfo = restClient
							.getLocReferenceDetailsBySic(shipmentDto.getOriginTerminalSicCd(), txnContext);
					originalSicCountryCd = StringUtils.isNotBlank(locationInfo.getLocReference().getSltCountryCd()) ?
							locationInfo.getLocReference().getSltCountryCd() :
								locationInfo.getLocConWayCorporation().getCountryCd();
				}

				if (StringUtils.isNotBlank(shipmentDto.getDestinationTerminalSicCd())
						&& shipmentDto.getDestinationTerminalSicCd() != shipmentDto.getOriginTerminalSicCd()) {
					GetLocReferenceDetailsBySicResp locationInfo = restClient
							.getLocReferenceDetailsBySic(shipmentDto.getDestinationTerminalSicCd(), txnContext);
					destSicCountryCd = StringUtils.isNotBlank(locationInfo.getLocReference().getSltCountryCd()) ?
							locationInfo.getLocReference().getSltCountryCd() :
								locationInfo.getLocConWayCorporation().getCountryCd();
				} else if (shipmentDto.getDestinationTerminalSicCd() != shipmentDto.getOriginTerminalSicCd()) {
					destSicCountryCd = originalSicCountryCd;
				}

				if (!Objects.isNull(parentShipment)) {
					List<CustomsBond> parentCustomsBonds = customsBondsByShmInstId.get(parentShipment.getShpInstId());
					if(CollectionUtils.isNotEmpty(parentCustomsBonds)
							&& StringUtils.isNotBlank(parentCustomsBonds.get(0).getBondNbr())) {
						customsBond = parentCustomsBonds.get(0);

						documentDetails.setCollectorOfCustomMessage(
								COLLECTOR_OF_CUSTOMS
								.concat(customsBond.getCity())
								.concat(COMMA)
								.concat(StringUtils.SPACE)
								.concat(customsBond.getStateCd()));
						consignee.setName1(
								NOTIFY.concat(parentAsMatchedPartyMap.get(MatchedPartyTypeCd.CONS).get(0).getName1()));
						inBondDescription = IN_BOND.concat(customsBond.getBondNbr());
					}
				}
			} else if (!Objects.isNull(consignee)) {
				consignee
				.setName1(consignee.getName1());
			}


			documentDetails.setShipper(shipper);

			documentDetails.setConsignee(consignee);

			documentDetails.setWarrantyText(FBDSCopyBillUtil.getWarrantyText(shipmentDto));
			documentDetails.setFreezableText(FBDSCopyBillUtil.getFreezableText(shipmentDto));
			documentDetails.setInBondDescription(inBondDescription);

			boolean isRestricted = getCustomerRestrictedInd(cisCustNbr, txnContext);

            // Set Rates and Charges
            boolean displayRatesCharges =
                util.displayRatesAndCharges
                    (isRestricted,
                     shipmentDto,
                     documentFormTypeCd,
                     fbdsAttributes,
                     supprRatesCharges,
                     shipmentAsMatchedPartyMap);
            util.setRatesAndCharges
                (miscLineItems,
                 shipmentDto,
                 documentDetails,
                 documentFormTypeCd,
                 fbdsAttributes,
                 supprRatesCharges,
                 isRestricted,
                 displayRatesCharges);

			boolean accessorialsSuppressed = false;

			// Accessorials information
			List<AccessorialService> displayAccsList = Lists.newArrayList();

            if (!DeliveryQualifierCd.PARTIAL_SHORT.equals(shipmentDto.getDeliveryQualifierCd())
                || documentFormTypeCd == DocumentFormTypeCd.COPY_BILL) {
            	if(CollectionUtils.isNotEmpty(accesorialServiceList)) {
                    for (AccessorialService accSvc: accesorialServiceList) {
                        if (!util.suppressAccessorialDisplay
                                (accSvc,
                                 shipmentDto,
                                 isRestricted,
                                 displayRatesCharges,
                                 documentFormTypeCd)) {
    						if (StringUtils.containsAny(accSvc.getAccessorialCd(), GUR_ACCS_CODE, GFR_ACCS_CODE)) {
    							accSvc.setDescription(
    									ASTERIKS_CONSTANT.concat(accSvc.getDescription()).concat(ASTERIKS_CONSTANT));
    						}

                            if (documentFormTypeCd == DocumentFormTypeCd.FBDS
                                && (displayRatesCharges
                                    || (ChargeToCd.BOTH.equals(shipmentDto.getChargeToCd())
                                        && !isRestricted))
                                && accSvc.getPrepaidPercentage() != 100) {
    							if (accSvc.getMinimumChargeInd()) {
                                    accSvc.setTariffsRate(accSvc.getTariffsRate());
    							} else {
    								if (accSvc.getTariffsRate() != 0)
    									accSvc.setTariffsRate(accSvc.getTariffsRate());
    							}
                            }
                            else if (documentFormTypeCd == DocumentFormTypeCd.COPY_BILL) {
    							// no need to check amounts
    						} else {
    							accSvc.setAmount(null);
    							accSvc.setTariffsRate(null);
    						}

    						displayAccsList.add(accSvc);

    					} else {
    						accessorialsSuppressed = true;
    					}

    				}
            	}
			}

			Map<AdvanceBeyondTypeCd, List<AdvanceBeyondCarrier>> shipmentCarrierMap = new HashMap<>();

			List<AdvanceBeyondCarrier> advanceBeyondCarriers = new ArrayList<>();

            if (BillClassCd.ASTRAY_FRT_SEGMENT.equals(shipmentDto.getBillClassCd())
                && printClearanceBill
                && documentFormTypeCd == DocumentFormTypeCd.FBDS) {
				advanceBeyondCarriers = advanceBeyondCarriersByShmInstId.get(parentShipment.getShpInstId());
			} else {
				advanceBeyondCarriers = advanceBeyondCarriersByShmInstId.get(shipment.getShpInstId());
			}

			if (CollectionUtils.isNotEmpty(advanceBeyondCarriers)) {
				shipmentCarrierMap = CollectionUtils.emptyIfNull(advanceBeyondCarriers)
						.stream()
						.collect(Collectors.groupingBy(AdvanceBeyondCarrier::getTypeCd));

				processAdvanceCarrier(documentDetails, shipmentCarrierMap, txnContext);
			}

			DeliveryNotification appointment = recentAppointmentsByShmInstId.get(shipment.getShpInstId());
			processNotificationDetails(shipment.getShpInstId(), documentDetails, shipmentCarrierMap, appointment, txnContext);

			documentDetails.setFinalItemsDescription(descriptions.toString());

			//Get revenue amounts -
			if (displayRatesCharges && !accessorialsSuppressed) {

				if (Objects.nonNull(shipmentCarrierMap)) {
					advancedChargeAmount = shipmentCarrierMap.containsKey(AdvanceBeyondTypeCd.ADV_CARR) ?
							shipmentCarrierMap.get(AdvanceBeyondTypeCd.ADV_CARR).get(0).getChargeAmount() : 0d;

					if (advancedChargeAmount != 0)
						documentDetails.setAdvancedRevenueAmount(BigDecimal.valueOf(advancedChargeAmount));

					beyondChargeAmount = shipmentCarrierMap.containsKey(AdvanceBeyondTypeCd.BYD_CARR) ?
							shipmentCarrierMap.get(AdvanceBeyondTypeCd.BYD_CARR).get(0).getChargeAmount() : 0d;
					if (beyondChargeAmount != 0)
						documentDetails.setBeyondRevenueAmount(BigDecimal.valueOf(beyondChargeAmount));
				}

                if (BillClassCd.ASTRAY_FRT_SEGMENT.equals(shipmentDto.getBillClassCd())
                    && printClearanceBill
                    && documentFormTypeCd == DocumentFormTypeCd.FBDS) {
					ctsRevAmount = parentShipment.getTotChrgAmt().doubleValue()
							- (advancedChargeAmount + beyondChargeAmount + codAmount);
				} else {
					ctsRevAmount = shipment.getTotChrgAmt().doubleValue()
							- (advancedChargeAmount + beyondChargeAmount + codAmount);
				}

				if (ctsRevAmount != 0)
					documentDetails.setCtsRevenueAmount(
							BasicTransformer.toBigDecimal(ctsRevAmount).setScale(2, RoundingMode.HALF_UP));
			}

            if (BillClassCd.ASTRAY_FRT_SEGMENT.equals(shipmentDto.getBillClassCd())
                && printClearanceBill
                && documentFormTypeCd == DocumentFormTypeCd.FBDS) {
                util.setRemarks
                    (documentFormTypeCd,
                     parentShipmentDto,
                     shipmentAsMatchedPartyMap,
                     documentDetails,
                     remarksByShmInstId.get(parentShipment.getShpInstId()),
                     displayRatesCharges,
                     fbdsAttributes);
            }
            else {
                util.setRemarks
                    (documentFormTypeCd,
                     shipmentDto,
                     shipmentAsMatchedPartyMap,
                     documentDetails,
                     remarksByShmInstId.get(shipment.getShpInstId()),
                     displayRatesCharges,
                     fbdsAttributes);
            }

			if (CollectionUtils.isNotEmpty(displayAccsList))
				documentDetails.setAccessorials(displayAccsList);

            FBDSCopyBillUtil.setTotalCounts
                (printClearanceBill,
                 shipmentDto,
                 documentDetails,
                 documentFormTypeCd,
                 displayRatesCharges,
                 accessorialsSuppressed,
                 fbdsAttributes);

			final List<String> customerReferenceNbrs = new ArrayList<>();
			final List<String> poNbrs = new ArrayList<>();
			final List<String> otherReferenceNbrs = new ArrayList<>();

            if (BillClassCd.ASTRAY_FRT_SEGMENT.equals(shipmentDto.getBillClassCd())
                && printClearanceBill
                && documentFormTypeCd == DocumentFormTypeCd.FBDS) {
				srnResponseList = util.retrieveSrnInfo(parentShipment, customerReferenceNbrs, poNbrs, otherReferenceNbrs);
			} else {
				srnResponseList = util.retrieveSrnInfo(shipment, customerReferenceNbrs, poNbrs, otherReferenceNbrs);
			}

            documentDetails.setEstimatedDeliveryDate(BasicTransformer.toTrimmedString(shipment.getEstimatedDlvrDt()));
			documentDetails.setShipperNbr(srnResponseList.stream().collect(Collectors.joining(StringUtils.LF)));
			documentDetails.setCustomerReferenceNbrs(customerReferenceNbrs);
			documentDetails.setPoNbrs(poNbrs);
			documentDetails.setOtherReferenceNbrs(otherReferenceNbrs);
			documentDetails.setShipmentInstId(shipment.getShpInstId());

			// Comodity lines info
			List<Commodity> shipmentCommodities = commoditiesByShmInstId.get(shipment.getShpInstId());

            FBDSCopyBillUtil.setCommodities
                (documentDetails,
                 shipmentCommodities,
                 shipmentDto,
                 documentFormTypeCd,
                 exceptionPiecesCount,
                 printClearanceBill,
                 displayRatesCharges);

			documentList.add(documentDetails);
		}


		//Update fbds count for shipments if shipment passed the error checks
		List<String> prosWithErrors = CollectionUtils.emptyIfNull(validationWarnings).stream()
				.map(error -> error.getFieldValue()).collect(Collectors.toList());
		if(CollectionUtils.isNotEmpty(shipmentEntitiesToUpdate)) {
			for (ShmShipment shipmentEntity : shipmentEntitiesToUpdate) {
				if(!prosWithErrors.contains(shipmentEntity.getProNbrTxt())) {
					if (shouldFbdsPrintCntIncremented) {
    					shipmentEntity.setFbdsPrintCnt(new BigDecimal(BasicTransformer.toInt(shipmentEntity.getFbdsPrintCnt()) + 1));
    		            DtoTransformer.setLstUpdateAuditInfo(shipmentEntity, auditInfo);
					}
				}
			}

			shmShipmentSubDAO.saveBulk(shipmentEntitiesToUpdate, entityManager);
			shmShipmentSubDAO.updateDB2ShmShipmentsBulk(shipmentEntitiesToUpdate,
				txnContext, db2EntityManager);
		}
		
		response.setFbdsDocuments(documentList);
        response.setValidationErrors(validationWarnings);
		return response;
	}

	private boolean checkShipmentPartialShortHistory(Shipment shipmentDto, List<Movement> movementsList) {
		boolean result = false;
		if(DeliveryQualifierCd.PARTIAL_SHORT.equals(shipmentDto.getDeliveryQualifierCd())) {
			result = true;
		}
		else {
			Comparator<Movement> reverseComparator = new Comparator<Movement>() {
				
				//Making sure we use REVERSED order
				@Override
				public int compare(Movement m1, Movement m2) {
					return m2.getAuditInfo().getCreatedTimestamp().compare(m1.getAuditInfo().getCreatedTimestamp());
				}
			};
			List<Movement> movements = CollectionUtils.emptyIfNull(movementsList).stream()
			.sorted(reverseComparator).collect(Collectors.toList());

			boolean foundInterveningRecord = false;
			if(CollectionUtils.isNotEmpty(movements)) {
				for (Movement movement : movements) {
					if(foundInterveningRecord) {
						if(StringUtils.equals(movement.getDeliveryQualifierCd(), MVMNT_DLVY_QLFR_CD_PART_SHORT)) {
							result = true;
							break;
						}
						else {
							foundInterveningRecord = false;
						}
					}

					if(StringUtils.equals(movement.getDeliveryQualifierCd(), MVMNT_DLVY_QLFR_CD_TRAPPED_SHIPMENT)
							|| StringUtils.equals(movement.getDeliveryQualifierCd(), MVMNT_DLVY_QLFR_CD_HOLD_FOR_CUSTOMS)
							|| StringUtils.equals(movement.getDeliveryQualifierCd(), MVMNT_DLVY_QLFR_CD_HOLD_FOR_APPT)
							|| StringUtils.equals(movement.getDeliveryQualifierCd(), MVMNT_DLVY_QLFR_CD_CARTAGE)
							|| StringUtils.equals(movement.getDeliveryQualifierCd(), MVMNT_DLVY_QLFR_CD_ATTEMPTED_DLVY)
							|| StringUtils.equals(movement.getDeliveryQualifierCd(), MVMNT_DLVY_QLFR_CD_REFUSED_DLVY)
							|| StringUtils.equals(movement.getDeliveryQualifierCd(), MVMNT_DLVY_QLFR_CD_DAMAGED_SHPMT)
							|| StringUtils.equals(movement.getDeliveryQualifierCd(), MVMNT_DLVY_QLFR_CD_RETURN_STATUS)
							|| StringUtils.equals(movement.getDeliveryQualifierCd(), MVMNT_DLVY_QLFR_CD_TRANSFER)) {
						foundInterveningRecord = true;
					}
				}
			}
			
		}

		return result;
	}

	/**
	 * Method to get Customer restricted indicator from CIS API
	 * @param cisCustNbr
	 * @param txnContext
	 * @return
	 * @throws ServiceException
	 */
	private boolean getCustomerRestrictedInd(Long cisCustNbr, TransactionContext txnContext) throws ServiceException {

		if (cisCustNbr == null || cisCustNbr <= 0)
			return false;

		DetermineRestrictedBillToResp custRestrictedResp = restClient.getCustomerRestrictedInfo(cisCustNbr, txnContext);

		if (custRestrictedResp != null)
			return custRestrictedResp.isBillToIsRestrictedInd();

		return false;
	}

	/**
	 * Method to get Bill-To customer number
	 * @param asMatchedPartyList
	 * @return
	 */
	private Long getCustNbr(List<AsMatchedParty> asMatchedPartyList) {

		BigInteger bgCustNbr = CollectionUtils.emptyIfNull(asMatchedPartyList)
				.stream()
				.filter(cust -> MatchedPartyTypeCd.BILL_TO_INB.equals(cust.getTypeCd())
						|| MatchedPartyTypeCd.BILL_TO_OTB.equals(cust.getTypeCd()))
				.filter(Objects::nonNull)
				.map(AsMatchedParty::getCisCustNbr)
				.findFirst()
				.orElse(null);

		if (bgCustNbr != null) {
			return bgCustNbr.longValue();
		}

		return null;

	}


	private void processNotificationDetails(long shpInstId, FBDSDocument documentDetails,
			Map<AdvanceBeyondTypeCd, List<AdvanceBeyondCarrier>> shipmentCarrierMap,
			final DeliveryNotification mostRecentNotification, TransactionContext txnContext) {

		String message = null;
		String messageObservation = null;
		String fbdsMessage = null;
		String notificationDate = null;
		String notificationTime = null;
		String contactName = null;

		if (Objects.nonNull(mostRecentNotification)) {
			final boolean isAppointment = FBDSCopyBillUtil.isAppointment(mostRecentNotification);
			final AdvanceBeyondCarrier carrier = MapUtils.isNotEmpty(shipmentCarrierMap)
					&& Objects.nonNull(shipmentCarrierMap.get(AdvanceBeyondTypeCd.ADV_CARR))
							? shipmentCarrierMap.get(AdvanceBeyondTypeCd.ADV_CARR).get(0)
							: null;
			final boolean hasAdvanceBeyondCarrier = Objects.nonNull(carrier)
					&& (StringUtils.isNotBlank(carrier.getCarrierScacCd())
							|| StringUtils.isNotBlank(carrier.getCarrierProNbr())
							|| StringUtils.isNotBlank(carrier.getCarrierPickupDate()));

			message = isAppointment ? "APPT:" : "NTFD:";

			if (isAppointment) {
				if (hasAdvanceBeyondCarrier) {
					messageObservation = "*SEE APPT";
					message = StringUtils.EMPTY;
				}
			} else {
				if (hasAdvanceBeyondCarrier) {
					messageObservation = "*SEE NTFD";
					message = StringUtils.EMPTY;
				}
			}

			if (StringUtils.isNotBlank(mostRecentNotification.getFbdsNote())) {
				if (!hasAdvanceBeyondCarrier && StringUtils.isBlank(messageObservation)) {
					messageObservation = "*SEE DLVY NOTE";
				}

				fbdsMessage = "DLVY NOTE: " + mostRecentNotification.getFbdsNote();
			}

			notificationDate = FBDSCopyBillUtil.buildNotificationDate(mostRecentNotification, isAppointment);
			notificationTime = FBDSCopyBillUtil.buildNotificationTime(mostRecentNotification, isAppointment);
			contactName = mostRecentNotification.getContactName();
		}

		final ShipmentNotification notification = new ShipmentNotification();
		notification.setMessage(message);
		notification.setMessageObservation(messageObservation);
		notification.setFbdsMessage(fbdsMessage);
		notification.setNotificationDate(notificationDate);
		notification.setNotificationTime(notificationTime);
		notification.setContactName(contactName);
		if(notificationDataExists(notification))
			documentDetails.setNotification(notification);
	}

	private boolean notificationDataExists(final ShipmentNotification notification) {
		return (Objects.nonNull(notification.getContactName()) || Objects.nonNull(notification.getFbdsMessage()) || Objects.nonNull(notification.getMessage())
				|| Objects.nonNull(notification.getMessageObservation()) || Objects.nonNull(notification.getNotificationDate()) || Objects.nonNull(notification.getNotificationTime()));
	}

	private Map<Long, DeliveryNotification> getMostRecentNotification(List<Long> shpInstIds, TransactionContext txnContext) {
		ListAppointmentNotificationsForShipmentsResp listShipmentAppointmentNotificationsResp = null;
		Map<Long, DeliveryNotification> recentAppointmentsByShipmentInstId = new HashMap<Long, DeliveryNotification>();
		
		try {
			listShipmentAppointmentNotificationsResp = restClient.listShipmentAppointmentNotifications(shpInstIds, txnContext);
		} catch (Exception e) {
			//TODO add specific exception
			LOGGER.error("There was an error while retrieving notification: ", e.getCause());
		}

		if (Objects.isNull(listShipmentAppointmentNotificationsResp)) {
			return recentAppointmentsByShipmentInstId;
		}

		final List<DeliveryNotification> appointmentNotifications = listShipmentAppointmentNotificationsResp
				.getShipmentAppointmentNotifications();
		
		Map<Long, List<DeliveryNotification>> appointmentsByShipmentInstId = CollectionUtils.emptyIfNull(appointmentNotifications)
				.stream().collect(Collectors.groupingBy(DeliveryNotification::getShipmentInstId));
		
		
		for (Long shipmentInstId: shpInstIds) {
			List<DeliveryNotification> records = appointmentsByShipmentInstId.get(shipmentInstId);
			
			final Predicate<DeliveryNotification> isAppointment = FBDSCopyBillUtil::isAppointment;
			final Predicate<DeliveryNotification> isNotification = isAppointment.negate();
			final Predicate<DeliveryNotification> isActive = notification -> notification.getStatusCd() != AppointmentNotificationStatCd.INACTIVE
					&& notification.getStatusCd() != AppointmentNotificationStatCd.CANCELLED
					&& StringUtils.isNotBlank(notification.getScheduledDeliveryDate());
			final Function<DeliveryNotification, Date> notificationUpdatedTimestamp = notification -> BasicTransformer
					.toDate(notification.getAuditInfo().getUpdatedTimestamp());

			DeliveryNotification recentAppointment = CollectionUtils.emptyIfNull(records)
					.stream()
					.filter(Objects::nonNull)
					.filter(isNotification.or(isAppointment.and(isActive)))
					.max(Comparator.comparing(notificationUpdatedTimestamp))
					.orElse(null);
			
			recentAppointmentsByShipmentInstId.put(shipmentInstId, recentAppointment);
		}
		
		return recentAppointmentsByShipmentInstId;
		
	}

	private void processAdvanceCarrier(FBDSDocument documentDetails,
			Map<AdvanceBeyondTypeCd, List<AdvanceBeyondCarrier>> shipmentCarrierMap, TransactionContext txnContext) {

		final AdvanceBeyondCarrier carrier = MapUtils.isNotEmpty(shipmentCarrierMap)
				&& Objects.nonNull(shipmentCarrierMap.get(AdvanceBeyondTypeCd.ADV_CARR))
						? shipmentCarrierMap.get(AdvanceBeyondTypeCd.ADV_CARR).get(0)
						: null;

		if (Objects.nonNull(carrier)) {
			documentDetails.setAdvanceCarrier(carrier);
		}
	}

    private void addValidationWarning(List<DataValidationError> validationWarnings,
                                      Mode mode,
                                      Shipment shipment,
                                      ErrorMessageIF error,
                                      Object... contextValues) {
        DataValidationError warning =
            buildValidationWarning(mode, shipment, error, contextValues);
        validationWarnings.add(warning);
    }

    private DataValidationError buildValidationWarning(Mode mode,
                                                       Shipment shipment,
                                                       ErrorMessageIF error,
                                                       Object... contextValues) {
        String[] contextValuesAsStrings =
            ArrayUtils.toStringArray(contextValues);

        String fieldName =
            mode == Mode.PRO_NBR ? "proNbrs" : "shipmentInstIds";

        String fieldValue =
            mode == Mode.PRO_NBR
                ? FBDSCopyBillUtil.formatFBDSPro(shipment.getProNbr())
                : shipment.getShipmentInstId().toString();

        DataValidationError warning = new DataValidationError();
        warning.setErrorCd(error.errorCode());
        warning.setMessage(error.message(contextValuesAsStrings));
        warning.setFieldName(fieldName);
        warning.setFieldValue(fieldValue);
        return warning;
    }

    private MoreInfo toMoreInfo(DataValidationError validationWarning) {
        String message = validationWarning.getMessage();
        if (StringUtils.isNotBlank(validationWarning.getFieldValue()))
            message += ": " + validationWarning.getFieldValue();

        return new MoreInfoBuilder()
            .errorCode(validationWarning.getErrorCd())
            .message(message)
            .location(validationWarning.getFieldName())
            .build();
    }

}
