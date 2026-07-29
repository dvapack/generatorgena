package org.flowersinvase.backend.mapper.generation;

import jakarta.validation.constraints.NotNull;
import org.flowersinvase.backend.dto.generation.*;
import org.flowersinvase.backend.entity.generation.GeneratedAsset;
import org.flowersinvase.backend.entity.generation.Generation;
import org.flowersinvase.backend.entity.generation.GenerationStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class GenerationMapper {

    public Generation toGenerationRequestEntity(CreateGenerationRequest request, UUID generationId, UUID userId) {
        return new Generation(
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
            @NotNull Generation generation,
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

    public CreateGenerationResponse toCreateGenerationResponse(Generation generation) {
        return new CreateGenerationResponse(
                generation.id(),
                generation.type(),
                generation.status()
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
            List<Generation> generations,
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
                generations.stream()
                        .map(generation -> toGenerationResponse(
                                generation,
                                assetsByRequestId.get(
                                        generation.id()
                                )
                        ))
                        .toList();
        return new GenerationPageResponse(generationResponses, page, size, total);
    }
}
