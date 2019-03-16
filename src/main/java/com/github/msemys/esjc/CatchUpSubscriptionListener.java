package com.github.msemys.esjc;

/**
 * The listener interface for receiving catch-up subscription action events.
 */
public interface CatchUpSubscriptionListener extends SubscriptionListener<CatchUpSubscription, ResolvedEvent> {

    /**
     * Invoked when the subscription switches from the reading phase to the live subscription phase.
     *
     * @param subscription target subscription.
     */
    default void onLiveProcessingStarted(CatchUpSubscription subscription) {

    }

}
