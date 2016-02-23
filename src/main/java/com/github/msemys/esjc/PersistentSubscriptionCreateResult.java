package com.github.msemys.esjc;

/**
 * Result type of a single operation creating a persistent subscription in the Event Store.
 */
public class PersistentSubscriptionCreateResult {

    /**
     * Status of create attempt.
     */
    public final PersistentSubscriptionCreateStatus status;

    public PersistentSubscriptionCreateResult(PersistentSubscriptionCreateStatus status) {
        this.status = status;
    }
}
