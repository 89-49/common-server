package org.pgsg.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

    // Security & Auth
    ACCESS_DENIED("AccessDeniedException"),
    UNAUTHORIZED("UnauthorizedException"),

    // Validation & Request
    INVALID_INPUT_VALUE("MethodArgumentNotValidException"),
    METHOD_NOT_ALLOWED("HttpRequestMethodNotSupportedException"),

    // Resource
    ENTITY_NOT_FOUND("EntityNotFoundException"),

    // Server Error
    INTERNAL_SERVER_ERROR("RuntimeException"),
    GATEWAY_TIMEOUT("GatewayTimeoutException"),
    ;

    private final String errorKey;
}
