package com.xpo.ltl.shipment.service.util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.xpo.ltl.api.transformer.BasicTransformer;

public class DB2DefaultValueUtil {

    public static final Timestamp LOW_TMST = BasicTransformer.toTimestamp("0001-01-01-00.00.00");

    public static String getValueOrSpace(String str) {
        return str == null ? StringUtils.SPACE : str;
    }

    public static BigDecimal getValueOr0(BigDecimal bigDecimal) {
        return bigDecimal == null ? BigDecimal.ZERO : bigDecimal;
    }

    public static Date getValueOrLowTmst(Date tmst) {
        return tmst == null ? LOW_TMST : tmst;
    }

    public static Timestamp getValueOrLowTmst(Timestamp tmst) {
        return tmst == null ? LOW_TMST : tmst;
    }

}
