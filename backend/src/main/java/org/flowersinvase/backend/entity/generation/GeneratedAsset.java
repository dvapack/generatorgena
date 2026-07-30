package org.flowersinvase.backend.entity.generation;

import org.flowersinvase.backend.enums.GeneratedAssetType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GeneratedAsset(
        UUID id,
        UUID requestId,
        String objectKey,
        GeneratedAssetType assetType,
        String contentType,
        Integer sizeBytes,
        Integer duration,
        Integer width,
        Integer height,
        OffsetDateTime createdAt
) {
}
