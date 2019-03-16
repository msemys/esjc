package com.github.msemys.esjc;

import com.github.msemys.esjc.proto.EventStoreClientMessages;

/**
 * Structure represents a single event or an resolved link event with retry count.
 */
public class RetryableResolvedEvent extends ResolvedEvent {

    /**
     * The number of times the event is being retried.
     */
    public final Integer retryCount;

    /**
     * Creates new instance from proto message.
     *
     * @param event      resolved indexed event.
     * @param retryCount the number of times the event is being retried.
     */
    public RetryableResolvedEvent(EventStoreClientMessages.ResolvedIndexedEvent event, Integer retryCount) {
        super(event);
        this.retryCount = retryCount;
    }

}
