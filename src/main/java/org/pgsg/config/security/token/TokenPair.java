package org.pgsg.config.security.token;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TokenPair {
    private final String accessToken;
    private final String refreshToken;
    private final Long accessTokenExpiresIn;

    public static TokenPair of(String accessToken, String refreshToken, Long expiresIn) {
        Objects.requireNonNull(accessToken, "액세스 토큰은 null일 수 없습니다.");
        Objects.requireNonNull(refreshToken, "리프레시 토큰은 null일 수 없습니다.");
        Objects.requireNonNull(expiresIn, "만료 시간은 null일 수 없습니다.");

        // 빈 문자열 및 공백 체크
        if (accessToken.isBlank() || refreshToken.isBlank()) {
            throw new IllegalArgumentException("토큰 값은 비어 있거나 공백일 수 없습니다.");
        }
        // 시간 유효성 체크
        if (expiresIn <= 0) {
            throw new IllegalArgumentException("만료 시간은 0보다 커야 합니다.");
        }

        return new TokenPair(accessToken, refreshToken, expiresIn);
    }
}