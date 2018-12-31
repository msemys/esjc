package com.github.msemys.esjc;

import org.junit.Test;

import java.util.List;

import static com.github.msemys.esjc.matcher.RecordedEventListMatcher.containsInOrder;
import static com.github.msemys.esjc.matcher.RecordedEventMatcher.equalTo;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

public class ITReadStreamEventsForward extends AbstractEventStoreTest {

    public ITReadStreamEventsForward(EventStore eventstore) {
        super(eventstore);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToReadWhenMaxCountIsLessOrEqualsZero() {
        final String stream = generateStreamName();
        eventstore.readStreamEventsForward(stream, 0, 0, false).join();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToReadWhenMaxCountOutOfRange() {
        final String stream = generateStreamName();
        eventstore.readStreamEventsForward(stream, StreamPosition.START, Integer.MAX_VALUE, false).join();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsWhenEventNumberIsLessThanMinusOne() {
        final String stream = generateStreamName();
        eventstore.readStreamEventsForward(stream, -1, 1, false).join();
    }

    @Test
    public void returnsStreamNotFoundIfStreamNotFound() {
        final String stream = generateStreamName();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 1, false).join();

        assertEquals(SliceReadStatus.StreamNotFound, slice.status);
    }

    @Test
    public void returnsStreamDeletedIfStreamWasDeleted() {
        final String stream = generateStreamName();

        eventstore.deleteStream(stream, ExpectedVersion.NO_STREAM, true).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 1, false).join();

        assertEquals(SliceReadStatus.StreamDeleted, slice.status);
    }

    @Test
    public void returnsNoEventsWhenCalledOnEmptyStream() {
        final String stream = generateStreamName();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 1, false).join();

        assertTrue(slice.events.isEmpty());
    }

    @Test
    public void returnsEmptySliceWhenCalledOnNonExistingRange() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(10)).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 11, 5, false).join();

        assertTrue(slice.events.isEmpty());
    }

    @Test
    public void returnsPartialSliceIfNotEnoughEventsInStream() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(10)).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 9, 5, false).join();

        assertEquals(1, slice.events.size());
    }

    @Test
    public void returnsEventsInSameOrderAsWritten() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(10);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, StreamPosition.START, events.size(), false).join();

        assertThat(slice.events.stream().map(e -> e.event).collect(toList()), containsInOrder(events));
    }

    @Test
    public void readsSingleEventFromArbitraryPosition() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(10);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 5, 1, false).join();

        assertThat(slice.events.get(0).event, equalTo(events.get(5)));
    }

    @Test
    public void readsSliceFromArbitraryPosition() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(10);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 5, 2, false).join();

        assertThat(slice.events.stream().map(e -> e.event).collect(toList()),
            containsInOrder(events.stream().skip(5).limit(2).collect(toList())));
    }

}
