package org.flowersinvase.backend.exception.exceptions.common;

public class ResourceNotFoundException extends RuntimeException{

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
