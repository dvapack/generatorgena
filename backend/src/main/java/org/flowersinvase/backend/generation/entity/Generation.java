package org.flowersinvase.backend.generation.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Generation(
      UUID id,
      UUID userId,
      String prompt,
      GenerationType type,
      GenerationStatus status,
      Integer rating,
      OffsetDateTime createdAt,
      OffsetDateTime completedAt
) {
}
