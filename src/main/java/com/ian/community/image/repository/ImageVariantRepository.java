package com.ian.community.image.repository;

import com.ian.community.image.domain.ImageAsset;
import com.ian.community.image.domain.ImageVariant;
import com.ian.community.image.domain.ImageVariantType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImageVariantRepository
        extends JpaRepository<ImageVariant, Long> {

    /**
     * 특정 ImageAsset의 모든 Variant를 조회합니다.
     */
    List<ImageVariant> findAllByImageAssetOrderByImageVariantIdAsc(
            ImageAsset imageAsset
    );

    /**
     * Asset ID를 기준으로 모든 Variant를 조회합니다.
     */
    List<ImageVariant> findAllByImageAsset_ImageAssetId(
            UUID imageAssetId
    );

    /**
     * 특정 Asset의 특정 Variant를 조회합니다.
     */
    Optional<ImageVariant>
    findByImageAsset_ImageAssetIdAndVariantType(
            UUID imageAssetId,
            ImageVariantType variantType
    );

    /**
     * 특정 Asset의 Variant가 존재하는지 검사합니다.
     */
    boolean existsByImageAsset_ImageAssetIdAndVariantType(
            UUID imageAssetId,
            ImageVariantType variantType
    );

    /**
     * 특정 Asset의 모든 Variant 정보를 삭제합니다.
     *
     * 일반적으로 ImageAsset의 Cascade 삭제를 사용하므로
     * 직접 호출할 일은 많지 않습니다.
     */
    void deleteAllByImageAsset_ImageAssetId(
            UUID imageAssetId
    );
}