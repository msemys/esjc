package com.github.msemys.esjc;

import com.github.msemys.esjc.operation.StreamDeletedException;
import com.github.msemys.esjc.operation.StreamNotFoundException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Stream events spliterator.
 */
public class StreamEventsSpliterator extends AbstractEventsSpliterator<Long, StreamEventsSlice> {

    StreamEventsSpliterator(long eventNumber, Function<Long, CompletableFuture<StreamEventsSlice>> reader) {
        super(eventNumber, reader);
    }

    @Override
    protected void onBatchReceived(StreamEventsSlice slice) {
        switch (slice.status) {
            case Success:
                super.onBatchReceived(slice);
                break;
            case StreamNotFound:
                throw new StreamNotFoundException(slice.stream);
            case StreamDeleted:
                throw new StreamDeletedException(slice.stream);
            default:
                throw new IllegalStateException(String.format("Unexpected read status: %s", slice.status));
        }
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
