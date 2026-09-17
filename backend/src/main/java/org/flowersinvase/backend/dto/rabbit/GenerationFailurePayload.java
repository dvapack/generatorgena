package org.flowersinvase.backend.dto.rabbit;

public record GenerationFailurePayload(
        String code,
        String message,
        boolean retryable
) {
}
