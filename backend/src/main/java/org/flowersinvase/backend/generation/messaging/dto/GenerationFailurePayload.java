package org.flowersinvase.backend.generation.messaging.dto;

public record GenerationFailurePayload(
        String code,
        String message,
        boolean retryable
) {
}
