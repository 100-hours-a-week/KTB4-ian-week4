package com.ian.community.common.image;

import com.ian.community.common.exception.CustomException;
import com.ian.community.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalImageStorageServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void storesValidatedPngWithServerGeneratedName()
            throws Exception {
        LocalImageStorageService service =
                new LocalImageStorageService(
                        tempDirectory.toString()
                );
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "../../unsafe-name.png",
                "image/png",
                pngBytes()
        );

        String imageUrl = service.storeProfile(image);
        Path stored = tempDirectory.resolve(
                imageUrl.replace("/uploads/", "")
        );

        assertThat(imageUrl)
                .matches(
                        "/uploads/profile/"
                                + "[0-9a-f-]{36}\\.png"
                );
        assertThat(stored)
                .exists()
                .isRegularFile();
        assertThat(Files.isExecutable(stored)).isFalse();
    }

    @Test
    void rejectsContentThatDoesNotMatchDeclaredMimeType() {
        LocalImageStorageService service =
                new LocalImageStorageService(
                        tempDirectory.toString()
                );
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "fake.png",
                "image/png",
                "not-an-image".getBytes()
        );

        assertThatThrownBy(
                () -> service.storeFeed(image)
        )
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.UNSUPPORTED_IMAGE_TYPE
                );
        assertThat(tempDirectory.resolve("feed"))
                .isEmptyDirectory();
    }

    @Test
    void rejectsFilesLargerThanTenMegabytes() {
        LocalImageStorageService service =
                new LocalImageStorageService(
                        tempDirectory.toString()
                );
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "large.jpg",
                "image/jpeg",
                new byte[10 * 1024 * 1024 + 1]
        );

        assertThatThrownBy(
                () -> service.storeFeed(image)
        )
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IMAGE_TOO_LARGE);
    }

    @Test
    void rejectsFilesWithoutContentType() {
        LocalImageStorageService service =
                new LocalImageStorageService(
                        tempDirectory.toString()
                );
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "unknown",
                null,
                pngBytes()
        );

        assertThatThrownBy(
                () -> service.storeFeed(image)
        )
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(
                        ErrorCode.UNSUPPORTED_IMAGE_TYPE
                );
    }

    private byte[] pngBytes() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x00
        };
    }
}
