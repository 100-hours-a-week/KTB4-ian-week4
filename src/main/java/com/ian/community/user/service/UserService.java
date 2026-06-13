package com.ian.community.user.service;

import com.ian.community.common.exception.CustomException;
import com.ian.community.common.exception.ErrorCode;
import com.ian.community.user.domain.User;
import com.ian.community.user.dto.LoginRequest;
import com.ian.community.user.dto.LoginResponse;
import com.ian.community.user.dto.SignupRequest;
import com.ian.community.user.dto.SignupResponse;
import com.ian.community.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public SignupResponse signup(SignupRequest request) {
        validateSignup(request);

        User user = userRepository.save(
                request.getEmail(),
                request.getPassword(),
                request.getNickname(),
                request.getProfile()
        );

        return new SignupResponse(user.getUserId());
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_LOGIN_REQUEST));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_LOGIN_REQUEST);
        }

        return new LoginResponse(user.getUserId());
    }

    private void validateSignup(SignupRequest request) {
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new CustomException(ErrorCode.INVALID_SIGNUP_REQUEST);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.INVALID_SIGNUP_REQUEST);
        }

        if (userRepository.existsByNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.INVALID_SIGNUP_REQUEST);
        }
    }
}
