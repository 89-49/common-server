package org.pgsg.common.exception;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "error")
public class ErrorConfigProperties {
	private Map<String ,ErrorDetail> configs;

	@Getter @Setter
	public static class ErrorDetail {
		private String code;
		private String message;
		private int status;
	}
}
