package com.xpo.ltl.shipment.service.dto;


public class ShipmentCntMonthDTO {

    private Long count;
    private Integer year;
    private Integer month;

    public ShipmentCntMonthDTO(Long count, Integer year, Integer month) {
        super();
        this.count = count;
        this.year = year;
        this.month = month;
    }

    public Long getCount() {
        return count;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getMonth() {
        return month;
    }

}
