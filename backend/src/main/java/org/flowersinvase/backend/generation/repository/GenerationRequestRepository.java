package org.flowersinvase.backend.generation.repository;

import org.flowersinvase.backend.generation.entity.Generation;
import org.flowersinvase.backend.generation.entity.GenerationStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GenerationRequestRepository {

    Generation save(Generation generation);

    List<Generation> findAllByUserId(UUID userId, int offset, int limit);

    Optional<Generation> findById(UUID id);

    Optional<Generation> findByIdAndUserId(UUID id, UUID userId);

    boolean updateRating(UUID id, UUID userId, Integer rating);

    boolean updateStatus(UUID id, GenerationStatus status, OffsetDateTime completedAt);

    boolean deleteByUserAndId(UUID userId, UUID id);

    long countByUserId(UUID userId);
}
