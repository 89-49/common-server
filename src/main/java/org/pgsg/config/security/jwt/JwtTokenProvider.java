package org.pgsg.config.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pgsg.config.security.UserDetailsImpl;
import org.pgsg.config.security.token.TokenPair;
import org.pgsg.config.security.token.TokenProvider;
import org.pgsg.config.security.token.TokenType;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class JwtTokenProvider implements TokenProvider {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    /**
     * 의존성 주입 완료 후, 설정 파일(yml)에서 가져온 SecretKey를
     * JWT 서명에 사용할 수 있는 Key 객체로 변환하여 초기화합니다.
     */
    @PostConstruct
    protected void init() {
		// 32자 이상의 아무 문자열이나 사용해도 JWT secret key로 변환 가능하도록, 먼저 base64로 인코딩한 후 키를 생성하도록 수정
        String base64UrlEncoded = Base64.getUrlEncoder()
                .encodeToString(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));

        byte[] keyBytes = Base64.getUrlDecoder().decode(base64UrlEncoded);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 사용자의 식별값(UUID)과 권한을 받아 Access Token과 Refresh Token 쌍을 생성합니다.
     * 생성된 토큰들은 TokenPair VO 객체에 담겨 반환됩니다.
     */
    @Override
    public TokenPair createTokenPair(UserDetailsImpl userDetails) {
        String accessToken = createAccessToken(userDetails, jwtProperties.getAccessTokenExpiration());
        String refreshToken = createRefreshToken(userDetails, jwtProperties.getRefreshTokenExpiration());

		return TokenPair.of(accessToken, refreshToken, jwtProperties.getAccessTokenExpiration());
    }

    /**
     * 토큰 내부의 Payload에서 사용자의 식별값(Subject)을 추출하여 UUID 형태로 반환합니다.
     */
    @Override
    public UUID getUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    /**
     * 토큰의 유효성을 검사합니다.
     * 서명 위조, 만료 여부, 형식 오류 등을 체크하며 문제가 없으면 true를 반환합니다.
     */
    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(JwtUtils.normalizeToken(token));
            return true;
        } catch (ExpiredJwtException e) {
			log.info("만료된 JWT 토큰입니다.");
            return false;
        } catch (SignatureException | MalformedJwtException e) {
			log.warn("잘못된 JWT 서명입니다.");
			return false;
		} catch (UnsupportedJwtException | IllegalArgumentException e) {
			log.warn("지원되지 않는 JWT 토큰이거나 잘못된 JWT 토큰입니다.");
			return false;
		}
    }

    /**
     * 만료된 Access Token으로부터 사용자의 식별값(Subject)을 추출합니다.
     * 토큰 재발급(Reissue) 시, 만료된 토큰의 정보를 확인하기 위해 사용됩니다.
     */
    @Override
    public String getSubjectFromExpiredAccessToken(String accessToken) {
        try {
            // 만료되지 않은 경우 정상적으로 Subject 추출
            return parseClaims(accessToken).getSubject();
        } catch (ExpiredJwtException e) {
            // 만료된 경우 예외 객체에 담긴 Claims에서 Subject 추출
            return e.getClaims().getSubject();
        }
    }

    /**
     * 실제 accessToken을 생성하는 공통 내부 메서드입니다.
     * 로그인한 사용자의 인증 정보 중 사용자 식별용 uuid를 Subject에 넣고, 나머지 인증 정보를 Custom Claim에 추가하여 서명합니다.
     */
    private String createAccessToken(UserDetails userDetails, Long expiration) {
        Date now = new Date();
        UserDetailsImpl userDetailsInfo = (UserDetailsImpl) userDetails;
		String accessToken = Jwts.builder()
				.subject(userDetailsInfo.getUuid().toString())
				.claim(JwtUtils.CLAIM_TOKEN_TYPE, TokenType.ACCESS.getValue())
				.claim(JwtUtils.CLAIM_USERNAME, userDetailsInfo.getUsername())
				.claim(JwtUtils.CLAIM_USER_ROLE, userDetailsInfo.getUserRole())
				.claim(JwtUtils.CLAIM_NAME, userDetailsInfo.getName())
				.claim(JwtUtils.CLAIM_NICKNAME, userDetailsInfo.getNickname())
				.claim(JwtUtils.CLAIM_ENABLED, userDetailsInfo.isEnabled())
				.issuedAt(now)
				.expiration(new Date(now.getTime() + expiration))
				.signWith(secretKey, Jwts.SIG.HS256)
				.compact();

		// 생성한 accessToken에 'Bearer '를 붙여서 반환
		return JwtUtils.BEARER_PREFIX + accessToken;
    }

    /**
     * 실제 refreshToken을 생성하는 메서드입니다.
     * 사용자 ID를 Subject에 넣고, 권한(role)을 Custom Claim으로 추가하여 서명합니다.
     */
    private String createRefreshToken(UserDetails userDetails, Long expiration) {
        Date now = new Date();
		UserDetailsImpl userDetailsInfo = (UserDetailsImpl) userDetails;

		return Jwts.builder()
				.subject(userDetailsInfo.getUuid().toString())
				.claim(JwtUtils.CLAIM_TOKEN_TYPE, TokenType.REFRESH.getValue())
				.claim(JwtUtils.CLAIM_USER_ROLE, userDetailsInfo.getUserRole())
				.issuedAt(now)
				.expiration(new Date(now.getTime() + expiration))
				.signWith(secretKey, Jwts.SIG.HS256)
				.compact();
    }

    /**
     * 토큰을 복호화(파싱)하여 내부의 데이터 묶음인 Claims를 추출합니다.
     * 서명 검증을 포함하며, 라이브러리 버전에 따라 parseSignedClaims와 getPayload를 사용합니다.
     */
	@Override
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(JwtUtils.normalizeToken(token))
                .getPayload();
    }

    @Override
    public long getRemainingTime(String token) {
        Date expiration = parseClaims(token).getExpiration();
        long now = new Date().getTime();
        return Math.max(0, expiration.getTime() - now);
    }
}
