package org.pgsg.common.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "error")
public class ErrorConfigProperties {

	private Map<String, ErrorDetail> configs = new HashMap<>();

	@Getter
	@Setter
	public static class ErrorDetail {
		private String code;
		private int status;
		private String message;
	}
}
