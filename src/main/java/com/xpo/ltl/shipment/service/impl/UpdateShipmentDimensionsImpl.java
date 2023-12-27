package com.xpo.ltl.shipment.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.CollectionUtils;

import com.google.api.client.util.Lists;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmHandlingUnit;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.DtoTransformer;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.LnhDimension;
import com.xpo.ltl.api.shipment.v2.ShipmentDimension;
import com.xpo.ltl.api.shipment.v2.ShipmentWithDimension;
import com.xpo.ltl.api.shipment.v2.UpdateHandlingUnitDimensionsRqst;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentDimensionsRqst;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentLinehaulDimensionsRqst;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.client.ExternalRestClient;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.HandlingUnitHelper;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import com.xpo.ltl.shipment.service.validators.UpdateShipmentDimensionsValidator;

@RequestScoped
@LogExecutionTime
public class UpdateShipmentDimensionsImpl {

    @PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
    private EntityManager db2EntityManager;

    @Inject
    private UpdateShipmentDimensionsValidator validator;

    @Inject
    private ShmShipmentSubDAO shmShipmentSubDAO;

    @Inject
	private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

	@Inject
	private UpdateShipmentLinehaulDimensionsImpl updateShipmentLinehaulDimensionsImpl;

    @Inject
	private UpdateHandlingUnitDimensionsImpl updateHandlingUnitDimensionsImpl;

	@Inject
	private ExternalRestClient restClient;

    private static final String UPD_DIM_PGM_ID = "DIMUPDT";

	private static final Map<String, String> mapCapturedByCd = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;
		{
			put("ACCURACY", "A");
			put("DOCK", "D");
			put("PICKUP", "P");
		}
	};

	public void updateShipmentDimensions(
		UpdateShipmentDimensionsRqst updateShipmentDimensionsRqst,
		String proNbr,
		EntityManager entityManager,
		TransactionContext txnContext) throws ServiceException {

        proNbr = validator.validate(updateShipmentDimensionsRqst, proNbr, txnContext, entityManager);

        AuditInfo auditInfo = AuditInfoHelper.getAuditInfoWithPgmId(UPD_DIM_PGM_ID, txnContext);

//		if(CollectionUtils.isEmpty(updateShipmentDimensionsRqst.getDimensions())) {
//			//throw exception?
//		}

		if (ProNumberHelper.isBluePro(proNbr)) {
			//update shipment by parent PRO applies to those using legacy label or PRO is exempted from tracking by handling unit
			ShmShipment shmShipment = shmShipmentSubDAO.findByIdOrProNumber(proNbr, null, entityManager);

			if (shmShipment == null) {
                throw ExceptionBuilder
                .exception(NotFoundErrorMessage.SHIPMENT_NF, txnContext)
                .moreInfo("proNbr", proNbr)
                .build();
			}

            validateLegacyProPrefixes(proNbr, shmShipment, txnContext);

            updateShimentHandlingUnitDimensionWhenExemption(updateShipmentDimensionsRqst, shmShipment, proNbr,
                auditInfo, entityManager, txnContext);

			UpdateShipmentLinehaulDimensionsRqst updateShipmentLinehaulDimensionsRqst = buildRequestForShpLinehaulDimensions(updateShipmentDimensionsRqst, shmShipment, auditInfo);

            updateShipmentLinehaulDimensionsImpl.updateShipmentLinehaulDimensions(updateShipmentLinehaulDimensionsRqst,
                updateShipmentDimensionsRqst.getPupVolumePercentage(),
                updateShipmentDimensionsRqst.getTotalVolumeCubicFeet(),
                updateShipmentDimensionsRqst.getCubicFeetProfileMthdCd(),
                updateShipmentDimensionsRqst.getCubicFeetProfileTypeCd(), 
                false, entityManager, txnContext);
		} else {
			ShmHandlingUnit shmHandlingUnit = shmHandlingUnitSubDAO.findByTrackingProNumber(proNbr, entityManager);

			if (shmHandlingUnit == null) {
                throw ExceptionBuilder
                .exception(NotFoundErrorMessage.SHM_HANDLING_UNIT_NOT_FOUND, txnContext)
                .moreInfo("proNbr", proNbr)
                .build();
			}

			UpdateHandlingUnitDimensionsRqst updateHandlingUnitDimensionsRqst = buildRequestForHandlingUnitDimensions(updateShipmentDimensionsRqst, shmHandlingUnit, auditInfo);
            updateHandlingUnitDimensionsImpl.updateHandlingUnitDimensions(updateHandlingUnitDimensionsRqst,
                updateShipmentDimensionsRqst.getTotalVolumeCubicFeet(),
                updateShipmentDimensionsRqst.getPupVolumePercentage(),
                updateShipmentDimensionsRqst.getCubicFeetProfileTypeCd(),
                updateShipmentDimensionsRqst.getCubicFeetProfileMthdCd(), proNbr, Optional.of(auditInfo), txnContext,
                entityManager);
		}

    }

    protected void validateLegacyProPrefixes(String proNbr, ShmShipment shmShipment, TransactionContext txnContext)
            throws ValidationException {
        List<String> nonLegacyProPrefixes = null;
        try {
            nonLegacyProPrefixes = buildNonLegacyProPrefixUsage(txnContext);
        } catch (final Exception e) {
            nonLegacyProPrefixes = Lists.newArrayList(); // if api call or building the list failed, assume blank list
        }

        boolean legacyPro = !nonLegacyProPrefixes.contains(proNbr.substring(1, 4));
        // validation below should only happen when it is a legacy pro. This will currently be true always until above
        // validation is added.
        if (!BasicTransformer.toBoolean(shmShipment.getHandlingUnitExemptionInd()) && !legacyPro) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo("updateShipmentDimensions", "Cannot enter dimension based on input parent PRO, use child PRO")
                .build();
        }
    }

    /**
     * update handling unit information only when exemption indication is TRUE, otherwise do nothing.
     * <ul>
     * <li>When total number of handling units (or total pieces of shipment) is equals total pieces of all dimensions
     * captured, mapping of update to SHM_HANDLING_UNIT will be 1-to-1</li>
     * <li>When total number of handling units is less than the total pieces of all dimensions captured, update all
     * handling units and disregard the fact there's more dimension pieces</li>
     * <li>When total number of handling units is greater than the total pieces of all dimensions captured, update as
     * many handling units as we can based on the number of pieces of dimensions captured. This will leave some handling
     * units not updated.</li>
     * </ul>
     */
    protected void updateShimentHandlingUnitDimensionWhenExemption(
        UpdateShipmentDimensionsRqst updateShipmentDimensionsRqst, ShmShipment shmShipment, String proNbr,
        AuditInfo auditInfo, EntityManager entityManager, TransactionContext txnContext) throws ServiceException {

        if (Boolean.TRUE.equals(BasicTransformer.toBoolean(shmShipment.getHandlingUnitExemptionInd()))) {
            List<ShmHandlingUnit> shmHndlUnitList = shmHandlingUnitSubDAO.findByParentProNumber(proNbr, entityManager);

            int shmHUListSize = CollectionUtils.emptyIfNull(shmHndlUnitList).size();
            int dimListSize = CollectionUtils.emptyIfNull(updateShipmentDimensionsRqst.getDimensions()).size();

            for (int i = 0; i < dimListSize && i < shmHUListSize; i++) {
                ShmHandlingUnit shmHandlingUnit = shmHndlUnitList.get(i);
                ShipmentDimension shmDimension = updateShipmentDimensionsRqst.getDimensions().get(i);
                shmHandlingUnit.setLengthNbr(Optional.ofNullable(shmDimension.getLength()).orElse(BigDecimal.ZERO));
                shmHandlingUnit.setWidthNbr(Optional.ofNullable(shmDimension.getWidth()).orElse(BigDecimal.ZERO));
                shmHandlingUnit.setHeightNbr(Optional.ofNullable(shmDimension.getHeight()).orElse(BigDecimal.ZERO));
                shmHandlingUnit
                    .setWgtLbs(Optional
                        .ofNullable(BasicTransformer.toBigDecimal(shmDimension.getWeight()))
                        .orElse(BigDecimal.ONE));
                Double volCubFt = HandlingUnitHelper
                    .calculateVolCubFt(BasicTransformer.toDouble(shmHandlingUnit.getLengthNbr()),
                        BasicTransformer.toDouble(shmHandlingUnit.getWidthNbr()),
                        BasicTransformer.toDouble(shmHandlingUnit.getHeightNbr()));
                shmHandlingUnit.setVolCft(BasicTransformer.toBigDecimal(volCubFt));
                shmHandlingUnit.setDimensionTypeCd(updateShipmentDimensionsRqst.getDimensionTypeCd());
                DtoTransformer.setLstUpdateAuditInfo(shmHandlingUnit, auditInfo);
                shmHandlingUnitSubDAO
                    .updateDB2ShmHandlingUnit(shmHandlingUnit, shmHandlingUnit.getLstUpdtTmst(), txnContext,
                        db2EntityManager);
            }
        }
    }

    private List<String> buildNonLegacyProPrefixUsage(TransactionContext txnContext) throws ServiceException {

        return restClient.listNonLegacyProBolPrefixMaster(txnContext)
				.getBolPrefixMaster().stream()
				.map(bolPrefixMaster -> bolPrefixMaster.getBolProPrefix())
				.collect(Collectors.toList());
	}

	private UpdateHandlingUnitDimensionsRqst buildRequestForHandlingUnitDimensions(UpdateShipmentDimensionsRqst updateShipmentDimensionsRqst, ShmHandlingUnit handlingUnit, AuditInfo auditInfo) {
		ShipmentDimension shipmentDimension = updateShipmentDimensionsRqst.getDimensions().get(0);
		UpdateHandlingUnitDimensionsRqst updateHandlingUnitDimensionsRqst = new UpdateHandlingUnitDimensionsRqst();
		updateHandlingUnitDimensionsRqst.setDimensionTypeCd(updateShipmentDimensionsRqst.getDimensionTypeCd());
        updateHandlingUnitDimensionsRqst
            .setHeightNbr(Optional.ofNullable(BasicTransformer.toDouble(shipmentDimension.getHeight())).orElse(0.0));
        updateHandlingUnitDimensionsRqst
            .setLengthNbr(Optional.ofNullable(BasicTransformer.toDouble(shipmentDimension.getLength())).orElse(0.0));
        updateHandlingUnitDimensionsRqst
            .setWidthNbr(Optional.ofNullable(BasicTransformer.toDouble(shipmentDimension.getWidth())).orElse(0.0));
		updateHandlingUnitDimensionsRqst.setCapturedByUserId(updateShipmentDimensionsRqst.getCapturedByUserId());
		updateHandlingUnitDimensionsRqst.setRequestingSicCd(updateShipmentDimensionsRqst.getRequestingSicCd());
		return updateHandlingUnitDimensionsRqst;
	}

	private UpdateShipmentLinehaulDimensionsRqst buildRequestForShpLinehaulDimensions(
		UpdateShipmentDimensionsRqst updateShipmentDimensionsRqst,
		ShmShipment shmShipment,
		AuditInfo auditInfo) {
		List<LnhDimension> lnhDimensions = new ArrayList<LnhDimension>();
		ShipmentWithDimension shipmentWithDimension = new ShipmentWithDimension();
		shipmentWithDimension.setShipmentInstId(shmShipment.getShpInstId());
		shipmentWithDimension.setLinehaulDimensions(lnhDimensions);

		for (ShipmentDimension shipmentDimension : updateShipmentDimensionsRqst.getDimensions()) {
			LnhDimension lnhDimension = new LnhDimension();
			lnhDimension.setShipmentInstId(shmShipment.getShpInstId());
			lnhDimension.setHeightNbr(BasicTransformer.toDouble(shipmentDimension.getHeight()));
			lnhDimension.setLengthNbr(BasicTransformer.toDouble(shipmentDimension.getLength()));
			lnhDimension.setWidthNbr(BasicTransformer.toDouble(shipmentDimension.getWidth()));
			lnhDimension.setPiecesCount(BasicTransformer.toLong(shipmentDimension.getPiecesCnt()));
			lnhDimension.setStackableInd(shipmentDimension.getStackableInd());
			lnhDimension.setCapturedByCd(mapCapturedByCd.get(updateShipmentDimensionsRqst.getDimensionTypeCd()));
			lnhDimension.setCapturedByUserid(updateShipmentDimensionsRqst.getCapturedByUserId());
			lnhDimension.setCapturedByDateTime(auditInfo.getUpdatedTimestamp());
			lnhDimension.setAuditInfo(auditInfo);
			lnhDimensions.add(lnhDimension);
		}

		UpdateShipmentLinehaulDimensionsRqst updateShipmentLinehaulDimensionsRqst = new UpdateShipmentLinehaulDimensionsRqst();
		updateShipmentLinehaulDimensionsRqst.setShipmentWithDimensions(Arrays.asList(shipmentWithDimension));
		updateShipmentLinehaulDimensionsRqst.setCapturedByUserId(updateShipmentDimensionsRqst.getCapturedByUserId());
		updateShipmentLinehaulDimensionsRqst.setRequestingSicCd(updateShipmentDimensionsRqst.getRequestingSicCd());
		return updateShipmentLinehaulDimensionsRqst;
	}


}
