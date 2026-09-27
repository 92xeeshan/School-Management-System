package com.schoolms.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationTriggerService triggerService;

    @Scheduled(cron = "0 0 6 * * *")
    public void dailyReminders() {
        try {
            triggerService.runDailyReminders();
        } catch (Exception ex) {
            log.warn("Daily notification reminders failed: {}", ex.getMessage());
        }
    }
}
