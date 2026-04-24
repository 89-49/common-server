package org.pgsg.config.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.pgsg.common.exception.ErrorConfigProperties;
import org.pgsg.common.exception.ErrorConfigProperties.ErrorDetail;
import org.pgsg.common.response.ErrorResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final ErrorConfigProperties errorConfigProperties;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        String traceId = MDC.get("traceId");

        ErrorDetail detail = errorConfigProperties.getConfigs().get("AccessDeniedException");

        int status = (detail != null) ? detail.getStatus() : 403;
        String code = (detail != null) ? detail.getCode() : "C003";
        String message = (detail != null) ? detail.getMessage() : "접근 권한이 없습니다.";

        // traceId가 없을 경우를 대비해 안전하게 로그 기록
        log.warn("[TraceID: {}] Access Denied: Method: {}, URI: {}, Message: {}",
                traceId != null ? traceId : "N/A",
                request.getMethod(),
                request.getRequestURI(),
                accessDeniedException.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
            HttpStatus.valueOf(status),
            code,
            message
        );

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setStatus(status);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}