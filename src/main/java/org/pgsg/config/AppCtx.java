package org.pgsg.config;

import org.pgsg.config.feign.FeignConfig;
import org.pgsg.config.json.JsonConfig;
import org.pgsg.config.kafka.KafkaConfig;
import org.pgsg.config.persistence.JPAConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.fasterxml.jackson.databind.ObjectMapper;

// 스프링 부트의 자동 설정 매커니즘에 참여하여 라이브러리 로드 시 자동 실행됨
@AutoConfiguration
@Import({
	FeignConfig.class,
	JPAConfig.class,
	JsonConfig.class,
	KafkaConfig.class
})
public class AppCtx {

	@Bean
	public LoginFilter loginFilter(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
		return new LoginFilter(resolver);
	}

	@Bean
	public CustomAuthenticationEntryPoint customAuthenticationEntryPoint(ObjectMapper  objectMapper) {
		return new CustomAuthenticationEntryPoint(objectMapper);
	}

	@Bean
	public CustomAccessDeniedHandler accessDeniedHandler(ObjectMapper  objectMapper) {
		return new CustomAccessDeniedHandler(objectMapper);
	}


	// SecurityConfig로 등록된 빈이 없다면 등록
	@Bean
	@ConditionalOnMissingBean(SecurityConfig.class)
	public SecurityConfig securityConfig(LoginFilter loginFilter, CustomAuthenticationEntryPoint customAuthenticationEntryPoint, CustomAccessDeniedHandler accessDeniedHandler) {
		return new SecurityConfigImpl(loginFilter, customAuthenticationEntryPoint, accessDeniedHandler);
	}

	// 전역 에러 출력 처리, GlobalExceptionAdvice로 등록된 빈이 없을때 기본 설정으로 등록됨
	@Bean
	@ConditionalOnMissingBean(GlobalExceptionAdvice.class)
	public GlobalExceptionAdvice globalExceptionAdvice() {
		return new GlobalExceptionAdviceImpl();
	}

	@Bean
	public CommonResponseAdvice commonResponseAdvice() {
		return new CommonResponseAdvice();
	}

	// MDC 기반의 로깅 추적을 위한 필터를 스프링 컨테이너에 등록함
	@Bean
	public FilterRegistrationBean<MdcLoggingFilter> mdcLoggingFilter() {
		FilterRegistrationBean<MdcLoggingFilter> registrationBean = new FilterRegistrationBean<>();
		registrationBean.setFilter(new MdcLoggingFilter());
		registrationBean.addUrlPatterns("/*");
		registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE); // 가장 먼저 적용되도록 우선순위를 가장 높게 지정(가장 작은 정수 범위)
		return registrationBean;
	}
}
