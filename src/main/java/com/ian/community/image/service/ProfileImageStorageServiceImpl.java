package com.ian.community.image.service;

import com.ian.community.common.exception.CustomException;
import com.ian.community.common.exception.ErrorCode;
import com.ian.community.image.domain.ImageAsset;
import com.ian.community.image.domain.ImageType;
import com.ian.community.image.domain.ImageVariant;
import com.ian.community.image.processor.ProfileImageProcessor;
import com.ian.community.image.repository.ImageAssetRepository;
import com.ian.community.storage.LocalImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProfileImageStorageServiceImpl
        implements ProfileImageStorageService {

    private static final long MAX_FILE_SIZE =
            10L * 1024L * 1024L;

    private static final Set<String> ALLOWED_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp"
            );

    private final ImageAssetRepository imageAssetRepository;
    private final ProfileImageProcessor profileImageProcessor;
    private final LocalImageStorage localImageStorage;
    private final Clock clock;

    @Override
    public ImageAsset store(
            Long ownerUserId,
            MultipartFile image
    ) {
        validate(image);

        BufferedImage source = decode(image);

        ImageAsset imageAsset =
                new ImageAsset(
                        ownerUserId,
                        ImageType.PROFILE,
                        clock
                );

        String relativeDirectory =
                "profile/" + imageAsset.getImageAssetId();

        try {
            Path directory =
                    localImageStorage.createDirectory(
                            relativeDirectory
                    );

            var results =
                    profileImageProcessor.process(
                            source,
                            directory,
                            relativeDirectory
                    );

            for (var result : results) {
                imageAsset.addVariant(
                        new ImageVariant(
                                imageAsset,
                                result.variantType(),
                                result.relativePath(),
                                result.width(),
                                result.height(),
                                result.fileSize()
                        )
                );
            }

            registerRollbackCleanup(relativeDirectory);

            return imageAssetRepository.save(imageAsset);

        } catch (IOException exception) {
            deleteDirectoryQuietly(relativeDirectory);

            throw new CustomException(
                    ErrorCode.INTERNAL_SERVER_ERROR
            );
        }
    }

    private void validate(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new CustomException(
                    ErrorCode.UNSUPPORTED_IMAGE_TYPE
            );
        }

        if (image.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(
                    ErrorCode.IMAGE_TOO_LARGE
            );
        }

        String contentType = image.getContentType();

        if (contentType == null
                || !ALLOWED_TYPES.contains(contentType)) {
            throw new CustomException(
                    ErrorCode.UNSUPPORTED_IMAGE_TYPE
            );
        }
    }

    private BufferedImage decode(MultipartFile image) {
        try {
            BufferedImage decoded =
                    ImageIO.read(image.getInputStream());

            if (decoded == null) {
                throw new CustomException(
                        ErrorCode.UNSUPPORTED_IMAGE_TYPE
                );
            }

            return decoded;

        } catch (IOException exception) {
            throw new CustomException(
                    ErrorCode.UNSUPPORTED_IMAGE_TYPE
            );
        }
    }

    private void registerRollbackCleanup(
            String relativeDirectory
    ) {
        if (!TransactionSynchronizationManager
                .isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCompletion(
                                    int status
                            ) {
                                if (status
                                        != STATUS_COMMITTED) {
                                    deleteDirectoryQuietly(
                                            relativeDirectory
                                    );
                                }
                            }
                        }
                );
    }

    private void deleteDirectoryQuietly(
            String relativeDirectory
    ) {
        try {
            localImageStorage.deleteDirectory(
                    relativeDirectory
            );
        } catch (IOException ignored) {
            // 추후 로그 기록 추가
        }
    }
}