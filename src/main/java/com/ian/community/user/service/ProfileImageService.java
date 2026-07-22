package com.ian.community.user.service;

import com.ian.community.common.exception.CustomException;
import com.ian.community.common.exception.ErrorCode;
import com.ian.community.image.domain.ImageAsset;
import com.ian.community.image.service.ImageLifecycleService;
import com.ian.community.image.service.ProfileImageStorageService;
import com.ian.community.user.domain.User;
import com.ian.community.user.dto.response.ProfileImageResponse;
import com.ian.community.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProfileImageService {
    private final UserRepository userRepository;
    private final ProfileImageStorageService storageService;
    private final ImageLifecycleService lifecycleService;

    @Transactional
    public ProfileImageResponse update(
            Long userId,
            MultipartFile image
    ) {
        User user = getActiveUser(userId);
        ImageAsset oldImage = user.getProfileImageAsset();
        ImageAsset newImage = storageService.store(userId, image);

        user.updateProfileImage(newImage);

        if (oldImage != null) {
            lifecycleService.softDelete(oldImage);
        }
        return ProfileImageResponse.from(newImage);
    }

    @Transactional
    public ProfileImageResponse reset(Long userId) {
        User user = getActiveUser(userId);
        ImageAsset oldImage = user.getProfileImageAsset();

        if (oldImage == null) {
            throw new CustomException(ErrorCode.NO_CHANGES_DETECTED);
        }

        user.resetProfileImage();
        lifecycleService.softDelete(oldImage);
        return ProfileImageResponse.from(null);
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.isUserDeleted()) {
            throw new CustomException(ErrorCode.USER_ALREADY_DELETED);
        }
        return user;
    }
}
