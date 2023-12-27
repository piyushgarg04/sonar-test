package com.xpo.ltl.shipment.service.impl.updateshipment.comparator;

import com.xpo.ltl.shipment.service.impl.updateshipment.update.abc.AbstractUpdate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class EntityComparer extends AbstractUpdate {

	private EntityComparer() {
	}

	public static <T> List<String> findDifferences(T obj1, T obj2, Comparator<T> comparator) {
		List<String> differences = new ArrayList<>();
		Field[] fields = obj1.getClass().getDeclaredFields();

		try {

			for (Field field : fields) {
				if (!field.getName().equalsIgnoreCase("serialVersionUID") && !field.getName().contains("$")) {
					Object value1 = getFieldValue(obj1, field);
					Object value2 = getFieldValue(obj2, field);
					if (!comparator.areEqual((T) value1, (T) value2)) {
						differences.add(field.getName() + " value1: " + value1 + " value2: " + value2);
					}
				}
			}
			return differences;
		} catch (ReflectiveOperationException e) {
			throw new UnsupportedOperationException("Unable to compare objects", e);
		}
	}

	private static Object getFieldValue(Object object, Field field) throws ReflectiveOperationException {
		String fieldName = field.getName();
		String getterName = "get" + capitalizeFirstLetter(fieldName);
		Method getterMethod = object.getClass().getMethod(getterName);
		return getterMethod.invoke(object);
	}

}
