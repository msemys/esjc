package com.github.msemys.esjc.node.cluster;

import com.github.msemys.esjc.EventStoreException;

/**
 * Exception thrown if cluster discovery fails.
 */
public class ClusterException extends EventStoreException {

    /**
     * Creates a new instance with the specified error message.
     *
     * @param message error message.
     */
    public ClusterException(String message) {
        super(message);
    }

    /**
     * Creates a new instance with the specified error message and cause.
     *
     * @param message error message.
     * @param cause   the cause.
     */
    public ClusterException(String message, Throwable cause) {
        super(message, cause);
    }
}
