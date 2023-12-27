package com.xpo.ltl.shipment.service.impl.updateshipment.update.factory;

import com.xpo.ltl.shipment.service.impl.updateshipment.update.LoadValues;

public interface LoadValuesFactory {

	<S, T> LoadValues<S, T> getFactoryImplementation(Class<?> converterClass);
}
