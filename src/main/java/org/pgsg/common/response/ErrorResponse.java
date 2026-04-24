package org.pgsg.common.response;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.time.LocalDateTime;

public record ErrorResponse(
	int status, // HTTP 상태 코드
	String code, // 커스텀 에러 코드, 해당 에러가 어떤 서비스에서 발생했는지 확인
	String error, // 에러 명칭, NOT_FOUND, BAD_REQUEST
	Object message, // 에러 상세 메시지(단문 또는 {필드: 메세지, 필드: 메세지 ...}
	String field, // 에러 발생 필드
	String traceId, // 로깅 추적을 위한 고유 ID (MDC 기반)
	LocalDateTime timestamp
) {
	public static ErrorResponse of(HttpStatusCode status, String code, Object message) {
		return new ErrorResponse(
			status.value(),
			code,
			resolveErrorName(status),
			message,
			null,
			MDC.get("traceId"),
			LocalDateTime.now()
		);
	}

	public static ErrorResponse of(HttpStatusCode status, String code ,String field, Object message) {
		return new ErrorResponse(
			status.value(),
			code,
			resolveErrorName(status),
			message,
			field,
			MDC.get("traceId"),
			LocalDateTime.now()
		);
	}

	private static String resolveErrorName(HttpStatusCode status) {
		if (status instanceof HttpStatus httpStatus) {
			return httpStatus.name(); //
		}
		return status.toString(); // 예외 케이스 처리
	}
}
