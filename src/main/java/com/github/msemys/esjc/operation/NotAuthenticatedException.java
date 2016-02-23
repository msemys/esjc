package com.github.msemys.esjc.operation;

import com.github.msemys.esjc.EventStoreException;

/**
 * Exception thrown if an operation requires authentication but the client is not authenticated.
 */
public class NotAuthenticatedException extends EventStoreException {

    /**
     * Creates a new instance with the specified error message.
     *
     * @param message error message.
     */
    public NotAuthenticatedException(String message) {
        super(message);
    }

}
