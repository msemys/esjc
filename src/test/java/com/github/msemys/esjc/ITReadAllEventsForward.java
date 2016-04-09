package com.github.msemys.esjc;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.github.msemys.esjc.matcher.RecordedEventMatcher.hasItems;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ITReadAllEventsForward extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void returnsEmptySliceIfAskedToReadFromEnd() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.noStream(), newTestEvents()).join();

        AllEventsSlice slice = eventstore.readAllEventsForward(Position.END, 1, false).join();

        assertTrue(slice.isEndOfStream());
        assertThat(slice.events.size(), is(0));
    }

    @Test
    public void returnsEventsInSameOrderAsWritten() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents();
        Position position = eventstore.appendToStream(stream, ExpectedVersion.noStream(), asList(events.get(0))).join().logPosition;
        eventstore.appendToStream(stream, ExpectedVersion.of(0), events.stream().skip(1).collect(toList())).join();
        eventstore.appendToStream(stream, ExpectedVersion.of(19), asList(newTestEvent(), newTestEvent())).join();

        AllEventsSlice slice = eventstore.readAllEventsForward(position, events.size() + 10, false).join();

        assertTrue(slice.events.size() >= events.size() + 2);
        assertThat(slice.events.stream().limit(events.size()).map(e -> e.event).collect(toList()), hasItems(events));
    }

    @Test
    public void readsSlice() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents();
        Position position = eventstore.appendToStream(stream, ExpectedVersion.noStream(), asList(events.get(0))).join().logPosition;
        eventstore.appendToStream(stream, ExpectedVersion.of(0), events.stream().skip(1).collect(toList())).join();

        AllEventsSlice slice = eventstore.readAllEventsForward(position, 4095, false).join();

        assertTrue(slice.events.size() >= events.size());
        assertThat(slice.events.stream().limit(events.size()).map(e -> e.event).collect(toList()), hasItems(events));
    }

    @Test
    public void readsAllEventsOneByOneUntilEndOfStream() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents();
        eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join();

        List<RecordedEvent> allEvents = new ArrayList<>();
        Position position = Position.START;
        AllEventsSlice slice;

        while (!(slice = eventstore.readAllEventsForward(position, 1, false).join()).isEndOfStream()) {
            allEvents.add(slice.events.get(0).event);
            position = slice.nextPosition;
        }

        int index = allEvents.indexOf(allEvents.stream()
            .filter(e -> e.eventId.equals(events.get(0).eventId))
            .findFirst()
            .get());

        assertThat(allEvents.stream().skip(index).limit(events.size()).collect(toList()), hasItems(events));
    }

    @Test
    public void readsAllEventsUntilEndOfStream() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents();
        eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join();

        List<RecordedEvent> allEvents = new ArrayList<>();
        Position position = Position.START;
        AllEventsSlice slice;

        while (!(slice = eventstore.readAllEventsForward(position, 5, false).join()).isEndOfStream()) {
            allEvents.addAll(slice.events.stream().map(e -> e.event).collect(toList()));
            position = slice.nextPosition;
        }

        int index = allEvents.indexOf(allEvents.stream()
            .filter(e -> e.eventId.equals(events.get(0).eventId))
            .findFirst()
            .get());

        assertThat(allEvents.stream().skip(index).limit(events.size()).collect(toList()), hasItems(events));
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToReadWhenMaxCountOutOfRange() {
        eventstore.readAllEventsForward(Position.START, 4096, false);
    }

    private static List<EventData> newTestEvents() {
        return range(0, 20).mapToObj(i -> newTestEvent()).collect(toList());
    }

}
