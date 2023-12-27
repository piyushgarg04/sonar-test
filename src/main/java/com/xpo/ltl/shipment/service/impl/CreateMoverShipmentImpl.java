package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dozer.DozerBeanMapper;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.location.v2.GetRefSicAddressResp;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ProFrtBillIndex;
import com.xpo.ltl.api.shipment.service.entity.ShmAsEntdCust;
import com.xpo.ltl.api.shipment.service.entity.ShmCmdyDimension;
import com.xpo.ltl.api.shipment.service.entity.ShmCommodity;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmt;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmtPK;
import com.xpo.ltl.api.shipment.service.entity.ShmRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmRemarkPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.service.entity.ShmSrNbr;
import com.xpo.ltl.api.shipment.transformer.v2.BillClassCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.BillStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.HandlingUnitMovementTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MatchedPartySourceCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MatchedPartyTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentRemarkTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.BillClassCd;
import com.xpo.ltl.api.shipment.v2.BillStatusCd;
import com.xpo.ltl.api.shipment.v2.Commodity;
import com.xpo.ltl.api.shipment.v2.CommodityDimension;
import com.xpo.ltl.api.shipment.v2.CreateMoverShipmentResp;
import com.xpo.ltl.api.shipment.v2.CreateMoverShipmentRqst;
import com.xpo.ltl.api.shipment.v2.EventLogSubTypeCd;
import com.xpo.ltl.api.shipment.v2.EventLogTypeCd;
import com.xpo.ltl.api.shipment.v2.GetProStatusResp;
import com.xpo.ltl.api.shipment.v2.HandlingUnitMovementTypeCd;
import com.xpo.ltl.api.shipment.v2.MatchedPartySourceCd;
import com.xpo.ltl.api.shipment.v2.MatchedPartyTypeCd;
import com.xpo.ltl.api.shipment.v2.MovementStatusCd;
import com.xpo.ltl.api.shipment.v2.ProStatusCd;
import com.xpo.ltl.api.shipment.v2.ShipmentRemarkTypeCd;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.java.util.cityoperations.ProNumber;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ProFrtBillIndexSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentAsEnteredCustomerDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentRemarkSubDAO;
import com.xpo.ltl.shipment.service.dao.ShipmentSupRefSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmCmdyDimensionSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmCommoditySubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitMvmtSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmEventDelegate;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.DB2DefaultValueUtil;
import com.xpo.ltl.shipment.service.util.TimestampUtil;


@RequestScoped
public class CreateMoverShipmentImpl {

	private static final String PRO_FRT_BILLED = "3";
    private static final String DEFAULT_MOVR_SHPR_NAME2 = "XPO LOGISTICS FREIGHT, INC.";
    private static final Log log = LogFactory.getLog(CreateMoverShipmentImpl.class);
	private static final String PGM_ID = "CRTEMOVR";
	private static final String TRAN_ID = "CRMV";
    private static final List<String> PRO_FRT_IN_USE = Lists.newArrayList(PRO_FRT_BILLED,"4","7");
    private static final List<ProStatusCd> PRO_FRT_IN_USE_ENUM_LIST = Lists
        .newArrayList(ProStatusCd.RECEIVED_PURGED_MAYBE_ON_FBA, ProStatusCd.VOIDED);

	private static final DozerBeanMapper mapper = new DozerBeanMapper(Collections.singletonList("shm-mover-mapper-config.xml"));

    @Inject
    private ProFrtBillIndexSubDAO proNbrFrtBillIndexDAO;

	@Inject ShmShipmentSubDAO shmShipmentSubDAO;

	@Inject ShmCommoditySubDAO shmCommoditySubDAO;

	@Inject ShipmentSupRefSubDAO shipmentSupRefSubDAO;

	@Inject ShipmentRemarkSubDAO shipmentRemarkSubDAO;

	@Inject ShmCmdyDimensionSubDAO shmCmdyDimensionSubDAO;

	@Inject ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

	@Inject ShmHandlingUnitMvmtSubDAO shmHandlingUnitMvmtSubDAO;

	@Inject ShipmentAsEnteredCustomerDAO shipmentAsEnteredCustomerDAO;

    @Inject
    private GetProStatusCdImpl getProStatusCdImpl;

    @Inject
    private ShmEventDelegate shmEventDelegate;

	@Inject ExternalRestClient externalRestClient;

	@PersistenceContext(unitName="ltl-java-db2-shipment-jaxrs")
	private EntityManager db2EntityManager;

	public CreateMoverShipmentResp createMoverShipment(CreateMoverShipmentRqst request, TransactionContext txnContext,
													   EntityManager entityManager) throws ServiceException, ValidationException {

		log.info("starting createMoverShipment");

		checkNotNull(request, "The request is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");

		validateRequest(request, txnContext);

		String parentPro = getAndValidateProNumber(request.getParentProNbr(), txnContext);
		String moverPro = getAndValidateProNumber(request.getMoverProNbr(), txnContext);

		log.info(String.format("createMoverShipment for parentPro: %s, moverPro: %s", parentPro, moverPro));

		//lookup parent shipment directly from Oracle
		ShmShipment parentShipment = shmShipmentSubDAO.findByProOrShipmentId(parentPro, null,
				null, false, null, entityManager);
		if (parentShipment == null) {
			throw ExceptionBuilder.exception(NotFoundErrorMessage.SHIPMENT_NOT_FOUND, txnContext).build();
		}

		List<String> parentChildPros = CollectionUtils.emptyIfNull(parentShipment.getShmHandlingUnits()).stream().map(ShmHandlingUnit::getChildProNbrTxt).collect(Collectors.toList());
		if(request.getChildProNbrs().containsAll(parentChildPros)){
			throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo("CreateMoverShipmentImpl","Cannot create a Mover for all handling units of a parent")
					.build();
		}

		if (parentShipment.getShmHandlingUnits().size() < 1){
			throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo("CreateMoverShipmentImpl","Cannot create a Mover from a parent with only one handling unit")
					.build();
		}
		

		if (shmShipmentSubDAO.findByProOrShipmentId(moverPro, null, null,
				false, null, entityManager) != null) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.MOVER_ALREADY_CREATED, txnContext)
					.moreInfo("CreateMoverShipmentImpl", "Mover Pro Number: " + moverPro )
					.build();
		}

		//create and persist child mover shipment
		AuditInfo auditInfo = AuditInfoHelper.getAuditInfoWithPgmId(PGM_ID, txnContext);

        ShmShipment childMovrShipment = createMover(request, moverPro, parentShipment, auditInfo, txnContext, entityManager);

		addCommodities(childMovrShipment, request, auditInfo, entityManager);

        addMoverParty(childMovrShipment, parentShipment, request, txnContext, auditInfo, entityManager);

		cloneSupplementalReferenceNumbers(childMovrShipment, parentShipment, entityManager);

		cloneRemarks(childMovrShipment, parentShipment, entityManager);

		addHandlingUnits(childMovrShipment, request, txnContext, auditInfo, entityManager);

		// add child mover pro to parent
		if (CollectionUtils.isEmpty(parentShipment.getShmShipments())){
			parentShipment.setShmShipments(new ArrayList<>());
		}
		parentShipment.addShmShipment(childMovrShipment);

		createDB2Mover(childMovrShipment);

		long createEventSeqNbr = shmEventDelegate
            .createEvent(0L, EventLogTypeCd.BILL_ENTRY, EventLogSubTypeCd.RATED_BILL, childMovrShipment, null, childMovrShipment.getOrigTrmnlSicCd(),
                Optional.empty(), TRAN_ID, entityManager, auditInfo);
		shmEventDelegate
            .createEvent(++createEventSeqNbr, EventLogTypeCd.CUSTOMER_MATCH, EventLogSubTypeCd.CUSTOMER_MATCH, childMovrShipment, null,
                childMovrShipment.getOrigTrmnlSicCd(), Optional.empty(), TRAN_ID, entityManager, auditInfo);

		upsertProFrtBillIndexByPro(childMovrShipment, auditInfo, entityManager, txnContext);

		log.info(String.format("finished createMoverShipment for parentPro: %s, moverPro: %s, moverInstId: %s", parentPro, moverPro, childMovrShipment.getShpInstId()));

		CreateMoverShipmentResp resp = new CreateMoverShipmentResp();
		resp.setMoverShipmentInstId(childMovrShipment.getShpInstId());
		resp.setParentProNbr(parentShipment.getProNbrTxt());
		resp.setMoverSuffix(childMovrShipment.getMovrSuffix());

		return resp;
	}

	private void createDB2Mover(ShmShipment childMovrShipment) {

		shmShipmentSubDAO.createDB2ShmShipment(childMovrShipment, childMovrShipment.getParentInstId(), db2EntityManager);

		childMovrShipment.getShmCommodities().forEach(shmCommodity -> {
			shmCommoditySubDAO.createDB2ShmCommodity(shmCommodity, db2EntityManager);
			shmCommodity.getShmCmdyDimensions().forEach(shmCmdyDimension -> shmCmdyDimensionSubDAO.createDB2ShmCmdyDimension(shmCmdyDimension, db2EntityManager));

		});

		childMovrShipment.getShmAsEntdCusts().forEach(asEntdCust -> shipmentAsEnteredCustomerDAO.createDB2ShmAsEntdCust(asEntdCust, db2EntityManager));

        childMovrShipment.getShmRemarks().forEach(shmRemark -> shipmentRemarkSubDAO.createDB2ShmRemark(shmRemark, db2EntityManager));

		if (CollectionUtils.isNotEmpty(childMovrShipment.getShmSrNbrs())) {
			childMovrShipment.getShmSrNbrs().forEach(shmSrNbr ->  shipmentSupRefSubDAO.createDB2ShmSrNbr(shmSrNbr, db2EntityManager));
		}

		if (CollectionUtils.isNotEmpty(childMovrShipment.getShmHandlingUnits())) {
			childMovrShipment.getShmHandlingUnits().forEach(shmHandlingUnit -> {
				shmHandlingUnitSubDAO.createDB2ShmHandlingUnit(shmHandlingUnit, db2EntityManager);
				shmHandlingUnit.getShmHandlingUnitMvmts().forEach(shmHandlingUnitMvmt -> shmHandlingUnitMvmtSubDAO.createDB2ShmHandlingUnitMvmt(shmHandlingUnitMvmt, db2EntityManager));
			});
		}

	}

	//Update Handling Units for every childPro supplied
	private void addHandlingUnits(ShmShipment childMovrShipment, CreateMoverShipmentRqst request, TransactionContext txnContext, AuditInfo auditInfo, EntityManager entityManager) throws ServiceException, ValidationException{
		if (CollectionUtils.isNotEmpty(request.getChildProNbrs())){
			//get childPro handling units
			List<ShmHandlingUnit> childHandlingUnits = shmHandlingUnitSubDAO.findByChildProNumberList(new HashSet<>(request.getChildProNbrs()), entityManager);
			//for each child
			for (ShmHandlingUnit childHandlingUnit : childHandlingUnits) {

				if(childHandlingUnit.getMovrProNbrTxt() != null){
					List<ShmShipment> shmShipments = shmShipmentSubDAO.findMovrShipmentsByProNbr(Arrays.asList(childHandlingUnit.getMovrProNbrTxt()), entityManager);
					if(shmShipments.size()< 1) {
						throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
								.moreInfo("CreateMoverShipmentImpl","Child Pro is already associated to a Mover")
								.build();
					}
				}

				//update child Handling Unit with Mover
				childHandlingUnit.setMovrSuffix(childMovrShipment.getMovrSuffix());
				childHandlingUnit.setMovrProNbrTxt(childMovrShipment.getProNbrTxt());
				DtoTransformer.setLstUpdateAuditInfo(childHandlingUnit, auditInfo);
				// Adding DB2 update of parent handling unit
				shmHandlingUnitSubDAO.updateDB2ShmHandlingUnit(childHandlingUnit, childHandlingUnit.getLstUpdtTmst(), txnContext, db2EntityManager);

				//create movrHandlingUnitMvmt
                ShmHandlingUnitMvmt movrHandlingUnitMvmt = createMoverHandlingUnitMvmt(childMovrShipment, childHandlingUnit, auditInfo, entityManager);

                childHandlingUnit.addShmHandlingUnitMvmt(movrHandlingUnitMvmt);

                shmHandlingUnitMvmtSubDAO.persist(movrHandlingUnitMvmt, entityManager);

                shmHandlingUnitMvmtSubDAO.createDB2ShmHandlingUnitMvmt(movrHandlingUnitMvmt, db2EntityManager);
			}
		}
	}

    private void addMoverParty(ShmShipment childMovrShipment, ShmShipment parentShipment, CreateMoverShipmentRqst request, TransactionContext txnContext, AuditInfo auditInfo,
        EntityManager entityManager) throws ServiceException {
		// Shipper Party
		ShmAsEntdCust shipperParty = getShipperParty(request, parentShipment, entityManager, childMovrShipment.getShpInstId(), auditInfo, txnContext);
		shipmentAsEnteredCustomerDAO.persist(shipperParty, entityManager);

		ShmAsEntdCust consigneeParty = getConsigneeParty(parentShipment, entityManager, auditInfo, childMovrShipment.getShpInstId());
		shipmentAsEnteredCustomerDAO.persist(consigneeParty, entityManager);

		List<ShmAsEntdCust> moverMatchedParty = new ArrayList<>();
		moverMatchedParty.add(shipperParty);
		moverMatchedParty.add(consigneeParty);

		childMovrShipment.setShmAsEntdCusts(moverMatchedParty);
	}

	/**Clone any shipment remarks by {@link ShmRemark} and add to ChildMoverShipment
	 *
	 * @param childMovrShipment
	 * @param entityManager
	 * @param parentShipment
	 */
	private void cloneRemarks( final ShmShipment childMovrShipment, final ShmShipment parentShipment, final EntityManager entityManager) throws ValidationException {
		//map remarks from parent.
		if (CollectionUtils.isEmpty(childMovrShipment.getShmRemarks())) {
			childMovrShipment.setShmRemarks(new ArrayList<>());
		}

		List<Optional<ShmRemark>> optionalRemarks = new ArrayList<>();
		optionalRemarks.add(cloneShipmentRemark(entityManager, parentShipment, ShipmentRemarkTypeCd.SHIPPING_RMK, childMovrShipment.getShpInstId()));
		optionalRemarks.add(cloneShipmentRemark(entityManager, parentShipment, ShipmentRemarkTypeCd.OPRATNL_FRT_HDLNG_RMK, childMovrShipment.getShpInstId()));
		optionalRemarks.stream()
			.filter(Optional::isPresent)
			.forEach(x-> childMovrShipment.addShmRemark(x.get()));

		if (CollectionUtils.isNotEmpty(childMovrShipment.getShmRemarks())) {
			shipmentRemarkSubDAO.persist(childMovrShipment.getShmRemarks(), entityManager);
		}
	}

	private ShmHandlingUnitMvmt createMoverHandlingUnitMvmt(ShmShipment childMovrShipment,
			ShmHandlingUnit childHandlingUnit, AuditInfo auditInfo, EntityManager entityManager) {

		ShmHandlingUnitMvmt movrHandlingUnitMvmt = new ShmHandlingUnitMvmt();
		ShmHandlingUnitMvmtPK id = new ShmHandlingUnitMvmtPK();
		id.setShpInstId(childHandlingUnit.getId().getShpInstId());
		id.setSeqNbr(childHandlingUnit.getId().getSeqNbr());
		id.setMvmtSeqNbr(shmHandlingUnitMvmtSubDAO.getNextMvmtBySeqNbrAndShpInstId(childHandlingUnit.getId().getShpInstId(), childHandlingUnit.getId().getSeqNbr(), entityManager));
		movrHandlingUnitMvmt.setId(id);
		DtoTransformer.setAuditInfo(movrHandlingUnitMvmt, auditInfo);
		movrHandlingUnitMvmt.setMvmtTmst(BasicTransformer.toTimestamp(auditInfo.getCreatedTimestamp()));
		movrHandlingUnitMvmt.setArchiveCntlCd(StringUtils.SPACE);
		movrHandlingUnitMvmt.setBypassScanInd(StringUtils.SPACE);
		movrHandlingUnitMvmt.setBypassScanReason(StringUtils.SPACE);
		movrHandlingUnitMvmt.setDmgdCatgCd(StringUtils.SPACE);
		movrHandlingUnitMvmt.setDockInstId(BigDecimal.ZERO);
		movrHandlingUnitMvmt.setRfsdRsnCd(StringUtils.SPACE);
		movrHandlingUnitMvmt.setExcpTypCd(StringUtils.SPACE);
		movrHandlingUnitMvmt.setScanTmst(DB2DefaultValueUtil.LOW_TMST);
		movrHandlingUnitMvmt.setTrlrInstId(BigDecimal.ZERO);
		movrHandlingUnitMvmt.setUndlvdRsnCd(StringUtils.SPACE);
		movrHandlingUnitMvmt.setMvmtTypCd(HandlingUnitMovementTypeCdTransformer.toCode(HandlingUnitMovementTypeCd.MOVER_SHIPMENT));
		movrHandlingUnitMvmt.setMvmtRptgSicCd(childMovrShipment.getOrigTrmnlSicCd());
		movrHandlingUnitMvmt.setSplitAuthorizeBy(StringUtils.SPACE);
		movrHandlingUnitMvmt.setSplitAuthorizeTmst(DB2DefaultValueUtil.LOW_TMST);
		return movrHandlingUnitMvmt;
	}

	private void addCommodities(ShmShipment childMovrShipment, CreateMoverShipmentRqst request, AuditInfo auditInfo, EntityManager entityManager) throws ServiceException {
		// Commodities
		List<ShmCommodity> commodities = new ArrayList<>();

		for (Commodity commodity : request.getCommodities()) {
			ShmCommodity shmCommodity = DtoTransformer.toShmCommodity(commodity, null);
			shmCommodity.setShmCmdyDimensions(new ArrayList<>());
			for (CommodityDimension dimension : commodity.getCommodityDimension()) {
				ShmCmdyDimension shmDimension = DtoTransformer.toShmCmdyDimension(dimension, null);
				shmDimension.getId().setShpInstId(childMovrShipment.getShpInstId());
				shmDimension.getId().setCmdySeqNbr(shmCommodity.getId().getSeqNbr());
				DtoTransformer.setAuditInfo(shmDimension, auditInfo);
				shmCmdyDimensionSubDAO.persist(shmDimension, entityManager);
				shmCommodity.getShmCmdyDimensions().add(shmDimension);
			}

			shmCommodity.getId().setShpInstId(childMovrShipment.getShpInstId());
			if(shmCommodity.getAsRatedClassCd() == null){
				shmCommodity.setAsRatedClassCd("");
			}

			shmCommodity.setTrfRt(BigDecimal.ZERO);
			shmCommodity.setAmt(BigDecimal.ZERO);
			DtoTransformer.setAuditInfo(shmCommodity, auditInfo);

			commodities.add(shmCommodity);
			entityManager.persist(shmCommodity);
		}

		childMovrShipment.setShmCommodities(commodities);
	}

    private ShmShipment createMover(CreateMoverShipmentRqst request, String moverPro, ShmShipment parentShipment, AuditInfo auditInfo,
        TransactionContext txnContext, EntityManager entityManager) throws ServiceException {

        // validateProNbr
        validateProNbr(moverPro, txnContext, entityManager);

        ShmShipment childMovrShipment = mapper.map(parentShipment, ShmShipment.class); // copy using dozer
		//update from request
		childMovrShipment.setParentInstId(initialize(BasicTransformer.toBigDecimal(parentShipment.getShpInstId())));
		childMovrShipment.setBillClassCd(BillClassCdTransformer.toCode(BillClassCd.ASTRAY_FRT_SEGMENT));
		childMovrShipment.setBillStatCd(BillStatusCdTransformer.toCode(BillStatusCd.RATED));
		childMovrShipment.setProNbrTxt(moverPro);
		childMovrShipment.setOrigTrmnlSicCd(request.getOriginSicCd());
		childMovrShipment.setDestTrmnlSicCd(request.getDestinationSicCd());

		//TODO fetch this up front? -  lazy loaded.
		childMovrShipment.setMovrSuffix(getNextMoverSuffix(parentShipment.getShmShipments()));

		childMovrShipment.setTotPcsCnt(initialize(BasicTransformer.toBigDecimal(request.getTotalPiecesCount())));
		childMovrShipment.setTotWgtLbs(initialize(BasicTransformer.toBigDecimal(request.getTotalWeightLbs())));
		childMovrShipment.setMtrzdPcsCnt(initialize(BasicTransformer.toBigDecimal(request.getTotalMotorMoves())));

		//default for a non-revenue movr
		childMovrShipment.setTotChrgAmt(BigDecimal.ZERO);
        childMovrShipment.setTotUsdAmt(BigDecimal.ZERO);
        childMovrShipment.setRevBillInd(BasicTransformer.toString(false));
        childMovrShipment.setRtgCrncd(StringUtils.SPACE);
        childMovrShipment.setInvcCrncd(StringUtils.SPACE);
        childMovrShipment.setRtgTrfId(StringUtils.SPACE);
        childMovrShipment.setRtAudtrInit(StringUtils.SPACE);
        childMovrShipment.setRtOrRtAudtqNm(StringUtils.SPACE);
        childMovrShipment.setDiscPct(BigDecimal.ZERO);
        childMovrShipment.setPrcAgrmtId(BigDecimal.ZERO);
        childMovrShipment.setPrcRulesetNbr(BigDecimal.ZERO);

        childMovrShipment.setShpmtAcqrTypCd(StringUtils.SPACE);
        childMovrShipment.setCftPrflMthdCd(StringUtils.SPACE);
        childMovrShipment.setCftPrflTypeCd(StringUtils.SPACE);
        childMovrShipment.setPupVolPct(BigDecimal.ZERO);
        childMovrShipment.setTotVolCft(BigDecimal.ZERO);
        childMovrShipment.setDlvryQalfrCd(StringUtils.SPACE);
        childMovrShipment.setChrgToCd(StringUtils.SPACE);
        childMovrShipment.setMvmtStatCd(MovementStatusCdTransformer.toCode(MovementStatusCd.ON_DOCK));

        //default for new shipment
        childMovrShipment.setLstMvRptgSicCd(StringUtils.SPACE);
        childMovrShipment.setLstMvmtTmst(TimestampUtil.getLowTimestamp());
        childMovrShipment.setRtePfxTxt(StringUtils.SPACE);
        childMovrShipment.setRteSfxTxt(StringUtils.SPACE);
        childMovrShipment.setRteTypCd(StringUtils.SPACE);
        childMovrShipment.setDlvrSigNmTxt(StringUtils.SPACE);
        childMovrShipment.setDlvrSigTmst(TimestampUtil.getLowTimestamp());
        childMovrShipment.setCalcSvcTmst(TimestampUtil.getLowTimestamp());
        childMovrShipment.setCalcSvcDays(BigDecimal.ZERO);
        childMovrShipment.setDiffCalcDays(BigDecimal.ZERO);
        childMovrShipment.setDiffTrnstDays(BigDecimal.ZERO);
        childMovrShipment.setHandlingUnitPartialInd(BasicTransformer.toString(false));
        childMovrShipment.setHandlingUnitSplitInd(BasicTransformer.toString(false));
        childMovrShipment.setShpSvcStatCd(StringUtils.SPACE);
        childMovrShipment.setSvcCalcStatCd(StringUtils.SPACE);

		DtoTransformer.setAuditInfo(childMovrShipment, auditInfo);

		shmShipmentSubDAO.create(childMovrShipment, entityManager);

		return childMovrShipment;
	}

	private ShmAsEntdCust getConsigneeParty(ShmShipment parentShipment, EntityManager entityManager, AuditInfo auditInfo, long newParentInstId) {

		// Get Consignee Party
		ShmAsEntdCust parentConsigneeParty = parentShipment.getShmAsEntdCusts().stream()
				.filter(e -> MatchedPartyTypeCdTransformer.toCode(MatchedPartyTypeCd.CONS).equals(e.getTypCd()))
				.findFirst()
				.get();
		entityManager.detach(parentConsigneeParty);

		ShmAsEntdCust consigneeParty = mapper.map(parentConsigneeParty, ShmAsEntdCust.class);
		consigneeParty.setTypCd(MatchedPartyTypeCdTransformer.toCode(MatchedPartyTypeCd.CONS));
		consigneeParty.setMchSourceCd(MatchedPartySourceCdTransformer.toCode(MatchedPartySourceCd.SYSTEM_MATCHED));
		consigneeParty.getId().setShpInstId(newParentInstId);
		consigneeParty.setEMailId(consigneeParty.getEMailId() == null ? StringUtils.SPACE : consigneeParty.getEMailId());
		//FIXME Other than CSHM5411, some CTM process also looks at the countryCd
		consigneeParty.setCntryCd(consigneeParty.getCntryCd().equalsIgnoreCase("CA") ? "CN" : consigneeParty.getCntryCd());
		DtoTransformer.setAuditInfo(consigneeParty, auditInfo);

		return consigneeParty;
	}

	private ShmAsEntdCust getShipperParty(CreateMoverShipmentRqst request, ShmShipment parentShipment, EntityManager entityManager, long newParentInstId, AuditInfo auditInfo, TransactionContext txnContext) throws ServiceException {

		//get origin service center address for mover Shipper...
		GetRefSicAddressResp originLocation = externalRestClient.getRefSicAddress(request.getOriginSicCd(), txnContext);

		ShmAsEntdCust parentShipperParty = parentShipment.getShmAsEntdCusts().stream()
				.filter(e -> MatchedPartyTypeCdTransformer.toCode(MatchedPartyTypeCd.SHPR).equals(e.getTypCd()))
				.findFirst()
				.get();
		entityManager.detach(parentShipperParty);

		ShmAsEntdCust shipperParty = mapper.map(parentShipperParty, ShmAsEntdCust.class);
		shipperParty.setId(parentShipperParty.getId());
		shipperParty.getId().setShpInstId(newParentInstId);
		shipperParty.setTypCd(MatchedPartyTypeCdTransformer.toCode(MatchedPartyTypeCd.SHPR));

        shipperParty.setAddrTxt(originLocation.getLocAddress().getAddr1() == null
                ? StringUtils.SPACE : originLocation.getLocAddress().getAddr1());
        shipperParty.setCtyTxt(originLocation.getLocAddress().getCityName() == null
                ? StringUtils.SPACE : originLocation.getLocAddress().getCityName());
        shipperParty.setStCd(originLocation.getLocAddress().getCountrySubdivisionCd() == null
                ? StringUtils.SPACE : originLocation.getLocAddress().getCountrySubdivisionCd());
        shipperParty.setZip4RestUsTxt(originLocation.getLocAddress().getPostalExtCd() == null
                ? StringUtils.SPACE : originLocation.getLocAddress().getPostalExtCd());
        shipperParty.setZip6Txt(originLocation.getLocAddress().getPostalCd() == null
                ? StringUtils.SPACE : originLocation.getLocAddress().getPostalCd());
        shipperParty.setCntryCd(originLocation.getLocAddress().getCountryCd() == null
                ? StringUtils.SPACE : originLocation.getLocAddress().getCountryCd());
        //FIXME Other than CSHM5411, some CTM process also looks at the countryCd
        shipperParty.setCntryCd(shipperParty.getCntryCd().equalsIgnoreCase("CA") ? "CN" : shipperParty.getCntryCd());

		shipperParty.setMchSourceCd(MatchedPartySourceCdTransformer.toCode(MatchedPartySourceCd.NOT_MATCHED));

		shipperParty.setName1Txt(parentShipperParty.getName1Txt());
		shipperParty.setName2Txt(DEFAULT_MOVR_SHPR_NAME2);
		shipperParty.setMchStatCd(parentShipperParty.getMchStatCd());
		shipperParty.setDebtorInd("N");
		shipperParty.setEMailId(shipperParty.getEMailId() == null ? StringUtils.SPACE : shipperParty.getEMailId());
        DtoTransformer.setAuditInfo(shipperParty, auditInfo);

		return shipperParty;
	}

	private Optional<ShmRemark> cloneShipmentRemark(EntityManager entityManager, ShmShipment parentShipment, ShipmentRemarkTypeCd typeCd, long newParentInstId) {

		ShmRemarkPK id = new ShmRemarkPK();
		id.setTypCd(ShipmentRemarkTypeCdTransformer.toCode(typeCd));
		id.setShpInstId(parentShipment.getShpInstId());

		ShmRemark remarks = shipmentRemarkSubDAO.findById(id, entityManager);
		if (remarks == null) {
			return Optional.empty();
		}
		entityManager.detach(remarks);
		remarks.getId().setShpInstId(newParentInstId);
		return Optional.of(remarks);
	}

	private List<ShmSrNbr> cloneSupplementalReferenceNumbers(ShmShipment childMovrShipment, ShmShipment parentShipment, EntityManager entityManager) throws ValidationException  {
		List<ShmSrNbr> childShmSrNbr = new ArrayList<>();
		long newParentInstId = childMovrShipment.getShpInstId();
		if (CollectionUtils.isNotEmpty(parentShipment.getShmSrNbrs())) {
			List<ShmSrNbr> parentSupRefs = shipmentSupRefSubDAO.listSupRefNumbersForShipmentIdList(Arrays.asList(parentShipment.getShpInstId()), entityManager);
			for (ShmSrNbr shmSrNbr : parentSupRefs) {
				entityManager.detach(shmSrNbr);
				shmSrNbr.getId().setShpInstId(newParentInstId);
				shipmentSupRefSubDAO.persist(shmSrNbr, entityManager);
				childShmSrNbr.add(shmSrNbr);
			}
		}

		childMovrShipment.setShmSrNbrs(childShmSrNbr);
		return childShmSrNbr;
	}


	private String getAndValidateProNumber(String string, TransactionContext txnContext) throws ValidationException {
		ProNumber _proNumber = ProNumber.from(string);
		if (_proNumber.isValid()) {
			return _proNumber.getNormalized();
		}else {
			throw ExceptionBuilder.exception(ValidationErrorMessage.PRO_NBR_FORMAT_INVALID, txnContext).build();
		}
	}

	/**Validate required request objects
	 * @param request
	 * @param txnContext
	 * @throws ValidationException
	 */
	private void validateRequest(@NotNull CreateMoverShipmentRqst request, @NotNull TransactionContext txnContext) throws ValidationException {

		if (StringUtils.isBlank(request.getParentProNbr())) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.PARENT_PRO_REQ, txnContext).build();
		}
		if (StringUtils.isBlank(request.getMoverProNbr())) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.MOVER_PRO_NBR_REQ, txnContext).build();
		}
		if (CollectionUtils.isEmpty(request.getChildProNbrs())){
			throw ExceptionBuilder.exception(ValidationErrorMessage.CHILD_PRO_REQ, txnContext).build();
		}
		if (CollectionUtils.isEmpty(request.getCommodities())) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.COMMODITY_GROUP_EMPTY, txnContext).build();
		}
		if (StringUtils.isBlank(request.getDestinationSicCd())) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.DEST_SIC_CD_REQ, txnContext).build();
		}
		if (StringUtils.isBlank(request.getOriginSicCd())) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.ORIGIN_SIC_CD_REQ, txnContext).build();
		}
		if (request.getTotalPiecesCount() == null || request.getTotalPiecesCount().compareTo(BigInteger.ZERO) == 0) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.PIECE_COUNTS_MISSING, txnContext).build();
		}
		if (request.getTotalWeightLbs() == null || request.getTotalWeightLbs().compareTo(Double.valueOf(0)) == 0) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.SHIPMENT_WEIGHT_IS_REQUIRED, txnContext).build();
		}
		//ensure this is upper case, everything but this from request can be manually entered
		request.setOriginSicCd(request.getOriginSicCd().toUpperCase());
	}

	private BigDecimal initialize(BigDecimal bigDecimal) {
		return bigDecimal == null ? BigDecimal.ZERO : bigDecimal;
	}

	private String getNextMoverSuffix(List<ShmShipment> shmShipments) {

		return shmShipments.stream()
				.filter(x -> x.getBillClassCd().equals(BillClassCdTransformer.toCode(BillClassCd.ASTRAY_FRT_SEGMENT)))
				.map(ShmShipment::getMovrSuffix)
				.filter(Objects::nonNull)
				.sorted(Comparator.reverseOrder())
				.findFirst()
				.map(letter -> {
					char currentSuffix = letter.charAt(0);
					return String.valueOf((char) (currentSuffix + 1));
				})
				.orElse("A");
	}

    private ProFrtBillIndex upsertProFrtBillIndexByPro(ShmShipment shmShipment,
        AuditInfo auditInfo, EntityManager entityManager, TransactionContext txnContext) throws ServiceException {

        ProFrtBillIndex proFrtBillIndexEntity = proNbrFrtBillIndexDAO.findById(shmShipment.getProNbrTxt(), entityManager);

        if(proFrtBillIndexEntity == null) {

            proFrtBillIndexEntity = new ProFrtBillIndex();
            proFrtBillIndexEntity.setProNbrTxt(shmShipment.getProNbrTxt());
            proFrtBillIndexEntity.setShpInstId(new BigDecimal(shmShipment.getShpInstId()));
            proFrtBillIndexEntity.setStatCd(PRO_FRT_BILLED);
            proFrtBillIndexEntity.setMvmtUnitSeqNbr(new BigDecimal(1));
            proFrtBillIndexEntity.setProPfxOvrdInd("N");
            proFrtBillIndexEntity.setBillSicCd(shmShipment.getOrigTrmnlSicCd());

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
                            shmShipment.getProNbrTxt()))
                    .build();
            }

            proFrtBillIndexEntity.setShpInstId(new BigDecimal(shmShipment.getShpInstId()));
            proFrtBillIndexEntity.setStatCd(PRO_FRT_BILLED);
            proFrtBillIndexEntity.setMvmtUnitSeqNbr(new BigDecimal(1));
            proFrtBillIndexEntity.setProPfxOvrdInd("N");
            proFrtBillIndexEntity.setBillSicCd(shmShipment.getOrigTrmnlSicCd());

            DtoTransformer.setAuditInfo(proFrtBillIndexEntity, auditInfo);

            proNbrFrtBillIndexDAO.save(proFrtBillIndexEntity, entityManager);
            proNbrFrtBillIndexDAO.updateDB2ProNbrFrtBillIndexForUpdSkeleton(proFrtBillIndexEntity, db2EntityManager);

        }

        return proFrtBillIndexEntity;


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
    }


}
