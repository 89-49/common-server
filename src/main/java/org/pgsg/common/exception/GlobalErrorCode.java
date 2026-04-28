package org.pgsg.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

    ACCESS_DENIED("AccessDeniedException"),
    UNAUTHORIZED("UnauthorizedException"),

    ;

    private final String errorKey;

}
