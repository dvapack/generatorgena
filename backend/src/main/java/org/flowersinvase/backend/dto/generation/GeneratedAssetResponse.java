package org.flowersinvase.backend.dto.generation;

import org.flowersinvase.backend.entity.generation.GeneratedAssetType;

import java.util.UUID;

public record GeneratedAssetResponse(
        UUID id,
        GeneratedAssetType assetType,
        String contentType,
        Integer width,
        Integer height,
        Integer duration,
        Integer sizeBytes
) {
}
