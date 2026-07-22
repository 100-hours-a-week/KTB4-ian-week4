package com.ian.community.user.controller;

import com.ian.community.common.exception.CustomException;
import com.ian.community.common.exception.ErrorCode;
import com.ian.community.security.jwt.JwtCookieProvider;
import com.ian.community.security.principal.AuthenticatedUser;
import com.ian.community.security.token.TokenPair;
import com.ian.community.security.token.TokenService;
import com.ian.community.user.domain.User;
import com.ian.community.user.dto.request.LoginRequest;
import com.ian.community.user.dto.request.SignupRequest;
import com.ian.community.user.dto.request.UserNicknameUpdateRequest;
import com.ian.community.user.dto.request.UserPasswordUpdateRequest;
import com.ian.community.user.dto.response.CurrentUserResponse;
import com.ian.community.user.dto.response.ProfileImageResponse;
import com.ian.community.user.service.ProfileImageService;
import com.ian.community.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ProfileImageService profileImageService;
    private final TokenService tokenService;
    private final JwtCookieProvider jwtCookieProvider;

    @PostMapping(
            value = "/signup",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CurrentUserResponse> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        User user = userService.signup(request);

        TokenPair tokenPair =
                tokenService.issueInitialTokens(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
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
                .body(
                        userService.getCurrentUser(
                                user.getUserId()
                        )
                );
    }

    @PostMapping("/login")
    public ResponseEntity<CurrentUserResponse> login(
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
                .body(
                        userService
                                .getCurrentUser(
                                        user.getUserId()
                                )
                );
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
        tokenService.logout(resolveRefreshTokenOrNull(request));

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

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> getCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ResponseEntity.ok(
                userService.getCurrentUser(principal.getUserId())
        );
    }

    @PatchMapping("/me/nickname")
    public ResponseEntity<Void> updateNickname(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody
            UserNicknameUpdateRequest request
    ) {
        userService.updateNickname(
                principal.getUserId(),
                request
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody
            UserPasswordUpdateRequest request
    ) {
        userService.updatePassword(
                principal.getUserId(),
                request
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    @PatchMapping(
            value = "/me/profile-image",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ProfileImageResponse> updateProfileImage(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestPart("image") MultipartFile image
    ) {
        return ResponseEntity.ok(
                profileImageService.update(
                        principal.getUserId(),
                        image
                )
        );
    }

    @DeleteMapping("/me/profile-image")
    public ResponseEntity<ProfileImageResponse> resetProfileImage(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ResponseEntity.ok(
                profileImageService.reset(principal.getUserId())
        );
    }

    @DeleteMapping("/me/delete")
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        userService.deleteUser(principal.getUserId());
        tokenService.logoutAll(principal.getUserId());

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE,
                        jwtCookieProvider.deleteAccessCookie().toString())
                .header(HttpHeaders.SET_COOKIE,
                        jwtCookieProvider.deleteRefreshCookie().toString())
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