package com.ian.community.user.dto.response;

import com.ian.community.user.domain.User;

public record CurrentUserResponse(
        String email,
        String nickname,
        ProfileImageResponse profileImage
) {
    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(
                user.getEmail(),
                user.getNickname(),
                ProfileImageResponse.from(user.getProfileImageAsset())
        );
    }
}
