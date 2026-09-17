package org.flowersinvase.backend.entity.generation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GeneratedAsset(
        UUID id,
        UUID requestId,
        String objectKey,
        String contentType,
        Integer sizeBytes,
        OffsetDateTime createdAt
) {
}
