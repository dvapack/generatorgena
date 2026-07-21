package org.flowersinvase.backend.generation.dto;

import org.flowersinvase.backend.generation.entity.GenerationStatus;
import org.flowersinvase.backend.generation.entity.GenerationType;

import java.util.UUID;

public record CreateGenerationResponse(
        UUID id,
        GenerationType type,
        GenerationStatus status
) {
}
