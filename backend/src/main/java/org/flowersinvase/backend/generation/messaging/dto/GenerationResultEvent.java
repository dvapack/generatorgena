package org.flowersinvase.backend.generation.messaging.dto;

import org.flowersinvase.backend.generation.entity.GenerationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GenerationResultEvent(
        UUID eventId,
        UUID commandId,
        UUID generationId,
        GenerationStatus status,
        OffsetDateTime occurredAt,
        GeneratedAssetPayload asset,
        GenerationFailurePayload error
) {
}
