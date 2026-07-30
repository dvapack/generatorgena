package org.flowersinvase.backend.exception.common;

public class BusinessRuntimeException extends RuntimeException {
    public BusinessRuntimeException(String message) {
        super(message);
    }
}
