package com.github.msemys.esjc.subscription;

/**
 * Actions to be taken by server in the case of a client NAK.
 */
public enum PersistentSubscriptionNakEventAction {

    /**
     * Client unknown on action. Let server decide.
     */
    Unknown,

    /**
     * Park message - do not resend. Put on poison queue.
     */
    Park,

    /**
     * Explicitly retry the message.
     */
    Retry,

    /**
     * Skip this message - do not resend, do not put in poison queue.
     */
    Skip,

    /**
     * Stop the subscription.
     */
    Stop
}
