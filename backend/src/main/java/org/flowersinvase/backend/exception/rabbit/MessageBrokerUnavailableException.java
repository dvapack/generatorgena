package org.flowersinvase.backend.exception.rabbit;

public class MessageBrokerUnavailableException
        extends RuntimeException {

    public MessageBrokerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}