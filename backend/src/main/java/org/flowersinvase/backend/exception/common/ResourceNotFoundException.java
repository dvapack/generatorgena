package org.flowersinvase.backend.exception.common;

public class ResourceNotFoundException extends BusinessRuntimeException{

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
