package com.github.msemys.esjc;

import com.github.msemys.esjc.proto.EventStoreClientMessages;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toCollection;

/**
 * Result type of a single read operation to the Event Store, that retrieves event list from $all stream.
 */
public class AllEventsSlice {

    /**
     * The direction of read request.
     */
    public final ReadDirection readDirection;

    /**
     * The position where this slice was read from.
     */
    public final Position fromPosition;

    /**
     * The position where the next slice should be read from.
     */
    public final Position nextPosition;

    /**
     * The events read.
     */
    public final List<ResolvedEvent> events;

    public AllEventsSlice(ReadDirection readDirection,
                          Position fromPosition,
                          Position nextPosition,
                          List<EventStoreClientMessages.ResolvedEvent> events) {
        this.readDirection = readDirection;
        this.fromPosition = fromPosition;
        this.nextPosition = nextPosition;
        this.events = (events == null) ? emptyList() : events.stream()
                .map(e -> new ResolvedEvent(e))
                .collect(toCollection(() -> new ArrayList(events.size())));
    }

    /**
     * Determines whether or not this is the end of the $all stream.
     *
     * @return {@code true} if the $all stream is ended, otherwise {@code false}
     */
    public boolean isEndOfStream() {
        return events.isEmpty();
    }

}
