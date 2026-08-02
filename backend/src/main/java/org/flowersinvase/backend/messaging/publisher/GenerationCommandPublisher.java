package org.flowersinvase.backend.messaging.publisher;

import org.flowersinvase.backend.entity.generation.GenerationEntity;

public interface GenerationCommandPublisher {
    void publish(GenerationEntity generationEntity);
}
