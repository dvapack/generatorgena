package org.flowersinvase.backend.messaging.generation.listener;

import org.flowersinvase.backend.dto.messaging.GenerationResultEvent;

public interface GenerationResultListener {
    void handle(GenerationResultEvent event);
}
