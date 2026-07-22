package com.ian.community.storage;

import com.ian.community.config.StorageProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

@Component
public class LocalImageStorage {

    private final Path root;

    public LocalImageStorage(
            StorageProperties storageProperties
    ) throws IOException {
        this.root = storageProperties
                .root()
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(this.root);
    }

    /**
     * Storage Root 아래에 이미지 저장 디렉터리를 만듭니다.
     *
     * 예:
     * relativeDirectory = profile/{imageAssetId}
     */
    public Path createDirectory(
            String relativeDirectory
    ) throws IOException {
        Path directory = resolveSafe(
                relativeDirectory
        );

        Files.createDirectories(directory);

        return directory;
    }

    /**
     * Storage Root 기준 상대 경로를
     * 안전한 절대 경로로 변환합니다.
     *
     * 실제 파일 생성 여부는 확인하지 않습니다.
     */
    public Path resolveFile(
            String relativePath
    ) {
        return resolveSafe(relativePath);
    }

    /**
     * 파일이 실제로 존재하며 읽을 수 있는지 확인한 후
     * 경로를 반환합니다.
     *
     * ImageResourceController에서 사용할 수 있습니다.
     */
    public Path resolveReadableFile(
            String relativePath
    ) throws IOException {
        Path file = resolveSafe(relativePath);

        if (!Files.exists(
                file,
                LinkOption.NOFOLLOW_LINKS
        )) {
            throw new IOException(
                    "이미지 파일을 찾을 수 없습니다."
            );
        }

        if (!Files.isRegularFile(
                file,
                LinkOption.NOFOLLOW_LINKS
        )) {
            throw new IOException(
                    "요청 경로가 파일이 아닙니다."
            );
        }

        if (!Files.isReadable(file)) {
            throw new IOException(
                    "이미지 파일을 읽을 수 없습니다."
            );
        }

        return file;
    }

    /**
     * ImageVariant에 저장된 상대 경로 목록을 받아
     * 실제 파일을 삭제합니다.
     *
     * 파일을 삭제한 후 비어 있는 디렉터리도 정리합니다.
     */
    public void deleteFilesAndEmptyParents(
            List<String> relativePaths
    ) throws IOException {
        if (relativePaths == null
                || relativePaths.isEmpty()) {
            return;
        }

        IOException firstException = null;

        for (String relativePath : relativePaths) {
            try {
                deleteFileAndEmptyParents(
                        relativePath
                );
            } catch (IOException exception) {
                if (firstException == null) {
                    firstException = exception;
                } else {
                    firstException.addSuppressed(
                            exception
                    );
                }
            }
        }

        if (firstException != null) {
            throw firstException;
        }
    }

    /**
     * 단일 파일을 삭제하고 비어 있는 상위 디렉터리를
     * Storage Root 직전까지 정리합니다.
     */
    public void deleteFileAndEmptyParents(
            String relativePath
    ) throws IOException {
        Path file = resolveSafe(relativePath);

        Files.deleteIfExists(file);

        deleteEmptyParents(
                file.getParent()
        );
    }

    /**
     * 특정 이미지 Asset 디렉터리 전체를 삭제합니다.
     *
     * 업로드 도중 오류가 발생하거나
     * DB Transaction이 Rollback됐을 때 사용합니다.
     *
     * 예:
     * profile/{imageAssetId}
     */
    public void deleteDirectory(
            String relativeDirectory
    ) throws IOException {
        Path directory = resolveSafe(
                relativeDirectory
        );

        if (!Files.exists(
                directory,
                LinkOption.NOFOLLOW_LINKS
        )) {
            return;
        }

        if (!Files.isDirectory(
                directory,
                LinkOption.NOFOLLOW_LINKS
        )) {
            throw new IOException(
                    "삭제 대상이 디렉터리가 아닙니다."
            );
        }

        try (var paths = Files.walk(directory)) {
            List<Path> deleteTargets =
                    paths.sorted(
                            Comparator.reverseOrder()
                    ).toList();

            for (Path target : deleteTargets) {
                Files.deleteIfExists(target);
            }
        }

        deleteEmptyParents(
                directory.getParent()
        );
    }

    /**
     * Storage Root 자체는 삭제하지 않고,
     * 비어 있는 하위 디렉터리만 제거합니다.
     */
    private void deleteEmptyParents(
            Path startDirectory
    ) throws IOException {
        Path current = startDirectory;

        while (current != null
                && !current.equals(root)) {

            if (!Files.exists(
                    current,
                    LinkOption.NOFOLLOW_LINKS
            )) {
                current = current.getParent();
                continue;
            }

            if (!Files.isDirectory(
                    current,
                    LinkOption.NOFOLLOW_LINKS
            )) {
                return;
            }

            if (!isEmptyDirectory(current)) {
                return;
            }

            Files.deleteIfExists(current);

            current = current.getParent();
        }
    }

    private boolean isEmptyDirectory(
            Path directory
    ) throws IOException {
        try (var entries = Files.list(directory)) {
            return entries.findAny().isEmpty();
        }
    }

    /**
     * 외부에서 전달된 상대 경로가 Storage Root를
     * 벗어나지 못하도록 검사합니다.
     *
     * ../../ 같은 경로 탐색 공격을 차단합니다.
     */
    private Path resolveSafe(
            String relativePath
    ) {
        if (relativePath == null
                || relativePath.isBlank()) {
            throw new IllegalArgumentException(
                    "Storage 상대 경로는 필수입니다."
            );
        }

        Path inputPath = Path.of(relativePath);

        if (inputPath.isAbsolute()) {
            throw new IllegalArgumentException(
                    "절대 경로는 사용할 수 없습니다."
            );
        }

        Path resolved = root
                .resolve(inputPath)
                .normalize();

        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException(
                    "Storage Root를 벗어난 경로입니다."
            );
        }

        return resolved;
    }
}