package com.github.msemys.esjc;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * $all stream events iterator.
 */
public class AllEventsIterator implements Iterator<ResolvedEvent> {
    private final Function<Position, CompletableFuture<AllEventsSlice>> reader;
    private Position position;
    private Iterator<ResolvedEvent> iterator;
    private boolean endOfStream;

    AllEventsIterator(Position position, Function<Position, CompletableFuture<AllEventsSlice>> reader) {
        this.position = position;
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
        AllEventsSlice slice = reader.apply(position).join();

        position = slice.nextPosition;
        iterator = slice.events.iterator();
        endOfStream = slice.isEndOfStream();
    }

}
