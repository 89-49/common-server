package org.pgsg.common.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

	private final ErrorCode errorCode;
	private final String field;	//예외가 발생한 필드

	public CustomException(ErrorCode errorCode) {
		this(errorCode, null);
	}

	public CustomException(ErrorCode errorCode, String field) {
        this.errorCode = errorCode;
		this.field = field;
	}
}
