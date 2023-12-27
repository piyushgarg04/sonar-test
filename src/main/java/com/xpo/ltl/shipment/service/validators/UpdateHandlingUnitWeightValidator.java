package com.xpo.ltl.shipment.service.validators;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.v2.UpdateHandlingUnitWeightRqst;

@ApplicationScoped
public class UpdateHandlingUnitWeightValidator extends Validator {

    public void validate(
        UpdateHandlingUnitWeightRqst updateHandlingUnitWeightRqst,
        String childProNbr, TransactionContext txnContext, EntityManager entityManager) throws ServiceException {

        // check input
        Checker
            .of(updateHandlingUnitWeightRqst)
                .must(Objects::nonNull)
                .orElseThrow(() -> requestRequiredError(txnContext));

        Checker.of(txnContext)
                .must(Objects::nonNull)
                .orElseThrow(() -> txnContextRequiredError(txnContext));

        Checker.of(entityManager)
                .must(Objects::nonNull)
                .orElseThrow(() -> entityManagerRequiredError(txnContext));

        Checker.of(childProNbr)
                .must(StringUtils::isNotBlank)
                .orElseThrow(() -> proNbrRequiredError(txnContext));

         Checker.of(updateHandlingUnitWeightRqst.getWeightLbs())
         .must(Checkers::greaterThanZero)
         .orElseThrow(() -> weightMininumError(txnContext));
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

    private ServiceException weightMininumError(TransactionContext txnContext) {

        return ExceptionBuilder
                .exception(ValidationErrorMessage.SHPMT_WEIGHT_GREATER_ZERO, txnContext)
                .build();
    }

}
