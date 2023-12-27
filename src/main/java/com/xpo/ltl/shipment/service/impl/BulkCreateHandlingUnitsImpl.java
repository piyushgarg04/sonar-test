package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.xpo.ltl.api.exception.NotFoundException;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.NotFoundErrorMessage;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.api.shipment.v2.AuditInfo;
import com.xpo.ltl.api.shipment.v2.BulkCreateHandlingUnitsRqst;
import com.xpo.ltl.api.shipment.v2.HandlingUnit;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.shipment.service.dao.ShmHandlingUnitSubDAO;
import com.xpo.ltl.shipment.service.dao.ShmShipmentSubDAO;
import com.xpo.ltl.shipment.service.delegates.ShmHandlingUnitDelegate;
import com.xpo.ltl.shipment.service.util.AuditInfoHelper;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;
import com.xpo.ltl.shipment.service.util.TimestampUtil;

@RequestScoped
public class BulkCreateHandlingUnitsImpl {

    @Inject
    private ShmShipmentSubDAO shipmentDAO;

    @Inject
    private ShmHandlingUnitSubDAO shmHandlingUnitSubDAO;

    @Inject
    private ShmHandlingUnitDelegate shmHandlingUnitDelegate;

    @PersistenceContext(unitName = "ltl-java-db2-shipment-jaxrs")
    private EntityManager db2EntityManager;

    private static final String HU_PGM_ID = "HUCRTE";


    public void bulkCreateHandlingUnits(BulkCreateHandlingUnitsRqst bulkCreateHandlingUnitsRqst, TransactionContext txnContext, EntityManager entityManager) throws ServiceException {

        checkNotNull(txnContext, "The TransactionContext is required.");
        checkNotNull(entityManager, "The EntityManager is required.");
        checkNotNull(bulkCreateHandlingUnitsRqst, "The request is required.");

        validateRequest(bulkCreateHandlingUnitsRqst, txnContext, entityManager);

        AuditInfo auditInfo = AuditInfoHelper
            .getAuditInfoWithPgmAndUserId(HU_PGM_ID, bulkCreateHandlingUnitsRqst.getUserId(), txnContext);

        Timestamp lastMvmtTimestamp = Timestamp.from(Instant.now());
        XMLGregorianCalendar lastMvmtDateTime = TimestampUtil.toXmlGregorianCalendar(lastMvmtTimestamp);

        List<String> distinctParentPros = bulkCreateHandlingUnitsRqst.getHandlingUnitShipments().stream().filter(hu -> StringUtils.isNotBlank(hu.getParentProNbr())).map(hu -> {
            try {
                return ProNumberHelper.validateProNumber(hu.getParentProNbr(), txnContext);
            } catch (ServiceException e) {
                return hu.getParentProNbr();
            }
        }).distinct().collect(Collectors.toList());

        if (NumberUtils.compare(distinctParentPros.size(), 1) != 0) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext).moreInfo("N/A", "Can process sibling PROs only.").log().build();
        }

        ShmShipment shmShipment = shipmentDAO.findByIdOrProNumber(distinctParentPros.get(0), null, entityManager);
        if (Objects.isNull(shmShipment)) {
            throw ExceptionBuilder.exception(NotFoundErrorMessage.SHIPMENT_NF, txnContext).moreInfo("BulkCreatetHandlingUnitsImpl", "Parent Pro: " + distinctParentPros.get(0)).build();
        }

        long maxDimensionSeqNbr = shmShipment.getShmLnhDimensions().stream().mapToLong(dim -> dim.getId().getDimSeqNbr()).max().orElse(0L);
        long nextHUSeqNbr = shmHandlingUnitSubDAO.getNextSeqNbrByShpInstId(shmShipment.getShpInstId(), entityManager);

        for (HandlingUnit handlingUnit : bulkCreateHandlingUnitsRqst.getHandlingUnitShipments()) {

            shmHandlingUnitDelegate
                .createShmHandlingUnitAndShmLnhDimension(handlingUnit, handlingUnit.getShipmentInstanceId(),
                    nextHUSeqNbr++, BasicTransformer.toDate(handlingUnit.getPickupDate()),
                bulkCreateHandlingUnitsRqst.getRequestingSicCd(), lastMvmtDateTime, auditInfo, entityManager,
                    txnContext, ++maxDimensionSeqNbr);
        }

    }

    private void validateRequest(BulkCreateHandlingUnitsRqst bulkCreateHandlingUnitsRqst,
        TransactionContext txnContext, EntityManager entityManager)
            throws ValidationException, NotFoundException, ServiceException {

        if(StringUtils.isBlank(bulkCreateHandlingUnitsRqst.getRequestingSicCd())) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.RPTG_SIC_RQ, txnContext).build();
        }

        if(StringUtils.isBlank(bulkCreateHandlingUnitsRqst.getUserId())) {
            throw ExceptionBuilder
                .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo("UserId", "User id is required.")
                .build();
        }

        shmHandlingUnitDelegate.validateHandlingUnits(bulkCreateHandlingUnitsRqst.getHandlingUnitShipments(), null, txnContext, entityManager);
    }

}
