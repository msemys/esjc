package com.github.msemys.esjc;

import com.github.msemys.esjc.system.SystemEventType;
import org.junit.Test;

import java.util.List;

import static com.github.msemys.esjc.matcher.RecordedEventMatcher.hasItems;
import static com.github.msemys.esjc.system.SystemStreams.metastreamOf;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.junit.Assert.*;

public class ITReadAllEventsForwardWithSoftDeletedStream extends AbstractIntegrationTest {

    private static final Integer DELETED_STREAM_EVENT_NUMBER = Integer.MAX_VALUE;

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void ensuresDeletedStream() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.noStream(), newTestEvents()).join();
        eventstore.deleteStream(stream, ExpectedVersion.any()).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 100, false).join();

        assertEquals(SliceReadStatus.StreamNotFound, slice.status);
        assertTrue(slice.events.isEmpty());
    }

    @Test
    public void returnsAllEventsIncludingTombstone() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents();
        Position position = eventstore.appendToStream(stream, ExpectedVersion.noStream(), asList(events.get(0))).join().logPosition;
        eventstore.appendToStream(stream, ExpectedVersion.of(0), events.stream().skip(1).collect(toList())).join();
        eventstore.deleteStream(stream, ExpectedVersion.any()).join();

        AllEventsSlice slice = eventstore.readAllEventsForward(position, events.size() + 10, false).join();

        assertThat(slice.events.stream().limit(events.size()).map(e -> e.event).collect(toList()), hasItems(events));

        RecordedEvent lastEvent = slice.events.get(slice.events.size() - 1).event;
        assertEquals(metastreamOf(stream), lastEvent.eventStreamId);
        assertEquals(SystemEventType.STREAM_METADATA.value, lastEvent.eventType);

        StreamMetadata metadata = StreamMetadata.fromJson(lastEvent.data);
        assertEquals(DELETED_STREAM_EVENT_NUMBER, metadata.truncateBefore);
    }

    private static List<EventData> newTestEvents() {
        return range(0, 20).mapToObj(i -> newTestEvent()).collect(toList());
    }

}
