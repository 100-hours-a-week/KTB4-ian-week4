package com.ian.community.image.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 하나의 ImageAsset에서 생성된 실제 이미지 파일 정보를 저장합니다.
 *
 * 예:
 * - high.webp
 * - 160.webp
 * - 34.webp
 */
@Getter
@Entity
@Table(
        name = "image_variants",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_image_variant_asset_type",
                        columnNames = {
                                "image_asset_id",
                                "variant_type"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_image_variant_asset",
                        columnList = "image_asset_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_variant_id")
    private Long imageVariantId;

    /**
     * 이 Variant가 속한 이미지 묶음입니다.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "image_asset_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_image_variant_asset"
            )
    )
    private ImageAsset imageAsset;

    /**
     * PROFILE_HIGH, PROFILE_160 등의 용도입니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "variant_type",
            nullable = false,
            length = 30
    )
    private ImageVariantType variantType;

    /**
     * Storage Root를 제외한 상대 경로입니다.
     *
     * 예:
     * profile/{imageAssetId}/160.webp
     */
    @Column(
            name = "storage_path",
            nullable = false,
            length = 500
    )
    private String storagePath;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(
            name = "mime_type",
            nullable = false,
            length = 50
    )
    private String mimeType;

    @Column(
            name = "file_size",
            nullable = false
    )
    private long fileSize;

    public ImageVariant(
            ImageAsset imageAsset,
            ImageVariantType variantType,
            String storagePath,
            int width,
            int height,
            long fileSize
    ) {
        validate(
                imageAsset,
                variantType,
                storagePath,
                width,
                height,
                fileSize
        );

        this.imageAsset = imageAsset;
        this.variantType = variantType;
        this.storagePath = storagePath;
        this.width = width;
        this.height = height;
        this.mimeType = "image/webp";
        this.fileSize = fileSize;
    }

    private void validate(
            ImageAsset imageAsset,
            ImageVariantType variantType,
            String storagePath,
            int width,
            int height,
            long fileSize
    ) {
        if (imageAsset == null) {
            throw new IllegalArgumentException(
                    "ImageAsset은 필수입니다."
            );
        }

        if (variantType == null) {
            throw new IllegalArgumentException(
                    "이미지 Variant 종류는 필수입니다."
            );
        }

        if (storagePath == null
                || storagePath.isBlank()) {
            throw new IllegalArgumentException(
                    "이미지 저장 경로는 필수입니다."
            );
        }

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "이미지 크기는 0보다 커야 합니다."
            );
        }

        if (fileSize < 0) {
            throw new IllegalArgumentException(
                    "파일 크기는 음수가 될 수 없습니다."
            );
        }
    }
}