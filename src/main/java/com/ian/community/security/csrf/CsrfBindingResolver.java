package com.ian.community.security.csrf;

import com.ian.community.security.jwt.JwtCookieProvider;
import com.ian.community.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class CsrfBindingResolver {

    private final JwtTokenProvider jwtTokenProvider;

    public CsrfBindingResolver(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public Optional<String> resolve(HttpServletRequest request) {
        String accessToken = cookieValue(request, JwtCookieProvider.ACCESS_TOKEN_COOKIE);
        if (accessToken != null) {
            try {
                Jwt jwt = jwtTokenProvider.decodeAccessToken(accessToken);
                return Optional.of("access:" + jwtTokenProvider.getTokenId(jwt));
            } catch (JwtException | IllegalArgumentException ignored) {
            }
        }

        String refreshToken = cookieValue(request, JwtCookieProvider.REFRESH_TOKEN_COOKIE);
        if (refreshToken != null) {
            try {
                Jwt jwt = jwtTokenProvider.decodeRefreshToken(refreshToken);
                return Optional.of("refresh:" + jwtTokenProvider.getFamilyId(jwt));
            } catch (JwtException | IllegalArgumentException ignored) {
            }
        }

        return Optional.ofNullable(
                cookieValue(request, CsrfCookieProvider.PREAUTH_BINDING_COOKIE)
        ).map(value -> "preauth:" + value);
    }

    public String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }
}
