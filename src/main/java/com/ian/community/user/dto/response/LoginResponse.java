package com.ian.community.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;

@Getter
public class LoginResponse {
    @JsonProperty("user_id")
    private Long userId;

    private Instant accessTokenExpiresAt;

    public LoginResponse(
            Long userId,
            Instant accessTokenExpiresAt
    ) {
        this.userId = userId;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }
}
