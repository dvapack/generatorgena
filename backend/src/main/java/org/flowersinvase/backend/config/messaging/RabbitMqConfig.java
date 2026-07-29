package org.flowersinvase.backend.config.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableConfigurationProperties(RabbitTopologyProperties.class)
public class RabbitMqConfig {

    @Bean
    public DirectExchange generationCommandsExchange(RabbitTopologyProperties properties) {
        return new DirectExchange(
                properties.commandsExchange(),
                true,
                false
        );
    }

    @Bean
    public TopicExchange generationEventsExchange(RabbitTopologyProperties properties) {
        return new TopicExchange(
                properties.eventsExchange(),
                true,
                false
        );
    }

    @Bean
    public Queue generationRequestsQueue(RabbitTopologyProperties properties) {
        return QueueBuilder
                .durable(properties.requestsQueue())
                .build();
    }

    @Bean
    public Queue generationResultsQueue(RabbitTopologyProperties properties) {
        return QueueBuilder
                .durable(properties.resultsQueue())
                .build();
    }

    @Bean
    public Binding generationRequestsBinding(
            @Qualifier("generationRequestsQueue") Queue queue,
            @Qualifier("generationCommandsExchange") DirectExchange exchange,
            RabbitTopologyProperties properties
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(properties.generateRoutingKey());
    }

    @Bean
    public Binding generationResultsBinding(
            @Qualifier("generationResultsQueue") Queue queue,
            @Qualifier("generationEventsExchange") TopicExchange exchange,
            RabbitTopologyProperties properties
    ) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(properties.eventsRoutingPattern());
    }

    @Bean
    public MessageConverter rabbitMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(
                jsonMapper,
                "org.flowersinvase.backend.generation.messaging"
        );
    }

}
