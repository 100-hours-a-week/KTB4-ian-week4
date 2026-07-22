package com.ian.community.image.service;

import com.ian.community.image.domain.ImageAsset;
import com.ian.community.image.domain.ImageStatus;
import com.ian.community.image.repository.ImageAssetRepository;
import com.ian.community.storage.LocalImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageCleanupWorker {

    private final ImageAssetRepository imageAssetRepository;
    private final LocalImageStorage localImageStorage;

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void purge(UUID imageAssetId) {
        ImageAsset imageAsset =
                imageAssetRepository
                        .findById(imageAssetId)
                        .orElse(null);

        if (imageAsset == null) {
            return;
        }

        if (imageAsset.getStatus()
                == ImageStatus.ACTIVE) {
            return;
        }

        try {
            localImageStorage
                    .deleteFilesAndEmptyParents(
                            imageAsset.getVariants()
                                    .stream()
                                    .map(variant ->
                                            variant.getStoragePath()
                                    )
                                    .toList()
                    );

            imageAssetRepository.delete(imageAsset);

        } catch (IOException exception) {
            imageAsset.markDeleteFailed(
                    exception.getMessage()
            );
        }
    }
}