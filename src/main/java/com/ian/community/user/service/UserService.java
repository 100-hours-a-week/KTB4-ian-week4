package com.ian.community.user.service;

import com.ian.community.common.exception.CustomException;
import com.ian.community.common.exception.ErrorCode;
import com.ian.community.user.domain.User;
import com.ian.community.user.dto.request.*;
import com.ian.community.user.dto.response.UserResponse;
import com.ian.community.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.isUserDeleted()) {
            throw new CustomException(ErrorCode.USER_ALREADY_DELETED);
        }

        return user;
    }

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (userRepository.existsByNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        if (!Objects.equals(request.getPassword(), request.getPasswordConfirm())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        User user = new User(
                request.getEmail(),
                request.getPassword(),
                request.getNickname(),
                request.getProfileImage()
        );

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        User user = getActiveUser(userId);

        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImage()
        );
    }

    @Transactional(readOnly = true)
    public void login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_LOGIN_REQUEST));

        if (user.isUserDeleted()) {
            throw new CustomException(ErrorCode.INVALID_LOGIN_REQUEST);
        }

        if (!user.getPassword().equals(request.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_LOGIN_REQUEST);
        }
    }

    @Transactional
    public void updateNickname(Long userId, UserNicknameUpdateRequest request) {
        User user = getActiveUser(userId);

        if (user.getNickname().equals(request.getNickname())) {
            throw new CustomException(ErrorCode.NO_CHANGES_DETECTED);
        }

        if (userRepository.existsByNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        user.updateNickname(request.getNickname());
    }

    @Transactional
    public void updateProfile(Long userId, UserProfileImageUpdateRequest request) {
        User user = getActiveUser(userId);

        if (user.getProfileImage().equals(request.getProfileImage())) {
            throw new CustomException(ErrorCode.NO_CHANGES_DETECTED);
        }

        user.updateProfile(request.getProfileImage());
    }

    @Transactional
    public void updatePassword(Long userId, UserPasswordUpdateRequest request) {
        User user = getActiveUser(userId);

        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        user.updatePassword(request.getPassword());
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = getActiveUser(userId);

        user.delete();
    }
}
