package com.github.msemys.esjc;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.github.msemys.esjc.matcher.RecordedEventListMatcher.containsInOrder;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class ITReadAllEventsForward extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void returnsEmptySliceIfAskedToReadFromEnd() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(20)).join();

        AllEventsSlice slice = eventstore.readAllEventsForward(Position.END, 1, false).join();

        assertTrue(slice.isEndOfStream());
        assertThat(slice.events.size(), is(0));
    }

    @Test
    public void returnsEventsInSameOrderAsWritten() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        Position position = eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events.get(0)).join().logPosition;
        eventstore.appendToStream(stream, ExpectedVersion.of(0), events.stream().skip(1).collect(toList())).join();
        eventstore.appendToStream(stream, ExpectedVersion.of(19), asList(newTestEvent(), newTestEvent())).join();

        AllEventsSlice slice = eventstore.readAllEventsForward(position, events.size() + 10, false).join();

        assertTrue(slice.events.size() >= events.size() + 2);
        assertThat(slice.events.stream().limit(events.size()).map(e -> e.event).collect(toList()), containsInOrder(events));
    }

    @Test
    public void readsSlice() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        Position position = eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events.get(0)).join().logPosition;
        eventstore.appendToStream(stream, ExpectedVersion.of(0), events.stream().skip(1).collect(toList())).join();

        AllEventsSlice slice = eventstore.readAllEventsForward(position, 4096, false).join();

        assertTrue(slice.events.size() >= events.size());
        assertThat(slice.events.stream().limit(events.size()).map(e -> e.event).collect(toList()), containsInOrder(events));
    }

    @Test
    public void readsAllEventsOneByOneUntilEndOfStream() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

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

        assertThat(allEvents.stream().skip(index).limit(events.size()).collect(toList()), containsInOrder(events));
    }

    @Test
    public void readsAllEventsUntilEndOfStream() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

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

        assertThat(allEvents.stream().skip(index).limit(events.size()).collect(toList()), containsInOrder(events));
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToReadWhenMaxCountOutOfRange() {
        eventstore.readAllEventsForward(Position.START, 4097, false);
    }

    @Test
    public void returnsEventsIfResolveLinkTosIsDisabledAndLinkedStreamIsHardDeleted() {
        final String stream1 = generateStreamName();
        final String stream2 = generateStreamName();
        final String stream3 = generateStreamName();

        eventstore.appendToStream(stream1, ExpectedVersion.NO_STREAM, newTestEvent()).join();
        eventstore.appendToStream(stream2, ExpectedVersion.NO_STREAM, newTestEvent()).join();

        Position position = eventstore.appendToStream(stream3, ExpectedVersion.NO_STREAM, newTestEvent()).join().logPosition;

        eventstore.appendToStream(stream3, ExpectedVersion.ANY, asList(
            EventData.newBuilder()
                .linkTo(0, stream1)
                .build(),
            EventData.newBuilder()
                .linkTo(0, stream2)
                .build(),
            newTestEvent()
        )).join();

        eventstore.deleteStream(stream1, ExpectedVersion.ANY, true).join();

        AllEventsSlice slice = eventstore.readAllEventsForward(position, 10, false).join();

        List<ResolvedEvent> streamEvents = slice.events.stream()
            .filter(e -> e.originalStreamId().equals(stream3))
            .collect(toList());

        assertEquals(4, streamEvents.size());

        assertNotNull(streamEvents.get(1).event);
        assertNull(streamEvents.get(1).link);

        assertNotNull(streamEvents.get(2).event);
        assertNull(streamEvents.get(2).link);
    }

    @Test
    public void returnsEventsIfResolveLinkTosIsEnabledAndLinkedStreamIsHardDeleted() {
        final String stream1 = generateStreamName();
        final String stream2 = generateStreamName();
        final String stream3 = generateStreamName();

        eventstore.appendToStream(stream1, ExpectedVersion.NO_STREAM, newTestEvent()).join();
        eventstore.appendToStream(stream2, ExpectedVersion.NO_STREAM, newTestEvent()).join();

        Position position = eventstore.appendToStream(stream3, ExpectedVersion.NO_STREAM, newTestEvent()).join().logPosition;

        eventstore.appendToStream(stream3, ExpectedVersion.ANY, asList(
            EventData.newBuilder()
                .linkTo(0, stream1)
                .build(),
            EventData.newBuilder()
                .linkTo(0, stream2)
                .build(),
            newTestEvent()
        )).join();

        eventstore.deleteStream(stream1, ExpectedVersion.ANY, true).join();

        AllEventsSlice slice = eventstore.readAllEventsForward(position, 10, true).join();

        List<ResolvedEvent> streamEvents = slice.events.stream()
            .filter(e -> e.originalStreamId().equals(stream3))
            .collect(toList());

        assertEquals(4, streamEvents.size());

        assertNull(streamEvents.get(1).event);
        assertNotNull(streamEvents.get(1).link);

        assertNotNull(streamEvents.get(2).event);
        assertNotNull(streamEvents.get(2).link);
    }

}
