package com.github.msemys.esjc;

/**
 * Result type of a single operation deleting a persistent subscription in the Event Store.
 */
public class PersistentSubscriptionDeleteResult {

    /**
     * Status of delete attempt
     */
    public final PersistentSubscriptionDeleteStatus status;

    public PersistentSubscriptionDeleteResult(PersistentSubscriptionDeleteStatus status) {
        this.status = status;
    }
}
