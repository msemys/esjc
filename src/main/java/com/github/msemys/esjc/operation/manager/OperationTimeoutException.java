package com.github.msemys.esjc.operation.manager;

import com.github.msemys.esjc.EventStoreException;

/**
 * Exception thrown if an operation times out.
 */
public class OperationTimeoutException extends EventStoreException {

    /**
     * Creates a new instance with the specified error message.
     *
     * @param message error message.
     */
    public OperationTimeoutException(String message) {
        super(message);
    }

}
