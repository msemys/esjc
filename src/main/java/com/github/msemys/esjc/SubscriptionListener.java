package com.github.msemys.esjc;

/**
 * The listener interface for receiving subscription action events.
 */
public interface SubscriptionListener {

    /**
     * Invoked when a new event is received over the subscription.
     *
     * @param event event appeared.
     */
    void onEvent(ResolvedEvent event);

    /**
     * Invoked when the subscription is dropped.
     *
     * @param reason    subscription drop reason.
     * @param exception subscription drop cause (maybe {@code null})
     */
    default void onClose(SubscriptionDropReason reason, Exception exception) {

    }

}
