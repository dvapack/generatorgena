package org.flowersinvase.backend.generation.dto;

import org.flowersinvase.backend.generation.entity.GenerationStatus;
import org.flowersinvase.backend.generation.entity.GenerationType;

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
