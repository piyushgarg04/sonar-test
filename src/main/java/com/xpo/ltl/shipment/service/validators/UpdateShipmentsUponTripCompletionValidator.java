package com.xpo.ltl.shipment.service.validators;

import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.xpo.ltl.api.exception.MoreInfo;
import com.xpo.ltl.api.exception.ServiceException;
import com.xpo.ltl.api.exception.ValidationException;
import com.xpo.ltl.api.rest.TransactionContext;
import com.xpo.ltl.api.shipment.exception.ExceptionBuilder;
import com.xpo.ltl.api.shipment.exception.ValidationErrorMessage;
import com.xpo.ltl.api.shipment.v2.DispatchEquipment;
import com.xpo.ltl.api.shipment.v2.UpdateShipmentsUponTripCompletionRqst;
import com.xpo.ltl.shipment.service.util.TimestampUtil;

public class UpdateShipmentsUponTripCompletionValidator  extends ShipmentValidator {

    public void validate(UpdateShipmentsUponTripCompletionRqst request, TransactionContext txnContext) throws ServiceException, ValidationException {

        List<MoreInfo> moreInfos = new ArrayList<>();
        if (request == null) {
            addMoreInfo(moreInfos, "request", "Request is null");
        }
        else {
        	if (CollectionUtils.isNotEmpty(request.getShipmentIds())) {
        		checkShipmentIds(request.getShipmentIds(), moreInfos);
        	}

            DispatchEquipment tripCompletionEquipment = request.getTripCompletionEquipment();
            if (tripCompletionEquipment == null) {
                addMoreInfo(moreInfos, "request.tripCompletionEquipment", "tripCompletionEquipment is null");
            }
            else {
                Long eqpId = tripCompletionEquipment.getEquipmentId();
                if (eqpId == null)
                    addMoreInfo(moreInfos, "request.tripCompletionEquipment.equipmentId", "EquipmentId is null");
                else if (eqpId == 0)
                    addMoreInfo(moreInfos, "request.tripCompletionEquipment.equipmentId", "Invalid EquipmentId " + eqpId);

                String eqpIdPfx = tripCompletionEquipment.getEquipmentIdPrefix();
                if (eqpIdPfx == null)
                    addMoreInfo(moreInfos, "request.tripCompletionEquipment.equipmentIdPrefix", "EquipmentIdPrefix is null");
                else if (StringUtils.isBlank(eqpIdPfx))
                    addMoreInfo(moreInfos, "request.tripCompletionEquipment.equipmentIdPrefix", "EquipmentIdPrefix is blank");

                Long eqpIdSfx = tripCompletionEquipment.getEquipmentIdSuffixNbr();
                if (eqpIdSfx == null)
                    addMoreInfo(moreInfos, "request.tripCompletionEquipment.equipmentIdSuffixNbr", "EquipmentIdSuffixNbr is null");
                else if (eqpIdSfx == 0)
                    addMoreInfo(moreInfos, "request.tripCompletionEquipment.equipmentIdSuffixNbr", "Invalid EquipmentIdSuffixNbr " + eqpIdSfx);

                String currSic = tripCompletionEquipment.getCurrentSic();
                if (currSic == null)
                    addMoreInfo(moreInfos, "request.tripCompletionEquipment.currentSic", "CurrentSic is null");
                else if (StringUtils.isBlank(currSic))
                    addMoreInfo(moreInfos, "request.tripCompletionEquipment.currentSic", "CurrentSic is blank");
            }

            final XMLGregorianCalendar closeDateTime = request.getCloseDateTime();
            if (closeDateTime == null) {
                addMoreInfo(moreInfos, "request.closeDateTime", "closeDateTime is null");
            } else if (TimestampUtil.hasInvalidTimestampFormat(closeDateTime)) {
                addMoreInfo(moreInfos, "request.closeDateTime", "Invalid closeDateTime " + closeDateTime);
            }
        }

        if (!moreInfos.isEmpty())
            throw ExceptionBuilder.exception(ValidationErrorMessage.VALIDATION_ERRORS_FOUND, txnContext)
                    .moreInfo(moreInfos)
                    .build();
    }


}
