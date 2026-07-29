package com.ian.community.user;

import com.ian.community.security.jwt.JwtCookieProvider;
import com.ian.community.security.jwt.JwtTokenProvider;
import com.ian.community.user.domain.User;
import com.ian.community.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Base64;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserApiIntegrationTest {

    private static final String SECRET =
            "dGVtcG9yYXJ5LWxvY2FsLWgyLXZlcmlmaWNhdGlv"
                    + "bi1zZWNyZXQtMjAyNi1sb25n";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("인증 사용자는 자신의 개인정보를 조회한다")
    void getCurrentUser() throws Exception {
        User user = saveUser("mine@example.com", "내닉네임");

        mockMvc.perform(
                        get("/api/users/me")
                                .cookie(accessCookie(user))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.email").value("mine@example.com"));
    }

    @Test
    @DisplayName("다른 사용자 ID로 개인정보를 조회하면 403을 반환한다")
    void rejectOtherUserLookup() throws Exception {
        User mine = saveUser("mine2@example.com", "내닉네임2");
        User other = saveUser("other@example.com", "다른닉네임");

        mockMvc.perform(
                        get("/api/users/{userId}", other.getUserId())
                                .cookie(accessCookie(mine))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("미인증 사용자 조회는 401 코드와 메시지를 반환한다")
    void rejectUnauthenticatedLookup() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    @DisplayName("존재하지 않는 인증 사용자는 404를 반환한다")
    void rejectMissingUser() throws Exception {
        String token = jwtTokenProvider.createAccessToken(
                999_999L,
                "missing@example.com",
                List.of("USER")
        );

        mockMvc.perform(
                        get("/api/users/me")
                                .cookie(new Cookie(
                                        JwtCookieProvider.ACCESS_TOKEN_COOKIE,
                                        token
                                ))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("만료 Access Token은 명시적인 만료 코드를 반환한다")
    void rejectExpiredAccessToken() throws Exception {
        String token = createExpiredAccessToken();

        mockMvc.perform(
                        get("/api/users/me")
                                .cookie(new Cookie(
                                        JwtCookieProvider.ACCESS_TOKEN_COOKIE,
                                        token
                                ))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("EXPIRED_ACCESS_TOKEN"));
    }

    @Test
    @DisplayName("회원가입 Bean Validation은 DTO 이메일 메시지를 보존한다")
    void preserveSignupEmailMessage() throws Exception {
        signup("""
                {
                  "email": "invalid",
                  "password": "Pulse123!",
                  "password_confirm": "Pulse123!",
                  "nickname": "사용자"
                }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(
                        jsonPath("$.message")
                                .value("올바른 이메일 형식이 아닙니다.")
                );
    }

    @Test
    @DisplayName("회원가입 Bean Validation은 빈 비밀번호 메시지를 보존한다")
    void preserveSignupBlankPasswordMessage() throws Exception {
        signup("""
                {
                  "email": "valid@example.com",
                  "password": "",
                  "password_confirm": "Pulse123!",
                  "nickname": "사용자"
                }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("비밀번호를 입력해주세요."));
    }

    @Test
    @DisplayName("회원가입 Bean Validation은 비밀번호 Pattern 메시지를 보존한다")
    void preserveSignupPasswordPatternMessage() throws Exception {
        signup("""
                {
                  "email": "valid2@example.com",
                  "password": "password",
                  "password_confirm": "password",
                  "nickname": "사용자"
                }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(
                        jsonPath("$.message")
                                .value("대문자,소문자,숫자,특수문자를 각각 최소 1개 이상 포함")
                );
    }

    @Test
    @DisplayName("정상 회원가입은 인증 쿠키와 만료 시각을 반환한다")
    void signupSuccess() throws Exception {
        signup("""
                {
                  "email": "signup@example.com",
                  "password": "Pulse123!",
                  "password_confirm": "Pulse123!",
                  "nickname": "가입사용자"
                }
                """)
                .andExpect(status().isOk())
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(jsonPath("$.user_id").isNumber())
                .andExpect(jsonPath("$.accessTokenExpiresAt").isString());
    }

    private org.springframework.test.web.servlet.ResultActions signup(
            String content
    ) throws Exception {
        return mockMvc.perform(
                post("/api/users/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content)
        );
    }

    private User saveUser(String email, String nickname) {
        return userRepository.saveAndFlush(
                new User(email, "encoded-password", nickname)
        );
    }

    private Cookie accessCookie(User user) {
        return new Cookie(
                JwtCookieProvider.ACCESS_TOKEN_COOKIE,
                jwtTokenProvider.createAccessToken(
                        user.getUserId(),
                        user.getEmail(),
                        List.of("USER")
                )
        );
    }

    private String createExpiredAccessToken() {
        Instant now = Instant.now();
        SecretKeySpec secretKey = new SecretKeySpec(
                Base64.getDecoder().decode(SECRET),
                "HmacSHA256"
        );
        NimbusJwtEncoder encoder = NimbusJwtEncoder
                .withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("ian-community")
                .subject("1")
                .issuedAt(now.minusSeconds(120))
                .expiresAt(now.minusSeconds(90))
                .id("expired-access-token")
                .claim("token_type", "access")
                .claim("email", "expired@example.com")
                .claim("roles", List.of("USER"))
                .build();

        return encoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256)
                                .type("JWT")
                                .build(),
                        claims
                )
        ).getTokenValue();
    }
}
