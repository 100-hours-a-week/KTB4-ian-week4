package com.ian.community.security.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class JwtCookieProvider {

    public static final String ACCESS_TOKEN_COOKIE =
            "accessToken";

    public static final String REFRESH_TOKEN_COOKIE =
            "refreshToken";

    private final JwtTokenProvider jwtTokenProvider;
    private final boolean cookieSecure;
    private final String sameSite;

    public JwtCookieProvider(
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.cookie.secure:false}")
            boolean cookieSecure,
            @Value("${app.cookie.same-site:Lax}")
            String sameSite
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieSecure = cookieSecure;
        this.sameSite = sameSite;
    }

    public ResponseCookie createAccessCookie(
            String accessToken
    ) {
        return ResponseCookie
                .from(ACCESS_TOKEN_COOKIE, accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(
                        jwtTokenProvider
                                .getAccessExpirationSeconds()
                )
                .build();
    }

    public ResponseCookie createRefreshCookie(
            String refreshToken
    ) {
        return ResponseCookie
                .from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(sameSite)
                .path("/api/users")
                .maxAge(
                        jwtTokenProvider
                                .getRefreshExpirationSeconds()
                )
                .build();
    }

    public ResponseCookie deleteAccessCookie() {
        return ResponseCookie
                .from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(0)
                .build();
    }

    public ResponseCookie deleteRefreshCookie() {
        return ResponseCookie
                .from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(sameSite)
                .path("/api/users")
                .maxAge(0)
                .build();
    }
}