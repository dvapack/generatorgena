package org.flowersinvase.backend.generation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.flowersinvase.backend.generation.entity.GenerationType;

public record CreateGenerationRequest(
        @NotBlank(message = "Промпт не может быть пустым")
        String prompt,

        @NotNull(message = "Тип генерации обязателен")
        GenerationType type

) {
}
