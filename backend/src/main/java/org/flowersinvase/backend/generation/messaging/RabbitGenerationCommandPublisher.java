package org.flowersinvase.backend.generation.messaging;

import lombok.RequiredArgsConstructor;
import org.flowersinvase.backend.generation.entity.GenerationRequest;
import org.flowersinvase.backend.generation.exception.MessageBrokerUnavailableException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RabbitGenerationCommandPublisher implements GenerationCommandPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTopologyProperties properties;

    @Override
    public void publish(GenerationRequest generation) {
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
