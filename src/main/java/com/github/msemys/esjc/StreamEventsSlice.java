package com.github.msemys.esjc;

import com.github.msemys.esjc.proto.EventStoreClientMessages.ResolvedIndexedEvent;

import java.util.ArrayList;
import java.util.List;

import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toCollection;

/**
 * Result type of a single read operation to the Event Store, that retrieves event list from stream.
 */
public class StreamEventsSlice {

    /**
     * Status of read attempt.
     */
    public final SliceReadStatus status;

    /**
     * The name of the stream read.
     */
    public final String stream;

    /**
     * The starting point (represented as a sequence number) of the read operation.
     */
    public final int fromEventNumber;

    /**
     * The direction of read request.
     */
    public final ReadDirection readDirection;

    /**
     * The events read.
     */
    public final List<ResolvedEvent> events;

    /**
     * The next event number that can be read.
     */
    public final int nextEventNumber;

    /**
     * The last event number in the stream.
     */
    public final int lastEventNumber;

    /**
     * Indicating whether or not this is the end of the stream.
     */
    public final boolean isEndOfStream;

    public StreamEventsSlice(SliceReadStatus status,
                             String stream,
                             int fromEventNumber,
                             ReadDirection readDirection,
                             List<ResolvedIndexedEvent> events,
                             int nextEventNumber,
                             int lastEventNumber,
                             boolean isEndOfStream) {
        checkArgument(!isNullOrEmpty(stream), "stream is null or empty");
        this.status = status;
        this.stream = stream;
        this.fromEventNumber = fromEventNumber;
        this.readDirection = readDirection;
        this.events = (events == null) ? emptyList() : events.stream()
            .map(ResolvedEvent::new)
            .collect(toCollection(() -> new ArrayList(events.size())));
        this.nextEventNumber = nextEventNumber;
        this.lastEventNumber = lastEventNumber;
        this.isEndOfStream = isEndOfStream;
    }
}
