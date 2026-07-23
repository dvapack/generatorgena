package org.flowersinvase.backend.generation.repository;

import org.flowersinvase.backend.generation.entity.GeneratedAsset;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GeneratedAssetRepository {

    GeneratedAsset save(GeneratedAsset generatedAsset);

    Optional<GeneratedAsset> findByUserIdAndRequestId(UUID userId, UUID requestId);

    List<GeneratedAsset> findAllByRequestIds(Collection<UUID> requestIds);

    boolean deleteByRequestId(UUID requestId);

    boolean deleteByUserIdAndRequestId(UUID userId, UUID requestId);
}
