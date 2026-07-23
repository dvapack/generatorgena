package org.flowersinvase.backend.generation.service;

import lombok.RequiredArgsConstructor;
import org.flowersinvase.backend.common.exception.ResourceNotFoundException;
import org.flowersinvase.backend.generation.dto.*;
import org.flowersinvase.backend.generation.entity.GeneratedAsset;
import org.flowersinvase.backend.generation.entity.GenerationRequest;
import org.flowersinvase.backend.generation.entity.GenerationStatus;
import org.flowersinvase.backend.generation.exception.InvalidGenerationStateException;
import org.flowersinvase.backend.generation.mapper.GenerationMapper;
import org.flowersinvase.backend.generation.repository.GeneratedAssetRepository;
import org.flowersinvase.backend.generation.repository.GenerationRequestRepository;
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

    @Override
    @Transactional
    public CreateGenerationResponse create(UUID userId, CreateGenerationRequest request) {
        UUID generationId = UUID.ofEpochMillis(System.currentTimeMillis());
        GenerationRequest generation = generationMapper.toGenerationRequestEntity(
                request,
                generationId,
                userId
        );
        GenerationRequest savedGeneration = generationRequestRepository.save(generation);
        return generationMapper.toCreateGenerationResponse(savedGeneration);
    }

    private GenerationRequest findGenerationById(UUID userId, UUID generationId) {
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
        GenerationRequest generation = findGenerationById(userId, generationId);
        GeneratedAsset asset = findAssetByGenerationId(userId, generationId);
        return generationMapper.toGenerationResponse(generation, asset);
    }

    @Override
    @Transactional
    public void updateRating(UUID userId, UUID generationId, UpdateGenerationRatingRequest request) {
        GenerationRequest generation = findGenerationById(userId, generationId);
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
        List<GenerationRequest> requests = generationRequestRepository.findAllByUserId(userId, offset, size);
        List<UUID> requestIds = requests.stream()
                .map(GenerationRequest::id)
                .toList();
        List<GeneratedAsset> assets = generatedAssetRepository.findAllByRequestIds(requestIds);
        long total = generationRequestRepository.countByUserId(userId);
        return generationMapper.toGenerationPageResponse(requests, assets, page, size, total);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID generationId) {
        boolean deleted = generationRequestRepository.deleteByUserAndId(userId, generationId);
        if (!deleted) {
            throw new ResourceNotFoundException("Генерация с данным id не найдена");
        }
    }
}
