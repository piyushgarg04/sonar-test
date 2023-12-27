package com.xpo.ltl.shipment.service.validators;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;

import com.xpo.ltl.api.exception.ErrorMessageIF;
import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;

public abstract class Validator {

    private static final String ENTITY_MANAGER = EntityManager.class.getSimpleName();
    private static final String TRANSACTION_CONTEXT = TransactionContext.class.getSimpleName();
    private static final String PARAM_IS_REQUIRED_MODEL = "%s is required";

    protected MoreInfo createMoreInfo(String location, String message) {
        MoreInfo moreInfo = new MoreInfo();
        moreInfo.setMessage(message);
        moreInfo.setLocation(location);
        return moreInfo;
    }

    protected void addMoreInfo(List<MoreInfo> moreInfos, String location, String message) {
        moreInfos.add(createMoreInfo(location, message));
    }

	protected void addMoreInfo(List<MoreInfo> moreInfo,
            String location,
            ErrorMessageIF error,
            Object... contextValues) {
		addMoreInfo(moreInfo, null, location, error, contextValues);
	}
	
	protected void addMoreInfo(List<MoreInfo> moreInfo,
            Integer itemNbr,
            String location,
            ErrorMessageIF error,
            Object... contextValues) {
		String message;
		if (contextValues != null)
			message = error.message(Arrays.stream(contextValues).map(Object::toString).toArray(String[]::new));
		else
			message = error.message();
		moreInfo.add(createMoreInfoStatic(itemNbr, location, message));
	}
	
    protected static void checkMoreInfo(TransactionContext txnContext, List<MoreInfo> moreInfo)
            throws ValidationException {
        if (CollectionUtils.isNotEmpty(moreInfo)) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                .moreInfo(moreInfo)
                .build();
        }
    }

    protected void checkTransactionContext(TransactionContext txnContext) {
        checkNotNull(txnContext, String.format(PARAM_IS_REQUIRED_MODEL, TRANSACTION_CONTEXT));
    }

    protected void checkEntityManager(EntityManager entityManager) {
        checkNotNull(entityManager, String.format(PARAM_IS_REQUIRED_MODEL, ENTITY_MANAGER));
    }

	protected static MoreInfo createMoreInfoStatic(Integer itemNbr, String location, String message) {
		MoreInfo moreInfo = new MoreInfo();
		moreInfo.setItemNbr(itemNbr);
		moreInfo.setMessage(message);
		moreInfo.setLocation(location);
		return moreInfo;
	}

}
