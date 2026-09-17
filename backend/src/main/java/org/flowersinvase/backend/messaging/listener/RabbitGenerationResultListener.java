package org.flowersinvase.backend.messaging.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowersinvase.backend.dto.rabbit.GenerationResultEvent;
import org.flowersinvase.backend.service.generation.GenerationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitGenerationResultListener implements GenerationResultListener {

    private final GenerationService generationService;

    @Override
    @RabbitListener(queues = "#{@generationResultsQueue.name}")
    public void handle(GenerationResultEvent event) {
        log.info(
                "Получено сообщение от rabbitmq: id={}, generationId={}, status={}",
                event.eventId(),
                event.generationId(),
                event.status()
        );
        generationService.handleResult(event);
    }
}
