package org.flowersinvase.backend.generation.dto;

import org.flowersinvase.backend.generation.entity.GeneratedAssetType;

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
