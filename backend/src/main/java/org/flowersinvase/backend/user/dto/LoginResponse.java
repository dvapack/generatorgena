package org.flowersinvase.backend.user.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}