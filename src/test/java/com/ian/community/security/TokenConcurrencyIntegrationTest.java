package com.ian.community.security;

import com.ian.community.common.exception.CustomException;
import com.ian.community.common.exception.ErrorCode;
import com.ian.community.security.refresh.RefreshTokenRepository;
import com.ian.community.security.token.TokenPair;
import com.ian.community.security.token.TokenService;
import com.ian.community.user.domain.User;
import com.ian.community.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class TokenConcurrencyIntegrationTest {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("동시 Refresh 요청은 하나만 Rotation에 성공한다")
    void allowOnlyOneConcurrentRotation() throws Exception {
        User user = userRepository.saveAndFlush(
                new User(
                        "concurrent-token@example.com",
                        "encoded-password",
                        "동시사용자"
                )
        );
        TokenPair initial = tokenService.issueInitialTokens(user);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        java.util.concurrent.Callable<ErrorCode> rotate = () -> {
            ready.countDown();
            start.await();
            try {
                tokenService.rotate(initial.refreshToken());
                return null;
            } catch (CustomException exception) {
                return exception.getErrorCode();
            }
        };

        Future<ErrorCode> first = executor.submit(rotate);
        Future<ErrorCode> second = executor.submit(rotate);
        ready.await();
        start.countDown();

        assertThat(Arrays.asList(first.get(), second.get()))
                .containsExactlyInAnyOrder(
                        null,
                        ErrorCode.REFRESH_TOKEN_REUSED
                );
    }
}
