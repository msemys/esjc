package com.github.msemys.esjc;

/**
 * {@code EventStoreException} is the superclass of exceptions that is thrown by a client.
 */
public class EventStoreException extends RuntimeException {

    /**
     * Creates a new instance.
     */
    public EventStoreException() {
    }

    /**
     * Creates a new instance with the specified error message.
     *
     * @param message error message.
     */
    public EventStoreException(String message) {
        super(message);
    }

    /**
     * Creates a new instance with the specified error message and cause.
     *
     * @param message error message.
     * @param cause   the cause.
     */
    public EventStoreException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new instance with the specified cause.
     *
     * @param cause the cause.
     */
    public EventStoreException(Throwable cause) {
        super(cause);
    }

}
