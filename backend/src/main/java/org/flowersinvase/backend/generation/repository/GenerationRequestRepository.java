package org.flowersinvase.backend.generation.repository;

import org.flowersinvase.backend.generation.entity.GenerationRequest;
import org.flowersinvase.backend.generation.entity.GenerationStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GenerationRequestRepository {

    GenerationRequest save(GenerationRequest generationRequest);

    List<GenerationRequest> findAllByUserId(UUID userId, int offset, int limit);

    Optional<GenerationRequest> findById(UUID id);

    Optional<GenerationRequest> findByIdAndUserId(UUID id, UUID userId);

    boolean updateRating(UUID id, UUID userId, Integer rating);

    boolean updateStatus(UUID id, GenerationStatus status, OffsetDateTime completedAt);

    boolean deleteByUserAndId(UUID userId, UUID id);

    long countByUserId(UUID userId);
}
