package com.github.msemys.esjc;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * $all stream events iterator.
 */
public class AllEventsIterator extends AbstractEventsIterator {
    private final Function<Position, CompletableFuture<AllEventsSlice>> reader;
    private Position position;

    AllEventsIterator(Position position, Function<Position, CompletableFuture<AllEventsSlice>> reader) {
        this.position = position;
        this.reader = reader;
    }

    @Override
    protected void read() {
        AllEventsSlice slice = reader.apply(position).join();

        position = slice.nextPosition;
        iterator = slice.events.iterator();
        endOfStream = slice.isEndOfStream();
    }

}
