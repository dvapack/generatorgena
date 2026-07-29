package org.flowersinvase.backend.entity.generation;

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
