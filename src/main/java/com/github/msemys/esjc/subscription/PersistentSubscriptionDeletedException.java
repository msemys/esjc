package com.github.msemys.esjc.subscription;

import com.github.msemys.esjc.EventStoreException;

/**
 * Thrown when the persistent subscription has been deleted to subscribers connected to it.
 */
public class PersistentSubscriptionDeletedException extends EventStoreException {

    /**
     * Creates a new instance.
     */
    public PersistentSubscriptionDeletedException() {
        super("The subscription has been deleted.");
    }
}
