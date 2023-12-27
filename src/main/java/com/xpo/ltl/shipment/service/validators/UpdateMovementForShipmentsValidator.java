package com.xpo.ltl.shipment.service.validators;

import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.transformer.v2.MovementStatusCdTransformer;
import com.xpo.ltl.api.shipment.v2.Movement;
import com.xpo.ltl.api.shipment.v2.UpdateMovementForShipmentsRqst;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import org.apache.commons.lang.StringUtils;

public class UpdateMovementForShipmentsValidator extends ShipmentValidator {

    public void validate(UpdateMovementForShipmentsRqst request,
                         TransactionContext txnContext,
                         EntityManager entityManager) throws ValidationException {
        checkTransactionContext(txnContext);
        checkEntityManager(entityManager);

        List<MoreInfo> moreInfos = new ArrayList<>();
        if (request == null) {
            addMoreInfo(moreInfos, "request", "Request is null");
        } else {
            checkShipmentIds(request.getShipmentIds(),moreInfos);
            checkMovementDetails(request, moreInfos);
        }

        if (!moreInfos.isEmpty())
            throw ExceptionBuilder
                    .exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                    .moreInfo(moreInfos)
                    .build();
    }

    private void checkMovementDetails(UpdateMovementForShipmentsRqst request, List<MoreInfo> moreInfos) {
        Movement movement = request.getMovement();
        if(movement == null) {
            addMoreInfo(moreInfos, "request.movement", "Movement cannot be null");
        } else {
            if(StringUtils.isEmpty(movement.getCurrentSicCd())) {
                addMoreInfo(moreInfos, "request.movement.currentSicCd", "Movement currentSicCd cannot be null");
            }
            if(StringUtils.isEmpty(MovementStatusCdTransformer.toCode(movement.getMovementStatusCd()))) {
                addMoreInfo(moreInfos, "request.movement.movementStatusCd", "Movement movementStatusCd cannot be null");
            }
        }
    }
}
