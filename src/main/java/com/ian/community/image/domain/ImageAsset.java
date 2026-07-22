package com.ian.community.image.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Table(name = "image_assets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageAsset {
    @Id
    @Column(name = "image_asset_id", columnDefinition = "UUID")
    private UUID imageAssetId;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 20)
    private ImageType imageType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImageStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "purge_at")
    private LocalDateTime purgeAt;

    @Column(name = "delete_retry_count", nullable = false)
    private int deleteRetryCount;

    @Column(name = "last_delete_error", length = 500)
    private String lastDeleteError;

    @OneToMany(mappedBy = "imageAsset", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImageVariant> variants = new ArrayList<>();

    public ImageAsset(Long ownerUserId, ImageType imageType, Clock clock) {
        this.imageAssetId = UUID.randomUUID();      // 랜덤한 UUID 값
        this.ownerUserId = ownerUserId;
        this.imageType = imageType;
        this.status = ImageStatus.ACTIVE;
        this.createdAt = LocalDateTime.now(clock);
    }

    public void addVariant(ImageVariant variant) {
        variants.add(variant);
    }

    public void softDelete(Clock clock) {
        if (status != ImageStatus.ACTIVE) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        this.status = ImageStatus.DELETED;
        this.deletedAt = now;
        this.purgeAt = now.plusDays(30);
        this.lastDeleteError = null;
    }

    public void markDeleteFailed(String error) {
        this.status = ImageStatus.DELETE_FAILED;
        this.deleteRetryCount++;
        this.lastDeleteError = error == null
                ? "unknown"
                : error.substring(0, Math.min(error.length(), 500));
    }

    public void prepareRetry() {
        if (status == ImageStatus.DELETE_FAILED) {
            status = ImageStatus.DELETED;
        }
    }
}
