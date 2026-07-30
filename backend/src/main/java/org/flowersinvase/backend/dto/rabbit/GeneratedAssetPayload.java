package org.flowersinvase.backend.dto.rabbit;

import org.flowersinvase.backend.enums.GeneratedAssetType;

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
