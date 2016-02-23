package com.github.msemys.esjc.operation;

import com.github.msemys.esjc.EventStoreException;

/**
 * Exception thrown if a server-side error occurs during an operation.
 */
public class ServerErrorException extends EventStoreException {

    /**
     * Creates a new instance with the specified error message.
     *
     * @param message error message.
     */
    public ServerErrorException(String message) {
        super(message);
    }

}
