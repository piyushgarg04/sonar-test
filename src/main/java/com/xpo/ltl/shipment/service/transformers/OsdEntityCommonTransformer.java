package com.xpo.ltl.shipment.service.transformers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import com.xpo.ltl.api.shipment.v2.OsdStatusCd;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmMgmtRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdHeader;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdImage;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.CloseReasonCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ConeColorCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.IdentifiedLocationTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OsdCategoryCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OsdStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ShipmentManagementRemarkTypeCdTransformer;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.OsdManagementRemark;
import com.xpo.ltl.api.shipment.v2.OsdParentShipment;
import com.xpo.ltl.api.shipment.v2.ShipmentManagementRemarkTypeCd;
import com.xpo.ltl.api.shipment.v2.UpsertOsdRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShipmentManagementRemarkSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmOsdHdrSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmOsdImageSubDAO;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;

public class OsdEntityCommonTransformer extends BasicTransformer {

	private static final List<OsdStatusCd> CLOSED_STATUS_CDS = Arrays.asList(OsdStatusCd.D_CLOSED, OsdStatusCd.O_CLOSED,
			OsdStatusCd.R_CLOSED, OsdStatusCd.S_CLOSED, OsdStatusCd.OT_CLOSED);
	private static final String SHM_OSD_HEADER_SEQ = "SHM_OSD_HEADER_SEQ";
	private static final String SHM_OVRG_IMG_HDR_SEQ = "SHM_OSD_IMAGE_SEQ";
	private static final String SHM_MGMT_REMARK_SEQ = "SHM_MGMT_REMARK_SEQ";
	private static final String HYPEN = "-";

	@Inject
	private ShipmentManagementRemarkSubDAO shipmentManagementRemarkDAO;

	@Inject
	private ShmOsdImageSubDAO shmOsdImageSubDAO;

	@Inject
	private ShmOsdHdrSubDAO shmOsdHdrSubDAO;

	public ShmOsdHeader buildShmOsdHeader(UpsertOsdRqst upsertOsdRqst, ShmOsdHeader shmOsdHeaderEntity,
			long documentImageCount, AuditInfo auditInfo, ShmShipment shmShipment, TransactionContext txnContext,
			EntityManager entityManager) throws ValidationException {

		ActionCd actionCd = upsertOsdRqst.getActionCd();
		OsdParentShipment osdParentShipment = upsertOsdRqst.getOsdParentShipment();

		if (ActionCd.ADD == actionCd) {
			shmOsdHeaderEntity = new ShmOsdHeader();
			shmOsdHdrSubDAO.setOsdId(shmOsdHeaderEntity, SHM_OSD_HEADER_SEQ, entityManager);
			shmOsdHeaderEntity.setArriveAtOsdTmst(
					BasicTransformer.toTimestamp(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance())));
			DtoTransformer.setAuditInfo(shmOsdHeaderEntity, auditInfo);
			shmOsdHeaderEntity.setOsdNumberTxt(
					getOsdNumber(shmOsdHeaderEntity.getOsdId(), osdParentShipment.getReportingSicCd()));
		}

		if (Objects.nonNull(osdParentShipment)) {

			if (ActionCd.UPDATE == actionCd) {
				shmOsdHeaderEntity.setOsdId(upsertOsdRqst.getOsdId());
				DtoTransformer.setLstUpdateAuditInfo(shmOsdHeaderEntity, auditInfo);
			}

			if (Objects.nonNull(osdParentShipment.getParentProNbr())) {
				shmOsdHeaderEntity.setProNbrTxt(
						ProNumberHelper.toElevenDigitPro(osdParentShipment.getParentProNbr(), txnContext));
			}

			if (Objects.nonNull(osdParentShipment.getReportingSicCd())) {
				shmOsdHeaderEntity.setReportingSicCd(osdParentShipment.getReportingSicCd());
			}

			if (Objects.nonNull(osdParentShipment.getShipmentInstId())) {
				shmOsdHeaderEntity.setShpInstId(BasicTransformer.toBigDecimal(osdParentShipment.getShipmentInstId()));
			}

			if (Objects.nonNull(shmShipment)) {
				shmOsdHeaderEntity.setShpInstId(BasicTransformer.toBigDecimal(shmShipment.getShpInstId()));
			}

			if (Objects.nonNull(osdParentShipment.getOsdCategoryCd())) {
				shmOsdHeaderEntity
						.setOsdCategoryCd(OsdCategoryCdTransformer.toCode(osdParentShipment.getOsdCategoryCd()));
			}

			if (Objects.nonNull(osdParentShipment.getConeColorCd())) {
				shmOsdHeaderEntity.setConeColorCd(ConeColorCdTransformer.toCode(osdParentShipment.getConeColorCd()));
			}

			if (Objects.nonNull(osdParentShipment.getConeNbr())) {
				shmOsdHeaderEntity.setConeNbr(BasicTransformer.toBigDecimal(osdParentShipment.getConeNbr()));
			}

			if (Objects.nonNull(osdParentShipment.getOsdDescription())) {
				shmOsdHeaderEntity.setOsdDescription(osdParentShipment.getOsdDescription());
			}

			if (Objects.nonNull(osdParentShipment.getIdentifiedLocationTypeCd())) {
				shmOsdHeaderEntity.setIdentifiedLocTypeCd(
						IdentifiedLocationTypeCdTransformer.toCode(osdParentShipment.getIdentifiedLocationTypeCd()));
			}

			if (Objects.nonNull(osdParentShipment.getIdentifiedLocationId())) {
				shmOsdHeaderEntity.setIdentifiedLocId(osdParentShipment.getIdentifiedLocationId());
			}

			if (documentImageCount > 0) {
				shmOsdHeaderEntity.setPhotoCnt(BasicTransformer.toBigDecimal(documentImageCount));
			}

			if (Objects.nonNull(osdParentShipment.getOsdPiecesCount())) {
				shmOsdHeaderEntity.setHuCnt(BasicTransformer.toBigDecimal(osdParentShipment.getOsdPiecesCount()));
			}

			if (Objects.nonNull(osdParentShipment.getAssignedUserId())) {
				shmOsdHeaderEntity.setAssignedUser(osdParentShipment.getAssignedUserId());
			}

			if (Objects.nonNull(osdParentShipment.getDockWorkerUserId())) {
				shmOsdHeaderEntity.setDockWorkerUserid(osdParentShipment.getDockWorkerUserId());
			}

			if (Objects.nonNull(osdParentShipment.getStatusCd())) {

				shmOsdHeaderEntity.setStatusCd(OsdStatusCdTransformer.toCode(osdParentShipment.getStatusCd()));
				if (CLOSED_STATUS_CDS.contains(osdParentShipment.getStatusCd())) {
					shmOsdHeaderEntity
							.setCloseReasonCd(CloseReasonCdTransformer.toCode(osdParentShipment.getCloseReasonCd()));
					shmOsdHeaderEntity.setOsdCloseTmst(BasicTransformer
							.toTimestamp(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance())));
				}
			}
		}

		return shmOsdHeaderEntity;
	}

	public ShmOsdImage buildShmOsdImage(ActionCd actionCd, ShmOsdHeader osdHeaderEntity,
			OsdParentShipment osdParentShipment, AuditInfo auditInfo, ShmOsdImage shmOsdImageEntity,
			TransactionContext txnContext, EntityManager entityManager) throws ValidationException {

		if (ActionCd.ADD == actionCd) {
			shmOsdImageEntity = new ShmOsdImage();
			shmOsdImageSubDAO.setInstId(shmOsdImageEntity, SHM_OVRG_IMG_HDR_SEQ, entityManager);
			DtoTransformer.setAuditInfo(shmOsdImageEntity, auditInfo);
		}

		if (ActionCd.UPDATE == actionCd) {
			DtoTransformer.setLstUpdateAuditInfo(shmOsdImageEntity, auditInfo);
		}

		if (Objects.nonNull(osdParentShipment)) {

			if (Objects.nonNull(osdParentShipment.getOsdCategoryCd())) {
				shmOsdImageEntity
						.setOsdCategoryCd(OsdCategoryCdTransformer.toCode(osdParentShipment.getOsdCategoryCd()));
			} else {
				shmOsdImageEntity.setOsdCategoryCd(osdHeaderEntity.getOsdCategoryCd());
			}

			if (Objects.nonNull(osdParentShipment.getReportingSicCd())) {
				shmOsdImageEntity.setRptgSicCd(osdParentShipment.getReportingSicCd());
			} else {
				shmOsdImageEntity.setRptgSicCd(osdHeaderEntity.getReportingSicCd());
			}

			if (Objects.nonNull(osdParentShipment.getSelectedTags())) {
				shmOsdImageEntity.setSelectedTags(osdParentShipment.getSelectedTags());
			}

			if (Objects.nonNull(osdParentShipment.getStatusCd())) {
				shmOsdImageEntity.setStatusCd(OsdStatusCdTransformer.toCode(osdParentShipment.getStatusCd()));
			} else {
				shmOsdImageEntity.setStatusCd(osdHeaderEntity.getStatusCd());
			}
			if (Objects.nonNull(osdParentShipment.getParentProNbr())) {
				shmOsdImageEntity.setOrigProNbrTxt(
						ProNumberHelper.toElevenDigitPro(osdParentShipment.getParentProNbr(), txnContext));
			} else if (Objects.nonNull(osdHeaderEntity.getProNbrTxt())){
				shmOsdImageEntity
						.setOrigProNbrTxt(ProNumberHelper.toElevenDigitPro(osdHeaderEntity.getProNbrTxt(), txnContext));
			}
		} else {

			if (Objects.nonNull(osdHeaderEntity.getOsdCategoryCd())) {
				shmOsdImageEntity.setOsdCategoryCd(osdHeaderEntity.getOsdCategoryCd());
			}

			if (Objects.nonNull(osdHeaderEntity.getReportingSicCd())) {
				shmOsdImageEntity.setRptgSicCd(osdHeaderEntity.getReportingSicCd());
			}

			if (Objects.nonNull(osdHeaderEntity.getStatusCd())) {
				shmOsdImageEntity.setStatusCd(osdHeaderEntity.getStatusCd());
			}

			if (Objects.nonNull(osdHeaderEntity.getProNbrTxt())) {
				shmOsdImageEntity
						.setOrigProNbrTxt(ProNumberHelper.toElevenDigitPro(osdHeaderEntity.getProNbrTxt(), txnContext));
			}
		}
		shmOsdImageEntity.setOverPairedWithShortInd("N");
		shmOsdImageEntity.setShmOsdHeader(osdHeaderEntity);
		return shmOsdImageEntity;
	}

	public ShmMgmtRemark buildShmMgmtRemark(ShmOsdHeader osdHeaderEntity, ShmShipment shmShipment,
			OsdManagementRemark osdManagementRemark, AuditInfo auditInfo, EntityManager entityManager) {

		ShmMgmtRemark shmMgmtRemark = new ShmMgmtRemark();

		shipmentManagementRemarkDAO.setRemarkId(shmMgmtRemark, SHM_MGMT_REMARK_SEQ, entityManager);
		shmMgmtRemark.setShmShipment(shmShipment);
		shmMgmtRemark.setShmOsdHeader(osdHeaderEntity);
		shmMgmtRemark.setMvmtExcpSeqNbr(BigDecimal.ZERO);
		shmMgmtRemark.setMvmtSeqNbr(BigDecimal.ZERO);
		shmMgmtRemark.setRecordVersionNbr(BasicTransformer.toLong(BigInteger.ZERO));
		shmMgmtRemark.setRemarkTxt(osdManagementRemark.getRemark());
		shmMgmtRemark.setTypeCd(
				ShipmentManagementRemarkTypeCdTransformer.toCode(ShipmentManagementRemarkTypeCd.OSD_REMARKS));
		DtoTransformer.setAuditInfo(shmMgmtRemark, auditInfo);
		return shmMgmtRemark;
	}

	private String getOsdNumber(Long osdId, String reportingSicCd) {
		String osdNumberTxt = null;

		if (reportingSicCd != null && osdId != null) {
			osdNumberTxt = reportingSicCd.concat(HYPEN).concat(osdId.toString());
		}
		return osdNumberTxt;
	}

}
