package org.flowersinvase.backend.generation.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.flowersinvase.backend.generation.dto.*;
import org.flowersinvase.backend.generation.service.GenerationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/generations")
public class GenerationController {

    private final GenerationService generationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateGenerationResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateGenerationRequest createGenerationRequest
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return generationService.create(userId, createGenerationRequest);
    }

    @GetMapping
    public GenerationPageResponse getAll(
            @AuthenticationPrincipal Jwt jwt,
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Min(1) @Max(100) @RequestParam(defaultValue = "20") int size
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return generationService.getPage(userId, page, size);
    }

    @GetMapping("/{id}")
    public GenerationResponse getById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID generationId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return generationService.getById(userId, generationId);
    }

    @PutMapping("/{id}/rating")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRating(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID generationId,
            @Valid @RequestBody UpdateGenerationRatingRequest updateGenerationRatingRequest
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        generationService.updateRating(userId, generationId, updateGenerationRatingRequest);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID generationId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        generationService.delete(userId, generationId);
    }
}
