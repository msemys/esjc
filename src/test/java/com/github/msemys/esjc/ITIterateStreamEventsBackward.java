package com.github.msemys.esjc;

import com.github.msemys.esjc.operation.StreamDeletedException;
import com.github.msemys.esjc.operation.StreamNotFoundException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static com.github.msemys.esjc.matcher.IteratorSizeMatcher.hasSize;
import static com.github.msemys.esjc.matcher.RecordedEventListMatcher.containsInOrder;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class ITIterateStreamEventsBackward extends AbstractEventStoreTest {

    public ITIterateStreamEventsBackward(EventStore eventstore) {
        super(eventstore);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToIterateWhenBatchSizeIsZero() {
        eventstore.iterateStreamEventsBackward(generateStreamName(), 0, 0, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToIterateWhenBatchSizeOutOfRange() {
        eventstore.iterateStreamEventsBackward("foo", StreamPosition.START, Integer.MAX_VALUE, false);
    }

    @Test
    public void failsToCallHasNextWhenIteratingNonExistingStream() {
        final String stream = generateStreamName();

        Iterator<ResolvedEvent> iterator = eventstore.iterateStreamEventsBackward(stream, 0, 5, false);

        try {
            iterator.hasNext();
            fail("should fail with 'StreamNotFoundException'");
        } catch (Exception e) {
            assertThat(e, instanceOf(StreamNotFoundException.class));
            assertEquals(stream, ((StreamNotFoundException) e).stream);
        }
    }

    @Test
    public void failsToCallNextWhenIteratingNonExistingStream() {
        final String stream = generateStreamName();

        Iterator<ResolvedEvent> iterator = eventstore.iterateStreamEventsBackward(stream, 0, 5, false);

        try {
            iterator.next();
            fail("should fail with 'StreamNotFoundException'");
        } catch (Exception e) {
            assertThat(e, instanceOf(StreamNotFoundException.class));
            assertEquals(stream, ((StreamNotFoundException) e).stream);
        }
    }

    @Test
    public void failsToCallHasNextWhenIteratingDeletedStream() {
        final String stream = generateStreamName();

        eventstore.deleteStream(stream, ExpectedVersion.NO_STREAM, true).join();

        Iterator<ResolvedEvent> iterator = eventstore.iterateStreamEventsBackward(stream, 0, 5, false);

        try {
            iterator.hasNext();
            fail("should fail with 'StreamDeletedException'");
        } catch (Exception e) {
            assertThat(e, instanceOf(StreamDeletedException.class));
            assertEquals(stream, ((StreamDeletedException) e).stream);
        }
    }

    @Test
    public void failsToCallNextWhenIteratingDeletedStream() {
        final String stream = generateStreamName();

        eventstore.deleteStream(stream, ExpectedVersion.NO_STREAM, true).join();

        Iterator<ResolvedEvent> iterator = eventstore.iterateStreamEventsBackward(stream, 0, 5, false);

        try {
            iterator.next();
            fail("should fail with 'StreamDeletedException'");
        } catch (Exception e) {
            assertThat(e, instanceOf(StreamDeletedException.class));
            assertEquals(stream, ((StreamDeletedException) e).stream);
        }
    }

    @Test
    public void failsToCallNextAfterEndOfStreamIsReached() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(10)).join();

        Iterator<ResolvedEvent> iterator = eventstore.iterateStreamEventsBackward(stream, 8, 3, false);

        assertTrue(iterator.hasNext());

        iterator.forEachRemaining(e -> {
            // do nothing
        });

        assertFalse(iterator.hasNext());

        try {
            iterator.next();
            fail("should fail with 'NoSuchElementException'");
        } catch (Exception e) {
            assertThat(e, instanceOf(NoSuchElementException.class));
        }
    }

    @Test
    public void iteratesStreamEventsToBegging() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(10);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        List<RecordedEvent> result = new ArrayList<>();
        eventstore.iterateStreamEventsBackward(stream, 8, 5, false).forEachRemaining(e -> result.add(e.event));

        assertEquals(9, result.size());
        assertThat(result, containsInOrder(reverse(events.stream().limit(9).collect(toList()))));
    }

    @Test
    public void iteratesStreamEventsFromEndToStartWithSmallBatchSize() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(10)).join();

        Iterator<ResolvedEvent> iterator = eventstore.iterateStreamEventsBackward(stream, StreamPosition.END, 2, false);
        assertThat(iterator, hasSize(10));
    }

    @Test
    public void iteratesStreamEventsFromEndToStartWithLargeBatchSize() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(10)).join();

        Iterator<ResolvedEvent> iterator = eventstore.iterateStreamEventsBackward(stream, StreamPosition.END, 20, false);
        assertThat(iterator, hasSize(10));
    }

    @Test
    public void iteratesStreamEventsFromEndToStartWithMaxBatchSize() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(10)).join();

        Iterator<ResolvedEvent> iterator = eventstore.iterateStreamEventsBackward(stream, StreamPosition.END, 4096, false);
        assertThat(iterator, hasSize(10));
    }

    @Test
    public void iteratesEventsReversedComparedToWritten() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(10);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        List<RecordedEvent> result = new ArrayList<>();
        eventstore.iterateStreamEventsBackward(stream, StreamPosition.END, 3, false).forEachRemaining(e -> result.add(e.event));

        assertEquals(10, result.size());
        assertThat(result, containsInOrder(reverse(events)));
    }

    @Test
    public void iteratesFirstEvent() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(10);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        Iterator<ResolvedEvent> iterator = eventstore.iterateStreamEventsBackward(stream, StreamPosition.START, 5, false);

        assertTrue(iterator.hasNext());
        assertEquals(events.get(0).eventId, iterator.next().event.eventId);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void iteratesLastEvent() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(10);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        Iterator<ResolvedEvent> iterator = eventstore.iterateStreamEventsBackward(stream, StreamPosition.END, 1, false);

        assertTrue(iterator.hasNext());
        assertEquals(events.get(9).eventId, iterator.next().event.eventId);
        assertTrue(iterator.hasNext());
        assertEquals(events.get(8).eventId, iterator.next().event.eventId);
        assertTrue(iterator.hasNext());
    }

}
