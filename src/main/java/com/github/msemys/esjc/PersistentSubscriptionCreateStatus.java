package com.github.msemys.esjc;

/**
 * Status of a single subscription create message.
 */
public enum PersistentSubscriptionCreateStatus {

    /**
     * The subscription was created successfully.
     */
    Success,

    /**
     * The subscription already exists.
     */
    NotFound,

    /**
     * Some failure happened creating the subscription.
     */
    Failure
}
