package com.github.msemys.esjc;

import com.github.msemys.esjc.system.SystemEventTypes;
import org.junit.Test;

import java.util.List;

import static com.github.msemys.esjc.matcher.RecordedEventListMatcher.containsInOrder;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

public class ITReadAllEventsForwardWithHardDeletedStream extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void ensuresDeletedStream() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(20)).join();
        eventstore.deleteStream(stream, ExpectedVersion.ANY, true).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 100, false).join();

        assertEquals(SliceReadStatus.StreamDeleted, slice.status);
        assertTrue(slice.events.isEmpty());
    }

    @Test
    public void returnsAllEventsIncludingTombstone() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        Position position = eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events.get(0)).join().logPosition;
        eventstore.appendToStream(stream, 0, events.stream().skip(1).collect(toList())).join();
        eventstore.deleteStream(stream, ExpectedVersion.ANY, true).join();

        AllEventsSlice slice = eventstore.readAllEventsForward(position, events.size() + 10, false).join();

        assertThat(slice.events.stream().limit(events.size()).map(e -> e.event).collect(toList()), containsInOrder(events));

        RecordedEvent lastEvent = slice.events.get(slice.events.size() - 1).event;
        assertEquals(stream, lastEvent.eventStreamId);
        assertEquals(SystemEventTypes.STREAM_DELETED, lastEvent.eventType);
    }

}
