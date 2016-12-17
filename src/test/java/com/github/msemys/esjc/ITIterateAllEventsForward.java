package com.github.msemys.esjc;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static com.github.msemys.esjc.matcher.RecordedEventListMatcher.containsInOrder;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class ITIterateAllEventsForward extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToIterateWhenBatchSizeIsZero() {
        eventstore.iterateAllEventsForward(Position.START, 0, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToIterateWhenBatchSizeOutOfRange() {
        eventstore.iterateAllEventsForward(Position.START, 4097, false);
    }

    @Test
    public void failsToCallNextAfterEndOfStreamIsReached() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        Position position = eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events.get(0)).join().logPosition;
        eventstore.appendToStream(stream, ExpectedVersion.of(0), events.stream().skip(1).collect(toList())).join();

        Iterator<ResolvedEvent> iterator = eventstore.iterateAllEventsForward(position, 3, false);

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
    public void returnsEmptyIteratorIfAskedToIterateFromEnd() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(20)).join();

        Iterator<ResolvedEvent> iterator = eventstore.iterateAllEventsForward(Position.END, 1, false);

        assertFalse(iterator.hasNext());
    }

    @Test
    public void returnsEventsInSameOrderAsWritten() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        Position position = eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events.get(0)).join().logPosition;
        eventstore.appendToStream(stream, ExpectedVersion.of(0), events.stream().skip(1).collect(toList())).join();

        List<RecordedEvent> result = new ArrayList<>();
        eventstore.iterateAllEventsForward(position, 3, false).forEachRemaining(e -> result.add(e.event));

        assertEquals(20, result.size());
        assertThat(result, containsInOrder(events));
    }

    @Test
    public void iteratesAllEventsOneByOneUntilEndOfStream() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        List<RecordedEvent> allEvents = new ArrayList<>();
        eventstore.iterateAllEventsForward(Position.START, 1, false).forEachRemaining(e -> allEvents.add(e.event));

        int index = allEvents.indexOf(allEvents.stream()
            .filter(e -> e.eventId.equals(events.get(0).eventId))
            .findFirst()
            .get());

        assertThat(allEvents.stream().skip(index).limit(events.size()).collect(toList()), containsInOrder(events));
    }

    @Test
    public void iteratesAllEventsUntilEndOfStream() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        List<RecordedEvent> allEvents = new ArrayList<>();
        eventstore.iterateAllEventsForward(Position.START, 5, false).forEachRemaining(e -> allEvents.add(e.event));

        int index = allEvents.indexOf(allEvents.stream()
            .filter(e -> e.eventId.equals(events.get(0).eventId))
            .findFirst()
            .get());

        assertThat(allEvents.stream().skip(index).limit(events.size()).collect(toList()), containsInOrder(events));
    }

    @Test
    public void iteratesAllEventsUntilEndOfStreamUsingMaxBatchSize() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        List<RecordedEvent> allEvents = new ArrayList<>();
        eventstore.iterateAllEventsForward(Position.START, 4096, false).forEachRemaining(e -> allEvents.add(e.event));

        int index = allEvents.indexOf(allEvents.stream()
            .filter(e -> e.eventId.equals(events.get(0).eventId))
            .findFirst()
            .get());

        assertThat(allEvents.stream().skip(index).limit(events.size()).collect(toList()), containsInOrder(events));
    }

}
