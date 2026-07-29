package com.ian.community.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class TokenRefreshResponse {
    private Instant accessTokenExpiresAt;
}
