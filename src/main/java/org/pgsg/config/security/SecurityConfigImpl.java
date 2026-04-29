package org.pgsg.config.security;

import org.pgsg.common.util.SecurityUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 도메인 로직(메서드 수준)에서 권한 통제를 하기 위해 유지
@RequiredArgsConstructor
public class SecurityConfigImpl implements SecurityConfig {

    private final LoginFilter loginFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(loginFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        // Explicitly permit public endpoints
                        .requestMatchers("/favicon.ico", "/error").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .exceptionHandling(c -> {
                    // 401 Unauthorized 처리
                    c.authenticationEntryPoint(authenticationEntryPoint);

                    // 403 Forbidden 처리 (추가)
                    c.accessDeniedHandler(accessDeniedHandler);
                });

        return http.build();
    }

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        // 시스템 기본값으로 사용할 UUID (예: 관리자나 시스템 계정 ID)
        UUID DEFAULT_SYSTEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

        return () -> SecurityUtil.getCurrentUserId()
                .or(() -> Optional.of(DEFAULT_SYSTEM_ID)); // null(empty)이면 기본값 포함된 Optional 반환
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/favicon.ico", "/error");
    }
}