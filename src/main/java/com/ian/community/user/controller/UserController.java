package com.ian.community.user.controller;

import com.ian.community.user.dto.request.*;
import com.ian.community.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest request) {
        userService.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
        userService.login(request);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{userId}/nickname")
    public ResponseEntity<Void> updateNickname(
            @PathVariable Long userId,
            @Valid @RequestBody UserNicknameUpdateRequest request
    ) {
        userService.updateNickname(userId, request);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/password")
    public ResponseEntity<Void> updatePassword(
            @PathVariable Long userId,
            @Valid @RequestBody UserPasswordUpdateRequest request
    ) {
        userService.updatePassword(userId, request);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/profile-image")
    public ResponseEntity<Void> updateProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UserProfileImageUpdateRequest request
    ) {
        userService.updateProfile(userId, request);

        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/{userId}/delete")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId
    ) {
        userService.deleteUser(userId);

        return ResponseEntity.noContent().build();
    }
}

