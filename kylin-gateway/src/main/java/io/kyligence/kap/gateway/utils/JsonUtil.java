package io.kyligence.kap.gateway.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

@Slf4j
public class JsonUtil {
	private static final ObjectMapper mapper = new ObjectMapper();

	static {
		mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	public static String toJson(Object obj) {
		if (obj == null) {
			return null;
		}
		String result = null;
		try {
			result = mapper.writeValueAsString(obj);
		} catch (JsonProcessingException ex) {
			log.error("Failed to serialize Object", ex);
		}
		return result;
	}

	public static <T> T toObject(String json, Class<T> valueType) {
		if (Strings.isNullOrEmpty(json)) {
			return null;
		}
		T object = null;
		try {
			log.debug("json: {}", json);
			object = mapper.readValue(json, valueType);
		} catch (Exception ex) {
			log.error("Failed to deserialize Object", ex);
		}
		return object;
	}

	public static <T> T toObject(String str, TypeReference<T> typeReference) {
		if (StringUtils.isEmpty(str) || typeReference == null) {
			return null;
		}
		try {
			return (T) (typeReference.getType().equals(String.class) ? str : mapper.readValue(str, typeReference));
		} catch (Exception e) {
			log.warn("Parse String to Object error", e);
			return null;
		}
	}
}
