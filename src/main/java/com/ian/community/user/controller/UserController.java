package com.ian.community.user.controller;

import com.ian.community.common.exception.CustomException;
import com.ian.community.common.exception.ErrorCode;
import com.ian.community.security.jwt.JwtCookieProvider;
import com.ian.community.security.token.TokenPair;
import com.ian.community.security.token.TokenService;
import com.ian.community.user.domain.User;
import com.ian.community.user.dto.request.*;
import com.ian.community.user.dto.response.UserResponse;
import com.ian.community.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TokenService tokenService;
    private final JwtCookieProvider jwtCookieProvider;

    @PostMapping(
            value = "/signup",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Void> signup(
            @RequestPart("request") SignupRequest request,
            @RequestPart(value = "image", required = false)
            MultipartFile image
    ) {
        userService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @Valid @RequestBody LoginRequest request
    ) {
        User user = userService.login(request);

        TokenPair tokenPair =
                tokenService.issueInitialTokens(user);

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        jwtCookieProvider
                                .createAccessCookie(
                                        tokenPair.accessToken()
                                )
                                .toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        jwtCookieProvider
                                .createRefreshCookie(
                                        tokenPair.refreshToken()
                                )
                                .toString()
                )
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            HttpServletRequest request
    ) {
        String refreshToken =
                resolveRefreshToken(request);

        TokenPair tokenPair =
                tokenService.rotate(refreshToken);

        return ResponseEntity
                .noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        jwtCookieProvider
                                .createAccessCookie(
                                        tokenPair.accessToken()
                                )
                                .toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        jwtCookieProvider
                                .createRefreshCookie(
                                        tokenPair.refreshToken()
                                )
                                .toString()
                )
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request
    ) {
        String refreshToken =
                resolveRefreshTokenOrNull(request);

        tokenService.logout(refreshToken);

        return ResponseEntity
                .noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        jwtCookieProvider
                                .deleteAccessCookie()
                                .toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        jwtCookieProvider
                                .deleteRefreshCookie()
                                .toString()
                )
                .build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(
                userService.getUser(userId)
        );
    }

    @PatchMapping("/{userId}/nickname")
    public ResponseEntity<Void> updateNickname(
            @PathVariable Long userId,
            @Valid @RequestBody
            UserNicknameUpdateRequest request
    ) {
        userService.updateNickname(userId, request);

        return ResponseEntity
                .noContent()
                .build();
    }

    @PatchMapping("/{userId}/password")
    public ResponseEntity<Void> updatePassword(
            @PathVariable Long userId,
            @Valid @RequestBody
            UserPasswordUpdateRequest request
    ) {
        userService.updatePassword(userId, request);

        return ResponseEntity
                .noContent()
                .build();
    }

    @PatchMapping("/{userId}/profile-image")
    public ResponseEntity<Void> updateProfile(
            @PathVariable Long userId,
            @Valid @RequestBody
            UserProfileImageUpdateRequest request
    ) {
        userService.updateProfile(userId, request);

        return ResponseEntity
                .noContent()
                .build();
    }

    @DeleteMapping("/{userId}/delete")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId
    ) {
        userService.deleteUser(userId);

        return ResponseEntity
                .noContent()
                .build();
    }

    private String resolveRefreshToken(
            HttpServletRequest request
    ) {
        String refreshToken =
                resolveRefreshTokenOrNull(request);

        if (refreshToken == null) {
            throw new CustomException(
                    ErrorCode.REFRESH_TOKEN_NOT_FOUND
            );
        }

        return refreshToken;
    }

    private String resolveRefreshTokenOrNull(
            HttpServletRequest request
    ) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie ->
                        JwtCookieProvider
                                .REFRESH_TOKEN_COOKIE
                                .equals(cookie.getName())
                )
                .map(Cookie::getValue)
                .filter(value ->
                        value != null
                                && !value.isBlank()
                )
                .findFirst()
                .orElse(null);
    }
}