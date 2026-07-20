package org.flowersinvase.backend.generation.entity;

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
