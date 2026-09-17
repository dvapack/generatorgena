package org.flowersinvase.backend.config.rabbit;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.rabbitmq.topology")
public record RabbitTopologyProperties(
        @NotBlank String commandsExchange,
        @NotBlank String eventsExchange,
        @NotBlank String requestsQueue,
        @NotBlank String resultsQueue,
        @NotBlank String generateRoutingKey,
        @NotBlank String eventsRoutingPattern
) {
}