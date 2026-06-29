package com.ian.community.user.controller;

import com.ian.community.user.dto.request.LoginRequest;
import com.ian.community.user.dto.request.SignupRequest;
import com.ian.community.user.dto.request.UserPasswordUpdateRequest;
import com.ian.community.user.dto.request.UserUpdateRequest;
import com.ian.community.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
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

    @PatchMapping("/{user_id}/nickname")
    public ResponseEntity<Void> updateNickname(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        userService.updateNickname(userId, request);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{user_id}/password")
    public ResponseEntity<Void> updatePassword(
            @PathVariable Long userId,
            @Valid @RequestBody UserPasswordUpdateRequest request
    ) {
        userService.updatePassword(userId, request);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{user_id}/profile-image")
    public ResponseEntity<Void> updateProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        userService.updateProfile(userId, request);

        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/{user_id}/delete")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId
    ) {
        userService.deleteUser(userId);

        return ResponseEntity.noContent().build();
    }
}

