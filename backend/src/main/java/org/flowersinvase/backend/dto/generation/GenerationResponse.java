package org.flowersinvase.backend.dto.generation;

import org.flowersinvase.backend.enums.GenerationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GenerationResponse(
        UUID id,
        String prompt,
        GenerationStatus status,
        Integer rating,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt,
        GeneratedAssetResponse asset
) {
}
