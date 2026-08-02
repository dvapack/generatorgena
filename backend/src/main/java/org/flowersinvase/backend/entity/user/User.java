package org.flowersinvase.backend.entity.user;

import java.util.UUID;

public record User(
        UUID id,
        String email,
        String passwordHash
) {
}
