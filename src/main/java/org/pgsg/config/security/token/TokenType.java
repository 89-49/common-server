package org.pgsg.config.security.token;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenType {
	ACCESS("access"),
	REFRESH("refresh");

	private final String value;

	public boolean matches(String value) {
		return this.value.equals(value);
	}
}
