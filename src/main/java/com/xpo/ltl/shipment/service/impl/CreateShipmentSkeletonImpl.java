package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StopWatch;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.humanresource.v1.EmployeeRole;
import com.xpo.ltl.api.location.v2.GetSicForPostalCodesResp;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ProFrtBillIndex;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCust;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCustPK;
import com.xpo.ltl.api.shipment.service.entity.ShmEventLog;
import com.xpo.ltl.api.shipment.service.entity.ShmEventLogPK;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmt;
import com.xpo.ltl.api.shipment.service.entity.ShmHazMat;
import com.xpo.ltl.api.shipment.service.entity.ShmHazMatPK;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementPK;
import com.xpo.ltl.api.shipment.service.entity.ShmRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmRemarkPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.BillClassCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.BillStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.DeliveryQualifierCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.EventLogSubTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.EventLogTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.FoodPoisonCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.HandlingUnitTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.HazmatSourceCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.LateTenderCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MatchedPartyTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ServiceTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentAcquiredTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentCreditStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentMovementTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentRemarkTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentSourceCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentVolumeMethodCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentVolumeTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipperLoadedTrailerCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.WarrantyStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.AsMatchedParty;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.BaseLog;
import com.xpo.ltl.api.shipment.v2.BillClassCd;
import com.xpo.ltl.api.shipment.v2.BillStatusCd;
import com.xpo.ltl.api.shipment.v2.BulkCreateShipmentSkeletonRqst;
import com.xpo.ltl.api.shipment.v2.CreateShipmentSkeletonRqst;
import com.xpo.ltl.api.shipment.v2.DeliveryQualifierCd;
import com.xpo.ltl.api.shipment.v2.EventLogSubTypeCd;
import com.xpo.ltl.api.shipment.v2.EventLogTypeCd;
import com.xpo.ltl.api.shipment.v2.GetProStatusResp;
import com.xpo.ltl.api.shipment.v2.HandlingUnit;
import com.xpo.ltl.api.shipment.v2.HandlingUnitMovement;
import com.xpo.ltl.api.shipment.v2.HandlingUnitMovementTypeCd;
import com.xpo.ltl.api.shipment.v2.HandlingUnitTypeCd;
import com.xpo.ltl.api.shipment.v2.HazMat;
import com.xpo.ltl.api.shipment.v2.HazmatSourceCd;
import com.xpo.ltl.api.shipment.v2.LateTenderCd;
import com.xpo.ltl.api.shipment.v2.LnhDimension;
import com.xpo.ltl.api.shipment.v2.MatchedPartyTypeCd;
import com.xpo.ltl.api.shipment.v2.MovementStatusCd;
import com.xpo.ltl.api.shipment.v2.ProStatusCd;
import com.xpo.ltl.api.shipment.v2.ServiceTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentAcquiredTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentId;
import com.xpo.ltl.api.shipment.v2.ShipmentMovementTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentRemarkTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentSkeleton;
import com.xpo.ltl.api.shipment.v2.ShipmentSkeletonResponse;
import com.xpo.ltl.api.shipment.v2.ShipmentSourceCd;
import com.xpo.ltl.api.shipment.v2.ShipmentWithDimension;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentLinehaulDimensionsRqst;
import com.xpo.ltl.api.shipment.v2.WarrantyStatusCd;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.CalculateTransitTimeTask;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ProFrtBillIndexSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentAsEnteredCustomerDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentRemarkSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmEventLogSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitMvmtSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHazMatSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmMovementSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.enums.BaseLogTypeEnum;
import com.xpo.ltl.shipment.service.helper.ShipmentSkeletonHelper;
import com.xpo.ltl.shipment.service.impl.interim.MaintainBolDocImpl;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.HandlingUnitHelper;
import com.xpo.ltl.shipment.service.util.HandlingUnitMovementHelper;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import com.xpo.ltl.shipment.service.util.TimestampUtil;


@RequestScoped
public class CreateShipmentSkeletonImpl {

	private static final String PICKUP_CAPTURE_CD = "P";
	private static final String PICKUP_DIMENSION_TYPE_CD = "PICKUP";
    private static final String DOCK_CAPTURE_CD = "D";
    private static final String DOCK_DIMENSION_TYPE_CD = "DOCK";
    private static final String DEFAULT_IND_VALUE = "N";
	private static final Set<String> VALID_BULK_QUANTITY_CODES = Sets.newHashSet("N", "B", "L");
    private static final String REMARK_AFTER_HOUR_TEXT = " * AFTER HOURS PICKUP *";
    private static final Log LOG = LogFactory.getLog(CreateShipmentSkeletonImpl.class);
    private static final String SUPRV_ROLE_ID = "165";
    private static final String NORMAL_MOVEMENT_CD = "NORMAL";
    private static final String ASTRAY_MOVEMENT_CD = "ASTRAY";
    private static final String COMMON_PGM_ID = "SHMCRTE";
    private static final List<String> PRO_FRT_IN_USE = Lists.newArrayList("3","4","7");
    private static final List<ProStatusCd> PRO_FRT_IN_USE_ENUM_LIST = Lists
            .newArrayList(ProStatusCd.RECEIVED_ON_FILE_AND_BILLED);
    private static final List<ProStatusCd> PRO_FRT_UNAVAILABLE_ENUM_LIST = Lists
        .newArrayList(ProStatusCd.RECEIVED_PURGED_MAYBE_ON_FBA, ProStatusCd.VOIDED);
    private static final String PRO_FRT_IN_USE_SUSPENSE = "9";
    private static final String PRO_FRT_IN_USE_UNBILLED = "2";
    private static final Map<String, String> MAP_CAPTURED_BY_CD = new HashMap<String, String>() {

        private static final long serialVersionUID = 1L;
        {
            put("ACCURACY", "A");
            put("DOCK", "D");
            put("PICKUP", "P");
            put("PICKUP_DIMENSIONER", "U");
        }
    };

    private static final EnumSet<BillStatusCd> BILLED_RATED = EnumSet.of(BillStatusCd.BILLED, BillStatusCd.RATED);

	@PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
	private EntityManager db2EntityManager;

	@Inject
	private ShmShipmentSubDAO shipmentDAO;

	@Inject
	private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

	@Inject
	private ShmEventLogSubDAO shmEventLogSubDAO;

	@Inject
	private ShmHazMatSubDAO shmHazMatSubDAO;

	@Inject
	private ShmHandlingUnitMvmtSubDAO shmHandlingUnitMvmtSubDAO;

	@Inject
	private ExternalRestClient restClient;

    @Inject
    private ShipmentRemarkSubDAO shipmentRemarkSubDAO;

    @Inject
    private UpdateShipmentLinehaulDimensionsImpl updateShipmentLinehaulDimensionsImpl;

    @Inject
    private ShipmentAsEnteredCustomerDAO shipmentAsEnteredCustomerDAO;

    @Inject
    private ProFrtBillIndexSubDAO proNbrFrtBillIndexDAO;

    @Inject
    private ShmMovementSubDAO shmMovementSubDAO;

    @Inject
    private MaintainBolDocImpl maintainBolDocImpl;
    
    @Inject
    private ShipmentSkeletonHelper shipmentSkeletonHelper;

    @Inject
    private GetProStatusCdImpl getProStatusCdImpl;

    public ShipmentSkeletonResponse createShipmentSkeleton(CreateShipmentSkeletonRqst createShipmentSkeletonRqst,
			TransactionContext txnContext, EntityManager entityManager) throws ServiceException {

		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
		checkNotNull(createShipmentSkeletonRqst, "The request is required.");
		checkNotNull(createShipmentSkeletonRqst.getShipmentSkeleton(), "The shipment skeleton is required.");
		Map<String, ShmShipment> existentShmShipmentList = new HashMap<>();
		List<ShipmentSkeleton> shipmentSkeletonList = new ArrayList<>();
		shipmentSkeletonList.add(createShipmentSkeletonRqst.getShipmentSkeleton());
		Collection<CalculateTransitTimeTask> calculateTransitTimeTaskList = new ArrayList<>();
        ShipmentSkeletonHelper
            .populateShmShipmentMap(shipmentSkeletonList, null, existentShmShipmentList, calculateTransitTimeTaskList,
				shipmentDAO, restClient, txnContext, entityManager);

		// PCT-19069: Skip update if billed or rated.
		ShmShipment existentShmShipment = existentShmShipmentList.get(createShipmentSkeletonRqst.getShipmentSkeleton().getParentProNbr());
		if (existentShmShipment != null && BILLED_RATED.contains(BillStatusCdTransformer.toEnum(existentShmShipment.getBillStatCd()))) {
		    return new ShipmentSkeletonResponse();
		}

		return createShipmentSkeleton(shipmentSkeletonList.get(0),createShipmentSkeletonRqst.getCarrierId(), createShipmentSkeletonRqst.getAuditInfo(), null,
            existentShmShipmentList, txnContext, entityManager);
    }

	private ShipmentSkeletonResponse createShipmentSkeleton(ShipmentSkeleton shipmentSkeleton,Long carrierID,
        AuditInfo inAuditInfo, GetSicForPostalCodesResp sicForPostalCodes, Map<String, ShmShipment> existentShmShipmentMap,
        TransactionContext txnContext,
        EntityManager entityManager)
            throws ServiceException, ValidationException {
        checkNotNull(shipmentSkeleton, "The shipment skeleton is required.");

		List<MoreInfo> moreInfos =  Lists.newArrayList();
        List<HandlingUnit> handlingUnits = shipmentSkeleton.getHandlingUnits();

        Set<String> childProNumberList = new HashSet<>();
        for(HandlingUnit handlingUnit: CollectionUtils.emptyIfNull(handlingUnits)) {
        	if(null != handlingUnit.getChildProNbr()) {
        		String childProNbr = ProNumberHelper.validateProNumber(handlingUnit.getChildProNbr(), txnContext);
        		childProNumberList.add(childProNbr);
        		handlingUnit.setChildProNbr(childProNbr);
        	}
        }
        List<ShmHandlingUnit> shmHandlingUnitList = shmHandlingUnitSubDAO.findByChildProNumberList(childProNumberList, entityManager);
		ShipmentSkeletonResponse shipmentSkeletonResponse = new ShipmentSkeletonResponse();


		XMLGregorianCalendar lastMvmtDateTime = shipmentSkeleton.getLastMovementDateTime();
		Timestamp lastMvmtTimestamp;
		if (Objects.isNull(lastMvmtDateTime) || TimestampUtil.isLowTimestamp(BasicTransformer.toTimestamp(lastMvmtDateTime))) {
			lastMvmtTimestamp = Timestamp.from(Instant.now());
			lastMvmtDateTime = TimestampUtil.toXmlGregorianCalendar(lastMvmtTimestamp);
		} else {
			lastMvmtTimestamp = BasicTransformer.toTimestamp(lastMvmtDateTime);
		}

		ServiceTypeCd serviceTypeCd = shipmentSkeleton.getServiceTypeCd();
		if (Objects.isNull(serviceTypeCd)) {
		    serviceTypeCd = ServiceTypeCd.NORMAL;
        }
		String formattedProNbr = shipmentSkeleton.getParentProNbr();

		boolean onUpdate = false;

		ShmShipment existentShmShipment =  existentShmShipmentMap.get(shipmentSkeleton.getParentProNbr());

        boolean isPickup = false;

        if (ShipmentAcquiredTypeCd.REGULAR_PKUP == shipmentSkeleton.getShipmentAcquiredTypeCd()
                || ShipmentAcquiredTypeCd.DOCK_DROP == shipmentSkeleton.getShipmentAcquiredTypeCd() ) {
            isPickup = true;
        }

		if (Objects.nonNull(existentShmShipment)){
			onUpdate = true;
        }

        substractAstraysFromRequestQuantities(shipmentSkeleton);

		validateShmSkeleton(shipmentSkeleton, existentShmShipment, moreInfos, onUpdate, lastMvmtDateTime, shmHandlingUnitList, entityManager, txnContext);

		// Dest sic on an update may be null. If so, use the existing shipment dest sic,
		// otherwise update it
        String destSicCd = ShipmentSkeletonHelper
            .getDestSicCd(shipmentSkeleton, existentShmShipment, sicForPostalCodes, restClient, moreInfos, txnContext);

        List<LnhDimension> lnhDimensions = shipmentSkeleton.getDimensions();
        List<HazMat> hazmatGroups = shipmentSkeleton.getHazmatGroups();

        String cftPrflMthdCd = shipmentSkeleton.getCubicFeetProfileMthdCd() == null ? StringUtils.SPACE :
            ShipmentVolumeMethodCdTransformer.toCode(shipmentSkeleton.getCubicFeetProfileMthdCd());
        String cftPrflTypeCd = shipmentSkeleton.getCubicFeetProfileTypeCd() == null ? StringUtils.SPACE :
            ShipmentVolumeTypeCdTransformer.toCode(shipmentSkeleton.getCubicFeetProfileTypeCd());

		if (CollectionUtils.isNotEmpty(moreInfos)) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo(moreInfos)
					.build();
		}

        AuditInfo auditInfo = getAuditInfo(shipmentSkeleton, inAuditInfo, txnContext);

		Long shipmentInstId;
		BigDecimal calcMvmtSeqNbr = BigDecimal.ZERO;
        String billStatusCd = BillStatusCdTransformer.toCode(BillStatusCd.UNBILLED);
        BigDecimal totalWeight = BigDecimal.ZERO;

        Date pkupDt = null;
        Date estDlvrDt = null;
        boolean mustCreateCubeUpdt = false;

        if (onUpdate) {
            mustCreateCubeUpdt = NumberUtils.compare(shipmentSkeleton.getPupVolumePercentage(), BasicTransformer.toDouble(existentShmShipment.getPupVolPct())) != 0;
            
			updateShmShipment(shipmentSkeleton, existentShmShipment, destSicCd,
					cftPrflMthdCd, cftPrflTypeCd, serviceTypeCd, lastMvmtTimestamp,
                isPickup, auditInfo, txnContext);

			pkupDt = existentShmShipment.getPkupDt();
            estDlvrDt = existentShmShipment.getEstimatedDlvrDt();

			shipmentDAO.save(existentShmShipment, entityManager);
            shipmentDAO.updateDb2ShmShipmentForUpdSkeketon(existentShmShipment, db2EntityManager);
			shipmentInstId = existentShmShipment.getShpInstId();
            billStatusCd = existentShmShipment.getBillStatCd();
            totalWeight = existentShmShipment.getTotWgtLbs();

			if (Objects.nonNull(existentShmShipment.getCalcMvmtSeqNbr())){
				calcMvmtSeqNbr = existentShmShipment.getCalcMvmtSeqNbr();
			}

        } else {
            shipmentSkeleton.setHazmatInd(shipmentSkeleton.getHazmatInd() || CollectionUtils.isNotEmpty(hazmatGroups));

            ShmShipment shmShipmentEntity = createShmShipment(shipmentSkeleton, destSicCd,
                cftPrflMthdCd, cftPrflTypeCd, serviceTypeCd, lastMvmtTimestamp, isPickup, auditInfo, txnContext, entityManager);

            pkupDt = shmShipmentEntity.getPkupDt();
            estDlvrDt = shmShipmentEntity.getEstimatedDlvrDt();

			shipmentDAO.create(shmShipmentEntity, entityManager);
			shipmentDAO.createDB2ShmShipment(shmShipmentEntity, null, db2EntityManager);
			shipmentInstId = shmShipmentEntity.getShpInstId();
            billStatusCd = shmShipmentEntity.getBillStatCd();
            totalWeight = shmShipmentEntity.getTotWgtLbs();


			if (Objects.nonNull(shmShipmentEntity.getCalcMvmtSeqNbr())){
				calcMvmtSeqNbr = shmShipmentEntity.getCalcMvmtSeqNbr();
			}

            createShipmentMovement(shmShipmentEntity, shipmentSkeleton,carrierID, auditInfo, entityManager);

        }

        deleteMissingShmHandlingUnits(handlingUnits, existentShmShipment, entityManager, txnContext);

        upsertProFrtBillIndexByPro(shipmentSkeleton, shipmentInstId, auditInfo, entityManager, txnContext);

		UpdateShipmentLinehaulDimensionsRqst updateShipmentLinehaulDimensionsRqst = new UpdateShipmentLinehaulDimensionsRqst();
		ShipmentWithDimension shipmentWithDimension = new ShipmentWithDimension();
		shipmentWithDimension.setShipmentInstId(shipmentInstId);
        List<LnhDimension> linehaulDimensions = new ArrayList<>();

        if (CollectionUtils.isEmpty(lnhDimensions) && CollectionUtils.isNotEmpty(handlingUnits)) {

            if (lnhDimensions == null) {
                lnhDimensions = new ArrayList<>();
            }

            // Build dimensions based on handlingUnits
            Double lengthNbr = null;
            Double heightNbr = null;
            Double widthNbr = null;
            for (HandlingUnit handlingUnit : handlingUnits) {
                if (isAstrayHUMovCd(handlingUnit)) {
                    continue;
                }

                lengthNbr = handlingUnit.getLengthNbr() == null? 0D: handlingUnit.getLengthNbr();
                heightNbr = handlingUnit.getHeightNbr() == null? 0D: handlingUnit.getHeightNbr();
                widthNbr = handlingUnit.getWidthNbr() == null? 0D: handlingUnit.getWidthNbr();

                if (Double.compare(lengthNbr, 0D) > 0 || Double.compare(heightNbr, 0D) > 0 || Double.compare(widthNbr, 0D) > 0) {
                    LnhDimension lnhDimension = new LnhDimension();
                    lnhDimension.setShipmentInstId(shipmentInstId);
                    lnhDimension.setPiecesCount(1L);
                    lnhDimension.setLengthNbr(handlingUnit.getLengthNbr());
                    lnhDimension.setWidthNbr(handlingUnit.getWidthNbr());
                    lnhDimension.setHeightNbr(handlingUnit.getHeightNbr());
                    lnhDimension.setStackableInd(handlingUnit.getStackableInd());
                    lnhDimension.setCapturedByCd(MAP_CAPTURED_BY_CD.get(handlingUnit.getDimensionTypeCd()));
                    lnhDimensions.add(lnhDimension);
                }
            }
        }

        for (LnhDimension lnhDimension : CollectionUtils.emptyIfNull(lnhDimensions)) {
            lnhDimension.setShipmentInstId(shipmentInstId);
            if (StringUtils.isBlank(lnhDimension.getCapturedByCd())) {
                lnhDimension.setCapturedByCd(
                    isPickup ? PICKUP_CAPTURE_CD: DOCK_CAPTURE_CD);
            }
            linehaulDimensions.add(lnhDimension);
        }

        String requestingSicCd = shipmentSkeleton.getRequestingSicCd();

        shipmentWithDimension.setLinehaulDimensions(linehaulDimensions);
		List<ShipmentWithDimension> shipmentWithDimensions = Lists.newArrayList(shipmentWithDimension);
        updateShipmentLinehaulDimensionsRqst.setShipmentWithDimensions(shipmentWithDimensions);
        updateShipmentLinehaulDimensionsRqst.setRequestingSicCd(requestingSicCd);
        updateShipmentLinehaulDimensionsRqst.setCapturedByUserId(auditInfo.getCreatedById());

        if (CollectionUtils.isNotEmpty(lnhDimensions)){
            updateShipmentLinehaulDimensionsImpl.updateShipmentLinehaulDimensions(updateShipmentLinehaulDimensionsRqst,
                shipmentSkeleton.getPupVolumePercentage(), shipmentSkeleton.getTotalVolumeCubicFeet(),
                shipmentSkeleton.getCubicFeetProfileMthdCd(), shipmentSkeleton.getCubicFeetProfileTypeCd(),
                mustCreateCubeUpdt, 
                entityManager, txnContext);
        }

        if (onUpdate) {
            shmHazMatSubDAO.bulkDeleteByShipmentInstId(shipmentInstId, entityManager);
            shmHazMatSubDAO.bulkDeleteByShipmentInstIdFromDB2(shipmentInstId, db2EntityManager);
            if (CollectionUtils.isNotEmpty(existentShmShipment.getShmHazMats())) {
                existentShmShipment.getShmHazMats().forEach(x -> entityManager.detach(x));
                existentShmShipment.setShmHazMats(null);
            }
            entityManager.flush();
            db2EntityManager.flush();
        }

        long hmSeqNbr = 1L;
        for (HazMat hazMat : CollectionUtils.emptyIfNull(hazmatGroups)) {
            createShmHazMat(hazMat, shipmentInstId, hmSeqNbr++, auditInfo, entityManager, txnContext);
        }

		if (CollectionUtils.isNotEmpty(handlingUnits)){

            Calendar lastMvmtDateTimeCal = BasicTransformer.toCalendar(lastMvmtDateTime);
            lastMvmtDateTimeCal.add(Calendar.MILLISECOND, 500);

            reweightHandlingUnitsEvenly(handlingUnits, shmHandlingUnitList, totalWeight);

            for (HandlingUnit handlingUnit : handlingUnits) {

                ShmHandlingUnit shmHandlingUnit = null;
                for(ShmHandlingUnit shmHU: CollectionUtils.emptyIfNull(shmHandlingUnitList)) {
					if(null != shmHU.getChildProNbrTxt() && StringUtils.equals(shmHU.getChildProNbrTxt(), handlingUnit.getChildProNbr())) {
						shmHandlingUnit = shmHU;
					}
				}

                if(Objects.isNull(shmHandlingUnit)) {
                    //weight does not have to be redistributed on update
                    if (!isAstrayHUMovCd(handlingUnit)) {
                        calculateAndSetVolumeCubicFeet(handlingUnit);
                        if (StringUtils.isBlank(handlingUnit.getDimensionTypeCd())) {
                            handlingUnit.setDimensionTypeCd(isPickup ? PICKUP_DIMENSION_TYPE_CD : DOCK_DIMENSION_TYPE_CD);
                        }
                    }

                    shmHandlingUnit = createShmHandlingUnit(handlingUnit, shipmentInstId,
                            requestingSicCd, lastMvmtDateTime, auditInfo, entityManager, txnContext);

                    BigInteger movememntSequenceNumber = BigInteger
                        .valueOf(shmHandlingUnitMvmtSubDAO.getNextMvmtBySeqNbrAndShpInstId(shipmentInstId,
                            shmHandlingUnit.getId().getSeqNbr(), entityManager));

                    if (isPickup) {

                        createShmHandlingUnitMvmt(shipmentInstId, shmHandlingUnit.getId().getSeqNbr(),
                            movememntSequenceNumber, requestingSicCd,
                            BasicTransformer.toXMLGregorianCalendar(lastMvmtDateTimeCal),
                            HandlingUnitMovementTypeCd.LOAD,
                            BasicTransformer.toLong(shmHandlingUnit.getCurrentTrlrInstId()), auditInfo, entityManager,
                            txnContext);

                        createShmHandlingUnitMvmt(shipmentInstId, shmHandlingUnit.getId().getSeqNbr(),
                            movememntSequenceNumber.add(BigInteger.ONE), requestingSicCd,
                            lastMvmtDateTime, HandlingUnitMovementTypeCd.PICKUP, null, auditInfo, entityManager,
                            txnContext);

                    } else {

                        createShmHandlingUnitMvmt(shipmentInstId, shmHandlingUnit.getId().getSeqNbr(),
                            movememntSequenceNumber, requestingSicCd,
                            lastMvmtDateTime, HandlingUnitMovementTypeCd.UNLOAD, null, auditInfo, entityManager,
                            txnContext);
                    }

                }else{
                    if (!isAstrayHUMovCd(handlingUnit)) {
                        calculateAndSetVolumeCubicFeet(handlingUnit);
                        if (StringUtils.isBlank(handlingUnit.getDimensionTypeCd())) {
                            handlingUnit.setDimensionTypeCd(isPickup ? PICKUP_DIMENSION_TYPE_CD : DOCK_DIMENSION_TYPE_CD);
                        }
                    }
                    if (BasicTransformer.toBoolean(shmHandlingUnit.getReweighInd())) {
                        handlingUnit.setReweighInd(true);
                    }
                    updateShmHandlingUnit(handlingUnit, shipmentInstId, shmHandlingUnit,
                            requestingSicCd, lastMvmtDateTime, auditInfo, entityManager, txnContext);
                }

			}
		}
        BigDecimal shprCustNbr = ZERO;

        if (shipmentSkeleton.getShipperParty() != null) {

            boolean hasShipper = false;
            if (onUpdate) {
                ShmAsEntdCust shipper = shipmentAsEnteredCustomerDAO.findByShipmentIdAndTypeCd(shipmentInstId,
                    MatchedPartyTypeCdTransformer.toCode(MatchedPartyTypeCd.SHPR), entityManager);
                hasShipper = shipper != null;
            }

            if (!hasShipper) {
                ShmAsEntdCust shmAsEntdCust = createShipper(shipmentInstId, shipmentSkeleton.getShipperParty(),
                    auditInfo, entityManager);
                shipmentAsEnteredCustomerDAO.save(shmAsEntdCust, entityManager);
                shipmentAsEnteredCustomerDAO.createDB2ShmAsEntdCust(shmAsEntdCust, db2EntityManager);
                shprCustNbr = shmAsEntdCust.getCisCustNbr();
            }
        }

        //build response
        shipmentSkeletonResponse.setSkeletonBasedOnPickup(false);
        shipmentSkeletonResponse.setCalculatedMovementSequenceNbr(BasicTransformer.toBigInteger(calcMvmtSeqNbr));
        shipmentSkeletonResponse.setDestinationSicCd(destSicCd);
        shipmentSkeletonResponse.setHazmatInd(BooleanUtils.isTrue(shipmentSkeleton.getHazmatInd()));
        shipmentSkeletonResponse.setLastMovementDateTime(lastMvmtDateTime);
        shipmentSkeletonResponse.setProNbr(formattedProNbr);
        shipmentSkeletonResponse.setReportingSicCd(requestingSicCd);
        shipmentSkeletonResponse.setShipmentInstId(shipmentInstId);
        shipmentSkeletonResponse.setTotalPiecesCount(shipmentSkeleton.getTotalPiecesCount());
        shipmentSkeletonResponse.setTotalWeightLbs(shipmentSkeleton.getTotalWeightLbs());
        shipmentSkeletonResponse.setShipperCustNbr(BasicTransformer.toLong(shprCustNbr));

        if (isPickup) {
            //build the event to be created for PU at a later time
            shipmentSkeletonResponse.setSkeletonBasedOnPickup(true);
            if(!onUpdate) {
                createEventLog(EventLogTypeCd.PICKUP, EventLogSubTypeCd.PICKUP_REQUEST,
                        shipmentInstId, formattedProNbr, requestingSicCd, destSicCd,
                        BasicTransformer.toTimestamp(lastMvmtDateTime), calcMvmtSeqNbr,
                        BasicTransformer.toBigDecimal(shipmentSkeleton.getTotalPiecesCount()),
                        BooleanUtils.isTrue(shipmentSkeleton.getHazmatInd()),
                        BasicTransformer.toBigDecimal(shipmentSkeleton.getTotalWeightLbs()), shprCustNbr,
                        pkupDt, estDlvrDt, billStatusCd,
                        auditInfo, entityManager, txnContext);
            }
        } else {

            String reportingSicCd = shipmentSkeleton.getLastMoveRptgSicCd() != null ?
                shipmentSkeleton.getLastMoveRptgSicCd() :
                StringUtils.SPACE;
            if(!onUpdate) {
                createEventLog(EventLogTypeCd.BILL_ENTRY, EventLogSubTypeCd.LOADING_SKEL_PRO, shipmentInstId, formattedProNbr,
                        reportingSicCd, destSicCd, lastMvmtTimestamp, calcMvmtSeqNbr,
                        new BigDecimal(shipmentSkeleton.getTotalPiecesCount()), shipmentSkeleton.getHazmatInd(),
                        new BigDecimal(shipmentSkeleton.getTotalWeightLbs()), ZERO, pkupDt, estDlvrDt, billStatusCd,
                        auditInfo, entityManager,
                        txnContext);
            }
        }

        if (onUpdate && shipmentSkeleton.getLateTenderCd() != null
                && BasicTransformer.toLong(existentShmShipment.getBillStatCd()) > BasicTransformer
                    .toLong(BillStatusCdTransformer.toCode(BillStatusCd.IN_FBES_SUSPENSE_QUEUE))
                && !existentShmShipment.getLateTenderCd().equals(LateTenderCdTransformer
                    .toCode(shipmentSkeleton.getLateTenderCd()))) {
            ShmRemarkPK id = new ShmRemarkPK();
            id.setShpInstId(existentShmShipment.getShpInstId());
            id.setTypCd(ShipmentRemarkTypeCdTransformer.toCode(ShipmentRemarkTypeCd.SHIPPING_RMK));
            ShmRemark shipmentRemark = shipmentRemarkSubDAO.findById(id, entityManager);
            if (shipmentRemark != null) {

                boolean hasEventCreation = false;
                String newRemarkText = "";
                if (!LateTenderCdTransformer
                    .toCode(shipmentSkeleton.getLateTenderCd())
                    .equals(LateTenderCdTransformer.toCode(LateTenderCd.NOT_A_LATE_TENDER))
                        && existentShmShipment.getLateTenderCd().equals(
                            LateTenderCdTransformer.toCode(LateTenderCd.NOT_A_LATE_TENDER))) {
                    hasEventCreation = true;
                    newRemarkText = shipmentRemark.getRmkTxt() != null ?
                        shipmentRemark.getRmkTxt().concat(REMARK_AFTER_HOUR_TEXT) :
                        REMARK_AFTER_HOUR_TEXT;

                } else if (LateTenderCdTransformer
                    .toCode(shipmentSkeleton.getLateTenderCd())
                    .equals(LateTenderCdTransformer.toCode(LateTenderCd.NOT_A_LATE_TENDER))
                        && !existentShmShipment.getLateTenderCd().equals(
                            LateTenderCdTransformer.toCode(LateTenderCd.NOT_A_LATE_TENDER))) {

                    hasEventCreation = true;
                    newRemarkText = shipmentRemark.getRmkTxt() != null ?
                        shipmentRemark.getRmkTxt().replace(REMARK_AFTER_HOUR_TEXT, "") :
                        "";
                }

                if (hasEventCreation) {

                    shipmentRemark.setRmkTxt(newRemarkText);
                    DtoTransformer.setLstUpdateAuditInfo(shipmentRemark, auditInfo);
                    shipmentRemarkSubDAO.save(shipmentRemark, entityManager);
                    shipmentRemarkSubDAO.updateDB2ShmRemark(shipmentRemark, shipmentRemark.getLstUpdtTmst(), txnContext,
                        db2EntityManager);

                    createEventLog(EventLogTypeCd.CORRECTIONS, EventLogSubTypeCd.SHIPMENT_CORRECTION, shipmentInstId,
                        formattedProNbr, requestingSicCd, destSicCd, lastMvmtTimestamp, calcMvmtSeqNbr,
                        new BigDecimal(shipmentSkeleton.getTotalPiecesCount()), shipmentSkeleton.getHazmatInd(),
                        new BigDecimal(shipmentSkeleton.getTotalWeightLbs()), ZERO, pkupDt, estDlvrDt, billStatusCd, auditInfo,
                        entityManager,
                        txnContext);

                }
            }
            shipmentSkeletonResponse.setBolInstId(shipmentSkeleton.getBolInstId());
            shipmentSkeletonResponse.setLateTenderCd(LateTenderCd.LATE_TENDER);

        } else {
            shipmentSkeletonResponse.setBolInstId(shipmentSkeleton.getBolInstId());
            shipmentSkeletonResponse.setLateTenderCd(LateTenderCd.NOT_A_LATE_TENDER);

        }

        return shipmentSkeletonResponse;
    }

    /**
     * <p>
     * All HUs are received in the request, so it's not necessary to search in the DB.
     * First we filter the ones with <b>reweightInd</b> or movement is <b>astray</b> out.
     * then substract reweighted HUs to the totalWeight.
     * Finally, distribute the remainder evenly to every HU (new HUs as
     * well as the old ones).
     * </p>
     * <p>
     * <b>This method updates the weight of the Handling Units passed by param.</b>
     * </p>
     *
     * @param handlingUnits
     */
    protected void reweightHandlingUnitsEvenly(List<HandlingUnit> handlingUnits, final List<ShmHandlingUnit> shmHandlingUnitListDB,
        BigDecimal totalWeight) {

        if (ObjectUtils.isEmpty(handlingUnits) || ObjectUtils.isEmpty(totalWeight)) {
            return;
        }

        // if the HU is an update set the reweight ind from DB, otherwise false.
        handlingUnits.stream().forEach(hu -> {
            Optional<ShmHandlingUnit> shmHuDB = ListUtils
                .emptyIfNull(shmHandlingUnitListDB)
                .stream()
                .filter(huDB -> StringUtils.equals(huDB.getChildProNbrTxt(), hu.getChildProNbr()))
                .findAny();
            hu.setReweighInd(shmHuDB.isPresent() ? BasicTransformer.toBoolean(shmHuDB.get().getReweighInd()) : false);
        });

        List<HandlingUnit> handlingUnitToBeReweighted = handlingUnits
            .stream()
            .filter(hu -> !isAstrayHUMovCd(hu) && !hu.getReweighInd())
            .collect(Collectors.toList());

        if (handlingUnitToBeReweighted.isEmpty()) {
            return;
        }

        Double totalHUWeightAlreadyReWeighted = handlingUnits
            .stream()
            .filter(hu -> hu.getReweighInd())
            .map(hu -> hu.getWeightLbs())
            .reduce((x, y) -> x + y)
            .orElse(0D);

        Double totalToBeReweighted = BasicTransformer.toDouble(totalWeight) - totalHUWeightAlreadyReWeighted;

        BigDecimal equallyDividedWeight = NumberUtils.compare(totalToBeReweighted, 0D) == 1
                ? new BigDecimal(totalToBeReweighted / handlingUnitToBeReweighted.size()).setScale(2, RoundingMode.HALF_DOWN)
                    : BigDecimal.ONE;

        handlingUnitToBeReweighted.stream().forEach(hu -> hu.setWeightLbs(BasicTransformer.toDouble(equallyDividedWeight)));
    }

    private void deleteMissingShmHandlingUnits(List<HandlingUnit> handlingUnits,
        ShmShipment existentShmShipment, EntityManager entityManager, TransactionContext txnContext)
            throws ValidationException, NotFoundException {

        if (Objects.isNull(existentShmShipment)) {
            return;
        }

        List<String> reqChildPros = CollectionUtils.emptyIfNull(handlingUnits).stream().map(hu -> hu.getChildProNbr()).collect(Collectors.toList());
        List<ShmHandlingUnit> candidatesToBeDeleted = CollectionUtils
            .emptyIfNull(existentShmShipment.getShmHandlingUnits())
            .stream()
            .filter(dbHu -> !reqChildPros.contains(dbHu.getChildProNbrTxt()))
            .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(candidatesToBeDeleted)) {
            return;
        }

        shmHandlingUnitSubDAO.remove(candidatesToBeDeleted, entityManager);
        for (ShmHandlingUnit toBeDeleted : candidatesToBeDeleted) {
            shmHandlingUnitSubDAO
                .deleteDB2(toBeDeleted.getId(), toBeDeleted.getLstUpdtTmst(), db2EntityManager, txnContext);
        }
    }

    /**
     * if there is any ASTRAY hu mov cd, it has to be substracted from loose, mm and total qties when appropiate. <br/>
     * they will be removed only if there are ASTRAYS in the request and the
     * {@code ShipmentSkeleton#getTotalPiecesCount()} is the
     * same as the size of the HU List.
     * NOTE: {@link ShipmentSkeleton} properties will be updated.
     *
     * @param shipmentSkeleton
     */
    private void substractAstraysFromRequestQuantities(ShipmentSkeleton shipmentSkeleton) {


        long huLooseAndAstrayQty = CollectionUtils
            .emptyIfNull(shipmentSkeleton.getHandlingUnits())
            .stream()
            .filter(hu -> HandlingUnitTypeCd.LOOSE == hu.getTypeCd() && isAstrayHUMovCd(hu))
            .count();
        long huMMAndAstrayQty = CollectionUtils

            .emptyIfNull(shipmentSkeleton.getHandlingUnits())
            .stream()
            .filter(hu -> HandlingUnitTypeCd.MOTOR == hu.getTypeCd() && isAstrayHUMovCd(hu))
            .count();

        BigInteger totalPcsCnt = shipmentSkeleton.getTotalPiecesCount();
        BigInteger huSize = BigInteger
                .valueOf(CollectionUtils.emptyIfNull(shipmentSkeleton.getHandlingUnits()).size());

        if ((huLooseAndAstrayQty > 0 || huMMAndAstrayQty > 0) && totalPcsCnt.equals(huSize)) {
            shipmentSkeleton
                .setLoosePiecesCount(
                    shipmentSkeleton.getLoosePiecesCount().subtract(BigInteger.valueOf(huLooseAndAstrayQty)));
            shipmentSkeleton
                .setMotorizedPiecesCount(
                    shipmentSkeleton.getMotorizedPiecesCount().subtract(BigInteger.valueOf(huMMAndAstrayQty)));
            shipmentSkeleton
                .setTotalPiecesCount(shipmentSkeleton
                    .getTotalPiecesCount()
                    .subtract(BigInteger.valueOf(huLooseAndAstrayQty + huMMAndAstrayQty)));

        }

    }

    private boolean isAstrayHUMovCd(HandlingUnit hu) {
        return ASTRAY_MOVEMENT_CD.equals(hu.getHandlingMovementCd());
    }

    private void calculateAndSetVolumeCubicFeet(HandlingUnit handlingUnit) {
        Double handlingUnitLength = handlingUnit.getLengthNbr();
        Double handlingUnitWidth = handlingUnit.getWidthNbr();
        Double handlingUnitHeight = handlingUnit.getHeightNbr();
        boolean isNotVolCubicFtCalculated = Objects.isNull(handlingUnit.getVolumeCubicFeet())
                || handlingUnit.getVolumeCubicFeet() <= 0;
        if (isNotVolCubicFtCalculated && Objects.nonNull(handlingUnitHeight) && Objects.nonNull(handlingUnitLength)
                && Objects.nonNull(handlingUnitWidth)) {
            handlingUnit
                .setVolumeCubicFeet(
                    HandlingUnitHelper.calculateVolCubFt(handlingUnitLength, handlingUnitWidth, handlingUnitHeight));
        }
    }

    private AuditInfo getAuditInfo(ShipmentSkeleton shipmentSkeleton, AuditInfo inAuditInfo, TransactionContext txnContext) {

        AuditInfo auditInfo = inAuditInfo;
        //PCT-18058 FIX. Use the input auditInfo when supplied.
        if(auditInfo !=null) {
            if(StringUtils.isBlank(auditInfo.getUpdateByPgmId())){
                auditInfo.setCreateByPgmId(COMMON_PGM_ID);
                auditInfo.setUpdateByPgmId(COMMON_PGM_ID);
            }
        } else {
            if (StringUtils.isNotBlank(shipmentSkeleton.getUserId())) {
                auditInfo = AuditInfoHelper.getAuditInfoWithPgmAndUserId(COMMON_PGM_ID, shipmentSkeleton.getUserId(), txnContext);
            } else {
                auditInfo = AuditInfoHelper.getAuditInfoWithPgmId(COMMON_PGM_ID, txnContext);
            }
        }
        return auditInfo;
    }

    private ShmMovement createShipmentMovement(ShmShipment shmShipmentEntity, ShipmentSkeleton shipmentSkeleton, Long carrierID,
        AuditInfo auditInfo, EntityManager entityManager) throws ValidationException {

        ShmMovement shmMovement = new ShmMovement();
        ShmMovementPK id = new ShmMovementPK();
        id.setShpInstId(shmShipmentEntity.getShpInstId());
        id.setSeqNbr(1);
        shmMovement.setId(id);
        DtoTransformer.setAuditInfo(shmMovement, auditInfo);
        shmMovement.setCrteUid(auditInfo.getCreatedById());
        shmMovement.setArchiveCntlCd(StringUtils.SPACE);
        shmMovement.setCurrSicCd(shmShipmentEntity.getCurrSicCd());
        shmMovement.setCustLocArivTmst(shipmentSkeleton.getCustomerLocationArrivalDateTime() != null ?
            BasicTransformer.toTimestamp(shipmentSkeleton.getCustomerLocationArrivalDateTime()) :
            Timestamp.from(Instant.now()));
        shmMovement.setDlvryQalfrCd(StringUtils.SPACE);
        shmMovement.setMvmtRptgSicCd(shmShipmentEntity.getLstMvRptgSicCd());
        shmMovement.setMvmtTmst(shmShipmentEntity.getLstMvmtTmst());
        shmMovement.setObcPkpDlvRteCd(StringUtils.SPACE);
        shmMovement.setQlfrRsnCd(StringUtils.SPACE);
        shmMovement.setRshpCredInd(DEFAULT_IND_VALUE);
        shmMovement.setRtePfxTxt(StringUtils.SPACE);
        shmMovement.setRteSfxTxt(StringUtils.SPACE);
        shmMovement.setScacCd(StringUtils.SPACE);
        shmMovement.setTrlrIdPfxTxt(StringUtils.SPACE);
        shmMovement.setTrlrIdSfxNbr(BigDecimal.ZERO);
        shmMovement.setTrlrInstId(BigDecimal.ZERO);
        shmMovement.setTrlrLdSeqNbr(BigDecimal.ZERO);
        shmMovement.setTypCd(ShipmentMovementTypeCdTransformer.toCode(ShipmentMovementTypeCd.PICKUP));
        BigDecimal carrierIdValue=ObjectUtils.defaultIfNull(BasicTransformer.toBigDecimal(carrierID), BigDecimal.ZERO);
        shmMovement.setCarrierId(carrierIdValue);

        shmMovementSubDAO.save(shmMovement, entityManager);
        shmMovementSubDAO.createDB2ShmMovement(shmMovement, db2EntityManager);
        return shmMovement;

    }

    private ShmAsEntdCust createShipper(Long shipmentInstId, AsMatchedParty shipperParty, AuditInfo auditInfo,
        EntityManager entityManager) {

        ShmAsEntdCust shmAsEntdCust = new ShmAsEntdCust();
        ShmAsEntdCustPK id = new ShmAsEntdCustPK();
        id.setShpInstId(shipmentInstId);
        id.setSeqNbr(shipmentAsEnteredCustomerDAO.getNextSeqNbrByShpInstId(shipmentInstId, entityManager));
        shmAsEntdCust.setId(id);
        DtoTransformer.setAuditInfo(shmAsEntdCust, auditInfo);
        shmAsEntdCust.setAddrTxt(shipperParty.getAddress() != null ? shipperParty.getAddress() : StringUtils.SPACE);
        shmAsEntdCust.setAllShpmtPpdInd(StringUtils.SPACE);
        shmAsEntdCust.setAlternateCustNbr(BigDecimal.ZERO);
        shmAsEntdCust.setArchiveCntlCd(StringUtils.SPACE);
        shmAsEntdCust.setAsMchMadCd(StringUtils.SPACE);
        shmAsEntdCust.setBiltoRelCd(StringUtils.SPACE);
        shmAsEntdCust.setBrkrCustKeyNbr(BigDecimal.ZERO);
        shmAsEntdCust.setCisCustNbr(
            shipperParty.getCisCustNbr() != null ? BasicTransformer.toBigDecimal(shipperParty.getCisCustNbr()) :
                BigDecimal.ZERO);
        shmAsEntdCust.setCntryCd(shipperParty.getCountryCd() != null ? shipperParty.getCountryCd() : StringUtils.SPACE);
        shmAsEntdCust.setCredStatCd(shipperParty.getCreditStatusCd() != null ?
            ShipmentCreditStatusCdTransformer.toCode(shipperParty.getCreditStatusCd()) :
            StringUtils.SPACE);
        shmAsEntdCust.setCtyTxt(shipperParty.getCity() != null ? shipperParty.getCity() : StringUtils.SPACE);
        shmAsEntdCust.setDebtorInd(StringUtils.SPACE);
        shmAsEntdCust.setDirCd(StringUtils.SPACE);
        shmAsEntdCust.setEMailId(StringUtils.SPACE);
        shmAsEntdCust.setLstMchTmst(TimestampUtil.getLowTimestamp());
        shmAsEntdCust.setMchInitTxt(StringUtils.SPACE);
        shmAsEntdCust.setMchSourceCd(StringUtils.SPACE);
        shmAsEntdCust.setMchStatCd(StringUtils.SPACE);
        shmAsEntdCust.setName1Txt(shipperParty.getName1() != null ? shipperParty.getName1() : StringUtils.SPACE);
        shmAsEntdCust.setName2Txt(shipperParty.getName2() != null ? shipperParty.getName2() : StringUtils.SPACE);
        shmAsEntdCust.setPacdNbr(
            shipperParty.getPhoneAreaCdNbr() != null ? shipperParty.getPhoneAreaCdNbr() : StringUtils.SPACE);
        shmAsEntdCust.setPccdNbr(
            shipperParty.getPhoneCountryCdNbr() != null ? shipperParty.getPhoneCountryCdNbr() : StringUtils.SPACE);
        shmAsEntdCust.setPextNbr(
            shipperParty.getPhoneExtensionNbr() != null ? shipperParty.getPhoneExtensionNbr() : StringUtils.SPACE);
        shmAsEntdCust.setPhonNbr(shipperParty.getPhoneNbr() != null ? shipperParty.getPhoneNbr() : StringUtils.SPACE);
        shmAsEntdCust.setPodImgInd(StringUtils.SPACE);
        shmAsEntdCust.setPrefPmtCrncyCd(StringUtils.SPACE);
        shmAsEntdCust.setSelfInvcInd(StringUtils.SPACE);
        shmAsEntdCust.setStCd(shipperParty.getStateCd() != null ? shipperParty.getStateCd() : StringUtils.SPACE);
        shmAsEntdCust.setTypCd(MatchedPartyTypeCdTransformer.toCode(MatchedPartyTypeCd.SHPR));
        shmAsEntdCust.setUseAsEntrdInd(StringUtils.SPACE);
        shmAsEntdCust
            .setZip4RestUsTxt(shipperParty.getZip4RestUs() != null ? shipperParty.getZip4RestUs() : StringUtils.SPACE);
        shmAsEntdCust.setZip6Txt(shipperParty.getZip6() != null ? shipperParty.getZip6() : StringUtils.SPACE);
        shmAsEntdCust.setPodRqrdInd(StringUtils.SPACE);

        return shmAsEntdCust;
    }

    private boolean isSupervisor(List<EmployeeRole> roles) {

		if (CollectionUtils.isEmpty(roles)) {
			return false;
		}

		for (EmployeeRole role : roles) {
			Date startDate = BasicTransformer.toDate(role.getStartDate());
			Date expDate = BasicTransformer.toDate(role.getExpirationDate());
			Date now = new Date();
			String roleId = role.getRole() != null ? role.getRole().getRoleId() : null;

			if (SUPRV_ROLE_ID.equals(roleId) && startDate != null && startDate.before(now)
					&& (expDate == null || expDate.after(now))) {
				return true;
			}
		}

		return false;
	}

	private ShmHandlingUnit createShmHandlingUnit(HandlingUnit handlingUnit, long shpInstId,
        String requestingSicCd, XMLGregorianCalendar lastMvmtDateTime, AuditInfo auditInfo, EntityManager entityManager,
			TransactionContext txnContext) throws ServiceException{

        setDefaultsHandlingUnit(handlingUnit, shpInstId, requestingSicCd, lastMvmtDateTime,
                auditInfo, entityManager);

        ShmHandlingUnit shmHandlingUnit = DtoTransformer.toShmHandlingUnit(handlingUnit, null);
		DtoTransformer.setAuditInfo(shmHandlingUnit, auditInfo);
        shmHandlingUnitSubDAO.save(shmHandlingUnit, entityManager);
		shmHandlingUnitSubDAO.createDB2ShmHandlingUnit(shmHandlingUnit, db2EntityManager);
		return shmHandlingUnit;
	}

    private ShmHandlingUnit updateShmHandlingUnit(HandlingUnit handlingUnit, long shpInstId, ShmHandlingUnit shmHandlingUnit,
            String requestingSicCd, XMLGregorianCalendar lastMvmtDateTime, AuditInfo auditInfo, EntityManager entityManager,
            TransactionContext txnContext) throws ServiceException{

        setDefaultsHandlingUnit(handlingUnit, shpInstId, requestingSicCd, lastMvmtDateTime,
            auditInfo, entityManager);
        shmHandlingUnit.setWgtLbs(BasicTransformer.toBigDecimal(handlingUnit.getWeightLbs()));
        shmHandlingUnit.setWidthNbr(BasicTransformer.toBigDecimal(handlingUnit.getWidthNbr()));
        shmHandlingUnit.setLengthNbr(BasicTransformer.toBigDecimal(handlingUnit.getLengthNbr()));
        shmHandlingUnit.setHeightNbr(BasicTransformer.toBigDecimal(handlingUnit.getHeightNbr()));
        shmHandlingUnit.setVolCft(BasicTransformer.toBigDecimal(handlingUnit.getVolumeCubicFeet()));
        shmHandlingUnit.setPupVolPct(BasicTransformer.toBigDecimal(handlingUnit.getPupVolumePercentage()));
        shmHandlingUnit.setTypeCd(HandlingUnitTypeCdTransformer.toCode(handlingUnit.getTypeCd()));
        shmHandlingUnit.setHandlingMvmtCd(handlingUnit.getHandlingMovementCd());
        shmHandlingUnit.setDimensionTypeCd(handlingUnit.getDimensionTypeCd());
        DtoTransformer.setLstUpdateAuditInfo(shmHandlingUnit, auditInfo);

        shmHandlingUnitSubDAO.save(shmHandlingUnit, entityManager);
        shmHandlingUnitSubDAO.updateDB2ShmHandlingUnit(shmHandlingUnit, shmHandlingUnit.getLstUpdtTmst(), txnContext,
                db2EntityManager);
        return shmHandlingUnit;
    }

    private void setDefaultsHandlingUnit(HandlingUnit handlingUnit, long shpInstId, String requestingSicCd,
            XMLGregorianCalendar lastMvmtDateTime, AuditInfo auditInfo, EntityManager entityManager) {

        handlingUnit.setShipmentInstanceId(shpInstId);
        handlingUnit.setSequenceNbr(
                BigInteger.valueOf(shmHandlingUnitSubDAO.getNextSeqNbrByShpInstId(shpInstId, entityManager)));

        if (Objects.isNull(handlingUnit.getAuditInfo())) {
            handlingUnit.setAuditInfo(auditInfo);
        }
        if (Objects.isNull(handlingUnit.getCurrentSicCd())) {
            handlingUnit.setCurrentSicCd(requestingSicCd);
        }

        if (Objects.isNull(handlingUnit.getCurrentDockLocation())) {
            handlingUnit.setCurrentDockLocation(StringUtils.SPACE);
        }
        if (Objects.isNull(handlingUnit.getDimensionTypeCd())) {
            handlingUnit.setDimensionTypeCd(StringUtils.SPACE);
        }
        if (Objects.isNull(handlingUnit.getHandlingMovementCd())) {
            handlingUnit.setHandlingMovementCd(NORMAL_MOVEMENT_CD);
        }
        if (Objects.isNull(handlingUnit.getTypeCd())) {
            handlingUnit.setTypeCd(HandlingUnitTypeCd.UNKNOWN);
        }
        if (Objects.isNull(handlingUnit.getMovementStatusCd())) {
            handlingUnit.setMovementStatusCd(MovementStatusCdTransformer.toCode(MovementStatusCd.ON_DOCK));
        }
        if (Objects.isNull(handlingUnit.getLastMovementDateTime())) {
            handlingUnit.setLastMovementDateTime(lastMvmtDateTime);
        }
        if (Objects.isNull(handlingUnit.getArchiveInd())) {
            handlingUnit.setArchiveInd(false);
        }
        if (Objects.isNull(handlingUnit.getStackableInd())) {
            handlingUnit.setStackableInd(false);
        }
        if (handlingUnit.getReweighInd() == null) {
            handlingUnit.setReweighInd(false);
        }
        if (Objects.isNull(handlingUnit.getPickupDate())) {
            handlingUnit.setPickupDate(BasicTransformer.toDateString(new Date()));
        }
        if (Objects.isNull(handlingUnit.getWeightLbs())) {
            handlingUnit.setWeightLbs(new Double(1));
        }
        if (Objects.isNull(handlingUnit.getCurrentTrailerInstanceId())) {
            handlingUnit.setCurrentTrailerInstanceId(0L);
        }
        if (Objects.isNull(handlingUnit.getVolumeCubicFeet())) {
            handlingUnit.setVolumeCubicFeet(0D);
        }
        if (Objects.isNull(handlingUnit.getLengthNbr())) {
            handlingUnit.setLengthNbr(new Double(0));
        }
        if (Objects.isNull(handlingUnit.getWidthNbr())) {
            handlingUnit.setWidthNbr(new Double(0));
        }
        if (Objects.isNull(handlingUnit.getHeightNbr())) {
            handlingUnit.setHeightNbr(new Double(0));
        }
        if (Objects.isNull(handlingUnit.getPoorlyPackagedInd())) {
            handlingUnit.setPoorlyPackagedInd(false);
        }
        if (Objects.isNull(handlingUnit.getSplitInd())) {
            handlingUnit.setSplitInd(false);
        }
    }

    private void createShmHandlingUnitMvmt(long shpInstId,
        long seqNbr, BigInteger movementSequenceNumber, String requestingSicCd, XMLGregorianCalendar lastMvmtDateTime,
        HandlingUnitMovementTypeCd handlingUnitMovementTypeCd, Long trailerInstanceId, AuditInfo auditInfo,
        EntityManager entityManager, TransactionContext txnContext) throws ServiceException {

        HandlingUnitMovement handlingUnitMvmt = new HandlingUnitMovement();
        handlingUnitMvmt.setShipmentInstanceId(shpInstId);
        handlingUnitMvmt.setSequenceNbr(BigInteger.valueOf(seqNbr));
        handlingUnitMvmt.setMovementSequenceNbr(movementSequenceNumber);

        handlingUnitMvmt.setAuditInfo(auditInfo);

        handlingUnitMvmt.setMovementTypeCd(handlingUnitMovementTypeCd);

        handlingUnitMvmt.setMovementReportingSicCd(requestingSicCd);

        handlingUnitMvmt.setMovementDateTime(lastMvmtDateTime);

        handlingUnitMvmt.setTrailerInstanceId(trailerInstanceId != null ? trailerInstanceId : 0L);
        HandlingUnitMovementHelper.setDefaultValues(handlingUnitMvmt);

        ShmHandlingUnitMvmt shmHandlingUnitMvmt = new ShmHandlingUnitMvmt();
        DtoTransformer.toShmHandlingUnitMvmt(handlingUnitMvmt,
            shmHandlingUnitMvmt);
        DtoTransformer.setAuditInfo(shmHandlingUnitMvmt, auditInfo);
        shmHandlingUnitMvmtSubDAO.save(shmHandlingUnitMvmt, entityManager);
        shmHandlingUnitMvmtSubDAO.createDB2ShmHandlingUnitMvmt(shmHandlingUnitMvmt, db2EntityManager);
    }

    private ShmHazMat createShmHazMat(final HazMat hazMat, final long shpInstId, final long hmSeqNbr, AuditInfo auditInfo,
			EntityManager entityManager, TransactionContext txnContext) throws ServiceException {
		ShmHazMat shmHazMat = new ShmHazMat();
		shmHazMat.setId(new ShmHazMatPK());
		DtoTransformer.setAuditInfo(shmHazMat, auditInfo);
		DtoTransformer.toShmHazMat(hazMat, shmHazMat);
		shmHazMat.getId().setHmSeqNbr(hmSeqNbr);
		shmHazMat.getId().setShpInstId(shpInstId);

		if (StringUtils.isBlank(shmHazMat.getSourceCd())) {
			shmHazMat.setSourceCd(StringUtils.SPACE);
		}
		if (StringUtils.isBlank(shmHazMat.getZoneCd())) {
			shmHazMat.setZoneCd(StringUtils.SPACE);
		}
		if (StringUtils.isBlank(shmHazMat.getPackingGrpCd())) {
			shmHazMat.setPackingGrpCd(StringUtils.SPACE);
		}
		if (StringUtils.isBlank(shmHazMat.getClassLbl())) {
			shmHazMat.setClassLbl(StringUtils.SPACE);
		}
		if (StringUtils.isBlank(shmHazMat.getOvrdMethodNm())) {
			shmHazMat.setOvrdMethodNm(StringUtils.SPACE);
		}

        if (StringUtils.isBlank(shmHazMat.getHmResidueInd())) {
            shmHazMat.setHmResidueInd(DEFAULT_IND_VALUE);
        }

        shmHazMat.setSourceCd(HazmatSourceCdTransformer.toCode(HazmatSourceCd.DRIVER));

		ShmHazMat savedShmHazMat = shmHazMatSubDAO.save(shmHazMat, entityManager);
		shmHazMatSubDAO.insertDB2ShmHazMat(shmHazMat, db2EntityManager);

		return savedShmHazMat;
	}

	private void validateShmSkeleton(ShipmentSkeleton shipmentSkeleton, ShmShipment existentShmShipment, List<MoreInfo> moreInfoList,
			boolean onUpdate, XMLGregorianCalendar lastMovementDateTime,
			List<ShmHandlingUnit> shmHandlingUnitList, EntityManager entityManager, TransactionContext txnContext) throws ServiceException {

		if (Objects.isNull(moreInfoList)){
			moreInfoList = Lists.newArrayList();
		}
        if (existentShmShipment != null && DeliveryQualifierCdTransformer.toEnum(existentShmShipment.getDlvryQalfrCd()) == DeliveryQualifierCd.FINAL) {
            moreInfoList.add(createMoreInfo("shipmentSkeleton",
                "Cannot update existing shipment skeleton with delivery qualifier of Final Delivered."));
        }
		if (Objects.isNull(shipmentSkeleton)){
			moreInfoList.add(createMoreInfo("shipmentSkeleton",
					ValidationErrorMessage.SHIPMENT_SKELETON_REQUIRED.message()));
		} else {
			Double totalWeightLbs = shipmentSkeleton.getTotalWeightLbs();
			BigInteger totalPiecesCount = shipmentSkeleton.getTotalPiecesCount();
			BigInteger motorizedPiecesCount = shipmentSkeleton.getMotorizedPiecesCount();
			BigInteger totalPalletsCount = shipmentSkeleton.getTotalPalletsCount();
			BigInteger loosePiecesCount = shipmentSkeleton.getLoosePiecesCount();
            Double totalVolumeCubicFeet = shipmentSkeleton.getTotalVolumeCubicFeet();
            Double pupVolumePercentage = shipmentSkeleton.getPupVolumePercentage();

            if (Objects.isNull(totalVolumeCubicFeet) || Objects.isNull(pupVolumePercentage)) {
                moreInfoList
                    .add(createMoreInfo("totalVolumeCubicFeet or pupVolumePercentage",
                        ValidationErrorMessage.TOT_VOL_CFT_IS_REQUIRED.message()));
            } else if (pupVolumePercentage <= 0) {
                moreInfoList
                    .add(createMoreInfo("pupVolumePercentage",
                        ValidationErrorMessage.PUP_VOL_PCT_NOT_CALCULATED.message()));
            }

			if (Objects.isNull(totalWeightLbs)){
				moreInfoList.add(createMoreInfo("totalWeightLbs",
						ValidationErrorMessage.SHIPMENT_WEIGHT_IS_REQUIRED.message()));
			} else if (totalWeightLbs <= 0) {
				moreInfoList.add(createMoreInfo("totalWeightLbs",
						ValidationErrorMessage.SHPMT_WEIGHT_GREATER_ZERO.message()));
			}

			if (Objects.isNull(totalPiecesCount)){
				moreInfoList.add(createMoreInfo("totalPiecesCount",
						ValidationErrorMessage.PCS_CNT_MUST_BE_ENTD.message()));
			} else if (totalPiecesCount.compareTo(BigInteger.ZERO) <= 0) {
				moreInfoList.add(createMoreInfo("totalPiecesCount",
						ValidationErrorMessage.SHPMT_TOT_PIECES_GREATER_ZERO.message()));
			}

			//pickup sends motorized piece cnt
            if (Objects.isNull(motorizedPiecesCount)) {
                moreInfoList
                    .add(createMoreInfo("motorizedPiecesCount",
                        ValidationErrorMessage.MOTORIZED_PIECES_COUNT_REQ.message()));
            } else if (motorizedPiecesCount.compareTo(BigInteger.ZERO) < 0) {
                moreInfoList
                    .add(createMoreInfo("motorizedPiecesCount",
                        ValidationErrorMessage.MPIECES_COUNT_GREATER_ZERO.message()));
            }

            if (totalPalletsCount != null && totalPalletsCount.compareTo(BigInteger.ZERO) < 0) {
				moreInfoList.add(createMoreInfo("totalPalletsCount",
						ValidationErrorMessage.SHM_TOTAL_PALLET_GREATER_ZERO.message()));
			}

			if (Objects.isNull(loosePiecesCount)){
				moreInfoList.add(createMoreInfo("loosePiecesCount",
						ValidationErrorMessage.LPIECES_COUNT_REQUIRED.message()));
			} else if (loosePiecesCount.compareTo(BigInteger.ZERO) < 0) {
				moreInfoList.add(createMoreInfo("loosePiecesCount",
						ValidationErrorMessage.LPIECES_COUNT_GREATER_ZERO.message()));
			}

			if (Objects.isNull(shipmentSkeleton.getHazmatInd())){
				moreInfoList.add(createMoreInfo("hazmatInd",
						ValidationErrorMessage.HAZMAT_IND_REQUIRED.message()));
			}

			if (Objects.isNull(shipmentSkeleton.getFreezableInd())){
				moreInfoList.add(createMoreInfo("freezableInd",
						ValidationErrorMessage.FREEZABLE_IND_REQUIRED.message()));
			}

			if (Objects.isNull(shipmentSkeleton.getGuaranteedInd())){
				moreInfoList.add(createMoreInfo("guaranteedInd",
						ValidationErrorMessage.GUAR_IND_REQUIRED.message()));
			}

			if (Objects.isNull(shipmentSkeleton.getFoodPoisonCd())){
				moreInfoList.add(createMoreInfo("foodPoisonCd",
						ValidationErrorMessage.FOOD_POISON_CODE_INVALID.message()));
			}

			if (Objects.isNull(shipmentSkeleton.getBulkLiquidInd())){
				moreInfoList.add(createMoreInfo("bulkLiquidInd",
						ValidationErrorMessage.BULK_IND_REQUIRED.message()));
			}

			String requestingSicCd = shipmentSkeleton.getRequestingSicCd();

			if (StringUtils.isBlank(requestingSicCd)) {
				moreInfoList.add(createMoreInfo("requestingSicCd",
						ValidationErrorMessage.CURRENT_SIC_RQ.message()));
            } else if (!restClient.isActiveSicAndLinehaul(requestingSicCd, txnContext)) {
				moreInfoList.add(createMoreInfo("requestingSicCd",
						ValidationErrorMessage.SIC_NOT_ACTIVE.message()));
			}

			String parentProNbr = shipmentSkeleton.getParentProNbr();
            List<HandlingUnit> handlingUnits = CollectionUtils
                .emptyIfNull(shipmentSkeleton.getHandlingUnits())
                .stream()
                .collect(Collectors.toList());
            List<LnhDimension> lnhDimensions = shipmentSkeleton.getDimensions();
			int handlingUnitsCount = 0;
			if (CollectionUtils.isNotEmpty(handlingUnits)){
				for (HandlingUnit handlingUnit : handlingUnits){

				    if(!isAstrayHUMovCd(handlingUnit)){
				        handlingUnitsCount++;
				    }

					if (StringUtils.isBlank(handlingUnit.getParentProNbr())){
						handlingUnit.setParentProNbr(parentProNbr);
					} else if (!parentProNbr.equals(ProNumberHelper.toElevenDigitPro(
							handlingUnit.getParentProNbr(), txnContext))) {
						moreInfoList.add(createMoreInfo("handlingUnit.parentProNbr",
								ValidationErrorMessage.HU_SAME_PARENT_PRO.message()
								+ " " + handlingUnit.getParentProNbr()));
					}

                    String childProNumber = handlingUnit.getChildProNbr();
					if (!ProNumberHelper.isYellowPro(childProNumber)) {
						moreInfoList.add(createMoreInfo("childProNbr",
								ValidationErrorMessage.PRO_NBR_FORMAT_INVALID.message() +
										ValidationErrorMessage.YELLOW_PRO_FORMAT.message()
										+ " " + childProNumber));

					} else {
					    //TODO PLT - Need to handle when ASTRAY is part of parent. createOrUpdateAstrayHandlingUnit will be refactored to cater the upsert of ASTRAY. This is future state.
						ShmHandlingUnit shmHandlingUnit = null;
						for(ShmHandlingUnit shmHU: CollectionUtils.emptyIfNull(shmHandlingUnitList)) {
							if(null != shmHU.getChildProNbrTxt() && StringUtils.equals(shmHU.getChildProNbrTxt(), childProNumber)) {
								shmHandlingUnit = shmHU;
							}
						}
								
                        if (Objects.nonNull(shmHandlingUnit)) {
                            String parentProNbrHU = ProNumberHelper.toElevenDigitPro(shmHandlingUnit.getParentProNbrTxt(), txnContext);
                            String parentProNbrEleventDigits =  ProNumberHelper.toElevenDigitPro(parentProNbr, txnContext);
                            if (!onUpdate || (!parentProNbrHU.equals(parentProNbrEleventDigits))) { // LDOC-1517 if the handling unit already exists for this shipment the exception won't be throwed, the hu will be updated instead
                                moreInfoList.add(createMoreInfo("handlingUnit",
                                		ValidationErrorMessage.CHILD_PRO_ALREADY_USED.message(ProNumberHelper.toTenDigitPro(childProNumber),
                                				ProNumberHelper.toNineDigitProHyphen(parentProNbrHU, txnContext),
                                				ProNumberHelper.toNineDigitProHyphen(parentProNbrEleventDigits, txnContext))));

                            }
                        }
						handlingUnit.setChildProNbr(childProNumber);

					}
					List<HandlingUnitMovement> handlingUnitMovements =
							handlingUnit.getHandlingUnitMovement();
					if (CollectionUtils.isNotEmpty(handlingUnitMovements)) {
						for (HandlingUnitMovement handlingUnitMovement : handlingUnitMovements) {
							validateHandlingUnitMvmt(handlingUnitMovement, requestingSicCd,
									lastMovementDateTime, moreInfoList);
						}
					}
				}

	            if (Objects.nonNull(loosePiecesCount) && Objects.nonNull(motorizedPiecesCount) &&
                        (loosePiecesCount.add(motorizedPiecesCount).intValueExact() != handlingUnitsCount)) {
	                moreInfoList.add(createMoreInfo("handlingUnits",
							ValidationErrorMessage.TOTAL_NUMBER_HU.message()));
	            }

			}

            if (CollectionUtils.isEmpty(handlingUnits) && CollectionUtils.isEmpty(lnhDimensions)) {
                if(Objects.isNull(shipmentSkeleton.getPupVolumePercentage()) && Objects.isNull(shipmentSkeleton.getTotalVolumeCubicFeet())) {
                    moreInfoList.add(
                        createMoreInfo("handlingUnits",
                            ValidationErrorMessage.HU_OR_DIM_REQUIRED.message()));
                }
            }

			if (CollectionUtils.isNotEmpty(shipmentSkeleton.getHazmatGroups())) {
				for (HazMat hazMat : shipmentSkeleton.getHazmatGroups()) {
					validateHazMat(hazMat, moreInfoList);
				}
			}
		}
	}

	private void validateHazMat(HazMat hazMat, List<MoreInfo> moreInfos) {
		if (Objects.isNull(hazMat.getHazmatWeightLbs()) || hazMat.getHazmatWeightLbs() <= 0) {
			moreInfos.add(createMoreInfo("hazmatWeightLbs",
					ValidationErrorMessage.HAZ_MAT_WEIGHT_REQD.message()));
		}
		if (StringUtils.isBlank(hazMat.getHazmatUnna())) {
			moreInfos.add(createMoreInfo("hazmatUnna",
					ValidationErrorMessage.HAZ_MAT_UNNA_CODE_REQD.message()));
		}
		if (StringUtils.isBlank(hazMat.getHazmatClassCd())) {
			moreInfos.add(createMoreInfo("hazmatClassCd",
					ValidationErrorMessage.HAZ_MAT_HAZARD_CLASS_CD_REQD.message()));
		}
		if (!VALID_BULK_QUANTITY_CODES.contains(hazMat.getHazmatBulkQuantityCd())) {
			moreInfos.add(createMoreInfo("hazmatBulkQuantityCd",
					ValidationErrorMessage.HAZ_MAT_BULK_QTY_CD_INV.message()));
		}
		if (Objects.isNull(hazMat.getHazmatResidueInd())) {
			moreInfos.add(createMoreInfo("hazmatResidueInd",
					ValidationErrorMessage.HAZ_MAT_RESIDUE_IND_INV.message()));
		}
	}

	private void validateHandlingUnitMvmt(HandlingUnitMovement handlingUnitMovement,
			String requestingSicCd, XMLGregorianCalendar lastMvmtDateTime,
			List<MoreInfo> moreInfos) {

		String movementReportingSicCd = handlingUnitMovement.getMovementReportingSicCd();
		XMLGregorianCalendar movementDateTime = handlingUnitMovement.getMovementDateTime();
		if (Objects.isNull(handlingUnitMovement.getMovementTypeCd()) ) {
			moreInfos.add(createMoreInfo("handlingUnitMovement.movementTypeCd",
					ValidationErrorMessage.HU_MVMT_TYPE_REQUIRED.message()));
		}

		if (Objects.nonNull(movementReportingSicCd) && !movementReportingSicCd.equals(requestingSicCd)) {
			moreInfos.add(createMoreInfo("handlingUnitMovement.movementReportingSicCd",
					ValidationErrorMessage.HU_MVMT_SIC.message()));
		}

		if (Objects.nonNull(movementDateTime) && (movementDateTime.compare(lastMvmtDateTime) != 0)) {
			moreInfos.add(createMoreInfo("handlingUnitMovement.movementDateTime",
					ValidationErrorMessage.HU_MVMT_DATE_TIME.message()));
		}
	}

    private ShmShipment createShmShipment(ShipmentSkeleton shipmentSkeleton, String destSicCd, String cftPrflMthdCd,
        String cftPrflTypeCd, ServiceTypeCd serviceTypeCd, Timestamp lastMvmtTimestamp, boolean isPickup,
        AuditInfo auditInfo, TransactionContext txnContext, EntityManager entityManager) throws ServiceException {

        //validate pro nbr.
        validateProNbr(shipmentSkeleton.getParentProNbr(), txnContext, entityManager);

		ShmShipment shmShipment = new ShmShipment();
        String lateTenderCd = isPickup ? LateTenderCdTransformer.toCode(shipmentSkeleton.getLateTenderCd()) :
            DEFAULT_IND_VALUE;
        shmShipment.setLateTenderCd(lateTenderCd != null ? lateTenderCd : DEFAULT_IND_VALUE);
		shmShipment.setProNbrTxt(shipmentSkeleton.getParentProNbr());
		shmShipment.setBillClassCd(BillClassCdTransformer.toCode(BillClassCd.NORMAL_MVMT));
		shmShipment.setBillStatCd(BillStatusCdTransformer.toCode(BillStatusCd.UNBILLED));
		shmShipment.setOrigTrmnlSicCd(shipmentSkeleton.getRequestingSicCd());
		shmShipment.setDestTrmnlSicCd(destSicCd);
		shmShipment.setCurrSicCd(shipmentSkeleton.getRequestingSicCd());

        shmShipment.setShpSvcStatCd(StringUtils.SPACE);
        shmShipment.setSvcCalcStatCd(StringUtils.SPACE);

        if (isPickup) {
            lastMvmtTimestamp = TimestampUtil.isLowTimestamp(lastMvmtTimestamp) ? Timestamp.from(Instant.now()) :
                lastMvmtTimestamp;
            shmShipment.setSrceCd(ShipmentSourceCdTransformer.toCode(ShipmentSourceCd.PKUP_REQUEST));
            shmShipment.setPkupDt(lastMvmtTimestamp);
        } else {
            shmShipment.setSrceCd(ShipmentSourceCdTransformer.toCode(ShipmentSourceCd.TCON_OR_LOAD));
            shmShipment.setPkupDt(TimestampUtil.getLowTimestamp());
        }

        shmShipment.setReadyTm(BasicTransformer.toTime(ShipmentSkeletonHelper.DEFAULT_START_TIME));
        shmShipment
            .setTotPlltCnt((shipmentSkeleton.getTotalPalletsCount() == null) ? BigDecimal.ZERO :
                new BigDecimal(shipmentSkeleton.getTotalPalletsCount()));
		shmShipment.setTotPcsCnt(new BigDecimal(shipmentSkeleton.getTotalPiecesCount()));
		shmShipment.setMtrzdPcsCnt(new BigDecimal(shipmentSkeleton.getMotorizedPiecesCount()));
		shmShipment.setLoosePcsCnt(new BigDecimal(shipmentSkeleton.getLoosePiecesCount()));
		shmShipment.setTotWgtLbs(new BigDecimal(shipmentSkeleton.getTotalWeightLbs()));
		shmShipment.setSplitInd(DEFAULT_IND_VALUE);
		shmShipment.setHazmatInd(BasicTransformer.toString(shipmentSkeleton.getHazmatInd()));
		shmShipment.setFrzbleInd(BasicTransformer.toString(shipmentSkeleton.getFreezableInd()));
        shmShipment.setGarntdInd(shipmentSkeleton.getGuaranteedInd() != null ?
            BasicTransformer.toString(shipmentSkeleton.getGuaranteedInd()) :
            DEFAULT_IND_VALUE);
		shmShipment.setFoodPoisonCd(FoodPoisonCdTransformer.toCode(shipmentSkeleton.getFoodPoisonCd()));
		shmShipment.setSigSvcInd(DEFAULT_IND_VALUE);
		shmShipment.setRevBillInd(DEFAULT_IND_VALUE);
		shmShipment.setManlRtgRqrdInd(DEFAULT_IND_VALUE);
		shmShipment.setAudtInd(DEFAULT_IND_VALUE);
		shmShipment.setCashInd(DEFAULT_IND_VALUE);
		shmShipment.setCashCollInd(DEFAULT_IND_VALUE);
		shmShipment.setGblTrfcInd(DEFAULT_IND_VALUE);
		shmShipment.setGarntdCpnInd(DEFAULT_IND_VALUE);
		shmShipment.setSvcTypCd(ServiceTypeCdTransformer.toCode(serviceTypeCd));
        shmShipment.setSvcStrtDt(TimestampUtil.getLowTimestamp());
		shmShipment.setPrgBlkInd(DEFAULT_IND_VALUE);
		shmShipment.setArchiveInd(DEFAULT_IND_VALUE);
		shmShipment.setCodInd(DEFAULT_IND_VALUE);
		shmShipment.setLstMvRptgSicCd(shipmentSkeleton.getLastMoveRptgSicCd() != null
				? shipmentSkeleton.getLastMoveRptgSicCd()
            : shipmentSkeleton.getRequestingSicCd());
		shmShipment.setLstMvmtTmst(lastMvmtTimestamp);
		shmShipment.setMvmtStatCd(MovementStatusCdTransformer.toCode(MovementStatusCd.ON_DOCK));
        shmShipment.setReqrDlvrDt(BasicTransformer.toDate(ShipmentSkeletonHelper.DEFAULT_REQ_DELIVER_DATE));
        shmShipment.setStrtDlvrTm(BasicTransformer.toTime(ShipmentSkeletonHelper.DEFAULT_START_TIME));
        shmShipment.setEndDlvrTm(BasicTransformer.toTime(ShipmentSkeletonHelper.DEFAULT_END_TIME));
        shmShipment.setClsTm(BasicTransformer.toTime(ShipmentSkeletonHelper.DEFAULT_END_TIME));
		shmShipment.setWarrantyInd(DEFAULT_IND_VALUE);
		shmShipment.setWarrantyStatCd(WarrantyStatusCdTransformer.toCode(WarrantyStatusCd.NONE));
        shmShipment.setTotVolCft(BasicTransformer.toBigDecimal(shipmentSkeleton.getTotalVolumeCubicFeet()));
        shmShipment.setPupVolPct(BasicTransformer.toBigDecimal(shipmentSkeleton.getPupVolumePercentage()));
        shmShipment.setBulkLqdInd(shipmentSkeleton.getBulkLiquidInd() != null ?
            BasicTransformer.toString(shipmentSkeleton.getBulkLiquidInd()) :
            DEFAULT_IND_VALUE);
        shmShipment.setMtrzdPcsKnwnInd(BasicTransformer.toString(Boolean.TRUE));
        shmShipment.setSingleShpmtAcqrInd(shipmentSkeleton.getSingleShipmentAcquiredInd() != null ?
            BasicTransformer.toString(shipmentSkeleton.getSingleShipmentAcquiredInd()) :
            DEFAULT_IND_VALUE);
		shmShipment.setCalcMvmtSeqNbr(BigDecimal.ZERO);
		shmShipment.setCallForApptInd(DEFAULT_IND_VALUE);
		shmShipment.setMxDoorToDoorInd(DEFAULT_IND_VALUE);
		shmShipment.setExcessiveValueInd(DEFAULT_IND_VALUE);
        shmShipment.setCftPrflMthdCd(cftPrflMthdCd);
        shmShipment.setCftPrflTypeCd(cftPrflTypeCd);
        shmShipment.setShpmtAcqrTypCd(shipmentSkeleton.getShipmentAcquiredTypeCd() != null ?
            ShipmentAcquiredTypeCdTransformer.toCode(shipmentSkeleton.getShipmentAcquiredTypeCd()) :
            StringUtils.SPACE);
		shmShipment.setInspectedInd(DEFAULT_IND_VALUE);
		shmShipment.setRtgOvrdCd(StringUtils.SPACE);
		shmShipment.setPkupBackdateInd(StringUtils.SPACE);
		shmShipment.setExemptRsnCd(StringUtils.SPACE);
		shmShipment.setRtePfxTxt(StringUtils.SPACE);
		shmShipment.setRteSfxTxt(StringUtils.SPACE);
		shmShipment.setRteTypCd(StringUtils.SPACE);
		shmShipment.setAbsMinChgInd(DEFAULT_IND_VALUE);
        shmShipment.setNtfictnCd(StringUtils.SPACE);
		shmShipment.setDlvrSigNmTxt(StringUtils.SPACE);
		shmShipment.setDlvrInfoRqdCd(StringUtils.SPACE);
		shmShipment.setArchiveCntlCd(StringUtils.SPACE);
		shmShipment.setRtOrRtAudtqNm(StringUtils.SPACE);
		shmShipment.setRtAudtrInit(StringUtils.SPACE);
		shmShipment.setRtgTrfId(StringUtils.SPACE);
		shmShipment.setObcPkpDlvRteCd(StringUtils.SPACE);
		shmShipment.setToPortCd(StringUtils.SPACE);
		shmShipment.setFromPortCd(StringUtils.SPACE);
		shmShipment.setInvcCrncd(StringUtils.SPACE);
		shmShipment.setRtgCrncd(StringUtils.SPACE);
		shmShipment.setDlvryQalfrCd(StringUtils.SPACE);
		shmShipment.setDiscCd(StringUtils.SPACE);
		shmShipment.setLstMovrProTxt(StringUtils.SPACE);
		shmShipment.setDfltTrfId(StringUtils.SPACE);
		shmShipment.setChrgToCd(StringUtils.SPACE);
		shmShipment.setTotChrgAmt(ZERO);
		shmShipment.setTotUsdAmt(ZERO);
		shmShipment.setCrncyConvFctr(ZERO);
		shmShipment.setCustProfInstId(ZERO);
		shmShipment.setHviestCmdySeqNo(ZERO);
		shmShipment.setFbdsPrintCnt(ZERO);
		shmShipment.setDlvrSigTmst(TimestampUtil.getLowTimestamp());
		shmShipment.setStdTrnstDays(ZERO);
		shmShipment.setActlTrnstDays(ZERO);
		shmShipment.setTrnstMvmtSeqNbr(ZERO);
		shmShipment.setDiscPct(ZERO);
		shmShipment.setPrcAgrmtId(ZERO);
		shmShipment.setPrcRulesetNbr(ZERO);
		shmShipment.setAreaInstId(ZERO);
		shmShipment.setAutoRateableInd(StringUtils.SPACE);
		shmShipment.setEstTrnstDays(ZERO);
        shmShipment.setEstimatedDlvrDt(TimestampUtil.getLowTimestamp());
		shmShipment.setCalcSvcDays(ZERO);
		shmShipment.setCalcSvcTmst(TimestampUtil.getLowTimestamp());
		shmShipment.setDiffCalcDays(ZERO);
		shmShipment.setDiffTrnstDays(ZERO);
		shmShipment.setSpotQuoteId(ZERO);
		shmShipment.setShprToConsMiles(ZERO);
        String shprLdTrlrCd = ShipperLoadedTrailerCdTransformer.toCode(shipmentSkeleton.getShipperLoadedTrlrCd());
        shmShipment.setShprLdTrlrCd(shprLdTrlrCd != null ? shprLdTrlrCd : DEFAULT_IND_VALUE);
		shmShipment.setConsUnldTrlrCd(DEFAULT_IND_VALUE);
		shmShipment.setLinealFootTotalNbr(ZERO);
		shmShipment.setPurInstId(ZERO);
		shmShipment.setExclusiveUseInd(DEFAULT_IND_VALUE);
		shmShipment.setDeclaredValueAmt(ZERO);
		shmShipment.setPkupTm(BasicTransformer.toTime(TimestampUtil.getLowTimestamp()));
        shmShipment
            .setHandlingUnitExemptionInd(BasicTransformer.toString(shipmentSkeleton.getHandlingUnitExemptionInd()));
        shmShipment.setHandlingUnitExemptionRsn(shipmentSkeleton.getHandlingUnitExemptionReason());
        if (StringUtils.isBlank(shmShipment.getHandlingUnitExemptionInd())) {
            shmShipment.setHandlingUnitExemptionInd(DEFAULT_IND_VALUE);
        }
        if (Objects.isNull(shmShipment.getHandlingUnitExemptionRsn())) {
            shmShipment.setHandlingUnitExemptionRsn(StringUtils.SPACE);
        }
		shmShipment.setDebtorTermFlipInd(DEFAULT_IND_VALUE);
        shmShipment.setApptRqrdInd(shipmentSkeleton.getAppointmentRequiredInd() != null ?
            BasicTransformer.toString(shipmentSkeleton.getAppointmentRequiredInd()) :
            DEFAULT_IND_VALUE);
		shmShipment.setDestNtfyInd(DEFAULT_IND_VALUE);
        shmShipment.setPoorlyPackagedInd(this.getShipmentPoorlyPackagedInd(shipmentSkeleton));
        shmShipment.setHandlingUnitPartialInd(DEFAULT_IND_VALUE);
        shmShipment.setHandlingUnitSplitInd(DEFAULT_IND_VALUE);
		DtoTransformer.setAuditInfo(shmShipment, auditInfo);
		return shmShipment;
	}

    private void validateProNbr(String proNbr, TransactionContext txnContext, EntityManager entityManager)
            throws ServiceException, ValidationException {
        GetProStatusResp getProStatusResp = getProStatusCdImpl.getProStatus(proNbr, txnContext, entityManager);
        if (PRO_FRT_IN_USE_ENUM_LIST.contains(getProStatusResp.getProStatusCd())) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.PRO_NUMBER_ALREADY_BILLED, txnContext)
                .moreInfo("parentProNbr", String.format("PRO %s is already in use", proNbr))
                .build();
        }
        GetProStatusResp getProStatusRespDB2 = getProStatusCdImpl.getProStatusDB2(proNbr, txnContext, db2EntityManager);
        if (PRO_FRT_IN_USE_ENUM_LIST.contains(getProStatusRespDB2.getProStatusCd())) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.PRO_NUMBER_ALREADY_BILLED, txnContext)
                .moreInfo("parentProNbr", String.format("PRO %s is already in use", proNbr))
                .build();
        }
        
        if (PRO_FRT_UNAVAILABLE_ENUM_LIST.contains(getProStatusResp.getProStatusCd())) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.PRO_BILLED_PURGED_OR_VOID, txnContext)
                .moreInfo("parentProNbr", String.format("PRO %s is not available for use.", proNbr))
                .build();
        }
    }

	private void updateShmShipment(ShipmentSkeleton shipmentSkeleton, ShmShipment shmShipment,
			String destSicCd, String cftPrflMthdCd, String cftPrflTypeCd,
			ServiceTypeCd serviceTypeCd, Timestamp lastMvmtDateTime, boolean isPickup,
        AuditInfo auditInfo, TransactionContext txnContext) {

        if (isPickup) {
            String lateTenderCd = LateTenderCdTransformer.toCode(shipmentSkeleton.getLateTenderCd());
            if (StringUtils.isNotEmpty(lateTenderCd)) {
                shmShipment.setLateTenderCd(lateTenderCd);
            }
            String shpmtAcqrTypCd = ShipmentAcquiredTypeCdTransformer.toCode(shipmentSkeleton.getShipmentAcquiredTypeCd());
            if (StringUtils.isNotEmpty(shpmtAcqrTypCd)) {
                shmShipment.setShpmtAcqrTypCd(shpmtAcqrTypCd);
            }
            String shprLdTrlrCd = ShipperLoadedTrailerCdTransformer.toCode(shipmentSkeleton.getShipperLoadedTrlrCd());
            if (StringUtils.isNotEmpty(shprLdTrlrCd)) {
                shmShipment.setShprLdTrlrCd(shprLdTrlrCd);
            }
            if (Objects.nonNull(shipmentSkeleton.getSingleShipmentAcquiredInd())) {
                shmShipment.setSingleShpmtAcqrInd(BasicTransformer.toString(shipmentSkeleton.getSingleShipmentAcquiredInd()));
            }
        }

        String svcTypeCd = ServiceTypeCdTransformer.toCode(serviceTypeCd);
        if (StringUtils.isNotEmpty(svcTypeCd)) {
            shmShipment.setSvcTypCd(svcTypeCd);
        }

        shmShipment.setOrigTrmnlSicCd(shipmentSkeleton.getRequestingSicCd());
		shmShipment.setDestTrmnlSicCd(destSicCd);
		shmShipment.setCurrSicCd(shipmentSkeleton.getRequestingSicCd());
		shmShipment.setTotPcsCnt(new BigDecimal(shipmentSkeleton.getTotalPiecesCount()));
        shmShipment
            .setTotPlltCnt((shipmentSkeleton.getTotalPalletsCount() == null) ? shmShipment.getTotPlltCnt() :
                new BigDecimal(shipmentSkeleton.getTotalPalletsCount()));
		shmShipment.setTotWgtLbs(new BigDecimal(shipmentSkeleton.getTotalWeightLbs()));
		shmShipment.setMtrzdPcsCnt(new BigDecimal(shipmentSkeleton.getMotorizedPiecesCount()));
		shmShipment.setLoosePcsCnt(new BigDecimal(shipmentSkeleton.getLoosePiecesCount()));
        shmShipment.setHazmatInd(BasicTransformer.toString(
            shipmentSkeleton.getHazmatInd() || CollectionUtils.isNotEmpty(shipmentSkeleton.getHazmatGroups())));
		shmShipment.setFrzbleInd(BasicTransformer.toString(shipmentSkeleton.getFreezableInd()));
		shmShipment.setGarntdInd(BasicTransformer.toString(shipmentSkeleton.getGuaranteedInd()));
		shmShipment.setFoodPoisonCd(FoodPoisonCdTransformer.toCode(shipmentSkeleton.getFoodPoisonCd()));
		shmShipment.setLstMvRptgSicCd(shipmentSkeleton.getLastMoveRptgSicCd() != null
				? shipmentSkeleton.getLastMoveRptgSicCd()
				: StringUtils.SPACE);
		shmShipment.setLstMvmtTmst(lastMvmtDateTime);
        shmShipment.setTotVolCft(BasicTransformer.toBigDecimal(shipmentSkeleton.getTotalVolumeCubicFeet()));
        shmShipment.setPupVolPct(BasicTransformer.toBigDecimal(shipmentSkeleton.getPupVolumePercentage()));
        shmShipment.setCftPrflMthdCd(cftPrflMthdCd);
        shmShipment.setCftPrflTypeCd(cftPrflTypeCd);
		shmShipment.setBulkLqdInd(BasicTransformer.toString(shipmentSkeleton.getBulkLiquidInd()));
        shmShipment.setMtrzdPcsKnwnInd(BasicTransformer.toString(Boolean.TRUE));
        shmShipment
            .setHandlingUnitExemptionInd(BasicTransformer.toString(shipmentSkeleton.getHandlingUnitExemptionInd()));
        shmShipment.setHandlingUnitExemptionRsn(shipmentSkeleton.getHandlingUnitExemptionReason());
        shmShipment.setPoorlyPackagedInd(this.getShipmentPoorlyPackagedInd(shipmentSkeleton));
		if (StringUtils.isBlank(shmShipment.getHandlingUnitExemptionInd())) {
			shmShipment.setHandlingUnitExemptionInd(DEFAULT_IND_VALUE);
		}
		if (Objects.isNull(shmShipment.getHandlingUnitExemptionRsn())) {
			shmShipment.setHandlingUnitExemptionRsn(StringUtils.SPACE);
		}
		if (StringUtils.isBlank(shmShipment.getDebtorTermFlipInd())) {
			shmShipment.setDebtorTermFlipInd(DEFAULT_IND_VALUE);
		}
        if (shipmentSkeleton.getAppointmentRequiredInd() != null) {
            shmShipment.setApptRqrdInd(BasicTransformer.toString(shipmentSkeleton.getAppointmentRequiredInd()));
        }

		DtoTransformer.setAuditInfo(shmShipment, auditInfo);
	}

    /**
     * <p>
     * <b>PLT PRO (HUs List is not empty):</b> check if any HandlingUnit has poorlyPackagedInd in TRUE, return true.
     * Otherwise, false.
     * </p>
     * <p>
     * <b>Legacy PRO (HUs List is empty):</b> return the value received in the shipment skeleton.
     * </p>
     *
     * @return
     */
    private String getShipmentPoorlyPackagedInd(ShipmentSkeleton shipmentSkeleton) {
        List<HandlingUnit> reqHandlingUnitList = shipmentSkeleton.getHandlingUnits();

        if (CollectionUtils.isEmpty(reqHandlingUnitList)) {
            return ObjectUtils.isEmpty(shipmentSkeleton.getPoorlyPackagedInd()) ? BasicTransformer.toString(false) :
                BasicTransformer.toString(shipmentSkeleton.getPoorlyPackagedInd());
        }

        return BasicTransformer.toString(reqHandlingUnitList.stream().filter(x -> x.getPoorlyPackagedInd() != null).anyMatch(HandlingUnit::getPoorlyPackagedInd));
    }

    private ShmEventLog createEventLog(EventLogTypeCd eventLogTypeCd, EventLogSubTypeCd eventLogSubTypeCd,
        Long shpInstId, String proNumber, String reportingSicCd,
			String destSicCd, Timestamp lastMovementDateTime, BigDecimal calcMvmtSeqNbr,
        BigDecimal totalPcsCount, Boolean hazmatInd, BigDecimal totalWeightLbs, BigDecimal shprCustNbr,
        Date pkupDt, Date estDlvrDt, String billStatusCd,
        AuditInfo auditInfo,
        EntityManager entityManager, TransactionContext txnContext) throws ValidationException {
		ShmEventLog shmEventLog = new ShmEventLog();
		ShmEventLogPK shmEventLogPK = new ShmEventLogPK();
		shmEventLogPK.setShpInstId(shpInstId);
        shmEventLogPK.setSeqNbr(shmEventLogSubDAO.getLastUsedSeqNbr(shpInstId, entityManager, db2EntityManager) + 1);
		shmEventLog.setId(shmEventLogPK);
        DtoTransformer.setAuditInfo(shmEventLog, auditInfo);
        shmEventLog.setTypCd(EventLogTypeCdTransformer.toCode(eventLogTypeCd));
        shmEventLog.setSubTypCd(EventLogSubTypeCdTransformer.toCode(eventLogSubTypeCd));
		shmEventLog.setProNbrTxt(proNumber);
		shmEventLog.setRptgSicCd(reportingSicCd);
		shmEventLog.setOrigTrmnlSicCd(reportingSicCd);
		shmEventLog.setDestTrmnlSicCd(destSicCd);
		shmEventLog.setOccurredTmst(lastMovementDateTime);
		shmEventLog.setTotPcsCnt(totalPcsCount);
		shmEventLog.setTotWgtLbs(totalWeightLbs);
		shmEventLog.setMvmtSeqNbr(calcMvmtSeqNbr);
		shmEventLog.setTranId(StringUtils.SPACE);
		shmEventLog.setPgmId(StringUtils.SPACE);
		shmEventLog.setAdminInstId(ZERO);
        shmEventLog.setAdminStatCd(billStatusCd);
		shmEventLog.setArchiveCntlCd(StringUtils.SPACE);
		shmEventLog.setBil21stCustNbr(ZERO);
		shmEventLog.setBil22ndCustNbr(ZERO);
		shmEventLog.setBillClassCd(BillClassCdTransformer.toCode(BillClassCd.NORMAL_MVMT));
		shmEventLog.setBilto1stRelCd(StringUtils.SPACE);
		shmEventLog.setBilto2ndRelCd(StringUtils.SPACE);
		shmEventLog.setBolInstId(ZERO);
		shmEventLog.setChrgToCd(StringUtils.SPACE);
		shmEventLog.setConsCntryCd(StringUtils.SPACE);
		shmEventLog.setConsCustNbr(ZERO);
		shmEventLog.setEdiSenderId(StringUtils.SPACE);
		shmEventLog.setEdiSenderShpId(StringUtils.SPACE);
		shmEventLog.setEnrouteInd(DEFAULT_IND_VALUE);
		shmEventLog.setEstimatedDlvrDt(Objects.nonNull(estDlvrDt) ? estDlvrDt : TimestampUtil.getLowTimestamp());
		shmEventLog.setGarntdInd(DEFAULT_IND_VALUE);
		shmEventLog.setHazmatInd(BasicTransformer.toString(hazmatInd));
		shmEventLog.setMvmtStatCd(MovementStatusCdTransformer.toCode(MovementStatusCd.ON_DOCK));
		shmEventLog.setPkupDt(Objects.nonNull(pkupDt) ? pkupDt : TimestampUtil.getLowTimestamp());
		shmEventLog.setPurInstId(ZERO);
        shmEventLog.setShprCustNbr(shprCustNbr != null ? shprCustNbr : ZERO);
		shmEventLog.setTotChrgAmt(ZERO);
		shmEventLog.setOccurredSicCd(reportingSicCd);
		shmEventLog.setParentInstId(ZERO);
		shmEventLog.setEnrouteInd(StringUtils.SPACE);
		shmEventLog.setShprCntryCd(StringUtils.SPACE);
		shmEventLog.setConsCntryCd(StringUtils.SPACE);
		shmEventLog.setDebtorCd(StringUtils.SPACE);
		shmEventLog.setThirdPartyInd(StringUtils.SPACE);
		shmEventLog.setCorrelationId(StringUtils.SPACE);
		shmEventLog.setTrlrIdPfxTxt(StringUtils.SPACE);
		shmEventLog.setTrlrIdSfxNbr(ZERO);

        ShmEventLog createdShmEventLog = shmEventLogSubDAO.create(shmEventLog, entityManager);
        shmEventLogSubDAO.createDB2ShmEventLog(createdShmEventLog, db2EntityManager);

        return createdShmEventLog;
	}

	private MoreInfo createMoreInfo(String location, String message) {
		MoreInfo moreInfo = new MoreInfo();
		moreInfo.setItemNbr(null);
		moreInfo.setMessage(message);
		moreInfo.setLocation(location);
		return moreInfo;
	}

    public void performLegacyUpdates(ShipmentSkeletonResponse shipmentSkeletonResponse, TransactionContext txnContext) throws ServiceException {

        boolean bypassBo11Call = BooleanUtils.isFalse(shipmentSkeletonResponse.getLateTenderCd() == LateTenderCd.LATE_TENDER ? true : false)
                && (shipmentSkeletonResponse.getBolInstId() == null || BigDecimal.ZERO.equals(shipmentSkeletonResponse.getBolInstId()));

        if (!bypassBo11Call) {
            maintainBolDocImpl.ediBolUpdate(shipmentSkeletonResponse,
                txnContext);
        }

        if (BooleanUtils.isTrue(shipmentSkeletonResponse.getLateTenderCd() == LateTenderCd.LATE_TENDER ? true : false)) {
            List<BaseLog> baseLogList = new ArrayList<>();
            BaseLog baseLog = new BaseLog();
            ShipmentId shmId = new ShipmentId();
            if (StringUtils.isNotEmpty(shipmentSkeletonResponse.getProNbr()))
                shmId.setProNumber(shipmentSkeletonResponse.getProNbr());
            baseLog.setShipmentId(shmId);
            baseLogList.add(baseLog);

            restClient.startCreateBaseLogChEnsemble
                (BaseLogTypeEnum.BASE_LOG_70, baseLogList, txnContext);
        }
    }

    private ProFrtBillIndex upsertProFrtBillIndexByPro(ShipmentSkeleton shipmentSkeleton, long shpInstId,
        AuditInfo auditInfo, EntityManager entityManager, TransactionContext txnContext) throws ServiceException {

		ProFrtBillIndex proFrtBillIndexEntity = proNbrFrtBillIndexDAO.findById(shipmentSkeleton.getParentProNbr(), entityManager);

		if(proFrtBillIndexEntity == null) {

			proFrtBillIndexEntity = new ProFrtBillIndex();
			proFrtBillIndexEntity.setProNbrTxt(shipmentSkeleton.getParentProNbr());
			proFrtBillIndexEntity.setShpInstId(new BigDecimal(shpInstId));
			proFrtBillIndexEntity.setStatCd("2");
			proFrtBillIndexEntity.setMvmtUnitSeqNbr(new BigDecimal(1));
			proFrtBillIndexEntity.setProPfxOvrdInd("N");
			proFrtBillIndexEntity.setBillSicCd(shipmentSkeleton.getRequestingSicCd());

			DtoTransformer.setAuditInfo(proFrtBillIndexEntity, auditInfo);

			proNbrFrtBillIndexDAO.save(proFrtBillIndexEntity, entityManager);
			proNbrFrtBillIndexDAO.createDB2ProFrtBillIndex(proFrtBillIndexEntity, db2EntityManager);

		}
		else {

			if(PRO_FRT_IN_USE.contains(proFrtBillIndexEntity.getStatCd()))
			{
				throw ExceptionBuilder
				.exception(ValidationErrorMessage.PRO_NUMBER_ALREADY_BILLED, txnContext)
				.moreInfo("parentProNbr",
                        String.format("PRO %s is already in use",
							shipmentSkeleton.getParentProNbr()))
					.build();
			}
			String previousStatusCd = proFrtBillIndexEntity.getStatCd();

			proFrtBillIndexEntity.setShpInstId(new BigDecimal(shpInstId));
			proFrtBillIndexEntity.setStatCd(PRO_FRT_IN_USE_SUSPENSE.equals(previousStatusCd) ? previousStatusCd : PRO_FRT_IN_USE_UNBILLED);
			proFrtBillIndexEntity.setMvmtUnitSeqNbr(new BigDecimal(1));
			proFrtBillIndexEntity.setProPfxOvrdInd("N");
			proFrtBillIndexEntity.setBillSicCd(shipmentSkeleton.getRequestingSicCd());

			DtoTransformer.setAuditInfo(proFrtBillIndexEntity, auditInfo);

			proNbrFrtBillIndexDAO.save(proFrtBillIndexEntity, entityManager);
            proNbrFrtBillIndexDAO.updateDB2ProNbrFrtBillIndexForUpdSkeleton(proFrtBillIndexEntity, db2EntityManager);

		}

		return proFrtBillIndexEntity;


	}

    public List<ShipmentSkeletonResponse> bulkCreateShipmentSkeleton(
        BulkCreateShipmentSkeletonRqst bulkCreateShipmentSkeletonRqst, TransactionContext txnContext,
        EntityManager entityManager) throws ValidationException, ServiceException {

        checkNotNull(bulkCreateShipmentSkeletonRqst, "The bulk shipment skeleton request is required.");

        List<ShipmentSkeletonResponse> response = new ArrayList<>();
        Map<String, ShmShipment> existentShmShipmentMap = new HashMap<>();

        ShipmentSkeletonHelper
        .populateShmShipmentMap(bulkCreateShipmentSkeletonRqst.getShipmentSkeletons(), null, existentShmShipmentMap, null,
            shipmentDAO, restClient, txnContext, entityManager);
        
        String[] desPostalCdArray = CollectionUtils.emptyIfNull(bulkCreateShipmentSkeletonRqst.getShipmentSkeletons()).stream().filter(skeleton -> StringUtils.isNotBlank(skeleton.getDestPostalCd())).distinct().map(skeleton -> skeleton.getDestPostalCd()).toArray(String[]::new);

        GetSicForPostalCodesResp sicForPostalCodes = !CollectionUtils.sizeIsEmpty(desPostalCdArray) ? restClient.getSicForPostalCodes(desPostalCdArray, txnContext) : null;

		for (ShipmentSkeleton shipmentSkeleton : CollectionUtils
            .emptyIfNull(bulkCreateShipmentSkeletonRqst.getShipmentSkeletons())) {

            response
                .add(createShipmentSkeleton(shipmentSkeleton, bulkCreateShipmentSkeletonRqst.getCarrierId(),
            		bulkCreateShipmentSkeletonRqst.getAuditInfo(), sicForPostalCodes, existentShmShipmentMap,
                    txnContext, entityManager));
        }
        return response;
    }

    public void bulkPerformLegacyUpdates(List<ShipmentSkeletonResponse> shipmentSkeletonResponseList,
        TransactionContext txnContext) throws ServiceException {

    	restClient.bulkPerformLegacyUpdates(shipmentSkeletonResponseList, maintainBolDocImpl, restClient, txnContext);

        }
}
