package org.flowersinvase.backend.entity.outbox;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OutboxMessage(
        UUID id,
        UUID aggregateId,
        String payload,
        OffsetDateTime createdAt,
        int attempts,
        OffsetDateTime nextAttemptAt
) {
}

