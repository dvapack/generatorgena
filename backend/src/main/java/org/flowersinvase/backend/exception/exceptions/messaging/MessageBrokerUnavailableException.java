package org.flowersinvase.backend.exception.exceptions.messaging;

public class MessageBrokerUnavailableException
        extends RuntimeException {

    public MessageBrokerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}