package org.flowersinvase.backend.service.outbox;

import org.flowersinvase.backend.dto.rabbit.GenerateContentCommand;
import org.flowersinvase.backend.entity.outbox.OutboxMessage;
import org.flowersinvase.backend.enums.GenerationStatus;
import org.flowersinvase.backend.messaging.publisher.GenerationCommandPublisher;
import org.flowersinvase.backend.repository.outbox.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutboxBatchPublisherTest {

    private final OutboxRepository repository = mock(OutboxRepository.class);
    private final GenerationCommandPublisher publisher = mock(GenerationCommandPublisher.class);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final OutboxBatchPublisher batchPublisher = createBatchPublisher();

    private OutboxBatchPublisher createBatchPublisher() {
        var result = new OutboxBatchPublisher(repository, publisher, jsonMapper);
        ReflectionTestUtils.setField(result, "batchSize", 20);
        return result;
    }

    @Test
    void successfulPublicationMarksMessagePublished() throws Exception {
        OutboxMessage message = message(0);
        when(repository.findPendingForUpdate(20)).thenReturn(List.of(message));

        batchPublisher.publishBatch();

        verify(publisher).publish(any(GenerateContentCommand.class));
        verify(repository).markPublished(message.id());
        verify(repository, never()).scheduleRetry(any(), anyInt(), any(), anyString());
    }

    @Test
    void failedPublicationIsScheduledForRetry() throws Exception {
        OutboxMessage message = message(2);
        when(repository.findPendingForUpdate(20)).thenReturn(List.of(message));
        doThrow(new IllegalStateException("RabbitMQ is down"))
                .when(publisher).publish(any(GenerateContentCommand.class));

        batchPublisher.publishBatch();

        verify(repository, never()).markPublished(any());
        verify(repository).scheduleRetry(
                eq(message.id()),
                eq(3),
                any(OffsetDateTime.class),
                eq("RabbitMQ is down")
        );
    }

    private OutboxMessage message(int attempts) throws Exception {
        UUID commandId = UUID.randomUUID();
        UUID generationId = UUID.randomUUID();
        var command = new GenerateContentCommand(
                commandId,
                generationId,
                "flowers",
                GenerationStatus.QUEUED
        );
        OffsetDateTime now = OffsetDateTime.now();
        return new OutboxMessage(
                commandId,
                generationId,
                jsonMapper.writeValueAsString(command),
                now,
                attempts,
                now
        );
    }
}
