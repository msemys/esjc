package com.github.msemys.esjc.system;

/**
 * System supported consumer strategies for use with persistent subscriptions.
 */
public class SystemConsumerStrategies {

    /**
     * Distributes events to a single client until it is full. Then round robin to the next client.
     */
    public static final String DISPATCH_TO_SINGLE = "DispatchToSingle";

    /**
     * Distribute events to each client in a round robin fashion.
     */
    public static final String ROUND_ROBIN = "RoundRobin";

    /**
     * Distribute events of the same streamId to the same client until it disconnects on a best efforts basis.
     * <p>
     * Designed to be used with indexes such as the category projection.
     * </p>
     */
    public static final String PINNED = "Pinned";

    private SystemConsumerStrategies() {
    }

}
