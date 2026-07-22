package com.ian.community.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * application.yaml의 app.storage 설정을 Java 객체로 바인딩합니다.
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        Path root,
        String profileDirectory,
        String feedDirectory,
        String cleanupCron
) {

    private static final Path DEFAULT_ROOT =
            Path.of("./storage/images");

    private static final String DEFAULT_PROFILE_DIRECTORY =
            "profile";

    private static final String DEFAULT_FEED_DIRECTORY =
            "feed";

    private static final String DEFAULT_CLEANUP_CRON =
            "0 0 3 * * *";

    public StorageProperties {
        root = normalizeRoot(root);

        profileDirectory = validateDirectoryName(
                profileDirectory,
                DEFAULT_PROFILE_DIRECTORY
        );

        feedDirectory = validateDirectoryName(
                feedDirectory,
                DEFAULT_FEED_DIRECTORY
        );

        cleanupCron = cleanupCron == null
                || cleanupCron.isBlank()
                ? DEFAULT_CLEANUP_CRON
                : cleanupCron.trim();
    }

    /**
     * 프로필 이미지 저장 디렉터리의 상대 경로를 만듭니다.
     *
     * 결과:
     * profile/{imageAssetId}
     */
    public String profileAssetDirectory(
            Object imageAssetId
    ) {
        if (imageAssetId == null) {
            throw new IllegalArgumentException(
                    "이미지 Asset ID는 필수입니다."
            );
        }

        return profileDirectory
                + "/"
                + imageAssetId;
    }

    /**
     * 피드 이미지 저장 디렉터리의 상대 경로를 만듭니다.
     *
     * 결과:
     * feed/{imageAssetId}
     */
    public String feedAssetDirectory(
            Object imageAssetId
    ) {
        if (imageAssetId == null) {
            throw new IllegalArgumentException(
                    "이미지 Asset ID는 필수입니다."
            );
        }

        return feedDirectory
                + "/"
                + imageAssetId;
    }

    private static Path normalizeRoot(
            Path configuredRoot
    ) {
        Path selectedRoot = configuredRoot == null
                ? DEFAULT_ROOT
                : configuredRoot;

        return selectedRoot
                .toAbsolutePath()
                .normalize();
    }

    private static String validateDirectoryName(
            String configuredName,
            String defaultName
    ) {
        String selectedName = configuredName == null
                || configuredName.isBlank()
                ? defaultName
                : configuredName.trim();

        Path directoryPath = Path.of(selectedName);

        if (directoryPath.isAbsolute()) {
            throw new IllegalArgumentException(
                    "Storage 하위 디렉터리는 절대 경로를 사용할 수 없습니다."
            );
        }

        Path normalized = directoryPath.normalize();

        if (normalized.startsWith("..")) {
            throw new IllegalArgumentException(
                    "Storage Root를 벗어나는 디렉터리는 사용할 수 없습니다."
            );
        }

        if (normalized.getNameCount() != 1) {
            throw new IllegalArgumentException(
                    "Storage 하위 디렉터리는 한 단계 이름만 사용할 수 있습니다."
            );
        }

        return normalized.toString();
    }
}