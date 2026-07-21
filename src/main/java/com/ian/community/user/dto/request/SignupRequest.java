package com.ian.community.user.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class SignupRequest {
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하입니다.")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!#$%&'()*+,./:;<=>?@^_`{|}~])(?=\\S+$)[A-Za-z0-9!#$%&'()*+,./:;<=>?@^_`{|}~]+$",
            message = "대문자,소문자,숫자,특수문자를 각각 최소 1개 이상 포함"
    )
    private String password;

    @JsonProperty("password_confirm")
    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하입니다.")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!#$%&'()*+,./:;<=>?@^_`{|}~])(?=\\S+$)[A-Za-z0-9!#$%&'()*+,./:;<=>?@^_`{|}~]+$",
            message = "대문자,소문자,숫자,특수문자를 각각 최소 1개 이상 포함"
    )
    private String passwordConfirm;

    @NotBlank(message = "닉네임을 입력해주세요.")
    @Size(min = 1, max = 10, message = "닉네임은 최대 10자 까지 작성 가능합니다.")
    private String nickname;

    @JsonProperty("profile_image")
    @NotBlank(message = "프로필 사진을 추가해주세요.")
    private String profileImage;
}