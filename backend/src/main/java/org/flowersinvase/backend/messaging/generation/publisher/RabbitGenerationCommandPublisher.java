package org.flowersinvase.backend.messaging.generation.publisher;

import lombok.RequiredArgsConstructor;
import org.flowersinvase.backend.entity.generation.Generation;
import org.flowersinvase.backend.exception.exceptions.messaging.MessageBrokerUnavailableException;
import org.flowersinvase.backend.dto.messaging.GenerateContentCommand;
import org.flowersinvase.backend.config.messaging.RabbitTopologyProperties;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RabbitGenerationCommandPublisher implements GenerationCommandPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTopologyProperties properties;

    @Override
    public void publish(Generation generation) {
        GenerateContentCommand command = new GenerateContentCommand(
                UUID.ofEpochMillis(System.currentTimeMillis()),
                generation.id(),
                generation.prompt(),
                generation.type()
        );
        try {
            rabbitTemplate.convertAndSend(
                    properties.commandsExchange(),
                    properties.generateRoutingKey(),
                    command
            );
        } catch (AmqpException e) {
            throw new MessageBrokerUnavailableException("Не удалось отправить задачу на генерацию", e);
        }
    }
}
