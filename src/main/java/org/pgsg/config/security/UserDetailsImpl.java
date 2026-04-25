package org.pgsg.config.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(exclude = "password")
public class UserDetailsImpl implements UserDetails {

    private final UUID uuid;          // 회원 고유 ID
    private final String username;    // 로그인용 아이디
    private final String password;    // 비밀번호
    private final String userRole;    // 권한 (예: ROLE_USER, ROLE_MASTER 등)
    private final String name;        // 회원의 실명
    private final String nickname;    // 회원의 닉네임(예약, 거래, 채팅 도메인에서 필요할 시)
    private final boolean enabled;

    @Builder
    public UserDetailsImpl(UUID uuid, String username, String password, String userRole,
                           String name, String nickname, boolean enabled) {
        this.uuid = uuid;
        this.username = username;
        this.password = password;
        this.userRole = userRole;
        this.nickname = nickname;
        this.name = name;
        this.enabled = enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // userRole 필드가 비어있을 경우 기본 권한 부여
        if (!StringUtils.hasText(userRole)) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // 권한이 콤마(,)로 구분된 여러 개일 경우를 대비한 로직 (단일 권한이어도 작동함)
        return Arrays.stream(userRole.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }
}