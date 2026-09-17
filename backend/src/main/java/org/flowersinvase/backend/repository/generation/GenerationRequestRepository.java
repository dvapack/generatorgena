package org.flowersinvase.backend.repository.generation;

import org.flowersinvase.backend.entity.generation.GenerationEntity;
import org.flowersinvase.backend.enums.GenerationStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GenerationRequestRepository {

    GenerationEntity save(GenerationEntity generationEntity);

    List<GenerationEntity> findAllByUserId(UUID userId, int offset, int limit);

    Optional<GenerationEntity> findById(UUID id);

    Optional<GenerationEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<GenerationEntity> findByIdForUpdate(UUID id);

    boolean updateRating(UUID id, UUID userId, Integer rating);

    boolean updateStatus(UUID id, GenerationStatus status, OffsetDateTime completedAt);

    boolean deleteByUserAndId(UUID userId, UUID id);

    long countByUserId(UUID userId);
}
