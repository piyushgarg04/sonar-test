package com.xpo.ltl.shipment.service.validators;

import static com.xpo.ltl.shipment.service.util.HandlingUnitHelper.MAX_HANDLING_UNIT_LENGTH;
import static com.xpo.ltl.shipment.service.util.HandlingUnitHelper.MAX_HANDLING_UNIT_WIDTH_HEIGHT;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.v2.UpdateHandlingUnitDimensionsRqst;

@ApplicationScoped
public class UpdateHandlingUnitDimensionsValidator extends Validator {

    public void validate(
            UpdateHandlingUnitDimensionsRqst updateHandlingUnitDimensionsRqst,
            String trackingProNbr,
            TransactionContext txnContext, EntityManager entityManager)
            throws ServiceException {

        // check input
        Checker.of(updateHandlingUnitDimensionsRqst)
                .must(Objects::nonNull)
                .orElseThrow(() -> requestRequiredError(txnContext));

        Checker.of(txnContext)
                .must(Objects::nonNull)
                .orElseThrow(() -> txnContextRequiredError(txnContext));

        Checker.of(entityManager)
                .must(Objects::nonNull)
                .orElseThrow(() -> entityManagerRequiredError(txnContext));

        Checker.of(trackingProNbr)
                .must(StringUtils::isNotBlank)
                .orElseThrow(() -> proNbrRequiredError(txnContext));

        // check dimensions
        validateDimension(updateHandlingUnitDimensionsRqst.getLengthNbr(), 
            updateHandlingUnitDimensionsRqst.getWidthNbr(), 
            updateHandlingUnitDimensionsRqst.getHeightNbr(),
            txnContext);
    }

    public void validateDimension(
        double lengthNbr, double widthNbr, double heightNbr,
        TransactionContext txnContext)
        throws ServiceException {

    // check dimensions
    Checker.of(lengthNbr)
            .must(Checkers.lessThanOrEq(MAX_HANDLING_UNIT_LENGTH))
            .orElseThrow(() -> lengthExceededError(txnContext));

    Checker.of(widthNbr)
            .must(Checkers.lessThanOrEq(MAX_HANDLING_UNIT_WIDTH_HEIGHT))
            .orElseThrow(() -> widthOrHeightExceededError(txnContext));

    Checker.of(heightNbr)
            .must(Checkers.lessThanOrEq(MAX_HANDLING_UNIT_WIDTH_HEIGHT))
            .orElseThrow(() -> widthOrHeightExceededError(txnContext));
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

    private ServiceException dimensionsMustBeGreaterThanZeroError(TransactionContext txnContext) {

        return ExceptionBuilder
                .exception(ValidationErrorMessage.HU_DIMENSIONS_GT_ZERO, txnContext)
                .build();
    }

    private ServiceException lengthExceededError(TransactionContext txnContext) {

        return ExceptionBuilder
                .exception(ValidationErrorMessage.LENGTH_GREATER_THAN_636, txnContext)
                .build();
    }

    private ServiceException widthOrHeightExceededError(TransactionContext txnContext) {

        return ExceptionBuilder
                .exception(ValidationErrorMessage.WIDTH_HEIGHT_GREATER_THAN_103, txnContext)
                .build();
    }

}
