package org.flowersinvase.backend.dto.user;

import java.util.UUID;

public record RegisterUserResponse(
        UUID id,
        String email
) {
}