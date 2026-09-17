package org.flowersinvase.backend.exception.generation;

import org.flowersinvase.backend.exception.common.BusinessRuntimeException;

public class InvalidGenerationStateException extends BusinessRuntimeException {
    public InvalidGenerationStateException(String message) {
        super(message);
    }
}
