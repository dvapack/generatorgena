package org.flowersinvase.backend.dto.generation;

import org.flowersinvase.backend.enums.GenerationStatus;

import java.util.UUID;

public record CreateGenerationResponse(
        UUID id,
        GenerationStatus status
) {
}
