package com.github.msemys.esjc;

/**
 * Represents the reason subscription drop happened.
 */
public enum SubscriptionDropReason {

    /**
     * Subscription dropped because the client called close.
     */
    UserInitiated,

    /**
     * Subscription dropped because the client is not authenticated.
     */
    NotAuthenticated,

    /**
     * Subscription dropped because access to the stream was denied.
     */
    AccessDenied,

    /**
     * Subscription dropped because of an error in the subscription phase.
     */
    SubscribingError,

    /**
     * Subscription dropped because of a server error.
     */
    ServerError,

    /**
     * Subscription dropped because the connection was closed.
     */
    ConnectionClosed,

    /**
     * Subscription dropped because of an error during the catch-up phase.
     */
    CatchUpError,

    /**
     * Subscription dropped because it's queue overflowed.
     */
    ProcessingQueueOverflow,

    /**
     * Subscription dropped because an exception was thrown by a handler.
     */
    EventHandlerException,

    /**
     * The maximum number of subscribers for the persistent subscription has been reached.
     */
    MaxSubscribersReached,

    /**
     * The persistent subscription has been deleted.
     */
    PersistentSubscriptionDeleted,

    /**
     * Subscription was dropped for an unknown reason.
     */
    Unknown,

    /**
     * Target of persistent subscription was not found. Needs to be created first.
     */
    NotFound;
}
