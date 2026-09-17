package org.flowersinvase.backend.repository.outbox;

import org.flowersinvase.backend.entity.outbox.OutboxMessage;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository {

    void save(OutboxMessage message);

    List<OutboxMessage> findPendingForUpdate(int limit);

    void markPublished(UUID id);

    void scheduleRetry(UUID id, int attempts, OffsetDateTime nextAttemptAt, String error);

    int deletePublishedBefore(OffsetDateTime threshold);
}

