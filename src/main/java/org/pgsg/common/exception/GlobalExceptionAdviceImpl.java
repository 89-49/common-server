package org.pgsg.common.exception;

import org.pgsg.common.exception.ErrorConfigProperties.ErrorDetail;
import org.pgsg.common.response.ErrorResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice("org.pgsg")
@RequiredArgsConstructor
public class GlobalExceptionAdviceImpl implements GlobalExceptionAdvice {

	private final ErrorConfigProperties errorConfigProperties;

	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
		// YAML에서 공통 정보 가져옴
		ErrorDetail detail=errorConfigProperties.getConfigs().get(e.getErrorName());
		
		//yaml에서 정의하지 않은 에러 발생
		if (detail == null) {
			log.error("[TraceID: {}] 정의되지 않은 에러: {}", MDC.get("traceId"), e.getErrorName());
			return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR) // 500
				.body(ErrorResponse.of(
					HttpStatus.valueOf(500),                    	// errorCode (상태코드와 동일하게 혹은 "SYSTEM_ERROR")
					e.getField(),           								// 어떤 필드에서 터졌는지 (예외에 담긴 값)
					"서버 내부 오류가 발생했습니다." // 상세 메시지
				));
		}

		int httpStatus=detail.getStatus();
		
		// MDC에서 traceId를 가져와 로그에 명시적으로 출력
		log.error("[TraceID: {}] Custom Exception: {}", MDC.get("traceId"), e.getMessage(), e);

		return ResponseEntity
			.status(httpStatus)
			.body(ErrorResponse.of(HttpStatus.valueOf(httpStatus), e.getField(), detail.getMessage()));
	}

}
