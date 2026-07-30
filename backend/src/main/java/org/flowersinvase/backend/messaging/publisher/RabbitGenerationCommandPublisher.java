package org.flowersinvase.backend.messaging.publisher;

import lombok.RequiredArgsConstructor;
import org.flowersinvase.backend.entity.generation.GenerationEntity;
import org.flowersinvase.backend.exception.rabbit.MessageBrokerUnavailableException;
import org.flowersinvase.backend.dto.rabbit.GenerateContentCommand;
import org.flowersinvase.backend.config.rabbit.RabbitTopologyProperties;
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
    public void publish(GenerationEntity generationEntity) {
        GenerateContentCommand command = new GenerateContentCommand(
                UUID.ofEpochMillis(System.currentTimeMillis()),
                generationEntity.id(),
                generationEntity.prompt(),
                generationEntity.type()
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
