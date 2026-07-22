package com.ian.community.image.service;

import com.ian.community.image.domain.ImageAsset;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileImageStorageService {

    ImageAsset store(
            Long ownerUserId,
            MultipartFile image
    );
}