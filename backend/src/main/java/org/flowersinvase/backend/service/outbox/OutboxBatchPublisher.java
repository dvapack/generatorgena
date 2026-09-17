package org.flowersinvase.backend.service.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowersinvase.backend.dto.rabbit.GenerateContentCommand;
import org.flowersinvase.backend.entity.outbox.OutboxMessage;
import org.flowersinvase.backend.messaging.publisher.GenerationCommandPublisher;
import org.flowersinvase.backend.repository.outbox.OutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxBatchPublisher {

    private static final Duration RETRY_DELAY = Duration.ofSeconds(30);

    private final OutboxRepository outboxRepository;
    private final GenerationCommandPublisher commandPublisher;
    private final ObjectMapper objectMapper;

    @Value("${app.outbox.batch-size:20}")
    private int batchSize;

    @Transactional
    public int publishBatch() {
        var messages = outboxRepository.findPendingForUpdate(batchSize);
        for (OutboxMessage message : messages) {
            publish(message);
        }
        return messages.size();
    }

    private void publish(OutboxMessage message) {
        try {
            GenerateContentCommand command = objectMapper.readValue(message.payload(), GenerateContentCommand.class);
            commandPublisher.publish(command);
            outboxRepository.markPublished(message.id());
            log.info(
                    "Команда из outbox отправлена: commandId={}, generationId={}",
                    command.commandId(),
                    command.generationId()
            );
        } catch (Exception exception) {
            int attempts = message.attempts() + 1;
            String error = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            outboxRepository.scheduleRetry(
                    message.id(),
                    attempts,
                    OffsetDateTime.now().plus(RETRY_DELAY),
                    error
            );
            log.warn(
                    "Не удалось отправить команду из outbox, попытка будет повторена: outboxId={}, attempt={}",
                    message.id(),
                    attempts,
                    exception
            );
        }
    }

    @Transactional
    public int deletePublishedBefore(OffsetDateTime threshold) {
        return outboxRepository.deletePublishedBefore(threshold);
    }
}
