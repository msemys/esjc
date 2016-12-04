package com.github.msemys.esjc;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.stream.IntStream.range;
import static org.junit.Assert.*;

public class ITStreamEventsIterator extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void lazyReadsBatchesForward() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        StreamEventsIteratorWithBatchCounter iterator = new StreamEventsIteratorWithBatchCounter(
            StreamPosition.START,
            i -> eventstore.readStreamEventsForward(stream, i, 3, false));

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

        assertFalse(iterator.hasNext());
        assertEquals(7, iterator.batchCount);

        range(0, 50).forEach(i -> {
            assertFalse(iterator.hasNext());
            assertEquals(7, iterator.batchCount);
        });

        assertEquals(20, result.size());
        range(0, 20).forEach(i -> assertEquals(events.get(i).eventId, result.get(i).event.eventId));
    }

    @Test
    public void lazyReadsBatchesBackward() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        StreamEventsIteratorWithBatchCounter iterator = new StreamEventsIteratorWithBatchCounter(
            19,
            i -> eventstore.readStreamEventsBackward(stream, i, 3, false));

        assertEquals(0, iterator.batchCount);

        List<ResolvedEvent> result = new ArrayList<>();

        range(0, 9).forEach(i -> result.add(iterator.next()));

        assertEquals(9, result.size());
        assertEquals(3, iterator.batchCount);

        assertTrue(iterator.hasNext());
        assertEquals(4, iterator.batchCount);

        range(0, 3).forEach(i -> result.add(iterator.next()));
        assertEquals(4, iterator.batchCount);

        range(0, 3).forEach(i -> result.add(iterator.next()));
        assertEquals(5, iterator.batchCount);

        range(0, 3).forEach(i -> result.add(iterator.next()));
        assertEquals(6, iterator.batchCount);

        result.add(iterator.next());
        assertEquals(7, iterator.batchCount);

        result.add(iterator.next());
        assertEquals(7, iterator.batchCount);

        assertFalse(iterator.hasNext());
        assertEquals(7, iterator.batchCount);

        range(0, 50).forEach(i -> {
            assertFalse(iterator.hasNext());
            assertEquals(7, iterator.batchCount);
        });

        assertEquals(20, result.size());
        List<EventData> reversedEvents = reverse(events);
        range(0, 20).forEach(i -> assertEquals(reversedEvents.get(i).eventId, result.get(i).event.eventId));
    }

    private static class StreamEventsIteratorWithBatchCounter extends StreamEventsIterator {
        int batchCount;

        StreamEventsIteratorWithBatchCounter(int eventNumber, Function<Integer, CompletableFuture<StreamEventsSlice>> reader) {
            super(eventNumber, reader);
        }

        @Override
        protected void onBatchReceived(StreamEventsSlice slice) {
            batchCount++;
            super.onBatchReceived(slice);
        }
    }

}
