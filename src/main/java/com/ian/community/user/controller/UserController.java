package com.ian.community.user.controller;

import com.ian.community.common.ApiResponse;
import com.ian.community.user.dto.LoginRequest;
import com.ian.community.user.dto.LoginResponse;
import com.ian.community.user.dto.SignupRequest;
import com.ian.community.user.dto.SignupResponse;
import com.ian.community.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        SignupResponse response = userService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>("signup_success", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = userService.login(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>("login_success", response));
    }
}
