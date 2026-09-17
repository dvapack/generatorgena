package org.flowersinvase.backend.dto.generation;

import java.util.UUID;

public record GeneratedAssetResponse(
        UUID id,
        String contentType,
        Integer sizeBytes
) {
}
