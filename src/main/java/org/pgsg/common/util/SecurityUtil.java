package org.pgsg.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.pgsg.common.exception.UnAuthorizedException;
import org.pgsg.config.security.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

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
		return getCurrentUserId().orElseThrow(UnAuthorizedException::new);	//todo: 공통 예외 설정 후 수정여부 확인
	}

	public static Optional<String> getCurrentUsername() {
		return getCurrentUser().map(UserDetailsImpl::getUsername);
	}
}
