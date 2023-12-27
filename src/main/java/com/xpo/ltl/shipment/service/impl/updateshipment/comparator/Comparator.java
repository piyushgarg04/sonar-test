package com.xpo.ltl.shipment.service.impl.updateshipment.comparator;

public interface Comparator<T> {
	boolean areEqual(T obj1, T obj2);
}
