package com.ian.community.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;

@Getter
public class SignupResponse {
    @JsonProperty("user_id")
    private Long userId;

    private Instant accessTokenExpiresAt;

    public SignupResponse(
            Long userId,
            Instant accessTokenExpiresAt
    ) {
        this.userId = userId;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }
}
