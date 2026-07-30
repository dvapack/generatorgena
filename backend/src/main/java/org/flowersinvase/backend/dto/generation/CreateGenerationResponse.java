package org.flowersinvase.backend.dto.generation;

import jakarta.validation.constraints.NotNull;
import org.flowersinvase.backend.enums.GenerationStatus;
import org.flowersinvase.backend.enums.GenerationType;

import java.util.UUID;

public record CreateGenerationResponse(
        UUID id,
        GenerationType type,
        GenerationStatus status
) {
}
