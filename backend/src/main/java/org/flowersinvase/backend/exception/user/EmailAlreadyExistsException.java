package org.flowersinvase.backend.exception.user;

import org.flowersinvase.backend.exception.common.BusinessRuntimeException;

public class EmailAlreadyExistsException extends BusinessRuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}