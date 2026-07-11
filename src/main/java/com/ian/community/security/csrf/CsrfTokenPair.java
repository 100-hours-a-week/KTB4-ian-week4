package com.ian.community.security.csrf;

public record CsrfTokenPair(
        String binding,
        String token
) {
}
