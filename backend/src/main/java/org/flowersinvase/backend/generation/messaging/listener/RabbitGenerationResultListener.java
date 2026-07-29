package org.flowersinvase.backend.generation.messaging.listener;

import lombok.RequiredArgsConstructor;
import org.flowersinvase.backend.generation.messaging.dto.GenerationResultEvent;
import org.flowersinvase.backend.generation.service.GenerationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitGenerationResultListener implements GenerationResultListener {

    private final GenerationService generationService;

    @Override
    @RabbitListener(queues = "#{@generationResultsQueue.name}")
    public void handle(GenerationResultEvent event) {
        generationService.handleResult(event);
    }
}
