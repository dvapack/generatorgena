package org.flowersinvase.backend.common.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ErrorResponse {
    private int code;
    private String message;
    private String path;
    private LocalDateTime timestamp;
    private List<String> errors;

    public ErrorResponse(int code, String message, String path, List<String> errors) {
        this.code = code;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now();
        this.errors = errors;
    }

    public ErrorResponse(int code, String message, String path) {
        this(code, message, path, List.of());
    }
}
