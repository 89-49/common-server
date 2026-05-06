package org.pgsg.config.json;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.pgsg.common.util.JsonUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class JsonConfig {
	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper om = new ObjectMapper();
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		om.registerModule(new JavaTimeModule());
		// 날짜를 숫자 배열로 나타내는 대신 ISO-8601 형식을 적용하기 위한 설정
		om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		return om;
	}

	@Bean
	public InitializingBean jsonUtilInitializer(ObjectMapper objectMapper) {
		return () -> JsonUtil.setObjectMapper(objectMapper);
	}
}
