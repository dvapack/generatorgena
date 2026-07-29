package org.flowersinvase.backend.dto.generation;

import org.flowersinvase.backend.entity.generation.GenerationStatus;
import org.flowersinvase.backend.entity.generation.GenerationType;

import java.util.UUID;

public record CreateGenerationResponse(
        UUID id,
        GenerationType type,
        GenerationStatus status
) {
}
