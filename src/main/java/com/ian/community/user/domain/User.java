package com.ian.community.user.domain;

import lombok.Getter;

@Getter
public class User {
    private Long userId;

    private String email;
    private String password;
    private String nickname;
    private String profile;

    private boolean userDeleted;

    public User(Long userId, String email, String password, String nickname, String profile) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.profile = profile;
        this.userDeleted = false;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateProfile(String profile) {
        this.profile = profile;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void delete() {
        this.email =  null;
        this.password = null;
        this.nickname = "알 수 없음";
        this.profile = "https://image.kr/default-profile.jpg";
        this.userDeleted = true;
    }
}
