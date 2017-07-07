package com.github.msemys.esjc;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.github.msemys.esjc.matcher.RecordedEventListMatcher.containsInOrder;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class ITStreamEventsSpliterator extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void failsWhenActionIsNull() {
        Spliterator<ResolvedEvent> spliterator = new StreamEventsSpliterator(
            StreamPosition.START,
            i -> eventstore.readStreamEventsForward("foo", i, 1, false));

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
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        Spliterator<ResolvedEvent> spliterator = new StreamEventsSpliterator(
            StreamPosition.START,
            i -> eventstore.readStreamEventsForward(stream, i, 17, false));

        List<ResolvedEvent> result = new ArrayList<>();

        spliterator.forEachRemaining(result::add);

        assertFalse(spliterator.tryAdvance(result::add));
        spliterator.forEachRemaining(e -> fail("Should be no more elements remaining"));

        assertNull(spliterator.trySplit());

        assertEquals(19, result.size());
        assertThat(recordedEventsFrom(result), containsInOrder(events));
    }

    @Test
    public void iteratesAllRemainingEventsForward() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(82);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        Spliterator<ResolvedEvent> spliterator = new StreamEventsSpliterator(
            StreamPosition.START,
            i -> eventstore.readStreamEventsForward(stream, i, 17, false));

        List<ResolvedEvent> result = new ArrayList<>();

        assertTrue(spliterator.tryAdvance(result::add));
        spliterator.forEachRemaining(result::add);

        assertFalse(spliterator.tryAdvance(result::add));
        spliterator.forEachRemaining(e -> fail("Should be no more elements remaining"));

        assertNull(spliterator.trySplit());

        assertEquals(82, result.size());
        assertThat(recordedEventsFrom(result), containsInOrder(events));
    }

    @Test
    public void iteratesAllRemainingEventsBackward() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(30);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        Spliterator<ResolvedEvent> spliterator = new StreamEventsSpliterator(
            StreamPosition.END,
            i -> eventstore.readStreamEventsBackward(stream, i, 11, false));

        List<ResolvedEvent> result = new ArrayList<>();

        assertTrue(spliterator.tryAdvance(result::add));
        spliterator.forEachRemaining(result::add);

        assertFalse(spliterator.tryAdvance(result::add));
        spliterator.forEachRemaining(e -> fail("Should be no more elements remaining"));

        assertNull(spliterator.trySplit());

        assertEquals(30, result.size());
        assertThat(recordedEventsFrom(result), containsInOrder(reverse(events)));
    }

    @Test
    public void iteratesAndSplitsEventsForward() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(17);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        Spliterator<ResolvedEvent> spliterator = new StreamEventsSpliterator(
            StreamPosition.START,
            i -> eventstore.readStreamEventsForward(stream, i, 5, false));

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
        assertThat(recordedEventsFrom(result), containsInOrder(events));
    }

    @Test
    public void iteratesAndSplitsEventsBackward() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(40);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        Spliterator<ResolvedEvent> spliterator = new StreamEventsSpliterator(
            StreamPosition.END,
            i -> eventstore.readStreamEventsBackward(stream, i, 8, false));

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
        assertThat(recordedEventsFrom(result), containsInOrder(reverse(events)));
    }

    @Test
    public void splitsAndIteratesEventsForward() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(17);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        Spliterator<ResolvedEvent> spliterator = new StreamEventsSpliterator(
            StreamPosition.START,
            i -> eventstore.readStreamEventsForward(stream, i, 5, false));

        List<ResolvedEvent> result = new ArrayList<>();
        Spliterator<ResolvedEvent> s1;

        s1 = spliterator.trySplit();
        assertNotNull(s1);
        s1.forEachRemaining(result::add);

        spliterator.forEachRemaining(result::add);

        assertNull(spliterator.trySplit());

        assertEquals(17, result.size());
        assertThat(recordedEventsFrom(result), containsInOrder(events));
    }

    @Test
    public void splitsAndIteratesEventsBackward() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(40);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        Spliterator<ResolvedEvent> spliterator = new StreamEventsSpliterator(
            StreamPosition.END,
            i -> eventstore.readStreamEventsBackward(stream, i, 8, false));

        List<ResolvedEvent> result = new ArrayList<>();
        Spliterator<ResolvedEvent> s1;

        s1 = spliterator.trySplit();
        assertNotNull(s1);
        s1.forEachRemaining(result::add);

        spliterator.forEachRemaining(result::add);

        assertNull(spliterator.trySplit());

        assertEquals(40, result.size());
        assertThat(recordedEventsFrom(result), containsInOrder(reverse(events)));
    }

    @Test
    public void splitsEventsForward() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(17);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        Spliterator<ResolvedEvent> spliterator = new StreamEventsSpliterator(
            StreamPosition.START,
            i -> eventstore.readStreamEventsForward(stream, i, 5, false));

        List<ResolvedEvent> result = new ArrayList<>();

        range(0, 4).forEach(i -> {
            Spliterator<ResolvedEvent> s = spliterator.trySplit();
            assertNotNull(s);
            s.forEachRemaining(result::add);
        });

        assertEquals(17, result.size());

        assertNull(spliterator.trySplit());
        spliterator.forEachRemaining(e -> fail("Should be no more elements remaining"));
        assertFalse(spliterator.tryAdvance(result::add));

        assertEquals(17, result.size());
        assertThat(recordedEventsFrom(result), containsInOrder(events));
    }

    @Test
    public void splitsEventsBackward() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(40);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        Spliterator<ResolvedEvent> spliterator = new StreamEventsSpliterator(
            StreamPosition.END,
            i -> eventstore.readStreamEventsBackward(stream, i, 8, false));

        List<ResolvedEvent> result = new ArrayList<>();

        range(0, 5).forEach(i -> {
            Spliterator<ResolvedEvent> s = spliterator.trySplit();
            assertNotNull(s);
            s.forEachRemaining(result::add);
        });

        assertEquals(40, result.size());

        assertNull(spliterator.trySplit());
        spliterator.forEachRemaining(e -> fail("Should be no more elements remaining"));
        assertFalse(spliterator.tryAdvance(result::add));

        assertEquals(40, result.size());
        assertThat(recordedEventsFrom(result), containsInOrder(reverse(events)));
    }

    @Test
    public void lazyReadsBatchesForward() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        StreamEventsSpliteratorWithBatchCounter spliterator = new StreamEventsSpliteratorWithBatchCounter(
            StreamPosition.START,
            i -> eventstore.readStreamEventsForward(stream, i, 3, false));

        assertEquals(0, spliterator.batchCount);

        List<ResolvedEvent> result = new ArrayList<>();

        range(0, 18).forEach(i -> assertTrue(spliterator.tryAdvance(result::add)));

        assertEquals(18, result.size());
        assertEquals(6, spliterator.batchCount);

        assertTrue(spliterator.tryAdvance(result::add));
        assertEquals(7, spliterator.batchCount);

        assertTrue(spliterator.tryAdvance(result::add));
        assertEquals(7, spliterator.batchCount);

        assertFalse(spliterator.tryAdvance(result::add));
        assertEquals(7, spliterator.batchCount);

        assertEquals(20, result.size());

        range(0, 50).forEach(i -> {
            assertFalse(spliterator.tryAdvance(result::add));
            assertEquals(7, spliterator.batchCount);
        });

        assertEquals(20, result.size());
        assertThat(recordedEventsFrom(result), containsInOrder(events));
    }

    @Test
    public void lazyReadsBatchesBackward() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        StreamEventsSpliteratorWithBatchCounter spliterator = new StreamEventsSpliteratorWithBatchCounter(
            19,
            i -> eventstore.readStreamEventsBackward(stream, i, 3, false));

        assertEquals(0, spliterator.batchCount);

        List<ResolvedEvent> result = new ArrayList<>();

        range(0, 9).forEach(i -> assertTrue(spliterator.tryAdvance(result::add)));
        assertEquals(9, result.size());
        assertEquals(3, spliterator.batchCount);

        range(0, 3).forEach(i -> assertTrue(spliterator.tryAdvance(result::add)));
        assertEquals(12, result.size());
        assertEquals(4, spliterator.batchCount);

        range(0, 3).forEach(i -> assertTrue(spliterator.tryAdvance(result::add)));
        assertEquals(15, result.size());
        assertEquals(5, spliterator.batchCount);

        assertTrue(spliterator.tryAdvance(result::add));
        assertEquals(6, spliterator.batchCount);
        assertTrue(spliterator.tryAdvance(result::add));
        assertEquals(6, spliterator.batchCount);
        assertTrue(spliterator.tryAdvance(result::add));
        assertEquals(6, spliterator.batchCount);

        assertTrue(spliterator.tryAdvance(result::add));
        assertEquals(7, spliterator.batchCount);
        assertTrue(spliterator.tryAdvance(result::add));
        assertEquals(7, spliterator.batchCount);
        assertFalse(spliterator.tryAdvance(result::add));
        assertEquals(7, spliterator.batchCount);

        assertEquals(20, result.size());

        range(0, 50).forEach(i -> {
            assertFalse(spliterator.tryAdvance(result::add));
            assertEquals(7, spliterator.batchCount);
        });

        assertEquals(20, result.size());
        assertThat(recordedEventsFrom(result), containsInOrder(reverse(events)));
    }

    private static class StreamEventsSpliteratorWithBatchCounter extends StreamEventsSpliterator {
        int batchCount;

        StreamEventsSpliteratorWithBatchCounter(long eventNumber, Function<Long, CompletableFuture<StreamEventsSlice>> reader) {
            super(eventNumber, reader);
        }

        @Override
        protected void onBatchReceived(StreamEventsSlice slice) {
            batchCount++;
            super.onBatchReceived(slice);
        }
    }

}
