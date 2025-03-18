package com.sensa.usermanagementservice.task;

import com.sensa.usermanagementservice.service.SystemDataCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ScheduledTasks {
    private final SystemDataCleanupService systemDataCleanupService;

    @Scheduled(cron = "0 0 0 * *")
    public void scheduleDataCleanup() {
        systemDataCleanupService.deleteOldData();
        log.info("Scheduled data cleanup: {}", LocalDateTime.now());
    }
}
