package org.pgsg.config.feign;

import java.util.List;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;

@Configuration
@EnableFeignClients("org.pgsg")
public class FeignConfig {
	private static final String HEADER_TRACE_ID = "X-Trace-Id";
	private static final String HEADER_USER_ID = "X-User-Id";
	private static final String HEADER_ROLES = "X-User-Roles";
	private static final String HEADER_NICKNAME = "X-User-NICKNAME";
	// private static final String HEADER_ENABLED = "X-User-Enabled";	//todo: 활성화 여부의 필요성 검토

	@Bean
	public RequestInterceptor requestInterceptor() {
		return tpl -> {
			ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
			if (attrs == null) return;

			HttpServletRequest req = attrs.getRequest();

			// Authorization 헤더 전파
			String auth = req.getHeader("Authorization");
			if (StringUtils.hasText(auth)) tpl.header("Authorization", auth);

			// Gateway로부터 넘어온 사용자 정보 헤더들 전파
			List<String> userHeaders = List.of(
				HEADER_TRACE_ID, HEADER_USER_ID, HEADER_NICKNAME, HEADER_ROLES
			);

			for (String headerName : userHeaders) {
				String value = req.getHeader(headerName);
				if (StringUtils.hasText(value)) {
					tpl.header(headerName, value);
				}
			}
		};
	}
}