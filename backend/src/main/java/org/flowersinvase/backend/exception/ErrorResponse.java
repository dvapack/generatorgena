package org.flowersinvase.backend.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ErrorResponse {
    private int status;
    private String message;
    private String path;
    private LocalDateTime timestamp;
    private List<String> errors;

    public ErrorResponse(int status, String message, String path, List<String> errors) {
        this.status = status;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now();
        this.errors = errors;
    }

    public ErrorResponse(int status, String message, String path) {
        this(status, message, path, List.of());
    }
}
