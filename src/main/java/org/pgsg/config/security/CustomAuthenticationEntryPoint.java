package org.pgsg.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

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

        // 공통 에러 응답 포맷 생성 (가급적 ErrorCode 같은 Enum 사용 권장)
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED,
                "인증에 실패했습니다. 유효한 인증 정보를 제공해주세요."
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}