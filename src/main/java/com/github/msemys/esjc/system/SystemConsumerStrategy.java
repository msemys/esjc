package com.github.msemys.esjc.system;

/**
 * System supported consumer strategies for use with persistent subscriptions.
 */
public enum SystemConsumerStrategy {

    /**
     * Distributes events to a single client until it is full. Then round robin to the next client.
     */
    DISPATCH_TO_SINGLE("DispatchToSingle"),

    /**
     * Distribute events to each client in a round robin fashion.
     */
    ROUND_ROBIN("RoundRobin"),

    /**
     * Distribute events of the same streamId to the same client until it disconnects on a best efforts basis.
     * <p>
     * Designed to be used with indexes such as the category projection.
     * </p>
     */
    PINNED("Pinned");

    public final String value;

    SystemConsumerStrategy(String value) {
        this.value = value;
    }

}
