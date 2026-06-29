package com.ian.community.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class SignupResponse {
    @JsonProperty("user_id")
    private Long userId;

    public SignupResponse(Long userId) {
        this.userId = userId;
    }
}
