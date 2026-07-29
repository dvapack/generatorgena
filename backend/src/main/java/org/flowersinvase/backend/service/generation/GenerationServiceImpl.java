package org.flowersinvase.backend.service.generation;

import lombok.RequiredArgsConstructor;
import org.flowersinvase.backend.dto.generation.*;
import org.flowersinvase.backend.exception.exceptions.common.ResourceNotFoundException;
import org.flowersinvase.backend.entity.generation.GeneratedAsset;
import org.flowersinvase.backend.entity.generation.Generation;
import org.flowersinvase.backend.entity.generation.GenerationStatus;
import org.flowersinvase.backend.exception.exceptions.generation.InvalidGenerationStateException;
import org.flowersinvase.backend.mapper.generation.GenerationMapper;
import org.flowersinvase.backend.dto.messaging.GeneratedAssetPayload;
import org.flowersinvase.backend.dto.messaging.GenerationResultEvent;
import org.flowersinvase.backend.messaging.generation.publisher.GenerationCommandPublisher;
import org.flowersinvase.backend.repository.generation.GeneratedAssetRepository;
import org.flowersinvase.backend.repository.generation.GenerationRequestRepository;
import org.flowersinvase.backend.storage.StorageService;
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

    private void markProcessing(Generation generation) {
        if (generation.status() != GenerationStatus.QUEUED) {
            return;
        }
        generationRequestRepository.updateStatus(
                generation.id(),
                GenerationStatus.PROCESSING,
                null
        );
    }

    private boolean isFinal(GenerationStatus status) {
        return status == GenerationStatus.COMPLETED
                || status == GenerationStatus.FAILED;
    }

    private void markCompleted(
            Generation generation,
            GenerationResultEvent event
    ) {
        if (isFinal(generation.status())) {
            return;
        }
        GeneratedAssetPayload payload = event.asset();
        if (payload == null) {
            throw new IllegalArgumentException("Событие COMPLETED должно содержать asset");
        }
        if (generatedAssetRepository.findByRequestId(generation.id()).isEmpty()) {
            GeneratedAsset asset = new GeneratedAsset(
                    UUID.ofEpochMillis(System.currentTimeMillis()),
                    generation.id(),
                    payload.objectKey(),
                    payload.assetType(),
                    payload.contentType(),
                    payload.sizeBytes(),
                    payload.duration(),
                    payload.width(),
                    payload.height(),
                    event.occurredAt()
            );
            generatedAssetRepository.save(asset);
        }
        generationRequestRepository.updateStatus(
                generation.id(),
                GenerationStatus.COMPLETED,
                event.occurredAt()
        );
    }

    private void markFailed(
            Generation generation,
            GenerationResultEvent event
    ) {
        if (isFinal(generation.status())) {
            return;
        }
        generationRequestRepository.updateStatus(
                generation.id(),
                GenerationStatus.FAILED,
                event.occurredAt()
        );
    }

    @Override
    @Transactional
    public void handleResult(GenerationResultEvent event) {
        Generation generation = generationRequestRepository
                .findByIdForUpdate(event.generationId())
                .orElse(null);
        if (generation == null) {
            return;
        }
        switch (event.status()) {
            case PROCESSING -> markProcessing(generation);
            case COMPLETED -> markCompleted(generation, event);
            case FAILED -> markFailed(generation, event);
            case QUEUED -> throw new IllegalArgumentException("Статус QUEUED не применим к результату генерации");
        }
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
