package org.pgsg.common.exception;

import org.pgsg.common.exception.ErrorConfigProperties.ErrorDetail;
import org.pgsg.common.response.ErrorResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
		String errorKey = e.getErrorCode().getErrorKey();
		ErrorDetail detail = errorConfigProperties.getConfigs().get(errorKey);

		if (detail == null) {
			log.error("[TraceID: {}] Undefined Error Key: field={}, errorKey={}",
					MDC.get("traceId"), e.getField(), errorKey, e);

			return ResponseEntity
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, e.getField(), "정의되지 않은 서버 에러가 발생했습니다."));
		}

		log.error("[TraceID: {}] CustomException: field={}, errorKey={}, message={}",
				MDC.get("traceId"), e.getField(), errorKey, detail.getMessage(), e);

		HttpStatus status = HttpStatus.valueOf(detail.getStatus());

		return ResponseEntity
				.status(status)
				.body(ErrorResponse.of(status, detail.getCode(), detail.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
		log.error("[TraceID: {}] MethodArgumentNotValidException: {}",
				MDC.get("traceId"), e.getMessage(), e);
		
		ErrorDetail detail = errorConfigProperties.getConfigs().get(GlobalErrorCode.INVALID_INPUT_VALUE.getErrorKey());

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(ErrorResponse.of(HttpStatus.BAD_REQUEST, detail.getCode(), detail.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception e) {
		ErrorDetail detail = errorConfigProperties.getConfigs().get(GlobalErrorCode.INTERNAL_SERVER_ERROR.getErrorKey());

		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, detail.getCode(), detail.getMessage()));
	}
}
