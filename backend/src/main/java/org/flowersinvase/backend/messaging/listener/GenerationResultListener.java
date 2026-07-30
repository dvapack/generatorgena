package org.flowersinvase.backend.messaging.listener;

import org.flowersinvase.backend.dto.rabbit.GenerationResultEvent;

public interface GenerationResultListener {
    void handle(GenerationResultEvent event);
}
