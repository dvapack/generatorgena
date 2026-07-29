package org.flowersinvase.backend.dto.generation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateGenerationRatingRequest(
        @NotNull(message = "Rating обязателен")
        @Min(value = 1, message = "Rating должен быть от 1 до 5")
        @Max(value = 5, message = "Rating должен быть от 1 до 5")
        Integer rating
) {
}
