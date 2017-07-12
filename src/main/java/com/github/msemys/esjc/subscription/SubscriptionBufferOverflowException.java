package com.github.msemys.esjc.subscription;

import com.github.msemys.esjc.EventStoreException;

/**
 * Thrown when subscription reaches buffer limit.
 */
public class SubscriptionBufferOverflowException extends EventStoreException {

    /**
     * Creates a new instance with the specified error message.
     *
     * @param message error message.
     */
    public SubscriptionBufferOverflowException(String message) {
        super(message);
    }

}
