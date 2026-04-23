package org.pgsg.common.util;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonUtil {
	private static ObjectMapper objectMapper;


	@Autowired	//todo: 서비스 구현 시 Autowired 작동 여부 확인
	public void init(ObjectMapper objectMapper) {
		JsonUtil.objectMapper = objectMapper;
	}

	public static String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			log.error("JSON 변환 실패: {}", e.getMessage(), e);
			return null;
		}
	}

	public static <T> T fromJson(String json, Class<T> clazz) {
		try {
			return objectMapper.readValue(json, clazz);
		} catch (JsonProcessingException e) {
			log.error("Java 객체 변환(Class) 실패: {}", e.getMessage(), e);
			return null;
		}
	}

	public static <T> T fromJson(String json, TypeReference<T> typeReference) {
		try {
			return objectMapper.readValue(json, typeReference);
		} catch (JsonProcessingException e) {
			log.error("Java 객체 변환(TypeReference) 실패: {}", e.getMessage(), e);
			return null;
		}
	}
}