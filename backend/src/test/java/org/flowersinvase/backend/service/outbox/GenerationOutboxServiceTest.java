package org.flowersinvase.backend.service.outbox;

import org.flowersinvase.backend.entity.generation.GenerationEntity;
import org.flowersinvase.backend.entity.outbox.OutboxMessage;
import org.flowersinvase.backend.enums.GenerationStatus;
import org.flowersinvase.backend.repository.outbox.OutboxRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GenerationOutboxServiceTest {

    @Test
    void enqueueStoresStableCommandPayload() {
        OutboxRepository repository = mock(OutboxRepository.class);
        var service = new GenerationOutboxService(repository, JsonMapper.builder().build());
        UUID generationId = UUID.randomUUID();
        var generation = new GenerationEntity(
                generationId,
                UUID.randomUUID(),
                "flowers in a vase",
                GenerationStatus.QUEUED,
                null,
                null,
                null
        );

        service.enqueue(generation);

        verify(repository).save(any(OutboxMessage.class));
        var captor = org.mockito.ArgumentCaptor.forClass(OutboxMessage.class);
        verify(repository).save(captor.capture());
        OutboxMessage saved = captor.getValue();
        assertThat(saved.aggregateId()).isEqualTo(generationId);
        assertThat(saved.id()).isNotNull();
        assertThat(saved.payload()).contains(generationId.toString());
        assertThat(saved.payload()).contains("flowers in a vase");
        assertThat(saved.attempts()).isZero();
    }
}

