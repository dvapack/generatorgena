package org.flowersinvase.backend.service.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxBatchPublisher batchPublisher;

    @Value("${app.outbox.retention:7d}")
    private Duration retention;

    @Scheduled(fixedDelayString = "${app.outbox.poll-delay:1s}")
    public void publishPendingMessages() {
        batchPublisher.publishBatch();
    }

    @Scheduled(
            fixedDelayString = "${app.outbox.cleanup-delay:1d}",
            initialDelayString = "${app.outbox.cleanup-delay:1d}"
    )
    public void cleanupPublishedMessages() {
        int deleted = batchPublisher.deletePublishedBefore(OffsetDateTime.now().minus(retention));
        if (deleted > 0) {
            log.info("Удалены старые записи outbox: count={}", deleted);
        }
    }
}
