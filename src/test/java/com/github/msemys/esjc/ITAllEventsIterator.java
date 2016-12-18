package com.github.msemys.esjc;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.github.msemys.esjc.matcher.RecordedEventListMatcher.containsInOrder;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ITAllEventsIterator extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void lazyReadsBatchesForward() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        Position position = eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events.get(0)).join().logPosition;
        eventstore.appendToStream(stream, ExpectedVersion.of(0), events.stream().skip(1).collect(toList())).join();

        AllEventsIteratorWithBatchCounter iterator = new AllEventsIteratorWithBatchCounter(
            position,
            p -> eventstore.readAllEventsForward(p, 3, false));

        assertEquals(0, iterator.batchCount);

        List<ResolvedEvent> result = new ArrayList<>();

        range(0, 18).forEach(i -> result.add(iterator.next()));

        assertEquals(18, result.size());
        assertEquals(6, iterator.batchCount);

        assertTrue(iterator.hasNext());
        assertEquals(7, iterator.batchCount);
        result.add(iterator.next());
        assertEquals(7, iterator.batchCount);

        assertTrue(iterator.hasNext());
        assertEquals(7, iterator.batchCount);
        result.add(iterator.next());
        assertEquals(7, iterator.batchCount);

        // should receive one more empty batch,
        // because AllEventsSlice.isEndOfStream() is true, only when no events has been received.
        assertFalse(iterator.hasNext());
        assertEquals(8, iterator.batchCount);

        range(0, 50).forEach(i -> {
            assertFalse(iterator.hasNext());
            assertEquals(8, iterator.batchCount);
        });

        assertEquals(20, result.size());
        assertThat(recordedEventsFrom(result), containsInOrder(events));
    }

    @Test
    public void lazyReadsBatchesBackward() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(20)).join();

        List<ResolvedEvent> firstEvents = eventstore.readAllEventsForward(Position.START, 11, false).join().events;

        Position position = firstEvents.get(10).originalPosition;

        AllEventsIteratorWithBatchCounter iterator = new AllEventsIteratorWithBatchCounter(
            position,
            p -> eventstore.readAllEventsBackward(p, 3, false));

        assertEquals(0, iterator.batchCount);

        List<ResolvedEvent> result = new ArrayList<>();

        range(0, 9).forEach(i -> result.add(iterator.next()));

        assertEquals(9, result.size());
        assertEquals(3, iterator.batchCount);

        assertTrue(iterator.hasNext());
        assertEquals(4, iterator.batchCount);
        result.add(iterator.next());
        assertEquals(4, iterator.batchCount);

        // should receive one more empty batch,
        // because AllEventsSlice.isEndOfStream() is true, only when no events has been received.
        assertFalse(iterator.hasNext());
        assertEquals(5, iterator.batchCount);

        range(0, 50).forEach(i -> {
            assertFalse(iterator.hasNext());
            assertEquals(5, iterator.batchCount);
        });

        assertEquals(10, result.size());
        assertThat(eventIdsFrom(result), is(eventIdsFrom(reverse(firstEvents.subList(0, 10)))));
    }

    private static class AllEventsIteratorWithBatchCounter extends AllEventsIterator {
        int batchCount;

        AllEventsIteratorWithBatchCounter(Position position, Function<Position, CompletableFuture<AllEventsSlice>> reader) {
            super(position, reader);
        }

        @Override
        protected void onBatchReceived(AllEventsSlice slice) {
            batchCount++;
            super.onBatchReceived(slice);
        }
    }

}
