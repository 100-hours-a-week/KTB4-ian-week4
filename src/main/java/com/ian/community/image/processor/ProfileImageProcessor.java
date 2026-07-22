package com.ian.community.image.processor;

import com.ian.community.image.domain.ImageVariantType;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class ProfileImageProcessor {

    private static final int HIGH_MAX_SIZE = 2048;
    private static final int MEDIUM_SIZE = 160;
    private static final int SMALL_SIZE = 34;

    /**
     * 입력 이미지를 중앙 기준 정사각형으로 자르고
     * WebP 형식의 3개 Variant를 생성합니다.
     */
    public List<ProcessedVariant> process(
            BufferedImage source,
            Path outputDirectory,
            String relativeDirectory
    ) throws IOException {
        validateSource(
                source,
                outputDirectory,
                relativeDirectory
        );

        Files.createDirectories(outputDirectory);

        BufferedImage squareImage =
                centerSquareCrop(source);

        int highSize = Math.min(
                HIGH_MAX_SIZE,
                Math.min(
                        squareImage.getWidth(),
                        squareImage.getHeight()
                )
        );

        ProcessedVariant high =
                createVariant(
                        squareImage,
                        outputDirectory,
                        relativeDirectory,
                        "high.webp",
                        ImageVariantType.PROFILE_HIGH,
                        highSize
                );

        ProcessedVariant medium =
                createVariant(
                        squareImage,
                        outputDirectory,
                        relativeDirectory,
                        "160.webp",
                        ImageVariantType.PROFILE_160,
                        MEDIUM_SIZE
                );

        ProcessedVariant small =
                createVariant(
                        squareImage,
                        outputDirectory,
                        relativeDirectory,
                        "34.webp",
                        ImageVariantType.PROFILE_34,
                        SMALL_SIZE
                );

        return List.of(
                high,
                medium,
                small
        );
    }

    private ProcessedVariant createVariant(
            BufferedImage source,
            Path outputDirectory,
            String relativeDirectory,
            String filename,
            ImageVariantType variantType,
            int targetSize
    ) throws IOException {
        BufferedImage resized =
                resizeSquare(
                        source,
                        targetSize
                );

        Path outputPath =
                outputDirectory.resolve(filename);

        writeWebp(
                resized,
                outputPath
        );

        return new ProcessedVariant(
                variantType,
                relativeDirectory + "/" + filename,
                targetSize,
                targetSize,
                Files.size(outputPath)
        );
    }

    /**
     * 이미지 중앙을 기준으로 정사각형 영역을 추출합니다.
     */
    private BufferedImage centerSquareCrop(
            BufferedImage source
    ) {
        int cropSize = Math.min(
                source.getWidth(),
                source.getHeight()
        );

        int x =
                (source.getWidth() - cropSize) / 2;

        int y =
                (source.getHeight() - cropSize) / 2;

        BufferedImage cropped =
                source.getSubimage(
                        x,
                        y,
                        cropSize,
                        cropSize
                );

        return copyAsRgb(cropped);
    }

    /**
     * 정사각형 이미지를 지정한 크기로 조정합니다.
     */
    private BufferedImage resizeSquare(
            BufferedImage source,
            int targetSize
    ) {
        BufferedImage output =
                new BufferedImage(
                        targetSize,
                        targetSize,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D graphics =
                output.createGraphics();

        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );

            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );

            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            /*
             * PNG 투명 배경이 들어오는 경우
             * JPEG/WebP 변환 시 검은색으로 변하는 것을 막기 위해
             * 흰색 배경을 먼저 그립니다.
             */
            graphics.setColor(Color.WHITE);
            graphics.fillRect(
                    0,
                    0,
                    targetSize,
                    targetSize
            );

            graphics.drawImage(
                    source,
                    0,
                    0,
                    targetSize,
                    targetSize,
                    null
            );

        } finally {
            graphics.dispose();
        }

        return output;
    }

    private BufferedImage copyAsRgb(
            BufferedImage source
    ) {
        BufferedImage copied =
                new BufferedImage(
                        source.getWidth(),
                        source.getHeight(),
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D graphics =
                copied.createGraphics();

        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(
                    0,
                    0,
                    copied.getWidth(),
                    copied.getHeight()
            );

            graphics.drawImage(
                    source,
                    0,
                    0,
                    null
            );

        } finally {
            graphics.dispose();
        }

        return copied;
    }

    private void writeWebp(
            BufferedImage image,
            Path outputPath
    ) throws IOException {
        boolean written =
                ImageIO.write(
                        image,
                        "webp",
                        outputPath.toFile()
                );

        if (!written) {
            throw new IOException(
                    "WebP ImageIO Writer를 찾을 수 없습니다."
            );
        }
    }

    private void validateSource(
            BufferedImage source,
            Path outputDirectory,
            String relativeDirectory
    ) {
        if (source == null) {
            throw new IllegalArgumentException(
                    "원본 이미지는 필수입니다."
            );
        }

        if (source.getWidth() <= 0
                || source.getHeight() <= 0) {
            throw new IllegalArgumentException(
                    "원본 이미지 크기가 올바르지 않습니다."
            );
        }

        if (outputDirectory == null) {
            throw new IllegalArgumentException(
                    "출력 디렉터리는 필수입니다."
            );
        }

        if (relativeDirectory == null
                || relativeDirectory.isBlank()) {
            throw new IllegalArgumentException(
                    "상대 저장 경로는 필수입니다."
            );
        }
    }

    /**
     * 생성된 파일을 ImageVariant Entity로 변환하기 위한 결과입니다.
     */
    public record ProcessedVariant(
            ImageVariantType variantType,
            String relativePath,
            int width,
            int height,
            long fileSize
    ) {
    }
}