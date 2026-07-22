package com.ian.community.image.repository;

import com.ian.community.image.domain.ImageAsset;
import com.ian.community.image.domain.ImageStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ImageAssetRepository
        extends JpaRepository<ImageAsset, UUID> {

    List<ImageAsset>
    findByStatusInAndPurgeAtLessThanEqualOrderByPurgeAtAsc(
            Collection<ImageStatus> statuses,
            LocalDateTime purgeAt,
            Pageable pageable
    );
}