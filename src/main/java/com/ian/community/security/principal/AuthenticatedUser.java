package com.ian.community.security.principal;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class AuthenticatedUser {

    private final Long userId;
    private final String email;
    private final List<GrantedAuthority> authorities;

    public AuthenticatedUser(
            Long userId,
            String email,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.userId = Objects.requireNonNull(
                userId,
                "인증 사용자 ID는 null일 수 없습니다."
        );

        this.email = Objects.requireNonNull(
                email,
                "인증 사용자 이메일은 null일 수 없습니다."
        );

        this.authorities = List.copyOf(
                Objects.requireNonNull(
                        authorities,
                        "권한 목록은 null일 수 없습니다."
                )
        );
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public List<GrantedAuthority> getAuthorities() {
        return authorities;
    }
}
