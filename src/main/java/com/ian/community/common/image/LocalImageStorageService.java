package com.ian.community.common.image;

import com.ian.community.common.exception.CustomException;
import com.ian.community.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Service
public class LocalImageStorageService
        implements ImageStorageService {
    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    private static final int SIGNATURE_LENGTH = 12;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/webp", ".webp"
    );

    private final Path storageRoot;

    public LocalImageStorageService(
            @Value("${app.storage.root:./storage/images}") String storageRoot
    ) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @Override
    public String storeProfile(MultipartFile image) {
        return store(image, "profile");
    }

    @Override
    public String storeFeed(MultipartFile image) {
        return store(image, "feed");
    }

    private String store(MultipartFile image, String directory) {
        if (image == null || image.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_POST_REQUEST);
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new CustomException(ErrorCode.IMAGE_TOO_LARGE);
        }

        String contentType = image.getContentType();
        String extension = contentType == null
                ? null
                : EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new CustomException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }

        String filename = UUID.randomUUID() + extension;
        Path destination = null;

        try {
            Files.createDirectories(storageRoot);
            Path realStorageRoot = storageRoot.toRealPath();
            Path targetDirectory = storageRoot
                    .resolve(directory)
                    .normalize();
            Files.createDirectories(targetDirectory);
            Path realTargetDirectory =
                    targetDirectory.toRealPath();

            if (!realTargetDirectory.startsWith(
                    realStorageRoot
            )) {
                throw new CustomException(
                        ErrorCode.UNSUPPORTED_IMAGE_TYPE
                );
            }

            destination = realTargetDirectory
                    .resolve(filename)
                    .normalize();

            try (InputStream inputStream = image.getInputStream()) {
                byte[] signature = inputStream.readNBytes(
                        SIGNATURE_LENGTH
                );
                if (!matchesSignature(
                        contentType,
                        signature
                )) {
                    throw new CustomException(
                            ErrorCode.UNSUPPORTED_IMAGE_TYPE
                    );
                }

                try (OutputStream outputStream =
                             Files.newOutputStream(
                        destination,
                        StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE
                )) {
                    long written = signature.length;
                    outputStream.write(signature);

                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        written += read;
                        if (written > MAX_IMAGE_SIZE) {
                            throw new CustomException(
                                    ErrorCode.IMAGE_TOO_LARGE
                            );
                        }
                        outputStream.write(buffer, 0, read);
                    }
                }
            }
        } catch (CustomException exception) {
            deletePartialFile(destination);
            throw exception;
        } catch (IOException exception) {
            deletePartialFile(destination);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return "/uploads/" + directory + "/" + filename;
    }

    private boolean matchesSignature(
            String contentType,
            byte[] signature
    ) {
        return switch (contentType) {
            case "image/png" -> startsWith(
                    signature,
                    new byte[]{
                            (byte) 0x89, 0x50, 0x4E, 0x47,
                            0x0D, 0x0A, 0x1A, 0x0A
                    }
            );
            case "image/jpeg" -> startsWith(
                    signature,
                    new byte[]{
                            (byte) 0xFF, (byte) 0xD8,
                            (byte) 0xFF
                    }
            );
            case "image/webp" ->
                    signature.length >= SIGNATURE_LENGTH
                            && startsWith(
                            signature,
                            new byte[]{0x52, 0x49, 0x46, 0x46}
                    )
                            && Arrays.equals(
                            Arrays.copyOfRange(signature, 8, 12),
                            new byte[]{0x57, 0x45, 0x42, 0x50}
                    );
            default -> false;
        };
    }

    private boolean startsWith(
            byte[] value,
            byte[] prefix
    ) {
        return value.length >= prefix.length
                && Arrays.equals(
                Arrays.copyOf(value, prefix.length),
                prefix
        );
    }

    private void deletePartialFile(Path destination) {
        if (destination == null) {
            return;
        }
        try {
            Files.deleteIfExists(destination);
        } catch (IOException ignored) {
            // A failed cleanup must not expose the original file contents.
        }
    }
}
