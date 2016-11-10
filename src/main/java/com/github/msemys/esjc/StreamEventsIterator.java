package com.github.msemys.esjc;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.github.msemys.esjc.util.Preconditions.checkState;

/**
 * Stream events iterator.
 */
public class StreamEventsIterator implements Iterator<ResolvedEvent> {
    private final Function<Integer, CompletableFuture<StreamEventsSlice>> reader;
    private int eventNumber;
    private Iterator<ResolvedEvent> iterator;
    private boolean endOfStream;

    StreamEventsIterator(int eventNumber, Function<Integer, CompletableFuture<StreamEventsSlice>> reader) {
        this.eventNumber = eventNumber;
        this.reader = reader;
    }

    @Override
    public boolean hasNext() {
        return iterator().hasNext();
    }

    @Override
    public ResolvedEvent next() {
        ResolvedEvent event = iterator().next();

        if (!iterator().hasNext() && !endOfStream) {
            read();
        }

        return event;
    }

    private Iterator<ResolvedEvent> iterator() {
        if (iterator == null) {
            read();
        }
        return iterator;
    }

    private void read() {
        StreamEventsSlice slice = reader.apply(eventNumber).join();

        checkState(slice.status == SliceReadStatus.Success, "Unexpected read status: %s", slice.status);

        eventNumber = slice.nextEventNumber;
        iterator = slice.events.iterator();
        endOfStream = slice.isEndOfStream;
    }

}
