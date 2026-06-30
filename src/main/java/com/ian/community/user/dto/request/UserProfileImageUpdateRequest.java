package com.ian.community.user.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserProfileImageUpdateRequest {
    @JsonProperty("profile_image")
    @NotBlank(message = "프로필 사진을 추가해주세요.")
    private String profileImage;
}
