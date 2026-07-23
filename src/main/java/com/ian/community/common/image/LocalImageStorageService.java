package com.ian.community.common.image;

import com.ian.community.common.exception.CustomException;
import com.ian.community.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
public class LocalImageStorageService {
    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
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

    public String storeProfile(MultipartFile image) {
        return store(image, "profile");
    }

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

        String extension = EXTENSIONS.get(image.getContentType());
        if (extension == null) {
            throw new CustomException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }

        Path targetDirectory = storageRoot.resolve(directory).normalize();
        String filename = UUID.randomUUID() + extension;
        Path destination = targetDirectory.resolve(filename).normalize();
        if (!destination.startsWith(targetDirectory)) {
            throw new CustomException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }

        try {
            Files.createDirectories(targetDirectory);
            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(
                        inputStream,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (IOException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return "/uploads/" + directory + "/" + filename;
    }
}
