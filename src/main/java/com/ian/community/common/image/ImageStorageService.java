package com.ian.community.common.image;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {
    String storeProfile(MultipartFile image);

    String storeFeed(MultipartFile image);
}
