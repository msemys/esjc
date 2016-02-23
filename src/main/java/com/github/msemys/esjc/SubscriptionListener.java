package com.github.msemys.esjc;

/**
 * The listener interface for receiving subscription action events.
 *
 * @param <T> subscription type.
 */
public interface SubscriptionListener<T> {

    /**
     * Invoked when a new event is received over the subscription.
     *
     * @param subscription target subscription.
     * @param event        event appeared.
     */
    void onEvent(T subscription, ResolvedEvent event);

    /**
     * Invoked when the subscription is dropped.
     *
     * @param subscription target subscription.
     * @param reason       subscription drop reason.
     * @param exception    subscription drop cause (maybe {@code null})
     */
    default void onClose(T subscription, SubscriptionDropReason reason, Exception exception) {

    }

}
