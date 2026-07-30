package org.flowersinvase.backend.dto.generation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.flowersinvase.backend.enums.GenerationType;

public record CreateGenerationRequest(
        @NotBlank(message = "Промпт не может быть пустым")
        String prompt,
        @NotNull(message = "Тип генерации обязателен")
        GenerationType type

) {
}
