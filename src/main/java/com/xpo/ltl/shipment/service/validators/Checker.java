package com.xpo.ltl.shipment.service.validators;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class Checker<T> {

    private T input;

    private Predicate<T> predicate;

    public Checker(T input) {
        this.input = input;
    }

    public static <T> Checker<T> of(T input) {

        return new Checker<T>(input);
    }

    public Checker<T> must(Predicate<T> predicate) {

        this.predicate = predicate;
        return this;
    }

    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {

        if (this.predicate.test(input)) {
            return input;

        } else {
            throw exceptionSupplier.get();
        }
    }
}
