package org.flowersinvase.backend.repository.generation;

import org.flowersinvase.backend.entity.generation.GeneratedAsset;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GeneratedAssetRepository {

    GeneratedAsset save(GeneratedAsset generatedAsset);

    Optional<GeneratedAsset> findByUserIdAndRequestId(UUID userId, UUID requestId);

    Optional<GeneratedAsset> findByRequestId(UUID requestId);

    List<GeneratedAsset> findAllByRequestIds(Collection<UUID> requestIds);

    boolean deleteByRequestId(UUID requestId);

    boolean deleteByUserIdAndRequestId(UUID userId, UUID requestId);
}
