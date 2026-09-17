package org.flowersinvase.backend.service.outbox;

import lombok.RequiredArgsConstructor;
import org.flowersinvase.backend.dto.rabbit.GenerateContentCommand;
import org.flowersinvase.backend.entity.generation.GenerationEntity;
import org.flowersinvase.backend.entity.outbox.OutboxMessage;
import org.flowersinvase.backend.enums.GenerationStatus;
import org.flowersinvase.backend.repository.outbox.OutboxRepository;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GenerationOutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void enqueue(GenerationEntity generation) {
        GenerateContentCommand command = new GenerateContentCommand(
                UUID.ofEpochMillis(System.currentTimeMillis()),
                generation.id(),
                generation.prompt(),
                GenerationStatus.QUEUED
        );
        try {
            OffsetDateTime now = OffsetDateTime.now();
            outboxRepository.save(new OutboxMessage(
                    command.commandId(),
                    generation.id(),
                    objectMapper.writeValueAsString(command),
                    now,
                    0,
                    now
            ));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Не удалось сериализовать команду генерации", exception);
        }
    }
}
