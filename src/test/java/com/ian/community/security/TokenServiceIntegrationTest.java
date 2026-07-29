package com.ian.community.security;

import com.ian.community.common.exception.CustomException;
import com.ian.community.common.exception.ErrorCode;
import com.ian.community.security.refresh.RefreshToken;
import com.ian.community.security.refresh.RefreshTokenRepository;
import com.ian.community.security.jwt.JwtTokenProvider;
import com.ian.community.security.token.TokenPair;
import com.ian.community.security.token.TokenService;
import com.ian.community.user.domain.User;
import com.ian.community.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class TokenServiceIntegrationTest {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.saveAndFlush(
                new User(
                        "token-user@example.com",
                        "encoded-password",
                        "토큰사용자"
                )
        );
    }

    @Test
    @DisplayName("Refresh 성공은 Access와 Refresh Token을 모두 교체한다")
    void rotateRefreshToken() {
        TokenPair initial = tokenService.issueInitialTokens(user);
        TokenPair rotated = tokenService.rotate(initial.refreshToken());

        assertThat(rotated.accessToken())
                .isNotEqualTo(initial.accessToken());
        assertThat(rotated.refreshToken())
                .isNotEqualTo(initial.refreshToken());
        assertThat(rotated.accessTokenExpiresAt()).isNotNull();
        assertThat(refreshTokenRepository.findAll())
                .hasSize(2)
                .extracting(RefreshToken::isRevoked)
                .containsExactlyInAnyOrder(true, false);

        assertThatThrownBy(() -> tokenService.validateAccessToken(
                jwtTokenProvider.decodeAccessToken(initial.accessToken())
        )).isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);

        tokenService.validateAccessToken(
                jwtTokenProvider.decodeAccessToken(rotated.accessToken())
        );
    }

    @Test
    @DisplayName("서로 다른 로그인 기기의 Token family는 독립적으로 유지된다")
    void keepOtherDeviceFamilyActive() {
        TokenPair firstDevice = tokenService.issueInitialTokens(user);
        TokenPair secondDevice = tokenService.issueInitialTokens(user);

        tokenService.rotate(firstDevice.refreshToken());

        tokenService.validateAccessToken(
                jwtTokenProvider.decodeAccessToken(secondDevice.accessToken())
        );
        TokenPair secondRotated = tokenService.rotate(secondDevice.refreshToken());
        tokenService.validateAccessToken(
                jwtTokenProvider.decodeAccessToken(secondRotated.accessToken())
        );
    }

    @Test
    @DisplayName("사용한 Refresh Token 재사용은 Family를 폐기한다")
    void rejectReusedRefreshToken() {
        TokenPair initial = tokenService.issueInitialTokens(user);
        tokenService.rotate(initial.refreshToken());

        assertThatThrownBy(() ->
                tokenService.rotate(initial.refreshToken())
        )
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REFRESH_TOKEN_REUSED);

        assertThat(refreshTokenRepository.findAll())
                .allMatch(RefreshToken::isRevoked);
    }

    @Test
    @DisplayName("위조 Refresh Token과 Refresh Token 부재를 구분한다")
    void rejectInvalidAndMissingRefreshToken() {
        assertErrorCode(
                () -> tokenService.rotate("forged"),
                ErrorCode.INVALID_REFRESH_TOKEN
        );
        assertErrorCode(
                () -> tokenService.rotate(null),
                ErrorCode.REFRESH_TOKEN_NOT_FOUND
        );
    }

    @Test
    @DisplayName("Logout 뒤 Refresh를 거부한다")
    void rejectRefreshAfterLogout() {
        TokenPair initial = tokenService.issueInitialTokens(user);
        tokenService.logout(initial.refreshToken());

        assertErrorCode(
                () -> tokenService.rotate(initial.refreshToken()),
                ErrorCode.REFRESH_TOKEN_REUSED
        );
    }

    @Test
    @DisplayName("탈퇴 사용자의 Refresh를 거부한다")
    void rejectDeletedUserRefresh() {
        TokenPair initial = tokenService.issueInitialTokens(user);
        user.delete();
        userRepository.flush();

        assertErrorCode(
                () -> tokenService.rotate(initial.refreshToken()),
                ErrorCode.USER_ALREADY_DELETED
        );
    }

    private void assertErrorCode(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
            ErrorCode errorCode
    ) {
        assertThatThrownBy(callable)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
