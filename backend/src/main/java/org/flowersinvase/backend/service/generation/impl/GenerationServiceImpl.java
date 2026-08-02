package org.flowersinvase.backend.service.generation.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowersinvase.backend.dto.generation.*;
import org.flowersinvase.backend.entity.generation.GenerationEntity;
import org.flowersinvase.backend.exception.common.ResourceNotFoundException;
import org.flowersinvase.backend.entity.generation.GeneratedAsset;
import org.flowersinvase.backend.enums.GenerationStatus;
import org.flowersinvase.backend.exception.generation.InvalidGenerationStateException;
import org.flowersinvase.backend.mapper.GenerationMapper;
import org.flowersinvase.backend.dto.rabbit.GeneratedAssetPayload;
import org.flowersinvase.backend.dto.rabbit.GenerationResultEvent;
import org.flowersinvase.backend.messaging.publisher.GenerationCommandPublisher;
import org.flowersinvase.backend.model.DownloadedAsset;
import org.flowersinvase.backend.repository.generation.GeneratedAssetRepository;
import org.flowersinvase.backend.repository.generation.GenerationRequestRepository;
import org.flowersinvase.backend.service.generation.GenerationService;
import org.flowersinvase.backend.service.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
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
        GenerationEntity generationEntity = generationMapper.toGenerationEntity(request, generationId, userId);
        GenerationEntity savedGenerationEntity = generationRequestRepository.save(generationEntity);
        generationCommandPublisher.publish(savedGenerationEntity);
        log.info("Генерация создана и отправлена в очередь: userId={}, generationId={}", userId, generationId);
        return generationMapper.toCreateGenerationResponse(savedGenerationEntity);
    }

    private GenerationEntity findGenerationById(UUID userId, UUID generationId) {
        return generationRequestRepository.findByIdAndUserId(generationId, userId)
                .orElseThrow(() -> {
                    log.debug("Ошибка запроса: у userId={} отсутствует generationId={}", userId, generationId);
                    return new ResourceNotFoundException("Генерация с данным id не найдена");
                });
    }

    private GeneratedAsset findAssetByGenerationId(UUID userId, UUID generationId) {
        return generatedAssetRepository.findByUserIdAndRequestId(userId, generationId)
                .orElse(null);
    }

    @Override
    public GenerationResponse getById(UUID userId, UUID generationId) {
        GenerationEntity generationEntity = findGenerationById(userId, generationId);
        GeneratedAsset asset = findAssetByGenerationId(userId, generationId);
        return generationMapper.toGenerationResponse(generationEntity, asset);
    }

    @Override
    public void updateRating(UUID userId, UUID generationId, UpdateGenerationRatingRequest request) {
        GenerationEntity generationEntity = findGenerationById(userId, generationId);
        if (generationEntity.status() != GenerationStatus.COMPLETED) {
            log.debug(
                    "Ошибка изменения оценки: нельзя оценить незавершенную генерацию у userId={} с generationId={}",
                    userId,
                    generationId
            );
            throw new InvalidGenerationStateException("Нельзя оценить незавершённую генерацию");
        }
        boolean updated = generationRequestRepository.updateRating(generationId, userId, request.rating());
        if (!updated) {
            log.debug("Ошибка изменения оценки: у userId={} отсутствует generationId={}", userId, generationId);
            throw new ResourceNotFoundException("Генерация с данным id не найдена");
        }
        log.info(
                "Обновлена оценка генерации: generationId={}, userId={}, rating={}",
                generationId,
                userId,
                request.rating()
        );
    }

    @Override
    public GenerationPageResponse getPage(UUID userId, int page, int size) {
        int offset = page * size;
        List<GenerationEntity> requests = generationRequestRepository.findAllByUserId(userId, offset, size);
        List<UUID> requestIds = requests.stream()
                .map(GenerationEntity::id)
                .toList();
        List<GeneratedAsset> assets = generatedAssetRepository.findAllByRequestIds(requestIds);
        long total = generationRequestRepository.countByUserId(userId);
        return generationMapper.toGenerationPageResponse(requests, assets, page, size, total);
    }

    private String buildFileName(GeneratedAsset asset) {
        return "generation-" + asset.requestId() + ".png";
    }

    @Override
    public DownloadedAsset download(UUID userId, UUID generationId) {
        GeneratedAsset asset = generatedAssetRepository
                .findByUserIdAndRequestId(userId, generationId)
                .orElseThrow(() -> {
                    log.debug("Ошибка скачивания: у userId={} отсутствует generationId={}", userId, generationId);
                    return new ResourceNotFoundException("Файл генерации не найден");
                });
        var stream = storageService.get(asset.objectKey());
        log.debug("Подготовлен файл генерации для скачивания: userId={}, generationId={}", userId, generationId);
        return new DownloadedAsset(stream, asset.contentType(), asset.sizeBytes(), buildFileName(asset));
    }

    private void markProcessing(GenerationEntity generationEntity) {
        if (generationEntity.status() != GenerationStatus.QUEUED) {
            return;
        }
        generationRequestRepository.updateStatus(generationEntity.id(), GenerationStatus.PROCESSING, null);
        log.debug(
                "Установлен статус генерации generationId={} : status={}",
                generationEntity.id(),
                GenerationStatus.PROCESSING
        );
    }

    private boolean isFinal(GenerationStatus status) {
        return status == GenerationStatus.COMPLETED || status == GenerationStatus.FAILED;
    }

    private void logIgnoredEvent(GenerationEntity generationEntity, GenerationResultEvent event) {
        log.debug(
                "Событие генерации проигнорировано: generationId={}, currentStatus={}, eventId={}, eventStatus={}",
                generationEntity.id(),
                generationEntity.status(),
                event.eventId(),
                event.status()
        );
    }

    private void markCompleted(GenerationEntity generationEntity, GenerationResultEvent event) {
        if (isFinal(generationEntity.status())) {
            logIgnoredEvent(generationEntity, event);
            return;
        }
        GeneratedAssetPayload payload = event.asset();
        if (payload == null) {
            log.warn(
                    "Ошибка изменения статуса генерации: событие eventId={} с generationId={} должно иметь asset",
                    event.eventId(),
                    generationEntity.id()
            );
            throw new IllegalArgumentException("Событие COMPLETED должно содержать asset");
        }
        if (generatedAssetRepository.findByRequestId(generationEntity.id()).isEmpty()) {
            GeneratedAsset asset = new GeneratedAsset(
                    UUID.ofEpochMillis(System.currentTimeMillis()),
                    generationEntity.id(),
                    payload.objectKey(),
                    payload.contentType(),
                    payload.sizeBytes(),
                    event.occurredAt()
            );
            generatedAssetRepository.save(asset);
        }
        generationRequestRepository.updateStatus(generationEntity.id(), GenerationStatus.COMPLETED, event.occurredAt());
        log.debug(
                "Установлен статус генерации generationId={} : status={}",
                generationEntity.id(),
                GenerationStatus.COMPLETED
        );
    }

    private void markFailed(GenerationEntity generationEntity, GenerationResultEvent event) {
        if (isFinal(generationEntity.status())) {
            logIgnoredEvent(generationEntity, event);
            return;
        }
        generationRequestRepository.updateStatus(generationEntity.id(), GenerationStatus.FAILED, event.occurredAt());
        log.warn("Генерация завершилась с ошибкой: generationId={}, eventId={}", generationEntity.id(), event.eventId());
    }

    @Override
    @Transactional
    public void handleResult(GenerationResultEvent event) {
        generationRequestRepository
                .findByIdForUpdate(event.generationId())
                .ifPresentOrElse(generationEntity -> {
                            switch (event.status()) {
                                case PROCESSING -> markProcessing(generationEntity);
                                case COMPLETED -> markCompleted(generationEntity, event);
                                case FAILED -> markFailed(generationEntity, event);
                                case QUEUED -> throw new IllegalArgumentException(
                                        "Статус QUEUED не применим к результату генерации"
                                );
                            }
                        },
                        () -> log.warn(
                                "Получено событие для неизвестной генерации: eventId={}, generationId={}",
                                event.eventId(),
                                event.generationId()
                        )
                );
    }

    @Override
    public void delete(UUID userId, UUID generationId) {
        findGenerationById(userId, generationId);
        GeneratedAsset asset = findAssetByGenerationId(userId, generationId);
        if (asset != null) {
            storageService.delete(asset.objectKey());
        }
        boolean deleted = generationRequestRepository.deleteByUserAndId(userId, generationId);
        if (!deleted) {
            log.warn("Ошибка удаления: у userId={} отсутствует generationId={}", userId, generationId);
            throw new ResourceNotFoundException("Генерация с данным id не найдена");
        }
        log.info("У пользователя userId={} удалена генерация с generationId={}", userId, generationId);
    }
}
