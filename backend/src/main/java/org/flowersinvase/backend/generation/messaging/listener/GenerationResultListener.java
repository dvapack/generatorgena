package org.flowersinvase.backend.generation.messaging.listener;

import org.flowersinvase.backend.generation.messaging.dto.GenerationResultEvent;

public interface GenerationResultListener {
    void handle(GenerationResultEvent event);
}
