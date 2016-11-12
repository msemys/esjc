package com.github.msemys.esjc;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.github.msemys.esjc.util.Preconditions.checkState;

/**
 * Stream events iterator.
 */
public class StreamEventsIterator extends AbstractEventsIterator {
    private final Function<Integer, CompletableFuture<StreamEventsSlice>> reader;
    private int eventNumber;

    StreamEventsIterator(int eventNumber, Function<Integer, CompletableFuture<StreamEventsSlice>> reader) {
        this.eventNumber = eventNumber;
        this.reader = reader;
    }

    @Override
    protected void read() {
        StreamEventsSlice slice = reader.apply(eventNumber).join();

        checkState(slice.status == SliceReadStatus.Success, "Unexpected read status: %s", slice.status);

        eventNumber = slice.nextEventNumber;
        iterator = slice.events.iterator();
        endOfStream = slice.isEndOfStream;
    }

}
