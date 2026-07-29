package org.flowersinvase.backend.dto.messaging;

import org.flowersinvase.backend.entity.generation.GenerationType;

import java.util.UUID;

public record GenerateContentCommand(
        UUID commandId,
        UUID generationId,
        String prompt,
        GenerationType type
) {
}