package com.ian.community.user.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UserPasswordUpdateRequest {
    @NotBlank(message = "현재 비밀번호를 입력해주세요.")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하입니다.")
    private String password;

    @NotBlank(message = "새로운 비밀번호를 입력해주세요.")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하입니다.")
    private String newPassword;

    @JsonProperty("password_confirm")
    @NotBlank(message = "비밀번호를 한번 더 입력해주세요.")
    @Size(min = 8, max = 20)
    private String newPasswordConfirm;
}
