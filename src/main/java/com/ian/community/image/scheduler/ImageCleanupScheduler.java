package com.ian.community.image.scheduler;

import com.ian.community.image.service.ImageCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class ImageCleanupScheduler {

    private final ImageCleanupService imageCleanupService;

    @Scheduled(
            cron = "${app.storage.cleanup-cron:0 0 3 * * *}",
            zone = "Asia/Seoul"
    )
    public void cleanup() {
        imageCleanupService.purgeExpiredImages();
    }
}