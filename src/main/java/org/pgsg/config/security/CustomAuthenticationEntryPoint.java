package org.pgsg.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.pgsg.common.exception.ErrorConfigProperties;
import org.pgsg.common.exception.ErrorConfigProperties.ErrorDetail;
import org.slf4j.MDC;
import org.pgsg.common.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final ErrorConfigProperties errorConfigProperties;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        ErrorDetail detail = errorConfigProperties.getConfigs().get("InsufficientAuthenticationException");

        int status = (detail != null) ? detail.getStatus() : 401;
        String code = (detail != null) ? detail.getCode() : "C002";
        String message = (detail != null) ? detail.getMessage() : "인증에 실패했습니다. 유효한 인증 정보를 제공해주세요.";

        // traceId가 없을 경우를 대비해 안전하게 로그 기록
        String traceId = MDC.get("traceId");
        log.warn("[TraceID: {}] Unauthorized access attempt to {}: {}",
                traceId != null ? traceId : "N/A",
                request.getRequestURI(),
                authException.getMessage());

        // 응답 설정
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ErrorResponse errorResponse = ErrorResponse.of(
            HttpStatus.valueOf(status),
            code,
            null,
            message
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}