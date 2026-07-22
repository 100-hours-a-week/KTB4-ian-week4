package com.ian.community.image.service;

import com.ian.community.image.domain.ImageAsset;
import com.ian.community.image.repository.ImageAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageLifecycleService {

    private final ImageAssetRepository imageAssetRepository;
    private final Clock clock;

    @Transactional
    public void softDelete(ImageAsset imageAsset) {
        if (imageAsset == null) {
            return;
        }

        imageAsset.softDelete(clock);
    }

    @Transactional
    public void softDelete(UUID imageAssetId) {
        ImageAsset imageAsset =
                imageAssetRepository
                        .findById(imageAssetId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "이미지를 찾을 수 없습니다."
                                )
                        );

        imageAsset.softDelete(clock);
    }
}