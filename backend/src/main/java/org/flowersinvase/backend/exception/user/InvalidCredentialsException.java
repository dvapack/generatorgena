package org.flowersinvase.backend.exception.user;

import org.flowersinvase.backend.exception.common.BusinessRuntimeException;

public class InvalidCredentialsException extends BusinessRuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
