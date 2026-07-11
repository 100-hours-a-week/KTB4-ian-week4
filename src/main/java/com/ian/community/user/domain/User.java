package com.ian.community.user.domain;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 256)
    private String email;

    @Column(length = 100)
    private String password;

    @Column(length = 10, nullable = false)
    private String nickname;

    @Column(name = "profile_image", length = 100, nullable = false)
    private String profileImage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "password_updated_at")
    private LocalDateTime passwordUpdatedAt;

    @Column(name = "nickname_updated_at")
    private LocalDateTime nicknameUpdatedAt;

    @Column(name = "profile_updated_at")
    private LocalDateTime profileUpdatedAt;

    @Column(name = "user_deleted", nullable = false)
    private boolean userDeleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public User(String email, String password, String nickname, String profile) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.profileImage = profile;
        this.createdAt = LocalDateTime.now();
        this.passwordUpdatedAt = null;
        this.nicknameUpdatedAt = null;
        this.profileUpdatedAt = LocalDateTime.now();
        this.userDeleted = false;
        this.deletedAt = null;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
        this.nicknameUpdatedAt = LocalDateTime.now();
    }

    public void updatePassword(String password) {
        this.password = password;
        this.passwordUpdatedAt = LocalDateTime.now();
    }

    public void updateProfile(String profile) {
        this.profileImage = profile;
        this.profileUpdatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.email =  null;
        this.password = null;
        this.nickname = "알 수 없음";
        this.profileImage = "https://image.kr/default-profile.jpg";
        this.userDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
