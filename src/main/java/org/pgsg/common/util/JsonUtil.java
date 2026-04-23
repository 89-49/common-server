package org.pgsg.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonUtil {

	private final ObjectMapper objectMapper;
	private static ObjectMapper mapper;

	@PostConstruct
	public void init() {
		mapper = this.objectMapper;
	}

	public static String toJson(Object obj) {
		try {
			return mapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			log.error("JSON 변환 실패: {}", e.getMessage(), e);
			return null;
		}
	}

	public static <T> T fromJson(String json, Class<T> clazz) {
		try {
			return mapper.readValue(json, clazz);
		} catch (JsonProcessingException e) {
			log.error("Java 객체 변환(Class) 실패: {}", e.getMessage(), e);
			return null;
		}
	}

	public static <T> T fromJson(String json, TypeReference<T> typeReference) {
		try {
			return mapper.readValue(json, typeReference);
		} catch (JsonProcessingException e) {
			log.error("Java 객체 변환(TypeReference) 실패: {}", e.getMessage(), e);
			return null;
		}
	}
}