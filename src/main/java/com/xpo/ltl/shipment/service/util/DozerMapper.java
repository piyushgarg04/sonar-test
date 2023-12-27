package com.xpo.ltl.shipment.service.util;

import org.dozer.DozerBeanMapper;

public class DozerMapper {

    private static DozerBeanMapper mapper = new DozerBeanMapper();

    private DozerMapper() {
    }

    public static DozerBeanMapper getInstance() {
        return mapper;
    }

}
