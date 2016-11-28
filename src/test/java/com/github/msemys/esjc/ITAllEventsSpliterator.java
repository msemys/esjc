package com.github.msemys.esjc;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class ITAllEventsSpliterator extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void failsWhenActionIsNull() {
        Spliterator<ResolvedEvent> spliterator = new AllEventsSpliterator(Position.START, p -> eventstore.readAllEventsForward(p, 1, false));

        try {
            spliterator.tryAdvance(null);
            fail("should fail with 'NullPointerException'");
        } catch (Exception e) {
            assertThat(e, instanceOf(NullPointerException.class));
        }

        try {
            spliterator.forEachRemaining(null);
            fail("should fail with 'NullPointerException'");
        } catch (Exception e) {
            assertThat(e, instanceOf(NullPointerException.class));
        }
    }

    @Test
    public void iteratesAllRemainingEventsForwardWithoutTryAdvanceCall() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(19);
        Position position = appendEvents(stream, events);

        Spliterator<ResolvedEvent> spliterator = new AllEventsSpliterator(position, p -> eventstore.readAllEventsForward(p, 17, false));

        List<ResolvedEvent> result = new ArrayList<>();

        spliterator.forEachRemaining(result::add);

        assertFalse(spliterator.tryAdvance(result::add));
        spliterator.forEachRemaining(e -> fail("Should be no more elements remaining"));

        assertNull(spliterator.trySplit());

        assertEquals(19, result.size());
        range(0, 19).forEach(i -> assertEquals(events.get(i).eventId, result.get(i).event.eventId));
    }

    @Test
    public void iteratesAllRemainingEventsForward() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(82);
        Position position = appendEvents(stream, events);

        Spliterator<ResolvedEvent> spliterator = new AllEventsSpliterator(position, p -> eventstore.readAllEventsForward(p, 17, false));

        List<ResolvedEvent> result = new ArrayList<>();

        assertTrue(spliterator.tryAdvance(result::add));
        spliterator.forEachRemaining(result::add);

        assertFalse(spliterator.tryAdvance(result::add));
        spliterator.forEachRemaining(e -> fail("Should be no more elements remaining"));

        assertNull(spliterator.trySplit());

        assertEquals(82, result.size());
        range(0, 82).forEach(i -> assertEquals(events.get(i).eventId, result.get(i).event.eventId));
    }

    @Test
    public void iteratesAllRemainingEventsBackward() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(31)).join();
        List<ResolvedEvent> firstEvents = eventstore.readAllEventsForward(Position.START, 31, false).join().events;
        Position position = firstEvents.get(30).originalPosition;

        Spliterator<ResolvedEvent> spliterator = new AllEventsSpliterator(position, p -> eventstore.readAllEventsBackward(p, 11, false));

        List<ResolvedEvent> result = new ArrayList<>();

        assertTrue(spliterator.tryAdvance(result::add));
        spliterator.forEachRemaining(result::add);

        assertFalse(spliterator.tryAdvance(result::add));
        spliterator.forEachRemaining(e -> fail("Should be no more elements remaining"));

        assertNull(spliterator.trySplit());

        assertEquals(30, result.size());
        List<ResolvedEvent> reversedFirstEvents = reverse(firstEvents.subList(0, 30));
        range(0, 30).forEach(i -> assertEquals(reversedFirstEvents.get(i).event.eventId, result.get(i).event.eventId));
    }

    @Test
    public void iteratesAndSplitsEventsForward() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(17);
        Position position = appendEvents(stream, events);

        Spliterator<ResolvedEvent> spliterator = new AllEventsSpliterator(position, p -> eventstore.readAllEventsForward(p, 5, false));

        List<ResolvedEvent> result = new ArrayList<>();
        Spliterator<ResolvedEvent> s1, s2, s3;

        assertTrue(spliterator.tryAdvance(result::add));

        s1 = spliterator.trySplit();
        assertNotNull(s1);
        assertTrue(s1.tryAdvance(result::add));

        s2 = s1.trySplit();
        assertNotNull(s2);
        assertTrue(s2.tryAdvance(result::add));
        s2.forEachRemaining(result::add);

        assertTrue(s1.tryAdvance(result::add));
        s1.forEachRemaining(result::add);

        assertTrue(spliterator.tryAdvance(result::add));

        s3 = spliterator.trySplit();
        assertNotNull(s3);
        assertTrue(s3.tryAdvance(result::add));
        s3.forEachRemaining(result::add);

        assertTrue(spliterator.tryAdvance(result::add));
        spliterator.forEachRemaining(result::add);

        assertNull(spliterator.trySplit());

        assertEquals(17, result.size());
        range(0, 17).forEach(i -> assertEquals(events.get(i).eventId, result.get(i).event.eventId));
    }

    @Test
    public void iteratesAndSplitsEventsBackward() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(41)).join();
        List<ResolvedEvent> firstEvents = eventstore.readAllEventsForward(Position.START, 41, false).join().events;
        Position position = firstEvents.get(40).originalPosition;

        Spliterator<ResolvedEvent> spliterator = new AllEventsSpliterator(position, p -> eventstore.readAllEventsBackward(p, 8, false));

        List<ResolvedEvent> result = new ArrayList<>();
        Spliterator<ResolvedEvent> s1, s2, s3;

        assertTrue(spliterator.tryAdvance(result::add));

        s1 = spliterator.trySplit();
        assertNotNull(s1);
        assertTrue(s1.tryAdvance(result::add));

        s2 = s1.trySplit();
        assertNotNull(s2);
        assertTrue(s2.tryAdvance(result::add));
        s2.forEachRemaining(result::add);

        assertTrue(s1.tryAdvance(result::add));
        s1.forEachRemaining(result::add);

        assertTrue(spliterator.tryAdvance(result::add));

        s3 = spliterator.trySplit();
        assertNotNull(s3);
        assertTrue(s3.tryAdvance(result::add));
        s3.forEachRemaining(result::add);

        assertTrue(spliterator.tryAdvance(result::add));
        spliterator.forEachRemaining(result::add);

        assertNull(spliterator.trySplit());

        assertEquals(40, result.size());
        List<ResolvedEvent> reversedFirstEvents = reverse(firstEvents.subList(0, 40));
        range(0, 40).forEach(i -> assertEquals(reversedFirstEvents.get(i).event.eventId, result.get(i).event.eventId));
    }

    @Test
    public void splitsAndIteratesEventsForward() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(17);
        Position position = appendEvents(stream, events);

        Spliterator<ResolvedEvent> spliterator = new AllEventsSpliterator(position, p -> eventstore.readAllEventsForward(p, 5, false));

        List<ResolvedEvent> result = new ArrayList<>();
        Spliterator<ResolvedEvent> s1;

        s1 = spliterator.trySplit();
        assertNotNull(s1);
        s1.forEachRemaining(result::add);

        spliterator.forEachRemaining(result::add);

        assertNull(spliterator.trySplit());

        assertEquals(17, result.size());
        range(0, 17).forEach(i -> assertEquals(events.get(i).eventId, result.get(i).event.eventId));
    }

    @Test
    public void splitsAndIteratesEventsBackward() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(41)).join();
        List<ResolvedEvent> firstEvents = eventstore.readAllEventsForward(Position.START, 41, false).join().events;
        Position position = firstEvents.get(40).originalPosition;

        Spliterator<ResolvedEvent> spliterator = new AllEventsSpliterator(position, p -> eventstore.readAllEventsBackward(p, 8, false));

        List<ResolvedEvent> result = new ArrayList<>();
        Spliterator<ResolvedEvent> s1;

        s1 = spliterator.trySplit();
        assertNotNull(s1);
        s1.forEachRemaining(result::add);

        spliterator.forEachRemaining(result::add);

        assertNull(spliterator.trySplit());

        assertEquals(40, result.size());
        List<ResolvedEvent> reversedFirstEvents = reverse(firstEvents.subList(0, 40));
        range(0, 40).forEach(i -> assertEquals(reversedFirstEvents.get(i).event.eventId, result.get(i).event.eventId));
    }

    @Test
    public void splitsEventsForward() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(17);
        Position position = appendEvents(stream, events);

        Spliterator<ResolvedEvent> spliterator = new AllEventsSpliterator(position, p -> eventstore.readAllEventsForward(p, 5, false));

        List<ResolvedEvent> result = new ArrayList<>();

        range(0, 4).forEach(i -> {
            Spliterator<ResolvedEvent> s = spliterator.trySplit();
            assertNotNull(s);
            s.forEachRemaining(result::add);
        });

        assertEquals(17, result.size());

        // should receive one more empty batch,
        // because AllEventsSlice.isEndOfStream() is true, only when no events has been received.
        Spliterator<ResolvedEvent> s = spliterator.trySplit();
        assertNotNull(s);
        assertFalse(s.tryAdvance(result::add));
        s.forEachRemaining(result::add);

        assertNull(spliterator.trySplit());
        spliterator.forEachRemaining(e -> fail("Should be no more elements remaining"));
        assertFalse(spliterator.tryAdvance(result::add));

        assertEquals(17, result.size());
        range(0, 17).forEach(i -> assertEquals(events.get(i).eventId, result.get(i).event.eventId));
    }

    @Test
    public void splitsEventsBackward() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(41)).join();
        List<ResolvedEvent> firstEvents = eventstore.readAllEventsForward(Position.START, 41, false).join().events;
        Position position = firstEvents.get(40).originalPosition;

        Spliterator<ResolvedEvent> spliterator = new AllEventsSpliterator(position, p -> eventstore.readAllEventsBackward(p, 8, false));

        List<ResolvedEvent> result = new ArrayList<>();

        range(0, 5).forEach(i -> {
            Spliterator<ResolvedEvent> s = spliterator.trySplit();
            assertNotNull(s);
            s.forEachRemaining(result::add);
        });

        assertEquals(40, result.size());

        // should receive one more empty batch,
        // because AllEventsSlice.isEndOfStream() is true, only when no events has been received.
        Spliterator<ResolvedEvent> s = spliterator.trySplit();
        assertNotNull(s);
        assertFalse(s.tryAdvance(result::add));
        s.forEachRemaining(result::add);

        assertNull(spliterator.trySplit());
        spliterator.forEachRemaining(e -> fail("Should be no more elements remaining"));
        assertFalse(spliterator.tryAdvance(result::add));

        assertEquals(40, result.size());
        List<ResolvedEvent> reversedFirstEvents = reverse(firstEvents.subList(0, 40));
        range(0, 40).forEach(i -> assertEquals(reversedFirstEvents.get(i).event.eventId, result.get(i).event.eventId));
    }

    @Test
    public void lazyReadsBatchesForward() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        Position position = appendEvents(stream, events);

        AllEventsSpliteratorWithBatchCounter spliterator = new AllEventsSpliteratorWithBatchCounter(
            position,
            p -> eventstore.readAllEventsForward(p, 3, false));

        assertEquals(0, spliterator.batchCount);

        List<ResolvedEvent> result = new ArrayList<>();

        range(0, 18).forEach(i -> assertTrue(spliterator.tryAdvance(result::add)));

        assertEquals(18, result.size());
        assertEquals(6, spliterator.batchCount);

        assertTrue(spliterator.tryAdvance(result::add));
        assertEquals(7, spliterator.batchCount);

        assertTrue(spliterator.tryAdvance(result::add));
        assertEquals(7, spliterator.batchCount);

        assertEquals(20, result.size());

        // should receive one more empty batch,
        // because AllEventsSlice.isEndOfStream() is true, only when no events has been received.
        assertFalse(spliterator.tryAdvance(result::add));
        assertEquals(8, spliterator.batchCount);

        range(0, 50).forEach(i -> {
            assertFalse(spliterator.tryAdvance(result::add));
            assertEquals(8, spliterator.batchCount);
        });

        assertEquals(20, result.size());
        range(0, 20).forEach(i -> assertEquals(events.get(i).eventId, result.get(i).event.eventId));
    }

    @Test
    public void lazyReadsBatchesBackward() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(20)).join();
        List<ResolvedEvent> firstEvents = eventstore.readAllEventsForward(Position.START, 11, false).join().events;
        Position position = firstEvents.get(10).originalPosition;

        AllEventsSpliteratorWithBatchCounter spliterator = new AllEventsSpliteratorWithBatchCounter(
            position,
            p -> eventstore.readAllEventsBackward(p, 3, false));

        assertEquals(0, spliterator.batchCount);

        List<ResolvedEvent> result = new ArrayList<>();

        range(0, 9).forEach(i -> assertTrue(spliterator.tryAdvance(result::add)));

        assertEquals(9, result.size());
        assertEquals(3, spliterator.batchCount);

        assertTrue(spliterator.tryAdvance(result::add));
        assertEquals(4, spliterator.batchCount);

        assertEquals(10, result.size());

        // should receive one more empty batch,
        // because AllEventsSlice.isEndOfStream() is true, only when no events has been received.
        assertFalse(spliterator.tryAdvance(result::add));
        assertEquals(5, spliterator.batchCount);

        range(0, 50).forEach(i -> {
            assertFalse(spliterator.tryAdvance(result::add));
            assertEquals(5, spliterator.batchCount);
        });

        assertEquals(10, result.size());
        List<ResolvedEvent> reversedFirstEvents = reverse(firstEvents.subList(0, 10));
        range(0, 10).forEach(i -> assertEquals(reversedFirstEvents.get(i).event.eventId, result.get(i).event.eventId));
    }

    private Position appendEvents(String stream, List<EventData> events) {
        Position position = eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events.get(0)).join().logPosition;
        eventstore.appendToStream(stream, ExpectedVersion.of(0), events.stream().skip(1).collect(toList())).join();
        return position;
    }

    private static List<EventData> newTestEvents(int count) {
        return range(0, count).mapToObj(i -> newTestEvent()).collect(toList());
    }

    private static class AllEventsSpliteratorWithBatchCounter extends AllEventsSpliterator {
        int batchCount;

        AllEventsSpliteratorWithBatchCounter(Position position, Function<Position, CompletableFuture<AllEventsSlice>> reader) {
            super(position, reader);
        }

        @Override
        protected void onBatchReceived(AllEventsSlice slice) {
            batchCount++;
            super.onBatchReceived(slice);
        }
    }

}
