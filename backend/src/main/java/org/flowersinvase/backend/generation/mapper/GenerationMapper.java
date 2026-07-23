package org.flowersinvase.backend.generation.mapper;

import org.flowersinvase.backend.generation.dto.*;
import org.flowersinvase.backend.generation.entity.GeneratedAsset;
import org.flowersinvase.backend.generation.entity.GenerationRequest;
import org.flowersinvase.backend.generation.entity.GenerationStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class GenerationMapper {

    public GenerationRequest toGenerationRequestEntity(CreateGenerationRequest request, UUID generationId, UUID userId) {
        return new GenerationRequest(
                generationId,
                userId,
                request.prompt(),
                request.type(),
                GenerationStatus.QUEUED,
                null,
                null,
                null
        );
    }

    public GenerationResponse toGenerationResponse(
            GenerationRequest generation,
            GeneratedAsset asset
    ) {
        return new GenerationResponse(
                generation.id(),
                generation.type(),
                generation.prompt(),
                generation.status(),
                generation.rating(),
                generation.createdAt(),
                generation.completedAt(),
                toGeneratedAssetResponse(asset)
        );
    }

    public CreateGenerationResponse toCreateGenerationResponse(GenerationRequest generationRequest) {
        return new CreateGenerationResponse(
                generationRequest.id(),
                generationRequest.type(),
                generationRequest.status()
        );
    }

    public GeneratedAssetResponse toGeneratedAssetResponse(GeneratedAsset generatedAsset) {
        if (generatedAsset == null) {
            return null;
        }
        return new GeneratedAssetResponse(
                generatedAsset.id(),
                generatedAsset.assetType(),
                generatedAsset.contentType(),
                generatedAsset.width(),
                generatedAsset.height(),
                generatedAsset.duration(),
                generatedAsset.sizeBytes()
        );
    }

    public GenerationPageResponse toGenerationPageResponse(
            List<GenerationRequest> generationRequests,
            List<GeneratedAsset> generatedAssets,
            int page,
            int size,
            long total
    ) {
        Map<UUID, GeneratedAsset> assetsByRequestId =
                generatedAssets.stream()
                        .collect(Collectors.toMap(
                                GeneratedAsset::requestId,
                                generatedAsset -> generatedAsset
                        ));
        List<GenerationResponse> generationResponses =
                generationRequests.stream()
                        .map(generation -> toGenerationResponse(
                                generation,
                                assetsByRequestId.get(
                                        generation.id()
                                )
                        ))
                        .toList();
        return new GenerationPageResponse(
                generationResponses,
                page,
                size,
                total
        );
    }
}
