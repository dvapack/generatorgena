package org.flowersinvase.backend.generation.messaging;

import org.flowersinvase.backend.generation.entity.GenerationType;

import java.util.UUID;

public record GenerateContentCommand(
        UUID commandId,
        UUID generationId,
        String prompt,
        GenerationType type
) {
}