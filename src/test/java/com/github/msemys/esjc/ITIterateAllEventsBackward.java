package com.github.msemys.esjc;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.github.msemys.esjc.matcher.RecordedEventMatcher.hasItems;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.junit.Assert.*;

public class ITIterateAllEventsBackward extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToIterateWhenBatchSizeIsZero() {
        eventstore.iterateAllEventsBackward(Position.START, 0, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToIterateWhenBatchSizeOutOfRange() {
        eventstore.iterateAllEventsBackward(Position.START, 4097, false);
    }

    @Test
    public void returnsEmptyIteratorIfAskedToIterateFromStart() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.noStream(), newTestEvents()).join();

        Iterator<ResolvedEvent> iterator = eventstore.iterateAllEventsBackward(Position.START, 1, false);

        assertFalse(iterator.hasNext());
    }

    @Test
    public void returnsEventsInReversedOrderComparedToWritten() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents();
        eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join();

        Iterator<ResolvedEvent> iterator = eventstore.iterateAllEventsBackward(Position.END, 6, false);

        List<RecordedEvent> result = new ArrayList<>();
        range(0, 20).forEach(i -> result.add(iterator.next().event));

        List<EventData> reversedEvents = reverse(events);

        range(0, 20).forEach(i -> assertEquals(reversedEvents.get(i).eventId, result.get(i).eventId));
    }

    @Test
    public void iteratesAllEventsOneByOneUntilEndOfStream() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents();
        eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join();

        List<RecordedEvent> allEvents = new ArrayList<>();
        eventstore.iterateAllEventsBackward(Position.END, 1, false).forEachRemaining(e -> allEvents.add(e.event));

        assertThat(allEvents.stream().limit(events.size()).collect(toList()), hasItems(reverse(events)));
    }

    @Test
    public void iteratesAllEventsUntilEndOfStream() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents();
        eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join();

        List<RecordedEvent> allEvents = new ArrayList<>();
        eventstore.iterateAllEventsBackward(Position.END, 5, false).forEachRemaining(e -> allEvents.add(e.event));

        assertThat(allEvents.stream().limit(events.size()).collect(toList()), hasItems(reverse(events)));
    }

    private static List<EventData> newTestEvents() {
        return range(0, 20).mapToObj(i -> newTestEvent()).collect(toList());
    }

}
