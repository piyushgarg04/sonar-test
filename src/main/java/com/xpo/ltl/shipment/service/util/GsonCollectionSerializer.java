package com.xpo.ltl.shipment.service.util;

import java.lang.reflect.Type;
import java.util.Collection;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class GsonCollectionSerializer implements JsonSerializer<Collection<?>>
{
	@Override
	public JsonElement serialize(final Collection<?> src, final Type type, final JsonSerializationContext context)
	{
		if (src == null || src.isEmpty()) {
			return null;
		}

		final JsonArray array = new JsonArray();

		for (final Object child : src) {
			if (child != null) {
				final JsonElement element = context.serialize(child);
				array.add(element);
			}
		}

		return array;
	}
}
