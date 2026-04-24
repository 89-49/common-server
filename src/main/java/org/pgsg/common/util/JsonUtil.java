package org.pgsg.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonUtil {
	private static ObjectMapper objectMapper;

	public static String toJson(Object obj) {
		try {
			return getObjectMapper().writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			log.error("JSON 변환 실패: {}", e.getMessage(), e);
			return null;
		}
	}

	public static <T> T fromJson(String json, Class<T> clazz) {
		try {
			return getObjectMapper().readValue(json, clazz);
		} catch (JsonProcessingException e) {
			log.error("Java 객체 변환(Class) 실패: {}", e.getMessage(), e);
			return null;
		}
	}

	public static <T> T fromJson(String json, TypeReference<T> typeReference) {
		try {
			return getObjectMapper().readValue(json, typeReference);
		} catch (JsonProcessingException e) {
			log.error("Java 객체 변환(TypeReference) 실패: {}", e.getMessage(), e);
			return null;
		}
	}

	public static void setObjectMapper(ObjectMapper mapper) {
		JsonUtil.objectMapper = mapper;
	}

	private static ObjectMapper getObjectMapper(){
		if(objectMapper == null){
			objectMapper = new ObjectMapper();
		}
		return objectMapper;
	}
}