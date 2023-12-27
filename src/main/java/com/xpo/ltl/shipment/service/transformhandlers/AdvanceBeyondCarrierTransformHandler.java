package com.xpo.ltl.shipment.service.transformhandlers;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import com.xpo.ltl.api.shipment.service.entity.ShmAdvBydCarr;
import com.xpo.ltl.api.shipment.v2.AdvanceBeyondCarrier;
import com.xpo.ltl.api.shipment.v2.AdvanceBeyondCarrier_;
import com.xpo.ltl.api.transformer.BasicTransformer;
import com.xpo.ltl.api.transformer.EntityTransformHandler;

public class AdvanceBeyondCarrierTransformHandler {

	private static FastDateFormat DATE_SLASH = FastDateFormat.getInstance("MM/dd/yy");
	
	public static EntityTransformHandler<AdvanceBeyondCarrier, ShmAdvBydCarr, AdvanceBeyondCarrier_> getHandlerForFBDS() {
		return new EntityTransformHandler<AdvanceBeyondCarrier, ShmAdvBydCarr, AdvanceBeyondCarrier_>() {
			@Override
			public String afterTransformDate(AdvanceBeyondCarrier_ attribute, String date) {
				return (attribute == AdvanceBeyondCarrier_.carrierPickupDate && StringUtils.isNotBlank(date))
						? DATE_SLASH.format(BasicTransformer.toDate(date)) 
						: date;
			}
		};
	}
}
