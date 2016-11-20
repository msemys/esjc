package com.github.msemys.esjc;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.Collections.emptyIterator;

abstract class AbstractEventsIterator<T, R> implements Iterator<ResolvedEvent> {
    private final Function<T, CompletableFuture<R>> reader;
    private T cursor;
    private Iterator<ResolvedEvent> iterator;
    private boolean endOfStream;

    AbstractEventsIterator(T cursor, Function<T, CompletableFuture<R>> reader) {
        this.cursor = cursor;
        this.reader = reader;
    }

    @Override
    public boolean hasNext() {
        return iterator().hasNext();
    }

    @Override
    public ResolvedEvent next() {
        ResolvedEvent event = iterator().next();

        if (!iterator.hasNext()) {
            iterator = endOfStream ? emptyIterator() : null;
        }

        return event;
    }

    private Iterator<ResolvedEvent> iterator() {
        if (iterator == null) {
            onBatchReceived(reader.apply(cursor).join());
        }
        return iterator;
    }

    protected void onBatchReceived(R slice) {
        cursor = getNextCursor(slice);
        iterator = getEvents(slice).iterator();
        endOfStream = isEndOfStream(slice);
    }

    protected abstract T getNextCursor(R slice);

    protected abstract List<ResolvedEvent> getEvents(R slice);

    protected abstract boolean isEndOfStream(R slice);

}
