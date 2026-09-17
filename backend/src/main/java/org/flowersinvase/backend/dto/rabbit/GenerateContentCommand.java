package org.flowersinvase.backend.dto.rabbit;

import org.flowersinvase.backend.enums.GenerationStatus;

import java.util.UUID;

public record GenerateContentCommand(
        UUID commandId,
        UUID generationId,
        String prompt,
        GenerationStatus status
) {
}