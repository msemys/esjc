package com.github.msemys.esjc;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.github.msemys.esjc.matcher.RecordedEventListMatcher.hasItems;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ITReadAllEventsBackward extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void returnsEmptySliceIfAskedToReadFromStart() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(20)).join();

        AllEventsSlice slice = eventstore.readAllEventsBackward(Position.START, 1, false).join();

        assertTrue(slice.isEndOfStream());
        assertThat(slice.events.size(), is(0));
    }

    @Test
    public void readsFirstEvent() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(20)).join();

        List<ResolvedEvent> firstEvents = eventstore.readAllEventsForward(Position.START, 2, false).join().events;

        Position position = firstEvents.get(1).originalPosition;

        List<ResolvedEvent> result = eventstore.readAllEventsBackward(position, 1, false).join().events;

        assertEquals(1, result.size());
        assertEquals(firstEvents.get(0).event.eventId, result.get(0).event.eventId);
    }

    @Test
    public void returnsEventsInReversedOrderComparedToWritten() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        AllEventsSlice slice = eventstore.readAllEventsBackward(Position.END, events.size(), false).join();

        assertThat(slice.events.stream().limit(events.size()).map(e -> e.event).collect(toList()), hasItems(reverse(events)));
    }

    @Test
    public void readsSlice() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        AllEventsSlice slice = eventstore.readAllEventsBackward(Position.END, 4096, false).join();

        assertTrue(slice.events.size() <= 4096);

        assertThat(slice.events.stream().limit(events.size()).map(e -> e.event).collect(toList()), hasItems(reverse(events)));
    }

    @Test
    public void readsAllEventsOneByOneUntilEndOfStream() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        List<RecordedEvent> allEvents = new ArrayList<>();
        Position position = Position.END;
        AllEventsSlice slice;

        while (!(slice = eventstore.readAllEventsBackward(position, 1, false).join()).isEndOfStream()) {
            allEvents.add(slice.events.get(0).event);
            position = slice.nextPosition;
        }

        assertThat(allEvents.stream().limit(events.size()).collect(toList()), hasItems(reverse(events)));
    }

    @Test
    public void readsAllEventsUntilEndOfStream() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        List<RecordedEvent> allEvents = new ArrayList<>();
        Position position = Position.END;
        AllEventsSlice slice;

        while (!(slice = eventstore.readAllEventsBackward(position, 5, false).join()).isEndOfStream()) {
            allEvents.addAll(slice.events.stream().map(e -> e.event).collect(toList()));
            position = slice.nextPosition;
        }

        assertThat(allEvents.stream().limit(events.size()).collect(toList()), hasItems(reverse(events)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToReadWhenMaxCountOutOfRange() {
        eventstore.readAllEventsBackward(Position.START, 4097, false);
    }

}
