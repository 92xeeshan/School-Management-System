package com.schoolms.certificate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CertificateScheduler {

    private final CertificateLifecycleService lifecycleService;

    @Scheduled(cron = "0 15 6 * * *")
    public void deactivateTransferAccounts() {
        try {
            int count = lifecycleService.deactivateDueAccounts();
            if (count > 0) {
                log.info("Deactivated {} student accounts after transfer certificate grace period", count);
            }
        } catch (Exception ex) {
            log.warn("Transfer certificate deactivation job failed: {}", ex.getMessage());
        }
    }
}
