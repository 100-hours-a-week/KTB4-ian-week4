package com.ian.community.image.controller;

import com.ian.community.image.domain.*;
import com.ian.community.image.repository.ImageAssetRepository;
import com.ian.community.storage.LocalImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageResourceController {
    private final ImageAssetRepository imageAssetRepository;
    private final LocalImageStorage storage;

    @GetMapping("/{assetId}/{variantName}")
    public ResponseEntity<Resource> getImage(
            @PathVariable UUID assetId,
            @PathVariable String variantName
    ) {
        ImageAsset asset = imageAssetRepository.findById(assetId)
                .filter(item ->
                        item.getStatus() == ImageStatus.ACTIVE)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "이미지를 찾을 수 없습니다."
                        ));

        ImageVariantType variantType =
                resolveVariantType(variantName);

        ImageVariant variant = asset.getVariants()
                .stream()
                .filter(item ->
                        item.getVariantType() == variantType)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "이미지 Variant를 찾을 수 없습니다."
                        ));

        Path path = storage.resolveFile(variant.getStoragePath());
        Resource resource = new FileSystemResource(path);

        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalArgumentException(
                    "이미지 파일을 찾을 수 없습니다."
            );
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        variant.getMimeType()
                ))
                .cacheControl(
                        CacheControl.maxAge(
                                java.time.Duration.ofDays(30)
                        ).cachePublic()
                )
                .body(resource);
    }

    private ImageVariantType resolveVariantType(String name) {
        return switch (name) {
            case "profile-high" ->
                    ImageVariantType.PROFILE_HIGH;
            case "profile-160" ->
                    ImageVariantType.PROFILE_160;
            case "profile-34" ->
                    ImageVariantType.PROFILE_34;
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 이미지 Variant입니다."
            );
        };
    }
}
