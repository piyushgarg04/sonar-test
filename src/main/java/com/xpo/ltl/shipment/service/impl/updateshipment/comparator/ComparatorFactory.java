package com.xpo.ltl.shipment.service.impl.updateshipment.comparator;

import java.math.BigDecimal;

public class ComparatorFactory {
	private ComparatorFactory() {
	}

	public static <T> Comparator<T> createStrictComparator() {
		return (value1, value2) -> {
			if (value1 == null && value2 == null) {
				return true;
			} else if (value1 == null || value2 == null) {
				return false;
			} else {
				if (value1 instanceof BigDecimal) {
					BigDecimal v1 = (BigDecimal) value1;
					BigDecimal v2 = (BigDecimal) value2;
					return v1.compareTo(v2) == 0;
				} else if (value1 instanceof String) {

					String v1 = (String) value1;
					String v2 = (String) value2;
					return v1.trim().equals(v2.trim());

				} else {
					return value1.equals(value2);
				}
			}
		};
	}
}
