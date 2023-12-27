package com.xpo.ltl.shipment.service.validators;


import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.v2.UpdateAppointmentRequiredIndRqst;

@ApplicationScoped
public class UpdateAppointmentRequiredIndValidator extends Validator {

    public void validate(
            UpdateAppointmentRequiredIndRqst request,
            TransactionContext txnContext)
            throws ServiceException {

        // check input
        Checker.of(request)
                .must(Objects::nonNull)
                .orElseThrow(() -> requestRequiredError(txnContext));

        Checker.of(txnContext)
                .must(Objects::nonNull)
                .orElseThrow(() -> txnContextRequiredError(txnContext));

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

}
