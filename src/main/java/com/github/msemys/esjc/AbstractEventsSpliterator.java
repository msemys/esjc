package com.github.msemys.esjc;

import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;
import static java.util.Spliterators.emptySpliterator;

abstract class AbstractEventsSpliterator<T, R> implements Spliterator<ResolvedEvent> {
    private static final int CHARACTERISTICS = SIZED | SUBSIZED | IMMUTABLE | ORDERED | NONNULL;

    private final Function<T, CompletableFuture<R>> reader;
    private T cursor;
    private Spliterator<ResolvedEvent> spliterator;
    private boolean endOfStream;
    private long estimate = Long.MAX_VALUE;

    AbstractEventsSpliterator(T cursor, Function<T, CompletableFuture<R>> reader) {
        this.cursor = cursor;
        this.reader = reader;
    }

    @Override
    public boolean tryAdvance(Consumer<? super ResolvedEvent> action) {
        checkNotNull(action, "action is null");

        if (spliterator == null) {
            spliterator = nextBatch().spliterator();
        }

        if (spliterator.tryAdvance(action)) {
            return true;
        } else if (!endOfStream) {
            spliterator = nextBatch().spliterator();
            return spliterator.tryAdvance(action);
        } else {
            spliterator = emptySpliterator();
            return false;
        }
    }

    @Override
    public Spliterator<ResolvedEvent> trySplit() {
        if (!endOfStream) {
            if (spliterator == null) {
                return nextBatch().spliterator();
            } else {
                Spliterator<ResolvedEvent> currentSpliterator = spliterator;
                spliterator = null;
                return currentSpliterator;
            }
        } else {
            if (spliterator == null) {
                spliterator = emptySpliterator();
            }
            return null;
        }
    }

    @Override
    public long estimateSize() {
        return estimate;
    }

    @Override
    public int characteristics() {
        return CHARACTERISTICS;
    }

    private List<ResolvedEvent> nextBatch() {
        R slice = reader.apply(cursor).join();

        onBatchReceived(slice);

        List<ResolvedEvent> events = getEvents(slice);
        estimate = events.size();

        return events;
    }

    protected void onBatchReceived(R slice) {
        cursor = getNextCursor(slice);
        endOfStream = isEndOfStream(slice);
    }

    protected abstract T getNextCursor(R slice);

    protected abstract List<ResolvedEvent> getEvents(R slice);

    protected abstract boolean isEndOfStream(R slice);

}
