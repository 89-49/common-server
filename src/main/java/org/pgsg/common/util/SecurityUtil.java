package org.pgsg.common.util;

import java.util.Optional;
import java.util.UUID;

import org.pgsg.common.exception.CustomException;
import org.pgsg.config.security.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SecurityUtil {

	public static Optional<UserDetailsImpl> getCurrentUser() {	//todo: config 설정 후 다시 확인
		return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
			.map(Authentication::getPrincipal)
			.filter(principal -> principal instanceof UserDetailsImpl)
			.map(UserDetailsImpl.class::cast);
	}

	public static Optional<UUID> getCurrentUserId() {
		return getCurrentUser().map(UserDetailsImpl::getUuid);
	}

	public static UUID getCurrentUserIdOrThrow() {
		// UnauthorizedException 클래스가 없어 컴파일 에러 발생 -> CustomException으로 대체
		return getCurrentUserId().orElseThrow(() -> new CustomException("UnAuthorizedException"));
	}

	public static Optional<String> getCurrentUsername() {
		return getCurrentUser().map(UserDetailsImpl::getUsername);
	}
}
