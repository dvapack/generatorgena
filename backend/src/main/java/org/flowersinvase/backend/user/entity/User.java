package org.flowersinvase.backend.user.entity;

import java.util.UUID;

public record User(
        UUID id,
        String email,
        String passwordHash
) {
}
