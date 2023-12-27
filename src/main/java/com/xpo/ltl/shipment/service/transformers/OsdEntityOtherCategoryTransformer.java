package com.xpo.ltl.shipment.service.transformers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.javatuples.Quartet;
import org.javatuples.Quintet;
import org.javatuples.Triplet;

import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmMgmtRemark;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdHeader;
import com.xpo.ltl.api.shipment.service.entity.ShmOsdImage;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.OsdStatusCdTransformer;
import com.xpo.ltl.api.shipment.transformer.v2.OtherOsdReasonCdTransformer;
import com.xpo.ltl.api.shipment.v2.ActionCd;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.OsdChildShipment;
import com.xpo.ltl.api.shipment.v2.OsdDocumentImage;
import com.xpo.ltl.api.shipment.v2.OsdManagementRemark;
import com.xpo.ltl.api.shipment.v2.OsdParentShipment;
import com.xpo.ltl.api.shipment.v2.OsdStatusCd;
import com.xpo.ltl.api.shipment.v2.OtherOsdReasonCd;
import com.xpo.ltl.api.shipment.v2.UpsertOsdRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;

public class OsdEntityOtherCategoryTransformer extends BasicTransformer {

	@Inject
	private OsdEntityCommonTransformer osdEntityCommonTransformer;

	public ShmOsdHeader buildShmOsdHeader(UpsertOsdRqst upsertOsdRqst,
			Quintet<ShmShipment, ShmShipment, Boolean, Map<String, ShmOsdHeader>,ShmOsdHeader> shipmentAndOsdDetails, ShmOsdHeader shmosdHeader,
			AuditInfo auditInfo, List<ShmOsdImage> childProTobeDeleted, TransactionContext txnContext, EntityManager entityManager)
			throws ValidationException {

		ShmShipment shmShipment = shipmentAndOsdDetails.getValue0();
		Boolean isLegacyPro = shipmentAndOsdDetails.getValue2();
		OsdParentShipment osdParentShipment = upsertOsdRqst.getOsdParentShipment();
		List<OsdChildShipment> osdChildShipments = upsertOsdRqst.getOsdChildShipments();
		List<OsdManagementRemark> osdManagementRemarks = upsertOsdRqst.getOsdManagementRemarks();
		List<OsdDocumentImage> osdDocumentImages = upsertOsdRqst.getOsdDocumentImages();
		ActionCd actionCd = upsertOsdRqst.getActionCd();
		List<String> dmsUrls = new ArrayList<>();
		List<ShmOsdImage> shmOsdImages = new ArrayList<>();
		List<ShmMgmtRemark> shmMgmtRemarks = new ArrayList<>();
		Set<String> childProNbrSet = new HashSet<>();

		if (ActionCd.ADD == actionCd) {

			dmsUrls.addAll(CollectionUtils.emptyIfNull(osdDocumentImages).stream()
					.filter(docImage -> StringUtils.isNotEmpty(docImage.getDmsUrl())).map(OsdDocumentImage::getDmsUrl)
					.distinct().collect(Collectors.toList()));

			shmosdHeader = osdEntityCommonTransformer.buildShmOsdHeader(upsertOsdRqst, shmosdHeader, dmsUrls.size(),
					auditInfo, shmShipment, txnContext, entityManager);

			if (Objects.nonNull(osdParentShipment)) {
				if (Objects.isNull(osdParentShipment.getStatusCd())) {
					shmosdHeader.setStatusCd(OsdStatusCdTransformer.toCode(OsdStatusCd.OT_NOT_STARTED));
				} else {
					shmosdHeader.setStatusCd(OsdStatusCdTransformer.toCode(osdParentShipment.getStatusCd()));
				}

				if (Objects.isNull(osdParentShipment.getParentProNbr())) {
					shmosdHeader.setOtherReasonCd(OtherOsdReasonCdTransformer.toCode(OtherOsdReasonCd.ONLY_CHILD_PRO));
					shmosdHeader.setHuCnt(BigDecimal.ONE);
				}

				if (Objects.nonNull(osdParentShipment.getOtherOsdReasonCd())) {
					shmosdHeader.setOtherReasonCd(
							OtherOsdReasonCdTransformer.toCode(osdParentShipment.getOtherOsdReasonCd()));
				}

				if (Objects.nonNull(osdParentShipment.getOsdPiecesCount())) {
					shmosdHeader.setHuCnt(BasicTransformer.toBigDecimal(osdParentShipment.getOsdPiecesCount()));
				} else if (!isLegacyPro) {
					shmosdHeader.setHuCnt(BigDecimal.ONE);
				}

				if (Objects.nonNull(osdParentShipment.getNewParentProNbr())) {
					shmosdHeader.setNewParentProNbrTxt(
							ProNumberHelper.toElevenDigitPro(osdParentShipment.getNewParentProNbr(), txnContext));
				}
			}

			if (Objects.nonNull(shmShipment)) {
				shmosdHeader.setShpInstId(shmosdHeader.getShpInstId());
			}

			ShmOsdImage shmOsdImageForParentPro = osdEntityCommonTransformer.buildShmOsdImage(actionCd, shmosdHeader,
					osdParentShipment, auditInfo, null, txnContext, entityManager);
			shmOsdImageForParentPro.setDmsUrl(String.join(",", dmsUrls));
			shmOsdImages.add(shmOsdImageForParentPro);

			Set<String> duplicateChildShipments = new HashSet<>();
			
			if (Objects.nonNull(osdChildShipments)) {
				for (OsdChildShipment osdChildShipment : osdChildShipments) {
					if (osdChildShipment != null
							&& duplicateChildShipments.add(osdChildShipment.getChildProNbr())) {
						ShmOsdImage shmOsdImageForChildPro = osdEntityCommonTransformer.buildShmOsdImage(
								osdChildShipment.getActionCd(), shmosdHeader, osdParentShipment, auditInfo, null,
								txnContext, entityManager);

						if (Objects.nonNull(osdChildShipment.getChildProNbr())) {
							shmOsdImageForChildPro.setProNbrTxt(
									ProNumberHelper.toElevenDigitPro(osdChildShipment.getChildProNbr(), txnContext));
						}
						shmOsdImages.add(shmOsdImageForChildPro);
					}
				}
			}
			shmosdHeader.setShmOsdImages(shmOsdImages);
		}

		if (ActionCd.UPDATE == actionCd) {

			ShmOsdImage shmOsdImageForParentPro = CollectionUtils.emptyIfNull(shmosdHeader.getShmOsdImages()).stream()
					.filter(osdImage -> StringUtils.isNotEmpty(osdImage.getDmsUrl())).findFirst().orElse(null);

			if (Objects.nonNull(shmOsdImageForParentPro)) {
				dmsUrls.addAll(
						CollectionUtils.emptyIfNull(Arrays.asList(shmOsdImageForParentPro.getDmsUrl().split(",")))
								.stream().collect(Collectors.toList()));
			}
			
			dmsUrls.addAll(CollectionUtils.emptyIfNull(osdDocumentImages).stream()
					.filter(docImage -> StringUtils.isNotEmpty(docImage.getDmsUrl())).map(OsdDocumentImage::getDmsUrl)
					.distinct().collect(Collectors.toList()));

			dmsUrls = dmsUrls.stream().distinct().collect(Collectors.toList());
			
			shmosdHeader = osdEntityCommonTransformer.buildShmOsdHeader(upsertOsdRqst, shmosdHeader, dmsUrls.size(),
					auditInfo, shmShipment, txnContext, entityManager);

			if (Objects.nonNull(osdParentShipment)) {
				if (Objects.nonNull(osdParentShipment.getStatusCd())) {
					shmosdHeader.setStatusCd(OsdStatusCdTransformer.toCode(osdParentShipment.getStatusCd()));
				}

				if (Objects.nonNull(osdParentShipment.getOtherOsdReasonCd())) {
					shmosdHeader.setOtherReasonCd(
							OtherOsdReasonCdTransformer.toCode(osdParentShipment.getOtherOsdReasonCd()));
				}

				if (Objects.nonNull(osdParentShipment.getOsdPiecesCount())) {
					shmosdHeader.setHuCnt(BasicTransformer.toBigDecimal(osdParentShipment.getOsdPiecesCount()));
				} else if (!isLegacyPro) {
					shmosdHeader.setHuCnt(BigDecimal.ONE);
				}

				if (Objects.nonNull(osdParentShipment.getNewParentProNbr())) {
					shmosdHeader.setNewParentProNbrTxt(
							ProNumberHelper.toElevenDigitPro(osdParentShipment.getNewParentProNbr(), txnContext));
				}
			}

			if (Objects.nonNull(shmShipment)) {
				shmosdHeader.setShpInstId(shmosdHeader.getShpInstId());
			}

			if (Objects.nonNull(shmOsdImageForParentPro)) {
				shmOsdImageForParentPro = osdEntityCommonTransformer.buildShmOsdImage(actionCd, shmosdHeader,
						osdParentShipment, auditInfo, shmOsdImageForParentPro, txnContext, entityManager);
				shmOsdImageForParentPro.setDmsUrl(String.join(",", dmsUrls));
			}

			Set<String> duplicateChildShipments = new HashSet<>();
			
			if (Objects.nonNull(osdChildShipments)) {
				for (OsdChildShipment osdChildShipment : osdChildShipments) {
			    	if (osdChildShipment != null
			    			&& duplicateChildShipments.add(osdChildShipment.getChildProNbr())) {
			    		String childProNbr = ProNumberHelper.toElevenDigitPro(osdChildShipment.getChildProNbr(),
							txnContext);
			    		childProNbrSet.add(childProNbr);
			    		ShmOsdImage shmOsdImageForChildPro = CollectionUtils.emptyIfNull(shmosdHeader.getShmOsdImages())
			    				.stream().filter(docImage -> StringUtils.isNotEmpty(docImage.getProNbrTxt())
			    						&& childProNbr.equalsIgnoreCase(docImage.getProNbrTxt()))
			    				.findFirst().orElse(null);
			    		
			    		if (ActionCd.ADD == osdChildShipment.getActionCd()) {
			    			shmOsdImageForChildPro = osdEntityCommonTransformer.buildShmOsdImage(
			    					osdChildShipment.getActionCd(), shmosdHeader, osdParentShipment, auditInfo, null,
			    					txnContext, entityManager);
			    			if (Objects.nonNull(osdChildShipment.getChildProNbr())) {
			    				shmOsdImageForChildPro.setProNbrTxt(
			    						ProNumberHelper.toElevenDigitPro(osdChildShipment.getChildProNbr(), txnContext));
			    			}
			    			shmosdHeader.addShmOsdImage(shmOsdImageForChildPro);
			    		}

			    		if (ActionCd.UPDATE == osdChildShipment.getActionCd()) {
			    			osdEntityCommonTransformer.buildShmOsdImage(osdChildShipment.getActionCd(), shmosdHeader,
			    					osdParentShipment, auditInfo, shmOsdImageForChildPro, txnContext, entityManager);
			    		}

			    		if (ActionCd.DELETE == osdChildShipment.getActionCd()) {
			    			childProTobeDeleted.add(shmOsdImageForChildPro);
			    		}

			    	}
				}
			}			
			
			if(Objects.nonNull(shmosdHeader) && Objects.nonNull(shmosdHeader.getShmOsdImages())) {
				for(ShmOsdImage shmOsdImage : shmosdHeader.getShmOsdImages()) {
					if(childProNbrSet.add(shmOsdImage.getProNbrTxt())) {
						osdEntityCommonTransformer.buildShmOsdImage(actionCd, shmosdHeader,
								osdParentShipment, auditInfo, shmOsdImage, txnContext, entityManager);
					}
				}
			}

		}

		if (Objects.nonNull(osdManagementRemarks)) {
			for (OsdManagementRemark osdManagementRemark : osdManagementRemarks) {
				shmMgmtRemarks.add(osdEntityCommonTransformer.buildShmMgmtRemark(shmosdHeader, shmShipment,
						osdManagementRemark, auditInfo, entityManager));
			}
			shmosdHeader.setShmMgmtRemarks(shmMgmtRemarks);
		}

		return shmosdHeader;
	}

}
