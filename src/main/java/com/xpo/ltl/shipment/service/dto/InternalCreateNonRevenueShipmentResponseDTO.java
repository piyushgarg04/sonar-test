package com.xpo.ltl.shipment.service.dto;

import java.util.Optional;

import com.xpo.ltl.api.shipment.v2.DataValidationError;

public class InternalCreateNonRevenueShipmentResponseDTO {

    private Optional<DataValidationError> dataValidationError = Optional.empty();
    private Long nonRevenueShipmentInstId;

    public Optional<DataValidationError> getDataValidationError() {
        return dataValidationError;
    }

    public void setDataValidationError(Optional<DataValidationError> dataValidationError) {
        this.dataValidationError = dataValidationError;
    }

    public Long getNonRevenueShipmentInstId() {
        return nonRevenueShipmentInstId;
    }

    public void setNonRevenueShipmentInstId(Long nonRevenueShipmentInstId) {
        this.nonRevenueShipmentInstId = nonRevenueShipmentInstId;
    }

}
