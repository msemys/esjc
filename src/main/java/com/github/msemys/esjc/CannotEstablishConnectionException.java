package com.github.msemys.esjc;

/**
 * Exception thrown if client is unable to establish a connection to an Event Store server.
 */
public class CannotEstablishConnectionException extends EventStoreException {

    /**
     * Creates a new instance with the specified error message.
     *
     * @param message error message.
     */
    public CannotEstablishConnectionException(String message) {
        super(message);
    }

    /**
     * Creates a new instance with the specified error message and cause.
     *
     * @param message error message.
     * @param cause   the cause.
     */
    public CannotEstablishConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
