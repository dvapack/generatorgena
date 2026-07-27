package org.flowersinvase.backend.generation.messaging;

import org.flowersinvase.backend.generation.entity.GenerationRequest;

public interface GenerationCommandPublisher {
    void publish(GenerationRequest generation);
}
