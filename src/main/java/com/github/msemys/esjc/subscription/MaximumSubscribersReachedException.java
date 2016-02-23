package com.github.msemys.esjc.subscription;

import com.github.msemys.esjc.EventStoreException;

/**
 * Thrown when maximum subscribers is set on subscription and it has been reached.
 */
public class MaximumSubscribersReachedException extends EventStoreException {

    /**
     * Creates a new instance.
     */
    public MaximumSubscribersReachedException() {
        super("Maximum subscriptions reached.");
    }
}
