package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.transformer.v2.BillClassCdTransformer;
import com.xpo.ltl.api.shipment.v2.BillClassCd;
import com.xpo.ltl.api.shipment.v2.GetMoverShipmentsResp;
import com.xpo.ltl.api.shipment.v2.HandlingUnit;
import com.xpo.ltl.api.shipment.v2.MoverShipment;
import com.xpo.ltl.api.shipment.v2.ShipmentDetails;
import com.xpo.ltl.api.shipment.v2.ShipmentId;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.java.util.cityoperations.ProNumber;
import com.xpo.ltl.shipment.service.dao.ShmShipmentEagerLoadPlan;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShipmentDetailsDelegate;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;

@ApplicationScoped
@LogExecutionTime
public class GetMoverShipmentsImpl {

    @Inject
    private ShmShipmentSubDAO shmShipmentSubDAO;

    @Inject
    private ShipmentDetailsDelegate shipmentDetailsDelegate;

    public GetMoverShipmentsResp getMoverShipments(String proNumber, TransactionContext txnContext, EntityManager entityManager)
            throws ServiceException, ValidationException {

        checkNotNull(proNumber, "The proNumber is required.");
		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");

        String proNumber11Dig = validateAndGet11DigitProNumber(proNumber, txnContext);

		//lookup parent shipment directly from Oracle

        ShmShipmentEagerLoadPlan moverShmShipmentEagerLoadPlan =
            new ShmShipmentEagerLoadPlan();
        moverShmShipmentEagerLoadPlan.setShmAsEntdCusts(true);
        moverShmShipmentEagerLoadPlan.setShmCommodities(true);

        ShmShipmentEagerLoadPlan parentShmShipmentEagerLoadPlan =
            new ShmShipmentEagerLoadPlan();
        parentShmShipmentEagerLoadPlan.setShmHandlingUnits(true);

        ShmShipment parentShipment =
            shmShipmentSubDAO.findByProOrShipmentId
                (proNumber11Dig,
                 null,
                 null,
                 false,
                 parentShmShipmentEagerLoadPlan,
                 entityManager);
        if (parentShipment == null) {
			throw ExceptionBuilder.exception(NotFoundErrorMessage.SHIPMENT_NOT_FOUND, txnContext).build();
		}

        if (BillClassCdTransformer.toEnum(parentShipment.getBillClassCd())
                == BillClassCd.ASTRAY_FRT_SEGMENT) {
            // This was a mover
            parentShipment =
                shmShipmentSubDAO.findByProOrShipmentId
                    (null,
                     null,
                     BasicTransformer.toLong(parentShipment.getParentInstId()),
                     false,
                     parentShmShipmentEagerLoadPlan,
                     entityManager);
        }

        List<ShmShipment> moverShipmentList =
            CollectionUtils.emptyIfNull
                (shmShipmentSubDAO.listShipmentsByParentShpInstIds
                     (Arrays.asList(parentShipment.getShpInstId()),
                      moverShmShipmentEagerLoadPlan,
                      entityManager))
                .stream()
                .filter(shm ->
                            BillClassCdTransformer.toEnum(shm.getBillClassCd())
                                == BillClassCd.ASTRAY_FRT_SEGMENT)
                .collect(Collectors.toList());

        ShipmentDetails parentDetail =
            shipmentDetailsDelegate.buildDetails
                (parentShipment,
                 parentShmShipmentEagerLoadPlan);

        List<MoverShipment> moverShipmentDetailsList = new ArrayList<>();
        for (ShmShipment moverShmShipment : moverShipmentList) {
            ShipmentDetails moverDetail =
                shipmentDetailsDelegate.buildDetails
                    (moverShmShipment,
                     moverShmShipmentEagerLoadPlan);

            MoverShipment moverShipment = new MoverShipment();
            moverShipment.setShipment(moverDetail.getShipment());
            moverShipment.setCommodities(moverDetail.getCommodity());
            moverShipment.setAsMatchedParties(moverDetail.getAsMatchedParty());

            List<HandlingUnit> moverHandlingUnits =
                parentDetail.getShipment().getHandlingUnit().stream()
                    .filter(handlingUnit ->
                                StringUtils.equals
                                    (handlingUnit.getMoverProNbr(),
                                     moverShmShipment.getProNbrTxt()))
                    .collect(Collectors.toList());

            if (!moverHandlingUnits.isEmpty())
                moverShipment.setHandlingUnits(moverHandlingUnits);

            moverShipmentDetailsList.add(moverShipment);
        }

        GetMoverShipmentsResp resp = new GetMoverShipmentsResp();
        resp.setMoverShipments(moverShipmentDetailsList);
        ShipmentId shmId = new ShipmentId();
        shmId.setShipmentInstId(BasicTransformer.toString(parentShipment.getShpInstId()));
        shmId.setProNumber(parentShipment.getProNbrTxt());
        shmId.setPickupDate(BasicTransformer.toXMLGregorianCalendar(parentShipment.getPkupDt()));
        resp.setParentShipmentId(shmId);

        return resp;
	}


    private String validateAndGet11DigitProNumber(String proNumberStr, TransactionContext txnContext) throws ValidationException {

        String rawProNumber = ProNumberHelper.isMvrProNumber(proNumberStr) ? ProNumberHelper.getMvrProNumberWithoutSuffix(proNumberStr) :
            proNumberStr;

        ProNumber proNumber = ProNumber.from(rawProNumber);

        if (!proNumber.isValid()) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.PRO_NBR_FORMAT_INVALID, txnContext).build();
        }

        return proNumber.getNormalized();
	}

}
