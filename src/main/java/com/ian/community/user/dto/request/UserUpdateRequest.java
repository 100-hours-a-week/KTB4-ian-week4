package com.ian.community.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserUpdateRequest {
    @NotBlank(message = "닉네임을 입력해주세요.")
    @Size(min = 1, max = 10, message = "닉네임은 최대 10자 까지 작성 가능합니다.")
    private String nickname;

    @NotBlank(message = "프로필 사진을 추가해주세요.")
    private String profileImage;
}
