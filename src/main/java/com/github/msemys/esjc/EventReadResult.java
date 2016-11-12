package com.github.msemys.esjc;

import com.github.msemys.esjc.proto.EventStoreClientMessages.ResolvedIndexedEvent;

import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;

/**
 * Result type of a single read operation to the Event Store that retrieves one event.
 */
public class EventReadResult {

    /**
     * Status of read attempt.
     */
    public final EventReadStatus status;

    /**
     * The name of the stream read.
     */
    public final String stream;

    /**
     * The event number of the requested event.
     */
    public final int eventNumber;

    /**
     * The event read.
     */
    public final ResolvedEvent event;

    /**
     * Creates a new instance.
     *
     * @param status      status of read attempt.
     * @param stream      the name of the stream read.
     * @param eventNumber the event number.
     * @param event       the event read.
     */
    public EventReadResult(EventReadStatus status, String stream, int eventNumber, ResolvedIndexedEvent event) {
        checkArgument(!isNullOrEmpty(stream), "stream is null or empty");
        this.status = status;
        this.stream = stream;
        this.eventNumber = eventNumber;
        this.event = (status == EventReadStatus.Success) ? new ResolvedEvent(event) : null;
    }
}
