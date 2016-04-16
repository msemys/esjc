package com.github.msemys.esjc;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.github.msemys.esjc.matcher.RecordedEventMatcher.hasItems;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.core.Is.is;
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

        eventstore.appendToStream(stream, ExpectedVersion.noStream(), newTestEvents()).join();

        AllEventsSlice slice = eventstore.readAllEventsBackward(Position.START, 1, false).join();

        assertTrue(slice.isEndOfStream());
        assertThat(slice.events.size(), is(0));
    }

    @Test
    public void returnsEventsInReversedOrderComparedToWritten() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents();
        eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join();

        AllEventsSlice slice = eventstore.readAllEventsBackward(Position.END, events.size(), false).join();

        assertThat(slice.events.stream().limit(events.size()).map(e -> e.event).collect(toList()), hasItems(reverse(events)));
    }

    @Test
    public void readsSlice() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents();
        eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join();

        AllEventsSlice slice = eventstore.readAllEventsBackward(Position.END, 4095, false).join();

        assertTrue(slice.events.size() <= 4095);

        assertThat(slice.events.stream().limit(events.size()).map(e -> e.event).collect(toList()), hasItems(reverse(events)));
    }

    @Test
    public void readsAllEventsOneByOneUntilEndOfStream() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents();
        eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join();

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

        List<EventData> events = newTestEvents();
        eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join();

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
        eventstore.readAllEventsBackward(Position.START, 4096, false);
    }

    private static List<EventData> newTestEvents() {
        return range(0, 20).mapToObj(i -> newTestEvent()).collect(toList());
    }

}
