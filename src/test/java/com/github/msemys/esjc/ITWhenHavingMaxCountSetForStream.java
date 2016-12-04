package com.github.msemys.esjc;

import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ITWhenHavingMaxCountSetForStream extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void readStreamForwardRespectsMaxCount() {
        final String stream = generateStreamName();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.newBuilder().maxCount(3).build()).join();

        List<EventData> events = newTestEvents(5);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice.status);
        assertEquals(3, slice.events.size());
        assertArrayEquals(events.stream().skip(2).map(e -> e.eventId).toArray(UUID[]::new),
            slice.events.stream().map(e -> e.event.eventId).toArray(UUID[]::new));
    }

    @Test
    public void readStreamBackwardRespectsMaxCount() {
        final String stream = generateStreamName();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.newBuilder().maxCount(3).build()).join();

        List<EventData> events = newTestEvents(5);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        StreamEventsSlice slice = eventstore.readStreamEventsBackward(stream, -1, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice.status);
        assertEquals(3, slice.events.size());
        assertArrayEquals(events.stream().skip(2).map(e -> e.eventId).toArray(UUID[]::new),
            reverse(slice.events).stream().map(e -> e.event.eventId).toArray(UUID[]::new));
    }

    @Test
    public void afterSettingLessStrictMaxCountReadStreamForwardReadsMoreEvents() {
        final String stream = generateStreamName();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.newBuilder().maxCount(3).build()).join();

        List<EventData> events = newTestEvents(5);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        StreamEventsSlice slice1 = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice1.status);
        assertEquals(3, slice1.events.size());
        assertArrayEquals(events.stream().skip(2).map(e -> e.eventId).toArray(UUID[]::new),
            slice1.events.stream().map(e -> e.event.eventId).toArray(UUID[]::new));

        eventstore.setStreamMetadata(stream, ExpectedVersion.of(0), StreamMetadata.newBuilder().maxCount(4).build()).join();

        StreamEventsSlice slice2 = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice2.status);
        assertEquals(4, slice2.events.size());
        assertArrayEquals(events.stream().skip(1).map(e -> e.eventId).toArray(UUID[]::new),
            slice2.events.stream().map(e -> e.event.eventId).toArray(UUID[]::new));
    }

    @Test
    public void afterSettingMoreStrictMaxCountReadStreamForwardReadsLessEvents() {
        final String stream = generateStreamName();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.newBuilder().maxCount(3).build()).join();

        List<EventData> events = newTestEvents(5);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        StreamEventsSlice slice1 = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice1.status);
        assertEquals(3, slice1.events.size());
        assertArrayEquals(events.stream().skip(2).map(e -> e.eventId).toArray(UUID[]::new),
            slice1.events.stream().map(e -> e.event.eventId).toArray(UUID[]::new));

        eventstore.setStreamMetadata(stream, ExpectedVersion.of(0), StreamMetadata.newBuilder().maxCount(2).build()).join();

        StreamEventsSlice slice2 = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice2.status);
        assertEquals(2, slice2.events.size());
        assertArrayEquals(events.stream().skip(3).map(e -> e.eventId).toArray(UUID[]::new),
            slice2.events.stream().map(e -> e.event.eventId).toArray(UUID[]::new));
    }

    @Test
    public void afterSettingLessStrictMaxCountReadStreamBackwardReadsMoreEvents() {
        final String stream = generateStreamName();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.newBuilder().maxCount(3).build()).join();

        List<EventData> events = newTestEvents(5);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        StreamEventsSlice slice1 = eventstore.readStreamEventsBackward(stream, -1, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice1.status);
        assertEquals(3, slice1.events.size());
        assertArrayEquals(events.stream().skip(2).map(e -> e.eventId).toArray(UUID[]::new),
            reverse(slice1.events).stream().map(e -> e.event.eventId).toArray(UUID[]::new));

        eventstore.setStreamMetadata(stream, ExpectedVersion.of(0), StreamMetadata.newBuilder().maxCount(4).build()).join();

        StreamEventsSlice slice2 = eventstore.readStreamEventsBackward(stream, -1, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice2.status);
        assertEquals(4, slice2.events.size());
        assertArrayEquals(events.stream().skip(1).map(e -> e.eventId).toArray(UUID[]::new),
            reverse(slice2.events).stream().map(e -> e.event.eventId).toArray(UUID[]::new));
    }

    @Test
    public void afterSettingMoreStrictMaxCountReadStreamBackwardReadsLessEvents() {
        final String stream = generateStreamName();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.newBuilder().maxCount(3).build()).join();

        List<EventData> events = newTestEvents(5);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        StreamEventsSlice slice1 = eventstore.readStreamEventsBackward(stream, -1, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice1.status);
        assertEquals(3, slice1.events.size());
        assertArrayEquals(events.stream().skip(2).map(e -> e.eventId).toArray(UUID[]::new),
            reverse(slice1.events).stream().map(e -> e.event.eventId).toArray(UUID[]::new));

        eventstore.setStreamMetadata(stream, ExpectedVersion.of(0), StreamMetadata.newBuilder().maxCount(2).build()).join();

        StreamEventsSlice slice2 = eventstore.readStreamEventsBackward(stream, -1, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice2.status);
        assertEquals(2, slice2.events.size());
        assertArrayEquals(events.stream().skip(3).map(e -> e.eventId).toArray(UUID[]::new),
            reverse(slice2.events).stream().map(e -> e.event.eventId).toArray(UUID[]::new));
    }

}
