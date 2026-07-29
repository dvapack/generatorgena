package org.flowersinvase.backend.dto.messaging;

import org.flowersinvase.backend.entity.generation.GeneratedAssetType;

public record GeneratedAssetPayload(
        String objectKey,
        GeneratedAssetType assetType,
        String contentType,
        Integer sizeBytes,
        Integer width,
        Integer height,
        Integer duration
) {
}
