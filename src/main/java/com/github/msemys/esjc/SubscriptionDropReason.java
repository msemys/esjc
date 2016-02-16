package com.github.msemys.esjc;


public enum SubscriptionDropReason {
    UserInitiated,
    NotAuthenticated,
    AccessDenied,
    SubscribingError,
    ServerError,
    ConnectionClosed,
    CatchUpError,
    ProcessingQueueOverflow,
    EventHandlerException,
    MaxSubscribersReached,
    PersistentSubscriptionDeleted,
    Unknown,
    NotFound;
}
