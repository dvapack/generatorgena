package org.flowersinvase.backend.repository.generation;

import org.flowersinvase.backend.entity.generation.Generation;
import org.flowersinvase.backend.entity.generation.GenerationStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GenerationRequestRepository {

    Generation save(Generation generation);

    List<Generation> findAllByUserId(UUID userId, int offset, int limit);

    Optional<Generation> findById(UUID id);

    Optional<Generation> findByIdAndUserId(UUID id, UUID userId);

    Optional<Generation> findByIdForUpdate(UUID id);

    boolean updateRating(UUID id, UUID userId, Integer rating);

    boolean updateStatus(UUID id, GenerationStatus status, OffsetDateTime completedAt);

    boolean deleteByUserAndId(UUID userId, UUID id);

    long countByUserId(UUID userId);
}
