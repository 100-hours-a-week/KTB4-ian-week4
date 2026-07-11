package com.ian.community.security.csrf;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class CsrfBindingResolver {

    public Optional<String> resolve(HttpServletRequest request) {
        return Optional.ofNullable(
                cookieValue(request, CsrfCookieProvider.PREAUTH_BINDING_COOKIE)
        ).map(value -> "session:" + value);
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
