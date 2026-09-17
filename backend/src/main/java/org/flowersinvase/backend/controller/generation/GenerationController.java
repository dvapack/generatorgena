package org.flowersinvase.backend.controller.generation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.flowersinvase.backend.dto.generation.*;
import org.flowersinvase.backend.model.DownloadedAsset;
import org.flowersinvase.backend.service.generation.GenerationService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
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

    @GetMapping("/{id}/asset")
    public ResponseEntity<InputStreamResource> downloadAssetFromMinio(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID generationId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        DownloadedAsset asset = generationService.download(userId, generationId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(asset.contentType()))
                .contentLength(asset.contentLength())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(asset.filename())
                                .build()
                                .toString()
                )
                .body(new InputStreamResource(asset.content()));
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
