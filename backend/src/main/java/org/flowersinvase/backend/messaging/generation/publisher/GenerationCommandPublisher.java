package org.flowersinvase.backend.messaging.generation.publisher;

import org.flowersinvase.backend.entity.generation.Generation;

public interface GenerationCommandPublisher {
    void publish(Generation generation);
}
