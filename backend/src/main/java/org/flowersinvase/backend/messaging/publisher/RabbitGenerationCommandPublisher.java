package org.flowersinvase.backend.messaging.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowersinvase.backend.exception.rabbit.MessageBrokerUnavailableException;
import org.flowersinvase.backend.dto.rabbit.GenerateContentCommand;
import org.flowersinvase.backend.config.rabbit.RabbitTopologyProperties;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitGenerationCommandPublisher implements GenerationCommandPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTopologyProperties properties;

    @Value("${app.outbox.confirmation-timeout:5s}")
    private Duration confirmationTimeout;

    @Override
    public void publish(GenerateContentCommand command) {
        CorrelationData correlationData = new CorrelationData(command.commandId().toString());
        try {
            rabbitTemplate.convertAndSend(
                    properties.commandsExchange(),
                    properties.generateRoutingKey(),
                    command,
                    correlationData
            );
            CorrelationData.Confirm confirm = correlationData.getFuture().get(
                    confirmationTimeout.toMillis(),
                    TimeUnit.MILLISECONDS
            );
            if (!confirm.ack()) {
                throw new IllegalStateException("RabbitMQ отклонил сообщение: " + confirm.reason());
            }
            if (correlationData.getReturned() != null) {
                throw new IllegalStateException("RabbitMQ не смог направить сообщение в очередь");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw publicationException(command, exception);
        } catch (AmqpException | ExecutionException | TimeoutException | IllegalStateException exception) {
            throw publicationException(command, exception);
        }
    }

    private MessageBrokerUnavailableException publicationException(
            GenerateContentCommand command,
            Exception exception
    ) {
        log.error("Ошибка RabbitMQ при отправке команды: generationId={}", command.generationId(), exception);
        return new MessageBrokerUnavailableException("Не удалось отправить задачу на генерацию", exception);
    }
}
