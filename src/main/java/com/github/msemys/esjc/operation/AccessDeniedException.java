package com.github.msemys.esjc.operation;

import com.github.msemys.esjc.EventStoreException;

/**
 * Exception thrown when a user is not authorised to carry out an operation.
 */
public class AccessDeniedException extends EventStoreException {

    /**
     * Creates a new instance with the specified error message.
     *
     * @param message error message.
     */
    public AccessDeniedException(String message) {
        super(message);
    }

}
