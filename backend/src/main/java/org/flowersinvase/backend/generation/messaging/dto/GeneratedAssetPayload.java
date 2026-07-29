package org.flowersinvase.backend.generation.messaging.dto;

import org.flowersinvase.backend.generation.entity.GeneratedAssetType;

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
