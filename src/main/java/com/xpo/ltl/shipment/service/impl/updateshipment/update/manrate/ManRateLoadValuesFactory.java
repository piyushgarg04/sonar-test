package com.xpo.ltl.shipment.service.impl.updateshipment.update.manrate;

import com.xpo.ltl.api.shipment.service.entity.ShmAcSvc;
import com.xpo.ltl.api.shipment.service.entity.ShmShipment;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.LoadValues;
import com.xpo.ltl.shipment.service.impl.updateshipment.update.factory.LoadValuesFactory;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ManRateLoadValuesFactory implements LoadValuesFactory {

	private static <S, T> LoadValues<S, T> castToConverter(Object converter) {
		return (LoadValues<S, T>) converter;
	}

	public <S, T> LoadValues<S, T> getFactoryImplementation(Class<?> converterClass) {
		if (converterClass.equals(ShmShipment.class)) {
			return castToConverter(new ManRateLoadShipmentValuesImpl());
		} else if (converterClass.equals(ShmAcSvc.class)) {
			return castToConverter(new ManRateLoadAccessorialValuesImpl());
		} else {
			throw new IllegalArgumentException("Not implementation for " + converterClass.getName());
		}
	}

}



