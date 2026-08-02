package org.flowersinvase.backend.dto.user;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}