package org.pgsg.config.security.token;

import io.jsonwebtoken.Claims;
import org.pgsg.config.security.UserDetailsImpl;

import java.util.UUID;

public interface TokenProvider {

    // 사용자 식별값과 권한 정보를 바탕으로 토큰 생성
    TokenPair createTokenPair(UserDetailsImpl userDetails);

    // 토큰에서 사용자 식별값(Subject) 추출
    UUID getUserId(String token);

    // 토큰 유효성 및 만료 여부 확인
    boolean validateToken(String token);

    // 만료된 토큰에서도 클레임을 추출 (재발급 로직용)
    String getSubjectFromExpiredAccessToken(String accessToken);

    Claims parseClaims(String token);

    // 토큰의 남은 유효 시간(밀리초) 계산
    long getRemainingTime(String token);
}