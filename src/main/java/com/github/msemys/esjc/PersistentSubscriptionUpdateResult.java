package com.github.msemys.esjc;

/**
 * Result type of a single operation updating a persistent subscription in the Event Store.
 */
public class PersistentSubscriptionUpdateResult {

    /**
     * Status of update attempt.
     */
    public final PersistentSubscriptionUpdateStatus status;

    public PersistentSubscriptionUpdateResult(PersistentSubscriptionUpdateStatus status) {
        this.status = status;
    }
}
