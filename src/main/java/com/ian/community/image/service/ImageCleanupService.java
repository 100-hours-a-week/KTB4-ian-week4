package com.ian.community.image.service;

import com.ian.community.image.domain.ImageAsset;
import com.ian.community.image.domain.ImageStatus;
import com.ian.community.image.repository.ImageAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageCleanupService {

    private static final int BATCH_SIZE = 100;

    private final ImageAssetRepository imageAssetRepository;
    private final ImageCleanupWorker imageCleanupWorker;
    private final Clock clock;

    public void purgeExpiredImages() {
        List<ImageAsset> targets =
                imageAssetRepository
                        .findByStatusInAndPurgeAtLessThanEqualOrderByPurgeAtAsc(
                                List.of(
                                        ImageStatus.DELETED,
                                        ImageStatus.DELETE_FAILED
                                ),
                                LocalDateTime.now(clock),
                                PageRequest.of(
                                        0,
                                        BATCH_SIZE
                                )
                        );

        for (ImageAsset target : targets) {
            imageCleanupWorker.purge(
                    target.getImageAssetId()
            );
        }
    }
}