package com.xpo.ltl.shipment.service.impl.updateshipment;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;

public class MockParent {

	public static final Object jsonStringToObject(Class<?> type, String value) {
		try {
			ObjectMapper objectMapper;
			objectMapper = new ObjectMapper();
			objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
			Reader reader = new StringReader(value);
			return objectMapper.readValue(reader, type);
		} catch (Exception e) {

		}
		return null;
	}

	public String getJsonFromProperty(String key) {
		String sValue = null;
		try (InputStream input = new FileInputStream("src/test/resources/test.properties")) {
			Properties prop = new Properties();
			prop.load(input);
			sValue = prop.getProperty(key);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		//

		return sValue;
	}
}
