package org.flowersinvase.backend.generation.exception;

public class MessageBrokerUnavailableException
        extends RuntimeException {

    public MessageBrokerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}