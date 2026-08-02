package org.flowersinvase.backend.mapper;

import jakarta.validation.constraints.NotNull;
import org.flowersinvase.backend.dto.generation.*;
import org.flowersinvase.backend.entity.generation.GeneratedAsset;
import org.flowersinvase.backend.entity.generation.GenerationEntity;
import org.flowersinvase.backend.enums.GenerationStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GenerationMapper {

    public GenerationEntity toGenerationEntity(CreateGenerationRequest request, UUID generationId, UUID userId) {
        return new GenerationEntity(
                generationId,
                userId,
                request.prompt(),
                GenerationStatus.QUEUED,
                null,
                null,
                null
        );
    }

    public GenerationResponse toGenerationResponse(@NotNull GenerationEntity generationEntity, GeneratedAsset asset) {
        return new GenerationResponse(
                generationEntity.id(),
                generationEntity.prompt(),
                generationEntity.status(),
                generationEntity.rating(),
                generationEntity.createdAt(),
                generationEntity.completedAt(),
                toGeneratedAssetResponse(asset)
        );
    }

    public CreateGenerationResponse toCreateGenerationResponse(GenerationEntity generationEntity) {
        return new CreateGenerationResponse(
                generationEntity.id(),
                generationEntity.status()
        );
    }

    public GeneratedAssetResponse toGeneratedAssetResponse(GeneratedAsset generatedAsset) {
        if (generatedAsset == null) {
            return null;
        }
        return new GeneratedAssetResponse(
                generatedAsset.id(),
                generatedAsset.contentType(),
                generatedAsset.sizeBytes()
        );
    }

    public GenerationPageResponse toGenerationPageResponse(
            List<GenerationEntity> generationEntities,
            List<GeneratedAsset> generatedAssets,
            int page,
            int size,
            long total
    ) {
        Map<UUID, GeneratedAsset> assetsByRequestId =
                generatedAssets.stream()
                        .collect(Collectors.toMap(
                                GeneratedAsset::requestId,
                                Function.identity()
                        ));
        List<GenerationResponse> generationResponses =
                generationEntities.stream()
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
