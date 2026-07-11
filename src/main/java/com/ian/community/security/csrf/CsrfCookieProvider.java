package com.ian.community.security.csrf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CsrfCookieProvider {

    public static final String TOKEN_COOKIE = "XSRF-TOKEN";
    public static final String PREAUTH_BINDING_COOKIE = "CSRF-PREAUTH";
    public static final String TOKEN_HEADER = "X-XSRF-TOKEN";

    private final boolean cookieSecure;
    private final String sameSite;

    public CsrfCookieProvider(
            @Value("${app.cookie.secure:false}") boolean cookieSecure,
            @Value("${app.cookie.same-site:Lax}") String sameSite
    ) {
        this.cookieSecure = cookieSecure;
        this.sameSite = sameSite;
    }

    public ResponseCookie createTokenCookie(String token) {
        return ResponseCookie.from(TOKEN_COOKIE, token)
                .httpOnly(false)
                .secure(cookieSecure)
                .sameSite(sameSite)
                .path("/")
                .build();
    }

    public ResponseCookie createPreAuthBindingCookie(String binding) {
        return ResponseCookie.from(PREAUTH_BINDING_COOKIE, binding)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(sameSite)
                .path("/")
                .build();
    }

    public ResponseCookie deleteTokenCookie() {
        return ResponseCookie.from(TOKEN_COOKIE, "")
                .httpOnly(false)
                .secure(cookieSecure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(0)
                .build();
    }

    public ResponseCookie deletePreAuthBindingCookie() {
        return ResponseCookie.from(PREAUTH_BINDING_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(0)
                .build();
    }
}
