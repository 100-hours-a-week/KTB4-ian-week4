package com.ian.community.security.token;

public record TokenPair(
        String accessToken,
        String refreshToken
) {
}