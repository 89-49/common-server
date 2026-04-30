package org.pgsg.config.security.jwt;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JwtUtils {

	public static final String BEARER_PREFIX = "Bearer ";

	// JWT Claim Keys
	public static final String CLAIM_TOKEN_TYPE = "token_type";
	public static final String CLAIM_USER_ROLE = "role";
	public static final String CLAIM_USERNAME = "username";
	public static final String CLAIM_NAME = "name";
	public static final String CLAIM_NICKNAME = "nickname";
	public static final String CLAIM_ENABLED = "enabled";

	// 게이트웨이 및 user-service에서 공통으로 사용할 상수
	public static final String HEADER_USER_ID = "X-User-Id";
	public static final String HEADER_USERNAME = "X-User-Username";
	public static final String HEADER_ROLES = "X-User-Roles";
	public static final String HEADER_USER_NAME = "X-User-Name";
	public static final String HEADER_USER_NICKNAME = "X-User-Nickname";
	public static final String HEADER_ENABLED = "X-User-Enabled";

	/**
	 * Authorization 헤더 값에서 "Bearer " 접두사를 제거하고 순수 토큰만 추출합니다.
	 */
	public static String resolveToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			return null;
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}

	/**
	 * 토큰에서 "Bearer " 접두사를 제거하여 순수 토큰 문자열만 반환합니다.
	 * 접두사가 없거나 이미 순수 토큰인 경우 그대로 반환합니다.
	 */
	public static String normalizeToken(String token) {
		if (token == null) {
			return null;
		}
		return token.startsWith(BEARER_PREFIX)
				? token.substring(BEARER_PREFIX.length())
				: token;
	}
}
