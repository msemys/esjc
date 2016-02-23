package com.github.msemys.esjc;

/**
 * The listener interface for receiving catch-up subscription action events.
 */
public interface CatchUpSubscriptionListener extends SubscriptionListener {

    /**
     * Invoked when the subscription switches from the reading phase to the live subscription phase.
     */
    default void onLiveProcessingStarted() {

    }

}
