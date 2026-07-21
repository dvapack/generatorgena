package org.flowersinvase.backend.generation.dto;

import java.util.List;

public record GenerationPageResponse(
        // TODO: разобраться с пагинацией, пока по ощущениям написано
    List<GenerationResponse> generations,
    int page,
    int size,
    long total
) {
    public GenerationPageResponse {
        generations = List.copyOf(generations);
    }
}
