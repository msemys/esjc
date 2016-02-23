package com.github.msemys.esjc;

/**
 * Status of a single subscription update message.
 */
public enum PersistentSubscriptionUpdateStatus {

    /**
     * The subscription was updated successfully.
     */
    Success,

    /**
     * The subscription already exists.
     */
    NotFound,

    /**
     * Some failure happened updating the subscription.
     */
    Failure,

    /**
     * You do not have permissions to update this subscription.
     */
    AccessDenied
}
