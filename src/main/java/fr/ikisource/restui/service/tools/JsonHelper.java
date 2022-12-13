package fr.ikisource.restui.service.tools;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonHelper {

	private static ObjectMapper objectMapper;

	private static ObjectMapper getObjectMapper() {

		if (objectMapper == null) {
			objectMapper = new ObjectMapper();
		}
		return objectMapper;
	}

	public static String pretty(final String value) throws IOException {

		String pretty = "";

		Object json = getObjectMapper().readValue(value == null ? "" : value, Object.class);
		pretty = getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(json);

		return pretty;
	}

}
