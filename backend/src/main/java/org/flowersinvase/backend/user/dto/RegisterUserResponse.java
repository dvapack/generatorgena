package org.flowersinvase.backend.user.dto;

import java.util.UUID;

public record RegisterUserResponse(
        UUID id,
        String email
) {
}