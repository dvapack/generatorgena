package org.flowersinvase.backend.dto.messaging;

public record GenerationFailurePayload(
        String code,
        String message,
        boolean retryable
) {
}
