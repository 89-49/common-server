package org.pgsg.common.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

	private final String errorName;	//YAML 파일의 key(예외명)
	private final String field;	//예외가 발생한 필드

	public CustomException(String errorName){
		this(errorName, null);
	}

	public CustomException(String errorName, String field) {
		super(errorName);
		this.errorName = errorName;
		this.field = field;
	}
}
