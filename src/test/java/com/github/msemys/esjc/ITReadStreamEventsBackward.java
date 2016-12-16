package com.github.msemys.esjc;

import org.junit.Test;

import java.util.List;

import static com.github.msemys.esjc.matcher.RecordedEventListMatcher.hasItems;
import static com.github.msemys.esjc.matcher.RecordedEventMatcher.equalTo;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

public class ITReadStreamEventsBackward extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsWhenCountIsZero() {
        final String stream = generateStreamName();
        eventstore.readStreamEventsBackward(stream, 0, 0, false).join();
    }

    @Test
    public void returnsStreamNotFoundIfStreamNotFound() {
        final String stream = generateStreamName();

        StreamEventsSlice slice = eventstore.readStreamEventsBackward(stream, StreamPosition.END, 1, false).join();

        assertEquals(SliceReadStatus.StreamNotFound, slice.status);
    }

    @Test
    public void returnsStreamDeletedIfStreamWasDeleted() {
        final String stream = generateStreamName();

        eventstore.deleteStream(stream, ExpectedVersion.NO_STREAM, true).join();

        StreamEventsSlice slice = eventstore.readStreamEventsBackward(stream, StreamPosition.END, 1, false).join();

        assertEquals(SliceReadStatus.StreamDeleted, slice.status);
    }

    @Test
    public void returnsNoEventsWhenCalledOnEmptyStream() {
        final String stream = generateStreamName();

        StreamEventsSlice slice = eventstore.readStreamEventsBackward(stream, StreamPosition.END, 1, false).join();

        assertTrue(slice.events.isEmpty());
    }

    @Test
    public void returnsPartialSliceIfNoEnoughEventsInStream() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(10)).join();

        StreamEventsSlice slice = eventstore.readStreamEventsBackward(stream, 1, 5, false).join();

        assertEquals(2, slice.events.size());
    }

    @Test
    public void returnsEventsReversedComparedToWritten() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(10);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        StreamEventsSlice slice = eventstore.readStreamEventsBackward(stream, StreamPosition.END, events.size(), false).join();

        assertThat(slice.events.stream().map(e -> e.event).collect(toList()), hasItems(reverse(events)));
    }

    @Test
    public void readsSingleEventFromArbitraryPosition() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(10);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        StreamEventsSlice slice = eventstore.readStreamEventsBackward(stream, 7, 1, false).join();

        assertThat(slice.events.get(0).event, equalTo(events.get(7)));
    }

    @Test
    public void readsFirstEvent() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(10)).join();

        StreamEventsSlice slice = eventstore.readStreamEventsBackward(stream, StreamPosition.START, 1, false).join();

        assertEquals(1, slice.events.size());
    }

    @Test
    public void readsLastEvent() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(10);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        StreamEventsSlice slice = eventstore.readStreamEventsBackward(stream, StreamPosition.END, 1, false).join();

        assertThat(slice.events.get(0).event, equalTo(events.get(events.size() - 1)));
    }

    @Test
    public void readsSliceFromArbitraryPosition() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(10);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        StreamEventsSlice slice = eventstore.readStreamEventsBackward(stream, 3, 2, false).join();

        List<EventData> expectedEvents = reverse(events.stream().skip(2).limit(2).collect(toList()));
        assertThat(slice.events.stream().map(e -> e.event).collect(toList()), hasItems(expectedEvents));
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToReadWhenMaxCountOutOfRange() {
        eventstore.readStreamEventsBackward("foo", StreamPosition.START, Integer.MAX_VALUE, false).join();
    }

}
