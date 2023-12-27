package com.xpo.ltl.shipment.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.v2.CreateNonRevenueShipmentRqst;
import com.xpo.ltl.shipment.service.delegates.CreateNonRevenueShipmentDelegate;
import com.xpo.ltl.shipment.service.dto.InternalCreateNonRevenueShipmentResponseDTO;

@RequestScoped
public class CreateNonRevenueShipmentImpl {

	private static final String DEFAULT_IND_VALUE = "N";

	@Inject
    private CreateNonRevenueShipmentDelegate createNonRevenueShmDelgate;


    public InternalCreateNonRevenueShipmentResponseDTO createNonRevenueShipmentImpl(
        CreateNonRevenueShipmentRqst createNonRevenueShipmentRqst,
			TransactionContext txnContext, EntityManager entityManager) throws ServiceException {

		checkNotNull(txnContext, "The TransactionContext is required.");
		checkNotNull(entityManager, "The EntityManager is required.");
		checkNotNull(createNonRevenueShipmentRqst, "The request is required.");

		validateRequest(createNonRevenueShipmentRqst, txnContext);

        return createNonRevenueShmDelgate
            .createNonRevenueShipment(createNonRevenueShipmentRqst, txnContext, entityManager);
	}

    /**
     * Validate required request objects
     *
     * @param request
     * @param txnContext
     * @throws ValidationException
     */
    public static void validateRequest(@NotNull CreateNonRevenueShipmentRqst request, @NotNull TransactionContext txnContext)
            throws ValidationException {

        if (StringUtils.isBlank(request.getShipmentSkeleton().getParentProNbr())) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.PARENT_PRO_REQ, txnContext).build();
        }
        if (CollectionUtils.isEmpty(request.getCommodities())) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.COMMODITY_GROUP_EMPTY, txnContext).build();
        }
        if (StringUtils.isBlank(request.getShipmentSkeleton().getRequestingSicCd())) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.ORIGIN_SIC_CD_REQ, txnContext).build();
        }
        if (StringUtils.isBlank(request.getShipmentSkeleton().getDestinationTerminalSicCd())) {
            throw ExceptionBuilder.exception(ValidationErrorMessage.DEST_SIC_CD_REQ, txnContext).build();
        }

    }


}
