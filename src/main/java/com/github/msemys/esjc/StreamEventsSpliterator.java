package com.github.msemys.esjc;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.github.msemys.esjc.util.Preconditions.checkState;

/**
 * Stream events spliterator.
 */
public class StreamEventsSpliterator extends AbstractEventsSpliterator<Long, StreamEventsSlice> {

    StreamEventsSpliterator(long eventNumber, Function<Long, CompletableFuture<StreamEventsSlice>> reader) {
        super(eventNumber, reader);
    }

    @Override
    protected void onBatchReceived(StreamEventsSlice slice) {
        checkState(slice.status == SliceReadStatus.Success, "Unexpected read status: %s", slice.status);
        super.onBatchReceived(slice);
    }

    @Override
    protected Long getNextCursor(StreamEventsSlice slice) {
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
