package com.ian.community.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserUpdateResponse {
    private String nickname;
    private String profileImage;
}
