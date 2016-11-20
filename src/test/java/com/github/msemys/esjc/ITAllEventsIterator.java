package com.github.msemys.esjc;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.junit.Assert.*;

public class ITAllEventsIterator extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void lazyReadsBatchesForward() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents();
        Position position = eventstore.appendToStream(stream, ExpectedVersion.noStream(), events.get(0)).join().logPosition;
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
        range(0, 20).forEach(i -> assertEquals(events.get(i).eventId, result.get(i).event.eventId));
    }

    @Test
    public void lazyReadsBatchesBackward() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.noStream(), newTestEvents()).join();

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
        List<ResolvedEvent> reversedFirstEvents = reverse(firstEvents.subList(0, 10));
        range(0, 10).forEach(i -> assertEquals(reversedFirstEvents.get(i).event.eventId, result.get(i).event.eventId));
    }

    private static List<EventData> newTestEvents() {
        return range(0, 20).mapToObj(i -> newTestEvent()).collect(toList());
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
