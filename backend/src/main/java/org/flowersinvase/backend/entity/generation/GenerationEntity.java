package org.flowersinvase.backend.entity.generation;

import org.flowersinvase.backend.enums.GenerationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GenerationEntity(
      UUID id,
      UUID userId,
      String prompt,
      GenerationStatus status,
      Integer rating,
      OffsetDateTime createdAt,
      OffsetDateTime completedAt
) {
}
