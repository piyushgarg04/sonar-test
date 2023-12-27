package com.xpo.ltl.shipment.service.validators;

import java.math.BigDecimal;
import java.util.function.Predicate;

public class Checkers {

    public static Boolean greaterThanZero(Double v) {

        return v > 0;
    }

    public static Predicate<Double> lessThanOrEq(Double max) {

        return v -> v <= max;
    }

    public static Boolean lessThanOrEq(BigDecimal left, BigDecimal right) {

        return left.compareTo(right) <= 0;
    }
}
