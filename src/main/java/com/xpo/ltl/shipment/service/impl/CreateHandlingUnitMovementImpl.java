package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmEventLog;
import com.xpo.ltl.api.shipment.service.entity.ShmEventLogPK;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmt;
import com.xpo.ltl.api.shipment.service.entity.ShmMovement;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementPK;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DeliveryQualifierCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.EventLogSubTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.EventLogTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.RouteTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentMovementTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.CreateHandlingUnitMovementResp;
import com.xpo.ltl.api.shipment.v2.CreateHandlingUnitMovementRqst;
import com.xpo.ltl.api.shipment.v2.DeliveryQualifierCd;
import com.xpo.ltl.api.shipment.v2.EquipmentId;
import com.xpo.ltl.api.shipment.v2.EventLogSubTypeCd;
import com.xpo.ltl.api.shipment.v2.EventLogTypeCd;
import com.xpo.ltl.api.shipment.v2.HandlingUnitMovement;
import com.xpo.ltl.api.shipment.v2.HandlingUnitMovementTypeCd;
import com.xpo.ltl.api.shipment.v2.MovementStatusCd;
import com.xpo.ltl.api.shipment.v2.RouteTypeCd;
import com.xpo.ltl.api.shipment.v2.ShipmentMovementTypeCd;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShmEventLogSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitMvmtSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmMovementSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.HandlingUnitMovementHelper;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import com.xpo.ltl.shipment.service.util.TimestampUtil;

@RequestScoped
public class CreateHandlingUnitMovementImpl {

	private static final String DEFAULT_IND_VALUE = "N";

	@PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
	private EntityManager db2EntityManager;

	@Inject
	private ShmShipmentSubDAO shipmentDAO;

	@Inject
	private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

	@Inject
	private ShmEventLogSubDAO shmEventLogSubDAO;

	@Inject
	private ShmMovementSubDAO shmMovementSubDAO;

	@Inject
	private ShmHandlingUnitMvmtSubDAO shmHandlingUnitMvmtSubDAO;

	@Inject
	private ExternalRestClient restClient;

	public CreateHandlingUnitMovementResp createHandlingUnitMovement(
			CreateHandlingUnitMovementRqst createHandlingUnitMovementRqst, String childProNbr,
			TransactionContext txnContext, EntityManager entityManager) throws ServiceException {

		CreateHandlingUnitMovementResp createHandlingUnitMovementResp =
				new CreateHandlingUnitMovementResp();
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
		checkNotNull(createHandlingUnitMovementRqst, "The request is required.");
		HandlingUnitMovement handlingUnitMovement = createHandlingUnitMovementRqst.getHandlingUnitMovement();

		List<MoreInfo> moreInfos =  Lists.newArrayList();

		String formattedProNbr = ProNumberHelper.validateProNumber(childProNbr, txnContext);
		if (!ProNumberHelper.isYellowPro(formattedProNbr)) {
			moreInfos.add(createMoreInfo("childProNbr",
					ValidationErrorMessage.PRO_NBR_FORMAT_INVALID.message() +
							"Expected a valid Yellow Pro Number format for childProNbr " +
							formattedProNbr));
		}

		validateShmHandlingMovement(handlingUnitMovement, moreInfos, txnContext);

		if (CollectionUtils.isNotEmpty(moreInfos)) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo(moreInfos)
					.build();
		}

		ShmHandlingUnit existentShmHandlingUnit =
				shmHandlingUnitSubDAO.findByTrackingProNumber(formattedProNbr, entityManager);

		if (Objects.isNull(existentShmHandlingUnit)){
			throw ExceptionBuilder.exception(NotFoundErrorMessage.SHM_HANDLING_UNIT_NOT_FOUND, txnContext)
					.moreInfo(moreInfos)
					.build();
		}

		AuditInfo auditInfo = new AuditInfo();
		AuditInfoHelper.setCreatedInfo(auditInfo, txnContext);
		AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);

		Long shipmentInstId = existentShmHandlingUnit.getId().getShpInstId();
		Long seqNbr = existentShmHandlingUnit.getId().getSeqNbr();

		EquipmentId trailer = createHandlingUnitMovementRqst.getTrailer();

		createShmHandlingUnitMvmt(handlingUnitMovement, trailer, shipmentInstId, seqNbr,
				auditInfo, entityManager);

		String movementReportingSicCd = handlingUnitMovement.getMovementReportingSicCd();
		BigDecimal trailerInstId = new BigDecimal(handlingUnitMovement.getTrailerInstanceId());

		if (HandlingUnitMovementTypeCd.CLOSE.equals(handlingUnitMovement.getMovementTypeCd())) {
			ShmShipment existentShmShipment = shipmentDAO.findByIdOrProNumber(null, shipmentInstId,
					entityManager);

			if (Objects.nonNull(existentShmShipment)){
				boolean reShpCredInd = false;
				Long moveSeqNumber = 1L;
				ShmMovement shmMovement =
						shmMovementSubDAO.findMostRecentByShpInstId(shipmentInstId, entityManager);
				if (Objects.nonNull(shmMovement)) {
					ShipmentMovementTypeCd shipmentMovementTypeCd =
							ShipmentMovementTypeCdTransformer.toEnum(shmMovement.getTypCd());
					if (ShipmentMovementTypeCd.CLOSE.equals(shipmentMovementTypeCd) ||
							ShipmentMovementTypeCd.FORCED_TO_LOCATION.equals(shipmentMovementTypeCd)) {
						if (!movementReportingSicCd.equals(shmMovement.getMvmtRptgSicCd()) &&
								!trailerInstId.equals(shmMovement.getTrlrInstId())) {
							reShpCredInd = true;
						}

					}
					moveSeqNumber = shmMovement.getId().getSeqNbr() + 1;
				}

				Timestamp lastMvmtTimestamp =
						BasicTransformer.toTimestamp(handlingUnitMovement.getMovementDateTime());

				createShmMvmt(trailer, movementReportingSicCd, lastMvmtTimestamp, reShpCredInd,
						shipmentInstId, moveSeqNumber, auditInfo, entityManager);

				if (StringUtils.isBlank(existentShmShipment.getRteTypCd()) &&
						DeliveryQualifierCdTransformer.toCode(DeliveryQualifierCd.ALL_SHORT)
								.equals(existentShmShipment.getDlvryQalfrCd())) {
					existentShmShipment.setRteTypCd(RouteTypeCdTransformer.toCode(RouteTypeCd.STAGED));
				}

				existentShmShipment.setCurrSicCd(StringUtils.SPACE);
				existentShmShipment.setMvmtStatCd(MovementStatusCdTransformer.toCode(MovementStatusCd.ON_TRAILER));
				existentShmShipment.setLstMvRptgSicCd(movementReportingSicCd);
				existentShmShipment.setLstMvmtTmst(lastMvmtTimestamp);
				DtoTransformer.setLstUpdateAuditInfo(existentShmShipment, auditInfo);
				shipmentDAO.save(existentShmShipment, entityManager);
				shipmentDAO.updateDB2ShmShipment(existentShmShipment, existentShmShipment.getLstUpdtTmst(), txnContext,
						db2EntityManager);

				ShmEventLog shmEventLog = createEventLog(shipmentInstId, existentShmShipment.getProNbrTxt(),
						movementReportingSicCd, existentShmShipment.getDestTrmnlSicCd(), trailer,
						lastMvmtTimestamp, new BigDecimal(moveSeqNumber), existentShmShipment.getTotPcsCnt(),
						existentShmShipment.getHazmatInd(), existentShmShipment.getTotWgtLbs(),
						auditInfo, entityManager);
				ShmEventLog createdShmEventLog = shmEventLogSubDAO.create(shmEventLog, entityManager);
				shmEventLogSubDAO.createDB2ShmEventLog(createdShmEventLog, db2EntityManager);
			}
		}

		createHandlingUnitMovementResp.setHandlingUnitMovement(handlingUnitMovement);
		return createHandlingUnitMovementResp;
	}



	private ShmHandlingUnitMvmt createShmHandlingUnitMvmt(HandlingUnitMovement handlingUnitMvmt,
			EquipmentId trailer, long shpInstId, long seqNbr, AuditInfo auditInfo,
			EntityManager entityManager) throws ServiceException{

		ShmHandlingUnitMvmt shmHandlingUnitMvmt = new ShmHandlingUnitMvmt();
		handlingUnitMvmt.setShipmentInstanceId(shpInstId);
		handlingUnitMvmt.setSequenceNbr(BigInteger.valueOf(seqNbr));
		handlingUnitMvmt.setMovementSequenceNbr(BigInteger.valueOf(shmHandlingUnitMvmtSubDAO.getNextMvmtBySeqNbrAndShpInstId(shpInstId,
				seqNbr, entityManager)));
		if (Objects.nonNull(trailer) && StringUtils.isNotBlank(trailer.getEquipmentInstId())){
			handlingUnitMvmt.setTrailerInstanceId(new Long(trailer.getEquipmentInstId()));
		}
		if (Objects.isNull(handlingUnitMvmt.getAuditInfo())){
			handlingUnitMvmt.setAuditInfo(auditInfo);
		}

		HandlingUnitMovementHelper.setDefaultValues(handlingUnitMvmt);

		shmHandlingUnitMvmt = DtoTransformer.toShmHandlingUnitMvmt(handlingUnitMvmt,
				shmHandlingUnitMvmt);
		DtoTransformer.setAuditInfo(shmHandlingUnitMvmt, auditInfo);
		shmHandlingUnitMvmtSubDAO.save(shmHandlingUnitMvmt, entityManager);
		shmHandlingUnitMvmtSubDAO.createDB2ShmHandlingUnitMvmt(shmHandlingUnitMvmt, db2EntityManager);
		return shmHandlingUnitMvmt;
	}

	private ShmMovement createShmMvmt(EquipmentId trailer, String reportingMvmtSicCd,
			Timestamp movementDateTime, boolean reshipCredInd, long shpInstId, long seqNbr, AuditInfo
			auditInfo, EntityManager entityManager) throws ServiceException{

		ShmMovement shmMovement = new ShmMovement();
		ShmMovementPK shmMovementPK = new ShmMovementPK();
		shmMovementPK.setShpInstId(shpInstId);
		shmMovementPK.setSeqNbr(seqNbr);
		shmMovement.setId(shmMovementPK);
		shmMovement.setRshpCredInd(BasicTransformer.toString(reshipCredInd));
		shmMovement.setMvmtRptgSicCd(reportingMvmtSicCd);
		shmMovement.setMvmtTmst(movementDateTime);
		shmMovement.setTypCd(ShipmentMovementTypeCdTransformer.toCode(ShipmentMovementTypeCd.CLOSE));
		if (Objects.nonNull(trailer)){
			String trailerInstId = trailer.getEquipmentInstId();
			String equipmentPrefix = trailer.getEquipmentPrefix();
			String equipmentSuffix = trailer.getEquipmentSuffix();

			if (StringUtils.isNotBlank(trailerInstId)){
				shmMovement.setTrlrInstId(new BigDecimal(trailerInstId));
			} else {
				shmMovement.setTrlrInstId(ZERO);
			}

			if (StringUtils.isNotBlank(equipmentSuffix)){
				shmMovement.setTrlrIdSfxNbr(new BigDecimal(equipmentSuffix));
			} else {
				shmMovement.setTrlrIdSfxNbr(ZERO);
			}

			if (StringUtils.isNotBlank(equipmentPrefix)){
				shmMovement.setTrlrIdPfxTxt(equipmentPrefix);
			} else {
				shmMovement.setTrlrIdPfxTxt(StringUtils.SPACE);
			}

		} else {
			shmMovement.setTrlrIdPfxTxt(StringUtils.SPACE);
			shmMovement.setTrlrIdSfxNbr(ZERO);
			shmMovement.setTrlrInstId(ZERO);
		}
		shmMovement.setTrlrLdSeqNbr(ZERO);
		shmMovement.setObcPkpDlvRteCd(StringUtils.SPACE);
		shmMovement.setScacCd(StringUtils.SPACE);
		shmMovement.setCurrSicCd(StringUtils.SPACE);
		shmMovement.setArchiveCntlCd(StringUtils.SPACE);
		shmMovement.setCustLocArivTmst(TimestampUtil.getLowTimestamp());
		shmMovement.setQlfrRsnCd(StringUtils.SPACE);
		shmMovement.setDlvryQalfrCd(StringUtils.SPACE);
		shmMovement.setRtePfxTxt(StringUtils.SPACE);
		shmMovement.setRteSfxTxt(StringUtils.SPACE);
		shmMovement.setCarrierId(ZERO);

		DtoTransformer.setAuditInfo(shmMovement, auditInfo);
		shmMovementSubDAO.save(shmMovement, entityManager);
		shmMovementSubDAO.createDB2ShmMovement(shmMovement, db2EntityManager);
		return shmMovement;
	}



	private void validateShmHandlingMovement(HandlingUnitMovement handlingUnitMovement, List<MoreInfo> moreInfoList,
			TransactionContext txnContext) throws ServiceException {

		if (Objects.isNull(moreInfoList)){
			moreInfoList = Lists.newArrayList();
		}
		if (Objects.isNull(handlingUnitMovement)){
			moreInfoList.add(createMoreInfo("handlingUnitMovement",
					"The shipment handling unit movement is required."));
		} else {

			XMLGregorianCalendar lastMovementDateTime = handlingUnitMovement.getMovementDateTime();

			if (Objects.isNull(lastMovementDateTime)) {
				moreInfoList.add(createMoreInfo("handlingUnitMovement.movementDateTime",
						ValidationErrorMessage.MVMT_TMST_RQ.message()));
			}

			String requestingSicCd = handlingUnitMovement.getMovementReportingSicCd();

			if (StringUtils.isBlank(requestingSicCd)) {
				moreInfoList.add(createMoreInfo("handlingUnitMovement.movementReportingSicCd",
						ValidationErrorMessage.LST_MV_RPT_SIC_RQ.message()));
			} else if (!restClient.isActiveSic(requestingSicCd, txnContext)) {
				moreInfoList.add(createMoreInfo("handlingUnitMovement.movementReportingSicCd",
						"The SIC code is not an active or valid SIC."));
			}

			if (Objects.isNull(handlingUnitMovement.getMovementTypeCd()) ) {
				moreInfoList.add(createMoreInfo("handlingUnitMovement.movementTypeCd",
						"Handling unit movement type code is required."));
			}

		}
	}


	private ShmEventLog createEventLog(Long shpInstId, String proNumber, String reportingSicCd,
			String destSicCd, EquipmentId trailer, Timestamp lastMovementDateTime,
			BigDecimal calcMvmtSeqNbr, BigDecimal totalPcsCount, String hazmatInd,
			BigDecimal totalWeightLbs, AuditInfo auditInfo, EntityManager entityManager){

		ShmEventLog shmEventLog = new ShmEventLog();
		ShmEventLogPK shmEventLogPK = new ShmEventLogPK();
		shmEventLogPK.setShpInstId(shpInstId);
        shmEventLogPK.setSeqNbr(shmEventLogSubDAO.getLastUsedSeqNbr(shpInstId, entityManager, db2EntityManager) + 1);
		shmEventLog.setId(shmEventLogPK);
		DtoTransformer.setAuditInfo(shmEventLog, auditInfo);
		shmEventLog.setTypCd(EventLogTypeCdTransformer.toCode(EventLogTypeCd.TRLR_CONTENTS));
		shmEventLog.setSubTypCd(EventLogSubTypeCdTransformer.toCode(EventLogSubTypeCd.LOAD_BY_TRAILER));
		shmEventLog.setProNbrTxt(proNumber);
		shmEventLog.setRptgSicCd(reportingSicCd);
		shmEventLog.setOrigTrmnlSicCd(reportingSicCd);

		if (StringUtils.isNotBlank(destSicCd)){
			shmEventLog.setDestTrmnlSicCd(destSicCd);
		} else {
			shmEventLog.setDestTrmnlSicCd(StringUtils.SPACE);
		}

		shmEventLog.setOccurredTmst(lastMovementDateTime);

		if (Objects.nonNull(totalPcsCount)) {
			shmEventLog.setTotPcsCnt(totalPcsCount);
		} else {
			shmEventLog.setTotPcsCnt(ZERO);
		}

		if (Objects.nonNull(totalWeightLbs)){
			shmEventLog.setTotWgtLbs(totalWeightLbs);
		} else {
			shmEventLog.setTotWgtLbs(ZERO);
		}

		shmEventLog.setMvmtSeqNbr(calcMvmtSeqNbr);

		if (StringUtils.isNotBlank(hazmatInd)){
			shmEventLog.setHazmatInd(hazmatInd);
		} else {
			shmEventLog.setHazmatInd(DEFAULT_IND_VALUE);
		}

		if (Objects.nonNull(trailer)){
			String equipmentPrefix = trailer.getEquipmentPrefix();
			String equipmentSuffix = trailer.getEquipmentSuffix();

			if (StringUtils.isNotBlank(equipmentSuffix)){
				shmEventLog.setTrlrIdSfxNbr(new BigDecimal(equipmentSuffix));
			} else {
				shmEventLog.setTrlrIdSfxNbr(ZERO);
			}

			if (StringUtils.isNotBlank(equipmentPrefix)){
				shmEventLog.setTrlrIdPfxTxt(equipmentPrefix);
			} else {
				shmEventLog.setTrlrIdPfxTxt(StringUtils.SPACE);
			}

		} else {
			shmEventLog.setTrlrIdPfxTxt(StringUtils.SPACE);
			shmEventLog.setTrlrIdSfxNbr(ZERO);
		}

		shmEventLog.setTranId(StringUtils.SPACE);
		shmEventLog.setAdminInstId(ZERO);
		shmEventLog.setPgmId(StringUtils.SPACE);
		shmEventLog.setAdminStatCd(StringUtils.SPACE);
		shmEventLog.setArchiveCntlCd(StringUtils.SPACE);
		shmEventLog.setBil21stCustNbr(ZERO);
		shmEventLog.setBil22ndCustNbr(ZERO);
		shmEventLog.setBillClassCd(StringUtils.SPACE);
		shmEventLog.setBilto1stRelCd(StringUtils.SPACE);
		shmEventLog.setBilto2ndRelCd(StringUtils.SPACE);
		shmEventLog.setBolInstId(ZERO);
		shmEventLog.setChrgToCd(StringUtils.SPACE);
		shmEventLog.setConsCntryCd(StringUtils.SPACE);
		shmEventLog.setConsCustNbr(ZERO);
		shmEventLog.setEdiSenderId(StringUtils.SPACE);
		shmEventLog.setEdiSenderShpId(StringUtils.SPACE);
		shmEventLog.setEnrouteInd(DEFAULT_IND_VALUE);
		shmEventLog.setEstimatedDlvrDt(TimestampUtil.getLowTimestamp());
		shmEventLog.setGarntdInd(DEFAULT_IND_VALUE);
		shmEventLog.setMvmtStatCd(MovementStatusCdTransformer.toCode(MovementStatusCd.ON_TRAILER));
		shmEventLog.setPkupDt(TimestampUtil.getLowTimestamp());
		shmEventLog.setPurInstId(ZERO);
		shmEventLog.setShprCustNbr(ZERO);
		shmEventLog.setTotChrgAmt(ZERO);
		shmEventLog.setOccurredSicCd(reportingSicCd);
		shmEventLog.setParentInstId(ZERO);
		shmEventLog.setEnrouteInd(StringUtils.SPACE);
		shmEventLog.setShprCntryCd(StringUtils.SPACE);
		shmEventLog.setConsCntryCd(StringUtils.SPACE);
		shmEventLog.setAdminStatCd(StringUtils.SPACE);
		shmEventLog.setDebtorCd(StringUtils.SPACE);
		shmEventLog.setThirdPartyInd(StringUtils.SPACE);
		shmEventLog.setCorrelationId(StringUtils.SPACE);

		return shmEventLog;
	}

	private MoreInfo createMoreInfo(String location, String message) {
		MoreInfo moreInfo = new MoreInfo();
		moreInfo.setItemNbr(null);
		moreInfo.setMessage(message);
		moreInfo.setLocation(location);
		return moreInfo;
	}

}
