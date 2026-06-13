package com.ian.community.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class SignupResponse {
    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("user_deleted")
    private boolean userDeleted;

    public SignupResponse(Long userId) {
        this.userId = userId;
        this.userDeleted = userDeleted;
    }
}
