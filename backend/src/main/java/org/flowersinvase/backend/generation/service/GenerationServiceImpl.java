package org.flowersinvase.backend.generation.service;

import lombok.RequiredArgsConstructor;
import org.flowersinvase.backend.exception.exceptions.common.ResourceNotFoundException;
import org.flowersinvase.backend.generation.dto.*;
import org.flowersinvase.backend.generation.entity.GeneratedAsset;
import org.flowersinvase.backend.generation.entity.Generation;
import org.flowersinvase.backend.generation.entity.GenerationStatus;
import org.flowersinvase.backend.exception.exceptions.generation.InvalidGenerationStateException;
import org.flowersinvase.backend.generation.mapper.GenerationMapper;
import org.flowersinvase.backend.generation.messaging.publisher.GenerationCommandPublisher;
import org.flowersinvase.backend.generation.repository.GeneratedAssetRepository;
import org.flowersinvase.backend.generation.repository.GenerationRequestRepository;
import org.flowersinvase.backend.generation.storage.service.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GenerationServiceImpl implements GenerationService {

    private final GeneratedAssetRepository generatedAssetRepository;
    private final GenerationRequestRepository generationRequestRepository;
    private final GenerationMapper generationMapper;
    private final GenerationCommandPublisher generationCommandPublisher;
    private final StorageService storageService;

    @Override
    @Transactional
    public CreateGenerationResponse create(UUID userId, CreateGenerationRequest request) {
        UUID generationId = UUID.ofEpochMillis(System.currentTimeMillis());
        Generation generation = generationMapper.toGenerationRequestEntity(
                request,
                generationId,
                userId
        );
        Generation savedGeneration = generationRequestRepository.save(generation);
        generationCommandPublisher.publish(savedGeneration);
        return generationMapper.toCreateGenerationResponse(savedGeneration);
    }

    private Generation findGenerationById(UUID userId, UUID generationId) {
        return generationRequestRepository.findByIdAndUserId(generationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Генерация с данным id не найдена"));
    }

    private GeneratedAsset findAssetByGenerationId(UUID userId, UUID generationId) {
        return generatedAssetRepository.findByUserIdAndRequestId(userId, generationId)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public GenerationResponse getById(UUID userId, UUID generationId) {
        Generation generation = findGenerationById(userId, generationId);
        GeneratedAsset asset = findAssetByGenerationId(userId, generationId);
        return generationMapper.toGenerationResponse(generation, asset);
    }

    @Override
    @Transactional
    public void updateRating(UUID userId, UUID generationId, UpdateGenerationRatingRequest request) {
        Generation generation = findGenerationById(userId, generationId);
        if (generation.status() != GenerationStatus.COMPLETED) {
            throw new InvalidGenerationStateException("Нельзя оценить незавершённую генерацию");
        }
        boolean updated = generationRequestRepository.updateRating(generationId, userId, request.rating());
        if (!updated) {
            throw new ResourceNotFoundException("Генерация с данным id не найдена");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GenerationPageResponse getPage(UUID userId, int page, int size) {
        int offset = Math.multiplyExact(page, size);
        List<Generation> requests = generationRequestRepository.findAllByUserId(userId, offset, size);
        List<UUID> requestIds = requests.stream()
                .map(Generation::id)
                .toList();
        List<GeneratedAsset> assets = generatedAssetRepository.findAllByRequestIds(requestIds);
        long total = generationRequestRepository.countByUserId(userId);
        return generationMapper.toGenerationPageResponse(requests, assets, page, size, total);
    }

    @Override
    public DownloadedAsset download(UUID userId, UUID generationId) {
        GeneratedAsset asset = generatedAssetRepository
                .findByUserIdAndRequestId(userId, generationId)
                .orElseThrow(() -> new ResourceNotFoundException("Файл генерации не найден"));
        var stream = storageService.get(asset.objectKey());
        String filename = "generation-" + asset.requestId() + ".png";
        return new DownloadedAsset(
                stream,
                asset.contentType(),
                asset.sizeBytes(),
                filename);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID generationId) {
        findGenerationById(userId, generationId);
        GeneratedAsset asset = findAssetByGenerationId(userId, generationId);
        if (asset != null) {
            storageService.delete(asset.objectKey());
        }
        boolean deleted = generationRequestRepository.deleteByUserAndId(userId, generationId);
        if (!deleted) {
            throw new ResourceNotFoundException("Генерация с данным id не найдена");
        }
    }
}
