package org.flowersinvase.backend.dto.generation;

import jakarta.validation.constraints.NotBlank;

public record CreateGenerationRequest(
        @NotBlank(message = "Промпт не может быть пустым")
        String prompt
) {
}
