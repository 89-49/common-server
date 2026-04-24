package org.pgsg.config.security;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LoginFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id"; // UUID 고유 사용자 id
    private static final String HEADER_USERNAME = "X-User-Username"; // 로그인 ID로 사용
    private static final String HEADER_USER_NAME = "X-User-Name";  // 실명
    private static final String HEADER_ROLES = "X-User-Roles";    // 권한
    private static final String HEADER_ENABLED = "X-User-Enabled";


    private final HandlerExceptionResolver resolver;

    public  LoginFilter(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        try {
            SecurityContextHolder.clearContext();
            doLogin(request);
        } catch (DisabledException e) {
            log.warn("[LoginFilter] Access Denied: {}", e.getMessage());
            resolver.resolveException(request, response, null, e);
            return;
        } catch (Exception e) {
            log.error("인증에 실패하였습니다: {}", e.getMessage(), e);
            resolver.resolveException(request, response, null, e);
            return;

        }

        filterChain.doFilter(request, response);
    }
    private void doLogin(HttpServletRequest request) {
        String userIdHeader = request.getHeader(HEADER_USER_ID); // UUID 고유 사용자 id
        String usernameHeader = request.getHeader(HEADER_USERNAME); // 로그인 ID로 사용

        // 직접 접근 시 헤더가 없으므로 여기서 즉시 return
        if (!StringUtils.hasText(userIdHeader) || !StringUtils.hasText(usernameHeader)) {
            return;
        }

        try {
            UUID uuid = UUID.fromString(userIdHeader);
            String username = usernameHeader.trim();
            String name = decodeHeader(request.getHeader(HEADER_USER_NAME));
            String roles = request.getHeader(HEADER_ROLES);
            String enabledStr = request.getHeader(HEADER_ENABLED);

            UserDetailsImpl userDetails = UserDetailsImpl.builder()
                .uuid(uuid)
                .username(username)
                .password("")
                .userRole(roles)
                .name(name)
                .enabled("true".equalsIgnoreCase(enabledStr))
                .build();

            if (!userDetails.isEnabled()) {
                String errorMsg = "승인 대기 중이거나 탈퇴한 사용자입니다.";
                log.warn("[LoginFilter] 접근이 제한된 사용자 입니다: {}, 사유: {}", username, errorMsg);
                throw new DisabledException(errorMsg);
            }

            // SecurityContext에 인증 객체 저장
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (IllegalArgumentException e) {
            // 잘못된 UUID 형식이 들어와도 401을 던지지 않고 익명 상태로 진행시킵니다.
            log.warn("Invalid UUID format from Gateway header: " + userIdHeader, e);
        }
    }

    private String decodeHeader(String value) {
        if (!StringUtils.hasText(value)) return "";
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("[LoginFilter] 헤더 디코딩 실패: {}", value);
            return value;
        }
    }

}