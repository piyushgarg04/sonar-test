package com.xpo.ltl.shipment.service.validators;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentDimensionsRqst;
import com.xpo.ltl.shipment.service.util.ProNumberHelper;

@ApplicationScoped
public class UpdateShipmentDimensionsValidator extends Validator {

    // TODO: Replace it when have the enum in the request.
    private static final List<String> VALID_DIM_TYPE_CDS = Arrays
        .asList("DOCK", "ACCURACY", "PICKUP", "PICKUP_DIMENSIONER");

    public String validate(
        UpdateShipmentDimensionsRqst updateShipmentDimensionsRqst,
        String proNbr,
            TransactionContext txnContext, EntityManager entityManager)
            throws ServiceException {

        // check input
        Checker
            .of(updateShipmentDimensionsRqst)
                .must(Objects::nonNull)
                .orElseThrow(() -> requestRequiredError(txnContext));

        Checker.of(txnContext)
                .must(Objects::nonNull)
                .orElseThrow(() -> txnContextRequiredError(txnContext));

        Checker.of(entityManager)
                .must(Objects::nonNull)
                .orElseThrow(() -> entityManagerRequiredError(txnContext));

        Checker
            .of(proNbr)
                .must(StringUtils::isNotBlank)
                .orElseThrow(() -> proNbrRequiredError(txnContext));

        String elevenDigitPro = ProNumberHelper.validateProNumber(proNbr, txnContext);

        Checker
            .of(updateShipmentDimensionsRqst.getDimensionTypeCd())
            .must(Objects::nonNull)
            .orElseThrow(() -> dimensionTypeCdRequiredError(txnContext));

        Checker
            .of(updateShipmentDimensionsRqst.getDimensionTypeCd())
            .must(dimTypeCd -> VALID_DIM_TYPE_CDS
                .contains(dimTypeCd))
            .orElseThrow(() -> dimensionTypeCdNotValidError(txnContext));

        return elevenDigitPro;
    }

    private ServiceException requestRequiredError(TransactionContext txnContext) {

        return ExceptionBuilder
                .exception(ValidationErrorMessage.REQUEST_REQUIRED, txnContext)
                .build();
    }

    private ServiceException txnContextRequiredError(TransactionContext txnContext) {

        return ExceptionBuilder
                .exception(ValidationErrorMessage.TXN_CONTEXT_REQUIRED, txnContext)
                .build();
    }

    private ServiceException entityManagerRequiredError(TransactionContext txnContext) {

        return ExceptionBuilder
                .exception(ValidationErrorMessage.ENTITY_MANAGER_REQUIRED, txnContext)
                .build();
    }

    private ServiceException proNbrRequiredError(TransactionContext txnContext) {

        return ExceptionBuilder
                .exception(ValidationErrorMessage.PRO_NBR_RQ, txnContext)
                .build();
    }

    private ServiceException dimensionTypeCdRequiredError(TransactionContext txnContext) {

        return ExceptionBuilder
            .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
            .moreInfo("UpdateShipmentDimension", "Dimension Type Cd is required")
                .build();
    }

    private ServiceException dimensionTypeCdNotValidError(TransactionContext txnContext) {

        return ExceptionBuilder
            .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
            .moreInfo("UpdateShipmentDimension", "Dimension Type Cd must be one of: " + VALID_DIM_TYPE_CDS)
            .build();
    }
}
