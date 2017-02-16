package com.github.msemys.esjc;

/**
 * Exception thrown by ongoing operations which are terminated by connection closing.
 */
public class ConnectionClosedException extends EventStoreException {

    /**
     * Creates a new instance with the specified error message.
     *
     * @param message error message.
     */
    public ConnectionClosedException(String message) {
        super(message);
    }

    /**
     * Creates a new instance with the specified error message and cause.
     *
     * @param message error message.
     * @param cause the cause.
     */
    public ConnectionClosedException(String message, Throwable cause) {
        super(message, cause);
    }

}
