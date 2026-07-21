package org.flowersinvase.backend.generation.dto;

import java.util.UUID;

public record GenerationRatingResponse(
        UUID id,
        Integer rating
) {
}
