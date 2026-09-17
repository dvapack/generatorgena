package org.flowersinvase.backend.service.generation;

import org.flowersinvase.backend.dto.generation.*;
import org.flowersinvase.backend.dto.rabbit.GenerationResultEvent;
import org.flowersinvase.backend.model.DownloadedAsset;

import java.util.UUID;

public interface GenerationService {

    CreateGenerationResponse create(UUID userId, CreateGenerationRequest request);

    GenerationResponse getById(UUID userId, UUID generationId);

    void updateRating(
            UUID userId, UUID generationId, UpdateGenerationRatingRequest request
    );

    GenerationPageResponse getPage(UUID userId, int page, int size);

    DownloadedAsset download(UUID userId, UUID generationId);

    void handleResult(GenerationResultEvent event);

    void delete(UUID userId, UUID generationId);
}
