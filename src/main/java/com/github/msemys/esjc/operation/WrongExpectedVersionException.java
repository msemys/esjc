package com.github.msemys.esjc.operation;

import com.github.msemys.esjc.EventStoreException;

/**
 * Exception thrown if the expected version specified on an operation
 * does not match the version of the stream when the operation was attempted.
 */
public class WrongExpectedVersionException extends EventStoreException {

    /**
     * Creates a new instance with the specified error message.
     *
     * @param message error message.
     */
    public WrongExpectedVersionException(String message) {
        super(message);
    }
}
