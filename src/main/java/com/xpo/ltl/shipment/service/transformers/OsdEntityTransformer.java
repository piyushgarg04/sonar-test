package com.xpo.ltl.shipment.service.transformers;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.humanresource.v2.InterfaceEmployee;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnitMvmt;
import com.xpo.ltl.api.shipment.service.entity.ShmMgmtRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmMovementExcp;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdHeader;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdImage;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.BillClassCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.CloseReasonCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.ConeColorCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.DeliveryQualifierCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.HandlingUnitTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.IdentifiedLocationTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementExceptionTypeCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.MovementStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OsdCategoryCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OsdStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OtherOsdReasonCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OverageApprovalStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.RefusedReasonCdTransformer;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.DeliveryQualifierCd;
import com.xpo.ltl.api.shipment.v2.MovementExceptionTypeCd;
import com.xpo.ltl.api.shipment.v2.MovementStatusCd;
import com.xpo.ltl.api.shipment.v2.OsdCategoryCd;
import com.xpo.ltl.api.shipment.v2.OsdChildShipment;
import com.xpo.ltl.api.shipment.v2.OsdHeader;
import com.xpo.ltl.api.shipment.v2.OsdImage;
import com.xpo.ltl.api.shipment.v2.OsdManagementRemark;
import com.xpo.ltl.api.shipment.v2.OsdParentShipment;
import com.xpo.ltl.api.shipment.v2.OsdPayloadTypeCd;
import com.xpo.ltl.api.shipment.v2.OverageApprovalStatusCd;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.impl.UpsertOsdImpl;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;

public class OsdEntityTransformer extends BasicTransformer {

	private static final String MOVEMENT_STATUS_CD_FINAL_DELIVERED = MovementStatusCdTransformer
			.toCode(MovementStatusCd.FINAL_DLVD);
	private static final String MOVEMENT_STATUS_CD_INTERIM_DLVRY = MovementStatusCdTransformer
			.toCode(MovementStatusCd.INTERIM_DLVRY);
	private static final String MOVEMENT_EXCEPTION_TYPE_CD_REFUSED = MovementExceptionTypeCdTransformer
			.toCode(MovementExceptionTypeCd.REFUSED);
	private static final String MOVEMENT_EXCEPTION_TYPE_CD_DAMAGED = MovementExceptionTypeCdTransformer
			.toCode(MovementExceptionTypeCd.DAMAGED);
	private static final String MOVEMENT_EXCEPTION_TYPE_CD_SHORT = MovementExceptionTypeCdTransformer
			.toCode(MovementExceptionTypeCd.SHORT);
	private static final String MOVEMENT_EXCEPTION_TYPE_CD_OVER = MovementExceptionTypeCdTransformer
			.toCode(MovementExceptionTypeCd.OVER);
	private static final String DELIVERY_QUALIFIER_CD_FINAL = DeliveryQualifierCdTransformer
			.toCode(DeliveryQualifierCd.FINAL);
	private static final String CHILD_SHIPMENT_STATUS_CD_DELIVERED = "Delivered";
	private static final String MVMT_TYPE_CD_DELIVER = "DELIVER";
	private static final List<String> closedStatusCds = Arrays.asList("d_closed", "o_closed", "r_closed", "s_closed","ot_closed");
	private static final Log LOG = LogFactory.getLog(UpsertOsdImpl.class);
	private static final String DELIVERY_QUALIFIER_CD_REFUSED = DeliveryQualifierCdTransformer
			.toCode(DeliveryQualifierCd.REFUSED);
	private static final String DELIVERY_QUALIFIER_CD_DAMAGED = DeliveryQualifierCdTransformer
			.toCode(DeliveryQualifierCd.DAMAGED);
	private static final String DELIVERY_QUALIFIER_CD_PARTIAL_SHORT = DeliveryQualifierCdTransformer
			.toCode(DeliveryQualifierCd.PARTIAL_SHORT);
	private static final String DELIVERY_QUALIFIER_CD_ALL_SHORT = DeliveryQualifierCdTransformer
			.toCode(DeliveryQualifierCd.ALL_SHORT);
	private static final String DELIVERY_QUALIFIER_CD_OVER_SHPMT = DeliveryQualifierCdTransformer
			.toCode(DeliveryQualifierCd.OVER_SHPMT);
	private static final List<String> DELIVERY_QUALIFIER_CDS_REFUSED_DAMAGED_SHORT_OVERAGE = Arrays.asList(
			DELIVERY_QUALIFIER_CD_REFUSED, DELIVERY_QUALIFIER_CD_DAMAGED, DELIVERY_QUALIFIER_CD_PARTIAL_SHORT,
			DELIVERY_QUALIFIER_CD_ALL_SHORT, DELIVERY_QUALIFIER_CD_OVER_SHPMT);
	private static final List<OsdCategoryCd> notOverageCategoryCds = Arrays.asList(OsdCategoryCd.DAMAGED, OsdCategoryCd.REFUSED, OsdCategoryCd.SHORT);
	private static final String MOBILE_SC_PGM_ID = "mobilesc";
	private static final List<OverageApprovalStatusCd> hasApprovalStatusCds  = Arrays.asList(OverageApprovalStatusCd.APPROVED, OverageApprovalStatusCd.DISAPPROVED);

	public static List<OsdChildShipment> buildOsdChildShipments(ShmShipment shmShipment, ShmOsdHeader shmOsdHeader,
			OsdPayloadTypeCd osdPayloadTypeCd, String proNbr) {

		List<OsdChildShipment> osdChildShipments = new ArrayList<>();
		Set<String> proNumbersExistInOsdImage = new HashSet<>();
		Set<String> proNumbersAddedInOsdChildShipmentSet = new HashSet<>();
		
		if (Objects.nonNull(shmOsdHeader) && Objects.nonNull(shmOsdHeader.getShmOsdImages())) {
		    if (osdPayloadTypeCd.equals(OsdPayloadTypeCd.SHM_APP) || 
		        (osdPayloadTypeCd.equals(OsdPayloadTypeCd.HH) 
		        		|| osdPayloadTypeCd.equals(OsdPayloadTypeCd.EDGE_OSD)) 
		        && (StringUtils.isBlank(proNbr) || 
		        (StringUtils.isNotBlank(proNbr) 
		        		&& !closedStatusCds.contains(shmOsdHeader.getStatusCd().toLowerCase())))) {
		        proNumberExistInOsdImage(shmOsdHeader, proNumbersExistInOsdImage);
		    }
		}

		if (Objects.nonNull(shmShipment) && Objects.nonNull(shmShipment.getShmHandlingUnits())) {
			for (ShmHandlingUnit handlingUnit : shmShipment.getShmHandlingUnits()) {
				List<ShmHandlingUnitMvmt> handlingUnitMvmts = handlingUnit.getShmHandlingUnitMvmts();
				OsdChildShipment osdChildShipment = new OsdChildShipment();
				osdChildShipment.setChildProNbr(handlingUnit.getChildProNbrTxt());
				osdChildShipment.setWeightLbs(BasicTransformer.toDouble(handlingUnit.getWgtLbs()));
				osdChildShipment.setHandlingUnitTypeCd(HandlingUnitTypeCdTransformer.toEnum(handlingUnit.getTypeCd()));
				osdChildShipment.setInOsdBayInd(
						proNumbersExistInOsdImage.contains(handlingUnit.getChildProNbrTxt()) ? Boolean.TRUE
								: Boolean.FALSE);
				if (MOVEMENT_STATUS_CD_FINAL_DELIVERED.equalsIgnoreCase(handlingUnit.getMvmtStatCd())) {
					osdChildShipment.setStatusCd(CHILD_SHIPMENT_STATUS_CD_DELIVERED);
				} else if (MOVEMENT_STATUS_CD_INTERIM_DLVRY.equalsIgnoreCase(handlingUnit.getMvmtStatCd())
						&& Objects.nonNull(handlingUnitMvmts)) {
					ShmHandlingUnitMvmt shmHandlingUnitMvmt = CollectionUtils.emptyIfNull(handlingUnitMvmts).stream()
							.filter(hum -> StringUtils.isNotEmpty(hum.getMvmtTypCd())
									&& hum.getMvmtTypCd().equalsIgnoreCase(MVMT_TYPE_CD_DELIVER))
							.sorted(Comparator.comparing(ShmHandlingUnitMvmt::getCrteTmst).reversed()).findFirst()
							.orElse(null);
					if (Objects.nonNull(shmHandlingUnitMvmt)
							&& StringUtils.isNotEmpty(shmHandlingUnitMvmt.getExcpTypCd())) {
						osdChildShipment.setStatusCd(MovementExceptionTypeCdTransformer.getLabel(
								MovementExceptionTypeCdTransformer.toEnum(shmHandlingUnitMvmt.getExcpTypCd())));
					}
				}

				if (!osdPayloadTypeCd.equals(OsdPayloadTypeCd.HH)) {
					osdChildShipment.setHeightNbr(BasicTransformer.toDouble(handlingUnit.getHeightNbr()));
					osdChildShipment.setLengthNbr(BasicTransformer.toDouble(handlingUnit.getLengthNbr()));
					osdChildShipment.setWidthNbr(BasicTransformer.toDouble(handlingUnit.getWidthNbr()));
				}
				proNumbersAddedInOsdChildShipmentSet.add(handlingUnit.getChildProNbrTxt());
				osdChildShipments.add(osdChildShipment);

			}
		}

		if (Objects.nonNull(shmOsdHeader) && Objects.nonNull(shmOsdHeader.getShmOsdImages())) {
			for (ShmOsdImage osdImage : shmOsdHeader.getShmOsdImages()) {
				if (Objects.nonNull(osdImage.getProNbrTxt())
						&& proNumbersAddedInOsdChildShipmentSet.add(osdImage.getProNbrTxt())
						&& (shmOsdHeader.getOsdCategoryCd().equalsIgnoreCase(OsdCategoryCd.OVERAGE.toString())
								|| (shmOsdHeader.getOsdCategoryCd()
										.equalsIgnoreCase(OsdCategoryCd.OTHER.toString())))) {
					OsdChildShipment osdChildShipment = new OsdChildShipment();
					osdChildShipment.setChildProNbr(osdImage.getProNbrTxt());
					osdChildShipment.setInOsdBayInd(Boolean.TRUE);
					if (!osdPayloadTypeCd.equals(OsdPayloadTypeCd.HH)) {
						osdChildShipment.setHeightNbr(BasicTransformer.toDouble(osdImage.getHghtNbr()));
						osdChildShipment.setLengthNbr(BasicTransformer.toDouble(osdImage.getLenNbr()));
						osdChildShipment.setWidthNbr(BasicTransformer.toDouble(osdImage.getWdthNbr()));
						osdChildShipment.setWeightLbs(BasicTransformer.toDouble(osdImage.getWgtLbs()));
						if (shmOsdHeader.getOsdCategoryCd().equalsIgnoreCase(OsdCategoryCd.OVERAGE.toString()))
							osdChildShipment.setStatusCd(OsdCategoryCd.OVERAGE.toString());
						else
							osdChildShipment.setStatusCd(OsdCategoryCd.OTHER.toString());
					}
					osdChildShipments.add(osdChildShipment);
				}

			}
		}

		return osdChildShipments;
	}

	private static void proNumberExistInOsdImage(ShmOsdHeader shmOsdHeader, Set<String> proNumbersExistInOsdImage) {
		List<ShmOsdImage> osdImages = shmOsdHeader.getShmOsdImages();
		if (Objects.nonNull(osdImages)) {
		    for (ShmOsdImage osdImage : osdImages) {
		        if (Objects.nonNull(osdImage) && Objects.nonNull(osdImage.getProNbrTxt())) {
		            proNumbersExistInOsdImage.add(osdImage.getProNbrTxt());
		        }
		    }
		}
	}

	public static OsdParentShipment buildOsdParentShipment(ShmOsdHeader shmOsdHeader,
			ShmShipment shmShipment, ShmMovementExcp shmMovementExcp, Boolean isLegacyPro,
			OsdPayloadTypeCd osdPayloadTypeCd, Map<String, InterfaceEmployee> employeeDetailsMap,
			ShmShipment newParentShmShipment, String proNbr, final TransactionContext txnContext) throws ValidationException {

		OsdParentShipment osdParentShipment = new OsdParentShipment();
		
		if (Objects.nonNull(shmOsdHeader)
				&& osdPayloadTypeCd.equals(OsdPayloadTypeCd.SHM_APP)) {
			populateOsdHeader(shmOsdHeader, isLegacyPro, osdPayloadTypeCd, employeeDetailsMap,
					osdParentShipment);
		} 
		//CCS-10529:ProNbr isNotBlank, StatusCd is Closed and osdPayloadTypeCds are HH and EDGE_OSD
		if (Objects.nonNull(shmOsdHeader)
				&& StringUtils.isNotBlank(proNbr)
				&& shmOsdHeader.getStatusCd() != null
				&& (osdPayloadTypeCd.equals(OsdPayloadTypeCd.HH)
				|| osdPayloadTypeCd.equals(OsdPayloadTypeCd.EDGE_OSD))) {
			if(!closedStatusCds.contains(shmOsdHeader.getStatusCd().toLowerCase())) {
				populateOsdHeader(shmOsdHeader, isLegacyPro, osdPayloadTypeCd, employeeDetailsMap,
						osdParentShipment);
			} else {
				setOsdCategoryCd(shmShipment, isLegacyPro, osdParentShipment);
			}
		}
		
		//CCS-10529:ProNbr isBlank and osdPayloadTypeCds are HH and EDGE_OSD
		if (Objects.nonNull(shmOsdHeader)
				&& StringUtils.isBlank(proNbr)
				&& (osdPayloadTypeCd.equals(OsdPayloadTypeCd.HH)
				|| osdPayloadTypeCd.equals(OsdPayloadTypeCd.EDGE_OSD))) {
			populateOsdHeader(shmOsdHeader, isLegacyPro, osdPayloadTypeCd, employeeDetailsMap,
					osdParentShipment);
		}

		if (Objects.nonNull(shmShipment)) {
			osdParentShipment.setShipmentInstId(shmShipment.getShpInstId());
			osdParentShipment.setPiecesCount(BasicTransformer.toBigInteger(shmShipment.getTotPcsCnt()));
			osdParentShipment.setTotalMotorMovesCount(BasicTransformer.toBigInteger(shmShipment.getMtrzdPcsCnt()));
			osdParentShipment.setTotalLoosePiecesCount(BasicTransformer.toBigInteger(shmShipment.getLoosePcsCnt()));
			osdParentShipment.setTotalWeight(BasicTransformer.toDouble(shmShipment.getTotWgtLbs()));
			if (isLegacyPro && Objects.nonNull(shmMovementExcp)) {
				if (MOVEMENT_EXCEPTION_TYPE_CD_REFUSED.equals(shmMovementExcp.getTypCd())) {
					osdParentShipment.setRefusedPiecesCount(BasicTransformer.toBigInteger(shmMovementExcp.getPcsCnt()));
				} else if (MOVEMENT_EXCEPTION_TYPE_CD_DAMAGED.equals(shmMovementExcp.getTypCd())) {
					osdParentShipment
							.setRefusedDamagedPiecesCount(BasicTransformer.toBigInteger(shmMovementExcp.getPcsCnt()));
				}
			}
			if (Objects.isNull(shmOsdHeader)) {
				setOsdCategoryCd(shmShipment, isLegacyPro, osdParentShipment);
			}

			if (StringUtils.isNotEmpty(shmShipment.getDlvryQalfrCd())) {
				osdParentShipment
						.setDeliveryQualifierCd(DeliveryQualifierCdTransformer.toEnum(shmShipment.getDlvryQalfrCd()));
			}
			
			//CCS-9806: Add new field to getOSD API to retrieve Hazmat details from SHM_SHIPMENT table
			if (Objects.nonNull(shmOsdHeader)
					&& Objects.nonNull(shmOsdHeader.getOsdCategoryCd())
					&& shmOsdHeader.getOsdCategoryCd().equalsIgnoreCase(OsdCategoryCd.OTHER.toString())
					&& StringUtils.isNotEmpty(shmShipment.getHazmatInd())
					&& !osdPayloadTypeCd.equals(OsdPayloadTypeCd.HH)) {
				osdParentShipment.setHazmatInd(BasicTransformer.toBoolean(shmShipment.getHazmatInd()));
			}
		}
		if(Objects.nonNull(newParentShmShipment)
			&& StringUtils.isNotBlank(newParentShmShipment.getBillClassCd()) ) {
				osdParentShipment.setNewBillClassCd(BillClassCdTransformer.toEnum(newParentShmShipment.getBillClassCd()));
		}
		return osdParentShipment;
	}

	private static void populateOsdHeader(ShmOsdHeader shmOsdHeader, Boolean isLegacyPro,
			OsdPayloadTypeCd osdPayloadTypeCd, Map<String, InterfaceEmployee> employeeDetailsMap,
			OsdParentShipment osdParentShipment) {
		osdParentShipment.setOsdId(shmOsdHeader.getOsdId());

		if (Objects.nonNull(shmOsdHeader.getOsdCategoryCd())) {
			osdParentShipment.setOsdCategoryCd(OsdCategoryCdTransformer.toEnum(shmOsdHeader.getOsdCategoryCd()));
		}
		if (Objects.nonNull(shmOsdHeader.getConeColorCd())) {
			osdParentShipment.setConeColorCd(ConeColorCdTransformer.toEnum(shmOsdHeader.getConeColorCd()));
		}
		if (Objects.nonNull(shmOsdHeader.getConeNbr())) {
			osdParentShipment.setConeNbr(BasicTransformer.toBigInteger(shmOsdHeader.getConeNbr()));
		}
		osdParentShipment.setParentProNbr(shmOsdHeader.getProNbrTxt());
		if (Objects.nonNull(shmOsdHeader.getStatusCd())) {
			osdParentShipment.setStatusCd(OsdStatusCdTransformer.toEnum(shmOsdHeader.getStatusCd()));
		}
		if(Objects.nonNull(shmOsdHeader.getNewParentProNbrTxt())) {
			osdParentShipment.setNewParentProNbr(shmOsdHeader.getNewParentProNbrTxt());
		}
		if(Objects.nonNull(shmOsdHeader.getOtherReasonCd())) {
			osdParentShipment.setOtherOsdReasonCd(OtherOsdReasonCdTransformer.toEnum(shmOsdHeader.getOtherReasonCd()));
		}
		if(Objects.nonNull(shmOsdHeader.getLastSicCd())) {
			osdParentShipment.setLastClosedSicCd(shmOsdHeader.getLastSicCd());
		}

		//CCS-9259: Add New OS&D "Estimated Value" Field
		if (Objects.nonNull(shmOsdHeader.getEstimatedValue())) {
			osdParentShipment.setEstimatedValue(shmOsdHeader.getEstimatedValue().doubleValue());
		}
		
		if (isLegacyPro) {
			osdParentShipment.setOsdPiecesCount(BasicTransformer.toBigInteger(shmOsdHeader.getHuCnt()));
		} else {
			Long osdChildProCount = CollectionUtils.emptyIfNull(shmOsdHeader.getShmOsdImages()).stream()
					.filter(osdImage -> StringUtils.isNotEmpty(osdImage.getProNbrTxt()))
					.map(osdImage -> osdImage.getProNbrTxt()).distinct().count();
			if (null != osdChildProCount) {
				osdParentShipment.setOsdPiecesCount(BigInteger.valueOf(osdChildProCount));
			}
		}

		ShmOsdImage shmOsdImageHasImageInformation = null;
				
		if (shmOsdHeader.getOsdCategoryCd().equalsIgnoreCase(OsdCategoryCd.OVERAGE.toString())) {
			shmOsdImageHasImageInformation = CollectionUtils.emptyIfNull(shmOsdHeader.getShmOsdImages())
			.stream().findFirst().orElse(null);
		} else {
			shmOsdImageHasImageInformation = CollectionUtils.emptyIfNull(shmOsdHeader.getShmOsdImages())
			.stream().filter(osdImage -> StringUtils.isEmpty(osdImage.getProNbrTxt())).findFirst().orElse(null);
		}
		
		if (null != shmOsdImageHasImageInformation) {
			if (StringUtils.isNotBlank(shmOsdImageHasImageInformation.getDmsUrl())) {
				String[] dmsUrlArrays = shmOsdImageHasImageInformation.getDmsUrl().split(",");
				List<String> dmsUrls = new ArrayList<>();
				for (String dmsUrl : dmsUrlArrays) {
					dmsUrls.add(dmsUrl);
				}
				osdParentShipment.setDmsUrls(dmsUrls);
			}

			if (shmOsdHeader.getOsdCategoryCd().equalsIgnoreCase(OsdCategoryCd.OVERAGE.toString())) {
				osdParentShipment.setOverageProNbr(shmOsdImageHasImageInformation.getProNbrTxt());
				if(Objects.nonNull(shmOsdImageHasImageInformation.getApprovalStatusCd())) {
					osdParentShipment.setApprovalStatusCd(OverageApprovalStatusCdTransformer.toEnum(shmOsdImageHasImageInformation.getApprovalStatusCd()));
				}
				osdParentShipment.setParentProNbr(shmOsdHeader.getProNbrTxt());
				osdParentShipment.setOsdPiecesCount(
						BasicTransformer.toBigInteger(shmOsdImageHasImageInformation.getPcsCnt()));
				osdParentShipment
						.setWeightLbs(BasicTransformer.toDouble(shmOsdImageHasImageInformation.getWgtLbs()));
				osdParentShipment
						.setLengthNbr(BasicTransformer.toDouble(shmOsdImageHasImageInformation.getLenNbr()));
				osdParentShipment
						.setHeightNbr(BasicTransformer.toDouble(shmOsdImageHasImageInformation.getHghtNbr()));
				osdParentShipment
						.setWidthNbr(BasicTransformer.toDouble(shmOsdImageHasImageInformation.getWdthNbr()));
		        osdParentShipment
		                .setPackageCd(shmOsdImageHasImageInformation.getPkgCd());

				if (!osdPayloadTypeCd.equals(OsdPayloadTypeCd.HH)) {
					osdParentShipment.setOveragePairedWithShortInd(
							BasicTransformer.toBoolean(shmOsdImageHasImageInformation.getOverPairedWithShortInd()));
					osdParentShipment.setReferenceNbr(shmOsdImageHasImageInformation.getReferenceNbrTxt());
					osdParentShipment.setBrand(shmOsdImageHasImageInformation.getBrand());
					osdParentShipment.setSelectedTags(shmOsdImageHasImageInformation.getSelectedTags());
				}
				//CCS-9402: Add "color" field to get OSD API
				if(Objects.nonNull(shmOsdImageHasImageInformation.getColor())) {
					osdParentShipment.setColor(shmOsdImageHasImageInformation.getColor());
				}
			}
		}

		osdParentShipment.setIdentifiedLocationTypeCd(
		        IdentifiedLocationTypeCdTransformer.toEnum(shmOsdHeader.getIdentifiedLocTypeCd()));

		osdParentShipment.setIdentifiedLocationId(shmOsdHeader.getIdentifiedLocId());

		if (!osdPayloadTypeCd.equals(OsdPayloadTypeCd.HH)) {
			osdParentShipment.setOsdNumber(shmOsdHeader.getOsdNumberTxt());
			osdParentShipment.setArriveAtOsdDateTime(
					BasicTransformer.toXMLGregorianCalendar(shmOsdHeader.getArriveAtOsdTmst()));

			if (Objects.nonNull(employeeDetailsMap)) {
				if (Objects.nonNull(employeeDetailsMap.get(shmOsdHeader.getDockWorkerUserid()))) {
					String name = getEmployeeName(employeeDetailsMap, shmOsdHeader.getDockWorkerUserid());
					osdParentShipment.setDockWorkerUserName(BasicTransformer.toTrimmedString(name));
				}
				if (Objects.nonNull(employeeDetailsMap.get(shmOsdHeader.getAssignedUser()))) {
					String name = getEmployeeName(employeeDetailsMap, shmOsdHeader.getAssignedUser());
					osdParentShipment.setAssignedUserName(BasicTransformer.toTrimmedString(name));
				}
				if (Objects.nonNull(employeeDetailsMap.get(shmOsdHeader.getCrteBy()))) {
					String name = getEmployeeName(employeeDetailsMap, shmOsdHeader.getCrteBy());
					osdParentShipment.setCreatedByUserName(BasicTransformer.toTrimmedString(name));
				}
			}

			if (shmOsdHeader.getOsdCategoryCd().equalsIgnoreCase(OsdCategoryCd.REFUSED.toString())
					&& StringUtils.isNotBlank(shmOsdHeader.getRefusedReasonCd())) {
				osdParentShipment
						.setRefusedReasonCd(RefusedReasonCdTransformer.toEnum(shmOsdHeader.getRefusedReasonCd()));
			}
			osdParentShipment.setDockWorkerUserId(shmOsdHeader.getDockWorkerUserid());
			osdParentShipment.setAssignedUserId(shmOsdHeader.getAssignedUser());
			osdParentShipment.setCreatedByUserId(shmOsdHeader.getCrteBy());
			osdParentShipment.setReportingSicCd(shmOsdHeader.getReportingSicCd());
			osdParentShipment.setCloseReasonCd(CloseReasonCdTransformer.toEnum(shmOsdHeader.getCloseReasonCd()));
			osdParentShipment.setOsdDescription(shmOsdHeader.getOsdDescription());
			osdParentShipment
					.setCloseOsdDateTime(BasicTransformer.toXMLGregorianCalendar(shmOsdHeader.getOsdCloseTmst()));
		}
	}

	public static List<OsdManagementRemark> buildOsdManagementRemarks(Map<String, InterfaceEmployee> employeeDetailsMap,
			ShmOsdHeader shmOsdHeader, OsdPayloadTypeCd osdPayloadTypeCd, String proNbr) {
		List<OsdManagementRemark> osdManagementRemarks = new ArrayList<>();
		if (Objects.nonNull(shmOsdHeader) && Objects.nonNull(shmOsdHeader.getShmMgmtRemarks())) {
		    if (osdPayloadTypeCd.equals(OsdPayloadTypeCd.SHM_APP)) {
		        populateOsdManagementRemark(employeeDetailsMap, shmOsdHeader, osdPayloadTypeCd, osdManagementRemarks);
		    } else if (osdPayloadTypeCd.equals(OsdPayloadTypeCd.HH) || osdPayloadTypeCd.equals(OsdPayloadTypeCd.EDGE_OSD)) {
		        if (StringUtils.isBlank(proNbr) || 
		            (StringUtils.isNotBlank(proNbr) && !closedStatusCds.contains(shmOsdHeader.getStatusCd().toLowerCase()))) {
		            populateOsdManagementRemark(employeeDetailsMap, shmOsdHeader, osdPayloadTypeCd, osdManagementRemarks);
		        }
		    }
		}
		return osdManagementRemarks;
	}
	
	private static void populateOsdManagementRemark(Map<String, InterfaceEmployee> employeeDetailsMap,
			ShmOsdHeader shmOsdHeader, OsdPayloadTypeCd osdPayloadTypeCd,
			List<OsdManagementRemark> osdManagementRemarks) {
		for (ShmMgmtRemark shmMgmtRemark : shmOsdHeader.getShmMgmtRemarks()) {
		    OsdManagementRemark osdManagementRemark = new OsdManagementRemark();
		    
		    if (Objects.nonNull(shmMgmtRemark) && Objects.nonNull(shmMgmtRemark.getRemarkTxt())) {
		        osdManagementRemark.setRemark(shmMgmtRemark.getRemarkTxt());

		        if (Objects.nonNull(employeeDetailsMap)
		                && Objects.nonNull(employeeDetailsMap.get(shmMgmtRemark.getCrteBy()))) {
		            InterfaceEmployee employee = employeeDetailsMap.get(shmMgmtRemark.getCrteBy());
		            String name = getEmployeeName(employeeDetailsMap, shmMgmtRemark.getCrteBy());
		            osdManagementRemark.setCreatedByUserName(name);
		            osdManagementRemark.setCreatedByUserTitle(employee.getJobDescription());
		        }

		        if (!osdPayloadTypeCd.equals(OsdPayloadTypeCd.HH)) {
		            if (Objects.nonNull(shmMgmtRemark.getCrteBy())) {
		                osdManagementRemark.setCreatedById(shmMgmtRemark.getCrteBy());
		            }
		            if (Objects.nonNull(shmMgmtRemark.getCrteTmst())) {
		                osdManagementRemark.setCreatedTimestamp(
		                        BasicTransformer.toXMLGregorianCalendar(shmMgmtRemark.getCrteTmst()));
		            }
		        }
		        osdManagementRemarks.add(osdManagementRemark);
		    }
		}
	}

	private static String getEmployeeName(Map<String, InterfaceEmployee> getEmployeeDetailsMap, String employeeId) {
		InterfaceEmployee employee = getEmployeeDetailsMap.get(employeeId);
		String name = employee.getFirstName() + StringUtils.SPACE + employee.getLastName();
		return name;
	}

	/**
	 * Method to create OSD header request
	 * 
	 * @param osdParentShipment
	 * @param txnContext
	 * @return
	 * @throws ServiceException
	 * @throws DatatypeConfigurationException 
	 */
	public OsdHeader buildOsdHeaderRqst(OsdParentShipment osdParentShipment, TransactionContext txnContext)
			throws ServiceException {

		OsdHeader osdHeader = new OsdHeader();

		if (osdParentShipment != null) {
			if (osdParentShipment.getAssignedUserId() != null ) {
				osdHeader.setAssignedUser(osdParentShipment.getAssignedUserId());
			}
			
			//CCS-7890: SHM_OSD_HEADER.OSD_CLOSE_TMST to be updated to system date time only when status_cd is being set to closed for all OS&D category codes.
			if(osdParentShipment.getStatusCd() != null) {
				if(closedStatusCds.contains(OsdStatusCdTransformer.toString(osdParentShipment.getStatusCd()))) {
					if (osdParentShipment.getCloseReasonCd() != null) {
						osdHeader.setCloseReasonCd(CloseReasonCdTransformer.toCode(osdParentShipment.getCloseReasonCd()));
						osdHeader.setOsdCloseDateTime(BasicTransformer.toXMLGregorianCalendar(Calendar.getInstance()));
					}
					else {
						throw ExceptionBuilder
						.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
						.moreInfo("closeReasonCd", "Please provide a valid closeReasonCd to close an OSD item.")
						.build();
					}
				}
			}
			if (osdParentShipment.getConeColorCd() != null) {
				osdHeader.setConeColorCd(ConeColorCdTransformer.toCode(osdParentShipment.getConeColorCd()));
			}
			if (osdParentShipment.getConeNbr() != null) {
				osdHeader.setConeNbr(osdParentShipment.getConeNbr());
			}
			if (osdParentShipment.getOsdCategoryCd() != null) {
				osdHeader.setOsdCategoryCd(OsdCategoryCdTransformer.toCode(osdParentShipment.getOsdCategoryCd()));
			}
			if (osdParentShipment.getDockWorkerUserId() != null) {
				osdHeader.setDockWorkerUserId(osdParentShipment.getDockWorkerUserId());
			}
			if (osdParentShipment.getIdentifiedLocationId() != null) {
				osdHeader.setIdentifiedLocationId(osdParentShipment.getIdentifiedLocationId());
			}
			if (osdParentShipment.getIdentifiedLocationTypeCd() != null) {
				osdHeader.setIdentifiedLocationTypeCd(
						IdentifiedLocationTypeCdTransformer.toCode(osdParentShipment.getIdentifiedLocationTypeCd()));
			}
			if (osdParentShipment.getShipmentInstId() != null) {
				osdHeader.setShipmentInstId(osdParentShipment.getShipmentInstId());
			}
			if (osdParentShipment.getOsdDescription() != null) {
				osdHeader.setOsdDescription(osdParentShipment.getOsdDescription());
			}
			if (osdParentShipment.getParentProNbr() != null) {
				osdHeader.setProNbr(handleProNum(osdParentShipment.getParentProNbr(), txnContext));
			}
			if (osdParentShipment.getReportingSicCd() != null) {
				osdHeader.setReportingSicCd(osdParentShipment.getReportingSicCd());
			}
			if (osdParentShipment.getRefusedReasonCd() != null) {
				osdHeader.setRefusedReasonCd(RefusedReasonCdTransformer.toCode(osdParentShipment.getRefusedReasonCd()));
			}
			if (Objects.nonNull(osdParentShipment.getStatusCd())) {
				osdHeader.setStatusCd(OsdStatusCdTransformer.toCode(osdParentShipment.getStatusCd()));
			}
		}

		return osdHeader;
	}

	private String handleProNum(String proNumber, TransactionContext txnContext) throws ServiceException {
		try {
			String elevenDigitProNum = ProNumberHelper.validateProNumber(proNumber, txnContext);
			if (StringUtils.isNotBlank(elevenDigitProNum)) {
				if (ProNumberHelper.isYellowPro(elevenDigitProNum)) {
					return ProNumberHelper.isValidChildProNum(elevenDigitProNum);
				}
				if (ProNumberHelper.isBluePro(elevenDigitProNum)) {
					return ProNumberHelper.formatProNineDigit(elevenDigitProNum);
				}
			}
		} catch (ServiceException ex) {
			throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
					.moreInfo("proNumber", "Invalid PRO Number").build();
		}
		return null;
	}

	public OsdImage buildImageRqstForChildPros(OsdChildShipment osdChildShipment, ShmOsdHeader osdHeaderEntity, OsdParentShipment osdParentShipment,
			AuditInfo auditInfo, TransactionContext txnContext) throws ValidationException {

		OsdImage osdImageRqst = new OsdImage();

		AuditInfoHelper.setCreatedInfo(auditInfo, txnContext);
		AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);
		osdImageRqst.setAuditInfo(auditInfo);

		if (osdParentShipment != null) {
			if (osdParentShipment.getReportingSicCd() != null) {
				osdImageRqst.setReportingSicCd(osdParentShipment.getReportingSicCd());
			}
			else {
				osdImageRqst.setReportingSicCd(osdHeaderEntity.getReportingSicCd());	
			}

			if (osdParentShipment.getOsdCategoryCd() != null) {
				osdImageRqst.setOsdCategoryCd(OsdCategoryCdTransformer.toCode(osdParentShipment.getOsdCategoryCd()));
			}
			else {
				osdImageRqst.setOsdCategoryCd(osdHeaderEntity.getOsdCategoryCd());
			}
			
			if (Objects.nonNull(osdParentShipment.getStatusCd())) {
				osdImageRqst.setStatusCd(OsdStatusCdTransformer.toCode(osdParentShipment.getStatusCd()));
			}
			else {
				osdImageRqst.setStatusCd(osdHeaderEntity.getStatusCd());
			}

			if (osdParentShipment.getSelectedTags() != null) {
				osdImageRqst.setSelectedTags(osdParentShipment.getSelectedTags());
			}
			if (osdParentShipment.getParentProNbr() != null) {
				osdImageRqst.setOriginProNbr(osdParentShipment.getParentProNbr());
			}
			else {
				osdImageRqst.setOriginProNbr(osdHeaderEntity.getProNbrTxt());
			}
		}
		else {
			if (osdHeaderEntity.getReportingSicCd() != null) {
				osdImageRqst.setReportingSicCd(osdHeaderEntity.getReportingSicCd());	
			}

			if (osdHeaderEntity.getOsdCategoryCd() != null) {
				osdImageRqst.setOsdCategoryCd(osdHeaderEntity.getOsdCategoryCd());
			}
			
			if (Objects.nonNull(osdHeaderEntity.getStatusCd())) {
				osdImageRqst.setStatusCd(osdHeaderEntity.getStatusCd());
			}

			if (osdHeaderEntity.getProNbrTxt() != null) {
				osdImageRqst.setOriginProNbr(osdHeaderEntity.getProNbrTxt());
			}
		}

		if (osdChildShipment != null) {
			if (StringUtils.isNotBlank(osdChildShipment.getChildProNbr())) {
				try {
					String elevenDigitProNum = ProNumberHelper.toElevenDigitPro(osdChildShipment.getChildProNbr(), txnContext);
					osdImageRqst.setProNbr(elevenDigitProNum);
				} catch (ServiceException ex) {
					throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
							.moreInfo("childProNumber", "Invalid child PRO Number").build();
				}
			}
		}

		osdImageRqst.setOverPairedWithShortInd(false);

		return osdImageRqst;
	}

	public OsdImage buildImageRqstForOveragePros(OsdParentShipment osdParentShipment, ShmOsdImage shmOsdImageEntity, String dmsUrl,
			String thumbnailImage, ActionCd actionCd, AuditInfo auditInfo, TransactionContext txnContext) throws ValidationException {

		OsdImage osdImageRqst = new OsdImage();

		AuditInfoHelper.setCreatedInfo(auditInfo, txnContext);
		AuditInfoHelper.setUpdatedInfo(auditInfo, txnContext);
		osdImageRqst.setAuditInfo(auditInfo);

		if (osdParentShipment != null) {
			if (osdParentShipment.getReportingSicCd() != null) {
				osdImageRqst.setReportingSicCd(osdParentShipment.getReportingSicCd());
			}
			if (osdParentShipment.getOverageProNbr() != null) {
				try {
					String elevenDigitProNum = ProNumberHelper.toElevenDigitPro(osdParentShipment.getOverageProNbr(), txnContext);
					osdImageRqst.setProNbr(elevenDigitProNum);
				} catch (ServiceException ex) {
					throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
							.moreInfo("overageProNumber", "Invalid overage PRO Number").build();
				}
			}

			//CCS-8394:Set APPROVAL_STATUS for Overage OSD type
			if(actionCd == ActionCd.ADD) {
				if(osdParentShipment.getOsdCategoryCd() != null
						&& osdParentShipment.getOsdCategoryCd().equals(OsdCategoryCd.OVERAGE)
						&& (auditInfo.getCreateByPgmId() != null
								&& StringUtils.equalsIgnoreCase(MOBILE_SC_PGM_ID, auditInfo.getCreateByPgmId()))) {
					osdImageRqst.setApprovalStatusCd(OverageApprovalStatusCd.NEEDS_APPROVAL);
					osdParentShipment.setApprovalStatusCd(OverageApprovalStatusCd.NEEDS_APPROVAL);
				} else if(osdParentShipment.getApprovalStatusCd() != null){
					osdImageRqst.setApprovalStatusCd(osdParentShipment.getApprovalStatusCd());
				}
			}
			
			if(actionCd == ActionCd.UPDATE) {

				if (osdParentShipment.getApprovalStatusCd() != null) {

					if (shmOsdImageEntity.getOsdCategoryCd() != null
							&& shmOsdImageEntity.getOsdCategoryCd().equalsIgnoreCase(OsdCategoryCdTransformer.toString(OsdCategoryCd.OVERAGE))
							&& hasApprovalStatusCds.contains(osdParentShipment.getApprovalStatusCd())) {

						osdImageRqst.setApprovalStatusCd(osdParentShipment.getApprovalStatusCd());

					} else if (osdParentShipment.getApprovalStatusCd().equals(OverageApprovalStatusCd.NEEDS_APPROVAL)
							&& hasApprovalStatusCds.contains(OverageApprovalStatusCdTransformer.toEnum(shmOsdImageEntity.getApprovalStatusCd()))) {

						throw ExceptionBuilder
								.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
								.moreInfo("approvalStatusCd", "Approval Status cannot be set back to “Needs Approval” for an approved/disapproved Overage OS&D entry.")
								.log()
								.build();

					}

				}

			}

			if (osdParentShipment.getParentProNbr() != null) {
				osdImageRqst.setOriginProNbr(osdParentShipment.getParentProNbr());
			}
			if (osdParentShipment.getBrand() != null) {
				osdImageRqst.setBrand(osdParentShipment.getBrand());
			}
			if (osdParentShipment.getPackageCd() != null) {
				osdImageRqst.setPackageCd(osdParentShipment.getPackageCd());
			}
			if (osdParentShipment.getOsdCategoryCd() != null) {
				osdImageRqst.setOsdCategoryCd(OsdCategoryCdTransformer.toCode(osdParentShipment.getOsdCategoryCd()));
			}
			if (osdParentShipment.getOsdDescription() != null) {
				osdImageRqst.setDescription(osdParentShipment.getOsdDescription());
			}
			if (osdParentShipment.getHeightNbr() != null) {
				osdImageRqst.setHeightNbr(osdParentShipment.getHeightNbr());
			}
			if (osdParentShipment.getLengthNbr() != null) {
				osdImageRqst.setLengthNbr(osdParentShipment.getLengthNbr());
			}
			if (osdParentShipment.getWidthNbr() != null) {
				osdImageRqst.setWidthNbr(osdParentShipment.getWidthNbr());
			}
			if (osdParentShipment.getWeightLbs() != null) {
				osdImageRqst.setWeightLbs(osdParentShipment.getWeightLbs());
			}
			if (osdParentShipment.getPiecesCount() != null) {
				osdImageRqst.setPiecesCount(osdParentShipment.getPiecesCount());
			}
			if (osdParentShipment.getReferenceNbr() != null) {
				osdImageRqst.setReferenceNbr(osdParentShipment.getReferenceNbr());
			}
			if (osdParentShipment.getSelectedTags() != null) {
				osdImageRqst.setSelectedTags(osdParentShipment.getSelectedTags());
			}
			if (Objects.nonNull(osdParentShipment.getStatusCd())) {
				osdImageRqst.setStatusCd(OsdStatusCdTransformer.toCode(osdParentShipment.getStatusCd()));
			}
		}
		if (thumbnailImage != null) {
			osdImageRqst.setThumbnailImage(thumbnailImage);
		}
		if (dmsUrl != null) {
			osdImageRqst.setDmsUrl(dmsUrl);
		}

		return osdImageRqst;
	}

	public OsdImage buildImageRqstForParentPros(OsdParentShipment osdParentShipment, String dmsUrl,
			String thumbnailImage, AuditInfo auditInfo, TransactionContext txnContext) {

		OsdImage osdImageRqst = new OsdImage();

		osdImageRqst.setAuditInfo(auditInfo);

		if (osdParentShipment != null) {
			if (StringUtils.isNotBlank(osdParentShipment.getReportingSicCd())) {
				osdImageRqst.setReportingSicCd(osdParentShipment.getReportingSicCd());
			}
			if (StringUtils.isNotBlank(osdParentShipment.getParentProNbr())) {
				osdImageRqst.setOriginProNbr(osdParentShipment.getParentProNbr());
			}
			if (osdParentShipment.getOsdCategoryCd() != null) {
				osdImageRqst.setOsdCategoryCd(OsdCategoryCdTransformer.toCode(osdParentShipment.getOsdCategoryCd()));
			}
			if (Objects.nonNull(osdParentShipment.getStatusCd())) {
				osdImageRqst.setStatusCd(OsdStatusCdTransformer.toCode(osdParentShipment.getStatusCd()));
			}
			if (osdParentShipment.getSelectedTags() != null) { 
				osdImageRqst.setSelectedTags(osdParentShipment.getSelectedTags());
			}
			if (osdParentShipment.getOsdDescription() != null) {
				osdImageRqst.setDescription(osdParentShipment.getOsdDescription());
			}
		}
		if (thumbnailImage != null) {
			osdImageRqst.setThumbnailImage(thumbnailImage);
		}
		if (dmsUrl != null) {
			osdImageRqst.setDmsUrl(dmsUrl);
		}

		return osdImageRqst;
	}
	
	private static void setOsdCategoryCd(ShmShipment shmShipment, Boolean isLegacyPro,
			OsdParentShipment osdParentShipment) {
		// OsdCategoryCd not needs to set for Legacy Pro
		if (!isLegacyPro) {
			List<ShmHandlingUnitMvmt> latestHandlingUnitMvmtBySeqNBr = new ArrayList<>();
			Set<String> movementStatusCd = new HashSet<>();
			for (ShmHandlingUnit shmHandlingUnit : shmShipment.getShmHandlingUnits()) {
				if (!movementStatusCd.contains(shmHandlingUnit.getMvmtStatCd())) {
					movementStatusCd.add(shmHandlingUnit.getMvmtStatCd());
				}
				if (CollectionUtils.isNotEmpty(shmHandlingUnit.getShmHandlingUnitMvmts())) {
					ShmHandlingUnitMvmt shmHandlingUnitMvmt = CollectionUtils.emptyIfNull(shmHandlingUnit.getShmHandlingUnitMvmts()).stream().
							filter(huMvmt -> StringUtils.isNotEmpty(huMvmt.getMvmtTypCd()) && huMvmt.getMvmtTypCd().equalsIgnoreCase(MVMT_TYPE_CD_DELIVER))
							.sorted(Comparator.comparing(ShmHandlingUnitMvmt::getCrteTmst).reversed()).findFirst()
							.orElse(null);
					if (null != shmHandlingUnitMvmt) {
						latestHandlingUnitMvmtBySeqNBr.add(shmHandlingUnitMvmt);
					}
				}
			}
			// /CCS-7745 : We need to set OsdCategoryCd only when all child pros are not
			// final delivered.
			if (!(movementStatusCd.size() == 1
					&& movementStatusCd.contains(MOVEMENT_STATUS_CD_FINAL_DELIVERED))) {
				Set<String> excpTypeCds = latestHandlingUnitMvmtBySeqNBr.stream()
						.filter(handlingUnitMvmt -> StringUtils.isNotBlank(handlingUnitMvmt.getExcpTypCd()))
						.map(handlingUnitMvmt -> handlingUnitMvmt.getExcpTypCd().trim())
						.collect(Collectors.toSet());

				if (CollectionUtils.isNotEmpty(excpTypeCds)) {
					if (excpTypeCds.contains(MOVEMENT_EXCEPTION_TYPE_CD_DAMAGED)) {
						osdParentShipment.setOsdCategoryCd(OsdCategoryCdTransformer
								.toEnum(OsdCategoryCd.DAMAGED.toString().toUpperCase()));
					}

					else if (excpTypeCds.contains(MOVEMENT_EXCEPTION_TYPE_CD_REFUSED)) {
						osdParentShipment.setOsdCategoryCd(OsdCategoryCdTransformer
								.toEnum(OsdCategoryCd.REFUSED.toString().toUpperCase()));
					}

					else if (excpTypeCds.contains(MOVEMENT_EXCEPTION_TYPE_CD_SHORT)) {
						osdParentShipment.setOsdCategoryCd(
								OsdCategoryCdTransformer.toEnum(OsdCategoryCd.SHORT.toString().toUpperCase()));
					} 
					
					else if (!excpTypeCds.contains(MOVEMENT_EXCEPTION_TYPE_CD_OVER)) {
						// CCS-8094 : If no child PROs have an exception
						osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
					}
				} else {
					// CCS-8094 : If no child PROs have an exception
					String deliveryQualifierCd = shmShipment.getDlvryQalfrCd();

					if(StringUtils.isNotBlank(deliveryQualifierCd) 
						&& DELIVERY_QUALIFIER_CDS_REFUSED_DAMAGED_SHORT_OVERAGE.contains(deliveryQualifierCd)) {
						
						if (DELIVERY_QUALIFIER_CD_REFUSED.contains(deliveryQualifierCd)) {
							osdParentShipment.setOsdCategoryCd(
									OsdCategoryCdTransformer.toEnum(OsdCategoryCd.REFUSED.toString().toUpperCase()));
						}
						else if (DELIVERY_QUALIFIER_CD_DAMAGED.contains(deliveryQualifierCd)) {
							osdParentShipment.setOsdCategoryCd(
									OsdCategoryCdTransformer.toEnum(OsdCategoryCd.DAMAGED.toString().toUpperCase()));
						}
						else if (DELIVERY_QUALIFIER_CD_PARTIAL_SHORT.contains(deliveryQualifierCd)
								|| DELIVERY_QUALIFIER_CD_ALL_SHORT.contains(deliveryQualifierCd)) {
							osdParentShipment.setOsdCategoryCd(
									OsdCategoryCdTransformer.toEnum(OsdCategoryCd.SHORT.toString().toUpperCase()));
						}
						else if (DELIVERY_QUALIFIER_CD_OVER_SHPMT.contains(deliveryQualifierCd)) {
							osdParentShipment.setOsdCategoryCd(
									OsdCategoryCdTransformer.toEnum(OsdCategoryCd.OVERAGE.toString().toUpperCase()));
						}
					}
					else {
						osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
					}
				}
			} else {
				// CCS-8094 : If all child PROs are final delivered
				osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			}
		} else {
			// CCS-8094 : If deliveryQualifierCd is blank or Z (final delivered)
			String deliveryQualifierCd = shmShipment.getDlvryQalfrCd();
			
			if(StringUtils.isBlank(deliveryQualifierCd)
			|| deliveryQualifierCd.equalsIgnoreCase(DELIVERY_QUALIFIER_CD_FINAL)
			|| !DELIVERY_QUALIFIER_CDS_REFUSED_DAMAGED_SHORT_OVERAGE.contains(deliveryQualifierCd)) {
				osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			}
			
			if (DELIVERY_QUALIFIER_CDS_REFUSED_DAMAGED_SHORT_OVERAGE.contains(deliveryQualifierCd)) {
				if (DELIVERY_QUALIFIER_CD_REFUSED.contains(deliveryQualifierCd)) {
					osdParentShipment.setOsdCategoryCd(
							OsdCategoryCdTransformer.toEnum(OsdCategoryCd.REFUSED.toString().toUpperCase()));
				}
				if (DELIVERY_QUALIFIER_CD_DAMAGED.contains(deliveryQualifierCd)) {
					osdParentShipment.setOsdCategoryCd(
							OsdCategoryCdTransformer.toEnum(OsdCategoryCd.DAMAGED.toString().toUpperCase()));
				}
				if (DELIVERY_QUALIFIER_CD_PARTIAL_SHORT.contains(deliveryQualifierCd)
						|| DELIVERY_QUALIFIER_CD_ALL_SHORT.contains(deliveryQualifierCd)) {
					osdParentShipment.setOsdCategoryCd(
							OsdCategoryCdTransformer.toEnum(OsdCategoryCd.SHORT.toString().toUpperCase()));
				}
				if (DELIVERY_QUALIFIER_CD_OVER_SHPMT.contains(deliveryQualifierCd)) {
					osdParentShipment.setOsdCategoryCd(
							OsdCategoryCdTransformer.toEnum(OsdCategoryCd.OVERAGE.toString().toUpperCase()));
				}
			}
			else {
				osdParentShipment.setOsdCategoryCd(OsdCategoryCd.OTHER);
			}
			
		}
		osdParentShipment.setParentProNbr(shmShipment.getProNbrTxt());
	}
}
