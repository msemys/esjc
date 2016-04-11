package com.github.msemys.esjc;

import org.junit.Test;

import java.util.List;

import static com.github.msemys.esjc.matcher.RecordedEventMatcher.equalTo;
import static com.github.msemys.esjc.matcher.RecordedEventMatcher.hasItems;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.junit.Assert.*;

public class ITReadStreamEventsForward extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
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

        eventstore.deleteStream(stream, ExpectedVersion.noStream(), true).join();

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

        eventstore.appendToStream(stream, ExpectedVersion.noStream(), newTestEvents()).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 11, 5, false).join();

        assertTrue(slice.events.isEmpty());
    }

    @Test
    public void returnsPartialSliceIfNotEnoughEventsInStream() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.noStream(), newTestEvents()).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 9, 5, false).join();

        assertEquals(1, slice.events.size());
    }

    @Test
    public void returnsEventsInSameOrderAsWritten() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents();
        eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, StreamPosition.START, events.size(), false).join();

        assertThat(slice.events.stream().map(e -> e.event).collect(toList()), hasItems(events));
    }

    @Test
    public void readsSingleEventFromArbitraryPosition() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents();
        eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 5, 1, false).join();

        assertThat(slice.events.get(0).event, equalTo(events.get(5)));
    }

    @Test
    public void readsSliceFromArbitraryPosition() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents();
        eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 5, 2, false).join();

        assertThat(slice.events.stream().map(e -> e.event).collect(toList()),
            hasItems(events.stream().skip(5).limit(2).collect(toList())));
    }

    private static List<EventData> newTestEvents() {
        return range(0, 10).mapToObj(i -> newTestEvent()).collect(toList());
    }

}
