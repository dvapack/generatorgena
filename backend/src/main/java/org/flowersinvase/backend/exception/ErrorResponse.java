package org.flowersinvase.backend.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
    int status,
    String message,
    String path,
    LocalDateTime timestamp,
    List<String> errors
) {
}
