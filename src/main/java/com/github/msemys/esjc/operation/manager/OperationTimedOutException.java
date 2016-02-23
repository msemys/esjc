package com.github.msemys.esjc.operation.manager;

import com.github.msemys.esjc.EventStoreException;

/**
 * Exception thrown if an operation times out.
 */
public class OperationTimedOutException extends EventStoreException {

    /**
     * Creates a new instance with the specified error message.
     *
     * @param message error message.
     */
    public OperationTimedOutException(String message) {
        super(message);
    }

}
