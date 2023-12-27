package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.freightflow.v2.CalculateTransitTimeRqst;
import com.xpo.ltl.api.freightflow.v2.PostalTransitTime;
import com.xpo.ltl.api.freightflow.v2.TransitTimeBasic;
import com.xpo.ltl.api.location.v2.DetermineOperationalServiceDateResp;
import com.xpo.ltl.api.location.v2.GetSicForPostalCodesResp;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ServiceErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCust;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.LateTenderCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MatchedPartyTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ServiceTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentAcquiredTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.CalculateServiceStandardForShipmentsResp;
import com.xpo.ltl.api.shipment.v2.CalculateServiceStandardForShipmentsRqst;
import com.xpo.ltl.api.shipment.v2.DataValidationError;
import com.xpo.ltl.api.shipment.v2.LateTenderCd;
import com.xpo.ltl.api.shipment.v2.MatchedPartyTypeCd;
import com.xpo.ltl.api.shipment.v2.ProAndDestination;
import com.xpo.ltl.api.shipment.v2.ServiceTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentAcquiredTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentServiceStandard;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.CalculateTransitTimeResult;
import com.xpo.ltl.shipment.service.client.CalculateTransitTimeTask;
import com.xpo.ltl.shipment.service.client.CalculateTransitTimesResult;
import com.xpo.ltl.shipment.service.client.CalculateTransitTimesTask;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShmShipmentEagerLoadPlan;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.helper.ShipmentSkeletonHelper;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.TimestampUtil;

@RequestScoped
public class CalculateServiceStandardForShipmentsImpl {

    private static final int MAX_PRO_AND_DESTINATION_RQ_ENTRIES = 40;

    private static final int MAX_FFO_SDK_TRANSIT_TIME_RQ_ENTRIES = 20;

    private static final Log LOG = LogFactory.getLog(CalculateServiceStandardForShipmentsImpl.class);

    private static final String PGM_ID = "CLSVCSTD";

    @PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
    private EntityManager db2EntityManager;

    @Inject
    private ShmShipmentSubDAO shipmentDAO;

    @Inject
    private ExternalRestClient restClient;

    public CalculateServiceStandardForShipmentsResp calculateServiceStandardForShipments(
        CalculateServiceStandardForShipmentsRqst calculateServiceStandardForShipmentsRqst,
			TransactionContext txnContext, EntityManager entityManager) throws ServiceException {

		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
        checkNotNull(calculateServiceStandardForShipmentsRqst, "The CalculateServiceStandardForShipmentsRqst is required.");
        requestValidation(calculateServiceStandardForShipmentsRqst, txnContext);

        List<DataValidationError> warnings = new ArrayList<DataValidationError>();

        Map<String, ProAndDestination> formattedProNbrProAndDestMap = getFormattedProNbrProAndDestinationMap(
            calculateServiceStandardForShipmentsRqst.getProAndDestinations(), warnings,
            txnContext);

        List<Map<String, ShmShipment>> proNbrShmShipmentMapList = buildProNbrShipmentMapFromDBList(formattedProNbrProAndDestMap, warnings,
            entityManager);

        List<ShipmentServiceStandard> shmServiceStdList = new ArrayList<>();
        // need to split the shipments with the ones with PostalCd and the ones without. This is because there is a restriction
        // in FFO that we cannot associate the request with the response
        // if we have 2 different request one using Postal Cd and the other one SIC and both are for the same destination SIC.
        for (Map<String, ShmShipment> proNbrShmShipmentMap : proNbrShmShipmentMapList) {

            if (proNbrShmShipmentMap.isEmpty()) {
                continue;
            }

            validateShipmentForCalculation(formattedProNbrProAndDestMap, proNbrShmShipmentMap, warnings, txnContext);

            Map<ShmShipment, TransitTimeBasic> shipmentTransitTimeBasicRqstMap = buildShipmentTransitTimeBasicRqstMap(proNbrShmShipmentMap,
                formattedProNbrProAndDestMap, warnings);

            Map<ShmShipment, PostalTransitTime> calculateTransitTimeMap = getShmShipmentPostalTransitTimeMap(shipmentTransitTimeBasicRqstMap,
                warnings, txnContext);

            for (ShmShipment shmShipment : proNbrShmShipmentMap.values()) {
                boolean doUpd = calculateTransitTimeMap.keySet().stream().anyMatch(shm -> shm.getProNbrTxt().equals(shmShipment.getProNbrTxt()));
                if (!doUpd) {
                    LOG.info("There is nothing to update for the shipment.");
                    continue;
                }

                populateServiceStandardInfo(shmShipment, calculateTransitTimeMap, txnContext, restClient);

                DtoTransformer.setLstUpdateAuditInfo(shmShipment, AuditInfoHelper.getAuditInfoWithPgmId(PGM_ID, txnContext));
                if (StringUtils.isBlank(shmShipment.getLstUpdtUid())) 
                    shmShipment.setLstUpdtUid(AuditInfoHelper.getDefaultUserId());

                shipmentDAO.save(shmShipment, entityManager);
                shipmentDAO.updateDb2ShmShipmentForUpdServiceStdInfo(shmShipment, db2EntityManager);

                buildAndAddShipmentServiceStd(shmServiceStdList, shmShipment);
            }
        }

        CalculateServiceStandardForShipmentsResp resp = new CalculateServiceStandardForShipmentsResp();
        resp.setShipmentServiceStandards(shmServiceStdList);
        resp.setWarnings(warnings);
        return resp;
	}

    private List<Map<String, ShmShipment>> buildProNbrShipmentMapFromDBList(Map<String, ProAndDestination> formattedProNbrProAndDestMap,
        List<DataValidationError> warnings, EntityManager entityManager) {

        List<String> formattedProNbrList = Lists.newArrayList(formattedProNbrProAndDestMap.keySet());

        ShmShipmentEagerLoadPlan shmShipmentEagerPlan = new ShmShipmentEagerLoadPlan();
        shmShipmentEagerPlan.setShmAsEntdCusts(true);
        shmShipmentEagerPlan.setShmOpsShipment(true);
        List<ShmShipment> existingShipmentList = shipmentDAO.listShipmentsByProNbrs(formattedProNbrList, shmShipmentEagerPlan, entityManager);

        Map<String, ShmShipment> proNbrShipmentWithPostalCdMap = new HashMap<String, ShmShipment>();
        Map<String, ShmShipment> proNbrShipmentNoPostalCdMap = new HashMap<String, ShmShipment>();
        List<String> proDbList = Lists.newArrayList();

        CollectionUtils.emptyIfNull(existingShipmentList).stream().forEach(shp ->
        {
            proDbList.add(shp.getProNbrTxt());
            if (shp.getPkupDt() == null 
                    || TimestampUtil.isLowTimestamp(
                        BasicTransformer.toTimestamp(BasicTransformer.toXMLGregorianCalendar(shp.getPkupDt())))) {
                String msg = String
                        .format("Pickup date is not known");
                addWarningToDataValidationErrorList("", "ProNbr", shp.getProNbrTxt(), msg, warnings);
            } else {

                ProAndDestination proAndDestination = formattedProNbrProAndDestMap.get(shp.getProNbrTxt());
                if (StringUtils.isNotBlank(proAndDestination.getDestinationPostalCd())) {
                    proNbrShipmentWithPostalCdMap.put(shp.getProNbrTxt(), shp);
                } else {
                    proNbrShipmentNoPostalCdMap.put(shp.getProNbrTxt(), shp);
                }
            }
        });

        // warning for those that couldn't be found in DB.
        formattedProNbrList
            .stream()
            .filter(proNbr -> !proDbList.contains(proNbr))
            .forEach(nonFoundProNbr -> {
            addWarningToDataValidationErrorList(NotFoundErrorMessage.SHIPMENT_NF.errorCode(), "ProNbr", nonFoundProNbr,
                NotFoundErrorMessage.SHIPMENT_NF.message("ProNbr", nonFoundProNbr), warnings);
        });

        List<Map<String, ShmShipment>> proNbrShmShipmentMapList = new ArrayList<>();
        proNbrShmShipmentMapList.add(proNbrShipmentWithPostalCdMap);
        proNbrShmShipmentMapList.add(proNbrShipmentNoPostalCdMap);

        return proNbrShmShipmentMapList;
    }

    private void requestValidation(CalculateServiceStandardForShipmentsRqst calculateServiceStandardForShipmentsRqst, TransactionContext txnContext)
            throws ValidationException {
        if (CollectionUtils.isEmpty(calculateServiceStandardForShipmentsRqst.getProAndDestinations())) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.REQUEST_REQUIRED, txnContext)
                .moreInfo("calculateServiceStandardForShipments", "ProAndDestinations list cannot be empty.")
                .build();
        }

        if (calculateServiceStandardForShipmentsRqst.getProAndDestinations().size() > MAX_PRO_AND_DESTINATION_RQ_ENTRIES) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.INPUT_LIST_LENGTH_EXCEEDED, txnContext).contextValues("40").build();
        }
    }

    private void buildAndAddShipmentServiceStd(List<ShipmentServiceStandard> shmServiceStdList, ShmShipment shmShipment) {
        ShipmentServiceStandard shmServiceStd = new ShipmentServiceStandard();
        shmServiceStd.setProNbr(shmShipment.getProNbrTxt());
        shmServiceStd.setEstimatedDeliveryDate(BasicTransformer.toDateString(shmShipment.getEstimatedDlvrDt()));
        shmServiceStd.setEstimatedTransitDays(BasicTransformer.toBigInteger(shmShipment.getEstTrnstDays()));
        shmServiceStd.setServiceStartDate(BasicTransformer.toDateString(shmShipment.getSvcStrtDt()));
        shmServiceStd.setStandardTransitDays(BasicTransformer.toBigInteger(shmShipment.getStdTrnstDays()));
        shmServiceStdList.add(shmServiceStd);
    }

    public static void populateServiceStandardInfo(ShmShipment shmShipment,
        Map<ShmShipment, PostalTransitTime> shmShipmentTransitTimeMap, TransactionContext txnContext,
        ExternalRestClient restClient) throws ServiceException {

        ServiceTypeCd serviceTypeCd = shmShipment.getSvcTypCd() != null ? ServiceTypeCdTransformer.toEnum(shmShipment.getSvcTypCd()) :
            ServiceTypeCd.NORMAL;

        String serviceStartDate = null;
        BigInteger standardTrnstDays = null;
        BigInteger estimatedTrnstDays = null;
        String estimatedDlvryDate = null;
        String originSic = shmShipment.getOrigTrmnlSicCd();
        String destSic = shmShipment.getDestTrmnlSicCd();
        String pickupDate = BasicTransformer.toDateString(shmShipment.getPkupDt());
        PostalTransitTime postalTransitTime = null;

        com.xpo.ltl.api.location.v2.ServiceTypeCd locServiceTypeCode = com.xpo.ltl.api.location.v2.ServiceTypeCd
                .fromValue(serviceTypeCd.toString());

        if (shmShipmentTransitTimeMap.containsKey(shmShipment)) {
            postalTransitTime = shmShipmentTransitTimeMap.get(shmShipment);
        }

        if (LateTenderCdTransformer.toCode(LateTenderCd.LATE_TENDER).equals(shmShipment.getLateTenderCd()))
            try {
                pickupDate = ShipmentSkeletonHelper.incrementPickupDate(pickupDate);
            } catch (ParseException e) {
                LOG.error(ExceptionUtils.getMessage(e));
            }
        serviceStartDate = populateServiceStartDate(shmShipment, txnContext, originSic, destSic, pickupDate, locServiceTypeCode,
            postalTransitTime, restClient);

        if (null != postalTransitTime && null != postalTransitTime.getTransitDays()) {
            standardTrnstDays = postalTransitTime.getTransitDays();
        }

        if (StringUtils.isNotBlank(BasicTransformer.toDateString(shmShipment.getEstimatedDlvrDt()))
                && !TimestampUtil.isLowTimestamp(BasicTransformer.toTimestamp(BasicTransformer.toXMLGregorianCalendar((shmShipment.getEstimatedDlvrDt()))))) {
            estimatedDlvryDate = BasicTransformer.toDateString(shmShipment.getEstimatedDlvrDt());
        } else {
            if (serviceTypeCd == ServiceTypeCd.RAPID_REMOTE_SERVICE) {
                if (null != postalTransitTime && true == postalTransitTime.getRrsInd()
                        && null != postalTransitTime.getRrsEstimatedDeliveryDate() && null != postalTransitTime.getRrsTransitDays()) {
                    estimatedDlvryDate = postalTransitTime.getRrsEstimatedDeliveryDate();
                    standardTrnstDays = postalTransitTime.getRrsTransitDays();
                    estimatedTrnstDays = postalTransitTime.getRrsTransitDays();
                }
            } else {
                if (null != postalTransitTime && null != postalTransitTime.getEstdDlvrDate()) {
                    estimatedDlvryDate = postalTransitTime.getEstdDlvrDate();
                }
            }
            if (null == estimatedDlvryDate) {
                // determine estimated delivery date
                DetermineOperationalServiceDateResp estdDlvryDateResp = restClient
                        .getOperationalServiceDates(originSic, destSic, serviceStartDate, locServiceTypeCode,
                            (null != standardTrnstDays) ? standardTrnstDays.intValue() : 0, true, txnContext);
                estimatedDlvryDate = estdDlvryDateResp.getServiceDate();
            }
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            estimatedDlvryDate = formatter.format(BasicTransformer.toDate(estimatedDlvryDate));
        }

        if (shmShipment.getEstTrnstDays().compareTo(BigDecimal.ZERO) == 0) {
            estimatedTrnstDays = standardTrnstDays;
        } else {
            estimatedTrnstDays = BasicTransformer.toBigInteger(shmShipment.getEstTrnstDays());
        }

        shmShipment.setStdTrnstDays(BasicTransformer.toBigDecimal(null != standardTrnstDays ? standardTrnstDays : BigInteger.ZERO));
        shmShipment.setEstTrnstDays(BasicTransformer.toBigDecimal(null != estimatedTrnstDays ? estimatedTrnstDays : BigInteger.ZERO));
        shmShipment
        .setEstimatedDlvrDt(
            Objects.nonNull(estimatedDlvryDate) ? BasicTransformer.toDate(estimatedDlvryDate) : TimestampUtil.getLowTimestamp());
        shmShipment.setSvcStrtDt(Objects.nonNull(serviceStartDate) ? BasicTransformer.toDate(serviceStartDate) : TimestampUtil.getLowTimestamp());

    }

    private static String populateServiceStartDate(ShmShipment shmShipment, TransactionContext txnContext, String originSic, String destSic,
        String pickupDate, com.xpo.ltl.api.location.v2.ServiceTypeCd serviceTypeCd, PostalTransitTime calculateTransitTimeResp,
        ExternalRestClient restClient) throws ServiceException {
        String serviceStartDate = null;
        if (StringUtils.isNotBlank(BasicTransformer.toDateString(shmShipment.getSvcStrtDt()))
                && !TimestampUtil.isLowTimestamp(BasicTransformer.toTimestamp(BasicTransformer.toXMLGregorianCalendar((shmShipment.getSvcStrtDt()))))) {
            serviceStartDate = BasicTransformer.toDateString(shmShipment.getSvcStrtDt());
        } else {
            if (null != calculateTransitTimeResp && null != calculateTransitTimeResp.getEarliestPkupDate()) {
                serviceStartDate = calculateTransitTimeResp.getEarliestPkupDate();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                serviceStartDate = formatter.format(BasicTransformer.toDate(serviceStartDate));
            }
            if (null == serviceStartDate) {
                DetermineOperationalServiceDateResp opSrvcDateResp = restClient
                    .getOperationalServiceDates(originSic, destSic, pickupDate, serviceTypeCd, 0, true, txnContext);
                serviceStartDate = opSrvcDateResp.getServiceDate();
            }
        }

        return serviceStartDate;
    }


    /**
     * Validate that the dest zip is serviced by the same dest sic stored in shm_shipment already. <br/>
     * if not, we will throw a warning for that particular pro, remove it from existentShmShipmentMap and keep processing
     * the
     * rest.
     */
    protected void validateShipmentForCalculation(Map<String, ProAndDestination> formattedProProAndDestMap,
        Map<String, ShmShipment> proNbrShmShipmentMap,
        List<DataValidationError> warnings, TransactionContext txnContext) throws ServiceException {

        Map<String, String> postalCodeSicMap = new HashMap<String, String>();

        String[] postalCds = formattedProProAndDestMap
            .values()
            .stream()
            .filter(pad -> StringUtils.isNotBlank(pad.getDestinationPostalCd()))
            .map(proAndDest -> proAndDest.getDestinationPostalCd())
            .toArray(String[]::new);

        if(postalCds.length == 0) {
            return;
        }

        GetSicForPostalCodesResp sicForPostalCodes = restClient.getSicForPostalCodes(postalCds, txnContext);
        CollectionUtils
            .emptyIfNull(sicForPostalCodes.getPostalSicAssignments())
            .forEach(pSicAssgmn -> postalCodeSicMap.putIfAbsent(pSicAssgmn.getPostalCd(), pSicAssgmn.getSicCd()));

        // validate that the destPC is the same to the one stored in DB, otherwise add a warning.
        //validate that the pickup date is available, otherwise add a warning.
        formattedProProAndDestMap
            .entrySet()
            .stream()
            .filter(entry -> StringUtils.isNotBlank(entry.getValue().getDestinationPostalCd()))
            .forEach(entry -> {
                String sicCode = postalCodeSicMap.get(entry.getValue().getDestinationPostalCd());
                String formattedProNbr = entry.getKey();
                ShmShipment shmFromDB = proNbrShmShipmentMap.get(formattedProNbr);

                if (shmFromDB == null)
                    return;

                String destSicCdFromDB = shmFromDB.getDestTrmnlSicCd();
                if (!destSicCdFromDB.equals(sicCode)) {
                    proNbrShmShipmentMap.remove(formattedProNbr);
                    String msg = String
                        .format("Destination postal code %s is for SIC: %s which is different than the stored destination SIC: %s",
                            entry.getValue().getDestinationPostalCd(), sicCode, destSicCdFromDB);
                    addWarningToDataValidationErrorList("", "ProNbr", entry.getValue().getProNbr(), msg, warnings);
                }
            });

    }

    private Map<String, ProAndDestination> getFormattedProNbrProAndDestinationMap(List<ProAndDestination> proAndDestList,
        List<DataValidationError> warnings, TransactionContext txnContext) {

        Map<String, ProAndDestination> formattedProNbrList = new HashMap<String, ProAndDestination>();
        for (ProAndDestination proAndDest : proAndDestList) {
            String formattedProNbr;
            try {
                formattedProNbr = ShipmentSkeletonHelper.getFormattedProNbr(proAndDest.getProNbr(), txnContext);
            } catch (ServiceException e) {
                addWarningToDataValidationErrorList("", "ProNbr", proAndDest.getProNbr(), "Pro cannot be formatted.", warnings);
                continue;
            }
            formattedProNbrList.put(formattedProNbr, proAndDest);
        }
        return formattedProNbrList;
    }

    public Map<ShmShipment, PostalTransitTime> getShmShipmentPostalTransitTimeMap(Map<ShmShipment, TransitTimeBasic> shmShipmentTransitTimeBasicMap,
        List<DataValidationError> warnings, TransactionContext txnContext)
            throws ServiceException {

        if (shmShipmentTransitTimeBasicMap.isEmpty()) {
            return new HashMap<ShmShipment, PostalTransitTime>();
        }

        List<CalculateTransitTimesTask> taskList = buildTaskListPartinioned(shmShipmentTransitTimeBasicMap, txnContext);

        Collection<CalculateTransitTimesResult> calculateTransitTimeResp = restClient.getCalculateTransitTimes(taskList, txnContext);

        final Map<ShmShipment, PostalTransitTime> shmShipmentPostalTransitTimeMap = new HashMap<ShmShipment, PostalTransitTime>();

        final Map<Triple<String, String, String>, PostalTransitTime> responseMap = new HashMap<Triple<String, String, String>, PostalTransitTime>();
        // load responseMap.
        for (CalculateTransitTimesResult resp : CollectionUtils.emptyIfNull(calculateTransitTimeResp)) {
            for (PostalTransitTime ptt : CollectionUtils.emptyIfNull(resp.getTransitTimeResp())) {
                PostalTransitTime postalTransitTime = ptt;
                String origin = postalTransitTime.getOrigSicCd();
                String dest = postalTransitTime.getDestSicCd();
                String destPc = postalTransitTime.getDestPostalCd();
                String strRqstdPkupDate;
                try {
                    strRqstdPkupDate = convertStrDateToStrDate(postalTransitTime.getRequestedPkupDate(), "MM/dd/yyyy", "yyyy-MM-dd");
                } catch (ParseException e) {
                    throw ExceptionBuilder
                    .exception(ServiceErrorMessage.UNHANDLED_SERVICE_EXCEPTION, txnContext)
                    .moreInfo("Parsing requested pkupDt", e.getMessage())
                    .build();
                }
                responseMap.put(Triple.of(origin, dest, strRqstdPkupDate), postalTransitTime);
                responseMap.put(Triple.of(origin, destPc, strRqstdPkupDate), postalTransitTime);
            }
        }

        // find the key and add it to skeletonPostalTransitTimeMap
        for (Entry<ShmShipment, TransitTimeBasic> entry : shmShipmentTransitTimeBasicMap.entrySet()) {
            String requestedPkupDateStr = null;
            try {
                requestedPkupDateStr = convertXmlGregorianCalToStrDate(entry.getValue().getRequestedPkupDate(), "yyyy-MM-dd");
            } catch (ParseException e) {
                addWarningToDataValidationErrorList("","ProNbr", entry.getKey().getProNbrTxt(),
                    String
                        .format("Orig: %s, Dest: %s, RqstPkUpDate: %s couldn't be found in Freight Flow service.",
                            entry.getValue().getOrigPostalCd(), entry.getValue().getDestPostalCd(), entry.getValue().getRequestedPkupDate()),
                    warnings);
                continue;
            }

            Triple<String, String, String> triple = Triple
                .of(entry.getValue().getOrigPostalCd(), entry.getValue().getDestPostalCd(), requestedPkupDateStr);
            PostalTransitTime postalTransitTime = responseMap.get(triple);
            if (postalTransitTime != null) {
                shmShipmentPostalTransitTimeMap.put(entry.getKey(), postalTransitTime);
            } else {
                addWarningToDataValidationErrorList("", "ProNbr", entry.getKey().getProNbrTxt(),
                    String
                        .format("Orig: %s, Dest: %s, RqstPkUpDate: %s couldn't be found in Freight Flow service.", triple.getLeft(),
                            triple.getMiddle(),
                            triple.getRight()),
                    warnings);
            }
        }

        return shmShipmentPostalTransitTimeMap;
    }

    /**
     * due to a limitation in FFO endpoint we have to split the request by
     * <code>MAX_ENTRIES_FFO_SDK_TRANSIT_TIME_RQST</code> entries each.
     */
    private List<CalculateTransitTimesTask> buildTaskListPartinioned(Map<ShmShipment, TransitTimeBasic> shmShipmentTransitTimeBasicMap,
        TransactionContext txnContext) {

        List<TransitTimeBasic> allTransitTimeBasicList = Lists.newArrayList(shmShipmentTransitTimeBasicMap.values());
        List<List<TransitTimeBasic>> partitionTransitTimeBasicList = ListUtils.partition(allTransitTimeBasicList, MAX_FFO_SDK_TRANSIT_TIME_RQ_ENTRIES);
        List<CalculateTransitTimesTask> taskList = new ArrayList<CalculateTransitTimesTask>();

        for (List<TransitTimeBasic> eachPartitionTransitTimeBasicList : partitionTransitTimeBasicList) {
            CalculateTransitTimeRqst calculateTransitTimeRqst = new CalculateTransitTimeRqst();
            calculateTransitTimeRqst.setTransitTime(eachPartitionTransitTimeBasicList);
            taskList.add(new CalculateTransitTimesTask(calculateTransitTimeRqst, restClient, txnContext));
        }
        return taskList;
    }

    private Map<ShmShipment, TransitTimeBasic> buildShipmentTransitTimeBasicRqstMap(Map<String, ShmShipment> proNbrShmShipmentMap,
        Map<String, ProAndDestination> formattedProNbrProAndDestMap, List<DataValidationError> warnings) throws ServiceException {

        Map<ShmShipment, TransitTimeBasic> shipmentTransitTimeBasicRqstMap = new HashMap<>();

        for (ShmShipment shmShipment : CollectionUtils.emptyIfNull(proNbrShmShipmentMap.values())) {
            TransitTimeBasic transitTimeBasic = buildTransitTimeBasic(shmShipment, formattedProNbrProAndDestMap);
            shipmentTransitTimeBasicRqstMap.put(shmShipment, transitTimeBasic);
        }

        return shipmentTransitTimeBasicRqstMap;
    }

    /**
     * if shipper asEntd is available and input dest postal is available, use the postal code for transit time calc, Only
     * use the SICs if those data are not there. <br/>
     * the origin sic and request pkup date are taken from the shipment.
     */
    public TransitTimeBasic buildTransitTimeBasic(ShmShipment shmShipment,
        Map<String, ProAndDestination> formattedProNbrProAndDestMap) {

        Optional<ShmAsEntdCust> shipperParty = CollectionUtils
            .emptyIfNull(shmShipment.getShmAsEntdCusts())
            .stream()
            .filter(shmAsEntd -> MatchedPartyTypeCd.SHPR == MatchedPartyTypeCdTransformer.toEnum(shmAsEntd.getTypCd()))
            .findAny();

        ProAndDestination proAndDestination = formattedProNbrProAndDestMap.get(shmShipment.getProNbrTxt());

        String destination;
        if (shipperParty.isPresent() && Objects.nonNull(proAndDestination)
                && StringUtils.isNotBlank(proAndDestination.getDestinationPostalCd())) {
            destination = proAndDestination.getDestinationPostalCd();
        } else {
            destination = shmShipment.getDestTrmnlSicCd();
        }

        TransitTimeBasic transitTimeBasic = new TransitTimeBasic();
        transitTimeBasic.setOrigPostalCd(shmShipment.getOrigTrmnlSicCd());
        transitTimeBasic.setDestPostalCd(destination);
        transitTimeBasic.setRequestedPkupDate(getPickupDate(shmShipment));

        return transitTimeBasic;

    }

    public static XMLGregorianCalendar getPickupDate(ShmShipment shmShipment) {

        Timestamp lastMvmtDateTime = shmShipment.getLstMvmtTmst();

        Calendar calendar = BasicTransformer.toCalendar(lastMvmtDateTime);
        if (Objects.isNull(lastMvmtDateTime) || TimestampUtil.isLowTimestamp(lastMvmtDateTime)) {
            lastMvmtDateTime = Timestamp.from(Instant.now());
            calendar = BasicTransformer.toCalendar(lastMvmtDateTime);
        }

        if (isPickup(shmShipment)) {
            lastMvmtDateTime = TimestampUtil.isLowTimestamp(lastMvmtDateTime) ?
                Timestamp.from(Instant.now()) :
                lastMvmtDateTime;
            calendar = BasicTransformer.toCalendar(lastMvmtDateTime);
        }

        LateTenderCd lateTenderCd = isPickup(shmShipment) ? LateTenderCdTransformer.toEnum(shmShipment.getLateTenderCd()) :
            LateTenderCd.NOT_A_LATE_TENDER;
        if (LateTenderCd.LATE_TENDER == lateTenderCd) {
            calendar = BasicTransformer.toCalendar(lastMvmtDateTime);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return BasicTransformer.toXMLGregorianCalendar(calendar);
    }

    public static boolean isPickup(ShmShipment shmShipment) {
        return ShipmentAcquiredTypeCd.REGULAR_PKUP == ShipmentAcquiredTypeCdTransformer.toEnum(shmShipment.getShpmtAcqrTypCd());
    }

    private static String convertStrDateToStrDate(String strDate, String inputFormat, String outputFormat) throws ParseException {
        Date date1 = new SimpleDateFormat(inputFormat).parse(strDate);
        DateFormat dateFormat = new SimpleDateFormat(outputFormat);
        return dateFormat.format(date1);
    }

    private static String convertXmlGregorianCalToStrDate(XMLGregorianCalendar xmlGregCal, String outputFormat) throws ParseException {
        Date date = BasicTransformer.toDate(xmlGregCal);
        DateFormat dateFormat = new SimpleDateFormat(outputFormat);
        return dateFormat.format(date);
    }

    private void addWarningToDataValidationErrorList(String code, String name, String value, String msg, List<DataValidationError> warnings) {
        DataValidationError warning = new DataValidationError();
        warning.setErrorCd(code);
        warning.setFieldName(name);
        warning.setFieldValue(value);
        warning.setMessage(msg);
        warnings.add(warning);
    }
}
