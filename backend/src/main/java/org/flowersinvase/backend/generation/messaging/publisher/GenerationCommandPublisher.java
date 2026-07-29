package org.flowersinvase.backend.generation.messaging.publisher;

import org.flowersinvase.backend.generation.entity.Generation;

public interface GenerationCommandPublisher {
    void publish(Generation generation);
}
