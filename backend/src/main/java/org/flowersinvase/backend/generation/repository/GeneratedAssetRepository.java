package org.flowersinvase.backend.generation.repository;

import org.flowersinvase.backend.generation.entity.GeneratedAsset;

import java.util.Optional;
import java.util.UUID;

public interface GeneratedAssetRepository {

    GeneratedAsset save(GeneratedAsset generatedAsset);

    Optional<GeneratedAsset> findByUserIdAndRequestId(UUID id, UUID requestId);

    boolean deleteByIdAndRequestId(UUID id, UUID requestId);
}
