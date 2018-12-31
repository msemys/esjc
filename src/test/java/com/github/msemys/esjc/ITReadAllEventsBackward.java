package com.github.msemys.esjc;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.github.msemys.esjc.matcher.RecordedEventListMatcher.containsInOrder;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class ITReadAllEventsBackward extends AbstractEventStoreTest {

    public ITReadAllEventsBackward(EventStore eventstore) {
        super(eventstore);
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

        assertThat(slice.events.stream().limit(events.size()).map(e -> e.event).collect(toList()), containsInOrder(reverse(events)));
    }

    @Test
    public void readsSlice() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        AllEventsSlice slice = eventstore.readAllEventsBackward(Position.END, 4096, false).join();

        assertTrue(slice.events.size() <= 4096);

        assertThat(slice.events.stream().limit(events.size()).map(e -> e.event).collect(toList()), containsInOrder(reverse(events)));
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

        assertThat(allEvents.stream().limit(events.size()).collect(toList()), containsInOrder(reverse(events)));
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

        assertThat(allEvents.stream().limit(events.size()).collect(toList()), containsInOrder(reverse(events)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToReadWhenMaxCountOutOfRange() {
        eventstore.readAllEventsBackward(Position.START, 4097, false);
    }

    @Test
    public void returnsEventsIfResolveLinkTosIsDisabledAndLinkedStreamIsHardDeleted() {
        final String stream1 = generateStreamName();
        final String stream2 = generateStreamName();
        final String stream3 = generateStreamName();

        eventstore.appendToStream(stream1, ExpectedVersion.NO_STREAM, newTestEvent()).join();
        eventstore.appendToStream(stream2, ExpectedVersion.NO_STREAM, newTestEvent()).join();
        eventstore.appendToStream(stream3, ExpectedVersion.NO_STREAM, asList(
            newTestEvent(),
            EventData.newBuilder()
                .linkTo(0, stream1)
                .build(),
            EventData.newBuilder()
                .linkTo(0, stream2)
                .build(),
            newTestEvent()
        )).join();

        eventstore.deleteStream(stream1, ExpectedVersion.ANY, true).join();

        AllEventsSlice slice = eventstore.readAllEventsBackward(Position.END, 10, false).join();

        List<ResolvedEvent> streamEvents = reverse(slice.events.stream()
            .filter(e -> e.originalStreamId().equals(stream3))
            .collect(toList()));

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
        eventstore.appendToStream(stream3, ExpectedVersion.NO_STREAM, asList(
            newTestEvent(),
            EventData.newBuilder()
                .linkTo(0, stream1)
                .build(),
            EventData.newBuilder()
                .linkTo(0, stream2)
                .build(),
            newTestEvent()
        )).join();

        eventstore.deleteStream(stream1, ExpectedVersion.ANY, true).join();

        AllEventsSlice slice = eventstore.readAllEventsBackward(Position.END, 10, true).join();

        List<ResolvedEvent> streamEvents = reverse(slice.events.stream()
            .filter(e -> e.originalStreamId().equals(stream3))
            .collect(toList()));

        assertEquals(4, streamEvents.size());

        assertNull(streamEvents.get(1).event);
        assertNotNull(streamEvents.get(1).link);

        assertNotNull(streamEvents.get(2).event);
        assertNotNull(streamEvents.get(2).link);
    }

}
