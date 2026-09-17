package org.flowersinvase.backend.dto.rabbit;

import org.flowersinvase.backend.enums.GenerationStatus;

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
