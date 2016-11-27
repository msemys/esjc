package com.github.msemys.esjc;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.github.msemys.esjc.util.Preconditions.checkState;

/**
 * Stream events spliterator.
 */
public class StreamEventsSpliterator extends AbstractEventsSpliterator<Integer, StreamEventsSlice> {

    StreamEventsSpliterator(int eventNumber, Function<Integer, CompletableFuture<StreamEventsSlice>> reader) {
        super(eventNumber, reader);
    }

    @Override
    protected void onBatchReceived(StreamEventsSlice slice) {
        checkState(slice.status == SliceReadStatus.Success, "Unexpected read status: %s", slice.status);
        super.onBatchReceived(slice);
    }

    @Override
    protected Integer getNextCursor(StreamEventsSlice slice) {
        return slice.nextEventNumber;
    }

    @Override
    protected List<ResolvedEvent> getEvents(StreamEventsSlice slice) {
        return slice.events;
    }

    @Override
    protected boolean isEndOfStream(StreamEventsSlice slice) {
        return slice.isEndOfStream;
    }

}
