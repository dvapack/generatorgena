package org.flowersinvase.backend.generation.service;

import org.flowersinvase.backend.generation.dto.*;

import java.util.UUID;

public interface GenerationService {

    CreateGenerationResponse create(UUID userId, CreateGenerationRequest request);

    GenerationResponse getById(UUID userId, UUID generationId);

    void updateRating(
            UUID userId, UUID generationId, UpdateGenerationRatingRequest request
    );

    GenerationPageResponse getPage(UUID userId, int page, int size);

    void delete(UUID userId, UUID generationId);
}
