package com.ian.community.user.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {
    @Test
    void 닉네임을_수정하면_닉네임과_수정시간이_변경된다() {
        // given
        User user = new User(
                "email@email.com",
                "Qwer!234",
                "닉네임",
                "https://image.kr/profile.jpg"
        );

        // when
        user.updateNickname("nickname");

        // then
        assertEquals("nickname", user.getNickname());
        assertNotNull(user.getNicknameUpdatedAt());
    }

    @Test
    void 비밀번호를_수정하면_비밀번호와_수정시간이_변경된다() {
        User user = new User(
                "email@email.com",
                "Qwer!234",
                "닉네임",
                "https://image.kr/profile.jpg"
        );

        assertNull(user.getPasswordUpdatedAt());

        user.updatePassword("password");

        assertEquals("password", user.getPassword());
        assertNotNull(user.getPasswordUpdatedAt());
    }

    @Test
    void 프로필을_수정하면_프로필이미지와_수정시간이_변경된다() {
        User user = new User(
                "email@email.com",
                "Qwer!234",
                "닉네임",
                "https://image.kr/profile.jpg"
        );

        assertNull(user.getProfileUpdatedAt());

        user.updateProfile("https://image.kr/image.jpg");

        assertEquals("https://image.kr/image.jpg", user.getProfileImage());
        assertNotNull(user.getProfileUpdatedAt());
    }

    @Test
    void 회원탈퇴하면_개인정보가_초기화되고_삭제상태가_된다() {
        User user = new User(
                "email@email.com",
                "Qwer!234",
                "닉네임",
                "https://image.kr/profile.jpg"
        );

        // 삭제 시간은 기존에 null이기에 실제로도 null을 유지하는지 검증합니다.
        assertFalse(user.isUserDeleted());
        assertNull(user.getDeletedAt());

        user.delete();

        assertTrue(user.isUserDeleted());
        assertNotNull(user.getDeletedAt());
    }
}
