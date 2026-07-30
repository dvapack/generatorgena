package org.flowersinvase.backend.dto.generation;

import org.flowersinvase.backend.enums.GenerationStatus;
import org.flowersinvase.backend.enums.GenerationType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GenerationResponse(
        UUID id,
        GenerationType type,
        String prompt,
        GenerationStatus status,
        Integer rating,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt,
        GeneratedAssetResponse asset
) {
}
