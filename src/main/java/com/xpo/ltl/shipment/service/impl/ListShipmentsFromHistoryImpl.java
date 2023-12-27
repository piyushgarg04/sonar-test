package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.service.entity.ShmShipmentHist;
import com.xpo.ltl.api.shipment.v2.ListShipmentsFromHistoryResp;
import com.xpo.ltl.api.shipment.v2.ListShipmentsFromHistoryRqst;
import com.xpo.ltl.api.shipment.v2.Shipment;
import com.xpo.ltl.api.shipment.v2.ShipmentId;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmShipmentHistSubDAO;
import com.xpo.ltl.shipment.service.interceptors.LogExecutionTime;
import com.xpo.ltl.shipment.service.transformers.ShipmentEntityTransformer;
import com.xpo.ltl.shipment.service.validators.ShipmentRequestsValidator;

@ApplicationScoped
@LogExecutionTime
public class ListShipmentsFromHistoryImpl {

    @Inject
    private ShmShipmentHistSubDAO shmShipmentHistSubDAO;

    @Inject
    private ShipmentRequestsValidator shipmentRequestsValidator;

    public ListShipmentsFromHistoryResp listShipmentsFromHistory
            (ListShipmentsFromHistoryRqst request,
             TransactionContext txnContext,
             EntityManager entityManager)
            throws ServiceException {
        checkNotNull(txnContext, "TransactionContext is required");
        checkNotNull(entityManager, "EntityManager is required");
        checkNotNull(request.getShipmentIds(), "ShipmentIds are required");

        List<Long> shpInstIds =
            request.getShipmentIds()
                .stream()
                .map(ShipmentId::getShipmentInstId)
                .filter(StringUtils::isNotBlank)
                .map(BasicTransformer::toLong)
                .collect(Collectors.toList());

        List<String> proNbrs =
            request.getShipmentIds()
                .stream()
                .map(ShipmentId::getProNumber)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());

        shipmentRequestsValidator.validateRequest
            (proNbrs, shpInstIds, txnContext);

        boolean dlvryQalfrCdChange =
            BooleanUtils.isTrue(request.getPreviousLastMovementInd());

        List<ShmShipmentHist> shmShipmentHists;
        if (CollectionUtils.isNotEmpty(shpInstIds))
            shmShipmentHists =
                shmShipmentHistSubDAO.findMostRecentByShpInstIds
                    (shpInstIds, dlvryQalfrCdChange, entityManager);
        else
            shmShipmentHists =
                shmShipmentHistSubDAO.findMostRecentByProNbrs
                    (proNbrs, dlvryQalfrCdChange, entityManager);

        List<Shipment> shipments =
            ShipmentEntityTransformer.toShipment(shmShipmentHists);

        ListShipmentsFromHistoryResp response =
            new ListShipmentsFromHistoryResp();
        response.setShipments(shipments);
        return response;
    }

}
