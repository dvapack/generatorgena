package org.flowersinvase.backend.messaging.publisher;

import org.flowersinvase.backend.dto.rabbit.GenerateContentCommand;

public interface GenerationCommandPublisher {
    void publish(GenerateContentCommand command);
}
