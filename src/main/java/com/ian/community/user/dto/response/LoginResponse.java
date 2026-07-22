package com.ian.community.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class LoginResponse {
    @JsonProperty("user_id")
    private Long userId;

    public LoginResponse(Long userId) {
        this.userId = userId;
    }
}