package com.github.msemys.esjc;

/**
 * Status of a single subscription delete message.
 */
public enum PersistentSubscriptionDeleteStatus {

    /**
     * The subscription was deleted successfully
     */
    Success,

    /**
     * Some failure happened deleting the subscription
     */
    Failure
}
