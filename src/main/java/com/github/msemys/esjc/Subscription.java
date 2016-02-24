package com.github.msemys.esjc;

import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;

/**
 * Subscription to a single stream or to the stream of all events in the Event Store.
 */
public abstract class Subscription implements AutoCloseable {

    /**
     * The name of the stream to which the subscription is subscribed.
     */
    public final String streamId;

    /**
     * The last commit position seen on the subscription (if this is a subscription to all events).
     */
    public final long lastCommitPosition;

    /**
     * The last event number seen on the subscription (if this is a subscription to a single stream).
     */
    public final Integer lastEventNumber;

    public Subscription(String streamId, long lastCommitPosition, Integer lastEventNumber) {
        this.streamId = streamId;
        this.lastCommitPosition = lastCommitPosition;
        this.lastEventNumber = lastEventNumber;
    }

    /**
     * Determines whether or not this subscription is to $all stream or to a specific stream.
     *
     * @return {@code true} if this subscription is to $all stream, otherwise {@code false}
     */
    public boolean isSubscribedToAll() {
        return isNullOrEmpty(streamId);
    }

    /**
     * Unsubscribes from the stream.
     */
    public abstract void unsubscribe();

    @Override
    public void close() throws Exception {
        unsubscribe();
    }
}
